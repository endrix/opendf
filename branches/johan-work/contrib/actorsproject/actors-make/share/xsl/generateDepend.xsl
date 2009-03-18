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
  
    <xsl:template match="XDF">
      <xsl:value-of select="@name"/>
      <xsl:text>.xdf :</xsl:text>               
      <xsl:value-of select="substring-after(base-uri(.), 'file:')"/>
      <xsl:text>&#xa; &#xa;</xsl:text>
      
      <xsl:text>XLIM_FILES=</xsl:text>
      <xsl:for-each select="//Instance/Note[@kind='UID']">
        <xsl:if test="not(contains(./@value,'_art'))">
          <xsl:value-of select="@value"/>
          <xsl:text>.xlim </xsl:text>  
        </xsl:if>                   
      </xsl:for-each>
      <xsl:text>&#xa; &#xa;</xsl:text> 
      <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="Instance">
      <xsl:variable name='UID' select="./Note[@kind='UID']/@value"/>   
      <xsl:value-of select="$UID"/> 
      <xsl:text>.xlim : </xsl:text>
      <xsl:value-of select="Actor/Note[@kind='filepath']/@value"/> 
      <xsl:text> </xsl:text>      
      <xsl:value-of select="$UID"/> 
      <xsl:text>.par </xsl:text>      
      <xsl:text>&#xa;</xsl:text>
    </xsl:template>
  
</xsl:stylesheet>
