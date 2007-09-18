<!--
	VariableUsage.xslt
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

	Annotate XHDL with a list of the variables referenced or modified.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.1"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"  >
<xsl:output method="xml"/>

  <xd:doc type="stylesheet">
    <xd:short>Annotate every element with a list of variables read or
      modified by it or any of its children.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on AnnotateDecls, VariableAnnotator, Inline</li>
        <li> TODO: foreach, choose not supported</li>
      </ul>
      This transform does not work through procedure or function calls, so
      they must be inlined first. Also, it now relies on the variable annotator instead
      of determining free variables directly.<br/>
      For each variable referenced or modified in an element or its children,
      a Note is created as follows:<br/>
      <code>&lt;Note kind="var-used" mode="read|write" name="<em>nnn</em>" scope-id="<em>xyz</em>">
      <br/>&lt;Type ... /></code><br/>
      The list of Notes is uniquified, and extra read Notes are added for variables that are only modified
      in a while statement or in one branch of an if statement. Notes from children are suppressed in the 
      defining scope, so the notes apply only to variables defined at a higher scope.<br/>
      Only referenced variable with a scope are annotated (ie. externals will not be).
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: VariableUsage.xslt 1038 2005-11-07 19:01:15Z davep $</xd:cvsId>
  </xd:doc>
	
<xd:doc>Roll up vars-used from children, add usage info for the current element, uniquify list </xd:doc>
<xsl:template match="*">
  
  <!-- Preserve the existing element information -->  
  <xsl:copy>
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

    <!-- Process child elements and catch return in a variable -->   
    <xsl:variable name="content">
      <xsl:apply-templates/>
    </xsl:variable>

    <!-- First get all usage information from child elements that is not local to current scope -->
    <xsl:variable name="this-scope" select="@id"/>
    <xsl:variable name="filtered-child-usage">
      <xsl:copy-of select="$content/*/Note[@kind='var-used'][@scope-id != $this-scope]"/>
    </xsl:variable>
    
    <!-- Gather all variables used in this element (handle uniqueness later) -->
    <xsl:variable name="usage">

      <xsl:copy-of select="$filtered-child-usage/Note"/>

      <!-- Inject notes based on this element type -->
      <xsl:choose>
              
        <!-- While statements must also read any modified variable (for phi block/fallthrough case) -->
        <xsl:when test="self::Stmt[ @kind='While' ]">
          <xsl:for-each select="$filtered-child-usage/Note[@mode='write']">
            <Note kind="var-used" mode="read" name="{@name}" decl-id="{@decl-id}"
              scope-id="{@scope-id}" scalar="{@scalar}"/>
          </xsl:for-each>
        </xsl:when>
      
        <!-- If statements with a write in only one branch must read the variable (for phi block/fallthrough case) -->
        <xsl:when test="self::Stmt[ @kind='If' ]">
          <xsl:for-each select="$filtered-child-usage/Note[@mode='write']">
            <xsl:variable name="decl-id" select="@decl-id"/>
            <xsl:if test="count( $filtered-child-usage/Note[@mode='write'][ @decl-id=$decl-id ] ) = 1">
              <Note kind="var-used" mode="read" name="{@name}" decl-id="{@decl-id}"
                scope-id="{@scope-id}"  scalar="{@scalar}"/>
            </xsl:if>
          </xsl:for-each>
        </xsl:when>
 
        <!-- Declarations which assign a value are modifiers too -->
        <xsl:when test="self::Decl[ @kind='Input' or Expr ]">
          <Note kind="var-used" mode="write" name="{@name}" decl-id="{@id}"
            scope-id="{ Note[@kind='declAnn']/@scope-id }">
            <xsl:attribute name="scalar">
              <xsl:choose>
                <xsl:when test="Type/Entry[@kind != 'Expr']">no</xsl:when>
                <xsl:otherwise>yes</xsl:otherwise>
              </xsl:choose>
            </xsl:attribute>
          </Note>
        </xsl:when>
    
      </xsl:choose>
          
      <!-- Add all variables referenced in this element which are not external -->      
      <xsl:for-each select="Note[@kind='varRef'][@scope-id]">
        <Note kind="var-used" mode="read" name="{../@name}" decl-id="{@decl-id}" scope-id="{@scope-id}">
          <xsl:attribute name="scalar">
            <xsl:choose>
              <xsl:when test="Type/Entry[@kind != 'Expr']">no</xsl:when>
              <xsl:otherwise>yes</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </Note>
      </xsl:for-each>

      <!-- Add all variables modified in this element -->      
      <xsl:for-each select="Note[@kind='varMod']">
        <Note kind="var-used" mode="write" name="{../@name}" decl-id="{@decl-id}" scope-id="{@scope-id}">
          <xsl:attribute name="scalar">
            <xsl:choose>
              <xsl:when test="Type/Entry[@kind != 'Expr']">no</xsl:when>
              <xsl:otherwise>yes</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </Note>
      </xsl:for-each>
      
    </xsl:variable>

    <!-- Put the child nodes into this element -->
    <xsl:copy-of select="$content"/>

    <!-- Put uniquified list of notes into this element -->
    <xsl:for-each select="$usage/Note">
      <xsl:variable name="name" select="@name"/>
      <xsl:variable name="scope-id" select="@scope-id"/>
      <xsl:variable name="mode" select="@mode"/>
      <xsl:if test="not(preceding-sibling::Note[@name=$name and @scope-id=$scope-id and @mode=$mode])">
        <xsl:copy-of select="."/>
      </xsl:if> 
    </xsl:for-each>
  </xsl:copy>

</xsl:template>

</xsl:stylesheet>
