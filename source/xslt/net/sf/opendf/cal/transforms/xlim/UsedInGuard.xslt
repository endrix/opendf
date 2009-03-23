<!--
	UsedInGuard.xslt
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the name of the copyright holder nor the names 
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
	
ENDCOPYRIGHT

-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  extension-element-prefixes="xsl xd"
  version="1.1">
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Identify all local variables used in guard expressions</xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on VariableUsage</li>
      </ul>
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: UsedInGuard.xslt 999 2005-11-01 22:12:58Z davep $</xd:cvsId>
  </xd:doc>
   
  <xd:doc>
    <xd:short>Determine if a variable is used in a guard expression</xd:short>
  </xd:doc>
  <xsl:template match="Decl[@kind='Variable']">
    
    <xsl:copy>
      <!-- Preserve the existing element information -->  
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:copy-of select="*"/>

      <xsl:apply-templates select="." mode="detect-guard-usage"/>
      
    </xsl:copy>
    
  </xsl:template>

  <xsl:template match="Decl" mode="detect-guard-usage">
    
    <xsl:variable name="decl-id" select="@id"/>
    
    <xsl:choose>
      <!-- Only applies to local variables of actions -->
      <xsl:when test="not( parent::Action )"/>
      
      <!-- See if this variable is directly used in a guard expr -->
      <xsl:when test="../Guards/Note[@kind='var-used'][@decl-id=$decl-id]">
        <Note kind="used-in-guard"/>
      </xsl:when>
      
      <!-- Repeat for all other local vars that use this one -->
      <xsl:otherwise>
        <xsl:apply-templates select="following-sibling::Decl[@kind='Variable']
        [Note[@kind='var-used'][@decl-id=$decl-id]]" mode="detect-guard-usage"/>
      </xsl:otherwise>
      
    </xsl:choose>
    
  </xsl:template>
  
  <xd:doc> Default just copies the input element to the result tree </xd:doc>
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>   
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>
