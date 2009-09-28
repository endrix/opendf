/*
 * Actor art_Source_bin (ActorClass_art_Source_bin)
 * Generated on Wed Jun 03 14:27:41 CEST 2009 from sysactors/art_Source_bin.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#include "art_Source.h"


static const PortDescription outputPortDescriptions[]={
  {"Out", sizeof(int32_t)}
};

static const int portRate_0[] = {
  0
};

static const ActionDescription actionDescriptions[] = {
  {"actionAtLine_7", 0, portRate_0}
};

ActorClass ActorClass_art_Source_bin = INIT_ActorClass(
  "art_Source_bin",
  ActorInstance_art_Source_bin,
  art_Source_bin_constructor,
  art_Source_bin_setParam,
  art_Source_bin_action_scheduler,
  art_Source_bin_destructor,
  0, 0,
  1, outputPortDescriptions,
  1, actionDescriptions
);
