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

/* make the header usable from C++ */
#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#define INPUT				0
#define OUTPUT				1
#define INTERNAL			0
#define EXTERNAL			1
#define COPY(a)				a
#define MAX_DATA_LENGTH		1024
#define MAX_ACTOR_NUM		128

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

#define ACTOR_NORMAL		0
#define ACTOR_STANDALONE	1

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
	void			(*set_param)(AbstractActorInstance*,int,                       ActorParameter*);
	int				*inputPortSizes;
	int				*outputPortSizes;
	int				actorExecMode;
};

typedef struct {
	ActorClass		*actorClass;
	int				*inputPorts;
	int				*outputPorts;
	int				numParams;
	ActorParameter	*params;
}ActorConfig;

typedef struct {
	ActorConfig		**networkActors;
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


/** Runs the actors network, including evaluating command line arguments, error message output,
 * network setup and finally execution. */
extern int execute_network(int argc, char *argv[], const NetworkConfig *network);

/** Sets up the actors network according to the information provided in \a network . */
extern void init_actor_network(const NetworkConfig * network);

/** Returns 1 if there is at least one token available for reading if \a port is an input port,
 * or if at least one token can be written if it is an output port.
 * Returns 0 otherwise. */
extern int pinStatus(ActorPort * port);

/** For an input port, pinStatus2 returns the number of readable bytes available in the ActorPort \a port.
 *  For an output port, pinStatus2 returns the number of bytes which can be written into the ActorPort \a port. */
extern int pinStatus2(ActorPort * port);

/** This function is similar to pinStatus(). Instead of 0 and 1 it returns 0 or the actual
 * number of tokens which can be read from \a port / written to \a port . */
extern int pinAvail(ActorPort * port);

/** Return number of tokens in int32 size which can be read from \a port / written to \a port . */
extern int pinAvail_int32_t(ActorPort *actorPort);

/** Return number of tokens in double size which can be read from \a port / written to \a port . */
extern int pinAvail_double(ActorPort *actorPort);

/** Return number of tokens in bool_t size which can be read from \a port / written to \a port . */
extern int pinAvail_bool_t(ActorPort *actorPort);

/** Reads one integer-sized token from \a port and returns it. Breaks if tokenSize > sizeof(int) . */
extern int pinRead(ActorPort * port);

/** Reads nonblocking \a length bytes from \a port into the buffer \a buf .
 * Potentially blocked writers waiting that enough space in \a port becomes free
 * are signalled. */
extern int pinRead2(ActorPort * port, char * buf, int length);

/** Reads one double-sized token from \a port and returns it. */
extern double pinRead_double(ActorPort *actorPort);

/** Reads one int32_t-sized token from \a port and returns it. */
extern int32_t pinRead_int32_t(ActorPort *actorPort);

/** Reads one bool_t-sized token from \a port and returns it. */
extern bool_t pinRead_bool_t(ActorPort *actorPort);

/** Reads one integer-sized token from \a port at the given \a offset relative to the current
 * read position (in tokens) and returns it. The current read position is not modified.
 * Breaks if tokenSize > sizeof(int) . */
extern int pinPeek(ActorPort * port, int offset);

/** Reads one int32_t-sized token from \a port at the given \a offset relative to the current
 * read position (in tokens) and returns it. The current read position is not modified. */
extern int32_t pinPeek_int32_t(ActorPort *actorPort, int offset);

/** Reads one double-sized token from \a port at the given \a offset relative to the current
 * read position (in tokens) and returns it. The current read position is not modified. */
extern double pinPeek_double(ActorPort *actorPort, int offset);

/** Reads one bool_t-sized token from \a port at the given \a offset relative to the current
 * read position (in tokens) and returns it. The current read position is not modified. */
extern bool_t pinPeek_bool_t(ActorPort *actorPort, int offset);

/** Writes nonblocking the int-sized token with the given \a value into the given \a port .
 * Potentially blocked readers waiting that data becomes available in \a port are signalled. */
extern int pinWrite(ActorPort * port, int value);

/** Writes nonblocking the int-sized token with the given \a value into the given \a port .
 * Potentially blocked readers waiting that data becomes available in \a port are signalled. */
extern int pinWrite2(ActorPort * port, const char * buf, int length);

/** Writes nonblocking the double-sized token with the given \a value into the given \a port .
 * Potentially blocked readers waiting that data becomes available in \a port are signalled. */
extern int pinWrite_double(ActorPort *actorPort,double val);

/** Writes nonblocking the int32_t-sized token with the given \a value into the given \a port .
 * Potentially blocked readers waiting that data becomes available in \a port are signalled. */
extern int pinWrite_int32_t(ActorPort *actorPort,int32_t val);

/** Writes nonblocking the bool_t-sized token with the given \a value into the given \a port .
 * Potentially blocked readers waiting that data becomes available in \a port are signalled. */
extern int pinWrite_bool_t(ActorPort *actorPort,bool_t val);

/** Marks the actor as waiting for \a port , i.e. waiting that \a length bytes
 * become available for reading or writing.
 * Does not block itself ! */
extern void pinWait(ActorPort * port, int length);

/** Printf-like tracing function to stderr. Prints only if \a level >= the current log_level. */
extern void actorTrace(AbstractActorInstance * base, int level, const char * message, ...);

/** Printf-like tracing function to stderr. Prints only if \a level >= the current log_level. */
extern void trace(int level, const char*,...);

/** Prints a range error message to stdout. */
int rangeError(int x, int y, const char *filename, int line);

#ifdef __cplusplus
}
#endif

#endif
