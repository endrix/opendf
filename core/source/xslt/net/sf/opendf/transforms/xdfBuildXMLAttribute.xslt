<!--
	xdfBuildXMLAttribute.xslt
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
    xmlns:math="http://exslt.org/math"
    version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Builds XML structure from xmlElement Attribute</xd:short>
    <xd:detail>
      For any Attribute of kind 'xmlElement' the structure of the attribute will be analyzed and
      an XML snippet generated based on that structure.  The generated XML is based on a map containing
      definintions for the 4 pieces which make up an XML element: name, attributes, children, and contents (text).  
    </xd:detail>
    <xd:cvsId>$Id:$</xd:cvsId>
    <xd:author>IDM</xd:author>
    <xd:copyright>Xilinx, 2007</xd:copyright>
  </xd:doc>

  <!--
      Build an XML fragment based on the structure defined in the xmlElement Attribute.
  -->
  <xsl:template match="Attribute[@name='xmlElement']">
    <!--
    <xsl:variable name="containedMaps" select=".//Expr[@kind='Map']"/>
    <xsl:call-template name="buildXMLExpr">
      <xsl:with-param name="mapSource" select="$containedMaps[1]"/>
    </xsl:call-template>
    -->
    <!-- Find the topmost List (multiple elements) or Map (single element) -->
    <xsl:apply-templates mode="discoverTopLevel"/>
  </xsl:template>

  <xsl:template match="Expr[@kind='List']" mode="discoverTopLevel">
    <!-- It must be a list of Map elements or it is incorrectly structured -->
    <xsl:for-each select="./Expr[@kind='Map']">
      <xsl:call-template name="buildXMLExpr">
        <xsl:with-param name="mapSource" select="."/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="Expr[@kind='Map']" mode="discoverTopLevel">
    <xsl:call-template name="buildXMLExpr">
      <xsl:with-param name="mapSource" select="."/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="*" mode="discoverTopLevel">
      <xsl:apply-templates mode="discoverTopLevel"/>
  </xsl:template>

  <!--
      convert maps to XML elements.  Recognized keys are:
        name -> element name
        attr -> contains a map of name->value
        children -> contains a List of either Map or directly copyable elements
        text -> contains a value
      Any map with a key named 'expr' will have it's Expr value copied directly.
  -->
  <xsl:template name="buildXMLExpr">
    <xsl:param name="mapSource" select="_default_to_empty"/>

    <xsl:variable name="expr" select="$mapSource/Mapping/Expr[@value='expr']/../Expr[not( @value='expr')]"/>
    <xsl:variable name="name" select="$mapSource/Mapping/Expr[@value='name']/../Expr[not( @value='name')]/@value"/>

    <xsl:choose>
      <!-- when the node is an expr simply copy it, othewise construct it -->
      <xsl:when test="$expr">
        <xsl:copy-of select="$expr"/>
      </xsl:when>

      <xsl:otherwise>
        <xsl:element name="{$name}">

          <!-- Define the attributes.  These must be static values (not evaluated) -->
          <xsl:variable name="attrMap" select="$mapSource/Mapping/Expr[@value='attr']/../Expr[not( @value='attr')]"/>
          <xsl:for-each select="$attrMap/Mapping">
            <xsl:variable name="exprs" select="Expr"/>
            <xsl:attribute name="{$exprs[1]/@value}"><xsl:value-of select="$exprs[2]/@value"/></xsl:attribute>
          </xsl:for-each>

          <!-- The list of children.  Maps will be further constructed, everything else copied 'as is' -->
          <xsl:variable name="childrenList" select="$mapSource/Mapping/Expr[@value='children']/../Expr[@kind='List']"/>
          <xsl:for-each select="$childrenList/*">
            <xsl:choose>
              <xsl:when test="self::Expr[@kind='Map']">
                <xsl:call-template name="buildXMLExpr">
                  <xsl:with-param name="mapSource" select="."/>
                </xsl:call-template>
              </xsl:when>
              <xsl:otherwise>
                <xsl:copy-of select="."/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>

          <!-- Copy any textual content -->
          <xsl:variable name="text" select="$mapSource/Mapping/Expr[@value='text']/../Expr[not( @value='text') ]/@value"/>
          <xsl:value-of select="$text"/>
          
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
  
</xsl:stylesheet>



