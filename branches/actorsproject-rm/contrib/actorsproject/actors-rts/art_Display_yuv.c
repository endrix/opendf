/*
 * Actor art_Sink_yuv (ActorClass_art_Display_yuv)
 * Generated on Thu Jun 04 14:28:52 CEST 2009 from sysactors/art_Display_yuv.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#include "art_Display_yuv.h"

static const PortDescription inputPortDescriptions[]={
  {"In", sizeof(int32_t)}
};

static const PortDescription outputPortDescriptions[]={
  {"Out", sizeof(int32_t)}
};

static const int portRate_1[] = {
  1
};

static const int portRate_0[] = {
  0
};

static const ActionDescription actionDescriptions[] = {
  {"read", portRate_1, portRate_0},
  {"done.comp", portRate_0, portRate_0},
  {"done.mb", portRate_0, portRate_0},
  {"report", portRate_0,portRate_1}
};

ActorClass ActorClass_art_Display_yuv = INIT_ActorClass(
  "art_Sink_yuv",
  ActorInstance_art_Display_yuv,
  art_Display_yuv_constructor,
  art_Display_yuv_setParam,
  art_Display_yuv_action_scheduler,
  art_Display_yuv_destructor,
  1, inputPortDescriptions,
  1, outputPortDescriptions,
  4, actionDescriptions
);
