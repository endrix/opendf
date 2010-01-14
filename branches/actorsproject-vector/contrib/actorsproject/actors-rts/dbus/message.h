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


#ifndef MESSAGE_H
#define MESSAGE_H

#include "FastDelegate.h"


#define MSG_EXIT_THREAD          (1)
#define MSG_SET_TIMEOUT_INTERVAL (2)

#define MSG_CUSTOM_ID_START         (1000)

class Message;

// fastdelegates are used here for implementing message handlers: http://www.codeproject.com/KB/cpp/FastDelegate.aspx
typedef fastdelegate::FastDelegate1<const Message*> MessageDelegate;

/** Define the type for message handlers, which are plain functions pointers, i.e. no class members.
    The argument must be always be of type Message* or a class drived from it. */
typedef void (*MessageCallback)(const Message*);

// This is necessary to make it possible that member function of any classes can be message handlers. See the macro below.
class GenericMessageHandler;

/** Use this macro to register a message handler which is a member function of any class.
    The argument must always be const Message* or a class derived from it */
#define MAKE_MESSAGE_DELEGATE(object, member) fastdelegate::MakeDelegate(object,                                       \
                                                   reinterpret_cast<void (GenericMessageHandler::*)(const Message*)>(member))



/** This is the base class for all messages which threads use to communicate with each other.
 * If you need a message which carries some data, you should subclass it and add your required data. */
class Message
{
   public:
      /** Create a new Message object with the given \a id. The \a id is used to determine the type
       * of the message at the receiver. */
      Message(int id):m_id(id) {}

      /** Return the id of this message. */
      int getId() const {return m_id;}
   protected:
      int m_id;
   private:
      Message();
};


/** This is a simple message, which additionally to the message id can also carry an arbitrary integer value; */
class IntMessage : public Message
{
   public:
      /** Create a new message object of the type IntMessage. The message is initialized using
       the given id and the the int value of this message is set to \a value . */
      IntMessage(int id, int value):Message(id), m_value(value)                                {}

      /** Return the integer value of this message. */
      int getValue() const                                                                     {return m_value;}
   protected:
      int m_value;
   private:
      IntMessage();
};

#endif

