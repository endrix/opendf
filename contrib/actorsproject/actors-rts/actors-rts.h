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
#include <assert.h>
#include "dll.h"

/* make the header usable from C++ */
#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#define COPY(a)				a

//#define XML_TRACE
//#define SCHED_EDF_SUPPORT
#define RM_SUPPORT

#ifndef XML_TRACE
#define TRACE_ACTION(INSTANCE,INDEX,NAME) (INSTANCE)->hasFiredHack=1
#else
#define TRACE_ACTION(INSTANCE,INDEX,NAME) \
        (INSTANCE)->hasFiredHack=1; \
        actionTrace((INSTANCE),INDEX,NAME)
#endif

#define RANGECHK(X,B) ((unsigned)(X)<(unsigned)(B)?(X):RANGEERR(X,B))
#define RANGEERR(X,B) (rangeError((X),(B),__FILE__,__LINE__))

#define pinWait(port,length)
#define pinAvail_int32_t(port)    pinAvail(port)
#define pinAvail_bool_t(port)     pinAvail(port)
#define pinAvail_double(port)     pinAvail(port)
#define pinAvailIn_int32_t(port)  pinAvailIn(port)
#define pinAvailOut_int32_t(port) pinAvailOut(port)
#define pinAvailIn_double(port)   pinAvailIn(port)
#define pinAvailOut_double(port)  pinAvailOut(port)
#define pinAvailIn_bool_t(port)   pinAvailIn(port)
#define pinAvailOut_bool_t(port)  pinAvailOut(port)
#define pinWaitIn(port, length)   pinWait(port,length)
#define pinWaitOut(port, length)  pinWait(port, length)

typedef int32_t           bool_t;

typedef struct ActorClass ActorClass;
typedef struct AbstractActorInstance AbstractActorInstance;
typedef struct OutputPort OutputPort;

typedef struct {
  char    *bufferStart; // properly aligned, given type T
  char    *bufferEnd;   // = bufferStart + sizeof(T)*capacity;
  char    *readPtr;
  unsigned numRead;     // in tokens
  unsigned availTokens; // in tokens
  unsigned capacity;    // in tokens

  const OutputPort *writer;

  AbstractActorInstance *readerActor;
  AbstractActorInstance *writerActor;

  int cid;

} InputPort;

struct OutputPort {
  char *bufferStart;    // properly aligned, given type T
  char *bufferEnd;      // = bufferStart + sizeof(T)*capacity;
  char *writePtr;
  unsigned numWritten;  // in tokens
  unsigned availSpace;  // in tokens
  unsigned capacity;    // in tokens

  int numReaders;
  InputPort **readers;

  AbstractActorInstance *writerActor;

  int cid;
};

struct AbstractActorInstance {
  ActorClass  *actor;          //actor
  InputPort   *inputPort;
  OutputPort  *outputPort;
  int         hasFiredHack;

  LIST        *list;
  LIST        *extList;

  int         firstActionIndex;
};

// Get port-pointers from abstract instance
#define INPUT_PORT(instance,n)  (instance.inputPort+(n))
#define OUTPUT_PORT(instance,n) (instance.outputPort+(n))

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
	const int*		(*action_scheduler)(AbstractActorInstance*);
	void			(*constructor)(AbstractActorInstance*);
	void			(*destructor)(AbstractActorInstance*);
	void			(*set_param)(AbstractActorInstance*,const char*, const char*);
	const PortDescription		*inputPortDescriptions;
	const PortDescription		*outputPortDescriptions;
	int				actorExecMode;
	int				numActions;
	const ActionDescription *actionDescriptions;
};

// Creates an ActorClass initializer

#define INIT_ActorClass(aClassName,              \
                        instance_t,              \
                        ctor,                    \
			setParam,                \
                        sched,	                 \
                        dtor,                    \
                        nInputs, inputDescr,	 \
                        nOutputs, outputDescr,	 \
                        nActions, actionDescr) { \
    .name=aClassName,                            \
    .numInputPorts=nInputs,                      \
    .numOutputPorts=nOutputs,                    \
    .sizeActorInstance=sizeof(instance_t),       \
    .action_scheduler=sched,                     \
    .constructor=ctor,                           \
    .destructor=dtor,                            \
    .set_param=setParam,                         \
    .inputPortDescriptions=inputDescr,           \
    .outputPortDescriptions=outputDescr,         \
    .actorExecMode=0,                            \
    .numActions=nActions,                        \
    .actionDescriptions=actionDescr              \
  }

// Action-scheduler exit code (first element of array)
// EXITCODE_TERMINATE = actor is dead
// EXITCODE_BLOCK(n)  = actor blocks on either of n ports
// EXITCODE_YIELD     = actor yielded, but may be fireable

#define EXITCODE_TERMINATE 0
#define EXITCODE_BLOCK(n)  (n)
#define EXITCODE_YIELD     -1

// Same pinAvail can be used for all token sizes (since we count tokens)

static inline unsigned pinAvailIn(const InputPort *p) {
  return p->availTokens;
}

static inline unsigned pinAvailOut(const OutputPort *p) {

  return p->availSpace;
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
#ifdef DEBUG
  assert(pinAvailIn(p)>0);
#endif
  int32_t *ptr=(int32_t*) p->readPtr;
  int32_t result=*ptr++;

  if (ptr==(int32_t*) p->bufferEnd)
    p->readPtr=p->bufferStart;
  else
    p->readPtr=(char*) ptr;
  p->numRead++;
  p->availTokens--;

  return result;
}

static inline bool_t pinRead_bool_t(InputPort *p) {
#ifdef DEBUG
  assert(pinAvailIn(p)>0);
#endif
  bool_t *ptr=(bool_t*) p->readPtr;
  bool_t result=*ptr++;

  if (ptr==(bool_t*) p->bufferEnd)
    p->readPtr=p->bufferStart;
  else
    p->readPtr=(char*) ptr;
  p->numRead++;
  p->availTokens--;
 
  return result;
}

static inline double pinRead_double(InputPort *p) {
#ifdef DEBUG
  assert(pinAvailIn(p)>0);
#endif
  double *ptr=(double*) p->readPtr;
  double result=*ptr++;

  if (ptr==(double*) p->bufferEnd)
    p->readPtr=p->bufferStart;
  else
    p->readPtr=(char*) ptr;
  p->numRead++;
  p->availTokens--;
 
  return result;
}

static inline void pinWrite_int32_t(OutputPort *p, int32_t token) {
#ifdef DEBUG
  assert(pinAvailOut(p)>0);
#endif
  int32_t *ptr=(int32_t*) p->writePtr;

  *ptr++=token;
  if (ptr==(int32_t*) p->bufferEnd)
    p->writePtr=p->bufferStart;
  else
    p->writePtr=(char*) ptr;
  p->numWritten++;
  p->availSpace--;
}

static inline void pinWrite_bool_t(OutputPort *p, bool_t token) {
#ifdef DEBUG
  assert(pinAvailOut(p)>0);
#endif
  bool_t *ptr=(bool_t*) p->writePtr;

  *ptr++=token;
  if (ptr==(bool_t*) p->bufferEnd)
    p->writePtr=p->bufferStart;
  else
    p->writePtr=(char*) ptr;
  p->numWritten++;
  p->availSpace--;
}

static inline void pinWrite_double(OutputPort *p, double token) {
#ifdef DEBUG
  assert(pinAvailOut(p)>0);
#endif
  double *ptr=(double*) p->writePtr;

  *ptr++=token;
  if (ptr==(double*) p->bufferEnd)
    p->writePtr=p->bufferStart;
  else
    p->writePtr=(char*) ptr;
  p->numWritten++;
  p->availSpace--;
}

extern int rangeError(int x, int y, const char *filename, int line);
extern void runtimeError(AbstractActorInstance*, const char *format,...);
extern void trace(int level, const char*,...);
extern void actionTrace(AbstractActorInstance *instance,
            int localActionIndex,
            char *actionName);
extern void wakeup_waitingList(LIST *list);

/** This function has to be called once by every thread executed within the runtime.
 * It registers the thread id of that thread, so it can be queried later. */
extern void register_thread_id(void);

/** Returns an array containing all thread ids of the currently existing threads.
 * The array has been allocated via malloc() and must be freed by the caller via free(). */
extern void get_thread_ids(int* count, pid_t** threadIds);


#ifdef __cplusplus
}
#endif

#endif
