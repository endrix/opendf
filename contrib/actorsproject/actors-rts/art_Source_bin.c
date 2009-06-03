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
 * Actor Source
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <pthread.h>
#include "actors-rts.h"

#define OUT0_Result				base.outputPort[0]
#define OUT0_TOKENSIZE			base.outputPort[0].tokenSize

#define TOKENSIZE_IN_INT32		8

typedef struct {
  AbstractActorInstance base;
  int fd;
} ActorInstance;


static void a_action_scheduler(AbstractActorInstance*);
static void constructor(AbstractActorInstance*);
static void destructor(AbstractActorInstance*);
static void set_param(AbstractActorInstance*,const char*,const char*);

// TODO: TOKENSIZE_IN_INT32 prevents us from type checking inputs/outputs
// The token size is not really 8*sizeof(int32_t), we are writing 8 tokens
static const PortDescription outputPortDescriptions[]={
  {"Out", TOKENSIZE_IN_INT32*sizeof(int32_t)}
};

static const int production[] = { TOKENSIZE_IN_INT32 };

static const ActionDescription actionDescriptions[] = {
  {0, 0, production}
};


ActorClass ActorClass_art_Source_bin ={
  "art_Source_bin",
  0, /* numInputPorts */
  1, /* numOutputPorts */
  sizeof(ActorInstance),
  a_action_scheduler,
  constructor,
  destructor,
  set_param,
  0, /* inputPortDescriptions */
  outputPortDescriptions,
  0, /* actorExecMode */
  1, /* numActions */
  actionDescriptions
};

static int read_file(int fd, int32_t *buf,int size)
{
	int num = 0;
	char tbuf[1024];
	int i;
	int32_t *pbuf = buf;

	if(fd){
		num = read(fd,tbuf,size);
		if(num>0)
		{
			for(i=0;i<num;i++,pbuf++)
			{
				*pbuf = (int32_t)tbuf[i];
			}
		}
	}
	return num;
}

static int Write0(ActorInstance *thisActor) {
	int32_t		buf[TOKENSIZE_IN_INT32];
	int			i,ret;

	ret = read_file(thisActor->fd,buf,TOKENSIZE_IN_INT32);
	
	if(ret<=0){
		stop_run();
	}else{
		for(i=0; i<ret; i++){
			pinWrite_int32_t(&thisActor->OUT0_Result,buf[i]);
		}
	}
	return ret;
}

static void a_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance *thisActor=(ActorInstance*) pBase;

	int ret;

	while(1)
	{
		if(pinAvailOut(&thisActor->OUT0_Result)>=TOKENSIZE_IN_INT32)
		{
			ret = Write0(thisActor);	
			if(ret <= 0)
				return;
		}
		else
		{
			pinWaitOut(&thisActor->OUT0_Result,thisActor->OUT0_TOKENSIZE);
			return;
		}
	}
}

static void constructor(AbstractActorInstance *pBase) {

}

static void destructor(AbstractActorInstance *pBase)
{
	ActorInstance *thisActor=(ActorInstance*) pBase;
	if(thisActor->fd)
		close(thisActor->fd);
}

static void set_param(AbstractActorInstance *pBase,const char *key, const char *value){
	ActorInstance *thisActor=(ActorInstance*) pBase;
	thisActor->fd = 0;
	if(strcmp(key,"fileName") == 0)
	{
		thisActor->fd = (int)open(value,O_RDONLY);
	}
}
