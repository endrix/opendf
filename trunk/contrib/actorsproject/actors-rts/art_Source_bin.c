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

typedef struct {
  AbstractActorInstance base;
  int fd;
} ActorInstance;


static void a_action_scheduler(AbstractActorInstance*);
static void constructor(AbstractActorInstance*);
static void destructor(AbstractActorInstance*);
static void set_param(AbstractActorInstance*,ActorParameter*);

ActorClass ActorClass_art_Source_bin ={
  "art_Source_bin",
  0, /* numInputPorts */
  1, /* numOutputPorts */
  sizeof(ActorInstance),
  a_action_scheduler,
  constructor,
  destructor,
  set_param
};

static int read_file(int fd, char *buf,int size)
{
	int ret = 0;
	char tbuf[1];
	int val = 0;

	if(fd){
// 		ret = read(fd,buf,size);
 		ret = read(fd,tbuf,1);
 		val = (int)tbuf[0];
 		memcpy(buf,&val,4);
 		ret *=4;		
	}
	return ret;
}

static void Write0(ActorInstance *thisActor) {
	char		buf[MAX_DATA_LENGTH];
	int			ret;

	ret = read_file(thisActor->fd,buf,thisActor->OUT0_TOKENSIZE);
	if(ret<=0){
		close(thisActor->fd);
		thisActor->fd = 0;
		printf("Source exit!\n");
		actorStatus[thisActor->base.aid]=1;
		pthread_exit(NULL);
	}
	pinWrite2(&thisActor->OUT0_Result,buf,ret);
}

static void a_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance *thisActor=(ActorInstance*) pBase;

	int available;

	while(1)
	{
		available=pinStatus2(&thisActor->OUT0_Result);
		if(available>=thisActor->OUT0_TOKENSIZE)
		{
			Write0(thisActor);	
		}
		else
		{
			pinWait(&thisActor->OUT0_Result,thisActor->OUT0_TOKENSIZE);
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

static void set_param(AbstractActorInstance *pBase,ActorParameter *param){
	ActorInstance *thisActor=(ActorInstance*) pBase;
	if(strcmp(param->key,"fileName") == 0)
	{
		thisActor->fd = (int)(int)open(param->value,O_RDONLY);
	}
}
