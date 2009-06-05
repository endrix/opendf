/*
 * Actor art_Sink_txt (ActorClass_art_Sink_txt)
 * Generated on Wed Jun 03 14:12:24 CEST 2009 from sysactors/art_Sink_txt.xlim
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

ActorClass ActorClass_art_Sink_txt = INIT_ActorClass(
  "art_Sink_txt",
  ActorInstance_art_Sink_txt,
  art_Sink_txt_constructor,
  art_Sink_txt_setParam,
  art_Sink_txt_action_scheduler,
  art_Sink_txt_destructor,
  1, inputPortDescriptions,
  0, 0,
  1, actionDescriptions
);
