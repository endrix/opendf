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
#include <pthread.h>
#include <assert.h>

/* make the header usable from C++ */
#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#define COPY(a)				a
#define DEFAULT_FIFO_LENGTH	256

#define TRACE_ACTION(INSTANCE,INDEX,NAME) /* empty */

#define RANGECHK(X,B) ((unsigned)(X)<(unsigned)(B)?(X):RANGEERR(X,B))
#define RANGEERR(X,B) (rangeError((X),(B),__FILE__,__LINE__))

#define	LOG_MUST			0			//must log this
#define	LOG_ERROR			1			//only log errors
#define	LOG_WARN			2			//also log warnings
#define	LOG_INFO			3			//also log info
#define	LOG_EXEC			4			//also log func exec
#define	LOG_STOP			(-99)		//disable log file

#define pinWait(port,length)
#define pinAvail_int32_t(port) pinAvail(port)
#define pinAvail_bool_t(port) pinAvail(port)
#define pinAvail_double(port) pinAvail(port)
#define pinAvailIn_int32_t(port) pinAvailIn(port)
#define pinAvailOut_int32_t(port) pinAvailOut(port)
#define pinAvailIn_double(port) pinAvailIn(port)
#define pinAvailOut_double(port) pinAvailOut(port)
#define pinAvailIn_bool_t(port) pinAvailIn(port)
#define pinAvailOut_bool_t(port) pinAvailOut(port)
#define pinWaitIn(port, length) pinWait(port,length)
#define pinWaitOut(port, length) pinWait(port, length)

typedef int32_t				bool_t;

typedef struct {
	char			*key;
	char			*value;
}ActorParameter;

typedef struct ActorClass ActorClass;

typedef struct {
  char *bufferStart; // properly aligned, given type T
  char *bufferEnd;   // = bufferStart + sizeof(T)*capacity;
  char *readPtr;
  char *writePtr;
  unsigned numWritten; // in tokens
  unsigned numRead;    // in tokens
  unsigned capacity;   // in tokens
} InputPort;

typedef struct {
  int numReaders;
  InputPort **readers;
} OutputPort;

typedef struct {
	ActorClass		*actor;					//actor
	InputPort		*inputPort;
	OutputPort		*outputPort;
}AbstractActorInstance;

typedef struct {
	const char	*name;
	const int 	*consumption;
	const int 	*production;
} ActionDescription;

typedef struct {
	const char	*name;
	int 		tokenSize;
} PortDescription;

struct ActorClass {
	char			*name;
	int				numInputPorts;
	int				numOutputPorts;
	int				sizeActorInstance;
	void			(*action_scheduler)(AbstractActorInstance*);
	void			(*constructor)(AbstractActorInstance*);
	void			(*destructor)(AbstractActorInstance*);
	void			(*set_param)(AbstractActorInstance*,const char*, const char*);
	const PortDescription		*inputPortDescriptions;
	const PortDescription		*outputPortDescriptions;
	int				actorExecMode;
	int				numActions;
	const ActionDescription *actionDescriptions;
};

#define INIT_ActorClass(aClassName,              \
                        instance_t,              \
                        ctor,                    \
                        sched,	                 \
                        nInputs, inputDescr,	 \
                        nOutputs, outputDescr,	 \
                        nActions, actionDescr) { \
    .name=aClassName,                            \
    .numInputPorts=nInputs,                      \
    .numOutputPorts=nOutputs,                    \
    .sizeActorInstance=sizeof(instance_t),       \
    .action_scheduler=sched,                     \
    .constructor=ctor,                           \
    .destructor=0,                               \
    .set_param=0,                                \
    .inputPortDescriptions=inputDescr,           \
    .outputPortDescriptions=outputDescr,         \
    .actorExecMode=0,                            \
    .numActions=nActions,                        \
    .actionDescriptions=actionDescr              \
  }

// Same pinAvail can be used for all token sizes (since we count tokens)

static inline unsigned pinAvailIn(const InputPort *p) {
  return p->numWritten - p->numRead;
}

static inline unsigned pinAvailOut(const OutputPort *p) {
  InputPort **readers=p->readers;
  InputPort **end=readers + p->numReaders;
  InputPort *inputPort=*readers;
  unsigned numWritten=inputPort->numWritten; // Should be same for all fifos
  unsigned minSpace=inputPort->capacity - numWritten + inputPort->numRead;

  while (++readers<end) {
	inputPort=*readers;
    unsigned space=inputPort->capacity - numWritten + inputPort->numRead;
    if (space<minSpace)
      minSpace=space;
  }

  return minSpace;
}

static inline int32_t pinPeekFront_int32_t(const InputPort *p) {
	return *((int32_t*) p->readPtr);
}

static inline bool_t pinPeekFront_bool_t(const InputPort *p) {
	return *((bool_t*) p->readPtr);
}

static inline double pinPeekFront_double(const InputPort *p) {
	return *((double*) p->readPtr);
}

static inline int32_t pinPeek_int32_t(const InputPort *p, int offset) {
  int32_t *ptr=(int32_t*) p->readPtr + offset;
  if (ptr >= (int32_t*) p->bufferEnd)
    ptr -= p->capacity;
  return *ptr;
}

static inline bool_t pinPeek_bool_t(const InputPort *p, int offset) {
  bool_t *ptr=(bool_t*) p->readPtr + offset;
  if (ptr >= (bool_t*) p->bufferEnd)
    ptr -= p->capacity;
  return *ptr;
}

static inline double pinPeek_double(const InputPort *p, int offset) {
  double *ptr=(double*) p->readPtr + offset;
  if (ptr >= (double*) p->bufferEnd)
    ptr -= p->capacity;
  return *ptr;
}

static inline int32_t pinRead_int32_t(InputPort *p) {
  int32_t *ptr=(int32_t*) p->readPtr;
  int32_t result=*ptr++;
  assert(pinAvailIn(p)>0);

  if (ptr==(int32_t*) p->bufferEnd)
    p->readPtr=p->bufferStart;
  else
    p->readPtr=(char*) ptr;
  p->numRead++;

  // Unblocking stuff goes here...
  // Function call? Try to limit number of calls...
 
  return result;
}

static inline bool_t pinRead_bool_t(InputPort *p) {
  bool_t *ptr=(bool_t*) p->readPtr;
  bool_t result=*ptr++;
  if (ptr==(bool_t*) p->bufferEnd)
    p->readPtr=p->bufferStart;
  else
    p->readPtr=(char*) ptr;
  p->numRead++;

  // Unblocking stuff goes here...
  // Function call? Try to limit number of calls...
 
  return result;
}

static inline double pinRead_double(InputPort *p) {
  double *ptr=(double*) p->readPtr;
  double result=*ptr++;
  if (ptr==(double*) p->bufferEnd)
    p->readPtr=p->bufferStart;
  else
    p->readPtr=(char*) ptr;
  p->numRead++;

  // Unblocking stuff goes here...
  // Function call? Try to limit number of calls...
 
  return result;
}

static inline void pinWrite_int32_t(OutputPort *p, int32_t token) {
  InputPort **readers=p->readers;
  InputPort **end=readers + p->numReaders;
  InputPort *inputPort=*readers;
  unsigned numWritten=inputPort->numWritten+1; // Should be same for all fifos

  assert(pinAvailOut(p)>0);
  for (; readers<end; ++readers) {
	inputPort=*readers;
    int32_t *ptr=(int32_t*) inputPort->writePtr;
    *ptr++ = token;
    if (ptr==(int32_t*) inputPort->bufferEnd)
      inputPort->writePtr=inputPort->bufferStart;
    else
      inputPort->writePtr=(char*) ptr;
    inputPort->numWritten=numWritten;
  }
}

static inline void pinWrite_bool_t(OutputPort *p, bool_t token) {
  InputPort **readers=p->readers;
  InputPort **end=readers + p->numReaders;
  InputPort *inputPort=*readers;
  unsigned numWritten=inputPort->numWritten+1; // Should be same for all fifos

  for (; readers<end; ++readers) {
	inputPort=*readers;
    bool_t *ptr=(bool_t*) inputPort->writePtr;
    *ptr++ = token;
    if (ptr==(bool_t*) inputPort->bufferEnd)
      inputPort->writePtr=inputPort->bufferStart;
    else
      inputPort->writePtr=(char*) ptr;
    inputPort->numWritten=numWritten;
  }
}

static inline void pinWrite_double(OutputPort *p, double token) {
  InputPort **readers=p->readers;
  InputPort **end=readers + p->numReaders;
  InputPort *inputPort=*readers;
  unsigned numWritten=inputPort->numWritten+1; // Should be same for all fifos

  for (; readers<end; ++readers) {
	inputPort=*readers;
    double *ptr=(double*) inputPort->writePtr;
    *ptr++ = token;
    if (ptr==(double*) inputPort->bufferEnd)
      inputPort->writePtr=inputPort->bufferStart;
    else
      inputPort->writePtr=(char*) ptr;
    inputPort->numWritten=numWritten;
  }
}

extern int						log_level;

extern void trace(int level, const char*,...);
extern int rangeError(int x, int y, const char *filename, int line);
extern void stop_run();
extern AbstractActorInstance *createActorInstance(ActorClass *actorClass);
extern OutputPort *createOutputPort(AbstractActorInstance *pInstance,
                             const char *portName,
							 int numberOfReaders);
extern InputPort *createInputPort(AbstractActorInstance *pInstance,
                           const char *portName,
						   int capacity);
extern void connectPorts(OutputPort *outputPort, InputPort *inputPort);
extern int executeNetwork(int argc, char *argv[],AbstractActorInstance **instances,
                          int numInstances);
extern void setParameter(AbstractActorInstance *pInstance,
                  const char *key,
                  const char *value);

#ifdef __cplusplus
}
#endif

#endif
