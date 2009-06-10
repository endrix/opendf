/* 
 * Copyright (c) Ericsson AB, 2009
 * Author: Charles Chen Xu (charles.chen.xu@ericsson.com)
 * All rights reserved.
 *
 * License terms:
 *
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 *     * Redistributions of source code must retain the above 
 *       copyright notice, this list of conditions and the 
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the 
 *       above copyright notice, this list of conditions and 
 *       the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names 
 *       of its contributors may be used to endorse or promote 
 *       products derived from this software without specific 
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <time.h>
#include <sys/timeb.h>
#include <sys/mman.h>
#include <sys/ioctl.h>

#include "art_Display_yuv.h"

/*To make sure that you are bounding your inputs in the range of 0 & 255*/
#define SATURATE8(x) ((unsigned int) x <= 255 ? x : (x < 0 ? 0: 255))

//Convert RGB5XX into a 16-bit number
#define RGB565(r, g, b) ((r >> 3) << 11)| ((g >> 2) << 5)| ((b >> 3) << 0)
#define RGB555(r, g, b) ((r >> 3) << 10)| ((g >> 3) << 5)| ((b >> 3) << 0)

// #define WIDTH	176
// #define HIGHT	144
#define DEPTH	12
#define FRAME_YUV_SIZE			thisActor->width*thisActor->height*DEPTH/8

#define	MICRO_YUV_SIZE			(6*64)
#define MICRO_RGB_SIZE			4*64
#define WIDTH_IN_MB				11
#define HEIGHT_IN_MB				9
#define IMAGE_WIDTH				176
#define IMAGE_HEIGHT			144

#define START_Y 0
#define START_U (4*64)
#define START_V (5*64)

#define IN0_A					base.inputPort[0]
#define IN0_TOKENSIZE			base.inputPort[0].tokenSize

#ifdef GTK
static void on_darea_expose(GtkWidget *widget,GdkEventExpose *event,gpointer user_data)
{
	ActorInstance_art_Display_yuv *thisActor = (ActorInstance_art_Display_yuv *)user_data;
	gdk_draw_rgb_image(widget->window, widget->style->fg_gc[GTK_STATE_NORMAL],
		      0, 0, thisActor->width, thisActor->height,
		      GDK_RGB_DITHER_MAX, thisActor->rgbbuf, thisActor->width*3);
	gtk_main_quit();
}

static void display_gdk(ActorInstance_art_Display_yuv *thisActor)
{
	gtk_signal_connect(GTK_OBJECT(thisActor->darea), "expose-event",GTK_SIGNAL_FUNC(on_darea_expose), thisActor);
	gtk_drawing_area_size(GTK_DRAWING_AREA(thisActor->darea), thisActor->width,thisActor->height);
	gtk_widget_show_all(thisActor->window);
	gtk_main();
}
#endif

static void display_mb(ActorInstance_art_Display_yuv *thisActor){
	int i,j,k;
	int tu,tv;
	int	dj,dk;
	int	ruv,guv,buv;
	int	y,t,r,g,b,jj,kk;
#if defined FB
	unsigned long location;
	unsigned short rgb565;
#elif defined GTK
	int xy;
#elif defined SDL
	int xy;
	int pixel;
#endif

	for(j=0; j<8; j++){
		for(k=0; k<8; k++){
			i = 8*j + k;
			tu = thisActor->macroBlock[START_U+i] - 128;
			tv = thisActor->macroBlock[START_V+i] - 128;
			ruv = 409*tv + 128;
			guv = 100*tu + 208*tv - 128;
			buv = 516*tu;
			for(dj=0; dj<2; dj++){
				for(dk=0; dk<2; dk++){
					jj = 2*j + dj;
					kk = 2*k + dk;
					y = thisActor->macroBlock[16*jj + kk];
					t = (y-16)*298;
					r = (t+ruv)>>8;
					g = (t+guv)>>8;
					b = (t+buv)>>8;
#if defined FB
					rgb565 = RGB565(SATURATE8(r),SATURATE8(g),SATURATE8(b));
					location = (thisActor->mbx+kk+thisActor->vinfo.xoffset) * (thisActor->vinfo.bits_per_pixel/8) + (thisActor->mby+jj+thisActor->vinfo.yoffset) * thisActor->finfo.line_length;
					*((unsigned short int*)(thisActor->fbp + location)) = rgb565;
#elif defined GTK
					xy = (thisActor->mby+jj) * thisActor->width;
					xy += thisActor->mbx+kk;
					xy *= 3;
					thisActor->rgbbuf[xy++]=SATURATE8(r);
					thisActor->rgbbuf[xy++]=SATURATE8(g);
					thisActor->rgbbuf[xy++]=SATURATE8(b);
					thisActor->ppf += 3;
#elif defined SDL
					xy = (thisActor->mby+jj) * thisActor->width;
					xy += thisActor->mbx+kk;
					xy *= 3;
					pixel = ((SATURATE8(r) << thisActor->image->format->Rshift) &
                            thisActor->image->format->Rmask) |
                            ((SATURATE8(g) << thisActor->image->format->Gshift) &
                            thisActor->image->format->Gmask) |
                            ((SATURATE8(b) << thisActor->image->format->Bshift) &
                            thisActor->image->format->Bmask);
					*(int*)&((char*)thisActor->image->pixels)[xy] = pixel;
					thisActor->ppf += 3;
#endif	
				}
			}
		}
	}
#ifdef GTK
	if(thisActor->ppf == thisActor->width*thisActor->height*3){
		thisActor->ppf = 0;
		display_gdk(thisActor);
	}
#elif defined SDL
	if(thisActor->ppf == thisActor->width*thisActor->height*3){
		thisActor->ppf = 0;
		SDL_BlitSurface(thisActor->image, NULL, thisActor->screen, NULL);
		SDL_Flip(thisActor->screen);
	}
#endif
}

static const int exitcode_block_In_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};

const int *art_Display_yuv_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance_art_Display_yuv *thisActor=
    (ActorInstance_art_Display_yuv *) pBase;
  int32_t start=thisActor->start;
  int32_t count=thisActor->count;
  int32_t comp=thisActor->comp;

  while(1) {
    do {
      do {
	while (count<64) {
	  /*** read action ***/ 
	  int32_t avail=pinAvailIn_int32_t(IN0_In(thisActor));
	  int32_t n;
	  unsigned char *ptr;
	  unsigned char *end;

	  if (avail==0) {
	    // Save back state
	    thisActor->start=start;
	    thisActor->count=count;
	    thisActor->comp=comp;
	    pinWaitIn(IN0_In(thisActor), 1);
	    return exitcode_block_In_1;
	  }

	  if (comp<4) {
	    // The Y sub-macroblocks are stored interleaved
	    // in the macroBlock buffer: a row (8 bytes) of Y0
	    // is followed by a row of Y1 (same for Y2/Y3).
	    // In the CAL code this is expressed as:
	    //
	    // if (comp<4) and (bitand(count, 7)=0) then
	    //   start := start + 8;
	    // end
	    //
	    // So we want to go to the next multiple of eight
	    n=8-(count & 7);
	  }
	  else
	    n=64-count;

	  if (avail<n)
	    n=avail;
	  ptr=thisActor->macroBlock+RANGECHK(start+count, MB_SIZE);
	  end=ptr+n;
	  do {
	    TRACE_ACTION(&thisActor->base, 0, "read");
	    *ptr++=pinRead_int32_t(IN0_In(thisActor));
	  } while (ptr<end);

	  count += n;
	  if (comp<4 && (count & 7)==0)
	    start=start+8;
	}

	/*** done.comp action ***/
	count=0;
	comp=comp+1;
	start=comp*64;
	if (comp==1 || comp==3)
	  start-=56;  // due to the interleaving of Y0/Y1 and Y2/Y3
      } while (comp<6);

      /*** done.mb action ***/
      display_mb(thisActor);
      comp=0;
      start=0;
      thisActor->mbx+=16;
    } while (thisActor->mbx<16*WIDTH_IN_MB);
    
    thisActor->mbx=0;
    thisActor->mby+=16;
    if (thisActor->mby>=16*HEIGHT_IN_MB){
	  thisActor->totFrames++;	
      thisActor->mby=0;
	  }
  }
} 

void art_Display_yuv_constructor(AbstractActorInstance *pBase) {
	ActorInstance_art_Display_yuv	*thisActor=(ActorInstance_art_Display_yuv*) pBase;
	struct timeb tb;

	ftime(&tb);
	memset(thisActor->macroBlock,0,MB_SIZE);	
	thisActor->mbx=0;
	thisActor->mby=0;
	thisActor->count=0;
	thisActor->comp=0;
	thisActor->start=0;
	
	thisActor->startTime=tb.time*1000+tb.millitm;
	thisActor->totFrames=0;
#ifdef FB
	/* size of video memory in bytes */
	long int screensize;

    /* Open the file for reading and writing */
    thisActor->fbfd = open("/dev/fb0", O_RDWR);
    if (thisActor->fbfd == -1) {
        perror("/dev/fb0");
        exit(1);
    }

    /* Get fixed screen information */
    if (ioctl(thisActor->fbfd, FBIOGET_FSCREENINFO, &thisActor->finfo)) {
        perror("FBIOGET_FSCREENINFO");
        exit(2);
    }

    /*  Get variable screen information */
    if (ioctl(thisActor->fbfd, FBIOGET_VSCREENINFO, &thisActor->vinfo)) {
        perror("FBIOGET_VSCREENINFO");;
        exit(3);
    }

    printf("%dx%d, %dbpp\n", thisActor->vinfo.xres, thisActor->vinfo.yres, thisActor->vinfo.bits_per_pixel );

    /* Figure out the size of the video memory in bytes */
    screensize = thisActor->vinfo.xres * thisActor->vinfo.yres * thisActor->vinfo.bits_per_pixel / 8;

    /* Map the device to memory */
    thisActor->fbp = (char *)mmap(0, screensize, PROT_READ | PROT_WRITE, MAP_SHARED,
                       thisActor->fbfd, 0);
    if ((int)thisActor->fbp == -1) {
        perror("mmap()");
        exit(4);
    }
#elif defined GTK
	gtk_init(NULL,NULL);
	gdk_init(NULL, NULL);
	gdk_rgb_init();
	thisActor->window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
	thisActor->darea = gtk_drawing_area_new();
 	gtk_drawing_area_size(GTK_DRAWING_AREA(thisActor->darea), IMAGE_WIDTH, IMAGE_HEIGHT);
 	gtk_container_add(GTK_CONTAINER(thisActor->window),thisActor->darea);
	gtk_window_set_title(GTK_WINDOW(thisActor->window),thisActor->title);
	thisActor->ppf=0;
#elif defined SDL
	//Start SDL
	SDL_Init(SDL_INIT_VIDEO);
	atexit(SDL_Quit);
    thisActor->screen = SDL_SetVideoMode(IMAGE_WIDTH, IMAGE_HEIGHT, 24, SDL_HWSURFACE|SDL_DOUBLEBUF);
	thisActor->image = SDL_CreateRGBSurface(SDL_SWSURFACE,
	                             IMAGE_WIDTH, IMAGE_HEIGHT,
	                             thisActor->screen->format->BitsPerPixel,
                                 thisActor->screen->format->Rmask,
                                 thisActor->screen->format->Gmask,
                                 thisActor->screen->format->Bmask,
                                 thisActor->screen->format->Amask);
#endif

}

void art_Display_yuv_destructor(AbstractActorInstance *pBase)
{
	ActorInstance_art_Display_yuv *thisActor=(ActorInstance_art_Display_yuv*) pBase;
	struct timeb tb;
	int totTime;
	int screensize;
	ftime(&tb);
	totTime = tb.time*1000 + tb.millitm - thisActor->startTime;
	if(thisActor->fbp){
		screensize = thisActor->vinfo.xres * thisActor->vinfo.yres * thisActor->vinfo.bits_per_pixel / 8;
    	munmap(thisActor->fbp, screensize);
	}
	if(thisActor->fbfd)
    	close(thisActor->fbfd);
#ifdef SDL
	if(thisActor->image)
		SDL_FreeSurface(thisActor->image);
#endif
	printf("%d total frames in %f seconds (%f fps)\n",
	       thisActor->totFrames,
	       (double) totTime/1000, 
	       (double) (thisActor->totFrames)*1000/totTime);

}

void art_Display_yuv_setParam(AbstractActorInstance *pBase,
		              const char *paramName, 
			      const char *value) {
  ActorInstance_art_Display_yuv *thisActor=
    (ActorInstance_art_Display_yuv*) pBase;

  if(strcmp(paramName,"title") == 0)
    thisActor->title = value;
  else if(strcmp(paramName,"height") == 0)
    thisActor->height = atoi(value);
  else if(strcmp(paramName,"width") == 0)	
    thisActor->width = atoi(value);
}
