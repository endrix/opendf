/*
 * Actor DDRModel (ActorClass_art_DDRModel)
 * Generated on Wed Jun 03 16:12:17 CEST 2009 from sysactors/art_DDRModel.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#ifndef ART_DDRMODEL_H
#define ART_DDRMODEL_H

#include "actors-rts.h"

#define IN0_RA(thisActor) INPUT_PORT(thisActor->base,0)
#define IN1_WA(thisActor) INPUT_PORT(thisActor->base,1)
#define IN2_WD(thisActor) INPUT_PORT(thisActor->base,2)
#define OUT0_RD(thisActor) OUTPUT_PORT(thisActor->base,0)

typedef struct {
  AbstractActorInstance base;
  int32_t s0_address;
  int32_t s1_burstSize;
  int32_t s3;
  int32_t s6_lastWA;
  int32_t *s2_buf;
} ActorInstance_art_DDRModel;

const int *art_DDRModel_action_scheduler(AbstractActorInstance *pBase);
void art_DDRModel_constructor(AbstractActorInstance *pBase);
void art_DDRModel_setParam(AbstractActorInstance*, const char*, const char*);
void art_DDRModel_destructor(AbstractActorInstance *pBase);

#endif
