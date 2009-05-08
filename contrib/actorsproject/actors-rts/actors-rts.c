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

#define internalError(a)	{printf("Error %s(%d): %s\n",__FILE__,__LINE__,a);assert(0);}

AbstractActorInstance	*actorInstance[MAX_ACTOR_NUM];
int						actorStatus[MAX_ACTOR_NUM];

int rangeError(int x, int y, const char *filename, int line) {
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

static void tprintf(char *buf,const char *fmt,va_list ap)
{
	vsprintf(buf+strlen(buf),fmt,ap);
}	

static void mylog(FILE *fd, const char *msg,va_list ap)
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

void actorTrace(AbstractActorInstance *base,int level,const char *msg,...)
{
	va_list ap;
	if (log_level<level) return;
	va_start(ap,msg);
	mylog(base->fd,msg,ap);
	va_end(ap);
}

void trace(int level,const char *msg,...)
{
	va_list ap;
	if (log_level<level) return;
	va_start(ap,msg);
	mylog(0,msg,ap);
	va_end(ap);	
}

int pinPeek2(ActorPort *actorPort, char *buf, int offset)
{
	CIRC_BUFFER		*cb;
	int				ret=-1;

	if(actorPort->portDir == INPUT)
	{
		ret = pinAvail(actorPort);
		if(ret >= offset){
			cb = &circularBuf[actorPort->cid];
			peek_circbuf_area(cb,buf,actorPort->tokenSize,actorPort->readIndex,offset*actorPort->tokenSize);
		}
		else
			internalError("Not enough data for peeking");
	}

	if(ret<0)
		internalError("Invalid port for pinPeek");

	return ret;	
}

int pinPeek(ActorPort *actorPort, int offset)
{
	int			val;

	pinPeek2(actorPort, (char*)&val, offset);

	return val;
}

int32_t pinPeek_int32_t(ActorPort *actorPort, int offset)
{
	int32_t			val;

	pinPeek2(actorPort, (char*)&val, offset);

	return val;
}

double pinPeek_double(ActorPort *actorPort, int offset)
{
	double			val;

	pinPeek2(actorPort, (char*)&val, offset);

	return val;
}

bool_t pinPeek_bool_t(ActorPort *actorPort, int offset)
{
	bool_t			val;

	pinPeek2(actorPort, (char*)&val, offset);

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
	else
		return 1;	
}

int pinAvail(ActorPort *actorPort)
{
	int				val;
	
	val = pinStatus2(actorPort);

	return (val/actorPort->tokenSize);
}

int pinAvail_int32_t(ActorPort *actorPort)
{
	return pinAvail(actorPort);
}

int pinAvail_double(ActorPort *actorPort)
{
	return pinAvail(actorPort);
}

int pinAvail_bool_t(ActorPort *actorPort)
{
	return pinAvail(actorPort);
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
		internalError("pinRead2 error");
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

double pinRead_double(ActorPort *actorPort)
{
	double			val;

	pinRead2(actorPort,(char*)&val,actorPort->tokenSize);

	return val;
}

int32_t pinRead_int32_t(ActorPort *actorPort)
{
	int32_t			val;

	pinRead2(actorPort,(char*)&val,actorPort->tokenSize);

	return val;
}

bool_t pinRead_bool_t(ActorPort *actorPort)
{
	bool_t			val;

	pinRead2(actorPort,(char*)&val,actorPort->tokenSize);

	return val;
}

int pinRead(ActorPort *actorPort)
{
	int				val;

	pinRead2(actorPort,(char*)&val,actorPort->tokenSize);

	return val;
}

int pinWrite2(ActorPort *actorPort,const char *buf, int length)
{
	CIRC_BUFFER		*cb;
	int				i,ret;
	BLOCK			*bk;
	char			msg[218];
	AbstractActorInstance *instance;
	
	
	cb = &circularBuf[actorPort->cid];
	ret = write_circbuf(cb,buf,length);

	if(ret!=0){
		sprintf(msg,"Write port[%d] error",actorPort->cid);
		internalError(msg);
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

int pinWrite_double(ActorPort *actorPort,double val)
{
	return pinWrite2(actorPort,(char*)&val,actorPort->tokenSize);
}

int pinWrite_int32_t(ActorPort *actorPort,int32_t val)
{
	return pinWrite2(actorPort,(char*)&val,actorPort->tokenSize);
}

int pinWrite_byte(ActorPort *actorPort,int val)
{
	return pinWrite2(actorPort,(char*)&val,actorPort->tokenSize);
}

int pinWrite_bool_t(ActorPort *actorPort,bool_t val)
{
	return pinWrite2(actorPort,(char*)&val,actorPort->tokenSize);
}

int pinWrite(ActorPort *actorPort,int val)
{
	return pinWrite2(actorPort,(char*)&val,actorPort->tokenSize);
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
