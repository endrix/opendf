<!--
	AnnotateActionQIDs.xslt
    
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
  version="1.1" >
  
  <xsl:output method="xml"/>
 
  <xd:doc type="stylesheet">
    <xd:short>Annotate QIDs with cross-references to each matching named Action</xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on AddID.xslt to provide Id attributes on all actions</li>
        <li>2005-07-13 DBP Created</li>
      </ul>
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: AnnotateActionQIDs.xslt 754 2005-07-28 21:02:25Z davep $</xd:cvsId>
    
  </xd:doc>

  <xd:doc>
    <xd:short>Annotate QIDs with cross-references to each matching named Action</xd:short>
    <xd:detail>
      Action QIDs are matched against all named Actions of the enclosing Actor.
      Cross-reference notes are added to the QID for
      each Action whose name matches the QID exactly, or for whose name the QID is a
      prefix (followed by '.'). The  cross-reference is of the form<br/>
      <code>&lt;Note kind="ActionId" id="<em>xxx</em>"></code><br/>
    </xd:detail>
  </xd:doc>
  <xsl:template match="QID">
    <xsl:copy>
      <!-- Preserve the existing attributes and contents -->
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>

      <xsl:variable name="QID" select="@name"/>      
      <xsl:variable name="prefix" select="concat($QID,'.')"/>
      <xsl:variable name="action-list" select="ancestor::*[name()='Actor']/Action"/>
      <xsl:for-each select="$action-list/QID[@name=$QID or starts-with(@name,$prefix)]">
        <Note kind="ActionId" id="{../@id}"/>
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
