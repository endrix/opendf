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


#include "dbus/genericdbushandler.h"

#include "custommessages.h"

#define ACTRM_INTERFACE_NAME "eu.actorsproject.ResourceManagerInterface"

class SystemActorDBusHandler : public GenericDBusHandler
{
   public:
      SystemActorDBusHandler(AbstractActorInstance* base, const char* name);
      ~SystemActorDBusHandler();
      void registerApp();
      void reportHappiness(int happiness);


      bool getNextOutgoingValue(int* value);
      void putNextOutgoingValue(int value);
   protected:
      void handleDBusMessage(DBusMessage* message);
      void handleDBusSignal(DBusMessage* message);
      void handleRegisterClient(const Message* msg);
      void handleReportHappiness(const IntMessage* msg);

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
#warning unsynchronized access to execState
         wakeup_waitingList(m_baseActorInstance->list);
      }
//      dbus_message_unref(message);
   }
   else if (dbus_message_is_signal(message, ACTRM_INTERFACE_NAME, "changeQualityLevel"))
   {

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


void SystemActorDBusHandler::reportHappiness(int happiness)
{
   this->postMessage(new IntMessage(MSG_REPORT_HAPPINESS, happiness));
}


void SystemActorDBusHandler::handleReportHappiness(const IntMessage* happinessMsg)
{
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
   dbus_connection_send(m_connection, methodCallMsg, &replies);
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);
}


void SystemActorDBusHandler::registerApp()
{
   this->postMessage(new Message(MSG_REGISTER_CLIENT));
}


void SystemActorDBusHandler::handleRegisterClient(const Message* registerClientMsg)
{
   assert(registerClientMsg);

   register_thread_id();

   //sleep(2);

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
   dbus_connection_send(m_connection, methodCallMsg, &replies);
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);

   //create thread group 
   unsigned int threadGroup = 21;
   methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
                                                "/",
                                                 ACTRM_INTERFACE_NAME,
                                                "createThreadGroup");
   dbus_message_iter_init_append(methodCallMsg, &args);
   dbus_message_iter_append_basic(&args, DBUS_TYPE_UINT32, &threadGroup);

   replies = 0;
   dbus_connection_send(m_connection, methodCallMsg, &replies);
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);

   //register thread groups
   int numberOfThreads = 0;
   pid_t* threadIds = 0;
   get_thread_ids(&numberOfThreads, &threadIds);
   fprintf(stderr, "******* %d threads are registered\n", numberOfThreads);
   for (int i = 0; i< numberOfThreads; i++)
   {
      fprintf(stderr, "*** thread %d is %d\n", i, threadIds[i]);
   }

   methodCallMsg = dbus_message_new_method_call(ACTRM_INTERFACE_NAME,
                                                "/",
                                                ACTRM_INTERFACE_NAME,
                                                "addThreadsToGroup");
///
   dbus_message_iter_init_append(methodCallMsg, &args);
//    dbus_message_iter_append_basic(&args, DBUS_TYPE_STRING, &tmpString);
   dbus_message_iter_append_basic(&args, DBUS_TYPE_UINT32, &threadGroup);
   dbus_message_iter_append_basic(&args, DBUS_TYPE_UINT32, &numberOfThreads);
   DBusMessageIter subArgs;

   dbus_message_iter_open_container(&args, DBUS_TYPE_ARRAY, DBUS_TYPE_UINT32_AS_STRING, &subArgs);

   dbus_message_iter_append_fixed_array(&subArgs, DBUS_TYPE_UINT32, &threadIds, numberOfThreads);
   dbus_message_iter_close_container(&args, &subArgs);

   replies = 0;
   dbus_connection_send(m_connection, methodCallMsg, &replies);
   dbus_connection_flush(m_connection);
   dbus_message_unref(methodCallMsg);

   free(threadIds);

   fprintf(stderr, "Sent thread ids\n");

}


//#define OUT0_Result				base.outputPort[0]
//#define OUT0_TOKENSIZE			base.outputPort[0].tokenSize

typedef struct {
   AbstractActorInstance base;
   SystemActorDBusHandler* dbusHandler;
} DBusActorInstance;

extern "C"
{
static const int *a_action_scheduler(AbstractActorInstance*);
static void constructor(AbstractActorInstance*);
static void destructor(AbstractActorInstance*);
static void set_param(AbstractActorInstance*, const char*, const char*);
}

#define OUT0_Out(thisActor) OUTPUT_PORT(thisActor->base,0)
#define IN0_In(thisActor)   INPUT_PORT(thisActor->base,0)

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

static const int *a_action_scheduler(AbstractActorInstance *pBase) {
//   fprintf(stderr, "********************\n");
   DBusActorInstance* thisActor = (DBusActorInstance*) pBase;
   int value = 0;
   bool result = thisActor->dbusHandler->getNextOutgoingValue(&value);
   if (result)
   {
      fprintf(stderr, "got data %d\n", value);
      int available=pinAvailOut_int32_t(OUT0_Out(thisActor));
      if(available>=1)
      {
        TRACE_ACTION(&thisActor->base, 0, "dbusInput");
         pinWrite_int32_t(OUT0_Out(thisActor), value); 
      }
      else
        pinWaitOut(OUT0_Out(thisActor),1);
   }

   int available = pinAvailIn_int32_t(IN0_In(thisActor));
//    fprintf(stderr, "----------- available: %d TOKENSIZE: %d\n", available, thisActor->IN0_TOKENSIZE);

   // report happiness once per second
   static time_t lastHappinessTime = 0;
   for (int i = 0; i<available; i++)
   {
      TRACE_ACTION(&thisActor->base, 0, "reportHappiness");
      value = pinRead_int32_t(IN0_In(thisActor));
      if ((time(0) - lastHappinessTime) > 0)
      {
         lastHappinessTime = time(0);
         thisActor->dbusHandler->reportHappiness(value);
         //fprintf(stderr, "-------------- %d\n", value);
      }
   }
   return exitcode_block_In_1;
}

static void constructor(AbstractActorInstance *pBase) {

   DBusActorInstance* thisActor = (DBusActorInstance*) pBase;
   thisActor->dbusHandler = new SystemActorDBusHandler(&thisActor->base, "caltest");

   int result = thisActor->dbusHandler->init();
   thisActor->dbusHandler->resumeThread();

   fprintf(stderr, "result from dbus init: %d\n", result);
   if (result !=0)
   {
   }

   thisActor->dbusHandler->registerApp();
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

}

