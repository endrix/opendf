<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cal="java:com.xilinx.systembuilder.evaluator.CalmlEvaluator"
                xmlns:sch="http://www.ascc.net/xml/schematron"
                version="2.0">
   <xsl:output method="xml" indent="yes"/>
   <xsl:include href="net/sf/caltrop/cal/checks/reportOffenders.xslt"/>
   <xsl:template match="*|@*" mode="schematron-get-full-path">
      <xsl:apply-templates select="parent::*" mode="schematron-get-full-path"/>
      <xsl:text>/</xsl:text>
      <xsl:if test="count(. | ../@*) = count(../@*)">@</xsl:if>
      <xsl:value-of select="name()"/>
      <xsl:text>[</xsl:text>
      <xsl:value-of select="1+count(preceding-sibling::*[name()=name(current())])"/>
      <xsl:text>]</xsl:text>
   </xsl:template>
   <xsl:template match="/">
      <xsl:variable name="contents">
         <xsl:apply-templates select="/" mode="M1"/>
      </xsl:variable>
      <xsl:for-each select="/*">
         <xsl:copy>
            <xsl:for-each select="@*">
               <xsl:attribute name="{name()}">
                  <xsl:value-of select="."/>
               </xsl:attribute>
            </xsl:for-each>
            <xsl:copy-of select="$contents"/>
            <xsl:copy-of select="*"/>
         </xsl:copy>
      </xsl:for-each>
      <xsl:for-each select="$contents/Note">
         <xsl:variable name="body">
            <xsl:value-of select="text()"/>
         </xsl:variable>
         <xsl:message>
            <xsl:value-of select="@severity"/>
            <xsl:text>: </xsl:text>
            <xsl:value-of select="normalize-space($body)"/>
            <xsl:text> [id </xsl:text>
            <xsl:value-of select="@id"/>
            <xsl:text>]</xsl:text>
         </xsl:message>
         <xsl:message>
            <xsl:text/>
            <xsl:value-of select="@subject"/>
         </xsl:message>
         <xsl:message/>
      </xsl:for-each>
      <xsl:if test="$contents/Note[@severity='Warning']">
         <xsl:message>
            <xsl:value-of select="count( $contents/Note[@severity='Warning'] )"/>
            <xsl:text> warning</xsl:text>
            <xsl:if test="count( $contents/Note[@severity='Warning'] ) &gt; 1">
               <xsl:text>s</xsl:text>
            </xsl:if>
         </xsl:message>
      </xsl:if>
      <xsl:if test="$contents/Note[@severity='Error']">
         <xsl:message terminate="yes">
            <xsl:value-of select="count( $contents/Note[@severity='Error'] )"/>
            <xsl:text> error</xsl:text>
            <xsl:if test="count( $contents/Note[@severity='Error'] ) &gt; 1">
               <xsl:text>s</xsl:text>
            </xsl:if>
            <xsl:text> [processing terminated]
</xsl:text>
         </xsl:message>
      </xsl:if>
   </xsl:template>







  <xsl:template match="Decl" priority="4000" mode="M1">

    
      <xsl:choose>
         <xsl:when test="not( some $decl in           ( parent::Generator/preceding-sibling::* |             parent::Input/preceding-sibling::* |             preceding-sibling::* )               /(self::Decl | self::Input/Decl | self::Generator/Decl)             satisfies $decl/@name = current()/@name )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.declaration.duplicate"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Duplicate declarations in the same scope</Note>
         </xsl:otherwise>
      </xsl:choose>

    
      <xsl:if test="some $decl in (           ancestor::*[ position() &gt; (if (current()/parent::Input or current()/parent::Generator) then 2 else 1) ] )           /(Decl | Input/Decl | Generator/Decl)             satisfies $decl/@name = current()/@name">
         <Note kind="Report" severity="Warning" id="variableChecks.declaration.shadows"
               subject="">
            <xsl:attribute name="subject">
               <xsl:apply-templates select="." mode="report-offender"/>
            </xsl:attribute>A declaration shadows another declaration in a containing scope</Note>
      </xsl:if>
            
      <xsl:apply-templates mode="M1"/>
   </xsl:template>

  

  
  
  

  <xsl:template match="Stmt[@kind='Assign']" priority="3996" mode="M1">
    
      <xsl:variable name="env">
         <Env>
            <xsl:copy-of select="ancestor::Actor[1]/Import"/>
         </Env>
      </xsl:variable>
      <xsl:choose>
         <xsl:when test="(some $decl in ancestor::*/(Decl | Input/Decl | Generator/Decl)               satisfies $decl/@name = current()/@name )               or cal:isDefined(@name, $env/Env) "/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.name.undefined" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Undefined variable reference</Note>
         </xsl:otherwise>
      </xsl:choose>
  
      <xsl:apply-templates mode="M1"/>
   </xsl:template>

  <xsl:template match="Expr[@kind='Var']" priority="3995" mode="M1">
    
      <xsl:variable name="env">
         <Env>
            <xsl:copy-of select="ancestor::Actor[1]/Import"/>
         </Env>
      </xsl:variable>
      <xsl:choose>
         <xsl:when test="(some $decl in ancestor::*/(Decl | Input/Decl | Generator/Decl)               satisfies $decl/@name = current()/@name )               or cal:isDefined(@name, $env/Env) "/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.name.undefined" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Undefined variable reference</Note>
         </xsl:otherwise>
      </xsl:choose>
  
    
      <xsl:if test="some $decl in ancestor::Decl satisfies ( $decl/@name = current/@name              and ($decl/Type/@kind='Procedure' or $decl/Type/@kind='Function') )">
         <Note kind="Report" severity="Warning" id="variableChecks.recursion" subject="">
            <xsl:attribute name="subject">
               <xsl:apply-templates select="." mode="report-offender"/>
            </xsl:attribute>Function or procedure recursion detected</Note>
      </xsl:if>
  
    
      <xsl:choose>
         <xsl:when test="not( some $decl in ancestor::Decl satisfies ( $decl/@name = current/@name              and not($decl/Type/@kind='Procedure') and not($decl/Type/@kind='Function') ) )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.selfReference" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Self reference in a variable declaration</Note>
         </xsl:otherwise>
      </xsl:choose>
  
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
   <xsl:template match="text()" priority="-1" mode="M1"/>

   <xsl:template match="text()" priority="-1"/>
</xsl:stylesheet>