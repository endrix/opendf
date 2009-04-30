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
  FILE *fd;
} ActorInstance;


static void a_action_scheduler(AbstractActorInstance*);
static void constructor(AbstractActorInstance*);
static void destructor(AbstractActorInstance*);
static void set_param(AbstractActorInstance*,int,ActorParameter*);

ActorClass ActorClass_art_Source_txt ={
  "art_Source_txt",
  0, /* numInputPorts */
  1, /* numOutputPorts */
  sizeof(ActorInstance),
  a_action_scheduler,
  constructor,
  destructor,
  set_param
};

static int read_file(FILE *fd, char *buf,int size)
{
	int ret = EOF;

	if(fd){

 		ret = fscanf((FILE*)fd,"%d",(int*)buf);
		if(ret != EOF){
			ret *= sizeof(int);
		}
	}
	return ret;
}

static int Write0(ActorInstance *thisActor) {
	char		buf[MAX_DATA_LENGTH];
	int			ret;

	ret = read_file(thisActor->fd,buf,thisActor->OUT0_TOKENSIZE);
	if(ret == EOF){
		if(rts_mode == THREAD_PER_ACTOR)
		{
 			fclose((FILE*)thisActor->fd);
 			thisActor->fd = 0;
 			printf("Source %s exit!\n",thisActor->base.actor->name);
			actorStatus[thisActor->base.aid]=0;
 			pthread_exit(NULL);
		}
		else
			thisActor->base.execState = 0;
	}else
		pinWrite2(&thisActor->OUT0_Result,buf,ret);

	return ret;
}

static void a_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance *thisActor=(ActorInstance*) pBase;

	int available;
	int	ret;

	while(1)
	{
		available=pinStatus2(&thisActor->OUT0_Result);
		if(available>=thisActor->OUT0_TOKENSIZE)
		{
			ret = Write0(thisActor);
			if (ret == EOF)
				return;
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
		fclose((FILE*)thisActor->fd);
}

static void set_param(AbstractActorInstance *pBase,int numParams,ActorParameter *param){
	ActorInstance *thisActor=(ActorInstance*) pBase;
	ActorParameter *p;
	int	i;
	for(i=0,p=param; i<numParams; i++,p++)
	{
		if(strcmp(p->key,"fileName") == 0)
		{
			thisActor->fd = (FILE *)fopen(param->value,"r");
		}
	}
}
