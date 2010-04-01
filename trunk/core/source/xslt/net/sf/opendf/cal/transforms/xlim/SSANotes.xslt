<!--
	SSANotes.xslt
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
  version="1.1"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  extension-element-prefixes="xsl xd"
  >
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Augment the Notes[@kind='var-used'] to indicate local source of a value or
    last modifier.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on VariableUsage</li>
      </ul>
      Add an attribute to the var-usage info pointing to last modifier and previous modifier.
      For each var-used note with mode 'read' a preceding-sibling-modifier attribute is added IFF the
      variable in question is modified by a prior (document order) sibling to the parent of
      the note.  If the var in question is NOT modified by a prior sibling, then no attribute
      is added, meaning that the value of the var is passed in from a containing scope.
      For each var-used note with mode 'write' a last-child-modifier attribute is added indicating the
      last child of the containing element to modify the variable.
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: SSANotes.xslt 1126 2005-12-29 17:28:13Z stephenn $</xd:cvsId>
  </xd:doc>
  
<xd:doc>Augment write notes to include tag of last modifier in this context</xd:doc>

<xsl:template match="Note[ @kind='var-used' and @mode='write' ]">
  <xsl:variable name="name" select="@name"/>
  <xsl:variable name="scope-id" select="@scope-id"/>
  <xsl:variable name="modifier" select="../*[Note[@kind='var-used' and @name=$name
    and @scope-id=$scope-id and @mode='write'] ][position() = last()]"/>
  <xsl:copy>
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

<!--  
    <xsl:if test="$modifier/@id">
      <xsl:attribute name="last-child-modifier"><xsl:value-of select="$modifier/@id"/></xsl:attribute>
    </xsl:if>
-->

	<xsl:choose> 
      <xsl:when test="$modifier/@id">
        <xsl:attribute name="last-child-modifier"><xsl:value-of select="$modifier/@id"/></xsl:attribute>
      </xsl:when>

      <xsl:otherwise>
        <xsl:attribute name="no-last-child-modifier">true</xsl:attribute>
      </xsl:otherwise>    
    </xsl:choose>
        
  </xsl:copy>
  
</xsl:template>

<xd:doc>Augment read notes to point to the most recent modifier in this scope (if there is one).</xd:doc>

<xsl:template match="Note[ @kind='var-used' and (@mode='read' or @mode='mutate')]">
  <xsl:variable name="name" select="@name"/>
  <xsl:variable name="scope-id" select="@scope-id"/>
  
  <!-- Find the nearest preceding modifier of this variable in document order -->

  <xsl:variable name="modifier" select="../preceding-sibling::*[Note[@kind='var-used' and @name=$name
    and @scope-id=$scope-id and @mode='write']][ position() = 1 ]"/>
 <!--
  <xsl:variable name="modifier" select="preceding::*[Note[@kind='var-used' and @name=$name
    and @scope-id=$scope-id and @mode='write']][ position() = 1 ]"/>
-->  
  <xsl:copy>
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

    <!-- Make sure not to list the "then" block as a preceding modifier for a variable
         consumed in an "else" block -->
<!--   
    <xsl:if test="$modifier/@id and not (../parent::Stmt[@kind='If'])">
      <xsl:attribute name="preceding-sibling-modifier"><xsl:value-of select="$modifier/@id"/></xsl:attribute>
    </xsl:if>
-->    
    <xsl:choose>
      <xsl:when test="$modifier/@id and not (../parent::Stmt[@kind='If'])">
        <xsl:attribute name="preceding-sibling-modifier"><xsl:value-of select="$modifier/@id"/></xsl:attribute>
      </xsl:when>

      <xsl:otherwise>
        <xsl:attribute name="no-preceding-sibling-modifier">true</xsl:attribute>
      </xsl:otherwise>    
    </xsl:choose>
    
    
  </xsl:copy>

</xsl:template> 
  	
<!-- Preserve other elements -->
<xsl:template match ="*">
  
  <xsl:copy>
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

    <xsl:apply-templates/>
  </xsl:copy>
  
</xsl:template>

</xsl:stylesheet>
