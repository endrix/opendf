/*
 * Actor art_Sink_yuv (ActorClass_art_Display_yuv)
 * Generated on Thu Jun 04 14:28:52 CEST 2009 from sysactors/art_Display_yuv.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#ifndef ART_DISPLAY_H
#define ART_DISPLAY_H

#include <linux/fb.h>
#ifdef GTK
#include <gtk/gtk.h>
#endif

#include "actors-rts.h"

#define MB_SIZE (6*64)

typedef struct {
  AbstractActorInstance		base;
  struct fb_var_screeninfo	vinfo;
  struct fb_fix_screeninfo	finfo;
  char						*fbp;
  int 						fbfd;
  int						height;
  int						width;
  const char					*title;	
  unsigned char 			macroBlock[MB_SIZE];	
  int						mbx;
  int					 	mby;
  int 						count;
  int 						comp;
  int 						start;
  int 						startTime;
  int 						totFrames;
#ifdef GTK
  GtkWidget					*window;
  GtkWidget					*darea;
  int						ppf;	
  guchar					rgbbuf[IMAGE_WIDTH*IMAGE_HEIGHT*3];
#endif
} ActorInstance_art_Display_yuv;

#define IN0_In(thisActor) INPUT_PORT(thisActor->base,0)

const int *art_Display_yuv_action_scheduler(AbstractActorInstance*);
void art_Display_yuv_constructor(AbstractActorInstance*);
void art_Display_yuv_setParam(AbstractActorInstance*, const char*, const char*);
void art_Display_yuv_destructor(AbstractActorInstance *pBase);

#endif
