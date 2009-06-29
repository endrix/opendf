/*
 * Actor art_Sink_txt (ActorClass_art_Sink_real)
 * Generated on Wed Jun 03 14:16:43 CEST 2009 from sysactors/art_Sink_real.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#include "art_Sink.h"

static const PortDescription inputPortDescriptions[]={
  {"In", sizeof(double)}
};

static const int portRate_1[] = {
  1
};

static const ActionDescription actionDescriptions[] = {
  {"actionAtLine_7", portRate_1, 0}
};

ActorClass ActorClass_art_Sink_real = INIT_ActorClass(
  "art_Sink_txt",
  ActorInstance_art_Sink_real,
  art_Sink_real_constructor,
  art_Sink_real_setParam,
  art_Sink_real_action_scheduler,
  art_Sink_real_destructor,
  1, inputPortDescriptions,
  0, 0,
  1, actionDescriptions
);
