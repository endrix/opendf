#define _GNU_SOURCE
#include <stdarg.h>
#include <stdlib.h>
#include <strings.h>
#include <string.h>
#include <sched.h>
#include "actors-rts.h"
#include <semaphore.h>
#include <pthread.h>
#include <limits.h>

#define DEFAULT_FIFO_LENGTH	4096

static int arg_loopmax = INT_MAX;
/*
 * Memory organization for runtime:
 *
 *   We should make sure that data is aligned on CACHE_LINE_SIZE
 *   boundaries. In order to make it possible to migrate ports 
 *   between cpus, we allocate space for all ports on all processors.
 *
 *   Producers (OutputPorts):
 *     Global access: numWritten 
 *                    buffer
 *     Local access:  writePos
 *                    capacity
 *                    availSpace
 *                    numReaders
 *                    reader
 *
 *   Consumers (InputPorts):
 *     Global access: numRead 
 *                    buffer
 *     Local access:  readPos
 *                    capacity
 *                    availTokens
 *                    writer
 *
 *   care has to be taken so that:
 *     1. numRead/numWriten and buffers doesn't share the same cacheline.
 *     2. numRead/numWriten are protected with appropriate barriers
 */

#define FLAG_TIMING      0x01
#define FLAG_SINGLE_CPU  0x02

typedef unsigned long long time_base_t;
typedef unsigned long long art_timer_t;
typedef struct {
  art_timer_t prefire;
  art_timer_t read_barrier;
  art_timer_t fire;
  art_timer_t write_barrier;
  art_timer_t postfire;
  art_timer_t sync_unblocked;
  art_timer_t sync_blocked;
  art_timer_t sync_sleep;
  art_timer_t total;
  long long nsleep;
  long long nloops;
} statistics_t;

typedef struct cpu_runtime_data {
  struct cpu_runtime_data *cpu; /* Pointer to first element in this list */
  int cpu_count;
  int cpu_index;
  void *(*main)(struct cpu_runtime_data *, int);
  pthread_t thread;
  int physical_id; /* physical index of this cpu */
  sem_t *sem;
  int *sleep; // Odd value indicates thread sleeping
  int starved; // Does this need to be cache_aligned?
  SharedContext *shared;
  LocalContext *local;
  int actors;
  AbstractActorInstance **actor; /* Pointer to actors for this cpu */
  void *actor_data;
  int *has_affected;
  statistics_t statistics;
} cpu_runtime_data_t;

/*
 * Error reporting
 */
void runtimeError(AbstractActorInstance *pInst, const char *format,...) {
  va_list ap;
  va_start(ap,format);
  vfprintf(stderr,format,ap);
  fprintf(stderr,"\n");
  va_end(ap);
  exit(1);
}

int rangeError(int x, int y, const char *filename, int line) {
  runtimeError(NULL, "Range check error: %d %d %s(%d)\n", 
	       x, y, filename, line);
  return 0;
}

static inline unsigned long long READ_TIMER()
{
  unsigned long long result;
  __asm__ __volatile__ ("rdtsc" : "=A" (result));
  return result;
}

static void add_timer(art_timer_t *timer,
		      time_base_t *tb)
{
  long long tmp;

  tmp = READ_TIMER();
  *timer += tmp - *tb;
  *tb = tmp;
}

/* 
 * Create runtime instances for all needed special cases
 */

static pthread_mutex_t mutex;
static int sleepers;
static long long balance;
static int terminate;

#define TIMING_PROBES

#define EXECUTE_NETWORK single_cpu_timed_execute_network
#define READ_BARRIER()
#define WRITE_BARRIER()
#define MUTEX_LOCK()
#define MUTEX_UNLOCK()
#include "actors_execute_network.h"
#undef EXECUTE_NETWORK
#undef READ_BARRIER
#undef WRITE_BARRIER
#undef MUTEX_LOCK
#undef MUTEX_UNLOCK

#define EXECUTE_NETWORK multi_cpu_timed_execute_network
#define READ_BARRIER() rmb()
#define WRITE_BARRIER() wmb()
#define MUTEX_LOCK() pthread_mutex_lock(&mutex)
#define MUTEX_UNLOCK() pthread_mutex_unlock(&mutex)
#include "actors_execute_network.h"
#undef EXECUTE_NETWORK
#undef READ_BARRIER
#undef WRITE_BARRIER
#undef MUTEX_LOCK
#undef MUTEX_UNLOCK

#undef TIMING_PROBES

#define EXECUTE_NETWORK single_cpu_execute_network
#define READ_BARRIER()
#define WRITE_BARRIER()
#define MUTEX_LOCK()
#define MUTEX_UNLOCK()
#include "actors_execute_network.h"
#undef EXECUTE_NETWORK
#undef READ_BARRIER
#undef WRITE_BARRIER
#undef MUTEX_LOCK
#undef MUTEX_UNLOCK

#define EXECUTE_NETWORK multi_cpu_execute_network
#define READ_BARRIER() rmb()
#define WRITE_BARRIER() wmb()
#define MUTEX_LOCK() pthread_mutex_lock(&mutex)
#define MUTEX_UNLOCK() pthread_mutex_unlock(&mutex)
#include "actors_execute_network.h"
#undef EXECUTE_NETWORK
#undef READ_BARRIER
#undef WRITE_BARRIER
#undef MUTEX_LOCK
#undef MUTEX_UNLOCK

/* 
 * We need to know processor affinity, etc before we allocate the true 
 * datastructures. For now we collect the actor layout in dummy objects
 * and unfolds them when execNetwork is invoked.
 */

typedef struct InputPort_1 {
  struct AbstractActorInstance_1 *owner;
  struct OutputPort_1 *output;
  int created;
  int capacity;
  InputPort *input;
} InputPort_1_t;

typedef struct OutputPort_1 {
  struct AbstractActorInstance_1 *owner;
  int created;
  int capacity;
  int buffer_bytes;
  int numberOfReaders;
  OutputPort *output;
} OutputPort_1_t;

typedef struct Parameter_1 {
  struct Parameter_1 *next;
  const char *key;
  const char *value;
} Parameter_1_t;

typedef struct AbstractActorInstance_1 {
  ActorClass *actorClass;
  int index;
  InputPort *input_list;
  InputPort_1_t *input;
  OutputPort *output_list;
  OutputPort_1_t *output;  
  Parameter_1_t *parameter;
  int affinity;
} ActorInstance_1_t;

AbstractActorInstance *createActorInstance(ActorClass *actorClass)
{
  ActorInstance_1_t *result;

  result = malloc(sizeof(*result));
  result->actorClass = actorClass;
  result->input = (InputPort_1_t*)calloc(actorClass->numInputPorts,
					     sizeof(*result->input));
  result->output = (OutputPort_1_t*)calloc(actorClass->numOutputPorts,
					      sizeof(*result->output));
  result->parameter = NULL;
  result->affinity = -1;
  return (AbstractActorInstance*)result;
}

OutputPort *createOutputPort(AbstractActorInstance *pInstance,
			     const char *portName,
			     int numberOfReaders)
{
  OutputPort *result = NULL;
  ActorInstance_1_t *instance = (ActorInstance_1_t*)pInstance;
  int i;

  for (i = 0 ; i < instance->actorClass->numOutputPorts ; i++) {
    if (strcmp(instance->actorClass->outputPortDescriptions[i].name, 
	       portName)  == 0) {
      instance->output[i].owner = instance;
      instance->output[i].created++;
      instance->output[i].numberOfReaders = numberOfReaders;
      result = (OutputPort*)&instance->output[i];
      break;
    }
  }
  
  return result;
}

InputPort *createInputPort(AbstractActorInstance *pInstance,
			   const char *portName,
			   int capacity)
{
  InputPort *result = NULL;
  ActorInstance_1_t *instance = (ActorInstance_1_t*)pInstance;
  int i;

  for (i = 0 ; i < instance->actorClass->numInputPorts ; i++) {
    if (strcmp(instance->actorClass->inputPortDescriptions[i].name, 
	       portName)  == 0) {
      instance->input[i].owner = instance;
      instance->input[i].created++;
      instance->input[i].capacity = capacity;
      result = (InputPort*)&instance->input[i];
      break;
    }
  }
  
  return result;
}

void connectPorts(OutputPort *outputPort, InputPort *inputPort)
{
  OutputPort_1_t *output = (OutputPort_1_t*)outputPort;
  InputPort_1_t *input = (InputPort_1_t*)inputPort;

  input->output = output;
}

void setParameter(AbstractActorInstance *pInstance,
		  const char *key,
		  const char *value)
{
  ActorInstance_1_t *instance = (ActorInstance_1_t*)pInstance;
  
  if (strcmp(key, "affinity") == 0) {
    instance->affinity = atoi(value);
  } else {
    Parameter_1_t *parameter = malloc(sizeof(*parameter));

    parameter->next = instance->parameter;
    parameter->key = key;
    if (value[0] != '$') {
      parameter->value = value;
    } else {
      char *tmp;

      tmp = getenv(&value[1]);
      if (tmp == NULL) {
	printf("No environment declaration for '%s'\n", &value[1]);
	exit(1);
      } else {
	parameter->value = strdup(tmp);
      }
    }
    instance->parameter = parameter;
  }
}

static int nr_of_cpus(cpu_set_t *cpu_set)
{
  int result, i;

  for (result = 0, i = 0 ; cpu_set && i < CPU_SETSIZE ; i++) {
    if (CPU_ISSET(i, cpu_set)) { result++; }
  }
  return result;
}

static int index_nodes(ActorInstance_1_t **instance,
		       int numInstances)
{
  int i;

  for (i = 0 ; i < numInstances ; i++) {
    int j;
    
    for (j =  i - 1 ; j >= 0 ; j--) {
      if (strcmp(instance[i]->actorClass->name,
		 instance[j]->actorClass->name) == 0) {
	instance[i]->index = instance[j]->index + 1;
	break;
      }
    }
  }
  return 0;
}

static int set_affinity(ActorInstance_1_t **instance,
			int numInstances,
			char *arg)
{
  int result = 0;
  char *p;
  int affinity = 0;

  for (p = arg ; p && *p && *p != '=' ; p++) {
    affinity = affinity * 10 + *p - '0';
  }
  while (p && *p && (*p == '=' || *p == ',')) {
    char *name;
    int len = 0;
    int index = 0;

    for (len = 0, p++, name = p ; p && *p && *p != '/' ; p++, len++) {}
    if (p && *p && *p == '/') {
      int i, found;
      for (p++ ; p && *p && *p != ',' ; p++) {
	index = index * 10 + *p - '0';
      }
      for (found = 0, i = 0 ; i < numInstances ; i++) {
	if (strncmp(instance[i]->actorClass->name, name, len) == 0 &&
	    len == strlen(instance[i]->actorClass->name) &&
	    instance[i]->index == index) {
	  instance[i]->affinity = affinity;
	  found = 1;
	  break;
	}
      }
      if (! found) {
	result = 1;
      }
    }
  }

  return result;
}

static int check_network(ActorInstance_1_t **instance,
			 int numInstances,
			 cpu_set_t *used_cpus,
			 int *flags)
{
  int result = 0;
  int i;
  cpu_set_t cpu_set;
  
  CPU_ZERO(&cpu_set);
  for (i = 0 ; i < numInstances ; i++) {
    // Check that we have processor affinity, else force single CPU mode
    if (instance[i]->affinity == -1) {
      if (!(*flags & FLAG_SINGLE_CPU)) {
	printf("Forcing single CPU mode!\n");
	*flags |= FLAG_SINGLE_CPU;
      }
      printf("No affinity %s/%d\n", instance[i]->actorClass->name,
	     instance[i]->index);
      instance[i]->affinity = 0;
    }
  }
  for (i = 0 ; i < numInstances ; i++) {
    int j;
    
    if ((*flags & FLAG_SINGLE_CPU)) {
      instance[i]->affinity = 0;
    }
    CPU_SET(instance[i]->affinity, &cpu_set);
 
    // Check that everything has been connected
    for (j = 0 ; j < instance[i]->actorClass->numOutputPorts ; j++) {
      if (instance[i] != instance[i]->output[j].owner) {
	printf("Wrong owner\n");
	result = 1;
      }
      if (instance[i]->output[j].created != 1) {
	printf("Not created once %d\n", instance[i]->output[j].created);
	result = 1;
      }
    }
    for (j = 0 ; j < instance[i]->actorClass->numInputPorts ; j++) {
      if (instance[i]->input[j].created != 1) {
	printf("Not created once %d\n", instance[i]->input[j].created);
	result = 1;
      }
      if (instance[i] != instance[i]->input[j].owner) {
	printf("Wrong owner\n");
	result = 1;
      }
      if (instance[i]->input[j].output == NULL) {
	printf("Not connected\n");
	result = 1;
      }
    }

  }
  {
    // Check that all needed cpus are present
    cpu_set_t old;
    int err;

    err = sched_getaffinity(0, sizeof(old), &old);
    if (err == 0) {
      int i, n_cpu;
      
      for (n_cpu = 0, i = 0 ; i < CPU_SETSIZE ; i++) {
	if (CPU_ISSET(i, &cpu_set)) {
	  cpu_set_t new;
	  
	  n_cpu++;
	  CPU_ZERO(&new);
	  CPU_SET(i, &new);
	  err = sched_setaffinity(0, sizeof(new), &new);
	  if (err) {
	    printf("System does not have a processor #%d\n", i);
	    result = 1;
	  }
	}
      }
      err = sched_setaffinity(0, sizeof(old), &old);
    }
  }
  if (result == 0 && used_cpus) {
    *used_cpus = cpu_set;
  }
  return result;
}

static void info_network(ActorInstance_1_t **instance,
			 int numInstances,
			 cpu_set_t *used_cpus)
{
  int i;

  printf("Need %d cpus\n", nr_of_cpus(used_cpus));
  for (i = 0 ; i < CPU_SETSIZE ; i++) {
    int j, reported;

    for (reported = 0, j = 0 ; j < numInstances ; j++) {
      if (instance[j]->affinity == i) {
	reported++;
	if (reported == 1) {
	  printf("--affinity%d=%s/%d", 
		 i, instance[j]->actorClass->name, instance[j]->index);
	} else {
	  printf(",%s/%d", 
		 instance[j]->actorClass->name, instance[j]->index);
	}
      }
    }
    if (reported) {
      printf("\n");
    }
  }
}

static void *cache_aligned_calloc(size_t size)
{
  void *result; 
  
  if (posix_memalign(&result, CACHE_LINE_SIZE, size) != 0) {
    runtimeError(NULL, "Failed to align %d bytes\n", size);
  }
  memset(result, 0, size);
  return result;
}

static int cache_bytes(int n)
{
  return ((n + CACHE_LINE_SIZE - 1) / CACHE_LINE_SIZE) * CACHE_LINE_SIZE;
}

static cpu_runtime_data_t *allocate_network(
  ActorInstance_1_t **instance, 
  int numInstances,
  cpu_set_t *used_cpus,
  int fifo_size)
{
  cpu_runtime_data_t *result;
  int num_outputs, num_inputs, buffer_bytes, index;
  int cpu, shared_bytes, local_bytes, actor_bytes, global_bytes;
  void *buffer_p, *global_p, *shared_p, *local_p;
  InputPort *input_p, **reader_p;
  OutputPort *output_p;
  int i, j, k;

  /* Set fifo capacity */
  for (i = 0 ; i < numInstances ; i++) {
    for (j = 0 ; j < instance[i]->actorClass->numInputPorts ; j++) {
      if (instance[i]->input[j].capacity == 0) {
	instance[i]->input[j].capacity = fifo_size / 
	  instance[i]->actorClass->inputPortDescriptions[j].tokenSize;
      }
      if (instance[i]->input[j].output->capacity < 
	  instance[i]->input[j].capacity) {
	instance[i]->input[j].output->capacity =
	  instance[i]->input[j].capacity;
      }
    }
  }
      
  /* Count number of inputs and outputs and needed buffer space */
  num_outputs = 0;
  num_inputs = 0;
  buffer_bytes = 0;
  actor_bytes = 0;
  for (i = 0 ; i < numInstances ; i++) {
    actor_bytes += instance[i]->actorClass->sizeActorInstance;
    num_outputs += instance[i]->actorClass->numOutputPorts;
    num_inputs += instance[i]->actorClass->numInputPorts;
    for (j = 0 ; j < instance[i]->actorClass->numOutputPorts ; j++) {
      instance[i]->output[j].buffer_bytes = cache_bytes(
	instance[i]->output[j].capacity * 
	instance[i]->actorClass->outputPortDescriptions[j].tokenSize);
      buffer_bytes += instance[i]->output[j].buffer_bytes;
    }
  }
  /* Global data: (semi-)constant (may only be changed in such a way that
   *              all cpus get a coherent view)
   * Local data:  only used by a single cpu
   * Shared data: written by one cpu, read by many, used to control 
   *              buffer access
   * Buffer data: the actual buffers containing data, written by one thread,
   *              read by many.
   * Access to buffer/shared data has to use barriers properly.
   */
  global_bytes = cache_bytes(
    num_outputs * sizeof(OutputPort) /* actor.output  */ +
    num_inputs * sizeof(InputPort)   /* actor.input   */ +
    num_inputs * sizeof(InputPort*)  /* output.reader */);
  local_bytes = cache_bytes(
    (num_outputs + num_inputs) * sizeof(LocalContext) +
    numInstances * sizeof(AbstractActorInstance *) + 
    actor_bytes +
    nr_of_cpus(used_cpus) * sizeof(*result[0].has_affected));
  shared_bytes = 
    cache_bytes(sizeof(*result[0].sleep))+
    cache_bytes((num_outputs + num_inputs) * sizeof(SharedContext));
  printf("n_out=%d n_in=%d global=%d local=%d shared=%d buf=%d\n",
	 num_outputs,
	 num_inputs,
	 global_bytes,
	 local_bytes * nr_of_cpus(used_cpus),
	 shared_bytes * nr_of_cpus(used_cpus),
	 buffer_bytes);
  result = malloc(sizeof(*result) * nr_of_cpus(used_cpus));
  buffer_p = cache_aligned_calloc(
    buffer_bytes +
    global_bytes +
    shared_bytes * nr_of_cpus(used_cpus) + 
    local_bytes * nr_of_cpus(used_cpus));
  global_p = buffer_p + buffer_bytes;
  shared_p = global_p + global_bytes;
  local_p = shared_p + shared_bytes * nr_of_cpus(used_cpus);

  output_p = global_p;
  global_p += num_outputs * sizeof(OutputPort);
  input_p = global_p;
  global_p += num_inputs * sizeof(InputPort);
  reader_p = global_p;
  global_p += num_inputs * sizeof(InputPort*);

  /* Assign ports and output buffers, and assign indices into local and
   * shared data structures. 
   * NOTE: the result will be sparse, since all cpus share index space. This
   *       will give a higher number of used cache-lines, but will simplify
   *       actor migration (if this ever will be implemented).
   */ 
  index = 0;
  for (i = 0 ; i < numInstances ; i++) {
    if (instance[i]->actorClass->numOutputPorts) {
      instance[i]->output_list = output_p;
    }
    if (instance[i]->actorClass->numInputPorts) {
      instance[i]->input_list = input_p;
    }
    for (j = 0 ; j < instance[i]->actorClass->numOutputPorts ; j++) {
      OutputPort *output = output_p++;
      
      instance[i]->output[j].output = output;
      output->index = index++;
      /*
       * buffer points to end of buffer to avoid compare with capacity on
       * each erad/write (compare with 0 assumed cheap) 
       */
      buffer_p += instance[i]->output[j].buffer_bytes;
      output->buffer = buffer_p;
      output->capacity = instance[i]->output[j].capacity;
      output->readers = 0;
      output->reader = reader_p;
      reader_p += instance[i]->output[j].numberOfReaders;
    }
    for (j = 0 ; j < instance[i]->actorClass->numInputPorts ; j++) {
      InputPort *input = input_p++;

      instance[i]->input[j].input = input;
      input->index = index++;
      input->capacity = instance[i]->input[j].capacity;
    }
  }
  /* Connect the newly assigned ports */
  for (i = 0 ; i < numInstances ; i++) {
    for (j = 0 ; j < instance[i]->actorClass->numInputPorts ; j++) {
      InputPort *input = instance[i]->input[j].input;
      OutputPort *output = instance[i]->input[j].output->output;

      output->reader[output->readers] = input;
      output->readers++;
      input->writer = output;
      input->buffer = output->buffer;
    }
  }

  /* Setup CPU local data */
  for (cpu = -1, i = 0 ; i < CPU_SETSIZE ; i++) {
    if (CPU_ISSET(i, used_cpus)) {
      void *cpu_shared_p, *cpu_local_p, *cpu_actor_data;

      cpu++;
      printf("Allocating for cpu #%d (%d)\n", i, cpu);

      /* Get cpu data */
      cpu_shared_p = shared_p;
      shared_p += shared_bytes;
      cpu_local_p = local_p;
      local_p += local_bytes;
	
      result[cpu].cpu = result;
      result[cpu].cpu_count = nr_of_cpus(used_cpus);
      result[cpu].cpu_index = cpu;
      result[cpu].physical_id = i;
      result[cpu].sem = malloc(sizeof(*result[cpu].sem));
      sem_init(result[cpu].sem, 0, 0); 

      // Data accessed from multiple cpus
      result[cpu].sleep = cpu_shared_p;
      cpu_shared_p += cache_bytes(sizeof(*result[cpu].sleep));
      result[cpu].shared = cpu_shared_p;

      // Data accessed from this cpu only
      result[cpu].local = cpu_local_p;
      cpu_local_p += (num_outputs + num_inputs) * sizeof(LocalContext);
      result[cpu].actors = 0;
      result[cpu].actor = cpu_local_p;
      cpu_local_p += numInstances * sizeof(AbstractActorInstance *);
      cpu_actor_data = cpu_local_p;
      cpu_local_p += actor_bytes;
      result[cpu].actor_data = cpu_actor_data;
      result[cpu].has_affected = cpu_local_p;
      cpu_local_p += (nr_of_cpus(used_cpus) * sizeof(*result[0].has_affected));

      for (j = 0 ; j < numInstances ; j++) {
	if (instance[j]->affinity == result[cpu].physical_id) {
	  AbstractActorInstance *actor;
	  char buf[1024];

	  actor = cpu_actor_data;
	  result[cpu].actor[result[cpu].actors] = cpu_actor_data;
	  result[cpu].actors++;
	  cpu_actor_data += instance[j]->actorClass->sizeActorInstance;

	  actor->actor = instance[j]->actorClass;
	  sprintf(buf, "%s/%d", 
		  instance[j]->actorClass->name,
		  instance[j]->index);
	  actor->name = strdup(buf);
	  actor->cpu_index = cpu;
	  actor->outputs = instance[j]->actorClass->numOutputPorts;
	  actor->output = instance[j]->output_list;
	  actor->inputs = instance[j]->actorClass->numInputPorts;
	  actor->input = instance[j]->input_list;
	  for (k = 0 ; k < actor->outputs ; k++) {
	    OutputPort *output = &actor->output[k];

	    output->cpu = cpu;
	    output->actor = actor;
	    output->shared = &result[cpu].shared[output->index];
	    output->local = &result[cpu].local[output->index];
	    output->local->pos = -(output->capacity);
	  }
	  for (k = 0 ; k < actor->inputs ; k++) {
	    InputPort *input = &actor->input[k];

	    input->cpu = cpu;
	    input->actor = actor;
	    input->shared = &result[cpu].shared[input->index];
	    input->local = &result[cpu].local[input->index];
	    input->local->pos = -(input->capacity);
	  }
	  if (actor->actor->set_param) {
	    struct Parameter_1 *p;

	    for (p = instance[j]->parameter ; p ; p = p->next) {
//	      printf("Call set_param[%d, %p, %s %s]...\n", j, actor, p->key, p->value);
	      actor->actor->set_param(actor, p->key, p->value);
	    }
	  }
	  if (actor->actor->constructor) {
//	    printf("Call constructor[%d, %p]...\n", j, actor);
	    actor->actor->constructor(actor);
	  }
	}
      }
    }
  }
  return result;
}

static void run_destructors(cpu_runtime_data_t *runtime)
{
  int i, j;

  for (i = 0 ; i < runtime[0].cpu_count ; i++) {
    for (j = 0 ; j < runtime[i].actors ; j++) {
      AbstractActorInstance *actor = runtime[i].actor[j];

      if (actor->actor->destructor) {
	actor->actor->destructor(actor);
      }
    }
  }
  
  
}

static void deallocate_network(cpu_runtime_data_t *runtime)
{
  int i, j;

  for (i = 0 ; i < runtime[0].cpu_count ; i++) {
    for (j = 0 ; j < runtime[i].actors ; j++) {
    }
  }
  
  
}

static void *run_with_affinity(void *arg) 
{
  cpu_runtime_data_t *cpu = arg;
  cpu_set_t affinity;
  
  CPU_ZERO(&affinity);
  CPU_SET(cpu->physical_id, &affinity);
  sched_setaffinity(0, sizeof(cpu_set_t), &affinity);

  return cpu->main(cpu, arg_loopmax);
}

static void run_threads(cpu_runtime_data_t *runtime,
			void *(execute)(cpu_runtime_data_t *, int))
{
  int i;

  for (i = 0 ; i < runtime->cpu_count ; i++) {
    runtime[i].main = execute;
    pthread_create(&runtime[i].thread,
		   NULL,
		   run_with_affinity,
		   &runtime[i]);
  }
  for (i = 0 ; i < runtime->cpu_count ; i++) {
    pthread_join(runtime[i].thread, NULL);
  }
}

static void show_result(cpu_runtime_data_t *cpu)
{
  int i;

  printf("### Statistics ###\n");
  
  for (i = 0 ; i < cpu->cpu_count ; i++) {
    int j;

    printf("%d prefire:        %Lu\n", i, cpu[i].statistics.prefire);
    printf("%d read_barrier:   %Lu\n", i, cpu[i].statistics.read_barrier);
    printf("%d fire:           %Lu\n", i, cpu[i].statistics.fire);
    printf("%d write_barrier:  %Lu\n", i, cpu[i].statistics.write_barrier);
    printf("%d postfire:       %Lu\n", i, cpu[i].statistics.postfire);
    printf("%d sync_unblocked: %Lu\n", i, cpu[i].statistics.sync_unblocked);
    printf("%d sync_blocked:   %Lu\n", i, cpu[i].statistics.sync_blocked);
    printf("%d sync_sleep:     %Lu\n", i, cpu[i].statistics.sync_sleep);
    printf("%d total:          %Lu\n", i, cpu[i].statistics.total);
    printf("%d nsleep:         %Lu\n", i, cpu[i].statistics.nsleep);
    printf("%d nloops:         %Lu\n", i, cpu[i].statistics.nloops);
    for (j = 0 ; j < cpu[i].actors ; j++) { 
      printf("actor[%d,%d] name,nloops,total: %s,%Ld,%Lu\n", i, j, 
	     cpu[i].actor[j]->name,
	     cpu[i].actor[j]->nloops,
	     cpu[i].actor[j]->total);
    }
    printf("\n");
      
  }
  
}

int executeNetwork(int argc, 
		   char *argv[],
		   AbstractActorInstance **instance, 
		   int numInstances)
{
  int result = 0;
  cpu_set_t used_cpus;
  ActorInstance_1_t **instance_1;
  instance_1 = (ActorInstance_1_t**)instance;
  int i;
  int flags = 0;
  cpu_runtime_data_t *runtime_data;
  int arg_print_info = 0;
  int arg_fifo_size = DEFAULT_FIFO_LENGTH;
  
  for (i = 1 ; i < argc ; i++) {
    if (strcmp(argv[i], "--timing") == 0) {
      flags |= FLAG_TIMING;
    } else if (strcmp(argv[i], "--single_cpu") == 0) {
      flags |= FLAG_SINGLE_CPU;
    } else if (strncmp(argv[i], "--affinity", 10) == 0) {
    } else if (strcmp(argv[i], "--info") == 0) {
      arg_print_info = 1;
    } else if (strncmp(argv[i], "--fifosize=", 11) == 0) {
      arg_fifo_size = atoi(&argv[i][11]);
    } else if (strncmp(argv[i], "--loopmax=", 10) == 0) {
      arg_loopmax = atoi(&argv[i][10]);
      printf("FIFO=%d\n", arg_fifo_size);
    } else if (strncmp(argv[i], "-", 1) == 0) {
      printf("Invalid flag '%s'\n", argv[i]);
      exit(1);
    }
  }
  result = index_nodes(instance_1, numInstances); 
  if (result == 0) {
    // Assign command line affinity
    for (i = 1 ; i < argc ; i++) {
      if (strncmp(argv[i], "--affinity", 10) == 0) {
	set_affinity(instance_1, numInstances, &argv[i][10]);
      }
    }
  }
  if (result == 0) {
    result = check_network(instance_1, numInstances, &used_cpus, &flags);
  }
  if (arg_print_info) {
    info_network(instance_1, numInstances, &used_cpus);
  }
  if (result == 0) {
    runtime_data = allocate_network(instance_1, numInstances, &used_cpus,
				    arg_fifo_size);
  }
  if (result == 0) {
    if (nr_of_cpus(&used_cpus) == 1) {
      flags |= FLAG_SINGLE_CPU;
    }
    printf("Flags: %d\n", flags);
    switch (flags) {
      case 0: {
	run_threads(runtime_data, multi_cpu_execute_network);
      } break;
      case FLAG_SINGLE_CPU: {
	single_cpu_execute_network(runtime_data, arg_loopmax);
      } break;
      case FLAG_TIMING: {
	run_threads(runtime_data, multi_cpu_timed_execute_network);
      } break;
      case FLAG_TIMING | FLAG_SINGLE_CPU: {
	single_cpu_timed_execute_network(runtime_data, arg_loopmax);
      } break;
    }
  }
  if (result == 0) {
    run_destructors(runtime_data);
  }
  if (result == 0) {
    show_result(runtime_data);
  }
  if (result == 0) {
    deallocate_network(runtime_data);
  }
  exit(result);
}

/*

x --affinity0=a1/0,a1/1 --affinity1=a2/0

*/
