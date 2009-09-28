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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sched.h>
#include <pthread.h>
#include <signal.h>
#include <time.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include "actors-rts.h"
#include "circbuf.h"

#define BLOCKED 		1

static int				Running = 1;
int						log_level=LOG_ERROR;

static void stop_run(int sig){
    trace(LOG_MUST,"\nprogram stop running: sig=%x pid=%x\n",sig,getpid());
	Running = 0;
}

static void init_print(int numInstances)
{
	int						i,j;
	AbstractActorInstance	*pInstance;
	ActorPort				*port;

	printf("\nRuntime System Initialization Info:\n");
	for(i=0; i<numInstances; i++)
	{
		pInstance = actorInstance[i];
		printf("\n %d Actor: %s\n",i,pInstance->actor->name);
		printf("Input ports: %d\n",pInstance->actor->numInputPorts);
		printf("[fifo:index] ");
		for(j=0; j<pInstance->actor->numInputPorts; j++){
			port = &pInstance->inputPort[j];
			printf("%d:%d  ",port->cid,port->readIndex);
		}
		printf("\nOutput ports: %d\n",pInstance->actor->numOutputPorts);
		printf("[fifo] ");
		for(j=0; j<pInstance->actor->numOutputPorts; j++){
			port = &pInstance->outputPort[j];
			printf("%d  ",port->cid);	
		}
		printf("\n");
	}
}

static void printstats(int numFifos)
{
	int						i,j;
	CIRC_BUFFER				*cb;
	STATS					*s;

	for (i = 0; i < numFifos; i++)
	{
		printf("Fifo: %d\n",i);
		cb = &circularBuf[i];
		s = &cb->stats;
		printf("  Write:   %ld\n",cb->numWrites);
		printf("  Nospace: %d\n",s->nospace);
		printf("  Read:");
		for(j=0; j<cb->numReaders; j++)
		{
			printf("    %ld[%d] %d",cb->reader[j].numReads,j,get_circbuf_area(cb,j));
		}
		printf("\n  Nodata:  %d\n",s->nodata);
	}	
}

static int wait_on_write(AbstractActorInstance *instance)
{
	CIRC_BUFFER		*cb;
	BLOCK			*bk;
	ActorPort		*port;
	int				i;

	for(i=0; i<instance->actor->numOutputPorts; i++)
	{
		port = &instance->outputPort[i];
		cb = &circularBuf[port->cid];
		bk = &cb->block;
		if(bk->num)
		{
			actorTrace(instance,LOG_INFO,"%s(%d) waiting for write %d:%d:%d\n",instance->actor->name,instance->aid,port->cid,bk->num,get_circbuf_space(cb));
			if(get_circbuf_space(cb) >= bk->num){
				bk->num = 0;
				return 0;
			}
		}
	}
	return 2;	
}

static int wait_on_read(AbstractActorInstance *instance)
{
	CIRC_BUFFER		*cb;
	BLOCK			*bk;
	ActorPort		*port;
	int				i;

	for(i=0; i<instance->actor->numInputPorts; i++)
	{
		port = &instance->inputPort[i];
		cb = &circularBuf[port->cid];
		bk = &cb->reader[port->readIndex].block;
		if(bk->num)
		{
			actorTrace(instance,LOG_INFO,"%s(%d) waiting for read  %d:%d:%d\n",instance->actor->name,instance->aid,port->cid,bk->num,get_circbuf_area(cb,port->readIndex));
			if(get_circbuf_area(cb,port->readIndex) >= bk->num){
				bk->num = 0;
				return 0;
			}	
		}
	}
	return 1;
}


static void exec_unit(AbstractActorInstance *instance)
{
	int				block;

	trace(LOG_MUST,"Actor %s start running......\n",instance->actor->name);

	//constructor
	if(instance->actor->constructor)
		instance->actor->constructor(instance);

	//action scheduler
	if(!instance->actor->action_scheduler)
		pthread_exit(NULL);

	while(Running)
	{
		//actor dispatch
		instance->actor->action_scheduler(instance);

		//check for write&read
		pthread_mutex_lock(&instance->mt);

		block = wait_on_write(instance);
		if(block)
			block = wait_on_read(instance);

		if(block)
		{
 			actorTrace(instance,LOG_EXEC,"%s[%d] waiting on write/read\n",instance->actor->name,instance->aid);
			actorStatus[instance->aid]=block;
			pthread_cond_wait(&instance->cv, &instance->mt);
			actorStatus[instance->aid]=0;
			actorTrace(instance,LOG_INFO,"%s[%d] wakeup\n",instance->actor->name,instance->aid);
		}
		pthread_mutex_unlock(&instance->mt);
	}

	pthread_exit(NULL);
}

int actors_status(int numInstances)
{
	int i;
	int ret = 1;

	for(i=0; i<numInstances; i++)
	{
		if(actorStatus[i] == 0)
		{
			ret = 0;
			break;
		}
	}

	if(ret)
	{
		trace(LOG_MUST,"Following actors are blocked on the output port:\n");
		for(i=0; i<numInstances; i++)
		{
			if(actorStatus[i] == 2)
				trace(LOG_MUST,"Actor[%d] %s\n",i,actorInstance[i]->actor->name);
		}
	}
	return ret;
}
int execute_network(int argc, char *argv[],NetworkConfig *networkConfig)
{
	int			i;
	int			numInstances,numFifos;
	int			interval=2;
	int			count=0;
	pthread_attr_t attr;

	//catch prog faults
	signal(SIGINT, stop_run);

	numInstances = networkConfig->numNetworkActors;
	numFifos = networkConfig->numFifos;

	trace(LOG_INFO,"numInstances: %d numFifos: %d\n",numInstances,numFifos);

	init_actor_network(networkConfig);
	
	if(log_level >=LOG_INFO) 
		init_print(numInstances);

	/* For portability, explicitly create threads in a joinable state */
	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

	//actor thread
	for(i=0;i<numInstances;i++)
		pthread_create((pthread_t*)&actorInstance[i]->tid, &attr, (void*)exec_unit, (void *) actorInstance[i]);

 	while(Running)
 	{
		count++;
 		sleep(interval);
		if(actors_status(numInstances) == BLOCKED)
		{
 			Running = 0;
			trace(LOG_MUST,"All the actors got blocked, ready to exit\n");
		}
		if(log_level >=LOG_INFO) 
			printstats(numFifos);
 	}	

	for (i=0; i<numInstances; i++) {
		if(actorInstance[i]->actor->destructor){
			actorInstance[i]->actor->destructor(actorInstance[i]);
		}
	}

	return 0;
}
