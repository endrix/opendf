<?xml version="1.0" encoding="UTF-8"?>
<!--*
  * Copyright(c)2008, Samuel Keller, Christophe Lucarz, Joseph Thomas-Kerr 
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *     * Redistributions of source code must retain the above copyright
  *       notice, this list of conditions and the following disclaimer.
  *     * Redistributions in binary form must reproduce the above copyright
  *       notice, this list of conditions and the following disclaimer in the
  *       documentation and/or other materials provided with the distribution.
  *     * Neither the name of the EPFL, University of Wollongong nor the
  *       names of its contributors may be used to endorse or promote products
  *       derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY  Samuel Keller, Christophe Lucarz, 
  * Joseph Thomas-Kerr ``AS IS'' AND ANY 
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL  Samuel Keller, Christophe Lucarz, 
  * Joseph Thomas-Kerr BE LIABLE FOR ANY
  * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *-->
<!DOCTYPE stylesheet SYSTEM "entities.dtd">
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:math="http://exslt.org/math"

  xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:bs2="urn:mpeg:mpeg21:2003:01-DIA-BSDL2-NS"

  xmlns:rvc="urn:mpeg:2006:01-RVC-NS" 
  xmlns:saxon="http://saxon.sf.net/" version="2.0">
  <xsl:include href="cal.xslt"/>
  <xsl:include href="functions.xslt"/>
  <xsl:include href="globals.xslt"/>

  <xsl:include href="length.xslt"/>

  <xsl:include href="actions.xslt"/>
  <xsl:include href="fsm.xslt"/>
  <xsl:include href="priorities.xslt"/>
  <xsl:variable name="preprocessedDocument" select="saxon:transform(
    saxon:compile-stylesheet(doc('preprocess.xslt')),/.)"/>
  
  <xsl:template match="/">
    <xsl:apply-templates select="$preprocessedDocument/*"/>
  </xsl:template>
  
  <xsl:variable name="targetNamespace" select="/xsd:schema/@targetNamespace"/>
  <xsl:variable name="targetPrefix" select="rvc:findPrefix($targetNamespace,/*,/)"/>

  <!-- imported nodes have had their name attribute augmented with a prefix -->

  <xsl:key name="types" use="resolve-QName(@name,.)"

    match="xsd:schema/xsd:complexType | xsd:schema/xsd:simpleType"/>



  <xsl:key name="globalElements" use="resolve-QName(@name,.)" 
    match="xsd:schema/xsd:element"/>



  <xsl:key name="groups" use="resolve-QName(@name,.)" match="xsd:schema/xsd:group"/>



  <xsl:template match="xsd:schema">

    <!-- resolve the root element -->

    <xsl:variable name="rootElementQName" select="resolve-QName(@bs2:rootElement,.)"/>

    <xsl:variable name="rootElement"

      select="xsd:element[QName($targetNamespace, @name)=$rootElementQName]"/>

    <xsl:variable name="rootElementType"

      select="if ($rootElement/@type) then 
      rvc:localName($rootElement/@type) else 
      concat(rvc:localName($rootElement/@name),'Type')"/>


<!--    <xsl:variable name="readList">
      <xsl:apply-templates select="$rootElement" mode="linearize"/>
      <read name="&finalState;" again="true" testRequired="true"/>
    </xsl:variable> -->
    

    <xsl:call-template name="actor">
      <xsl:with-param name="name">BSDLParser</xsl:with-param>

      <xsl:with-param name="inputs">
        <xsl:call-template name="port">
          <xsl:with-param name="kind">Input</xsl:with-param>
          <xsl:with-param name="type">bool</xsl:with-param>
          <xsl:with-param name="name">bitstream</xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
      <xsl:with-param name="outputs">
        <xsl:apply-templates select="xsd:annotation/xsd:appinfo/rvc:output" mode="outputPorts"/>
      </xsl:with-param>
      <xsl:with-param name="imports">
        <xsl:call-template name="import"><!-- import the bitOps package -->
          <xsl:with-param name="name" select="('caltrop','lib','BitOps')"/>
          <xsl:with-param name="kind" select="'package'"/>
        </xsl:call-template>
        <xsl:call-template name="import">        <!-- import java.lang.Object -->
          <xsl:with-param name="name" select="('java','lang','Object')"/>
        </xsl:call-template>
       <!-- <xsl:call-template name="import">  
             <xsl:with-param name="name" select="('org','iso','mpeg','rvc','objects')"/>
             <xsl:with-param name="kind" select="'package'"/>
          </xsl:call-template>
        <xsl:call-template name="import">  
          <xsl:with-param name="name" select="('org','iso','mpeg','rvc')"/>
          <xsl:with-param name="kind" select="'package'"/>
        </xsl:call-template>-->
        <xsl:call-template name="import"><!--  import java.lang.system.out -->
          <xsl:with-param name="name" select="('java','lang','System','out')"/>
          <xsl:with-param name="kind" select="'package'"/>
        </xsl:call-template>
      </xsl:with-param>
      <xsl:with-param name="children">

        <!-- declare the bit-reading action, functions etc. -->

        <xsl:call-template name="bitAction"/>

        <!-- declare the current element list -->   <!--modified by Christophe Lucarz, 26.09.07 for the scope of the meeting demo-->

        <!--<xsl:call-template name="list">

          <xsl:with-param name="name">&current;</xsl:with-param>

          <xsl:with-param name="size" select="xsd:integer(max($readList//@depth))"/>
          <xsl:with-param name="type">&object;</xsl:with-param>
          </xsl:call-template>-->



        <!-- create length constants -->

        <xsl:call-template name="lengthConstants"/>

        <!--  create the global variables/constants-->
        <xsl:apply-templates select="$rootElement" mode="globals">
          <xsl:with-param name="defaultPrefix" select="$targetPrefix" tunnel="yes"/>
        </xsl:apply-templates>
        
        <!-- create the actions -->
        <!--<xsl:apply-templates mode="buildActions" select="$readList">
          <xsl:with-param name="defaultPrefix" select="$targetPrefix" tunnel="yes"/>
          </xsl:apply-templates>-->
        <xsl:apply-templates mode="actions" select="*"/>
        

        <!-- create the FSM -->

        <xsl:call-template name="fsm">

          <!--<xsl:with-param name="initialState" select="concat($readList/read[1]/@name,'&existsStateSuffix;')"/>--><!-- assuming the root exists! -->
          <xsl:with-param name="initialState">
            <xsl:apply-templates mode="firstname" select="$rootElement" />
          </xsl:with-param>

          <xsl:with-param name="transitions">
            <xsl:apply-templates mode="fsm" select="$rootElement"/>
          </xsl:with-param>

        </xsl:call-template>


        <xsl:apply-templates mode="priorities" select="*"/>
        <!-- create the priorities -->
        <!--<xsl:call-template name="priorities">
          <xsl:with-param name="priorities">
            <xsl:apply-templates mode="priorities" select="*"/>
          </xsl:with-param>
        </xsl:call-template>-->
      </xsl:with-param>

    </xsl:call-template>

  </xsl:template>


  <xsl:template match="rvc:output" mode="outputPorts">
    <xsl:call-template name="port">
      <xsl:with-param name="name" select="@name"/>
    </xsl:call-template>    
  </xsl:template>
</xsl:stylesheet>