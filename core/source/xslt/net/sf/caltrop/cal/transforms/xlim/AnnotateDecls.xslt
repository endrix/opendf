<!--
	AnnotateDecls.xslt
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
  version="1.1">
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Annotate Declarations with the ID of the enclosing scope, and a flag
    denoting whether or not the value is assigned to at runtime.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on AddID.</li>
      </ul>
      Each Decl has a child element added of the form:<br/>
      <code>&lt;Note kind="declAnn" scope-id="<em>xyz</em>" reassigned="yes|no"/></code><br/>
      One ugly special case - look for applications of a member method specifically named "set".
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: AnnotateDecls.xslt 2451 2007-06-28 19:23:34Z imiller $</xd:cvsId>
  </xd:doc>

  <xd:doc>
    <xd:short>Find the varMod Notes for all variables that are defined in the specified scope.</xd:short>
    <xd:detail>
      This key can be used to determine if a variable is actually assigned in the
      program, whether or not it is declared as assignable. The key returns all the <code>varMod</code>
      Notes that refer to the variables defined in a given scope. This list can then be fitered to
      find the Notes just for a given variable name. If the resulting list is empty, then that
      variable never changes after it is initialized.
    </xd:detail>
  </xd:doc>
  <xsl:key name="assignments" match="Note[ @kind='varMod' ]" use="@scope-id"/>
                  
  <xd:doc>
    <xsl:short>Annotate Decl with an attribute indicating whether or not the variable is ever assigned to,
    and a reference to the enclosing scope.</xsl:short>
    <xsl:detail>The enclosing scope is determined in the same way that VariableAnnotator does it.
    If this ever gets out of sync, the downstream analyses will not work.
    <ul>
      <li> The enclosing scope for Decls inside a <code>Generator</code> is the grand-parent (ie.
        the enclosing List expr.</li>
      <li>The enclosing scope for Decls inside an <code>Input</code> is the grand-parent (ie.
      the enclosing Action.</li>
      <li> The scope for all other Decls is the parent.</li>
    </ul>
    </xsl:detail>
  </xd:doc>
  <xsl:template match="Decl">

    <!-- Determine id of enclosing scope according to the same rules as VariableAnnotator.xslt -->
    <xsl:variable name="scope-id">
      <xsl:choose>
        <xsl:when test="@kind='Generator'"><xsl:value-of select="../../@id"/></xsl:when>
        <xsl:when test="@kind='Input'"><xsl:value-of select="../../@id"/></xsl:when>
        <xsl:otherwise><xsl:value-of select="../@id"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="name" select="@name"/>

    <!-- Determine if this variable is ever actually written to at run-time -->    
    <xsl:variable name="mod">
      <xsl:choose>
        <xsl:when test="key('assignments',$scope-id)[../@name=$name]">yes</xsl:when>
        <xsl:otherwise>no</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- Determine if the set method is ever applied to this variable -->
    
        
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>

      <Note kind="declAnn" scope-id="{$scope-id}" reassigned="{$mod}"/>

    </xsl:copy>
      
  </xsl:template>

  <xd:doc>
    Default preserves the existing template.
  </xd:doc>
  <xsl:template match="*">
    
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>
      
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>