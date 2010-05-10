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

/*
 * Actor Source
 */

#include <dbus/dbus.h>

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <pthread.h>
#include <time.h>

#include <string>
#include "actors-rts.h"
#include "internal.h"
#include "dbus/genericdbushandler.h"
#include "custommessages.h"
#include "xmlParser.h"

#define ACTRM_INTERFACE_NAME "eu.actorsproject.ResourceManagerInterface"

static AbstractActorInstance *dbusInstance;

class SystemActorDBusHandler : public GenericDBusHandler
{
   public:
      SystemActorDBusHandler(AbstractActorInstance* base, const char* name);
      ~SystemActorDBusHandler();
      void registerApp();
      void reportHappiness(int happiness);
      void announceCPUCategory();
      void announceServiceLevels();
	  void commit();
      void addThreadToGroup(int tid, int index);
      void addThreadsToGroup();


      bool getNextOutgoingValue(int* value);
      void putNextOutgoingValue(int value);
   protected:
	  void handleCommit(const Message* msg);
      void handleDBusMessage(DBusMessage* message);
      void handleDBusSignal(DBusMessage* message);
      void handleRegisterClient(const Message* msg);
      void handleReportHappiness(const IntMessage* msg);
      void handleAnnounceCPUCategory(const Message *msg);
      void handleAnnounceServiceLevels(const Message* msg);
      void handleAddThreadToGroup(const AddThreadMessage* msg);
      void handleAddThreadsToGroup(const Message* msg);

      std::string m_name;
      pthread_mutex_t m_outgoingValuesMutex;
      std::queue<int> m_outgoingValues;
      AbstractActorInstance* m_baseActorInstance;

};

SystemActorDBusHandler::SystemActorDBusHandler(AbstractActorInstance* base, const char* name)
:m_name(name)
,m_baseActorInstance(base)
{
   pthread_mutex_init(&m_outgoingValuesMutex, 0);
   addMessageHandler(MSG_REGISTER_CLIENT, MAKE_MESSAGE_DELEGATE(this, &SystemActorDBusHandler::handleRegisterClient));
   addMessageHandler(MSG_REPORT_HAPPINESS, MAKE_MESSAGE_DELEGATE(this, &SystemActorDBusHandler::handleReportHappiness));
   addMessageHandler(MSG_CPU_CATEGORY, MAKE_MESSAGE_DELEGATE(this, &SystemActorDBusHandler::handleAnnounceCPUCategory));
   addMessageHandler(MSG_SERVICE_LEVELS, MAKE_MESSAGE_DELEGATE(this, &SystemActorDBusHandler::handleAnnounceServiceLevels));
   addMessageHandler(MSG_ADDTO_GROUP, MAKE_MESSAGE_DELEGATE(this, &SystemActorDBusHandler::handleAddThreadToGroup));
   addMessageHandler(MSG_ADDSTO_GROUP, MAKE_MESSAGE_DELEGATE(this, &SystemActorDBusHandler::handleAddThreadsToGroup));
   addMessageHandler(MSG_COMMIT, MAKE_MESSAGE_DELEGATE(this, &SystemActorDBusHandler::handleCommit));
}

SystemActorDBusHandler::~SystemActorDBusHandler()
{
   pthread_mutex_destroy(&m_outgoingValuesMutex);
}

bool SystemActorDBusHandler::getNextOutgoingValue(int* value)
{
   assert(value);
   bool result = false;
   pthread_mutex_lock(&m_outgoingValuesMutex);

   if (!m_outgoingValues.empty())
   {
      *value = m_outgoingValues.front();
      m_outgoingValues.pop();
      result = true;
   }

   pthread_mutex_unlock(&m_outgoingValuesMutex);

   return result;
}

void SystemActorDBusHandler::putNextOutgoingValue(int value)
{
   assert(value);
   pthread_mutex_lock(&m_outgoingValuesMutex);
   m_outgoingValues.push(value);
   pthread_mutex_unlock(&m_outgoingValuesMutex);
}

void SystemActorDBusHandler::handleDBusMessage(DBusMessage* message)
{
   fprintf(stderr, "DBUS received message %p\n", message);
}

void SystemActorDBusHandler::handleDBusSignal(DBusMessage* message)
{
   fprintf(stderr, "DBUS received signal %p\n", message);
   if (dbus_message_is_signal(message, ACTRM_INTERFACE_NAME, "changeContinuous"))
   {
   }
   else if (dbus_message_is_signal(message, ACTRM_INTERFACE_NAME, "changeServiceLevel"))
   {
      char* receiverName = 0;
      dbus_uint32_t newValue = 0;
      DBusMessageIter args;
      dbus_message_iter_init(message, &args);
      dbus_message_iter_get_basic(&args, &receiverName);
      if (m_dbusName == receiverName)
      {
         dbus_message_iter_next(&args);
         dbus_message_iter_get_basic(&args, &newValue);
         fprintf(stderr, "signal for %s, new value is %d my name is %s\n", receiverName, newValue, dbus_bus_get_unique_name(m_connection));
         putNextOutgoingValue(newValue);
      }
      dbus_message_unref(message);
   }
   else
   {
      const char* signal =dbus_message_get_member(message);
      if(strcmp(signal,"NameAcquired")==0){
        char* name = 0;
        DBusMessageIter args;
        dbus_message_iter_init(message, &args);
        dbus_message_iter_get_basic(&args, &name);
        fprintf(stderr, "%s %s\n", signal,name);
      }
      else
        fprintf(stderr, "Received unknown signal %s\n", signal);
   }
}

void SystemActorDBusHandler::announceServiceLevels()
{
   this->postMessage(new Message(MSG_SERVICE_LEVELS));
}

void SystemActorDBusHandler::handleAnnounceServiceLevels(const Message* serviceLevelsMsg)
{
   bool result;
   DBusMessageIter args1;
   DBusMessage* message = 0;
   DBusPendingCall* pending;

   assert(serviceLevelsMsg);

   message = dbus_message_new_method_call(ACTRM_INTERFACE_NAME, "/", ACTRM_INTERFACE_NAME, "announceServiceLevels");
   dbus_message_iter_init_append(message, &args1);
   
   //current service level
   dbus_message_iter_append_basic(&args1, ((int) 'u'), &rmInterface.currentServiceIndex);
   //service level count
   dbus_message_iter_append_basic(&args1, ((int) 'u'), &rmInterface.numServiceLevels);
   //service levels
   DBusMessageIter args2;
   dbus_message_iter_open_container(&args1, DBUS_TYPE_ARRAY, "(uuuuua{uu})", &args2);
   for(int i=0; i<rmInterface.numServiceLevels;i++)
   {
	   ServiceLevel *slevel = &rmInterface.serviceLevels[i];
	   DBusMessageIter args3;
	   dbus_message_iter_open_container(&args2, DBUS_TYPE_STRUCT, NULL, &args3);
	   //quality of service 
       dbus_message_iter_append_basic(&args3, ((int) 'u'), &slevel->quality);
	   //total BW 
       dbus_message_iter_append_basic(&args3, ((int) 'u'), &slevel->totalBW);
	   //granularity 
       dbus_message_iter_append_basic(&args3, ((int) 'u'), &slevel->granularityValue);
	   //specifier (mode) 
       dbus_message_iter_append_basic(&args3, ((int) 'u'), &slevel->mode);
	   //BW distrinution count
	   dbus_message_iter_append_basic(&args3, ((int) 'u'), &slevel->numBMDistributions);

	   DBusMessageIter args4;
	   dbus_message_iter_open_container(&args3, DBUS_TYPE_ARRAY, "{uu}", &args4);
	   for (int j=0; j<slevel->numBMDistributions; j++)
	   {
		   BWDistribution *bw=&slevel->bwDistributions[j];
		   int groupID=bw->id + rmInterface.groupIDBase;
           DBusMessageIter args5;
		   dbus_message_iter_open_container(&args4, DBUS_TYPE_DICT_ENTRY, NULL, &args5);
		   //group ID
		   dbus_message_iter_append_basic(&args5, ((int) 'u'), &groupID);
		   //bandwidth
		   dbus_message_iter_append_basic(&args5, ((int) 'u'), &bw->value);
		   dbus_message_iter_close_container(&args4, &args5);
	   }
       dbus_message_iter_close_container(&args3, &args4);
       dbus_message_iter_close_container(&args2, &args3);
   }
   dbus_message_iter_close_container(&args1, &args2);

   // handle the reply
   //dbus_uint32_t replies = 0; 
   //result = dbus_connection_send(m_connection, message, &replies);
   //if(result==false)
   //   fprintf(stderr, "dbus_connection_send failed\n");
   //else
   //   fprintf(stderr, "dbus_connection_send announceServiceLevels\n");
   //dbus_connection_flush(m_connection);
   //dbus_message_unref(message);
   if(!dbus_connection_send_with_reply(m_connection, message, &pending,-1))
   {
	  fprintf(stderr, "Out Of Memory!\n");
	  return;
   }
   if (NULL == pending) { 
	  fprintf(stderr, "Pending Call Null\n"); 
	  return; 
   }
   dbus_connection_flush(m_connection);
   dbus_message_unref(message);
   //block until we receive a reply
   dbus_pending_call_block(pending);
   fprintf(stderr, "dbus_connection_send announceServiceLevels\n");
}

void SystemActorDBusHandler::commit()
{
   this->postMessage(new Message(MSG_COMMIT));
}

void SystemActorDBusHandler::handleCommit(const Message* commitMsg)
{
   bool result;
  
   assert(commitMsg);

   DBusMessage* methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
                                                             "/",
                                                             ACTRM_INTERFACE_NAME,
                                                             "commit");
   DBusMessageIter args;
   dbus_message_iter_init_append(methodCallMsg, &args);

   //const char* tmpString = m_name.c_str();

   dbus_uint32_t replies = 0;
   result = dbus_connection_send(m_connection, methodCallMsg, &replies);
   if(result==false)
      fprintf(stderr, "dbus_connection_send failed\n");
   else
      fprintf(stderr, "dbus_connection_send commit\n");

   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);
}

void SystemActorDBusHandler::announceCPUCategory()
{
   this->postMessage(new Message(MSG_CPU_CATEGORY));
}

void SystemActorDBusHandler::handleAnnounceCPUCategory(const Message* categoryMsg)
{
   bool result;
   DBusPendingCall* pending;
   assert(categoryMsg);
   DBusMessage* methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
                                                             "/",
                                                             ACTRM_INTERFACE_NAME,
                                                             "announceCPUCategory");
   DBusMessageIter args;
   dbus_message_iter_init_append(methodCallMsg, &args);

   const char* tmpString = m_name.c_str();
   int value = rmInterface.categoryValue;
   dbus_message_iter_append_basic(&args, DBUS_TYPE_INT32, &value);

   //dbus_uint32_t replies = 0;
   //result = dbus_connection_send(m_connection, methodCallMsg, &replies);
   //if(result==false)
   //   fprintf(stderr, "dbus_connection_send failed\n");
   //else
   //   fprintf(stderr, "dbus_connection_send announceCPUCategory %d\n",value);
   //dbus_connection_flush(m_connection);
   //dbus_message_unref(methodCallMsg);
   if(!dbus_connection_send_with_reply(m_connection, methodCallMsg, &pending,-1))
   {
	  fprintf(stderr, "Out Of Memory!\n");
	  return;
   }
   if (NULL == pending) { 
	  fprintf(stderr, "Pending Call Null\n"); 
	  return; 
   }
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);
   //block until we receive a reply
   dbus_pending_call_block(pending);
   fprintf(stderr, "dbus_connection_send announceCPUCategory\n");
}

void SystemActorDBusHandler::addThreadsToGroup()
{
   this->postMessage(new Message(MSG_ADDSTO_GROUP));
}

//add threads to the thread group
void SystemActorDBusHandler::handleAddThreadsToGroup(const Message* addsToGroup)
{
   //register thread groups
   DBusMessageIter args;
   int             numberOfThreads;
   ThreadID        *theThreadIDs;
   pid_t           *threadIds;
   DBusMessage*    methodCallMsg;
   DBusPendingCall *pending;

   numberOfThreads=get_thread_ids(&theThreadIDs);

   if(numberOfThreads==0)
      return;

   threadIds = (pid_t*)malloc(numberOfThreads * sizeof(pid_t));

   int numThreads = 1; 
   for(int i=0; i< numberOfThreads; i++){
   unsigned int threadGroup = rmInterface.groupIDBase+theThreadIDs[i].cpu;
   threadIds[0] = (pid_t)theThreadIDs[i].id;
   methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
                                                "/",
                                                ACTRM_INTERFACE_NAME,
                                                "addThreadsToGroup");
   dbus_message_iter_init_append(methodCallMsg, &args);
   dbus_message_iter_append_basic(&args, DBUS_TYPE_UINT32, &threadGroup);
   dbus_message_iter_append_basic(&args, DBUS_TYPE_UINT32, &numThreads);
   DBusMessageIter subArgs;
   dbus_message_iter_open_container(&args, DBUS_TYPE_ARRAY, DBUS_TYPE_UINT32_AS_STRING, &subArgs);
   dbus_message_iter_append_fixed_array(&subArgs, DBUS_TYPE_UINT32, &threadIds, numThreads);
   dbus_message_iter_close_container(&args, &subArgs);

   //replies = 0;
   //dbus_connection_send(m_connection, methodCallMsg, &replies);
   //dbus_connection_flush(m_connection);
   //dbus_message_unref(methodCallMsg);
   if(!dbus_connection_send_with_reply(m_connection, methodCallMsg, &pending,-1))
   {
	  fprintf(stderr, "Out Of Memory!\n");
	  return;
   }
   if (NULL == pending) { 
	  fprintf(stderr, "Pending Call Null\n"); 
	  return; 
   }
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);
   //block until we receive a reply
   dbus_pending_call_block(pending);

   fprintf(stderr, "Added the thread %d to group %d\n",threadIds[0],threadGroup);   
   }

   free(threadIds);

   handleCommit(0);
}

void SystemActorDBusHandler::addThreadToGroup(int tid, int group)
{
   this->postMessage(new AddThreadMessage(MSG_ADDTO_GROUP,tid,group));
}

//add thread id to the thread group
void SystemActorDBusHandler::handleAddThreadToGroup(const AddThreadMessage* addToGroup)
{

   DBusMessageIter args;
   int             numberOfThreads = 1;
   unsigned int    threadGroup;
   DBusMessage*    methodCallMsg;
   dbus_uint32_t   replies = 0;
   pid_t*          threadIds;
   DBusPendingCall* pending;

   assert(addToGroup);

   threadIds = (pid_t*)malloc(numberOfThreads * sizeof(pid_t));
   threadIds[0] = (pid_t)addToGroup->getTid();

   threadGroup = rmInterface.groupIDBase + addToGroup->getGroup();

   methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
                                                "/",
                                                ACTRM_INTERFACE_NAME,
                                                "addThreadsToGroup");

   dbus_message_iter_init_append(methodCallMsg, &args);

   dbus_message_iter_append_basic(&args, DBUS_TYPE_UINT32, &threadGroup);
   dbus_message_iter_append_basic(&args, DBUS_TYPE_UINT32, &numberOfThreads);

   DBusMessageIter subArgs;
   dbus_message_iter_open_container(&args, DBUS_TYPE_ARRAY, DBUS_TYPE_UINT32_AS_STRING, &subArgs);

   dbus_message_iter_append_fixed_array(&subArgs, DBUS_TYPE_UINT32, &threadIds, numberOfThreads);
   dbus_message_iter_close_container(&args, &subArgs);

   //replies = 0;
   //dbus_connection_send(m_connection, methodCallMsg, &replies);
   //dbus_connection_flush(m_connection);
   //dbus_message_unref(methodCallMsg);
   if(!dbus_connection_send_with_reply(m_connection, methodCallMsg, &pending,-1))
   {
	  fprintf(stderr, "Out Of Memory!\n");
	  return;
   }
   if (NULL == pending) { 
	  fprintf(stderr, "Pending Call Null\n"); 
	  return; 
   }
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);
   //block until we receive a reply
   dbus_pending_call_block(pending);
 
   fprintf(stderr, "Added the thread %d to group %d\n",threadIds[0],threadGroup);

   free(threadIds);

   handleCommit(0);
}

void SystemActorDBusHandler::reportHappiness(int happiness)
{
   this->postMessage(new IntMessage(MSG_REPORT_HAPPINESS, happiness));
}

void SystemActorDBusHandler::handleReportHappiness(const IntMessage* happinessMsg)
{
   bool result;
   assert(happinessMsg);
   DBusMessage* methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
                                                             "/",
                                                             ACTRM_INTERFACE_NAME,
                                                             "reportHappiness");
   DBusMessageIter args;
   dbus_message_iter_init_append(methodCallMsg, &args);

   const char* tmpString = m_name.c_str();
   int value = happinessMsg->getValue();
//    dbus_message_iter_append_basic(&args, DBUS_TYPE_STRING, &tmpString);
   dbus_message_iter_append_basic(&args, DBUS_TYPE_INT32, &value);

   dbus_uint32_t replies = 0;
   result = dbus_connection_send(m_connection, methodCallMsg, &replies);
   if(result==false)
      fprintf(stderr, "dbus_connection_send failed\n");
   else
      fprintf(stderr, "dbus_connection_send happiness %d\n",value);
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);

}

void SystemActorDBusHandler::registerApp()
{
   this->postMessage(new Message(MSG_REGISTER_CLIENT));
}

void SystemActorDBusHandler::handleRegisterClient(const Message* registerClientMsg)
{
   DBusPendingCall* pending;

   assert(registerClientMsg);

   //register application
   DBusMessage* methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
                                                             "/",
                                                             ACTRM_INTERFACE_NAME,
                                                             "registerApp");
   DBusMessageIter args;
   dbus_message_iter_init_append(methodCallMsg, &args);

   const char* tmpString = m_name.c_str();
   dbus_message_iter_append_basic(&args, DBUS_TYPE_STRING, &tmpString);
   dbus_uint32_t replies = 0;
//   dbus_connection_send(m_connection, methodCallMsg, &replies);
//   dbus_connection_flush(m_connection);
//   dbus_message_unref(methodCallMsg);
   if(!dbus_connection_send_with_reply(m_connection, methodCallMsg, &pending,-1))
   {
	  fprintf(stderr, "Out Of Memory!\n");
	  return;
   }
   if (NULL == pending) { 
      fprintf(stderr, "Pending Call Null\n"); 
      return; 
   }
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);
   //block until we receive a reply
   dbus_pending_call_block(pending);

   fprintf(stderr, "dbus_connection_send registerApp\n");

   //create thread group
   for(int i=0; i<numPartitions; i++)
   {
	   unsigned int threadGroup = rmInterface.groupIDBase+i;
	   methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
													"/",
													 ACTRM_INTERFACE_NAME,
													"createThreadGroup");
	   dbus_message_iter_init_append(methodCallMsg, &args);
	   dbus_message_iter_append_basic(&args, DBUS_TYPE_UINT32, &threadGroup);

	   //replies = 0;
	   //dbus_connection_send(m_connection, methodCallMsg, &replies);
	   //dbus_connection_flush(m_connection);
	   //dbus_message_unref(methodCallMsg);
	   if(!dbus_connection_send_with_reply(m_connection, methodCallMsg, &pending,-1))
	   {
		  fprintf(stderr, "Out Of Memory!\n");
		  return;
	   }
	   if (NULL == pending) { 
		  fprintf(stderr, "Pending Call Null\n"); 
		  return; 
	   }
	   dbus_connection_flush(m_connection);
	   dbus_message_unref(methodCallMsg);
	   //block until we receive a reply
	   dbus_pending_call_block(pending);
		   
	   fprintf(stderr, "dbus_connection_send createThreadGroup: %d\n",threadGroup);
   }
}

typedef struct {
   AbstractActorInstance base;
   SystemActorDBusHandler* dbusHandler;
} DBusActorInstance;

extern "C"
{

ART_ACTION_CONTEXT(1,1)

ART_ACTION_SCHEDULER(a_action_scheduler);
static void constructor(AbstractActorInstance*);
static void destructor(AbstractActorInstance*);
static void set_param(AbstractActorInstance*, const char*, const char*);
}

static const PortDescription inputPortDescriptions[]={
  {"In", sizeof(int32_t)}
};

static const PortDescription outputPortDescriptions[]={
  {"Out", sizeof(int32_t)}
};

static const int portRate_0[] = {
  0
};

static const int portRate_1[] = {
  1
};

static const ActionDescription actionDescriptions[] = {
  {"reportHappiness", portRate_0, portRate_1},
  {"dbusInput",       portRate_1, portRate_0}
};

static const int exitcode_block_In_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};

/* actor interface functions */
/*
ActorClass ActorClass_art_DBus_test = INIT_ActorClass(
  "art_DBus_test",
  DBusActorInstance,
  constructor,
  set_param,
  a_action_scheduler,
  destructor,
  1, inputPortDescriptions,
  1, outputPortDescriptions,
  2, actionDescriptions
);
*/

ActorClass ActorClass_art_DBus_test = {
  "art_DBus_test",
  1,
  1,
  sizeof(DBusActorInstance),
  a_action_scheduler,
  constructor,
  destructor,
  set_param,
  inputPortDescriptions,
  outputPortDescriptions,
  0,
  2,
  actionDescriptions
};

extern "C"
{

ART_ACTION_SCHEDULER(a_action_scheduler){
   DBusActorInstance* thisActor = (DBusActorInstance*) pBase;
   const int *exitCode=EXIT_CODE_YIELD;
   int value = 0;
   bool result;
   int  available;
   static time_t lastHappinessTime = 0;

   ART_ACTION_SCHEDULER_ENTER(1, 1);

   result = thisActor->dbusHandler->getNextOutgoingValue(&value);
   available = pinAvailIn_int32_t(ART_INPUT(0));

   ART_ACTION_SCHEDULER_LOOP {
   ART_ACTION_SCHEDULER_LOOP_TOP;

   if (result)
   {
      fprintf(stderr, "got data %d\n", value);
      int space=pinAvailOut_int32_t(ART_OUTPUT(0));
      if(space>=1)
      {
         ART_ACTION_ENTER("dbusInput",0);
         pinWrite_int32_t(ART_OUTPUT(0), value); 
      }
   }

   // report happiness once per second
   if(available>0)
   {
      available--;
      ART_ACTION_ENTER("reportHappiness",1);
      value = pinRead_int32_t(ART_INPUT(0));
//       fprintf(,stderr,"time(0)(%d)-last(%d)=%d value=%d\n",time(0),lastHappinessTime,time(0)-lastHappinessTime,value);
      if ((time(0) - lastHappinessTime) > 0)
      {
         lastHappinessTime = time(0);
         thisActor->dbusHandler->reportHappiness(value);
      }
   }
   else
   {
      exitCode=exitcode_block_In_1;
      goto action_scheduler_exit;
   }
   ART_ACTION_SCHEDULER_LOOP_BOTTOM;
   }

action_scheduler_exit:
   ART_ACTION_SCHEDULER_EXIT(1,1);
   return exitCode;
}

static void constructor(AbstractActorInstance *pBase) {

   int i;
   DBusActorInstance* thisActor = (DBusActorInstance*) pBase;
   thisActor->dbusHandler = new SystemActorDBusHandler(&thisActor->base, rmInterface.name);

   int result = thisActor->dbusHandler->init();

   thisActor->dbusHandler->resumeThread();

   fprintf(stderr, "result from dbus init: %d\n", result);
   if (result !=0)
   {
   }

   //register app 
   thisActor->dbusHandler->registerApp();

   //announce service levels;
   thisActor->dbusHandler->announceServiceLevels();

   //announce cpu category
   thisActor->dbusHandler->announceCPUCategory();

   //commit
   //thisActor->dbusHandler->commit();

   dbusInstance = pBase;
}

static void destructor(AbstractActorInstance *pBase)
{
   DBusActorInstance *thisActor=(DBusActorInstance*) pBase;
   thisActor->dbusHandler->exitThread();
   thisActor->dbusHandler->joinThread();
   delete thisActor->dbusHandler;
   thisActor->dbusHandler = 0;
}

static void set_param(AbstractActorInstance *pBase,const char *key,const char *value){
}

void add_threads_to_group()
{
  if(dbusInstance){
    DBusActorInstance *thisActor=(DBusActorInstance*) dbusInstance;
    thisActor->dbusHandler->addThreadsToGroup();
  }
}

void add_thread_to_group(int tid, int index)
{
  if(dbusInstance){
    DBusActorInstance *thisActor=(DBusActorInstance*) dbusInstance;
    thisActor->dbusHandler->addThreadToGroup(tid,index);
  }
}

}


