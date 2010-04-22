#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include "actors-rts.h"

typedef struct {
  AbstractActorInstance base;
  long produced;
  long N;
  int chunk;
} ActorInstance_art_producer;

static const int exitcode_block_Out_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};

static const int exitcode_terminate[] = {
  EXITCODE_TERMINATE
};

ART_ACTION_CONTEXT(0, 1);

ART_ACTION(action1,  ActorInstance_art_producer)
//static void action1(ActorInstance_art_producer *thisActor,
//		    ActionContext *context)
{
  ART_ACTION_ENTER(action1, 0);
  pinWrite_int32_t(ART_OUTPUT(0), (int)thisActor->produced);
  thisActor->produced++;
  ART_ACTION_EXIT(action1, 0);
} 

ART_ACTION_SCHEDULER(art_producer_action_scheduler)
{
  int n, i;
  ActorInstance_art_producer *thisActor=(ActorInstance_art_producer*) pBase;
  const int *result = EXIT_CODE_YIELD;
  ART_ACTION_SCHEDULER_ENTER(0, 1)

  n = pinAvailOut_int32_t(ART_OUTPUT(0));
  if (n > thisActor->chunk) { n = thisActor->chunk; }
  i = 0;
  ART_ACTION_SCHEDULER_LOOP {
    ART_ACTION_SCHEDULER_LOOP_TOP;
    if (i < n) {
      i++;
      if (thisActor->produced >= thisActor->N) {
	result = exitcode_terminate;
	goto out;
      } else {
	ART_FIRE_ACTION(action1);
      }
    } else {
      result = exitcode_block_Out_1;
      goto out;
    } 
    ART_ACTION_SCHEDULER_LOOP_BOTTOM;
  }
out:
  ART_ACTION_SCHEDULER_EXIT(0, 1)
  return result;
}


static void art_producer_constructor(AbstractActorInstance *pBase) 
{
  ActorInstance_art_producer *thisActor=(ActorInstance_art_producer*) pBase;

  (void)thisActor;
}

static void art_producer_setParam(AbstractActorInstance *pBase, 
				  const char *paramName, 
				  const char *value) 
{
  ActorInstance_art_producer *thisActor=(ActorInstance_art_producer*) pBase;

  (void)thisActor;
  if (strcmp(paramName, "N") == 0) {
    thisActor->N = atol(value);
  } else if (strcmp(paramName, "chunk") == 0) {
    thisActor->chunk = atoi(value);
  } else {
    runtimeError(pBase,"No such parameter: %s", paramName);
  }
}

static void art_producer_destructor(AbstractActorInstance *pBase)
{
  ActorInstance_art_producer *thisActor=(ActorInstance_art_producer*) pBase;

  (void)thisActor;
}

static const PortDescription outputPortDescriptions[]={
  {"Out", sizeof(int32_t)}
};

ActorClass ActorClass_art_producer = { 
  .name="art_producer", 
  .numInputPorts=0, 
  .numOutputPorts=1, 
  .sizeActorInstance=sizeof(ActorInstance_art_producer), 
  .action_scheduler=art_producer_action_scheduler, 
  .constructor=art_producer_constructor, 
  .destructor=art_producer_destructor, 
  .set_param=art_producer_setParam, 
  .inputPortDescriptions=0, 
  .outputPortDescriptions=outputPortDescriptions, 
  .actorExecMode=0, 
  .numActions=0, 
  .actionDescriptions=0 
};
