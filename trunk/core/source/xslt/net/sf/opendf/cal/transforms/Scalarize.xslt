<!--
  Scalarize.xslt
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
  extension-element-prefixes="xsl" 
  version="2.0">
  <xsl:output method="xml"/>

  <xsl:key name="decl" match="Decl" use="@id"/>
  
  <xsl:variable name="dim-sep">$dim$</xsl:variable>
  
  <!-- scalarize a variable -->
  <xsl:template match="Decl[ Note[@kind='Scalarizable'] ]">
    
    <xsl:variable name="types-in-doc-order">
      <xsl:copy-of select="Type/descendant-or-self::Type[@name='List']"/>
    </xsl:variable> 
    
    <xsl:call-template name="scalarize">
      <xsl:with-param name="name" select="@name"/>
      <xsl:with-param name="dims">
        <xsl:copy-of select="$types-in-doc-order/Type/Entry[@kind='Expr'][@name='size']/Expr"/>
      </xsl:with-param>
      <xsl:with-param name="init">
        <xsl:copy-of select="Expr/*"/>
      </xsl:with-param>
    </xsl:call-template>
    
  </xsl:template>
   
  <!-- scalarize input tokens -->
  <xsl:template match="Input[ Decl[ Note[@kind='Scalarizable'] ] ]">
    <Input>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:variable name="tokens">
        <xsl:for-each select="Decl">
          <xsl:call-template name="scalarize">
            <xsl:with-param name="name" select="@name"/>
            <xsl:with-param name="dims">
              <xsl:copy-of select="../Repeat/Expr"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:for-each>
      </xsl:variable>
      
       
      <!-- now interleave the tokens -->
      <xsl:variable name="first-prefix" select="concat( Decl[1]/@name, $dim-sep )"/>
      <xsl:variable name="decls" select="Decl"/>
      <xsl:for-each select="$tokens/Decl[ starts-with( @name, $first-prefix ) ]">
        <xsl:variable name="i" select="1 + last() - position()"/>
        <xsl:variable name="n" select="last()"/>
        <xsl:for-each select="$decls">
          <xsl:variable name="j" select="position() - 1"/>
          <xsl:copy-of select="$tokens/Decl[$i + $j * $n]"/>
        </xsl:for-each>
      </xsl:for-each>
      
    </Input>
  </xsl:template>

  <xsl:template match="Expr[@kind='Indexer']">
    
    <xsl:variable name="decl-id" select="Expr/Note[@kind='varRef']/@decl-id"/>
    
    <xsl:choose>
      <xsl:when test="key('decl',$decl-id)/Note[@kind='Scalarizable']">
        <xsl:variable name="name">
          <xsl:call-template name="indexed-name">
            <xsl:with-param name="base" select="Expr/@name"/>
            <xsl:with-param name="args" select="Args/Expr"/>
          </xsl:call-template>
        </xsl:variable>
        <Expr kind="Var" name="{$name}"/>
      </xsl:when>
      
      <xsl:otherwise>
        <!-- Preserve the existing element information -->  
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          
          <xsl:apply-templates/>   
          
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>
 
  
  <xsl:template match="Stmt[@kind='Assign']">
    
    <xsl:variable name="decl-id" select="Note[@kind='var-used'][@mode='write']/@decl-id"/>
    
    <xsl:choose>
      <xsl:when test="key('decl',$decl-id)/Note[@kind='Scalarizable']">
        <xsl:variable name="name">
          <xsl:call-template name="indexed-name">
            <xsl:with-param name="base" select="@name"/>
            <xsl:with-param name="args" select="Args/Expr"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="if (name()='name') then $name else ."/></xsl:attribute>
          </xsl:for-each>
          
          <xsl:apply-templates select="*[not( self::Args )]"/>   
          
        </xsl:copy>
      </xsl:when>
      
      <xsl:otherwise>
        <!-- Preserve the existing element information -->  
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          
          <xsl:apply-templates/>   
          
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>
    
  <xsl:template match="*">
    
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>   
      
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="indexed-name">
    <xsl:param name="base"/>
    <xsl:param name="args"/>
    <xsl:choose>
      <xsl:when test="not($args)">
        <xsl:value-of select="$base"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="indexed-name">
          <xsl:with-param name="base" select="concat($base,$dim-sep,$args[1]/@value)"/>
          <xsl:with-param name="args" select="$args[position() &gt; 1]"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template name="scalarize">
    <xsl:param name="name"/>
    <xsl:param name="dims"/>
    <xsl:param name="init" select="__empty__"/>
    
    <xsl:variable name="i" select="$dims/Expr[1]/@value - 1"/>
    <xsl:variable name="new-name" select="concat($name,$dim-sep,$i)"/>

    <xsl:choose>
       <!-- iterate over all the remaining dimension -->
      <xsl:when test="count( $dims/Expr ) &gt; 1">
        <xsl:call-template name="scalarize">
          <xsl:with-param name="name" select="$new-name"/>
          <xsl:with-param name="dims">
            <xsl:copy-of select="$dims/Expr[position() &gt; 1]"/>
          </xsl:with-param>
          <xsl:with-param name="init">
            <xsl:copy-of select="$init/Expr[last()]/Expr"/>
          </xsl:with-param>
        </xsl:call-template> 
      </xsl:when>
      
      <!-- generate this element -->
      <xsl:otherwise>
        <Decl name="{$new-name}" kind="{@kind}">
          <xsl:choose>
            <xsl:when test="parent::Input">
              <xsl:copy-of select="Type"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:copy-of select="Type//descendant-or-self::Type[last()]"/>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:copy-of select="$init/Expr[last()]"/>
        </Decl>
      </xsl:otherwise>
    </xsl:choose>
    
    <!-- complete iterating over this dimension -->
    <xsl:if test="$i &gt; 0">
      <xsl:call-template name="scalarize">
        <xsl:with-param name="name" select="$name"/>
        <xsl:with-param name="dims">
          <Expr value="{$i}"/>
          <xsl:copy-of select="$dims/Expr[position() &gt; 1]"/>
        </xsl:with-param>
        <xsl:with-param name="init">
          <xsl:copy-of select="$init/Expr[position() != last()]"/>
        </xsl:with-param>
      </xsl:call-template> 
    </xsl:if>
    
  </xsl:template>

</xsl:stylesheet>