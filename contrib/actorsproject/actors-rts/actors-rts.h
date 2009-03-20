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

#ifndef _ACTORS_RTS_H
#define _ACTORS_RTS_H

#include <string.h>
#include <stdint.h>
#include <stdio.h>
#include <semaphore.h>
#include "circbuf.h"
#include "dll.h"

#define INPUT				0
#define OUTPUT				1
#define INTERNAL			0
#define EXTERNAL			1
#define COPY(a)				a
#define MAX_DATA_LENGTH		1024

#define TRACE_ACTION(INSTANCE,INDEX,NAME) \
        if(trace_action) trace(LOG_MUST,"%s %d %s\n", (INSTANCE)->actor->name, INDEX, NAME)

#define RANGECHK(X,B) ((unsigned)(X)<(unsigned)(B)?(X):RANGEERR(X,B))
#define RANGEERR(X,B) (rangeError((X),(B),__FILE__,__LINE__))

#define	LOG_MUST			0			//must log this
#define	LOG_ERROR			1			//only log errors
#define	LOG_WARN			2			//also log warnings
#define	LOG_INFO			3			//also log info
#define	LOG_EXEC			4			//also log func exec
#define	LOG_STOP			(-99)		//disable log file

#define	RTS_VERSION			"1.1.0"

#define THREAD_PER_ACTOR	1
#define THREAD_PER_LIST		2
#define SINGLE_LIST			3

typedef int					bool_t;

typedef struct {
	char			*key;
	char			*value;
}ActorParameter;

typedef struct {
	int				aid;					//back link to actor ID
	int				cid;					//circular buffer ID
	int				tokenSize;				//token size	
	int				portDir;				//0-input 1-output
	int				readIndex;				//the reader index 
}ActorPort;

typedef struct ActorClass ActorClass;

typedef struct {
	pthread_t 		tid;					//thread ID
	int				aid;					//actor ID
	ActorClass		*actor;					//actor
	ActorPort		*inputPort;	
	ActorPort		*outputPort;
	int				execState;

	FILE			*fd;

	pthread_mutex_t	mt;
	pthread_cond_t	cv; 
}AbstractActorInstance;

struct ActorClass {
	char			*name;
	int				numInputPorts;
	int				numOutputPorts;
	int				sizeActorInstance;
	void			(*action_scheduler)(AbstractActorInstance*);
	void			(*constructor)(AbstractActorInstance*);
	void			(*destructor)(AbstractActorInstance*);
	void			(*set_param)(AbstractActorInstance*,                                  ActorParameter*);
};

typedef struct {
	ActorClass		*actorClass;
	int				*inputPorts;
	int				*outputPorts;
	int				numParams;
	ActorParameter	*params;
}ActorConfig;

typedef struct {
	ActorConfig	**networkActors;
	int				numNetworkActors;
	int				numFifos;
}NetworkConfig;

extern AbstractActorInstance	*actorInstance[];
extern ActorClass				*actorClass[];
extern LIST						actorLists[];
extern int						actorStatus[];
extern int						log_level;
extern int						trace_action;
extern int						num_lists;
extern int						rts_mode;

extern int	execute_network(int argc, char *argv[], NetworkConfig *network);
extern void init_actor_network(NetworkConfig *);
extern int pinStatus(ActorPort *);
extern int pinStatus2(ActorPort *);
extern int pinAvail(ActorPort *);	
extern int pinRead(ActorPort *);
extern int pinRead2(ActorPort *,char *,int);
extern int pinPeek(ActorPort *,int);
extern int pinWrite(ActorPort *,int);
extern int pinWrite2(ActorPort *,char *, int);
extern void source(AbstractActorInstance *);
extern void sink(AbstractActorInstance *);
extern void pinWait(ActorPort *,int);
extern int getNumOfInstances();
extern void actorTrace(AbstractActorInstance *,int,char *,...);
extern void trace(int,char*,...);
int rangeError(int x, int y, char *filename, int line);
#endif
