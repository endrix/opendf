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
  
   
 
  

    <xsl:template match="Connection[not(@dst='') and not(@src='')]" priority="4000" mode="M1">
      <xsl:variable name="conn" select="."/>

      <xsl:choose>
         <xsl:when test="../Instance[@id=$conn/@dst]/Actor/Port[@name=$conn/@dst-port] or                    ../HDL/Port[@name=$conn/@dst-port]"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="xdfSynthesis.unknownDestinationPort"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Unknown destination port</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
         <xsl:when test="../Instance[@id=$conn/@src]/Actor/Port[@name=$conn/@src-port] or         ../HDL/Port[@name=$conn/@src-port]"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="xdfSynthesis.unknownSourcePort" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Unknown source port</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:variable name="dstType"
                    select="../Instance[@id=$conn/@dst]/Actor/Port[@name=$conn/@dst-port]/Type"/>
      <xsl:variable name="srcType"
                    select="../Instance[@id=$conn/@src]/Actor/Port[@name=$conn/@src-port]/Type"/>
      <xsl:choose>
         <xsl:when test="$srcType/@name = $dstType/@name or                   ../HDL[@id=$conn/@dst]/Port[@name=$conn/@dst-port]"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="xdfSynthesis.typeMismatch" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Mismatched types in network connection</Note>
         </xsl:otherwise>
      </xsl:choose>

      <xsl:variable name="srcSize"
                    select="$srcType/Entry[@name='size']/Expr[@kind='Literal']/@value"/>
      <xsl:variable name="dstSize"
                    select="$dstType/Entry[@name='size']/Expr[@kind='Literal']/@value"/>
      <xsl:choose>
         <xsl:when test="$dstType/@name='bool' or $srcType/@name='bool' or $srcSize = $dstSize or                   ../HDL[@id=$conn/@dst]/Port[@name=$conn/@dst-port]"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="xdfSynthesis.sizeMismatch" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Mismatched sizes in network connection</Note>
         </xsl:otherwise>
      </xsl:choose>

      <xsl:apply-templates mode="M1"/>
   </xsl:template>

    <xsl:template match="Actor/Port" priority="3999" mode="M1">
      <xsl:variable name="portName" select="@name"/>
      <xsl:variable name="instId" select="../../@id"/>
      
      <xsl:choose>
         <xsl:when test="./Type/@name='bool' or ./Type/Entry[@name='size']"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="xdfSynthesis.sizeMissing" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>Port size missing</Note>
         </xsl:otherwise>
      </xsl:choose>
  
            
      <xsl:choose>
         <xsl:when test="@kind='Output' or count(../../../Connection[@dst=$instId][@dst-port=$portName]) &lt; 2"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="xdfSynthesis.fanin.actor" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="parent::*" mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>
               <xsl:text>Actor input port has fan-in:</xsl:text>
               <xsl:value-of select="$portName"/>
            </Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
    
    
    <xsl:template match="XDF/Port[@kind='Output']" priority="3998" mode="M1">
      <xsl:variable name="portName" select="@name"/>
      
      <xsl:choose>
         <xsl:when test="count(../Connection[@dst=''][@dst-port=$portName]) &lt; 2"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="xdfSynthesis.fanin.network" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="parent::*" mode="report-offender"/>
               </xsl:attribute>
               <xsl:apply-templates select="." mode="annotate-location"/>
               <xsl:text>Network output port has fan-in:</xsl:text>
               <xsl:value-of select="$portName"/>
            </Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
  <xsl:template match="text()" priority="-1" mode="M1"/>
  
   <xsl:template match="text()" priority="-1"/>
</xsl:stylesheet>