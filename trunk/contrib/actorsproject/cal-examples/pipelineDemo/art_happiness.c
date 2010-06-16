/*
 * Actor happiness (ActorClass_art_happiness)
 */

#include <stdio.h>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/time.h>
#include <fcntl.h>
#include <semaphore.h>
#include "actors-rts.h"

#define OUT0_Out ART_OUTPUT(0)
#define OUT1_Out ART_OUTPUT(1)

#define ACTION_NULL         0
#define ACTION_HAPPY        1
#define ACTION_NOT_HAPPY    2
#define ACTION_QUIT         3			

static int             action;
static pthread_mutex_t actionMutex;

typedef struct {
  AbstractActorInstance base;
} ActorInstance_happiness_0;

typedef struct cpu_runtime_data {
  struct cpu_runtime_data *cpu; /* Pointer to first element in this list */
  int cpu_count;
  int cpu_index;
  void *(*main)(struct cpu_runtime_data *, int);
  pthread_t thread;
  int physical_id; /* physical index of this cpu */
  sem_t *sem;
  int *sleep; // Odd value indicates thread sleeping
} cpu_runtime_data_t;

ART_ACTION_CONTEXT(0,2)

ART_ACTION(happiness_yes,ActorInstance_happiness_0);
ART_ACTION(happiness_no,ActorInstance_happiness_0);
ART_ACTION(happiness_quit,ActorInstance_happiness_0);
ART_ACTION_SCHEDULER(happiness_0_action_scheduler);

static void happiness_0_constructor(AbstractActorInstance*);
static void happiness_0_destructor(AbstractActorInstance*);

static const PortDescription outputPortDescriptions[]={
  {"Out",  sizeof(int32_t)},
  {"Quit", sizeof(int32_t)},
};

static const int portRate_1[] = {
  1
};

static const ActionDescription actionDescriptions[] = {
  {"happiness_yes",    0, portRate_1},
  {"happiness_no",     0, portRate_1},
  {"happiness_quit",   0, portRate_1}
};

ActorClass ActorClass_art_happiness = INIT_ActorClass(
  "happiness",
  ActorInstance_happiness_0,
  happiness_0_constructor,
  0, /* no setParam */
  happiness_0_action_scheduler,
  happiness_0_destructor,
  //0, /* no destructor */
  0, 0,
  2, outputPortDescriptions,
  3, actionDescriptions
);

ART_ACTION(happiness_quit,ActorInstance_happiness_0) {
  ART_ACTION_ENTER(happiness_quit,1);
  pinWrite_int32_t(OUT1_Out, 1);
  ART_ACTION_EXIT(happiness_quit,1);
}

ART_ACTION(happiness_yes,ActorInstance_happiness_0) {
  ART_ACTION_ENTER(happiness_yes,1);
  pinWrite_int32_t(OUT0_Out, 1);
  ART_ACTION_EXIT(happiness_yes,1);
}

ART_ACTION(happiness_no,ActorInstance_happiness_0) {
  ART_ACTION_ENTER(happiness_no,1);
  pinWrite_int32_t(OUT0_Out, 0);
  ART_ACTION_EXIT(happiness_no,1);
}

static const int exitcode_block_Out_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};

ART_ACTION_SCHEDULER(happiness_0_action_scheduler) {
  ActorInstance_happiness_0 *thisActor=(ActorInstance_happiness_0*) pBase;
  const int *exitCode=EXIT_CODE_YIELD;
  int32_t         t;
  int             state;
  ART_ACTION_SCHEDULER_ENTER(0,2);
  ART_ACTION_SCHEDULER_LOOP {
    ART_ACTION_SCHEDULER_LOOP_TOP;

    pthread_mutex_lock(&actionMutex);
    state = action;
	action = ACTION_NULL;
    pthread_mutex_unlock(&actionMutex);

    switch(state) {
      case ACTION_NULL:      //no action 
        break;
      case ACTION_HAPPY:     //happy
        t=pinAvailOut_int32_t(OUT0_Out);
        if ((t>=(1)))
          ART_FIRE_ACTION(happiness_yes);
        break;
      case ACTION_NOT_HAPPY: //not happy
        t=pinAvailOut_int32_t(OUT0_Out);
        if ((t>=(1)))
          ART_FIRE_ACTION(happiness_no);
        break;
      case ACTION_QUIT:      //quit
        t=pinAvailOut_int32_t(OUT1_Out);
        if(t>=1)
          ART_FIRE_ACTION(happiness_quit);
        exitCode=EXITCODE_TERMINATE; goto action_scheduler_exit;
      default:
        break;
    }
	exitCode=exitcode_block_Out_1; goto action_scheduler_exit;

    ART_ACTION_SCHEDULER_LOOP_BOTTOM;
  }
  action_scheduler_exit:
  ART_ACTION_SCHEDULER_EXIT(0,2);

  return exitCode;
}

static void wakeup_me(void *instance)
{
  if(instance){
    ActorInstance_happiness_0 *thisActor=(ActorInstance_happiness_0*)instance;
	cpu_runtime_data_t *cpu=(cpu_runtime_data_t *)thisActor->base.cpu;

	// wake me up if I'm sleeping
	if(*cpu->sleep&1)
      sem_post(cpu->sem);		
  }
}

static int kbhit(void)
{
	struct termios oldt, newt;
	int ch;
	int oldf;

	tcgetattr(STDIN_FILENO, &oldt);
	newt = oldt;
	newt.c_lflag &= ~(ICANON | ECHO);
	tcsetattr(STDIN_FILENO, TCSANOW, &newt);
	oldf = fcntl(STDIN_FILENO, F_GETFL, 0);
	fcntl(STDIN_FILENO, F_SETFL, oldf | O_NONBLOCK);

	ch = getchar();
    tcsetattr(STDIN_FILENO, TCSANOW, &oldt);
	fcntl(STDIN_FILENO, F_SETFL, oldf);

	if(ch != EOF)
	{
		ungetc(ch, stdin);
		return 1;
	}
	return 0;
}

static void myFlush()
{
  int ch=0;
  while(kbhit())
	getchar();
}

void *keyboardProc(void *instance)
{
  char ch=0;
  int  key_input;

  while(ch!='q')
  {
    if(ch==0xa)
      printf("Are you happy? ");

    ch = getchar();
	
	//flush the rest of the inputs
	myFlush();

    if(ch=='y')
      key_input = ACTION_HAPPY;
    else if(ch=='n')
      key_input = ACTION_NOT_HAPPY;
    else if(ch=='q')
      key_input = ACTION_QUIT;
    else
      key_input = ACTION_NULL;

    if(key_input >= ACTION_HAPPY && key_input <= ACTION_QUIT)
    {
      pthread_mutex_lock(&actionMutex);
      action = key_input;
      pthread_mutex_unlock(&actionMutex);

      //wakeup me in case sleeping
      wakeup_me(instance);
    }
  }
  pthread_exit(NULL);
}

static void happiness_0_constructor(AbstractActorInstance *pBase) {
  ActorInstance_happiness_0 *thisActor=(ActorInstance_happiness_0*) pBase;
  pthread_t thread;
  int rc=0;
  rc = pthread_create(&thread,NULL,keyboardProc,(void*)thisActor);
  if(rc)
  {
    printf("ERROR; return code from pthread_create() is %d\n", rc);
    exit(-1);
  }
}
static void happiness_0_destructor(AbstractActorInstance *pBase) {
   pthread_mutex_destroy(&actionMutex);
}
