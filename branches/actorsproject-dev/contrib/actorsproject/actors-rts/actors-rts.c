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
#include <assert.h>
#include <sched.h>
#include <pthread.h>
#include <signal.h>
#include <time.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/timeb.h>
#include <stdarg.h>
#include "actors-rts.h"

extern ActorClass		ActorClass_art_Sink;
extern ActorClass		ActorClass_art_Source;
extern char				fileName[];

AbstractActorInstance	*actorInstance[128];
int						actorStatus[128];

int rangeError(int x, int y, char *filename, int line) {
	printf("Range check error: %d %d %s(%d)\n",x,y,filename,line);
	assert(0);
	return 0;
}

static void timestamp(char* buf)
{
	struct timeb	tb;
	struct tm		*tm;
	ftime(&tb);
	tm=localtime(&tb.time);
	sprintf(buf,"%02d:%02d:%02d.%03d> ",tm->tm_hour,tm->tm_min,tm->tm_sec,tb.millitm);
}

static void tprintf(char *buf,char *fmt,va_list ap)
{
	vsprintf(buf+strlen(buf),fmt,ap);
}	

static void mylog(FILE *fd, char *msg,va_list ap)
{
	char		buf1[2048]="";
	if (*msg!='\t') timestamp(buf1);	
	tprintf(buf1,msg,ap);
	if (!strchr(buf1,'\n') && !strchr(buf1,'\t')) strcat(buf1,"\n");
	if(fd)
		fprintf((FILE*)fd,"%s",buf1);
	else
		fprintf(stderr,"%s",buf1);
}

void actorTrace(AbstractActorInstance *base,int level,char *msg,...)
{
	va_list ap;
	if (log_level<level) return;
	va_start(ap,msg);
	mylog(base->fd,msg,ap);
	va_end(ap);
}

void trace(int level,char *msg,...)
{
	va_list ap;
	if (log_level<level) return;
	va_start(ap,msg);
	mylog(0,msg,ap);
	va_end(ap);	
}

int pinPeek(ActorPort *actorPort, int which)
{
	CIRC_BUFFER		*cb;
	int				ret,val=-1;

	if(actorPort->portDir == INPUT)
	{
		ret = pinAvail(actorPort);
		if(ret >= which){
			cb = &circularBuf[actorPort->cid];
			peek_circbuf_area(cb,(char*)&val,actorPort->tokenSize,actorPort->readIndex,which*actorPort->tokenSize);
		}
	}

	return val;	
}

int pinStatus2(ActorPort *actorPort)
{
	CIRC_BUFFER		*cb;
	int				val;

	if(actorPort->portDir == INPUT)
	{
		cb = &circularBuf[actorPort->cid];
		val = get_circbuf_area(cb,actorPort->readIndex);
		actorTrace(actorInstance[actorPort->aid],LOG_EXEC,"%s_%d.in pinAvail[%d]=%d\n",actorInstance[actorPort->aid]->actor->name,actorPort->aid,actorPort->cid,val);
	}
	else
	{
		cb = &circularBuf[actorPort->cid];
		val = get_circbuf_space(cb);
		actorTrace(actorInstance[actorPort->aid],LOG_EXEC,"%s_%d.out pinAvail[%d]=%d\n",actorInstance[actorPort->aid]->actor->name,actorPort->aid,actorPort->cid,val);
	}
	return val;	
}

int pinStatus(ActorPort *actorPort)
{
	int				val;

	val = pinStatus2(actorPort);

	if(val < actorPort->tokenSize)
		return 0;

	return 1;	
}

int pinAvail(ActorPort *actorPort)
{
	int				val;
	
	val = pinStatus2(actorPort);
	
	return (val/actorPort->tokenSize);
}

int pinRead2(ActorPort *actorPort,char *buf,int length)
{
	CIRC_BUFFER		*cb;
	int				ret;
	BLOCK			*bk;
	AbstractActorInstance *instance;
	
	cb = &circularBuf[actorPort->cid];
	ret = read_circbuf(cb,buf,length,actorPort->readIndex);
	if(ret!=0)
	{
		trace(LOG_ERROR,"Read port[%d] error\n",actorPort->cid);
		return -1;
	}
	actorTrace(actorInstance[actorPort->aid],LOG_EXEC,"%s_%d pinRead[%d:%d]=%d\n",actorInstance[actorPort->aid]->actor->name,actorPort->aid,actorPort->cid,actorPort->readIndex,length);

	//signal waiting writer
  	bk = &cb->block;
	instance = actorInstance[bk->aid];

	if(rts_mode != THREAD_PER_ACTOR)
	{
		if(!instance->execState)
			instance->execState = 1;
	}
	else
	{
		pthread_mutex_lock(&instance->mt);
		if(bk->num)
		{
			if (get_circbuf_space(cb) >= bk->num)
			{
				actorTrace(actorInstance[actorPort->aid],LOG_INFO,"%s_%d signal %s_%d to write(%d)\n",actorInstance[actorPort->aid]->actor->name,actorPort->aid,instance->actor->name,bk->aid,actorPort->cid);
				pthread_cond_signal(&instance->cv);
			}
		}
		pthread_mutex_unlock(&instance->mt);
	}
		

	return ret;
}

int pinRead(ActorPort *actorPort)
{
	int				ret;
	int				buf;

	ret = pinRead2(actorPort,(char*)&buf,actorPort->tokenSize);

	if(ret!=0)
	{
		trace(LOG_ERROR,"Read port[%d] error\n",actorPort->cid);
		return -1;
	}

	return buf;
}

int pinWrite2(ActorPort *actorPort,char *buf, int length)
{
	CIRC_BUFFER		*cb;
	int				i,ret;
	BLOCK			*bk;
	AbstractActorInstance *instance;
	
	
	cb = &circularBuf[actorPort->cid];
	ret = write_circbuf(cb,buf,length);

	if(ret!=0){
		trace(LOG_ERROR,"Write port[%d] error\n",actorPort->cid);
		return -1;
	}
	actorTrace(actorInstance[actorPort->aid],LOG_EXEC,"%s_%d pinWrite(%d)=%d\n",actorInstance[actorPort->aid]->actor->name,actorPort->aid,actorPort->cid,length);

	//signal waiting readers
	for(i=0; i<cb->numReaders; i++)
	{
		bk = &cb->reader[i].block;
		instance = actorInstance[bk->aid];

		if(rts_mode != THREAD_PER_ACTOR){
			if(!instance->execState)
				instance->execState = 1;
		}
		else
		{
			pthread_mutex_lock(&instance->mt);
			if(bk->num)
			{
				if(get_circbuf_area(cb,i) >=bk->num)
				{
					actorTrace(actorInstance[actorPort->aid],LOG_INFO,"%s_%d signal %s_%d to read(%d,%d)=%d \n",actorInstance[actorPort->aid]->actor->name,actorPort->aid,instance->actor->name,bk->aid,actorPort->cid,i,get_circbuf_area(cb,i));
					pthread_cond_signal(&instance->cv);
				}
			}
			pthread_mutex_unlock(&instance->mt);
		}
	}

	return 0;
}

int pinWrite(ActorPort *actorPort,int val)
{
	int ret;

	ret = pinWrite2(actorPort,(char*)&val,actorPort->tokenSize);

	return ret;

}

void pinWait(ActorPort *actorPort,int length)
{
	CIRC_BUFFER		*cb;
	BLOCK			*bk;
	AbstractActorInstance *instance;

	if(rts_mode != THREAD_PER_ACTOR)
		return;

	cb = &circularBuf[actorPort->cid];
	if(actorPort->portDir == INPUT)
	{
		bk = &cb->reader[actorPort->readIndex].block;
	}
	else
	{	
		bk = &cb->block;
	}
	instance = actorInstance[actorPort->aid];

	pthread_mutex_lock(&instance->mt);
	actorTrace(instance,LOG_EXEC,"%s_%d.%s pinWait(%d)=%d\n",instance->actor->name,actorPort->aid,(actorPort->portDir == INPUT)?"in":"out",actorPort->cid,length);
	bk->num = length;
	pthread_mutex_unlock(&instance->mt);
}

void init_actor_network(NetworkConfig *network)
{
	CIRC_BUFFER				*cb;
	ActorClass				*ptr;
	ActorPort				*port;
	int						i,j;
	DLLIST					*lnode;
	ActorConfig				**actors;
	AbstractActorInstance	*pInstance;
	int						cid,numFifos;
	int						NumReaderInstances[128];
	int						FifoOutputPortIndex[128];
	int						FifoInputPortIndex[128][128];

	int						listIndex = 0;
	int						numActorsPerList = 0;

	memset(NumReaderInstances,0,sizeof(NumReaderInstances));

	actors = network->networkActors;

	//get number of actors per list in average
	if(rts_mode != THREAD_PER_ACTOR)
	{
		numActorsPerList = network->numNetworkActors/num_lists;
		while(network->numNetworkActors - numActorsPerList*num_lists > numActorsPerList)
			numActorsPerList++;
	}

	//actor port init
	for(i=0; i<network->numNetworkActors; i++)
	{
		
		ptr = actors[i]->actorClass;
		pInstance = (AbstractActorInstance*)malloc(ptr->sizeActorInstance);
		memset(pInstance,0,ptr->sizeActorInstance);
		pInstance->actor = ptr;
		pInstance->aid = i;

		for(j=0; j<actors[i]->numParams; j++){
			if(pInstance->actor->set_param)
				pInstance->actor->set_param(pInstance,&actors[i]->params[j]);
		}

		pInstance->inputPort = (ActorPort*)malloc(sizeof(ActorPort)*ptr->numInputPorts);
		pInstance->outputPort = (ActorPort*)malloc(sizeof(ActorPort)*ptr->numOutputPorts);

		for( j=0; j<ptr->numInputPorts; j++)
		{
			port = &pInstance->inputPort[j];
			cid = actors[i]->inputPorts[j];
			port->cid = cid;
			port->aid = i;
			port->portDir = INPUT;
			port->readIndex = NumReaderInstances[cid];
			port->tokenSize = TOKEN_SIZE;
			FifoInputPortIndex[cid][port->readIndex] = i;
			NumReaderInstances[cid]++;
		}

		for( j=0; j<ptr->numOutputPorts; j++)
		{
			port = &pInstance->outputPort[j];	
			cid = actors[i]->outputPorts[j];
			port->cid = cid;
			port->aid = i;
			port->tokenSize = TOKEN_SIZE;
			port->portDir = OUTPUT;
			FifoOutputPortIndex[cid] = i;
		}

		pthread_mutex_init(&pInstance->mt, NULL);
		pthread_cond_init (&pInstance->cv, NULL);

		pInstance->aid = i;
		pInstance->execState = 1;
		actorStatus[i] = 1;
		actorInstance[i] = pInstance;

		//append to a double linked list
		if(rts_mode != THREAD_PER_ACTOR)
		{
			if(i >= numActorsPerList-1){
				if(i%numActorsPerList == 0){
					if((network->numNetworkActors - 1 - i) > numActorsPerList/2)
						listIndex++;
				}
			}
			lnode = (DLLIST *)malloc(sizeof(DLLIST));
			lnode->obj = pInstance;
			append_node(&actorLists[listIndex],lnode);
			actorLists[listIndex].lid = listIndex;
		}		

	}
	if(rts_mode != THREAD_PER_ACTOR)
	{
		if(num_lists < listIndex + 1){
			num_lists = listIndex + 1;
			trace(LOG_MUST,"Number of list adjusted to %d\n",num_lists); 
		}
	}
		
	//fifo init
	numFifos = network->numFifos;
	for(i=0; i<numFifos; i++)
	{
		cb = &circularBuf[i]; 
		init_circbuf(cb, NumReaderInstances[i]);
		cb->block.aid = FifoOutputPortIndex[i];
		for(j=0; j<NumReaderInstances[i]; j++)
			cb->reader[j].block.aid = FifoInputPortIndex[i][j];
	}
#if 0
	trace(LOG_EXEC,"\nFifo configuration:\n");
 	for(i=0; i<numFifos; i++)
 	{
		cb = &circularBuf[i]; 
		trace(LOG_EXEC,"Fifo[%d]:\n",i);
		trace(LOG_EXEC,"Input from actor: ");
		trace(LOG_EXEC,"%d\n",cb->block.aid);
		trace(LOG_EXEC,"Output to actor : ");
		for(j=0; j<NumReaderInstances[i]; j++)
 			trace(LOG_EXEC,"%d ",cb->reader[j].block.aid);
		trace(LOG_INFO,"\n");
 	}
#endif
}
