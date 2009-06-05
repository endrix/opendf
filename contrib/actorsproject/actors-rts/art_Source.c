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
#include "art_Source.h"

static const int exitcode_block_Out_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};

#define NUM_TOKENS_TO_READ 512

const int *art_Source_bin_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance_art_Source *thisActor=(ActorInstance_art_Source*) pBase;
  char buf[NUM_TOKENS_TO_READ];

  int space;

  if (thisActor->file==0) {
    runtimeError(pBase,"Parameter not set: fileName");
    return EXITCODE_TERMINATE;
  }

  space=pinAvailOut_int32_t(OUT0_Out(thisActor));
  while (space>=1) {
    do {
      int i;
      int n=(space>NUM_TOKENS_TO_READ)? NUM_TOKENS_TO_READ : space;
      n=fread(buf,sizeof(char),n,thisActor->file);
      if (n==0)
	return EXITCODE_TERMINATE;

      for (i=0; i<n; ++i) {
	TRACE_ACTION(&thisActor->base, 0, "actionAtLine_7");
	pinWrite_int32_t(OUT0_Out(thisActor), buf[i]);
      }
      space-=n;
    } while (space>0);
    space=pinAvailOut_int32_t(OUT0_Out(thisActor));
  }

  pinWaitOut(OUT0_Out(thisActor),1);
  return exitcode_block_Out_1;
}


const int *art_Source_txt_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance_art_Source *thisActor=(ActorInstance_art_Source*) pBase;
  int space;

  if (thisActor->file==0) {
    runtimeError(pBase,"Parameter not set: fileName");
    return EXITCODE_TERMINATE;
  }

  space=pinAvailOut_int32_t(OUT0_Out(thisActor));
  while (space>=1) {
    do {
      int token;
      int n=fscanf(thisActor->file,"%d",&token);
      if (n<=0)
	return EXITCODE_TERMINATE;

      TRACE_ACTION(&thisActor->base, 0, "actionAtLine_7");
      pinWrite_int32_t(OUT0_Out(thisActor), token);
    } while (--space>0);
    space=pinAvailOut_int32_t(OUT0_Out(thisActor));
  }

  pinWaitOut(OUT0_Out(thisActor),1);
  return exitcode_block_Out_1;
}


const int *art_Source_real_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance_art_Source *thisActor=(ActorInstance_art_Source*) pBase;
  int space;

  if (thisActor->file==0) {
    runtimeError(pBase,"Parameter not set: fileName");
    return EXITCODE_TERMINATE;
  }

  space=pinAvailOut_double(OUT0_Out(thisActor));
  while (space>=1) {
    do {
      double token;
      int n=fscanf(thisActor->file,"%lf",&token);
      if (n<=0)
	return EXITCODE_TERMINATE;

      TRACE_ACTION(&thisActor->base, 0, "actionAtLine_7");
      pinWrite_double(OUT0_Out(thisActor), token);
    } while (--space>0);
    space=pinAvailOut_double(OUT0_Out(thisActor));
  }

  pinWaitOut(OUT0_Out(thisActor),1);
  return exitcode_block_Out_1;
}


static void constructor(AbstractActorInstance *pBase) {
  ActorInstance_art_Source *thisActor=(ActorInstance_art_Source*) pBase;
  thisActor->file=0;
}

void art_Source_bin_constructor(AbstractActorInstance *pBase) {
  constructor(pBase);
}

void art_Source_txt_constructor(AbstractActorInstance *pBase) {
  constructor(pBase);
}

void art_Source_real_constructor(AbstractActorInstance *pBase) {
  constructor(pBase);
}


static void setParam(AbstractActorInstance *pBase, 
		     const char *paramName, 
		     const char *value,
		     const char *fileMode) {
  ActorInstance_art_Source *thisActor=(ActorInstance_art_Source*) pBase;
  if (strcmp(paramName,"fileName")==0) {
    thisActor->file=fopen(value,fileMode);
    if (thisActor->file==0)
      runtimeError(pBase,"Cannot open file for input: %s: %s", 
		   value, strerror(errno));
  }
  else
    runtimeError(pBase,"No such parameter: %s", paramName);
}

void art_Source_bin_setParam(AbstractActorInstance *pBase, 
                           const char *paramName, 
		           const char *value) {
  setParam(pBase,paramName,value,"rb");
}

void art_Source_txt_setParam(AbstractActorInstance *pBase, 
                           const char *paramName, 
		           const char *value) {
  setParam(pBase,paramName,value,"r");
}

void art_Source_real_setParam(AbstractActorInstance *pBase, 
                           const char *paramName, 
		           const char *value) {
  setParam(pBase,paramName,value,"r");
}


static void destructor(AbstractActorInstance *pBase) {
  ActorInstance_art_Source *thisActor=(ActorInstance_art_Source*) pBase;
  if (thisActor->file!=0)
    fclose(thisActor->file);
}

void art_Source_bin_destructor(AbstractActorInstance *pBase) {
  destructor(pBase);
}

void art_Source_txt_destructor(AbstractActorInstance *pBase) {
  destructor(pBase);
}

void art_Source_real_destructor(AbstractActorInstance *pBase) {
  destructor(pBase);
}

