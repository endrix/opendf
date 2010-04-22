#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include "actors-rts.h"

typedef struct {
  AbstractActorInstance base;
  long consumed;
  long N;
  long N0;
  int chunk_max;
} ActorInstance_art_consumer;

static const int exitcode_block_Out_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};

ART_ACTION_CONTEXT(1, 0);

ART_ACTION(action1,  ActorInstance_art_consumer)
{
  int32_t tmp;
  ART_ACTION_ENTER(action1, 0);
  tmp = pinRead_int32_t(ART_INPUT(0));
  if (tmp != (int)thisActor->consumed) {
    printf("OOPS: %d %ld\n", tmp, thisActor->consumed);
    exit(1);
  }
  thisActor->consumed++;
  ART_ACTION_EXIT(action1, 0);
} 

ART_ACTION_SCHEDULER(art_consumer_action_scheduler)
{
  int i, n;
  ActorInstance_art_consumer *thisActor=(ActorInstance_art_consumer*) pBase;
  const int *result = EXIT_CODE_YIELD;
  ART_ACTION_SCHEDULER_ENTER(1, 0)

  n = pinAvailIn_int32_t(ART_INPUT(0));
  thisActor->N++;
  if (n == 0) { thisActor->N0++; }
  i = 0;
  ART_ACTION_SCHEDULER_LOOP {
    ART_ACTION_SCHEDULER_LOOP_TOP;
    if (i < n) {
      i++;
      ART_FIRE_ACTION(action1);
    } else {
      result = exitcode_block_Out_1; 
      goto out;
    }
    ART_ACTION_SCHEDULER_LOOP_BOTTOM;
  }
out:
  if (i > thisActor->chunk_max) { thisActor->chunk_max = i; }
  ART_ACTION_SCHEDULER_EXIT(1, 0)
  return result;
}


static void art_consumer_constructor(AbstractActorInstance *pBase) 
{
  ActorInstance_art_consumer *thisActor=(ActorInstance_art_consumer*) pBase;

  (void)thisActor;
}

static void art_consumer_setParam(AbstractActorInstance *pBase, 
				  const char *paramName, 
				  const char *value) 
{
  ActorInstance_art_consumer *thisActor=(ActorInstance_art_consumer*) pBase;

  (void)thisActor;
  {
    runtimeError(pBase,"No such parameter: %s", paramName);
  }
}

static void art_consumer_destructor(AbstractActorInstance *pBase)
{
  ActorInstance_art_consumer *thisActor=(ActorInstance_art_consumer*) pBase;

  printf("Consumed=%ld N=%ld N0=%ld max=%d\n", 
	 thisActor->consumed,
	 thisActor->N,
	 thisActor->N0,
	 thisActor->chunk_max);
}

static const PortDescription inputPortDescriptions[]={
  {"In", sizeof(int32_t)}
};

ActorClass ActorClass_art_consumer = { 
  .name="art_consumer", 
  .numInputPorts=1, 
  .numOutputPorts=0, 
  .sizeActorInstance=sizeof(ActorInstance_art_consumer), 
  .action_scheduler=art_consumer_action_scheduler, 
  .constructor=art_consumer_constructor, 
  .destructor=art_consumer_destructor, 
  .set_param=art_consumer_setParam, 
  .inputPortDescriptions=inputPortDescriptions, 
  .outputPortDescriptions=0, 
  .actorExecMode=0, 
  .numActions=0, 
  .actionDescriptions=0 
};
