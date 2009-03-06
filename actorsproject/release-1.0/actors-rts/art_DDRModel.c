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
 * Actor DDRModel (ActorClass_art_DDRModel_0)
 * Generated on Fri Jan 16 17:00:10 CET 2009 from art_DDRModel_0.xlim
 * by xlim2c version 0.4 (Jan 14, 2009)
 */

#include "actors-rts.h"

#define IN0_RA base.inputPort[0]
#define IN1_WA base.inputPort[1]
#define IN2_WD base.inputPort[2]
#define OUT0_RD base.outputPort[0]

/*
 * When compiling from the source art_DDRModel.cal we patch 
 * two things manually:
 * 1) The size of the frame buffer (MEMSIZE), since the SSAGenerator
 *    produces an inconveniently large initializer (4M XML elements),
 *    which leads to out-of-heap-memory on many machines
 * 2) There is a timing-dependence that may lead to deadlock.
 */

#define MEMSIZE 4194304

typedef struct {
  AbstractActorInstance base;
  int s0_address;
  int s1_burstSize;
  int s2_buf[MEMSIZE];  /*** patched MEMSIZE ***/
  int s3;
  int s4;
  int s5;
} ActorInstance;


static void a0_select_read(ActorInstance *);
static void a1_select_write(ActorInstance *);
static void a2_data_read(ActorInstance *);
static void a3_data_write(ActorInstance *);
static void a4_action_scheduler(ActorInstance *);
static void constructor(AbstractActorInstance*);

ActorClass ActorClass_art_DDRModel ={
  "DDRModel",
  3, /* numInputPorts */
  1, /* numOutputPorts */
  sizeof(ActorInstance),
  (void*)a4_action_scheduler,
  constructor
};


static void a0_select_read(ActorInstance *thisActor) {
  TRACE_ACTION(&thisActor->base, 0, "select.read");
  int32_t t0;
  t0=pinRead(&thisActor->IN0_RA);
  thisActor->s0_address=t0;
  thisActor->s1_burstSize=(96);
}

static void a1_select_write(ActorInstance *thisActor) {
  TRACE_ACTION(&thisActor->base, 1, "select.write");
  int32_t t7;
  t7=pinRead(&thisActor->IN1_WA);
  thisActor->s0_address=t7;
  thisActor->s1_burstSize=(96);
}

static void a2_data_read(ActorInstance *thisActor) {
  TRACE_ACTION(&thisActor->base, 2, "data.read");
  int32_t t15,t19,t23,t29;
  t15=thisActor->s0_address;
  t19=thisActor->s2_buf[RANGECHK((((1)*t15)),MEMSIZE)]; /*** MEMSIZE ***/
  t23=thisActor->s0_address;
  t29=thisActor->s1_burstSize;
  pinWrite(&thisActor->OUT0_RD,t19);
  thisActor->s0_address=(((int32_t) (((int64_t) t23)+((int64_t) (1)))));
  thisActor->s1_burstSize=(((int32_t) (((int64_t) t29)-((int64_t) (1)))));
}

static void a3_data_write(ActorInstance *thisActor) {
  TRACE_ACTION(&thisActor->base, 3, "data.write");
  int32_t t38,t39,t43,t49;
  t38=pinRead(&thisActor->IN2_WD);
  t39=thisActor->s0_address;
  t43=thisActor->s1_burstSize;
  t49=thisActor->s0_address;
  thisActor->s2_buf[RANGECHK(((1)*t39),MEMSIZE)]=t38; /*** MEMSIZE ***/
  thisActor->s1_burstSize=(((int32_t) (((int64_t) t43)-((int64_t) (1)))));
  thisActor->s0_address=(((int32_t) (((int64_t) t49)+((int64_t) (1)))));
}

static void a4_action_scheduler(ActorInstance *thisActor) {
  while (1) {
    bool_t t70,t75,t81,t94,t97,t107,t108,t109;
    int32_t t66,t67,t68,t79,t89,t95,t101;
    if (!(1)) break;
    t66=pinAvail(&thisActor->IN0_RA);
    t67=pinAvail(&thisActor->IN1_WA);
    t68=pinAvail(&thisActor->IN2_WD);
    t70=t66>=(1);
    t75=t67>=(1);
    t79=thisActor->s1_burstSize;
    t81=t79>(0);
    t89=thisActor->s1_burstSize;
    t94=(t68>=(1)) && (t89>(0));
    t95=thisActor->s1_burstSize;
    t97=t95==(0);
    t101=pinAvail(&thisActor->OUT0_RD);
    t107=thisActor->s3;
    t108=thisActor->s4;
    t109=thisActor->s5;
    if (t107) {  
      /*** fsm-state=getAddr ***/
      if (t75) {
        /*** WA available ***/
        a1_select_write(thisActor);
        thisActor->s3=(0);
        thisActor->s5=(1);
      }
      else {
        if (t70) {
          /*** RA available ***/
          if (t101 >= 96) {
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
            a0_select_read(thisActor);
            thisActor->s3=(0);
            thisActor->s4=(1);
          }
          else {
            pinWait(&thisActor->IN1_WA,sizeof(int));
            pinWait(&thisActor->OUT0_RD,96*sizeof(int)); return;
          }
        }
        else {
          pinWait(&thisActor->IN0_RA,sizeof(int));
          pinWait(&thisActor->IN1_WA,sizeof(int)); return;
        }
      }
    }
    else {
      if (t108) {
        /*** fsm-state=doDataRead ***/
        if (t81) {
          /*** burstSize > 0 ***/
          if (t101 >= 1) {
            /*** space (one token) available in RD */
            a2_data_read(thisActor);
            thisActor->s4=(1);
          }
          else {
            pinWait(&thisActor->OUT0_RD,sizeof(int)); return;
          }
        }
        else {
          /*** burstSize==0 ***/
          thisActor->s4=(0);
          thisActor->s3=(1);
        }
      }
      else {
        if (t109) {
          /*** fsm-state=doDataWrite ***/
          if (t94) {
            /*** WD available and burstSize>0 */ 
            a3_data_write(thisActor);
            thisActor->s5=(1);
          }
          else {
            if (t97) {
              /*** burstSize=0 ***/
              thisActor->s5=(0);
              thisActor->s3=(1);
            }
            else {
              pinWait(&thisActor->IN2_WD,sizeof(int)); return;
            }
          }
        }
      }
    }
  }
}

static void constructor(AbstractActorInstance *pBase) {
  ActorInstance *thisActor=(ActorInstance*) pBase;
  thisActor->s0_address=0;
  thisActor->s1_burstSize=0;
  memset(thisActor->s2_buf, 0, MEMSIZE*sizeof(int)); /*** MEMSIZE ***/
  thisActor->s3=1;
  thisActor->s4=0;
  thisActor->s5=0;
}
