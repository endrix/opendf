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

/*
 * Actor Display
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/fb.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include "actors-rts.h"

/*To make sure that you are bounding your inputs in the range of 0 & 255*/
#define SATURATE8(x) ((unsigned int) x <= 255 ? x : (x < 0 ? 0: 255))

//Convert RGB5XX into a 16-bit number
#define RGB565(r, g, b) ((r >> 3) << 11)| ((g >> 2) << 5)| ((b >> 3) << 0)
#define RGB555(r, g, b) ((r >> 3) << 10)| ((g >> 3) << 5)| ((b >> 3) << 0)

// #define WIDTH	176
// #define HIGHT	144
#define DEPTH	12
#define FRAME_YUV_SIZE			thisActor->width*thisActor->hight*DEPTH/8
#define	MICRO_YUV_SIZE			6*64
#define MICRO_RGB_SIZE			4*64
#define WIDTH_IN_MB				11
#define HIGHT_IN_MB				9

#define IN0_A					base.inputPort[0]
#define IN0_TOKENSIZE			base.inputPort[0].tokenSize

typedef struct {
  AbstractActorInstance		base;
  struct fb_var_screeninfo	vinfo;
  struct fb_fix_screeninfo	finfo;
  char						*fbp;
  int 						fbfd;
  int						hight;
  int						width;

  int 						START_U;
  int 						START_V;
  int 						MB_SIZE;
  unsigned char 			macroBlock[MICRO_YUV_SIZE];	
  int						mbx;
  int					 	mby;
  int 						count;
  int 						comp;
  int 						start;

} ActorInstance;

static void a_action_scheduler(ActorInstance *);
static void constructor(AbstractActorInstance*);
static void destructor(AbstractActorInstance*);
static void set_param(AbstractActorInstance*,int,ActorParameter*);

ActorClass ActorClass_art_Display_yuv ={
  "art_Display_yuv",
  1, /* numInputPorts */
  0, /* numOutputPorts */
  sizeof(ActorInstance),
  (void*)a_action_scheduler,
  constructor,
  destructor,
  set_param
};

static void display_mb(ActorInstance *thisActor){
	int i,j,k;
	int tu,tv;
	int	dj,dk;
	int	ruv,guv,buv;
	int	y,t,r,g,b,jj,kk;
	unsigned long location;
	unsigned short rgb565;

	for(j=0; j<8; j++){
		for(k=0; k<8; k++){
			i = 8*j + k;
			tu = thisActor->macroBlock[thisActor->START_U+i] - 128;
			tv = thisActor->macroBlock[thisActor->START_V+i] - 128;
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
					rgb565 = RGB565(SATURATE8(r),SATURATE8(g),SATURATE8(b));
					location = (thisActor->mbx+kk+thisActor->vinfo.xoffset) * (thisActor->vinfo.bits_per_pixel/8) + (thisActor->mby+jj+thisActor->vinfo.yoffset) * thisActor->finfo.line_length;
					*((unsigned short int*)(thisActor->fbp + location)) = rgb565;
				}
			}
		}
	}
}

static void done_mb(ActorInstance *thisActor)
{
	display_mb(thisActor);
	thisActor->count = 0;
	thisActor->comp = 0;
	thisActor->start = 0;
	thisActor->mbx += 16;
	if(thisActor->mbx >= 16*WIDTH_IN_MB){
		thisActor->mbx = 0;
		thisActor->mby += 16;
		if(thisActor->mby >= 16*HIGHT_IN_MB){
			thisActor->mby = 0;
		}
	}
}

static void done_comp(ActorInstance *thisActor)
{
	thisActor->count = 0;
	thisActor->comp += 1;
	if(thisActor->comp == 1 || thisActor->comp == 3)
		thisActor->start = (thisActor->comp-1)*64+8;
	else
		thisActor->start = thisActor->comp*64;
}

static void Read0(ActorInstance *thisActor) {
	int			ret,val;

	ret = pinRead2(&thisActor->IN0_A,(char*)&val,thisActor->IN0_TOKENSIZE);
	thisActor->macroBlock[thisActor->start+thisActor->count] = (unsigned char)val;
	thisActor->count++;

	if(thisActor->comp<4 && ((thisActor->count&7)==0))
	{
		thisActor->start += 8;
	}
}

static void a_action_scheduler(ActorInstance *thisActor)
{
	int available;

	while(1)
	{
		available=pinStatus2(&thisActor->IN0_A);

		if(thisActor->count == 64 && thisActor->comp == 5)
			done_mb(thisActor);
		else if(thisActor->count == 64)
			done_comp(thisActor);
		else if(available>=thisActor->IN0_TOKENSIZE)
			Read0(thisActor);	
		else
		{
			pinWait(&thisActor->IN0_A,thisActor->IN0_TOKENSIZE);
			return;
		}
	}
}

static void constructor(AbstractActorInstance *pBase)
{
	ActorInstance	*thisActor=(ActorInstance*) pBase;

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

	thisActor->START_U = 4*64;
	thisActor->START_V = 5*64;
	thisActor->MB_SIZE = 6*64;
	memset(thisActor->macroBlock,0,thisActor->MB_SIZE);	
	thisActor->mbx=0;
	thisActor->mby=0;
	thisActor->count=0;
	thisActor->comp=0;
	thisActor->start=0;
}

static void destructor(AbstractActorInstance *pBase)
{
	ActorInstance *thisActor=(ActorInstance*) pBase;
	int screensize;
	if(thisActor->fbp){
		screensize = thisActor->vinfo.xres * thisActor->vinfo.yres * thisActor->vinfo.bits_per_pixel / 8;
    	munmap(thisActor->fbp, screensize);
	}
	if(thisActor->fbfd)
    	close(thisActor->fbfd);
}

static void set_param(AbstractActorInstance *pBase,int numParams,ActorParameter *param){
	ActorInstance *thisActor=(ActorInstance*) pBase;
	ActorParameter *p;
	int	i;
	for(i=0,p=param; i<numParams; i++,p++)
	{
		if(strcmp(p->key,"displayMode") == 0)
		{
			if(strcmp(p->value,"sqcif")==0){
				thisActor->hight = 128;
				thisActor->width = 96;
			}
			else if(strcmp(p->value,"qcif")==0){
				thisActor->hight = 144;
				thisActor->width = 176;
			}
			else if(strcmp(p->value,"qvga")==0){
				thisActor->hight = 320;
				thisActor->width = 240;
			}
			else if(strcmp(p->value,"vga")==0){
				thisActor->hight = 720;
				thisActor->width = 480;
			}
			else{
				thisActor->hight = 320;
				thisActor->width = 240;
			}
		}
	}
}
