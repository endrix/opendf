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
    xmlns:cal="java:net.sf.caltrop.xslt.cal.CalmlEvaluator"  
    version="2.0">
  <xsl:output method="xml"/>
  
  <xsl:include href="net/sf/caltrop/cal/checks/reportOffenders.xslt"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Replace any expression that is unchanging at run-time with its compile-time value.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on AddDefaultTypes, VariableAnnotator, VariableSorter, AnnotateDecls</li>
      </ul>
    </xd:detail>
    When a new scope is entered, all the Decls are collected, evaluated one at a time, and added
    to the environment (a list of all declarations in scope). Each Decl in the list must be evaluated
    with all preceding Decls already in the environment because there may be dependencies on preceding
    siblings in their initializers. For the purposes of evaluating initializers, all siblings are
    considered constant even if the variables themselves can be assigned at run time. Once
    the new Decls have been added to the environment, the initializer values for variables (ie
    any values that can be reassigned at run-time) are marked as undefined so that run-time
    expressions that reference them are not treated as constant.
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: EvaluateConstantExpressions.xslt 2451 2007-06-28 19:23:34Z imiller $</xd:cvsId>
  </xd:doc>
  
  <xd:doc>
    <xd:short>Evaluate an expression (and replace if constant)</xd:short>
    <xd:detail>The expression evaluator is called with this expression and the
    current environment. The expression will be replaced with the evaluated
    result if its value can be determined. If the evaluator returns a Note it
    will be added to the Expr. If there are any Decls in scope they will be
    added to the environment before the Expr is evaluated.
    </xd:detail>
    <xd:param name="env">The environment table.</xd:param>
  </xd:doc>
  <xsl:template match="Expr">
    <xsl:param name="env" select="_default_to_empty_"/>

    <!-- Add any Decls that have just come in scope, preserving document order -->
    <xsl:variable name="new-env">
      <xsl:call-template name="new-env">
        <xsl:with-param name="old-env">
          <xsl:copy-of select="$env/*"/>
        </xsl:with-param>
        <xsl:with-param name="new-context" select="."/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="freeVars" select=".//Note[@kind='freeVar']"/>        
    <xsl:variable name="wrapped-env">
      <env>
        <!-- Filter to relevant decls (ignore shadowing for simplicity but it will still work) -->
        <!-- xsl:copy-of select="$new-env/Import"/>
             <xsl:for-each select="$new-env/Decl">
             <xsl:variable name="name" select="@name"/>
             <xsl:if test="$freeVars[@name = $name]">
             <xsl:copy-of select="."/>
             </xsl:if>
             </xsl:for-each -->
        <xsl:if test="@kind != 'Literal'">
          <xsl:copy-of select="$new-env/*"/>
        </xsl:if>
      </env>
    </xsl:variable>

    <xsl:variable name="eval">
      <xsl:copy-of select="cal:evaluateExpr( ., $wrapped-env/env)"/>
    </xsl:variable>

    <xsl:variable name="error" select="$eval//Note[ @kind='Report' ][ @severity='Error' ][1]"/>

    <!-- For debug
         <xsl:message>Evaluating expr <xsl:value-of select="@name"/>@<xsl:value-of select="@id"/>
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
        <!-- To allow partial evaluation, keep the original Expr in case of error
             <Expr kind="Undefined">
             <Note kind="Report" severity="Error" id="{$error/@id}">
             <xsl:attribute name="subject">
             <xsl:apply-templates select="." mode="report-offender-context"/>
             </xsl:attribute>
             <xsl:copy-of select="$error/text()"/>
             </Note>
             </Expr>
        -->
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          
          <xsl:apply-templates>
            <xsl:with-param name="env">
              <xsl:copy-of select="$new-env/*"/>
            </xsl:with-param>
          </xsl:apply-templates>
          
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

          <xsl:apply-templates select="*[not(self::Note[@kind='exprType'])]">
            <xsl:with-param name="env">
              <xsl:copy-of select="$new-env/*"/>
            </xsl:with-param>
          </xsl:apply-templates>
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
    <xd:short>Evaluate a Decl.</xd:short>
    <xd:detail>If the Decl is not in the environment list, evaluate its children
    and emit the resulting element. If it is in the environment list, and it
    does not contain Undefined Exprs, emit it.
    </xd:detail>
    <xd:param name="env">The environment table.</xd:param>
  </xd:doc>
  <xsl:template match="Decl">
    <xsl:param name="env" select="_default_to_empty_"/>

    <xsl:variable name="name" select="@name"/>
    <xsl:variable name="scope-id" select="Note[@kind='declAnn']/@scope-id"/>
    <xsl:variable name="this" select="$env/Decl[@name=$name][ Note[@kind='declAnn'][@scope-id=$scope-id] ]"/>
    
    <xsl:choose>
      
      <!-- Decl is in the symbol table, but initializer could not be evaluated --> 
      <xsl:when test="$this//Expr[@kind='Undefined']">
        <!-- Emit the element with evaluated children -->
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          <xsl:apply-templates>
            <xsl:with-param name="env">
              <xsl:copy-of select="$env/*"/>
            </xsl:with-param>
          </xsl:apply-templates>
        </xsl:copy>
      </xsl:when>
      
      <!-- Decl is in the symbol table, with an evaluated result -->
      <xsl:when test="$this">
        <xsl:copy-of select="$this"/>
      </xsl:when>
      
      <!-- Decl needs to be added to the symbol table -->          
      <xsl:otherwise>
        <xsl:for-each select="ancestor-or-self::*">
          <xsl:message><xsl:value-of select="@id"/></xsl:message>
        </xsl:for-each>
        <xsl:message terminate="yes">   
          Internal fault - missing Decl (<xsl:value-of select="@name"/>)in environment
        </xsl:message>
      </xsl:otherwise>

    </xsl:choose>
  </xsl:template>

  <xd:doc>
    <xd:short>Constant propagation template for all elements that can scope a Decl (other than Expr)</xd:short>
    <xd:detail>Any Decl child elements in scope are added to the
    environment by a recursive call to the add-symbols template, to account
    for sibling dependencies.
    </xd:detail>
    <xd:param name="env">The environment table.</xd:param>
  </xd:doc>
  <xsl:template match="Actor | Action | Stmt | XDF">
    <xsl:param name="env" select="_default_to_empty_"/>
    
    <!-- Add any Decls that have just come in scope, preserving document order -->
    <xsl:variable name="new-env">
      <xsl:call-template name="new-env">
        <xsl:with-param name="old-env">
          <xsl:copy-of select="$env/*"/>
        </xsl:with-param>
        <xsl:with-param name="new-context" select="."/>
      </xsl:call-template>
    </xsl:variable>
    
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates>
        <xsl:with-param name="env">
          <xsl:copy-of select="$new-env/*"/> 
        </xsl:with-param>      
      </xsl:apply-templates>   
      
    </xsl:copy>
  </xsl:template>
  
  <xd:doc>
    Default constant propagation template for elements that cannot scope a Decl.
    <xd:param name="env">The environment table.</xd:param>
  </xd:doc>
  <xsl:template match="*">
    <xsl:param name="env" select="_default_to_empty_"/>
    
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates>
        <xsl:with-param name="env">
          <xsl:copy-of select="$env/*"/>
        </xsl:with-param>     
      </xsl:apply-templates>   
      
    </xsl:copy>
  </xsl:template>
  
  <xd:doc>
    <xd:short>Named template to construct symbol table.</xd:short>
    <xd:detail>Evaluates a list of declarations and adds them to the environment
    table. The list is processed recursively one element at a
    time because there may be dependencies. The VariableSorter transformation
    guarantees that document-order respects these dependencies.</xd:detail>
    <xd:param name="env">Contains Decls for all the in-scope constants found so far.</xd:param>
    <xd:param name="decls">A list of all the Decls to be evaluated.</xd:param>
  </xd:doc>
  <xsl:template name="add-symbols">
    <xsl:param name="env"/>
    <xsl:param name="decls"/>
    
    <xsl:choose>
      
      <xsl:when test="count($decls/*)=0">
        <!-- When there are no more decls, return the completed symbol table -->
        <xsl:copy-of select="$env/*"/>
      </xsl:when>
      
      <xsl:otherwise>
        
        <xsl:call-template name="add-symbols">
          <xsl:with-param name="env">
            <xsl:copy-of select="$env/*"/>
            <xsl:apply-templates select="$decls/*[1]" mode="add-symbol">
              <xsl:with-param name="env">
                <xsl:copy-of select="$env/*"/>
              </xsl:with-param>
            </xsl:apply-templates>
          </xsl:with-param>
          <xsl:with-param name="decls">
            <xsl:copy-of select="$decls/*[position()>1]"/>
          </xsl:with-param>
        </xsl:call-template> 
      </xsl:otherwise>
      
    </xsl:choose>
  </xsl:template>

  <xd:doc>
    Named template to augment environment with Decls that have just come into scope.
    <xd:param name="old-env">The symbol table</xd:param>
  </xd:doc>
  <xsl:template name="new-env">
    <xsl:param name="old-env"/>
    <xsl:param name="new-context"/>

    <!-- Add Decls one by one to the new environment -->
    <xsl:variable name="new-env">
      <xsl:call-template name="add-symbols">
        <xsl:with-param name="env">
          <xsl:copy-of select="$old-env/*"/>
        </xsl:with-param>
        <xsl:with-param name="decls">
          <!-- Iterate over all elements, adding new Decls. -->
          <xsl:copy-of select="$new-context/Import"/>
          <xsl:for-each select="$new-context/*">
            <xsl:choose>
              <xsl:when test="self::Decl">
                <xsl:copy-of select="."/>
              </xsl:when>
              <xsl:when test="self::Generator | self::Input">
                <xsl:copy-of select="Decl"/>
              </xsl:when>
            </xsl:choose>
          </xsl:for-each>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:variable>

    <!-- Once the environment is augmented, mark all reassignables as unknown -->
    <xsl:for-each select="$new-env/*">
      <xsl:copy>
        <xsl:for-each select="@*">
          <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
        </xsl:for-each>
        
        <xsl:choose>
          <xsl:when test="Note[@kind='declAnn']/@reassigned='yes'">
            <Expr kind="Undefined"/>
          </xsl:when>
          
          <xsl:otherwise>
            <xsl:copy-of select="Expr"/>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:copy-of select="*[ name() != 'Expr' ]"/>
      </xsl:copy>
    </xsl:for-each>
  </xsl:template>

  <!-- ************************************************************
       Moded templates to do evaluations of elements to be included
       in the environment. In this mode, 'Undefined' expressions
       are allowed
       ************************************************************* -->
  <xd:doc>
    Evaluate a Decl for inclusion in the environment.
    <xd:param name="env">The environment table.</xd:param>
  </xd:doc>
  <xsl:template match="Decl" mode="add-symbol">
    <xsl:param name="env" select="_default_to_empty_"/>

    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates mode="add-symbol">
        <xsl:with-param name="env">
          <xsl:copy-of select="$env/*"/>
        </xsl:with-param>
      </xsl:apply-templates>

      <!-- set the variable undefined if it does not have an initializer -->
      <xsl:if test="not( Expr )">
        <Expr kind="Undefined"/>
      </xsl:if>         
    </xsl:copy>
    
  </xsl:template>

  <xd:doc>
    Evaluate an expression as part of a new symbol to be added to the environment.
    <xd:param name="env">The environment table.</xd:param>
  </xd:doc>
  <xsl:template match="Expr" mode="add-symbol">
    <xsl:param name="env" select="_default_to_empty_"/>
    
    <!-- Add any Decls that have just come in scope, preserving document order -->
    <xsl:variable name="new-env">
      <xsl:call-template name="new-env">
        <xsl:with-param name="old-env">
          <xsl:copy-of select="$env/*"/>
        </xsl:with-param>
        <xsl:with-param name="new-context" select="."/>
      </xsl:call-template>
    </xsl:variable>
    
    <xsl:variable name="freeVars" select=".//Note[@kind='freeVar']"/>        
    <xsl:variable name="wrapped-env">
      <env>
        <!-- Filter to relevant decls (ignore shadowing for simplicity but it will still work) -->
        <!-- xsl:copy-of select="$new-env/Import"/>
             <xsl:for-each select="$new-env/Decl">
             <xsl:variable name="name" select="@name"/>
             <xsl:if test="$freeVars[@name = $name]">
             <xsl:copy-of select="."/>
             </xsl:if>
             </xsl:for-each -->
        <xsl:if test="@kind != 'Literal'">
          <xsl:copy-of select="$new-env/*"/>
        </xsl:if>
      </env>
    </xsl:variable>
    
    <xsl:if test="$new-env//Expr[@kind='Application' or @kind='Var']">
      <xsl:message terminate="yes">
        Whoops!
        <xsl:copy-of select="$new-env"/>
      </xsl:message>
    </xsl:if>
    <xsl:copy-of select="cal:evaluateExpr( ., $wrapped-env/env)"/>
    
  </xsl:template>  

  <xd:doc>
    Include other element types (eg Import) in the environment.
    <xd:param name="env">The environment table.</xd:param>
  </xd:doc>
  <xsl:template match="*" mode="add-symbol">
    <xsl:param name="env" select="_default_to_empty_"/>
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates mode="add-symbol">
        <xsl:with-param name="env">
          <xsl:copy-of select="$env/*"/>
        </xsl:with-param>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>
