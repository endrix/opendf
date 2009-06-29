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

#include <stdlib.h>
#include "art_DDRModel.h"

/*
 * When compiling from the source art_DDRModel.cal we patch 
 * two things manually:
 * 1) The size of the frame buffer (MEMSIZE), since the SSAGenerator
 *    produces an inconveniently large initializer (4M XML elements),
 *    which leads to out-of-heap-memory on many machines
 * 2) There is a timing-dependence that may lead to deadlock.
 */

#define MEMSIZE 4194304

static const int exitcode_block_WA_1_RD_96[] = {
  EXITCODE_BLOCK(2), 1, 1, 3, 96
};

static const int exitcode_block_WA_1[] = {
  EXITCODE_BLOCK(1), 1, 1
};

static const int exitcode_block_RA_1_WA_1[] = {
  EXITCODE_BLOCK(2), 0, 1, 1, 1
};

static const int exitcode_block_WD_1[] = {
  EXITCODE_BLOCK(1), 2, 1
};

#define STATE_getAddr     1
/* #define STATE_doDataRead  2 */
#define STATE_doDataWrite 3

const int *art_DDRModel_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance_art_DDRModel *thisActor=(ActorInstance_art_DDRModel*) pBase;
  while ((1)) {
    if (thisActor->s3==STATE_getAddr) {
      /*** state getAddr ***/

      if (pinAvailIn_int32_t(IN1_WA(thisActor))>=1) {
	int32_t wa;

	TRACE_ACTION(&thisActor->base, 1, "select.write.prefer");

	wa=pinRead_int32_t(IN1_WA(thisActor));
	thisActor->s0_address=wa;
	thisActor->s6_lastWA=wa;
	thisActor->s1_burstSize=96;
	
        thisActor->s3=STATE_doDataWrite; 
      }
      else if (pinAvailIn_int32_t(IN0_RA(thisActor))>=1) {
	/*** RA available ***/
	int32_t ra=pinPeekFront_int32_t(IN0_RA(thisActor));
	if (((ra ^ thisActor->s6_lastWA) & 0x200000)!=0 // frame(RA)!=frame(WA)
	    || (ra & 0x1fc000)==0x1fc000                // y(RA)<0
	    || ra<thisActor->s6_lastWA) {               // WA ahead of RA
	  /*
           * This is the patch which can't be achieved by
           * modifying the CAL source. We are checking that
           * there is sufficient space in the OUTPUT fifo (RD)
           * for an entire read burst. This prevents the case
           * of a blocked read burst that keeps following write
           * bursts from being processed -a situation that may
           * lead to deadlock due to full buffers along the feed-
           * back loop (SearchWindow-Unpack-Interpolate-Add-MBPacker-
           * -DDRModel) when add is forwarding textureOnly.
           */ 
	  if (pinAvailOut_int32_t(OUT0_RD(thisActor))>=96) {
	    int32_t address;
	    int32_t *ptr;
	    int32_t *last;
	    TRACE_ACTION(&thisActor->base, 0, "select.read.prefer");
  
	    address=pinRead_int32_t(IN0_RA(thisActor));
	    
	    /*** state doDataRead follows directly ***/

	    // We have already checked that RD can accomodate 96 tokens
	    // so now we can fire data.read 96 times!
	    ptr=thisActor->s2_buf+RANGECHK(address,MEMSIZE);
	    last=thisActor->s2_buf+RANGECHK(address+95,MEMSIZE);
	    thisActor->s0_address=address+96;
	    while (ptr<=last) {  
	      TRACE_ACTION(&thisActor->base, 4, "data.read");
	      pinWrite_int32_t(OUT0_RD(thisActor), *ptr++);
	    }
	    thisActor->s1_burstSize=0;
	  }
	  else {
	    // Either wait on a WA token *or* 96 spaces for RD tokens
	    pinWaitIn(IN1_WA(thisActor),1);
	    pinWaitOut(OUT0_RD(thisActor),96);
	    return exitcode_block_WA_1_RD_96;
	  }
	}
	else {
	  // Wait for a WA token (write address)
	  pinWaitIn(IN1_WA(thisActor),1);
	  return exitcode_block_WA_1;
	}
      }
      else {
	// Wait for RA or a WA token (read/write address)
	pinWaitIn(IN0_RA(thisActor),1);
	pinWaitIn(IN1_WA(thisActor),1);
	return exitcode_block_RA_1_WA_1;
      }
    }
    else {
      /*** state doDataWrite ***/
      int32_t n=thisActor->s1_burstSize;
      int32_t avail=pinAvailIn_int32_t(IN2_WD(thisActor));
      int32_t address=thisActor->s0_address;
      int32_t *ptr=thisActor->s2_buf+RANGECHK(address,MEMSIZE);
      int32_t *last;

      if (avail<n)
	n=avail;
      if (n==0) {
	pinWaitIn(IN2_WD(thisActor),1);
	return exitcode_block_WD_1;
      }

      last=thisActor->s2_buf+RANGECHK(address+n-1,MEMSIZE);
      do {
	TRACE_ACTION(&thisActor->base, 5, "data.write");
	
	*ptr++=pinRead_int32_t(IN2_WD(thisActor));
      } while (ptr<=last);

      thisActor->s1_burstSize-=n;
      thisActor->s0_address+=n;
      if (thisActor->s1_burstSize==0)
	thisActor->s3=STATE_getAddr;
    }
  }
}

void art_DDRModel_constructor(AbstractActorInstance *pBase) {
  ActorInstance_art_DDRModel *thisActor=(ActorInstance_art_DDRModel*) pBase;
  
  thisActor->s0_address=0;
  thisActor->s1_burstSize=0;
  thisActor->s2_buf=(int32_t*)calloc(MEMSIZE,sizeof(int32_t));
  thisActor->s3=STATE_getAddr;
  thisActor->s6_lastWA=0;
}

void art_DDRModel_destructor(AbstractActorInstance *pBase) {
  ActorInstance_art_DDRModel *thisActor=(ActorInstance_art_DDRModel*) pBase;
 
  free(thisActor->s2_buf);
}


void art_DDRModel_setParam(AbstractActorInstance *pBase, 
			   const char *paramName, 
			   const char *valuee) {
  // MAXW_IN_MB, MAXH_IN_MB hardwired in current implementation
}
