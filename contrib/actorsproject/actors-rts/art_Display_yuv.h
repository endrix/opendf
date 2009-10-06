/*
 * Actor art_Sink_yuv (ActorClass_art_Display_yuv)
 * Generated on Thu Jun 04 14:28:52 CEST 2009 from sysactors/art_Display_yuv.xlim
 * by xlim2c version 0.6 (June 3, 2009)
 */

#ifndef ART_DISPLAY_H
#define ART_DISPLAY_H

#ifdef FB
#include <linux/fb.h>
#elif defined GTK
#include <gtk/gtk.h>
#elif defined SDL
#include "SDL/SDL.h"
#endif

#include "actors-rts.h"

#define MB_SIZE (6*64)
#define IMAGE_WIDTH				176
#define IMAGE_HEIGHT			144

typedef struct {
  AbstractActorInstance base;
  int                   height;
  int                   width;
  const char            *title;
  unsigned char         macroBlock[MB_SIZE];
  int                   mbx,mby;
  int                   count;
  int                   comp;
  int                   start;
  int                   startTime;
  int                   totFrames;
  int                   fp;
  int                   ppf;
#ifdef FB
  struct fb_var_screeninfo  vinfo;
  struct fb_fix_screeninfo  finfo;
  char                  *fbp;
  int                   fbfd;
#elif defined GTK
  GtkWidget             *window;
  GtkWidget             *darea;
  guchar                rgbbuf[IMAGE_WIDTH*IMAGE_HEIGHT*3];
#elif defined SDL
  SDL_Surface           *screen;
  SDL_Surface           *image;
#endif
} ActorInstance_art_Display_yuv;

#define IN0_In(thisActor) INPUT_PORT(thisActor->base,0)

const int *art_Display_yuv_action_scheduler(AbstractActorInstance*);
void art_Display_yuv_constructor(AbstractActorInstance*);
void art_Display_yuv_setParam(AbstractActorInstance*, const char*, const char*);
void art_Display_yuv_destructor(AbstractActorInstance *pBase);

#endif
