/*
 * Actor art_Source_bin (ActorClass_art_Source_bin)
 * Generated on Wed Jun 03 14:27:41 CEST 2009 from sysactors/art_Source_bin.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#ifndef ART_SOURCE_H
#define ART_SOURCE_H

#include <stdio.h>
#include "actors-rts.h"

#define OUT0_Out(thisActor) OUTPUT_PORT(thisActor->base,0)

typedef struct {
  AbstractActorInstance base;
  FILE *file;
} ActorInstance_art_Source;

typedef ActorInstance_art_Source ActorInstance_art_Source_bin;
typedef ActorInstance_art_Source ActorInstance_art_Source_txt;
typedef ActorInstance_art_Source ActorInstance_art_Source_real;

const int *art_Source_bin_action_scheduler(AbstractActorInstance*);
void art_Source_bin_constructor(AbstractActorInstance*);
void art_Source_bin_setParam(AbstractActorInstance*, const char*, const char*);
void art_Source_bin_destructor(AbstractActorInstance*);

const int *art_Source_txt_action_scheduler(AbstractActorInstance*);
void art_Source_txt_constructor(AbstractActorInstance*);
void art_Source_txt_setParam(AbstractActorInstance*, const char*, const char*);
void art_Source_txt_destructor(AbstractActorInstance*);

const int *art_Source_real_action_scheduler(AbstractActorInstance*);
void art_Source_real_constructor(AbstractActorInstance*);
void art_Source_real_setParam(AbstractActorInstance*, const char*, const char*);
void art_Source_real_destructor(AbstractActorInstance*);

#endif
