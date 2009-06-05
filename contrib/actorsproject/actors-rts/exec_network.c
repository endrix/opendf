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

#include <ctype.h>
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
#include <sys/stat.h>
#include <sched.h>
#include <syscall.h>
#include <sys/timeb.h>
#include <stdarg.h>
#include "actors-rts.h"

int						log_level = LOG_ERROR;


int rangeError(int x, int y, const char *filename, int line) {
	printf("Range check error: %d %d %s(%d)\n",x,y,filename,line);
	assert(0);
	return 0;
}

void runtimeError(AbstractActorInstance *pInst, const char *format,...) {
  va_list ap;
  va_start(ap,format);
  vfprintf(stderr,format,ap);
  fprintf(stderr,"\n");
  va_end(ap);
  // TODO: (1) here we should terminate, somehow
  //       (2) range errors should be reported the same way
  //           file/line number is nice (pInst not used)
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

void trace(int level,const char *msg,...)
{
	va_list ap;
	if (log_level<level) return;
	va_start(ap,msg);
	mylog(0,msg,ap);
	va_end(ap);	
}

AbstractActorInstance *createActorInstance(ActorClass *actorClass) {
	int instanceSize = actorClass->sizeActorInstance;
	AbstractActorInstance *pInstance =
        (AbstractActorInstance*)calloc(1,instanceSize);

	pInstance->actor = actorClass;
	pInstance->inputPort = (InputPort*)
        calloc(actorClass->numInputPorts,sizeof(InputPort));
	pInstance->outputPort = (OutputPort*)
		calloc(actorClass->numOutputPorts,sizeof(OutputPort));

	if(actorClass->constructor){
		actorClass->constructor(pInstance);
	}
	return pInstance;
}

void setParameter(AbstractActorInstance *pInstance,
                  const char *key,
                  const char *value) {
	//setup parameters if any
	if(pInstance->actor->set_param)
		pInstance->actor->set_param(pInstance,key,value);
}

OutputPort *createOutputPort(AbstractActorInstance *pInstance,
                             const char *portName,
							 int numberOfReaders) {
	ActorClass *actorClass=pInstance->actor;
	int numOutputPorts=actorClass->numOutputPorts;
	const PortDescription *descr=actorClass->outputPortDescriptions;
	int j;

	for (j=0; j<numOutputPorts; ++j)
		if (strcmp(portName, descr[j].name)==0) {
			OutputPort *port=&pInstance->outputPort[j];
			port->numReaders=numberOfReaders;
			port->readers=(InputPort**)
				calloc(numberOfReaders, sizeof(InputPort*));

			return port;
		}

	return 0;
}

InputPort *createInputPort(AbstractActorInstance *pInstance,
                           const char *portName,
						   int capacity) {
ActorClass *actorClass=pInstance->actor;
	int numInputPorts=actorClass->numInputPorts;
	const PortDescription *descr=actorClass->inputPortDescriptions;
	int j;

	if (capacity==0)
		capacity=DEFAULT_FIFO_LENGTH;

	for (j=0; j<numInputPorts; ++j)
		if (strcmp(portName, descr[j].name)==0) {
			InputPort *port=&pInstance->inputPort[j];
			int size=capacity*descr[j].tokenSize;
			char *buffer=malloc(size);

			port->bufferStart=buffer;
  			port->bufferEnd=buffer+size;
  			port->readPtr=buffer;
  			port->writePtr=buffer;
  			port->numWritten=0;
  			port->numRead=0;
  			port->capacity=capacity;

			return port;
		}

	return 0;
}


void connectPorts(OutputPort *outputPort, InputPort *inputPort) {
	int numReaders=outputPort->numReaders;
	int j = 0;

	while (j<numReaders && outputPort->readers[j]!=0)
	 	++j;
	if (j<numReaders)
		outputPort->readers[j] = inputPort;
}

int runActorBarebone(AbstractActorInstance **actorInstance,int numInstances)
{
	int i;
	AbstractActorInstance *pInstance;
	int networkAliveHack;

	do {
		networkAliveHack=0;
		for (i=0; i<numInstances; i++) {
			pInstance=actorInstance[i];
			pInstance->hasFiredHack=0;
			pInstance->actor->action_scheduler(pInstance);
			if (pInstance->hasFiredHack)
			  networkAliveHack=1;
		}
	} while (networkAliveHack);
	
	return 0;
}

int executeNetwork(int argc, char *argv[],AbstractActorInstance **actorInstance, int numInstances)
{
	int i;
	trace(LOG_MUST,"ModuleName            : %s\n",argv[0]);
	trace(LOG_MUST,"numInstances          : %d\n",numInstances);

	runActorBarebone(actorInstance,numInstances);

    for (i=0; i<numInstances; i++) {
        if(actorInstance[i]->actor->destructor){
            actorInstance[i]->actor->destructor(actorInstance[i]);
        }
    }
	trace(LOG_MUST,"Exit\n");

	return 0;
}
