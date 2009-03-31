<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator"
                xmlns:sch="http://www.ascc.net/xml/schematron"
                version="2.0">
   <xsl:output method="xml" indent="yes"/>
   <xsl:include href="net/sf/opendf/cal/checks/reportOffenders.xslt"/>
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
   </xsl:template>
  
   
 
  
    
    <xsl:template match="Type[ not( ancestor::Note ) ]" priority="4000" mode="M1">
      
      
      <xsl:choose>
         <xsl:when test="contains(' list int bool ', concat( ' ', @name, ' ' ) )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.types.unsupported" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Type unsupported for hardware synthesis</Note>
         </xsl:otherwise>
      </xsl:choose>

      
      <xsl:choose>
         <xsl:when test="not( @name = 'int' or @name = 'list' ) or Entry[@kind='Expr'][@name='size']"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.types.missingSize" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Type '<xsl:value-of select="@name"/>' must have a 'size' attribute for hardware synthesis</Note>
         </xsl:otherwise>
      </xsl:choose>

      
      <xsl:choose>
         <xsl:when test="not( @name = 'list' ) or Entry[@kind='Type'][@name='type']"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.types.missingType" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Type '<xsl:value-of select="@name"/>' must have a Type attribute for hardware synthesis</Note>
         </xsl:otherwise>
      </xsl:choose>
            
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
    
    <xsl:template match="Entry[ not( ancestor::Note ) ][@kind='Expr'][@name='size']"
                 priority="3999"
                 mode="M1">
      
      <xsl:choose>
         <xsl:when test="Expr[ @kind='Literal' ][ @literal-kind='Integer' ]"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.typeAttributes.unresolved"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>the value of Type attribute '<xsl:value-of select="@name"/>' could not be resolved at compile time</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
 
       
    <xsl:template match="Decl" priority="3998" mode="M1">
      
      
      <xsl:choose>
         <xsl:when test="Type"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.variable.missingType" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>variable must have a type for hardware synthesis</Note>
         </xsl:otherwise>
      </xsl:choose>

      
      <xsl:choose>
         <xsl:when test="parent::Actor or parent::Network or Type/@name != 'list'"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.variable.localList" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>local variables of type 'list' not supported for hardware synthesis</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
    <xsl:template match="Expr[ parent::Decl[ parent::Actor or parent::Network ] ]"
                 priority="3997"
                 mode="M1">
      
      
      
      <xsl:choose>
         <xsl:when test="../Type[ @name = 'list' ] or @kind = 'Literal'"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.initializer.unresolved" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="parent::*" mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>initializer could not be resolved at compile time</Note>
         </xsl:otherwise>
      </xsl:choose>

      
      <xsl:variable name="checkResults"
                    select="cal:checkTypes( ../Type, Note[@kind='exprType']/Type )"/>
      <xsl:choose>
         <xsl:when test="../Type[ @name != 'list' ] or          $checkResults/Okay/@identical = 'true'"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.initializer.incompatible"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="parent::*" mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>list initializer is incompatible with its type declaration<xsl:value-of select="$checkResults/@msg"/>
            </Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
    <xsl:template match="Expr[ ancestor::Guards ]" priority="3996" mode="M1">
      
      
      <xsl:choose>
         <xsl:when test="not(@kind='Indexer')"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="synthesis.guardExpressions.indexerInGuard"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="parent::*" mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Indexer is not allowed in a guard statement</Note>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
  <xsl:template match="text()" priority="-1" mode="M1"/>
  
   <xsl:template match="text()" priority="-1"/>
</xsl:stylesheet>