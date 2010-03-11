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


#ifndef CUSTOMMESSAGES_H
#define CUSTOMMESSAGES_H

#include "message.h"
//#include "client.h"
//#include "resourcemanagerinterface.h"

#define MSG_REGISTER_CLIENT         (1001)
#define MSG_REPORT_HAPPINESS        (1002)
#define MSG_CPU_CATEGORY            (1003)
#define MSG_QUALITY_LEVELS          (1004)
#define MSG_ADDTO_GROUP             (1008)
//#define MSG_CLIENT_HAPPINESS        (1002)
//#define MSG_CLIENT_CPU_CATEGORY     (1003)
//#define MSG_CLIENT_QUALITY_LEVELS   (1004)
//#define MSG_CLIENT_QUALITY_FUNCTION (1005)
//#define MSG_IMPORTANCES             (1006)
//#define MSG_UNREGISTER_CLIENT       (1007)



/** This message is sent from the dbus handler to the resourcemanager whenever a client application connects
to the dbus handler. */
/*class RegisterClientMessage : public Message
{
   public:
      RegisterClientMessage(const char* name) : Message(MSG_REGISTER_CLIENT), m_name(name)     {}
      const char* getName() const                                                              {return m_name.c_str();}
   private:
      std::string m_name;
      RegisterClientMessage();
};*/


#if 0

/** This message is sent from the dbus handler to the resourcemanager whenever a client application reports
a new happiness value. */

class ClientHappinessMessage : public Message
{
   public:
      ClientHappinessMessage(const char* clientId, int happiness) : Message(MSG_CLIENT_HAPPINESS)
                                                                  , m_clientId(clientId)
                                                                  , m_happiness(happiness)   {}
      const std::string& getClientId() const                                                   {return m_clientId;}
      int getHappiness() const                                                                 {return m_happiness;}
   protected:
      std::string m_clientId;
      int m_happiness;
};


/** This message is sent from the dbus handler to the resourcemanager whenever a client application
announces its overall CPU deman category. */
class ClientCategoryMessage : public Message
{
   public:
      ClientCategoryMessage(const char* clientId, ResourceManagerInterface::Category category)
            : Message(MSG_CLIENT_CPU_CATEGORY)
            , m_clientId(clientId)
            , m_category(category)                                                             {}

      const std::string& getClientId() const                                                   {return m_clientId;}
      ResourceManagerInterface::Category getCategory() const                                   {return m_category;}
   protected:
      std::string m_clientId;
      ResourceManagerInterface::Category m_category;
};


/** This message is sent from the dbus handler to the resourcemanager whenever a client application
announces its quality levels. */
class ClientQualityLevelsMessage : public Message
{
   public:
      ClientQualityLevelsMessage(const char* clientId,
                                 const std::vector<ResourceManagerInterface::QualityLevel>& levels)
            : Message(MSG_CLIENT_QUALITY_LEVELS)
            , m_clientId(clientId)
            , m_qualityLevels(levels)                                                          {}

      const std::string& getClientId() const                                                   {return m_clientId;}
      const std::vector<ResourceManagerInterface::QualityLevel>& getQualityLevels() const      {return m_qualityLevels;}
   protected:
      std::string m_clientId;
      std::vector<ResourceManagerInterface::QualityLevel> m_qualityLevels;
};


/** This message is sent from the dbus handler to the resourcemanager whenever a client application
announces its continuous quality function. */
class ClientQualityFunctionMessage : public Message
{
   public:
      ClientQualityFunctionMessage(const char* clientId,
                                   ResourceManagerInterface::FunctionType type,
                                   double param0, double param1, double param2, double param3, double param4)
            : Message(MSG_CLIENT_QUALITY_FUNCTION)
            , m_clientId(clientId)
            , m_functionType(type)
            {m_params[0]=param0; m_params[1]=param1;  m_params[2]=param2;  m_params[3]=param3;  m_params[4]=param4;}

      const std::string& getClientId() const                                                   {return m_clientId;}
      ResourceManagerInterface::FunctionType getFunctionType() const                           {return m_functionType;}
      const double* getParams() const                                                          {return m_params;}
   protected:
      std::string m_clientId;
      ResourceManagerInterface::FunctionType m_functionType;
      double m_params[5];
};


/** This message is sent from the dbus handler to the resourcemanager whenever a client application unregisters. */
class UnregisterClientMessage : public Message
{
   public:
      UnregisterClientMessage(const char* clientId)
            : Message(MSG_UNREGISTER_CLIENT)
            , m_clientId(clientId)
            {}

            const std::string& getClientId() const                                                   {return m_clientId;}
   protected:
      std::string m_clientId;
};


/** This message is sent from the application object to the resourcemanager when it has read
 the importances config file. */
class ImportancesMessage : public Message
{
   public:
      ImportancesMessage() : Message(MSG_IMPORTANCES)                                          {}

      std::map<std::string, unsigned int>& importances()                                       {return m_importances;}
      const std::map<std::string, unsigned int>& importances() const                           {return m_importances;}
   protected:
      std::map<std::string, unsigned int> m_importances;
};


#endif

#endif

