<?xml version="1.0" ?>
<!-- Basic metastylesheet for the Schematron XML Schema Language.
	http://www.ascc.net/xml/resource/schematron/schematron.html

 Copyright (c) 2000,2001 Rick Jelliffe and Academia Sinica Computing Center, Taiwan

 This software is provided 'as-is', without any express or implied warranty. 
 In no event will the authors be held liable for any damages arising from 
 the use of this software.

 Permission is granted to anyone to use this software for any purpose, 
 including commercial applications, and to alter it and redistribute it freely,
 subject to the following restrictions:

 1. The origin of this software must not be misrepresented; you must not claim
 that you wrote the original software. If you use this software in a product, 
 an acknowledgment in the product documentation would be appreciated but is 
 not required.

 2. Altered source versions must be plainly marked as such, and must not be 
 misrepresented as being the original software.

 3. This notice may not be removed or altered from any source distribution.
-->

<!-- Schematron basic -->

<xsl:stylesheet
   version="2.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:axsl="http://www.w3.org/1999/XSL/TransformAlias"
   xmlns:cal="java:net.sf.caltrop.xslt.cal.CalmlEvaluator" >

<xsl:import href="skeleton1-5.xsl"/>

<xsl:namespace-alias stylesheet-prefix="axsl" result-prefix="xsl"/>
 
<xsl:template name="process-prolog">
  <axsl:output method="xml" indent="yes"/>
  <axsl:include href="net/sf/caltrop/cal/checks/reportOffenders.xslt"/>
</xsl:template>

<xsl:template name="process-root">
  <xsl:param name="contents"/>
  <xsl:param name="fpi"/>
  <xsl:param name="title"/>
  <xsl:param name="id"/>
  <xsl:param name="icon"/>
  <xsl:param name="lang"/>
  <xsl:param name="version"/>
  <xsl:param name="schemaVersion"/>

  <axsl:variable name="contents">
    <xsl:copy-of select="$contents"/>
  </axsl:variable>
  
  <xsl:variable name="arg">{name()}</xsl:variable>
  
  <!-- Copy the source doc, inserting the messages in the root element -->
  <axsl:for-each select="/*">
    <axsl:copy>
      <axsl:for-each select="@*">
        <axsl:attribute name="{$arg}"><axsl:value-of select="."/></axsl:attribute>
      </axsl:for-each>
      <axsl:copy-of select="$contents"/>
      <axsl:copy-of select="*"/>
    </axsl:copy>
  </axsl:for-each>
  <!-- 
  <axsl:for-each select="$contents/Note">
    <axsl:variable name="body">
      <axsl:value-of select="text()"/>
    </axsl:variable>
    <axsl:message>
      <axsl:value-of select="@severity"/><axsl:text>: </axsl:text>
      <axsl:value-of select="normalize-space($body)"/><axsl:text> [id </axsl:text>
      <axsl:value-of select="@id"/><axsl:text>]</axsl:text>
    </axsl:message>
    <axsl:message>
      <axsl:text>    </axsl:text>
      <axsl:value-of select="@subject"/>
    </axsl:message>
    <axsl:message/>
  </axsl:for-each>
  <axsl:if test="$contents/Note[@severity='Warning']">
    <axsl:message>
      <axsl:value-of select="count( $contents/Note[@severity='Warning'] )"/>
      <axsl:text> warning</axsl:text>
      <axsl:if test="count( $contents/Note[@severity='Warning'] ) &gt; 1">
        <axsl:text>s</axsl:text>
      </axsl:if>
    </axsl:message>
  </axsl:if>
  <axsl:if test="$contents/Note[@severity='Error']">
    <axsl:message terminate="yes">
      <axsl:value-of select="count( $contents/Note[@severity='Error'] )"/>
      <axsl:text> error</axsl:text>
      <axsl:if test="count( $contents/Note[@severity='Error'] ) &gt; 1">
        <axsl:text>s</axsl:text>
      </axsl:if>
      <axsl:text> [processing terminated]&#xA;</axsl:text>
    </axsl:message>
  </axsl:if>
  -->
</xsl:template>

<xsl:template name="process-assert">
  <xsl:param name="role"/>
  <xsl:param name="test"/>
  <xsl:param name="id"/>
  <xsl:param name="icon"/>
  <xsl:param name="diagnostics"/>
  <xsl:param name="subject"/>
  <xsl:variable name="id-list">
    <xsl:for-each select="ancestor-or-self::*[@id]">
      <xsl:value-of select="@id"/>
      <xsl:if test="not( position()=last() )">.</xsl:if>
    </xsl:for-each>
  </xsl:variable>
  <Note kind="Report" severity="{$role}" id="{translate($id-list,' ', '')}" subject="{$subject}">
      <xsl:call-template name="process-message">
         <xsl:with-param name="pattern" select="$test"/>
         <xsl:with-param name="role" select="$role"/>
      </xsl:call-template>
  </Note>
</xsl:template>

<xsl:template name="process-report">
  <xsl:param name="role"/>
  <xsl:param name="test"/>
  <xsl:param name="id"/>
  <xsl:param name="icon"/>
  <xsl:param name="diagnostics"/>
  <xsl:param name="subject"/>
  <xsl:variable name="id-list">
    <xsl:for-each select="ancestor-or-self::*[@id]">
      <xsl:value-of select="@id"/>
      <xsl:if test="not( position()=last() )">.</xsl:if>
    </xsl:for-each>
  </xsl:variable>
  <Note kind="Report" severity="{$role}" id="{translate($id-list,' ', '')}" subject="{$subject}">
    <xsl:call-template name="process-message">
      <xsl:with-param name="pattern" select="$test"/>
      <xsl:with-param name="role" select="$role"/>
    </xsl:call-template>
  </Note>
</xsl:template>
    
</xsl:stylesheet>
