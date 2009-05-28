<?xml version="1.0" encoding="UTF-8"?>

<!--
  Copyright (c) Ericsson AB, 2009
  Author: Johan Eker (johan.eker@ericsson.com)
  All rights reserved.

  License terms:

  Redistribution and use in source and binary forms, 
  with or without modification, are permitted provided 
  that the following conditions are met:
      * Redistributions of source code must retain the above 
        copyright notice, this list of conditions and the 
        following disclaimer.
      * Redistributions in binary form must reproduce the 
        above copyright notice, this list of conditions and 
        the following disclaimer in the documentation and/or 
        other materials provided with the distribution.
      * Neither the name of the copyright holder nor the names 
        of its contributors may be used to endorse or promote 
        products derived from this software without specific 
        prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
-->

<xsl:stylesheet 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   version="2.0"
   xmlns:art="http://whatever"> 
  <xsl:output method="text" indent="yes"/>
  <xsl:strip-space elements="*" />
  
  <xsl:function name="art:getActorType">
    
    <xsl:param name="instance"/>      
    <xsl:variable name="UID" select="$instance/Note[@kind='UID']/@value"/>            
    <xsl:choose>
      <xsl:when test="contains($UID, 'art_')">
        <xsl:value-of select="$instance/Note[@kind='className']/@value"/>
      </xsl:when>
      <xsl:otherwise> 
        <xsl:value-of select="$UID"/> 
      </xsl:otherwise>
    </xsl:choose> 
  </xsl:function>
  
  <xsl:template match="/">  
    <xsl:text>#include "actors-rts.h"&#xa;&#xa;</xsl:text>

    <!--    
            1. create a enum of Fifos
            enum Fifos{
            Actor_port_0,
            Actor_port_1,
            ..........
            numberOfFifos
            };
      -->     
    
    <xsl:text>enum Fifos{</xsl:text>
    <xsl:text>&#xa;</xsl:text>
    <xsl:for-each select="//Port[@kind='Output']">
      <xsl:text>  </xsl:text>         
      <xsl:value-of select="./../../Note[@kind='UID']/@value"/>        
      <xsl:text>_</xsl:text>
      <xsl:value-of select="./@name"/>
      <xsl:text>,&#xa;</xsl:text>                    
    </xsl:for-each>
    <xsl:text>  numberOfFifos&#xa;</xsl:text>                          
    <xsl:text>};</xsl:text>
    <xsl:text>&#xa;&#xa;</xsl:text>
    
    <!--
      create an array containing the fifo sizes   
    -->
        
    <xsl:text>int FifoSizes[] = {</xsl:text>
    <xsl:text>&#xa;</xsl:text>
    <xsl:for-each select="//Port[@kind='Output']">
      <xsl:variable name="src_id" select="../../@id"/>
      <xsl:variable name="port_name" select="@name"/>
      <xsl:variable name="sz" select="//Connection[@src=$src_id]/Attribute[@name='bufferSize']"/>  
      <xsl:text>  </xsl:text> 
      <xsl:choose>
        <xsl:when test="$sz">
           <xsl:value-of select="$sz[1]/Expr/@value"/> 
        </xsl:when>
        <xsl:otherwise>0</xsl:otherwise>
      </xsl:choose>
      <xsl:text>,&#xa;</xsl:text> 
    </xsl:for-each>
    <xsl:text>};</xsl:text>
    <xsl:text>&#xa;&#xa;</xsl:text>    
    
    <!--
        2. define a new network structure:
        typedef struct {
        ActorClass actorClass;
        enum Fifos *inputPorts;
        enum Fifos *outputPorts;
        }NetworkActor
	
        3. generate input ports and output ports for each actor:
        static int  ActorClass_Add_In[]={Actor_port_0,.........};
        static int  ActorClass_Add_Out[]={Actor_port_1,........};
      -->
    
    <xsl:for-each select="//Instance">
      <xsl:variable name='id' select="@id"/> 
      <xsl:variable name='UID' select="./Note[@kind='UID']/@value"/>   
      
      <xsl:text>static int ActorClass_</xsl:text>
      <xsl:value-of select="$UID"/>
      <xsl:text>_Out[]={</xsl:text>  
      <xsl:for-each select="Actor/Port[@kind='Output']">
        <xsl:value-of select="$UID"/>
        <xsl:text>_</xsl:text> 
        <xsl:value-of select="@name"/>  
        <xsl:if test="not(position() = last())">
          <xsl:text>, </xsl:text>             
        </xsl:if>          
      </xsl:for-each>
      <xsl:text>};&#xa;</xsl:text>
      
      <xsl:text>static int ActorClass_</xsl:text>
      <xsl:value-of select="$UID"/>
      <xsl:text>_In[]={</xsl:text>  
      <xsl:for-each select="Actor/Port[@kind='Input']">
        <xsl:variable name="portName" select="@name"/>  
        <xsl:variable name="connection" select="//Connection[@dst = $id][@dst-port = $portName]"/> 
        <xsl:variable name="src-instance" select="//Instance[@id=$connection/@src]"/>
        <xsl:value-of select="$src-instance/Note[@kind='UID']/@value"/> 
        <xsl:text>_</xsl:text> 
        <xsl:value-of select="$connection/@src-port"/>  
        <xsl:if test="not(position() = last())">
          <xsl:text>, </xsl:text>             
        </xsl:if>
      </xsl:for-each>
      <xsl:text>};&#xa;</xsl:text>
      
      <xsl:if test="contains($UID, 'art_')">          
        <xsl:text>static ActorParameter params_</xsl:text>
        <xsl:value-of select="$UID"/>
        <xsl:text>[]={</xsl:text>
        <xsl:for-each select="Parameter">
          <xsl:text>{"</xsl:text>
          <xsl:value-of select="@name" />
          <xsl:text>", "</xsl:text>  
          <xsl:value-of select=".//@value" />
          <xsl:text>"}</xsl:text>  
          <xsl:if test="not(position() = last())">
            <xsl:text>, </xsl:text>             
          </xsl:if>
        </xsl:for-each>
        <xsl:text>};&#xa;&#xa;</xsl:text>
      </xsl:if>          
      
    </xsl:for-each>            
    <xsl:text>&#xa;&#xa;</xsl:text>
    
    <!--
        generate extern declarations for each actor class 
      -->            
    
    <xsl:for-each select="//Instance">                  
      <xsl:text>extern ActorClass ActorClass_</xsl:text>
      <xsl:value-of select="art:getActorType(.)"/>
      <xsl:text>;&#xa;</xsl:text>
    </xsl:for-each>     
    <xsl:text>&#xa;</xsl:text>
    
    <!--
        4 generate network structure data for each actor:
        ActorConfig actorConfig_Add_0= {
        ActorClass_Add_0,
        ActorClass_Add_In,
        ActorClass_Add_Out
        }
      -->      

    <xsl:for-each select="//Instance">       
      <xsl:variable name='TypeName' select="art:getActorType(.)"/>   
      
      <xsl:text>static ActorConfig actorConfig_</xsl:text>
      <xsl:value-of select="./Note[@kind='UID']/@value"/>
      <xsl:text>={&#xa;</xsl:text>
      <xsl:text>   &amp;ActorClass_</xsl:text>
      <xsl:value-of select="$TypeName"/>
      <xsl:text>,&#xa;</xsl:text> 
      <xsl:text>   ActorClass_</xsl:text>
      <xsl:value-of select="./Note[@kind='UID']/@value"/>
      <xsl:text>_In,&#xa;</xsl:text> 
      <xsl:text>   ActorClass_</xsl:text>
      <xsl:value-of select="./Note[@kind='UID']/@value"/>
      <xsl:text>_Out,&#xa;</xsl:text>
      
      <xsl:choose>
        <xsl:when test="contains(./Note[@kind='UID']/@value, 'art_')">
          <xsl:text>   </xsl:text>
          <xsl:value-of select="count(./Parameter)"/>
          <xsl:text>,&#xa;</xsl:text>  
          <xsl:text>   params_</xsl:text>
          <xsl:value-of select="./Note[@kind='UID']/@value"/>
          <xsl:text>&#xa;</xsl:text>                          
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>   0,&#xa;</xsl:text>  
          <xsl:text>   0&#xa;</xsl:text>                          
        </xsl:otherwise>            
        
      </xsl:choose> 
      
      <xsl:text>};&#xa;&#xa;</xsl:text>
    </xsl:for-each>     
    <xsl:text>&#xa;</xsl:text>
    
    <!--    
            5. generate a array of networks actors:
            NetworkConfig *networkConfig []={
            &networkActor_Add_0,
            ..........
            };
      -->
    
    <xsl:text>static ActorConfig *actors []={&#xa;</xsl:text>      
    <xsl:for-each select="//Instance">
      <xsl:text>   &amp;actorConfig_</xsl:text>
      <xsl:value-of select="./Note[@kind='UID']/@value"/>
      <xsl:if test="not(position() = last())">
        <xsl:text>,</xsl:text>             
      </xsl:if>  
      <xsl:text> &#xa;</xsl:text>
    </xsl:for-each>     
    <xsl:text>};&#xa;&#xa;</xsl:text>
    
    <xsl:text>static NetworkConfig networkConfig = {&#xa;</xsl:text>
    <xsl:text>  actors,&#xa;</xsl:text>        
    <xsl:text>   sizeof(actors)/sizeof(ActorConfig*),&#xa;</xsl:text>
    <xsl:text>  numberOfFifos&#xa;</xsl:text>        
    <xsl:text></xsl:text>
    <xsl:text>};&#xa;&#xa;</xsl:text>
    
    <xsl:text>int main(int argc, char *argv[]) {&#xa;</xsl:text>
    <xsl:text>  return execute_network(argc, argv, &amp;networkConfig);&#xa;</xsl:text>        
    <xsl:text>};&#xa;&#xa;</xsl:text>
    
  </xsl:template> 
  
</xsl:stylesheet>
