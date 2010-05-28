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

#if defined(__i386__)

#define CACHE_LINE_SIZE 128
#define mb()    asm volatile("mfence":::"memory")
#define rmb()   asm volatile("lfence":::"memory")
#define wmb()   asm volatile("sfence" ::: "memory")

#elif defined(__arm__)

#define CACHE_LINE_SIZE 64
#error Not implemented yet

#endif

#define COPY(a) a

#define RANGECHK(X,B) ((unsigned)(X)<(unsigned)(B)?(X):RANGEERR(X,B))
#define RANGEERR(X,B) (rangeError((X),(B),__FILE__,__LINE__))

typedef int32_t				bool_t;

typedef struct {
	char			*key;
	char			*value;
}ActorParameter;

typedef struct ActorClass ActorClass;
typedef struct OutputPort OutputPort;
typedef struct InputPort InputPort;
typedef struct AbstractActorInstance AbstractActorInstance;

typedef struct LocalContext {
  int pos;
  int count;
  int available;
} LocalContext;

typedef struct { volatile int value; } atomic_value_t;
#define atomic_get(a) ((a)->value)
#define atomic_set(a,v) (((a)->value) = (v))

typedef struct SharedContext {
  atomic_value_t count;
} SharedContext;

struct InputPort {
  // Struct accessed between cores, only semi-constant data
  int index;     // index into shared/local vectors
  int cpu;       // The CPU where this port currently resides
  AbstractActorInstance *actor;
  SharedContext *shared;
  LocalContext *local;
  void *buffer; 
  unsigned capacity;
  const OutputPort *writer;
};

struct OutputPort {
  // Struct accessed between cores, only semi-constant data
  int index;     // index into shared/local vectors
  int cpu;       // The CPU where this port currently resides
  AbstractActorInstance *actor;
  SharedContext *shared;
  LocalContext *local;
  void *buffer; 
  unsigned capacity;
  int readers;
  InputPort **reader;
};

typedef struct {
  int pos;
  int available;
  void *buffer; 
  unsigned capacity;
} LocalInputPort;

typedef struct {
  int pos;
  int available;
  void *buffer; 
  unsigned capacity;
} LocalOutputPort;

struct AbstractActorInstance {
  ActorClass *actor;					//actor
  char *name;
  int cpu_index;       // The CPU where this actor currently resides
  int outputs;
  OutputPort *output;
  int inputs;
  InputPort *input;
  int fireable;
  int fired;
  int terminated;
  long long nloops;
  unsigned long long total;
};

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
  char *name;
  int numInputPorts;
  int numOutputPorts;
  int sizeActorInstance;
  const int* (*action_scheduler)(AbstractActorInstance*, int);
  void (*constructor)(AbstractActorInstance*);
  void (*destructor)(AbstractActorInstance*);
  void (*set_param)(AbstractActorInstance*,const char*, const char*);
  const PortDescription *inputPortDescriptions;
  const PortDescription *outputPortDescriptions;
  int actorExecMode;
  int numActions;
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
#define EXIT_CODE_YIELD     NULL


extern AbstractActorInstance	*actorInstance[];
extern int						log_level;

extern void trace(int level, const char*,...);
extern int rangeError(int x, int y, const char *filename, int line);
extern void runtimeError(AbstractActorInstance*, const char *format,...);
extern AbstractActorInstance *createActorInstance(ActorClass *actorClass);
extern OutputPort *createOutputPort(AbstractActorInstance *pInstance,
                             const char *portName,
							 int numberOfReaders);
extern InputPort *createInputPort(AbstractActorInstance *pInstance,
                           const char *portName,
						   int capacity);
extern void connectPorts(OutputPort *outputPort, InputPort *inputPort);
extern int executeNetwork(int argc, char *argv[],AbstractActorInstance **instances, int numInstances);
extern void setParameter(AbstractActorInstance *pInstance,
                  const char *key,
                  const char *value);

#define ART_INPUT(index) &(context->input[index])
#define ART_OUTPUT(index) &(context->output[index])

#define ART_ACTION_CONTEXT(numInputs, numOutputs)	\
  typedef struct art_action_context {			\
    int fired;						\
    int loop;						\
    LocalInputPort input[numInputs];			\
    LocalOutputPort output[numOutputs];			\
  } art_action_context_t;

#define ART_ACTION(name, thistype)					\
  static void name(art_action_context_t *context, thistype *thisActor)	

#define ART_ACTION_SCHEDULER(name)				\
  static const int *name(AbstractActorInstance *pBase,		\
			 int maxloops)

#define ART_ACTION_SCHEDULER_ENTER(numInputs, numOutputs)		\
  art_action_context_t theContext;					\
  art_action_context_t *context = &theContext;				\
  context->fired = 0;							\
  {									\
    int i;								\
    for (i = 0 ; i < numInputs ; i++) {					\
      /* cpu is not used in actors */					\
      /* shared is not used in actors */				\
      context->input[i].pos = pBase->input[i].local->pos;		\
      context->input[i].available = pBase->input[i].local->available;	\
      context->input[i].buffer = pBase->input[i].buffer;		\
      context->input[i].capacity = pBase->input[i].capacity;		\
      /* writer is not used in actors */				\
    }									\
    for (i = 0 ; i < numOutputs ; i++) {				\
      /* cpu is not used in actors */					\
      /* shared is not used in actors */				\
      context->output[i].pos = pBase->output[i].local->pos;		\
      context->output[i].available = pBase->output[i].local->available;	\
      context->output[i].buffer = pBase->output[i].buffer;		\
      context->output[i].capacity = pBase->output[i].capacity;	\
      /* readers is not used in actors */				\
      /* reader is not used in actors */				\
    }									\
  }

#define ART_ACTION_SCHEDULER_LOOP					\
  for (context->loop = 0 ; context->loop < maxloops ; context->loop++)

#define ART_ACTION_SCHEDULER_LOOP_TOP

#define ART_ACTION_SCHEDULER_LOOP_BOTTOM

#define ART_ACTION_SCHEDULER_EXIT(numInputs, numOutputs)		\
  if (context->fired){							\
    int i;								\
    pBase->fired = context->fired;					\
    for (i = 0 ; i < numInputs ; i++) {					\
      pBase->input[i].local->pos = context->input[i].pos;		\
      pBase->input[i].local->count =					\
	pBase->input[i].local->available - context->input[i].available;	\
      pBase->input[i].local->available = context->input[i].available;	\
    }									\
    for (i = 0 ; i < numOutputs ; i++) {				\
      pBase->output[i].local->pos = context->output[i].pos;		\
      pBase->output[i].local->count =					\
	pBase->output[i].local->available - context->output[i].available; \
      pBase->output[i].local->available = context->output[i].available; \
    }									\
  }

#define ART_FIRE_ACTION(name)			\
  name(context, thisActor)

#define ART_ACTION_ENTER(name, index)		\
  context->fired++;

#define ART_ACTION_EXIT(name, index)

#define FIFO_TYPE int32_t
#include "actors-fifo.h"
#undef FIFO_TYPE 
#define FIFO_TYPE bool_t
#include "actors-fifo.h"
#undef FIFO_TYPE 
#define FIFO_TYPE double
#include "actors-fifo.h"
#undef FIFO_TYPE 



#endif