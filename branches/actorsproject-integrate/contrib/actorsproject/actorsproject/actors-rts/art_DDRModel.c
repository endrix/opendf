/*
 * Actor DDRModel (ActorClass_art_DDRModel)
 * Generated on Wed Jun 03 16:12:17 CEST 2009 from sysactors/art_DDRModel.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#include "art_DDRModel.h"

static const PortDescription inputPortDescriptions[]={
  {"RA", sizeof(int32_t)},
  {"WA", sizeof(int32_t)},
  {"WD", sizeof(int32_t)}
};

static const PortDescription outputPortDescriptions[]={
  {"RD", sizeof(int32_t)}
};

static const int portRate_1_0_0[] = {
  1, 0, 0
};

static const int portRate_0[] = {
  0
};

static const int portRate_0_1_0[] = {
  0, 1, 0
};

static const int portRate_0_0_0[] = {
  0, 0, 0
};

static const int portRate_0_0_1[] = {
  0, 0, 1
};

static const ActionDescription actionDescriptions[] = {
  {"select.read.prefer", portRate_1_0_0, portRate_0},
  {"select.write.prefer", portRate_0_1_0, portRate_0},
  {"select.read.low", portRate_1_0_0, portRate_0},
  {"select.write.low", portRate_0_1_0, portRate_0},
  {"data.read", portRate_0_0_0, portRate_0},
  {"data.write", portRate_0_0_1, portRate_0}
};

ActorClass ActorClass_art_DDRModel = INIT_ActorClass(
  "DDRModel",
  ActorInstance_art_DDRModel,
  art_DDRModel_constructor,
  art_DDRModel_setParam,
  art_DDRModel_action_scheduler,
  art_DDRModel_destructor,
  3, inputPortDescriptions,
  1, outputPortDescriptions,
  6, actionDescriptions
);
