<!--
	EnumerateStates.xslt
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
    <xd:short>Add notes that enumerate all FSM states</xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
      </ul>
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: EnumerateStates.xslt 999 2005-11-01 22:12:58Z davep $</xd:cvsId>
  </xd:doc>
  
  <xd:doc>
    <xd:short>Add a "state-enum" Note for each state referenced in an FSM declaration.</xd:short>
  </xd:doc>
  
  <xsl:template match="Actor">
    
    <xsl:copy>
      <!-- Preserve the existing element information -->  
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:copy-of select="*"/>
      
      <xsl:variable name="states">
        <!-- Initial state must be first on the list -->
        <xsl:if test="Schedule[@kind='fsm']/@initial-state">
          <state name="{Schedule[@kind='fsm']/@initial-state}"/>
        </xsl:if>
        <xsl:for-each select="Schedule[@kind='fsm']/Transition">
          <state name="{@from}"/>
          <state name="{@to}"/>
        </xsl:for-each>
      </xsl:variable>
 
      <xsl:variable name="enum">
        <xsl:for-each select="$states/state">
          <xsl:variable name="pos" select="position()"/>
          <xsl:variable name="name" select="@name"/>
          <xsl:if test="not( $states/state[ position() &lt; $pos][ @name=$name ] )">
            <Note name="{@name}"/>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      
      <xsl:for-each select="$enum/Note">
        <Note kind="state-enum" name="{@name}" index="{position() - 1}"/>
      </xsl:for-each>
      
    </xsl:copy>
    
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
