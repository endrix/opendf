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
 * Actor Sink
 */

#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include "actors-rts.h"

#define IN0_A					base.inputPort[0]
#define IN0_TOKENSIZE			base.inputPort[0].tokenSize

typedef struct {
  AbstractActorInstance base;
  FILE *fd;
} ActorInstance;


static void a_action_scheduler(AbstractActorInstance*);
static void constructor(AbstractActorInstance*);
static void destructor(AbstractActorInstance*);
static void set_param(AbstractActorInstance*,ActorParameter*);


ActorClass ActorClass_art_Sink_txt ={
  "art_Sink_txt",
  1, /* numInputPorts */
  0, /* numOutputPorts */
  sizeof(ActorInstance),
  a_action_scheduler,
  constructor,
  destructor,
  set_param
};

static void Read0(ActorInstance *thisActor) {
	int			ret = -1;
	int			val;
	static int	count;

	if (thisActor->IN0_TOKENSIZE == sizeof(val));
		ret = pinRead2(&thisActor->IN0_A,(char*)&val,thisActor->IN0_TOKENSIZE);
	if(ret == 0)
	{
		if(thisActor->fd)
		{
			fprintf((FILE*)thisActor->fd,"%d\n",val);
		}
		else
		{
			printf("%d %d\n",count++,val);
		}
	}
}

static void a_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance *thisActor=(ActorInstance*) pBase;
	int available;

	while(1)
	{
		available=pinStatus2(&thisActor->IN0_A);
		if(available>=thisActor->IN0_TOKENSIZE)
		{
			Read0(thisActor);	
		}
		else
		{
			pinWait(&thisActor->IN0_A,thisActor->IN0_TOKENSIZE);
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

static void set_param(AbstractActorInstance *pBase,ActorParameter *param){
	ActorInstance *thisActor=(ActorInstance*) pBase;
	if(strcmp(param->key,"fileName") == 0)
	{
		thisActor->fd = (FILE*)fopen(param->value,"w");
	}
}