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

#include "display.h"

typedef short int16bpp_t;
typedef int   int32bpp_t;

/*To make sure that you are bounding your inputs in the range of 0 & 255*/
#define SATURATE8(x) ((unsigned int) x <= 255 ? x : (x < 0 ? 0: 255))



void display_yuv_16bpp(int x, int y,
		       yuv_sample_t macroBlock[MB_SIZE],
		       const struct FrameBuffer *format) {
  yuv_sample_t *yptr=macroBlock + START_Y;
  yuv_sample_t *uptr=macroBlock + START_U;
  yuv_sample_t *vptr=macroBlock + START_V;
  int lineLength=format->pixelsPerLine;
  int16bpp_t *fbp=format->framePtr;
  int Rshift=format->Rshift;
  int Rmask=format->Rmask;
  int Gshift=format->Gshift;
  int Gmask=format->Gmask;
  int Bshift=format->Bshift;
  int Bmask=format->Bmask;
  int j,k;
	

  fbp += x + y*lineLength;
  for(j=0; j<8; j++){
    for(k=0; k<8; k++){
      int tu = *uptr++ - 128;
      int tv = *vptr++ - 128;
      int ruv = 409*tv + 128;
      int guv = 100*tu + 208*tv - 128;
      int buv = 516*tu;
      int dj,dk;
      
      for(dk=0; dk<2; dk++, yptr++, fbp++){
	for(dj=0; dj<2; dj++){
	  int y = yptr[16*dj];
	  int t = (y-16)*298;
	  int r = ((SATURATE8((t+ruv)>>8) << Rshift) & Rmask);
	  int g = ((SATURATE8((t+guv)>>8) << Gshift) & Gmask);
	  int b = ((SATURATE8((t+buv)>>8) << Bshift) & Bmask);
	  fbp[lineLength*dj] = r | g | b;
	}
      }
    }
    // Skip odd lines of Y-samples (inner loop takes care of them)
    yptr += 16;

    // Skip odd lines of frame buffer
    fbp += 2*lineLength - 16;
  }
}


void display_yuv_32bpp(int x, int y,
		       yuv_sample_t macroBlock[MB_SIZE],
		       const struct FrameBuffer *format) {
  yuv_sample_t *yptr=macroBlock + START_Y;
  yuv_sample_t *uptr=macroBlock + START_U;
  yuv_sample_t *vptr=macroBlock + START_V;
  int lineLength=format->pixelsPerLine;
  int32bpp_t *fbp=format->framePtr;
  int Rshift=format->Rshift;
  int Rmask=format->Rmask;
  int Gshift=format->Gshift;
  int Gmask=format->Gmask;
  int Bshift=format->Bshift;
  int Bmask=format->Bmask;
  int j,k;
	

  fbp += x + y*lineLength;
  for(j=0; j<8; j++){
    for(k=0; k<8; k++){
      int tu = *uptr++ - 128;
      int tv = *vptr++ - 128;
      int ruv = 409*tv + 128;
      int guv = 100*tu + 208*tv - 128;
      int buv = 516*tu;
      int dj,dk;
      
      for(dk=0; dk<2; dk++, yptr++, fbp++){
	for(dj=0; dj<2; dj++){
	  int y = yptr[16*dj];
	  int t = (y-16)*298;
	  int r = ((SATURATE8((t+ruv)>>8) << Rshift) & Rmask);
	  int g = ((SATURATE8((t+guv)>>8) << Gshift) & Gmask);
	  int b = ((SATURATE8((t+buv)>>8) << Bshift) & Bmask);
	  fbp[lineLength*dj] = r | g | b;
	}
      }
    }
    // Skip odd lines of Y-samples (inner loop takes care of them)
    yptr += 16;

    // Skip odd lines of frame buffer
    fbp += 2*lineLength - 16;
  }
}

