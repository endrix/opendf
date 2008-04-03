<!--
    EvaluateNetworkExpressions.xslt
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
  
  <xsl:include href="net/sf/opendf/cal/checks/reportOffenders.xslt"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Replace certain expressions that are unchanging at run-time with its compile-time value.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on AddDefaultTypes, VariableAnnotator, VariableSorter, AnnotateDecls</li>
      </ul>
    </xd:detail>
    Evaluates expressions within network level attributes and instance parameters.  No
    environment is provided as any necessary declarations are assumed to be copied
    local to the expression.  This is done during the flattening process by CopyRequireDecls.
    <xd:author>IDM, DBP</xd:author>
    <xd:copyright>Xilinx, 2007</xd:copyright>
  </xd:doc>
  
  <xd:doc>
    <xd:short>Evaluate an expression (and replace if constant)</xd:short>
    <xd:detail>The expression evaluator is called with this expression.  The
    expression will be replaced with the evaluated result if its value can
    be determined. If the evaluator returns a Note it will be added to the
    Expr.
    </xd:detail>
  </xd:doc>
  <xsl:template match="Instance/Attribute/Expr">
    <xsl:call-template name="evaluate-expr"/>
  </xsl:template>

  <xsl:template match="Instance/Parameter/Expr">
    <xsl:call-template name="evaluate-expr"/>
  </xsl:template>
  
  <xsl:template match="Connection/Attribute/Expr">
    <xsl:call-template name="evaluate-expr"/>
  </xsl:template>

  
  <xsl:template name="evaluate-expr">

    <xsl:variable name="eval">
      <xsl:copy-of select="cal:evaluateConstantExpr( . )"/>
    </xsl:variable>

    <xsl:variable name="error" select="$eval//Note[ @kind='Report' ][ @severity='Error' ][1]"/>

    <!-- For debug
         <xsl:message>Evaluated expr <xsl:value-of select="@name"/>@<xsl:value-of select="@id"/>
         <xsl:for-each select="$eval/*">
         <xsl:value-of select="name()"/>
         <xsl:for-each select="@*">
         <xsl:value-of select="name()"/>=<xsl:value-of select="."/>:
         </xsl:for-each>
         </xsl:for-each>
         </xsl:message>
    -->
    
    <xsl:choose>
      <!-- When the evaluator cannot determine a type, keep this error -->
      <xsl:when test="$error">
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          
          <xsl:apply-templates/>
          
          <Note>
            <xsl:for-each select="$error/@*">
              <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:attribute name="subject">
              <xsl:apply-templates select="." mode="report-offender-context"/>
            </xsl:attribute>
            <xsl:copy-of select="$error/text()"/>
          </Note>
        </xsl:copy>
      </xsl:when>

      <!-- Undefined values, or varRefs in an Indexer cannot be replaced -->
      <xsl:when test="$eval//Expr[ @kind='Undefined' ] or parent::Expr[ @kind='Indexer' ]">
        <!--Keep this Expr, posssibly adding a return type Note -->
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>

          <xsl:apply-templates select="*[not(self::Note[@kind='exprType'])]"/>
          <xsl:copy-of select="$eval/Expr/Note"/>
        </xsl:copy>

      </xsl:when>

      <!-- successful evaluation, copy the new Expr -->
      <xsl:otherwise>
        <xsl:copy-of select="$eval/Expr"/>
      </xsl:otherwise>
      
    </xsl:choose>
  </xsl:template>  
  
  <xd:doc>
    Default constant propagation template
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
