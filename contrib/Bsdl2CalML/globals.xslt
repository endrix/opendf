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

<xsl:stylesheet   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  
  xmlns:bs2x="urn:mpeg:mpeg21:2003:01-DIA-BSDL2x-NS"

  xmlns:math="http://exslt.org/math"

  xmlns:xsd="http://www.w3.org/2001/XMLSchema" 

  xmlns:rvc="urn:mpeg:2006:01-RVC-NS"

  xmlns:bs0="urn:mpeg:mpeg21:2003:01-DIA-BSDL0-NS" 

  xmlns:bs2="urn:mpeg:mpeg21:2003:01-DIA-BSDL2-NS" 

  version="2.0">
  
  <xsl:template match="*[@bs0:variable='true']" mode="globals" priority="5">
    <xsl:param name="defaultPrefix" tunnel="yes"/>
    
    <xsl:call-template name="statement">  
      <xsl:with-param name="expressions">
        <xsl:call-template name="variableDeclaration">
          <!--<xsl:with-param name="name" select="concat('$',rvc:simpleValidPrefixedName(@name,$defaultPrefix))"/>-->
          <xsl:with-param name="name" select="@name"/>
          <xsl:with-param name="type">int</xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
    
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@bs2:nOccurs]" mode="globals" priority="5">
    <xsl:call-template name="statement">
      <xsl:with-param name="expressions">
        <xsl:call-template name="variableDeclaration">
          <xsl:with-param name="name">
            <xsl:value-of select="@name"/>
            <xsl:text>&countSuffix;</xsl:text>
          </xsl:with-param>
          <xsl:with-param name="initialValue" select="0"/>
          <xsl:with-param name="type">int</xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:next-match/>
  </xsl:template>
  
  <xsl:template match="bs2x:variable" mode="globals" priority="5">
    <xsl:call-template name="statement">
      <xsl:with-param name="expressions">
        <xsl:call-template name="variableDeclaration">
          <xsl:with-param name="name">
            <xsl:value-of select="@name"/>
          </xsl:with-param>
          <xsl:with-param name="type">int</xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*" priority="0" mode="globals">
    <xsl:apply-templates mode="globals" select="*"/>
  </xsl:template>

</xsl:stylesheet>

