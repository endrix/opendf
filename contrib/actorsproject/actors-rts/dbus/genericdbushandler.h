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


#ifndef GENERICDBUSHANDLER_H
#define GENERICDBUSHANDLER_H

#include <poll.h>

#include <map>
#include <vector>
#include <queue>
#include <string>

#include <dbus/dbus.h>

#include "message.h"

#define MAX_POLL_FDS 32


/** The class GenericDBusHandler implements the generic functionality for communicating
 * over DBus in C++. It connects to the DBus bus, sets up handlers for DBus messages etc. */
class GenericDBusHandler
{
   public:
      /** Create a GenericDBusHandler object and initialize everything. It doesn't connect yet to a bus. */
      GenericDBusHandler();

      virtual ~GenericDBusHandler();

      virtual const char* interfaceName() const {return 0;}
      virtual const char* xmlIntrospectionData() const {return 0;}

      /** Connects to the DBus bus and sets up the DBus handler functions. Returns 0 on success.*/
      int init();

      /** Send the message \a msg to this thread.
      The message \a msg must have been allocated using new. It will be deleted automatically after delivery. */
      void postMessage(const Message* msg);

      /** Suspends the thread after it has finished processing the current message.
      Does nothing if it is already suspended. */
      void suspendThread();

      /** Resumes the thread in case it is suspended right now, does nothing otherwise. */
      void resumeThread();

      /** This function has to be called from another thread, it waits for the termination of the thread
      which runs the DBus mainloop inside GenericDBusHandler. */
      int joinThread();

      /** Asks this thread to terminate. Should be called before joinThread() .*/
      void exitThread();

      /** Add a member function of any object as message handler for messages with the given \a messageId .
      Use the macro MAKE_MESSAGE_DELEGATE defined above to create the MessageDelegate ·*/
      void addMessageHandler(int messageId, MessageDelegate memberFunctionCallback);

   protected:
      DBusConnection* m_connection;
      std::string m_dbusName;
      virtual void handleDBusMessage(DBusMessage* message) = 0;
      virtual void handleDBusSignal(DBusMessage* message) = 0;
   private:
      pthread_mutex_t m_notificationMutex;
      std::queue<const Message*> m_messageQueue;

      std::map<int, MessageDelegate> m_messageHandlers;
      int m_notificationPipe[2];
      int m_notificationPipeIndex;
      bool m_exitMainloop;
      bool m_suspended;
      bool m_dbusEventOccured;

      struct pollfd m_pollFds[MAX_POLL_FDS];  // should be enough for 1 for the pipe + 2 for the dbus connection
      unsigned int m_pollFdCount;

      pthread_t m_mainloopThread;

      /** This is the DBus mainloop. Here DBus messages are received and also sent. It runs in
      a separate pthread (started via mainloopHelper() ). */
      void mainloop();

      /** Static helper function to start GenericDBusHandler::mainloop() in a thread. */
      static void* mainloopHelper(void* pointerToThis);

      /** Sets up m_pollFds as necessary for reacting on DBus messages and the internal notification pipe. */
      void setupPollFds();

      /** Setup a new connection so it is ready to use, i.e. add watches and filters etc. */
      int setupConnection(DBusConnection* connection);

      /** Closes the notification pipe to the parent thread. */
      void closeNotificationPipe();

      /** Handle the internal Message \a msg and call the respective message handler. */
      bool handleMessage(const Message* msg);

      /** Message handler for the MSG_EXIT_THREAD message. */
      void handleExitThread(const Message*);

      // dbus related stuff
      std::vector<DBusWatch*> m_dbusWatches;

      // the following 4 static functions are just helper functions. They serve as callback and
      // their only purpose is to "forward" the callback into the class, i.e. into a non-static member function
      /** Static helper function used as callback for the watch added callback, forwards to watchAddedCallback() .*/
      static dbus_bool_t watchAddedCallbackHelper(DBusWatch* watch, void* data);

      /** Static helper function used as callback for the watch removed callback, forwards to watchRemovedCallback() .*/
      static void watchRemovedCallbackHelper(DBusWatch* watch, void* data);

      /** Static helper function used as callback for the watch toggled callback, forwards to watchToggledCallback() .*/
      static void watchToggledCallbackHelper(DBusWatch* watch, void* data);

      /** Static helper function used as as message filter callback, forwards to messageFilter() .*/
      static DBusHandlerResult messageFilterHelper(DBusConnection* connection, DBusMessage* message, void *data);

      // These are the 4 callbacks the functions above are forwarding to.
      /** This is called via watchAddedCallbackHelper() when a DBus watch is added. */
      dbus_bool_t watchAddedCallback(DBusWatch* watch);

      /** This is called via watchRemovedCallbackHelper() when a DBus watch is removed. */
      void watchRemovedCallback(DBusWatch* watch);

      /** This is called via watchToggledCallbackHelper() when a DBus watch is toggled.
          I don't know when this happens and what this means.*/
      void watchToggledCallback(DBusWatch* watch);

      /** Process a received DBus message and call the respective handler function. */
      DBusHandlerResult messageFilter(DBusConnection* connection, DBusMessage* message);

      /** Handle a DBus watch event, i.e. dispatch the received DBus message to the respective watch handler. */
      void handleDBusWatchEvent(DBusWatch* watch, short events);

#ifdef USE_DBUS_PEER_TO_PEER
      // server stuff
      int initServer();
      static void newConnectionCallbackHelper(DBusServer* server, DBusConnection* connection, void* data);
      void newConnectionCallback(DBusServer* server, DBusConnection* connection);

      DBusServer* m_server;
      std::vector<DBusConnection*> m_connections;
#endif

};

#endif
