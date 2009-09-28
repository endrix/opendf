/*
 * Actor art_Sink_bin (ActorClass_art_Sink_bin)
 * Generated on Wed Jun 03 11:04:36 CEST 2009 from sysactors/art_Sink_bin.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#include "art_Sink.h"


static const PortDescription inputPortDescriptions[]={
  {"In", sizeof(int32_t)}
};


static const int portRate_1[] = {
  1
};

static const ActionDescription actionDescriptions[] = {
  {"actionAtLine_7", portRate_1, 0}
};

ActorClass ActorClass_art_Sink_bin = INIT_ActorClass(
  "art_Sink_bin",
  ActorInstance_art_Sink_bin,
  art_Sink_bin_constructor,
  art_Sink_bin_setParam,
  art_Sink_bin_action_scheduler,
  art_Sink_bin_destructor,
  1, inputPortDescriptions,
  0, 0,
  1, actionDescriptions
);
