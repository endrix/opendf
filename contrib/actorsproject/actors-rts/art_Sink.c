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

#include <string.h>
#include <errno.h>
#include "art_Sink.h"

static const int exitcode_block_In_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};


const int *art_Sink_bin_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance_art_Sink *thisActor=(ActorInstance_art_Sink*) pBase;
  int32_t numTokens;

  if (thisActor->file==0) {
    runtimeError(pBase,"Parameter not set: fileName");
    return EXITCODE_TERMINATE;
  }

  numTokens=pinAvailIn_int32_t(IN0_In(thisActor));
  while (numTokens>=1) {
    do {
      TRACE_ACTION(&thisActor->base, 0, "actionAtLine_7");
      int32_t token=pinRead_int32_t(IN0_In(thisActor));
      fputc(token, thisActor->file);
    } while (--numTokens>0);
    numTokens=pinAvailIn_int32_t(IN0_In(thisActor));
  }

  pinWaitIn(IN0_In(thisActor),1);
  return exitcode_block_In_1;
}


const int *art_Sink_txt_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance_art_Sink *thisActor=(ActorInstance_art_Sink*) pBase;
  int32_t numTokens;

  numTokens=pinAvailIn_int32_t(IN0_In(thisActor));
  while (numTokens>=1) {
    do {
      TRACE_ACTION(&thisActor->base, 0, "actionAtLine_7");
      int32_t token=pinRead_int32_t(IN0_In(thisActor));
      fprintf(thisActor->file, "%d\n", token);
    } while (--numTokens>0);
    numTokens=pinAvailIn_int32_t(IN0_In(thisActor));
  }

  pinWaitIn(IN0_In(thisActor),1);
  return exitcode_block_In_1;
}


const int *art_Sink_real_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance_art_Sink *thisActor=(ActorInstance_art_Sink*) pBase;
  int32_t numTokens;

  numTokens=pinAvailIn_double(IN0_In(thisActor));
  while (numTokens>=1) {
    do {
      TRACE_ACTION(&thisActor->base, 0, "actionAtLine_7");
      double token=pinRead_double(IN0_In(thisActor));
      fprintf(thisActor->file, "%f\n", token);
    } while (--numTokens>0);
    numTokens=pinAvailIn_double(IN0_In(thisActor));
  }

  pinWaitIn(IN0_In(thisActor),1);
  return exitcode_block_In_1;
}

static void constructor(AbstractActorInstance *pBase, FILE *defaultFile) {
  ActorInstance_art_Sink *thisActor=(ActorInstance_art_Sink*) pBase;
  thisActor->file=defaultFile;
}

void art_Sink_bin_constructor(AbstractActorInstance *pBase) {
  constructor(pBase,0);
}

void art_Sink_txt_constructor(AbstractActorInstance *pBase) {
  constructor(pBase,stdout);
}

void art_Sink_real_constructor(AbstractActorInstance *pBase) {
  constructor(pBase,stdout);
}


static void setParam(AbstractActorInstance *pBase, 
		     const char *paramName, 
		     const char *value,
		     const char *fileMode) {
  ActorInstance_art_Sink *thisActor=(ActorInstance_art_Sink*) pBase;
  if (strcmp(paramName,"fileName")==0) {
    thisActor->file=fopen(value,fileMode);
    if (thisActor->file==0)
      runtimeError(pBase,"Cannot open file for output: %s: %s", 
		   value, strerror(errno));
  }
  else
    runtimeError(pBase,"No such parameter: %s", paramName);
}

void art_Sink_bin_setParam(AbstractActorInstance *pBase, 
                           const char *paramName, 
		           const char *value) {
  setParam(pBase,paramName,value,"wb");
}


void art_Sink_txt_setParam(AbstractActorInstance *pBase, 
                           const char *paramName, 
		           const char *value) {
  setParam(pBase,paramName,value,"w");
}

void art_Sink_real_setParam(AbstractActorInstance *pBase, 
                           const char *paramName, 
		           const char *value) {
  setParam(pBase,paramName,value,"w");
}


static void destructor(AbstractActorInstance *pBase) {
  ActorInstance_art_Sink *thisActor=(ActorInstance_art_Sink*) pBase;
  if (thisActor->file!=0 && thisActor->file!=stdout)
    fclose(thisActor->file);
}

void art_Sink_bin_destructor(AbstractActorInstance *pBase) {
  destructor(pBase);
}

void art_Sink_txt_destructor(AbstractActorInstance *pBase) {
  destructor(pBase);
}

void art_Sink_real_destructor(AbstractActorInstance *pBase) {
  destructor(pBase);
}
