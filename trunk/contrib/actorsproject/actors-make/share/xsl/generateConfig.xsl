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
    <xsl:text>#include &lt;stdlib.h&gt;&#xa;&#xa;</xsl:text>
    <xsl:text>#include "actors-config.h"&#xa;&#xa;</xsl:text>
    <xsl:text>static void initNetwork(AbstractActorInstance ***pInstances, int *pNumberOfInstances) {&#xa;&#xa;</xsl:text>
    <xsl:text>  int numberOfInstances = </xsl:text>
    <xsl:value-of select="count(//Instance)"></xsl:value-of>
    <xsl:text>;&#xa;</xsl:text>
    <xsl:text>  AbstractActorInstance **actorInstances = (AbstractActorInstance **) malloc(numberOfInstances * sizeof(AbstractActorInstance *));&#xa;&#xa;</xsl:text>
    <xsl:text>  *pInstances=actorInstances;&#xa;&#xa;</xsl:text>
    <xsl:text>  *pNumberOfInstances=numberOfInstances;&#xa;&#xa;</xsl:text>
    <xsl:for-each select="//Instance"> 
      <xsl:variable name='className' select="art:getActorType(.)"/> 
      <xsl:variable name='instance' select="concat('p', Note[@kind='UID']/@value)"/> 

      <xsl:text>  extern ActorClass ActorClass_</xsl:text>
      <xsl:value-of select="$className"/>
      <xsl:text>;&#xa;</xsl:text>
      <xsl:text>  AbstractActorInstance *</xsl:text>
      <xsl:value-of select="$instance"/>
      <xsl:text>;&#xa;</xsl:text>
      <xsl:for-each select=".//Port[@kind='Input']">  
        <xsl:text>  InputPort *</xsl:text>
        <xsl:value-of select="concat($instance, '_', @name)"/>
        <xsl:text>;&#xa;</xsl:text>
      </xsl:for-each>
      <xsl:for-each select=".//Port[@kind='Output']">  
        <xsl:text>  OutputPort *</xsl:text>
        <xsl:value-of select="concat($instance, '_', @name)"/>
        <xsl:text>;&#xa;</xsl:text>
      </xsl:for-each>
      <xsl:text>&#xa;</xsl:text>
    </xsl:for-each>
    <xsl:text>&#xa;</xsl:text>

    <xsl:for-each select="//Instance"> 
      <xsl:variable name='className' select="art:getActorType(.)"/> 
      <xsl:variable name='instance' select="concat('p', Note[@kind='UID']/@value)"/> 
 
      <xsl:text>  </xsl:text>
      <xsl:value-of select="$instance"/>
      <xsl:text> = createActorInstance(&amp;ActorClass_</xsl:text>
      <xsl:value-of select="$className"/>
      <xsl:text>);&#xa;</xsl:text>
      
      <xsl:for-each select=".//Port[@kind='Input']">  
        <xsl:variable name="dstId" select="../../@id"/>
        <xsl:variable name="dstPort" select="@name"/> 
        <xsl:variable name="connection" select="//Connection[@dst=$dstId and @dst-port=$dstPort]"/>

        <xsl:text>  </xsl:text>
        <xsl:value-of select="concat($instance, '_', @name)"/>
        <xsl:text> = createInputPort(</xsl:text>  
        <xsl:value-of select="$instance"/>
        <xsl:text>, "</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>", </xsl:text>
        
        <xsl:choose>
          <xsl:when test="$connection/Attribute[@name = 'bufferSize']">
            <xsl:value-of select="$connection/Attribute[@name = 'bufferSize']/Expr/@value"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>0</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text>);&#xa;</xsl:text>
      </xsl:for-each>
      
      <xsl:for-each select=".//Port[@kind='Output']">
        <xsl:variable name="srcId" select="../../@id"/>
        <xsl:variable name="srcPort" select="@name"/> 
        <xsl:variable name="fanOut" select="count(//Connection[@src=$srcId and @src-port=$srcPort])"/>
       
        <xsl:text>  </xsl:text>
        <xsl:value-of select="concat($instance, '_', @name)"/>
        <xsl:text> = createOutputPort(</xsl:text>  
        <xsl:value-of select="$instance"/>
        <xsl:text>, "</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>", </xsl:text>
        <xsl:value-of select="$fanOut"/>
        <xsl:text>);&#xa;</xsl:text>
      </xsl:for-each>
      
      <xsl:for-each select="Parameter">
        <xsl:text>  setParameter(</xsl:text>
        <xsl:value-of select="$instance"/><xsl:text>, "</xsl:text>
        <xsl:value-of select="@name" /> 
        <xsl:text>", "</xsl:text> 
        <xsl:value-of select=".//@value"/>
        <xsl:text>");&#xa;</xsl:text>
      </xsl:for-each>
      <xsl:text>  actorInstances[</xsl:text>
      <xsl:value-of select="position() - 1"/>
      <xsl:text>] = </xsl:text>
      <xsl:value-of select="$instance"/>
      <xsl:text>;&#xa;&#xa;</xsl:text>
    </xsl:for-each>

    <xsl:for-each select="//Connection">
      <xsl:variable name="srcId" select="@src"/> 
      <xsl:variable name="srcPort" select="@src-port"/>     
      <xsl:variable name="dstId" select="@dst"/> 
      <xsl:variable name="dstPort" select="@dst-port"/>
      <xsl:variable name="srcInstance" select="//Instance[@id=$srcId]/Note[@kind='UID']/@value"/>
      <xsl:variable name="dstInstance" select="//Instance[@id=$dstId]/Note[@kind='UID']/@value"/>
      <xsl:text>  connectPorts(p</xsl:text>
      <xsl:value-of select="concat($srcInstance, '_', $srcPort)"/>
      <xsl:text>, p</xsl:text>
      <xsl:value-of select="concat($dstInstance, '_', $dstPort)"/>
      <xsl:text>);&#xa;</xsl:text>     
    </xsl:for-each>
    <xsl:text>}&#xa;&#xa;</xsl:text>
        
    <xsl:text>int main(int argc, char *argv[]) {&#xa;</xsl:text>
    <xsl:text>  int numberOfInstances;&#xa;</xsl:text>  
    <xsl:text>  AbstractActorInstance **instances;&#xa;</xsl:text>
    <xsl:text>  initNetwork(&amp;instances, &amp;numberOfInstances);&#xa;</xsl:text>        
    <xsl:text>  executeNetwork(argc, argv, instances, numberOfInstances);&#xa;</xsl:text>        
    <xsl:text>}&#xa;&#xa;</xsl:text>
  </xsl:template> 
  
</xsl:stylesheet>
