<!--
	CopyQIDToAction.xslt
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
  version="1.1" >
  
  <xsl:output method="xml"/>
 
  <xd:doc type="stylesheet">
    <xd:short>Copy the name field from matched QID to the Action tag</xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes CALML</li>
        <li>Relies on AnnotateActionQIDs.xslt to provide correlation from Action id to QID note</li>
        <li>2006-07-18 IDM created</li>
      </ul>
    </xd:detail>
    <xd:author>IDM</xd:author>
    <xd:copyright>Xilinx, 2006</xd:copyright>
    <xd:cvsId>$Id: AnnotateActionQIDs.xslt 754 2005-07-28 21:02:25Z davep $</xd:cvsId>
  </xd:doc>

  <xd:doc>
    <xd:short>Annotate Actions with name attribute derived from QID or
    generated if no QID available</xd:short>
    <xd:detail>
      Action names are annotated as attributes as derived from the QID or as generated
      from the start line number attribute if there is no QID matched to the action.
    </xd:detail>
  </xd:doc>
  <xsl:template match="Action">
    <xsl:variable name="actionid" select="@id"/>
    <xsl:copy>
      <!-- Preserve the existing attributes and contents -->
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <!-- Find matching QID (sub-node) and annotate the name -->
      <xsl:variable name="matchedQID" select="descendant::QID/Note[@kind='ActionId'][@id=$actionid]"/>
      <xsl:choose>
        <xsl:when test="count($matchedQID) = 1">
          <xsl:attribute name="name"><xsl:value-of select="$matchedQID[1]/../@name"/></xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="name"><xsl:value-of select="concat('actionAtLine_',./@text-begin-line)"/></xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates/>

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
