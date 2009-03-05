<!--
    EvaluateConstantExpressions.xslt
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
  
  <xsl:variable name="empty-env">
    <env kind="Initial"/>
    <env kind="Runtime"/>
  </xsl:variable>
  
  <!-- all templates are parameterized with environment and mode.
  
      env is a list of <env @kind/> elements
      kind can be 'Initial' or 'Runtime'
      
      the mode says which environment to use for evaluating Exprs 
  -->
  
  <!-- anything (except Expr) that scopes a Decl -->
  <xsl:template match="*[ Decl or Generator or Input ]" priority="1">
    <xsl:param name="env" select="$empty-env/env"/>
    <xsl:param name="mode">Runtime</xsl:param>
    
    <!-- construct the local environment -->
    <xsl:variable name="local-env">
      <xsl:call-template name="build-environment">
        <xsl:with-param name="parent-env" select="$env"/>
        <xsl:with-param name="mode" select="$mode"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates>
        <xsl:with-param name="env" select="$local-env/env"/>
        <xsl:with-param name="mode" select="$mode"/>
      </xsl:apply-templates>
    </xsl:copy>
    
  </xsl:template>

  <xsl:template match="Expr" priority="10">
    <xsl:param name="env" select="$empty-env/env"/>
    <xsl:param name="mode">Runtime</xsl:param>

    <!-- try to evaluate it -->
    <xsl:variable name="e" select="cal:evaluateExpr( . , $env[@kind=$mode] )"/>
    <!-- xsl:if test="$e//Note[ @kind='Report' ]">
      <debug>
        <input-expr>
          <xsl:copy-of select="."/>
        </input-expr>
        <result-expr>
          <xsl:copy-of select="$e"/>
        </result-expr>
        <xsl:copy-of select="$env[@kind=$mode]"/>
      </debug>
    </xsl:if -->
    
    <xsl:variable name="eval">

      <xsl:choose>
        <!-- suppress exprType info in a Type parameter -->
        <xsl:when test="parent::Entry and $e/Expr/@kind='Literal'">
          <Expr>
            <xsl:for-each select="$e/Expr/@*">
              <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
          </Expr>
        </xsl:when>
        <xsl:otherwise>
          <xsl:copy-of select="$e"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
 
    <xsl:choose>

      <!-- replace constants, except for ROM access -->
      <xsl:when test="not( $eval//Expr[@kind='Undefined'] ) and not( parent::Expr[@kind='Indexer'] )">
        <xsl:copy-of select="$eval/Expr"/>
      </xsl:when>
      
      <!-- keep the Expr from the input document, add an exprType note -->
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          <xsl:choose>
            <!-- creates a new environment for evaluating the children -->
            <xsl:when test="Decl or Generator">
              <xsl:variable name="local-env">
                <xsl:call-template name="build-environment">
                  <xsl:with-param name="parent-env" select="$env"/>
                  <xsl:with-param name="mode" select="$mode"/>
                </xsl:call-template>
              </xsl:variable>  
              <xsl:apply-templates>
                <xsl:with-param name="env" select="$local-env/env"/>
                <xsl:with-param name="mode" select="$mode"/>
              </xsl:apply-templates>
            </xsl:when>
            <xsl:otherwise>
              <!-- use the existing environment for evaluating the children -->
              <xsl:apply-templates>
                <xsl:with-param name="env" select="$env"/>
                <xsl:with-param name="mode" select="$mode"/>
              </xsl:apply-templates>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:copy-of select="$eval/Expr/Note[@kind='exprType']"/>
        </xsl:copy>
      </xsl:otherwise>  
      
    </xsl:choose>

  </xsl:template>
 
  <xsl:template match="Decl">
    <xsl:param name="env" select="$empty-env/env"/>
    <!-- no need for mode, Decls are always evaluated with 'Initial' -->
    
    <xsl:variable name="id" select="@id"/>
    <xsl:variable name="decl" select="$env[@kind='Initial']/Decl[@id=$id]"/>
    <xsl:variable name="expr" select="$decl/Expr"/>

    <xsl:choose>
      
      <!-- if this refers to a true constant other than a ROM we can remove the Decl -->
      <xsl:when test="$expr and Note[@kind='declAnn'][@reassigned='no'] and $expr/@kind!='List'
                  and not($decl//Expr[@kind='Undefined'])">
        <Note kind="Report" severity="Info" id="declaration.eliminated">
          <xsl:attribute name="subject">
            <xsl:apply-templates select="." mode="report-offender"/>
          </xsl:attribute>
          Unchanging declared variable eliminated by constant propagation
        </Note>
      </xsl:when>
      
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          
          <!--
            Note: the Type in the calml can diverge from the type to use
            in the environment because in repeats on inputs.
            
          <xsl:copy-of select="$decl/*[not( self::Expr )]"/>
          <xsl:choose>
            <xsl:when test="$expr">
              <xsl:copy-of select="$expr"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="Expr">
                <xsl:with-param name="env" select="$env"/>
                <xsl:with-param name="mode">Initial</xsl:with-param>
              </xsl:apply-templates>
            </xsl:otherwise>
          </xsl:choose>
          -->
          
          <xsl:apply-templates select="*">
            <xsl:with-param name="env" select="$env"/>
            <xsl:with-param name="mode">Initial</xsl:with-param>
          </xsl:apply-templates>
        </xsl:copy>
      </xsl:otherwise>
      
    </xsl:choose>
    
  </xsl:template>
   
  <xsl:template match="*">
    <xsl:param name="env" select="$empty-env/env"/>
    <xsl:param name="mode">Runtime</xsl:param>
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates>
        <xsl:with-param name="env" select="$env"/>
        <xsl:with-param name="mode" select="$mode"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  
  <!-- the local initial environment has all initializers evaluated with the
       parent environment augmented with all the preceding sibling Decls in this scope.
       Use this local environment for the intitializers of Decls in this scope. -->
  <xsl:template name="local-initial-decls">
    <xsl:param name="parent-env"/>
    <xsl:param name="src-decls"/>
    
    <xsl:if test="$src-decls">
      
      <!-- first evaluate all preceding Decls in this scope -->
      <xsl:variable name="preceding-siblings">
        <xsl:call-template name="local-initial-decls">
          <xsl:with-param name="parent-env" select="$parent-env"/>
          <xsl:with-param name="src-decls" select="$src-decls[position() &lt; last()]"/>
        </xsl:call-template>
      </xsl:variable>
      
      <xsl:variable name="this" select="$src-decls[last()]"/>
      
      <!-- env for evaluation of initializer includes all preceding sibling Decls -->
      <xsl:variable name="eval-env">
        <env kind="Initial">
          <xsl:copy-of select="$parent-env/*"/>
          <xsl:copy-of select="$preceding-siblings/Decl"/>
        </env>
      </xsl:variable>
      
      <!-- emit the Decls of the local environment -->
      <xsl:copy-of select="$preceding-siblings/Decl"/>
      
      <!-- append the current Decl with evaluated initializer -->
      <Decl>
        <xsl:for-each select="$this/@*">
          <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
        </xsl:for-each>
        
        <xsl:apply-templates select="$this/Type">
          <xsl:with-param name="env" select="$eval-env/env"/>
          <xsl:with-param name="mode">Initial</xsl:with-param>
        </xsl:apply-templates>
        
        <!-- this is needed for conversion from Initial to Runtime env -->
        <xsl:copy-of select="$this/Note[@kind='declAnn']"/>

        <xsl:variable name="e">
          <xsl:if test="$this/Expr">
            <xsl:copy-of select="cal:evaluateExpr( $this/Expr , $eval-env/env )"/>
          </xsl:if>
        </xsl:variable>
        
        <xsl:choose>
          <xsl:when test="$e and not( $e//Expr[@kind='Undefined'] )">            
            <xsl:copy-of select="$e"/>
          </xsl:when> 
          <xsl:otherwise>
            <Expr kind="Undefined"/>
          </xsl:otherwise>
        </xsl:choose>
      </Decl>
    </xsl:if>
    
  </xsl:template>

  <!-- the local runtime environment has any Decls that are assigned to at runtime
       set to Undefined. Use this environment for all Exprs other than initializers. -->
  <xsl:template name="local-runtime-decls">
    <xsl:param name="local-initial-decls"/>
    
    <xsl:for-each select="$local-initial-decls">
      <Decl>
        <xsl:for-each select="@*">
          <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
        </xsl:for-each>
        <xsl:copy-of select="Type"/>
        <xsl:choose>
          <xsl:when test="Note[@kind='declAnn'][@reassigned='yes']">
            <Expr kind="Undefined"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:copy-of select="Expr"/>
          </xsl:otherwise>
        </xsl:choose>
      </Decl>
    </xsl:for-each>
  </xsl:template>
  
  <!-- build a new environment -->
  <xsl:template name="build-environment">
    <xsl:param name="parent-env"/>
    <xsl:param name="mode"/>
    
    <!-- get all Decls that just came in scope, preserving document order -->
    <xsl:variable name="local-decls">
      <xsl:for-each select="*">
        <xsl:choose>
          <xsl:when test="self::Decl">
            <xsl:copy-of select="."/>
          </xsl:when>
          <xsl:when test="self::Input[ Repeat ]">
            <xsl:for-each select="Decl">
              <Decl>
                <xsl:for-each select="@*">
                  <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
                </xsl:for-each>
                <Type name="list">
                  <Entry kind="Expr" name="size">
                    <xsl:copy-of select="../Repeat/Expr"/>
                  </Entry>
                  <Entry kind="Type" name="type">
                    <xsl:copy-of select="Type"/>
                  </Entry>
                </Type>
              </Decl>
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="self::Input or self::Generator">
            <xsl:copy-of select="Decl"/>
          </xsl:when>
        </xsl:choose>
      </xsl:for-each>
    </xsl:variable>
    
    <!-- augment the parent environment with Imports -->
    <xsl:variable name="parent-env-plus-imports">
      <env>
        <xsl:copy-of select="$parent-env[@kind=$mode]/*"/>
        <xsl:copy-of select="Import"/>
      </env>
    </xsl:variable>
    
    <xsl:variable name="local-initial-decls">
      <xsl:call-template name="local-initial-decls">
        <xsl:with-param name="parent-env" select="$parent-env-plus-imports/env"/>
        <xsl:with-param name="src-decls" select="$local-decls/Decl"/>
      </xsl:call-template>
    </xsl:variable>

    <env kind="Initial">
      <xsl:copy-of select="$parent-env-plus-imports/env/*"/>
      <xsl:copy-of select="$local-initial-decls/Decl"/>
    </env>
    <env kind="Runtime">
      <xsl:copy-of select="$parent-env-plus-imports/env/*"/>
      <xsl:call-template name="local-runtime-decls">
        <xsl:with-param name="local-initial-decls" select="$local-initial-decls/Decl"/>
      </xsl:call-template>
    </env>

  </xsl:template>
  
</xsl:stylesheet>
