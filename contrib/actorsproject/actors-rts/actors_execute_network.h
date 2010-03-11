#ifdef TIMING_PROBES

#define DECLARE_TIMEBASE(t) time_base_t t;
#define CLEAR_TIMER(timer) *timer = 0;
#define INIT_TIMEBASE(tb) *tb = READ_TIMER();
#define ADD_TIMER(timer, tb) add_timer(timer, tb);

#else

#define DECLARE_TIMEBASE(t) 
#define CLEAR_TIMER(timer) *timer = 0; 
#define INIT_TIMEBASE(tb) 
#define ADD_TIMER(timer, tb) 

#endif

static void *EXECUTE_NETWORK(cpu_runtime_data_t *runtime,
			     int loopmax)
{
  int i, j, k, fired;
  int *old_sleep;
  AbstractActorInstance **actor = runtime->actor;
  int actors = runtime->actors;
  cpu_runtime_data_t *cpu = runtime->cpu;
  int this_cpu = runtime->cpu_index;
  int this_balance;
  DECLARE_TIMEBASE(t1);
  DECLARE_TIMEBASE(t2);
  DECLARE_TIMEBASE(t3);
  statistics_t statistics;

#ifdef RM
  register_thread_id();
#endif

//  printf("START#%d %s %s %p\n", this_cpu, __DATE__, __TIME__, runtime);
  old_sleep = malloc(runtime->cpu_count * sizeof(*old_sleep));
  for (i  = 0 ; i < runtime->cpu_count ; i++) {
    old_sleep[i] = 0;
  }

  this_balance = 0;
  cpu[this_cpu].starved = 0;
  CLEAR_TIMER(&statistics.prefire);
  CLEAR_TIMER(&statistics.read_barrier);
  CLEAR_TIMER(&statistics.fire);
  CLEAR_TIMER(&statistics.write_barrier);
  CLEAR_TIMER(&statistics.postfire);
  CLEAR_TIMER(&statistics.sync_unblocked);
  CLEAR_TIMER(&statistics.sync_blocked);
  CLEAR_TIMER(&statistics.sync_sleep);
  CLEAR_TIMER(&statistics.total);
  statistics.nsleep = 0;
  statistics.nloops = 0;
  INIT_TIMEBASE(&t1);
  INIT_TIMEBASE(&t2);
  while (1) {
    statistics.nloops++;
    // Determine how much data can be read/written and cache the values
    // since we need to use read/write barriers
    for (i = 0 ; i < actors ; i++) {
      actor[i]->fireable = 0;
      actor[i]->fired = 0;
      if (!actor[i]->terminated) {
	for (j = 0 ; j < actor[i]->inputs ; j++) {
	  int available = (
	    atomic_get(&actor[i]->input[j].writer->shared->count) -
	    atomic_get(&actor[i]->input[j].shared->count));
	  actor[i]->input[j].local->available = available;
	  actor[i]->input[j].local->count = 0;
	  if (available) { 
	    actor[i]->fireable = 1; 
	  }
	}
	for (j = 0 ; j < actor[i]->outputs ; j++) {
	  int max_unconsumed = 0;
	  int available;
	  for (k = 0 ; k < actor[i]->output[j].readers ; k++) {
	    int unconsumed = (
	      atomic_get(&actor[i]->output[j].shared->count) -
	      atomic_get(&actor[i]->output[j].reader[k]->shared->count));
	    if (unconsumed > max_unconsumed) { max_unconsumed = unconsumed; }
	  }
	  available = actor[i]->output[j].capacity - max_unconsumed;
	  actor[i]->output[j].local->available = available;
	  actor[i]->output[j].local->count = 0;
	  if (available) { 
	    actor[i]->fireable = 1;	
	  }
	}
      }
    }
    ADD_TIMER(&statistics.prefire, &t1);
    // Make all buffer writes that has happened up to the values read
    // above visible on this cpu
    READ_BARRIER(); 
    ADD_TIMER(&statistics.read_barrier, &t1);
    for (i = 0 ; i < actors ; i++) {
      if (actor[i]->fireable) {
	const int *result;
	INIT_TIMEBASE(&t3);
	result = actor[i]->actor->action_scheduler(actor[i], loopmax);
	ADD_TIMER(&actor[i]->total, &t3);
	actor[i]->nloops++;
	if (result == EXITCODE_TERMINATE) {
	  actor[i]->terminated = 1;
	}
      }
    }
    ADD_TIMER(&statistics.fire, &t1);
    // Make all buffer writes that has happened above visible
    // on other cpus before we make the changes visible
    WRITE_BARRIER();
    ADD_TIMER(&statistics.write_barrier, &t1);
    fired = 0;
    for (i = 0 ; i < actors ; i++) {
      // Tell other cpus how much data has been read/written
      if (actor[i]->fired) {
	// Only fired actors have changed
	fired = 1;

	for (j = 0 ; j < actor[i]->inputs ; j++) {
	  InputPort *input = &actor[i]->input[j];
	  int count = input->local->count;

	  if (count) {
	    int wcpu = input->writer->cpu;

	    atomic_set(&input->shared->count, 
		       atomic_get(&input->shared->count) + count);
	    cpu[this_cpu].has_affected[wcpu] = 1;
	    this_balance -= count;
	  }
	}
	for (j = 0 ; j < actor[i]->outputs ; j++) {
	  OutputPort *output = &actor[i]->output[j];
	  int count = output->local->count;

	  if (count) {
	    atomic_set(&output->shared->count,
		       atomic_get(&output->shared->count) + count);
	    for (k = 0 ; k < output->readers ; k++) {
	      cpu[this_cpu].has_affected[output->reader[k]->cpu] = 1;
	      this_balance += count;
	    }
	  }
	}
      }
    }
    ADD_TIMER(&statistics.postfire, &t1);

    if (this_balance > 2000000000 || this_balance < -2000000000) {
      MUTEX_LOCK();
      balance += this_balance;
      this_balance = 0;
      MUTEX_UNLOCK();
    }
    
    if (fired) {  
      if (cpu[this_cpu].starved != 0) {
	cpu[this_cpu].starved = 0;
      }

      // Check if we should wake up someone
      for (i = 0 ; i < runtime->cpu_count ; i++) { 
	if (i != this_cpu) {
	  int current_sleep = *cpu[i].sleep;

	  if (current_sleep != old_sleep[i]) {
	    if ((current_sleep & 1) == 0) {
	      // Thread is active, no need to wake it
	      old_sleep[i] = current_sleep;
	    } else {
	      if (cpu[this_cpu].has_affected[i]) {
		// This assumes that sem_post does not reach the waiting 
		// cpu before all the updates from above are visible.
		// Do we need an extra write_barrier, and how can we test it?
		sem_post(cpu[i].sem);
		// Only wake it once for each sleep
		cpu[this_cpu].has_affected[i] = 0;
		old_sleep[i] = current_sleep;
#ifdef TRACE
    xmlTraceWakeup(cpu[this_cpu].file,i);
#endif
	      }
	    }
	  }
	}
      }
      ADD_TIMER(&statistics.sync_unblocked, &t1);
    } else {
      // No fired actors found, prepare to sleep
      int sleep = 1;

      MUTEX_LOCK();
      // Possible scenarios:
      //   1. Active CPU connected to this CPU that will eventually 
      //      generate data for us. At some point in the future
      //      that CPU will find that this CPU is sleeping, 
      //      either in the active loop wakeup or at point 3 below.
      //
      //   2. CPU about to go to sleep (blocking on the mutex we hold) 
      //      but has yet unseen communication with this CPU will wake 
      //      this CPU when they enter this part (i.e. their #3).
      //
      //   3. We have data for sleeping CPU, we will have to wake them up.
      //
      //   4. We have got data from a sleeping CPU, we should not 
      //      go to sleep.
      //
      sleepers++;
      balance += this_balance;
      this_balance = 0;
      if (sleepers == runtime->cpu_count && balance == 0) { 
	// Only terminate when all threads are starved
	int do_terminate = 1;
	for (i = 0 ; i < runtime->cpu_count ; i++) { 
	  if (cpu[i].starved == 0) {
	    sem_post(cpu[i].sem);
	    do_terminate = 0;
	  }
	}
	if (do_terminate) {
	  terminate = 1; 
	}
      } else {
	// Check if some sleepers need to be awoken
	for (i = 0 ; i < runtime->cpu_count ; i++) { 
	  if (i == this_cpu) {
	    // Should not need to consider ourself
	  } else {
	    int current_sleep = *cpu[i].sleep;

	    if (current_sleep != old_sleep[i]) {
	      if ((current_sleep & 1) == 0) {
		// Thread is active, no need to wake it
		old_sleep[i] = current_sleep;
	      } else {	  
		if (cpu[this_cpu].has_affected[i]) {
		  // We have data for some sleeping thread
		  sem_post(cpu[i].sem);
		  // Only wake it once for each sleep
		  cpu[this_cpu].has_affected[i] = 0;
		  old_sleep[i] = current_sleep;
		}
		if (cpu[i].has_affected[this_cpu]) {
		  // Sleeping thread has data for us, since thread is
		  // sleeping and we hold mutex, we can safely access 
		  // their state to indicate that we have seen 
		  // their data
		  cpu[i].has_affected[this_cpu] = 0;
		  sleep = 0;
		}
	      }
	    }
	  }
	}
      }
      if (sleep) {
	// This will be entered, unless some sleeping thread generated
	// data for us while we were preparing to sleep.
	(*cpu[this_cpu].sleep)++;
	MUTEX_UNLOCK();
	ADD_TIMER(&statistics.sync_blocked, &t1);

	if (terminate) { goto done; }
#ifdef TRACE
   xmlTraceStatus(cpu[this_cpu].file,0);
#endif
	sem_wait(cpu[this_cpu].sem);
	statistics.nsleep++;
	
	if (terminate) { goto done; }
	
	ADD_TIMER(&statistics.sync_sleep, &t1);
	MUTEX_LOCK();
	(*cpu[this_cpu].sleep)++;
      }
#ifdef TRACE
      xmlTraceStatus(cpu[this_cpu].file,1);
#endif
      sleepers--;
      cpu[this_cpu].starved = 1;
      MUTEX_UNLOCK();
      while (sem_trywait(cpu[this_cpu].sem) == 0) { 
	// Consume all active activations (might have been awakened 
	// by more than one thread), but as far away as possible from 
	// the wait to catch as many wakeups as possible.
	// We assume that a sem_wait/sem_trywait guarantees that all
	// data from the thread that did the sem_signal has reached us.
      }
      ADD_TIMER(&statistics.sync_blocked, &t1);
    }
  }
done:
  ADD_TIMER(&statistics.total, &t2);
  
  // All actors are starved, terminate gracefully  
  for (i = 0 ; i < runtime->cpu_count ; i++) { 
    if (this_cpu == i) {
      continue;
    }
    sem_post(cpu[i].sem);
  }
  cpu[this_cpu].statistics = statistics;

  return NULL;
}

#undef CLEAR_TIMER
#undef DECLARE_TIMEBASE
#undef INIT_TIMEBASE
#undef ADD_TIMER


