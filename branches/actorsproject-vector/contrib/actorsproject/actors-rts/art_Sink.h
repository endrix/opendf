/*
 * Actor art_Sink_bin (ActorClass_art_Sink_bin)
 * Generated on Wed Jun 03 11:04:36 CEST 2009 from sysactors/art_Sink_bin.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#ifndef ART_SINK_H
#define ART_SINK_H

#include <stdio.h>
#include "actors-rts.h"

#define IN0_In(thisActor) INPUT_PORT(thisActor->base,0)

typedef struct {
  AbstractActorInstance base;
  FILE *file;
} ActorInstance_art_Sink;

typedef ActorInstance_art_Sink ActorInstance_art_Sink_bin;
typedef ActorInstance_art_Sink ActorInstance_art_Sink_txt;
typedef ActorInstance_art_Sink ActorInstance_art_Sink_real;

const int *art_Sink_bin_action_scheduler(AbstractActorInstance*);
void art_Sink_bin_constructor(AbstractActorInstance*);
void art_Sink_bin_setParam(AbstractActorInstance*, const char*, const char*);
void art_Sink_bin_destructor(AbstractActorInstance*);

const int *art_Sink_txt_action_scheduler(AbstractActorInstance*);
void art_Sink_txt_constructor(AbstractActorInstance*);
void art_Sink_txt_setParam(AbstractActorInstance*, const char*, const char*);
void art_Sink_txt_destructor(AbstractActorInstance*);

const int *art_Sink_real_action_scheduler(AbstractActorInstance*);
void art_Sink_real_constructor(AbstractActorInstance*);
void art_Sink_real_setParam(AbstractActorInstance*, const char*, const char*);
void art_Sink_real_destructor(AbstractActorInstance*);

#endif
