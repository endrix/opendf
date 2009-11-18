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
#include <sys/time.h>
#include <stdarg.h>
#include <errno.h>
#include "actors-rts.h"
#include "internal.h"
#include "dll.h"
#include "xmlTrace.h"
#include "edf_syscall.h"

int             log_level = LOG_ERROR;
static int      numLists = 1;
static int      Running = 1;
static int      numWaitingThread=0;
pthread_mutex_t numWaitingThreadsLock;
static LIST     **actorLists;
pthread_mutex_t threadIdsMutex;
int             numberOfCreatedThreads = 0;
pid_t           threadIds[MAX_ACTOR_NUM];

static int      text_action_trace=0;

#if defined SCHED_EDF_SUPPORT
struct timespec default_period = { 0, 50000000 };
struct timespec default_runtime = { 0, 48000000 };
#endif

static void stop_run(){
  Running = 0;
}

static void wakeupLists(LIST *myList){
  int i;
  for(i=0;i<numLists;i++){
    LIST *list=actorLists[i];
    if(list==myList)
      continue;
    pthread_mutex_lock(&list->mt);
    if(list->state==WAITING_STATE)
      pthread_cond_signal(&list->cv);
    pthread_mutex_unlock(&list->mt);
  }
}

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

unsigned int timestamp(char* buf)
{
#if 0
  struct timeb  tb;
  struct tm   *tm;
  ftime(&tb);
  tm=localtime(&tb.time);
  sprintf(buf,"%02d:%02d:%02d.%03d> ",tm->tm_hour,tm->tm_min,tm->tm_sec,tb.millitm);
#endif
  struct timeval tv;
  static double init=0;
  double now;
  gettimeofday(&tv, NULL);
  if(!init){
    init=tv.tv_sec*1000000 + tv.tv_usec;
    now=0;
  }else{
    now=tv.tv_sec*1000000 + tv.tv_usec - init;
  }
  if(buf)
    sprintf(buf,"%f ",now/1000000);

  return (unsigned int)now;
}

static void tprintf(char *buf,const char *fmt,va_list ap)
{
  vsprintf(buf+strlen(buf),fmt,ap);
} 

static void mylog(FILE *fd, const char *msg,va_list ap)
{
  char    buf1[2048]="";
  if (*msg!='\t') timestamp(buf1);  
  tprintf(buf1,msg,ap);
  if (!strchr(buf1,'\n') && !strchr(buf1,'\t')) strcat(buf1,"\n");
  if(fd)
    fprintf((FILE*)fd,"%s",buf1);
  else
    printf("%s",buf1);
}

void trace(int level,const char *msg,...)
{
  va_list ap;
  if (log_level<level) return;
  va_start(ap,msg);
  mylog(0,msg,ap);
  va_end(ap);  
}

void actionTrace(AbstractActorInstance *instance,
         int localActionIndex,
         char *actionName) {
  if (text_action_trace)
    trace(LOG_MUST, "%s %d %s\n", instance->actor->name,
      localActionIndex,
      actionName);

  // TODO: it should be the "global" action index
  if (instance->list->file)
    xmlTraceAction(instance->list->file,instance->firstActionIndex + localActionIndex);
}

int InitializeCriticalSection(sem_t *semaphore)
{
  int ret;
  ret = sem_init(semaphore, 0, 1);
  if(ret != 0){
    perror("Unable to initialize the semaphore");
  }
  return ret;
}

int EnterCriticalSection(sem_t *semaphore)
{
  int ret;
  do {
    ret = sem_wait(semaphore);
    if (ret != 0){
    /* the lock wasn't acquired */
    if (errno != EINVAL) {
      perror("Error in sem_wait.");
      return -1;
    } else {
      /* sem_wait() has been interrupted by a signal: looping again */
      printf("sem_wait interrupted. Trying again for the lock...\n");
    }
    }
  } while (ret != 0);

  return ret;
}

int LeaveCriticalSection(sem_t *semaphore)
{
  int ret;
  ret = sem_post(semaphore);
  if (ret != 0)
    perror("Error in sem_post");
  return ret;
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

  // Find the OutputPort
  for (j=0; j<numOutputPorts; ++j)
    if (strcmp(portName, descr[j].name)==0)
      break;

  assert(j<numOutputPorts);
  OutputPort *port=&pInstance->outputPort[j];
  port->numReaders=numberOfReaders;
  port->readers=(InputPort**)
    calloc(numberOfReaders, sizeof(InputPort*));

  port->writerActor=pInstance;

  return port;
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
    if (strcmp(portName, descr[j].name)==0)
      break;

  assert(j<numInputPorts);
  InputPort *port=&pInstance->inputPort[j];
  port->capacity=capacity;

  port->readerActor=pInstance;

  return port;
}


void connectPorts(OutputPort *outputPort, InputPort *inputPort) {
  int numReaders=outputPort->numReaders;
  int j;

  // Set outputPort as the writer of inputPort
  inputPort->writer=outputPort;

  // Set inputPort as a reader of outputPort
  for (j=0; j<numReaders && outputPort->readers[j]!=0; ++j)
    ;
  assert(j<numReaders);
  outputPort->readers[j] = inputPort;

  // Compute maximum capacity among the readers
  if (inputPort->capacity > outputPort->capacity)
    outputPort->capacity = inputPort->capacity;

  inputPort->writerActor = outputPort->writerActor;

}

static void allocate_buffers(AbstractActorInstance **actorInstance,
                             int numInstances) {
  int i;
  int cid=0;
  for (i=0; i<numInstances; ++i) {
    OutputPort *outputPort=actorInstance[i]->outputPort;
    ActorClass *actorClass=actorInstance[i]->actor;
    const PortDescription *descr=actorClass->outputPortDescriptions;
    int numOutputPorts=actorClass->numOutputPorts;
    int j;

    for (j=0; j<numOutputPorts; ++j) {
      OutputPort *out=outputPort + j;
      int capacity=out->capacity;
      int size=capacity*descr[j].tokenSize;
      char *buffer=malloc(size);
      int k;

      // Assign the buffer to the output port
      out->bufferStart=buffer;
      out->bufferEnd=buffer+size;
      out->writePtr=buffer;
      out->availSpace=capacity;
      out->cid=cid++;

      for (k=0; k<out->numReaders; ++k) {
        InputPort *in=out->readers[k];
        
        // Assign the buffer to each of the readers
        in->bufferStart=buffer;
        in->bufferEnd=buffer+size;
        in->readPtr=buffer;
        in->capacity=capacity;
        in->cid=out->cid;
        // TODO: make a 'gap' if we want to maintain a smaller capacity
      }
    }
  }
}

static unsigned compute_availSpace(const OutputPort *p) {
  InputPort **readers=p->readers;
  InputPort **end=readers + p->numReaders;
  InputPort *inputPort=*readers;
  unsigned numWritten=p->numWritten; // Should be same for all fifos
  unsigned maxUnread=numWritten - inputPort->numRead;

  while (++readers<end) {
  inputPort=*readers;
    unsigned unread=numWritten - inputPort->numRead;
    if (unread>maxUnread)
      maxUnread=unread;
  }

  return p->capacity - maxUnread;
}

static unsigned compute_availTokens(const InputPort *p) {
  // TODO: in a multi-core setting we need count "committed writes"
  // and have a write-barrier before updating the counter
  return p->writer->numWritten - p->numRead;
}

static void compute_pinAvail(AbstractActorInstance *pInstance) {
  ActorClass *actorClass=pInstance->actor;
  InputPort  *inputPort=pInstance->inputPort;
  InputPort  *endInput=inputPort+actorClass->numInputPorts;
  OutputPort *outputPort;
  OutputPort *endOutput;

  // Pre-compute availTokens for all input ports
  for (; inputPort<endInput; ++inputPort){
    inputPort->availTokens = compute_availTokens(inputPort);
  }

  // Pre-compute availSpace for all output ports
  outputPort=pInstance->outputPort;
  endOutput=outputPort+actorClass->numOutputPorts;
  for (; outputPort<endOutput; ++outputPort){
    outputPort->availSpace = compute_availSpace(outputPort);
  }
}

static void append_node(LIST *a,LinkedNode *lnode)
{
    LOCK(a);
    if(a->head == NULL) {
        a->head = lnode;
        lnode->prev = NULL;
    } else {
        a->tail->next = lnode;
        lnode->prev = a->tail;
    }
    a->tail = lnode;
    lnode->next = NULL;
    a->numNodes++;
    UNLOCK(a);
}

static LinkedNode *remove_fired_node(LIST *a)
{
  LinkedNode *lnode;
  for(lnode=a->head;lnode;lnode=lnode->next){
    AbstractActorInstance *pInstance=(AbstractActorInstance *)lnode->obj;
    if(pInstance->hasFiredHack)
      break;   
  }
  if(lnode){
    if(lnode->prev == NULL)
      a->head = lnode->next;
    else
      lnode->prev->next = lnode->next;

    if(lnode->next == NULL)
      a->tail = lnode->prev;
    else
      lnode->next->prev = lnode->prev;

    a->numNodes--;
  }
  return lnode;
}

int objNotExit(LIST *list,void *obj){
  LinkedNode *lnode;
  for(lnode=list->head;lnode;lnode=lnode->next){
    if(lnode->obj==obj)
      return 0;
  }
  return 1;
}

void append_list(LIST *list, void *obj){
  if(objNotExit(list,obj))
  {
      LinkedNode *lnode = (LinkedNode *)calloc(1,sizeof(LinkedNode));
      lnode->obj=obj;
      append_node(list,lnode);
  }
}

void computeExtList2(LIST *extList,AbstractActorInstance *pInstance){
  ActorClass *actorClass=pInstance->actor;
  InputPort  *inputPort=pInstance->inputPort;
  InputPort  *endInput=inputPort+actorClass->numInputPorts;
  OutputPort *outputPort;
  OutputPort *endOutput;
  LIST       *myList=pInstance->list;

  // external lists for all input ports
  for (; inputPort<endInput; ++inputPort){
    AbstractActorInstance *writerActor = inputPort->writerActor;
    LIST *wList = writerActor->list;
    if(myList != wList)
      append_list(extList,wList);
  }

  // external lists for all output ports
  outputPort=pInstance->outputPort;
  endOutput=outputPort+actorClass->numOutputPorts;
  for (; outputPort<endOutput; ++outputPort){
    int i;
    for(i=0; i<outputPort->numReaders;i++){
      InputPort *in = outputPort->readers[i];
      AbstractActorInstance *readerActor=in->readerActor;
      LIST *rList=readerActor->list;
      if(myList != rList)
        append_list(extList,rList);
    }
  }
}

void print_extList(AbstractActorInstance *pInstance){
  LIST *extList = pInstance->extList;
  LIST *myList = pInstance->list;
  LinkedNode *lnode;

  printf("Actor %s of List[%d]\n",pInstance->actor->name,myList->lid);
  for(lnode=extList->head;lnode;lnode=lnode->next){
    LIST *list=(LIST*)lnode->obj;
    printf("extList[%d] ",list->lid);
  }
  printf("\n");
}

void print_extList2(LIST *list){
  LIST *extList = list->extList;
  LinkedNode *lnode;

  printf("ExList for list %d\n",list->lid);
  for(lnode=extList->head;lnode;lnode=lnode->next){
    LIST *l=(LIST*)lnode->obj;
    printf("[%d] ",l->lid);
  }
  printf("\n");
}

void printActorList(LIST **list,int numList)
{
  int i;
  LinkedNode *lnode;
  for(i=0;i<numList;i++){
    LIST *a=list[i];
    printf("list %d\n",i);
    for(lnode=a->head;lnode;lnode=lnode->next){
      AbstractActorInstance *instance=(AbstractActorInstance *)lnode->obj;
      printf("%s\n",instance->actor->name);
    }
  }
}

void computeExtList(LIST *list)
{
  LinkedNode *lnode;
  if(list->extList)
    free(list->extList);
  list->extList=(LIST*)calloc(1,sizeof(LIST));
  for(lnode=list->head;lnode;lnode=lnode->next){
    AbstractActorInstance *pInstance=(AbstractActorInstance *)lnode->obj;
    computeExtList2(list->extList,pInstance);
  }
  print_extList2(list);
}

enum Actors{
ParseHeaders,
MVSequence,
BlockExpand,
MVReconstruct,
Sequence,
DCSplit,
DCPred,
ZigzagAddr,
Zigzag,
ACPred,
Dequant,
Scale,
Combine,
ShuffleFly,
Shuffle,
Final,
RowSort,
FairMerge,
Downsample,
Separate,
Transpose,
Retranspose,
Clip,
MemoryManager,
MBPacker,
SearchWindow,
Unpack,
Interpolate,
Add,
byte2bit,
art_Source_bin,
art_Sink_yuv,
DDRModel,
art_DBus,
art_Sink
};

LIST **initList(int argc,char **argv,AbstractActorInstance **actorInstance,int numInstances,int *ptrNumLists){
  int actorIndex[]={
    art_Source_bin,
    byte2bit,
    ParseHeaders,
    MVSequence,
    BlockExpand,
    MVReconstruct,
    Sequence,
    DCSplit,
    DCPred,
    ZigzagAddr,
    Zigzag,
    ACPred,
    Dequant,
    Scale,
    Combine,
    ShuffleFly,
    Shuffle,
    Final,
    RowSort,
    FairMerge,
    Downsample,
    Separate,
    Transpose,
    Retranspose,
    Clip,
    MemoryManager,
    MBPacker,
    SearchWindow,
    Unpack,
    Interpolate,
    Add,
    art_Sink_yuv,
    DDRModel,
    art_Sink,
    art_DBus,
  };
  int i,j;
  LIST **list=(LIST**)calloc(128,sizeof(LIST*));
#ifdef XML_TRACE
  char filename[128];
  int firstActionIndex=0;
#endif
  int arrayIndex=0;
  int listIndex;
  int num;
  int totNum=numInstances;

  for(i=2,listIndex=0;;listIndex++,i++)
  {
    list[listIndex]=(LIST*)calloc(1,sizeof(LIST));
    list[listIndex]->lid=listIndex;
#if defined SCHED_EDF_SUPPORT
    list[listIndex]->list_period = default_period;
    list[listIndex]->list_budget = default_runtime;
#endif
    pthread_mutex_init(&list[listIndex]->mt, NULL);
    pthread_cond_init (&list[listIndex]->cv, NULL);
    if(argc>i)
      num=atoi(argv[i]);
    else
      num=totNum;

    for(j=0;j<num;j++,arrayIndex++,totNum--)
    {
      LinkedNode *lnode = (LinkedNode *)calloc(1,sizeof(LinkedNode));
      AbstractActorInstance *pInstance=actorInstance[actorIndex[arrayIndex]];
      lnode->obj=pInstance;
      append_node(list[listIndex],lnode);
      pInstance->list=list[listIndex];
      pInstance->extList=(LIST*)calloc(1,sizeof(LIST));
    }
    if(!totNum)
    {
        listIndex++;
        break;
    }
  }
  printActorList(list,listIndex);
  //compute the external list per actor instance
  for(i=0; i<numInstances; i++){
    AbstractActorInstance *pInstance=actorInstance[i];
    //set the external list per actor instance
    computeExtList2(pInstance->extList,pInstance);
//     print_extList(pInstance);
#ifdef XML_TRACE
    // global action index (used in traces)
    pInstance->firstActionIndex=firstActionIndex;
    firstActionIndex += pInstance->actor->numActions;
#endif
  }

  //compute the external list per list
  for(i=0;i<listIndex;i++){
    LIST *myList=list[i];
    computeExtList(myList);
#ifdef XML_TRACE
    //create file per list
    sprintf(filename,"trace_%d.xml",myList->lid);
    myList->file=xmlCreateTrace(filename);
#endif
  }
  *ptrNumLists = listIndex;
  return list;
}

#if 0
#define SEPERATOR   -1
#define TERMINATOR  -2
LIST **initList(AbstractActorInstance **actorInstance,int numInstances,int *ptrNumLists){
  int actorIndex[]={
    art_Source_bin,
    byte2bit,
    ParseHeaders,
    MVSequence,
    BlockExpand,
    MVReconstruct,
    Sequence,
    DCSplit,
    DCPred,
    ZigzagAddr,
    Zigzag,
    ACPred,
    Dequant,
    Scale,
    Combine,
    ShuffleFly,
    Shuffle,
    Final,
    RowSort,
    FairMerge,
    Downsample,
    Separate,
    Transpose,
    Retranspose,
SEPERATOR,	
    Clip,
    MemoryManager,
    MBPacker,
    SearchWindow,
    Unpack,
    Interpolate,
    Add,
    art_Sink_yuv,
    DDRModel,
    art_Sink,
//SEPERATOR,
    art_DBus,
TERMINATOR
  };
  int i;
  LIST **list=(LIST**)calloc(128,sizeof(LIST*));
#ifdef XML_TRACE
  char filename[128];
  int firstActionIndex=0;
#endif
  int arrayIndex=0;
  int endOfArray=0;
  int listIndex;

  //setup the lists
  for(listIndex=0;!endOfArray;listIndex++){
    list[listIndex]=(LIST*)calloc(1,sizeof(LIST));
    list[listIndex]->lid=listIndex;
#if defined SCHED_EDF_SUPPORT
    list[listIndex]->list_period = default_period;
    list[listIndex]->list_budget = default_runtime;
#endif
    pthread_mutex_init(&list[listIndex]->mt, NULL);
    pthread_cond_init (&list[listIndex]->cv, NULL);

    for(;actorIndex[arrayIndex]!=SEPERATOR; arrayIndex++){
      if(actorIndex[arrayIndex] == TERMINATOR){
        endOfArray=1;
        break;
      }
      LinkedNode *lnode = (LinkedNode *)calloc(1,sizeof(LinkedNode));
      AbstractActorInstance *pInstance=actorInstance[actorIndex[arrayIndex]];
      lnode->obj=pInstance;
      append_node(list[listIndex],lnode);
      pInstance->list=list[listIndex];
      pInstance->extList=(LIST*)calloc(1,sizeof(LIST));
    }
    arrayIndex++;
  }

  //compute the external list per actor instance
  for(i=0; i<numInstances; i++){
    AbstractActorInstance *pInstance=actorInstance[i];
    //set the external list per actor instance
    computeExtList2(pInstance->extList,pInstance);
//     print_extList(pInstance);
#ifdef XML_TRACE
    // global action index (used in traces)
    pInstance->firstActionIndex=firstActionIndex;
    firstActionIndex += pInstance->actor->numActions;
#endif
  }

  //compute the external list per list
  for(i=0;i<listIndex;i++){
    LIST *myList=list[i];
    computeExtList(myList);
#ifdef XML_TRACE
    //create file per list
    sprintf(filename,"trace_%d.xml",myList->lid);
    myList->file=xmlCreateTrace(filename);
#endif
  }
  *ptrNumLists = listIndex;
  return list;
}
#endif

void wakeup_waitingList(LIST *list){
  pthread_mutex_lock(&list->mt);
  if(list->state==WAITING_STATE){
    pthread_mutex_lock(&numWaitingThreadsLock);
    numWaitingThread--;
    pthread_mutex_unlock(&numWaitingThreadsLock);
    list->state=WAKEUP_STATE;
    trace(LOG_MUST,"dbus wakeup %d\n",list->lid);
    pthread_cond_signal(&list->cv);
  }else if(list->state==READY_STATE){
    list->state=WAKEUP_STATE;
  }
  pthread_mutex_unlock(&list->mt);
}

void wakeup_waitingLists(LIST *me,LIST *list){
  LinkedNode *lnode,*lnode2;
  int migrateFlag=2;
  for(lnode=list->head;lnode;lnode=lnode->next){
    LIST *extList=(LIST*)lnode->obj;
    pthread_mutex_lock(&extList->mt);
    if(extList->state==WAITING_STATE){
      pthread_mutex_lock(&numWaitingThreadsLock);
      numWaitingThread--;
      pthread_mutex_unlock(&numWaitingThreadsLock);
      //migrate a node from the active list over to the sleeping one
      if(migrateFlag==0){
        lnode2=remove_fired_node(me);
        if(lnode2){
          AbstractActorInstance *pInstance=(AbstractActorInstance *)lnode2->obj;
          pInstance->list=extList; 
          append_node(extList,lnode2);
          computeExtList(extList);
        }  
        migrateFlag=1;
      }  
      extList->state=WAKEUP_STATE;
#ifdef XML_TRACE
      xmlTraceWakeup(me->file,extList->lid);
#endif
      //trace(LOG_MUST,"%d wakeup %d\n",me->lid,extList->lid);
      pthread_cond_signal(&extList->cv);
    }else if(extList->state==READY_STATE){
      extList->state=WAKEUP_STATE;
    }
    pthread_mutex_unlock(&extList->mt);
  }
  if(migrateFlag==1)
    computeExtList(me);
}

void wakeup_waitingThreads(LIST *me,AbstractActorInstance *pInstance){
  ActorClass *actorClass=pInstance->actor;
  InputPort  *inputPort=pInstance->inputPort;
  InputPort  *endInput=inputPort+actorClass->numInputPorts;
  OutputPort *outputPort;
  OutputPort *endOutput;

  //If inputPort updated(data being read), wakeup the writer thread
  for (; inputPort<endInput; ++inputPort){
    AbstractActorInstance *writerActor=inputPort->writerActor;
    LIST *list=writerActor->list;
    if(list!=me&&inputPort->availTokens<inputPort->capacity){
    pthread_mutex_lock(&list->mt);
    if(list->state==WAITING_STATE){
      pthread_mutex_lock(&numWaitingThreadsLock);
      numWaitingThread--;
      pthread_mutex_unlock(&numWaitingThreadsLock);
      list->state=WAKEUP_STATE;
      pthread_cond_signal(&list->cv);
    }else if(list->state==READY_STATE){
      list->state=WAKEUP_STATE;
    }
    pthread_mutex_unlock(&list->mt);
    }
  }

  //If outputPort updated(data being wirtten), wakeup the reader thread
  outputPort=pInstance->outputPort;
  endOutput=outputPort+actorClass->numOutputPorts;
  for (; outputPort<endOutput; ++outputPort){
    int i;
    for(i=0; i<outputPort->numReaders;i++){
      InputPort *in = outputPort->readers[i];
      AbstractActorInstance *readerActor=in->readerActor;
      LIST *list=readerActor->list;
      if(list!=me&&outputPort->availSpace<outputPort->capacity){
      pthread_mutex_lock(&list->mt);
      if(list->state==WAITING_STATE){
        pthread_mutex_lock(&numWaitingThreadsLock);
        numWaitingThread--;
        pthread_mutex_unlock(&numWaitingThreadsLock);
        list->state=WAKEUP_STATE;
        pthread_cond_signal(&list->cv);
      }else if(list->state==READY_STATE){
        list->state=WAKEUP_STATE;
      }
      pthread_mutex_unlock(&list->mt);
      } 
    }
  }
}

// if gettid() is not available in the system headers, but we have
// __NR_gettid, then create a gettid() ourselves: e.g. on ubuntu 7.10
#include <linux/unistd.h>

void register_thread_id(void)
{
  pthread_mutex_lock(&threadIdsMutex);

  // this is gettid(), which does not exist everywhere
  pid_t my_tid = syscall(__NR_gettid);

  threadIds[numberOfCreatedThreads] = my_tid;
  numberOfCreatedThreads++;
  pthread_mutex_unlock(&threadIdsMutex);
}

void unregister_thread_id(void)
{
  int i = 0;
  // this is gettid(), which does not exist everywhere
  pid_t my_tid = syscall(__NR_gettid);

  pthread_mutex_lock(&threadIdsMutex);

  for (i=0; i<numberOfCreatedThreads; i++)
  {
    if (threadIds[i] == my_tid)
    {
      if (i<numberOfCreatedThreads-1)
      {
        threadIds[i] = threadIds[numberOfCreatedThreads-1];
      }
      numberOfCreatedThreads--;
      break;
    }
  }
  pthread_mutex_unlock(&threadIdsMutex);
}

void get_thread_ids(int* count, pid_t** theThreadIds)
{
  if ((count == NULL) || (theThreadIds == NULL))
  {
    return;
  }
  pthread_mutex_lock(&threadIdsMutex);
  *count = numberOfCreatedThreads;
  *theThreadIds = (pid_t*)malloc(numberOfCreatedThreads * sizeof(pid_t));
  memcpy(*theThreadIds, threadIds, numberOfCreatedThreads * sizeof(pid_t));

  pthread_mutex_unlock(&threadIdsMutex);
}

int GetMilliCount(void){
  struct timeb tb;
  ftime( &tb );
  int nCount = tb.millitm + (tb.time & 0xfffff) * 1000;
  return nCount;
}

int GetMilliSpan( int nTimeStart ){
  int nSpan = GetMilliCount() - nTimeStart;
  if ( nSpan < 0 )
    nSpan += 0x100000 * 1000;
  return nSpan;
}

void runActorList(LIST *list)
{
  AbstractActorInstance *pInstance;
  LinkedNode *lnode;
  int networkAliveHack=0;
  unsigned long mask = list->cid;
  unsigned int startCount;
#if defined SCHED_EDF_SUPPORT
  struct sched_param2 sched_edfp;
#endif

  mask = 1<<mask;
  syscall(__NR_sched_setaffinity, 0, sizeof(mask), &mask);

#if defined SCHED_EDF_SUPPORT
  sched_edfp.sched_edf_period = list->list_period;
  sched_edfp.sched_edf_runtime = list->list_budget;
  sched_setscheduler2(0, SCHED_EDF,
                      (struct sched_param2 *)(&sched_edfp));
#endif
 
  trace(LOG_MUST,"Thread[%d] containing %d actors start running at cpu %d......\n",list->lid,list->numNodes,sched_getcpu());
#ifdef RM_SUPPORT
  register_thread_id();
#endif

  while(Running)
  {
    networkAliveHack=0;
    for(lnode=list->head;lnode;lnode=lnode->next){
      pInstance = (AbstractActorInstance*)lnode->obj;
      compute_pinAvail(pInstance);
      pInstance->hasFiredHack=0;
      pInstance->actor->action_scheduler(pInstance);
      if (pInstance->hasFiredHack){
        networkAliveHack=1;
        //Wakeup the waiting thread if there's any
        /* wakeup waiting thread per actor */
        //wakeup_waitingLists(list,pInstance->extList);
        /* wakeup waiting thread per port */ 
        wakeup_waitingThreads(list,pInstance); 
      }
    }
    if(networkAliveHack){
      //Wakeup the waiting thread if there's any
      /* wakeup waiting thread per list */
      //wakeup_waitingLists(list,list->extList); 
    }else{
      //None of the actors in the list fired
      pthread_mutex_lock(&list->mt);
      if(list->state==WAKEUP_STATE){
        //Someone tried to wake me up
        list->state=READY_STATE;
        pthread_mutex_unlock(&list->mt);
        continue;
      }

      pthread_mutex_lock(&numWaitingThreadsLock);
      numWaitingThread++;
      pthread_mutex_unlock(&numWaitingThreadsLock);

      //Terminate if all the threads blocked
      if(numWaitingThread>=numLists){
        stop_run();
        pthread_mutex_unlock(&list->mt);
        //Wakeup the blocked lists so they have a chance to get out
//         printf("%d do wakeup for exit!!\n",list->lid);
        wakeupLists(list);
        break;
      }

      //Going to waiting state
      list->state=WAITING_STATE;
      //trace(LOG_MUST,"%d going to sleep\n",list->lid);
      //trace(LOG_MUST,"%d 0\n",list->lid);
      list->stat.totNumSleeps++;
      startCount = GetMilliCount();
#ifdef XML_TRACE
      xmlTraceStatus(list->file,0);
#endif
      pthread_cond_wait(&list->cv, &list->mt);
      //trace(LOG_MUST,"%d woke up\n",list->lid);
      //trace(LOG_MUST,"%d 1\n",list->lid);
#ifdef XML_TRACE
      xmlTraceStatus(list->file,1);
#endif
      list->stat.totTimeSleeps += GetMilliSpan(startCount);
      list->state=READY_STATE;
      pthread_mutex_unlock(&list->mt);
    }
//     trace(LOG_MUST,"%d %d\n",list->lid,list->numNodes);
  }
  pthread_exit(NULL);
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
      compute_pinAvail(pInstance);
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
  int numCpusToRun=1;
  int numCpusOnline;
  pthread_attr_t attr;
  pthread_t  tid[128];
  unsigned int totNumSleeps=0;
  unsigned int totTimeSleeps=0;
#ifdef XML_TRACE
  FILE *netfile;
#endif

  allocate_buffers(actorInstance, numInstances);
  numCpusOnline = sysconf(_SC_NPROCESSORS_ONLN);
  if(argc>1)
    numCpusToRun = atoi(argv[1]);

  if(numCpusToRun>numCpusOnline)
    numCpusToRun = numCpusOnline;

#ifdef RM_SUPPORT
  pthread_mutex_init(&threadIdsMutex, 0);
  register_thread_id();
#endif
  //catch ctrl-c
  signal(SIGINT, stop_run);

  printf("ModuleName            : %s\n",argv[0]);
  printf("numInstances          : %d\n",numInstances);
  printf("numCpusOnline         : %d\n",numCpusOnline);
  printf("numCpusToRun          : %d\n",numCpusToRun);

#ifdef LIST_BASED
  pthread_mutex_init(&numWaitingThreadsLock,NULL);
  pthread_attr_init(&attr);
  pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

  actorLists = initList(argc,argv,actorInstance,numInstances,&numLists);

//   printActorList(actorLists,numLists);

  //distribute lists on cpu cores
  for(i=0;i<numLists;i++){
    if(i<numCpusToRun)
      actorLists[i]->cid=i;
    else
      actorLists[i]->cid=0;
  }

#ifdef XML_TRACE
  netfile=xmlCreateTrace("net_trace.xml");
  xmlDeclareNetwork(netfile,argv[0],actorInstance,numInstances);
  xmlCloseTrace(netfile);
#endif

  for(i=0; i<numLists; i++){
    pthread_create(&tid[i], &attr, (void*)runActorList, (void *)actorLists[i]);
  }
 
  for (i=0; i<numLists; i++) {
     pthread_join(tid[i], NULL);
  }

  printf("\nCore  NumSleeps   IdleTimes(ms):\n");
  for(i=0; i<numLists; i++){
    printf("%3d %10d %10d\n",
            actorLists[i]->cid,
            actorLists[i]->stat.totNumSleeps,
            actorLists[i]->stat.totTimeSleeps);
    totNumSleeps += actorLists[i]->stat.totNumSleeps;
    totTimeSleeps += actorLists[i]->stat.totTimeSleeps;
#ifdef XML_TRACE
    if(actorLists[i]->file);
      xmlCloseTrace(actorLists[i]->file);
#endif
  }
  printf("Total num sleeps:      %d\n",totNumSleeps);
  printf("Total time sleeps(ms): %d\n",totTimeSleeps);

#else
  runActorBarebone(actorInstance,numInstances);
#endif

  for (i=0; i<numInstances; i++) {
    if(actorInstance[i]->actor->destructor){
      actorInstance[i]->actor->destructor(actorInstance[i]);
    }
  }
#ifdef RM_SUPPORT
  pthread_mutex_destroy(&threadIdsMutex);
#endif
  trace(LOG_MUST,"Exit\n");

  return 0;
}
