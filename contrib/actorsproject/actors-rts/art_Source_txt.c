/*
 * Actor art_Source_txt (ActorClass_art_Source_txt)
 * Generated on Wed Jun 03 14:27:41 CEST 2009 from sysactors/art_Source_txt.xlim
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

ActorClass ActorClass_art_Source_txt = INIT_ActorClass(
  "art_Source_txt",
  ActorInstance_art_Source_txt,
  art_Source_txt_constructor,
  art_Source_txt_setParam,
  art_Source_txt_action_scheduler,
  art_Source_txt_destructor,
  0, 0,
  1, outputPortDescriptions,
  1, actionDescriptions
);
