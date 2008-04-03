<!--
  ReplaceConstantGenerators.xslt
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
  xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator" 
  extension-element-prefixes="xsl xd math cal" 
  version="2.0">
  <xsl:output method="xml"/>

  <xsl:template match="Expr[ Generator ]">
    
    <Expr>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>

      <xsl:call-template name="replace-constant-generators">
        <xsl:with-param name="E">
          <xsl:copy-of select="Expr"/>
        </xsl:with-param>
        <xsl:with-param name="G" select="Generator"/>
      </xsl:call-template>
      
      <xsl:apply-templates select="*[not( self::Expr )][not( self::Generator )]"/>
      
    </Expr>
    
  </xsl:template>
  
  <!-- we have to re-evaluate after this, so clear out the notes -->
  <xsl:template match="Note[ @kind='exprType' ]"/>
  
  <xsl:template match="*">
    
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>   
      
    </xsl:copy>
  </xsl:template>

  <xsl:template name="replace-constant-generators">
    <xsl:param name="E"/>
    <xsl:param name="G"/>
    
    <xsl:choose>
      <!-- all done -->
      <xsl:when test="not( $G )">
        <xsl:copy-of select="$E/*"/>
      </xsl:when>
      
      <!-- generator is not constant -->
      <xsl:when test="$G/Expr[not( @kind='List' ) or Generator ]">
        <xsl:copy-of select="$E/*"/>
        <xsl:copy-of select="$G"/>
      </xsl:when>
      
      <xsl:otherwise>
        <xsl:call-template name="replace-constant-generators">
          <xsl:with-param name="E">
            <xsl:for-each select="$G[last()]/Expr/Expr">
              <!-- create a decl for use in a Let -->
              <xsl:variable name="D">
                <Decl name="{../../Decl/@name}">
                  <xsl:copy-of select="../../Decl/Type"/>
                  <xsl:copy-of select="."/>
                </Decl>
              </xsl:variable>
              <xsl:for-each select="$E/*">
                <Expr kind="Let">
                  <xsl:copy-of select="$D/*"/>
                  <xsl:copy-of select="."/>
                </Expr>
              </xsl:for-each>
            </xsl:for-each>
          </xsl:with-param>
          <xsl:with-param name="G" select="$G[ position() != last() ]"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>

</xsl:stylesheet>