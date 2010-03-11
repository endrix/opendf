/*
 * Copyright (c) TU Kaiserslautern, 2009
 * Author: Alexander Neundorf (neundorf@eit.uni-kl.de)
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
#include <assert.h>
#include <unistd.h>

#include <algorithm>

//#include "log.h"

#include "genericdbushandler.h"

GenericDBusHandler::GenericDBusHandler()
:m_connection(0)
,m_dbusName("")
,m_notificationPipeIndex(-1)
,m_exitMainloop(false)
,m_suspended(true)
,m_dbusEventOccured(false)
,m_pollFdCount(0)
#ifdef USE_DBUS_PEER_TO_PEER
,m_server(0)
#endif
{
   addMessageHandler(MSG_EXIT_THREAD,          MAKE_MESSAGE_DELEGATE(this, &GenericDBusHandler::handleExitThread));

   m_notificationPipe[0] = -1;
   m_notificationPipe[1] = -1;
   f = fopen("/tmp/fibdbuslog", "w+");
   pthread_mutex_init(&m_notificationMutex, 0);

   setupPollFds();
}


GenericDBusHandler::~GenericDBusHandler()
{
   fprintf(f, "~GenericDBusHandler()\n");
   pthread_mutex_destroy(&m_notificationMutex);

   closeNotificationPipe();
}


void GenericDBusHandler::closeNotificationPipe()
{
   if (m_notificationPipe[0]!=-1)
   {
      close(m_notificationPipe[0]);
   }
   if (m_notificationPipe[1]!=-1)
   {
      close(m_notificationPipe[1]);
   }
   m_notificationPipeIndex = -1;
}


#ifdef USE_DBUS_PEER_TO_PEER

int GenericDBusHandler::initServer()
{
   DBusError error;
   dbus_error_init (&error);
   m_server = dbus_server_listen("unix:path=/tmp/actrm", &error);
   if (m_server == 0)
   {
      fprintf(stderr, "initServer() dbus_server_listen() returned 0\n");
      return -1;
   }
   fprintf(f, "initServer() succsessfully created server\n");

   dbus_server_set_new_connection_function(m_server, GenericDBusHandler::newConnectionCallbackHelper, this, 0);

   if (!dbus_server_set_watch_functions(m_server,
        GenericDBusHandler::watchAddedCallbackHelper,
        GenericDBusHandler::watchRemovedCallbackHelper,
        GenericDBusHandler::watchToggledCallbackHelper,
        this,
        0))
   {
      fprintf(stderr, "dbus_server_set_watch_functions failed\n");
      return -2;
   }

   return 0;
}


void GenericDBusHandler::newConnectionCallbackHelper(DBusServer* server, DBusConnection* connection, void* data)
{
   assert(data!=0);
   GenericDBusHandler* h = (GenericDBusHandler*)data;
   h->newConnectionCallback(server, connection);
}


void GenericDBusHandler::newConnectionCallback(DBusServer* server, DBusConnection* connection)
{
   assert(server == m_server);
   assert(connection != 0);
   fprintf(f, "newConnectionCallback() got new connection\n");
   dbus_connection_ref(connection);
   m_connections.push_back(connection);
}
#endif


int GenericDBusHandler::init()
{
#ifdef USE_DBUS_PEER_TO_PEER
   initServer();
#endif

   int result = pipe(m_notificationPipe);
   fprintf(f, "notificationPipe: %d %d\n", m_notificationPipe[0], m_notificationPipe[1]);
   if (result != 0)
   {
      return -1;
   }

   // dbus setup
   DBusError err;
   dbus_error_init(&err);

//    m_connection = dbus_bus_get(DBUS_BUS_SESSION, &err);
   m_connection = dbus_bus_get(DBUS_BUS_SYSTEM, &err);
   if (dbus_error_is_set(&err))
   {
      fprintf(f, "Connection Error(%s)\n", err.message);
      dbus_error_free(&err);
   }
   if (m_connection==0)
   {
      return -2;
   }

   const char* myInterfaceName = interfaceName();
   if (myInterfaceName != 0)
   {
      result = dbus_bus_request_name(m_connection, interfaceName(), DBUS_NAME_FLAG_REPLACE_EXISTING, &err);
      if (dbus_error_is_set(&err))
      {
         fprintf(f, "Name Error (%s)\n", err.message);
         dbus_error_free(&err);
         // TODO: what to do with the connection in this case ?
         return -3;
      }
   }

   result = setupConnection(m_connection);
   if (result!=0)
   {
      return result;
   }

   // use the NameOwerChanged signal to detect if one of our clients has disconnected from the bus
//   dbus_bus_add_match(m_connection, "type='signal',interface='org.freedesktop.DBus',member='NameOwnerChanged'", &err);
   dbus_bus_add_match(m_connection, "type='signal',interface='eu.actorsproject.ResourceManagerInterface'", &err);
   if (dbus_error_is_set(&err))
   {
      fprintf(f, "AddMatch Error (%s)\n", err.message);
      dbus_error_free(&err);
      // TODO: what to do with the connection in this case ?
      return -6;
   }

   m_dbusName = dbus_bus_get_unique_name(m_connection);

   pthread_create(&m_mainloopThread, 0, &GenericDBusHandler::mainloopHelper, (void*) this);

   return 0;
}


int GenericDBusHandler::setupConnection(DBusConnection* connection)
{
   if (!dbus_connection_set_watch_functions(connection,
        GenericDBusHandler::watchAddedCallbackHelper,
        GenericDBusHandler::watchRemovedCallbackHelper,
        GenericDBusHandler::watchToggledCallbackHelper,
        this,
        NULL))
   {
      fprintf(stderr, "dbus_connection_set_watch_functions failed\n");
      return -4;
   }

   if (!dbus_connection_add_filter(connection, GenericDBusHandler::messageFilterHelper, this, 0))
   {
      fprintf(stderr, "could not install message filter\n");
      return -5;
   }

   setupPollFds();
   return 0;
}


void GenericDBusHandler::addMessageHandler(int messageId, MessageDelegate callback)
{
   m_messageHandlers[messageId] = callback;
}


void GenericDBusHandler::postMessage(const Message* msg)
{
   if (msg!=0)
   {
      pthread_mutex_lock(&m_notificationMutex);
      m_messageQueue.push(msg);
      char dummy = 0;
      write(m_notificationPipe[1], &dummy, sizeof(dummy));
      pthread_mutex_unlock(&m_notificationMutex);
   }
}


bool GenericDBusHandler::handleMessage(const Message* msg)
{
   assert(msg!=0);
   std::map<int, MessageDelegate>::const_iterator delegateIt = m_messageHandlers.find(msg->getId());
   if (delegateIt == m_messageHandlers.end())
   {
      fprintf(stderr, "no message handler: %d\n", msg->getId());
      return false;
   }

   delegateIt->second(msg);
   return true;
}


void GenericDBusHandler::mainloop()
{
//    FILE* f = fopen("/tmp/fibdbuslog", "w+");
   // this is the mainloop
   while (m_exitMainloop == false)
   {
      const Message* msg = 0;

      // lock the mutex, keep that section short
      pthread_mutex_lock(&m_notificationMutex);
      // this is the "wait for something to happen"-loop
      while (m_suspended || ((m_messageQueue.empty()) && (m_dbusEventOccured==false)))
      {
         setupPollFds();
//          fprintf(stderr, "Calling poll...\n");
         fprintf(f, "Calling poll...\n");

         pthread_mutex_unlock(&m_notificationMutex);
         int numberOfFds = poll(m_pollFds, m_pollFdCount, -1);
         pthread_mutex_lock(&m_notificationMutex);
         fprintf(f, "poll() returned %d pipe: %d\n", numberOfFds, m_notificationPipeIndex);

         if (numberOfFds > 0)
         {
            // index zero is always the notification pipe
            if ((m_notificationPipeIndex >= 0) && (m_pollFds[m_notificationPipeIndex].revents != 0))
            {
               if (m_pollFds[m_notificationPipeIndex].revents & POLLIN)
               {
                  char dummy;
                  read(m_notificationPipe[0], &dummy, 1);
                  fprintf(f, "pipe POLLIN\n");
               }
               if (m_pollFds[m_notificationPipeIndex].revents & POLLNVAL)
               {
                  fprintf(f, "pipe POLLNVAL\n");
                  closeNotificationPipe();
               }

               numberOfFds--;
            }

            for (unsigned int i = 0; i< m_pollFdCount; i++)
            {
               if (m_pollFds[i].revents & POLLIN)
               {
                   fprintf(f, "fd %d: POLLIN\n", i);

               }
               if (m_pollFds[i].revents & POLLNVAL)
               {
                  fprintf(f, "fd %d: POLLNVAL\n", i);
               }
               if (m_pollFds[i].revents & POLLPRI)
               {
                  fprintf(f, "fd %d: POLLPRI\n", i);
               }
               if (m_pollFds[i].revents & POLLOUT)
               {
                  fprintf(f, "fd %d: POLLOUT\n", i);
               }
               if (m_pollFds[i].revents & POLLRDHUP)
               {
                  fprintf(f, "fd %d: POLLRDHUP\n", i);
               }
               if (m_pollFds[i].revents & POLLERR)
               {
                  fprintf(f, "fd %d: POLLERR\n", i);
               }
               if (m_pollFds[i].revents & POLLHUP)
               {
                  fprintf(f, "fd %d: POLLHUP\n", i);
               }
            }

            if (numberOfFds > 0)
            {
               m_dbusEventOccured = true;
            }
         }
      }

      if (m_messageQueue.empty()==false)
      {
         msg = m_messageQueue.front();
         m_messageQueue.pop();
      }
      pthread_mutex_unlock(&m_notificationMutex);
      // mutex is unlocked now again

      // handle dbus
      // fd at index==0 is the notification pipe, so start after this
      if (m_dbusEventOccured)
      {
         for (unsigned int i=1; i<m_pollFdCount; i++)
         {
            if (m_pollFds[i].revents & POLLIN)
            {
               handleDBusWatchEvent(m_dbusWatches[i-1], m_pollFds[i].revents);
               m_dbusEventOccured = false;
            }
         }
      }

      // handle the message from the message queue
      if (msg != 0)
      {
         handleMessage(msg);
         delete msg;
         msg = 0;
      }
   }
}


void* GenericDBusHandler::mainloopHelper(void* pointerToThis)
{
   GenericDBusHandler* dbusHandler = (GenericDBusHandler*) pointerToThis;
   dbusHandler->mainloop();
   return 0;
}


void GenericDBusHandler::exitThread()
{
   this->postMessage(new Message(MSG_EXIT_THREAD));
}


void GenericDBusHandler::handleExitThread(const Message*)
{
   m_exitMainloop = true;
}


int GenericDBusHandler::joinThread()
{
   int result = pthread_join(m_mainloopThread, NULL);
   return result;
}


void GenericDBusHandler::suspendThread()
{
   pthread_mutex_lock(&m_notificationMutex);
   m_suspended = true;
   // send the signal, so the thread really goes into the non-timeout cond_wait
   char dummy = 0;
   write(m_notificationPipe[1], &dummy, sizeof(dummy));
   pthread_mutex_unlock(&m_notificationMutex);
}


void GenericDBusHandler::resumeThread()
{
   pthread_mutex_lock(&m_notificationMutex);
   m_suspended = false;
   char dummy = 0;
   write(m_notificationPipe[1], &dummy, sizeof(dummy));
   pthread_mutex_unlock(&m_notificationMutex);
}


void GenericDBusHandler::setupPollFds()
{
   m_pollFdCount = 0;
   if (m_notificationPipe[0] != -1)
   {
      m_pollFds[m_pollFdCount].fd = m_notificationPipe[0];
      m_pollFds[m_pollFdCount].events = POLLIN;
      m_pollFds[m_pollFdCount].revents = 0;
      m_notificationPipeIndex = m_pollFdCount;
      m_pollFdCount++;
   }

   for (std::vector<DBusWatch*>::const_iterator it = m_dbusWatches.begin(); it != m_dbusWatches.end(); ++it)
   {
#if (DBUS_VERSION_MAJOR == 1 && DBUS_VERSION_MINOR == 1 && DBUS_VERSION_MICRO >= 1) || (DBUS_VERSION_MAJOR == 1 && DBUS_VERSION_MAJOR > 1) || (DBUS_VERSION_MAJOR > 1)
	  m_pollFds[m_pollFdCount].fd=dbus_watch_get_unix_fd(*it);
#else
	  m_pollFds[m_pollFdCount].fd=dbus_watch_get_fd(*it);
#endif
      //m_pollFds[m_pollFdCount].fd = dbus_watch_get_unix_fd(*it);
      m_pollFds[m_pollFdCount].events = POLLIN;
      m_pollFds[m_pollFdCount].revents = 0;

      m_pollFdCount++;
      if (m_pollFdCount >= MAX_POLL_FDS)
      {
         fprintf(stderr, "too many DBusWatches\n");
         break;
      }
   }
//    fprintf(stderr, "setupPollFds, %d fds\n", m_pollFdCount);
}


dbus_bool_t GenericDBusHandler::watchAddedCallbackHelper(DBusWatch* watch, void* data)
{
   assert(data!=0);
   GenericDBusHandler* h = (GenericDBusHandler*)data;
   return h->watchAddedCallback(watch);
}


void GenericDBusHandler::watchRemovedCallbackHelper(DBusWatch* watch, void* data)
{
   assert(data!=0);
   GenericDBusHandler* h = (GenericDBusHandler*)data;
   h->watchRemovedCallback(watch);
}


void GenericDBusHandler::watchToggledCallbackHelper(DBusWatch* watch, void* data)
{
   assert(data!=0);
   fprintf(stderr, "toggle %p\n", watch);
   GenericDBusHandler* h = (GenericDBusHandler*)data;
   h->watchToggledCallback(watch);
}


DBusHandlerResult GenericDBusHandler::messageFilterHelper(DBusConnection* connection, DBusMessage* message, void *data)
{
   assert(data!=0);
   GenericDBusHandler* h = (GenericDBusHandler*)data;
   return h->messageFilter(connection, message);
}


dbus_bool_t GenericDBusHandler::watchAddedCallback(DBusWatch* watch)
{
   fprintf(f, "add %p\n", watch);
   m_dbusWatches.push_back(watch);
   setupPollFds();
   return TRUE;
}


void GenericDBusHandler::watchRemovedCallback(DBusWatch* watch)
{
   fprintf(f, "remove %p\n", watch);
   std::vector<DBusWatch*>::iterator it = std::find(m_dbusWatches.begin(), m_dbusWatches.end(), watch);
   if (it!=m_dbusWatches.end())
   {
      m_dbusWatches.erase(it);
   }
   else
   {
      fprintf(stderr, "did not find watch which was removed ! %p\n", watch);
   }
   setupPollFds();
}


void GenericDBusHandler::watchToggledCallback(DBusWatch* watch)
{
   fprintf(stderr, "toggle %p\n", watch);
}


DBusHandlerResult GenericDBusHandler::messageFilter(DBusConnection* connection, DBusMessage* message)
{
   assert(message);
   assert(connection);

   const char* interface = dbus_message_get_interface(message);
   const char* object = dbus_message_get_path(message);
   const char* function = dbus_message_get_member(message);
   fprintf(f, "Executing message filter for message, interface %s, object %s, member %s, from %s\n",
              interface, object, function, dbus_message_get_sender(message));
   if (connection != m_connection)
   {
      fprintf(f, "connection of message != m_connection\n");
      return DBUS_HANDLER_RESULT_HANDLED;
   }

   int messageType = dbus_message_get_type(message);
   switch (messageType)
   {
      case DBUS_MESSAGE_TYPE_METHOD_CALL:
      {
         fprintf(f, "Received message METHOD_CALL\n");
         if(interface && (strcmp(interface, "org.freedesktop.DBus.Introspectable")==0))
         {
            if (object
                && (strcmp(object, "/") == 0)
                && function
                && (strcmp(function, "Introspect") == 0))
            {
               // return the xml introspection data over dbus
               const char* introspectionData = xmlIntrospectionData();
               DBusMessageIter args;
               dbus_message_iter_init(message, &args);
               DBusMessage* reply = dbus_message_new_method_return(message);
               dbus_message_iter_init_append(reply, &args);
               dbus_message_iter_append_basic(&args, DBUS_TYPE_STRING, &introspectionData);
               dbus_connection_send(connection, reply, 0);
               dbus_connection_flush(connection);
               dbus_message_unref(reply);
            }
         }
         else
         {
            handleDBusMessage(message);
         }
         break;
      }
      case DBUS_MESSAGE_TYPE_METHOD_RETURN:
         fprintf(f, "Received message METHOD_RETURN\n");
         break;
      case DBUS_MESSAGE_TYPE_ERROR:
         fprintf(f, "Received message TYPE_ERROR\n");
         break;
      case DBUS_MESSAGE_TYPE_SIGNAL:
         fprintf(f, "Received message TYPE_SIGNAL\n");
         handleDBusSignal(message);
         break;
      default:
         fprintf(stderr, "received message of unexpected type %d, ignoring\n", messageType);
         break;
   }

   return DBUS_HANDLER_RESULT_HANDLED;
}


void GenericDBusHandler::handleDBusWatchEvent(DBusWatch* watch, short events)
{
   assert(watch!=0);

   if (!dbus_watch_handle(watch, DBUS_WATCH_READABLE))
   {
      fprintf(stderr, "dbus_watch_handle() reported low memory !\n");
      assert(0);
   }

   while (dbus_connection_dispatch(m_connection) == DBUS_DISPATCH_DATA_REMAINS)
   {
      // do nothing, just loop
   }
}
