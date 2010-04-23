<!--
	AddDefaultTypes.xslt
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
    <xd:short>Add default types and default type parameters for various declarations.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>2005-08-02 DBP Created</li>
        <li>Relies on AddDirectives.xslt and suitable directives in either
          DefaultDirectives.xml or the individual project directives.</li>
      </ul>
      Default values for types and type parameters must be attached to the root
      node of the design document using the AddDirectives mechanism. The defaults
      will appear as:<br/>
      <code>&lt;Note kind="Directive" name="default-type-name"></code> and<br/>
      <code>&lt;Note kind="Directive" name="default-type-templates"></code><br/>
      Input token declarations do not receive a type.  One cannot just assign the
      input port type as this only works for single token reads. The correct type
      of input tokens must be determined later by the code generator.
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: AddDefaultTypes.xslt 828 2005-08-18 19:20:38Z davep $</xd:cvsId>
  </xd:doc>

  <xd:doc>
    <xd:short>Add default type declarations to variables, parameters and lambdas.</xd:short>
    <xd:detail>The default type is selected by finding the closest "default-type-name" directive
    along the ancestor-or-self:: axis. This ensures that the directive with the most specific
    context will be used.</xd:detail>
  </xd:doc>
  <xsl:template match="Decl[ not(Type) ] | Expr[ @kind='Lambda' ][ not(Type) ]">
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>
      
      <xsl:variable name="name" select="ancestor-or-self::*/Note[ @kind='Directive' ]
                           [ @name='default-type-name' ][1]/Expr/@value"/>

      <xsl:copy-of select="ancestor-or-self::*/Note[ @kind='Directive' ]
          [ @name='default-type-templates' ][1]/Type[ @name=$name ]"/>
          
    </xsl:copy>
  </xsl:template>

  <xd:doc>
    <xd:short>Add missing type parameters to Type elements.</xd:short>
    <xd:detail>The closest prototype declaration for the type is found in a
    "default-type-templates" directive along the ancestor-or-self:: axis. Any
    Entry elements in the prototype that are not present will be copied over.</xd:detail>
  </xd:doc>
  <xsl:template match="Type">
    
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>

      <xsl:variable name="name" select="@name"/>            
      <xsl:variable name="this" select="."/>      
      <xsl:variable name="prototype" select="ancestor-or-self::*/Note[ @kind='Directive'
        and @name='default-type-templates'][1]/Type[ @name=$name]"/>
      <xsl:for-each select="$prototype/Entry">
        <xsl:variable name="entry-name" select="@name"/>
        <xsl:if test="not( $this/Entry[ @name=$entry-name ] )">
          <xsl:copy-of select="."/>
        </xsl:if>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>
  
  <xd:doc>
    Preserve all other elements.
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