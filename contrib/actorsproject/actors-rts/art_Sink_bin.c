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

#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include "actors-rts.h"

#define IN0_A					base.inputPort[0]
#define IN0_TOKENSIZE			base.inputPort[0].tokenSize

typedef struct {
  AbstractActorInstance base;
  int fd;
} ActorInstance;


static void a_action_scheduler(AbstractActorInstance*);
static void constructor(AbstractActorInstance*);
static void destructor(AbstractActorInstance*);
static void set_param(AbstractActorInstance*,int,ActorParameter*);

static const PortDescription inputPortDescriptions[]={
  {"In", sizeof(int32_t)}
};

static const int consumption[] = { 1 };

static const ActionDescription actionDescriptions[] = {
  {0, consumption, 0}
};

ActorClass ActorClass_art_Sink_bin ={
  "art_Sink_bin",
  1, /* numInputPorts */
  0, /* numOutputPorts */
  sizeof(ActorInstance),
  a_action_scheduler,
  constructor,
  destructor,
  set_param,
  inputPortDescriptions,
  0, /* outputPortDescriptions */
  0, /* actorExecMode */
  1, /* numActions */
  actionDescriptions
};

static void Read0(ActorInstance *thisActor) {
	int			ret;
	int			val;
	char		ch;

	ret = pinRead2(&thisActor->IN0_A,(char*)&val,thisActor->IN0_TOKENSIZE);
	if(thisActor->fd)
	{
		ch = (char)val;
		write(thisActor->fd,&ch,1);
	}
}

static void a_action_scheduler(AbstractActorInstance *pBase) {
  ActorInstance *thisActor=(ActorInstance*) pBase;

	while(1)
	{
		if(pinAvailIn_int32_t(&thisActor->IN0_A)>=1)
		{
			Read0(thisActor);	
		}
		else
		{
			pinWaitIn(&thisActor->IN0_A,thisActor->IN0_TOKENSIZE);
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

static void set_param(AbstractActorInstance *pBase,int numParams,ActorParameter *param){
	ActorInstance *thisActor=(ActorInstance*) pBase;
	ActorParameter *p;
	int	i;
	thisActor->fd = 0;
	for(i=0,p=param; i<numParams; i++,p++)
	{
		if(strcmp(p->key,"fileName") == 0)
		{
			thisActor->fd = (int)open(p->value,O_CREAT|O_TRUNC|O_RDWR,S_IREAD|S_IWRITE);
		}
	}
}
