<!--
	AddInputTypes.xslt
	Xilinx Confidential
	Copyright (c) 2005 Xilinx Inc.
    2005-08-02 DBP   Creation
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  xmlns:math="http://exslt.org/math"
  xmlns:cal="java:com.xilinx.systembuilder.evaluator.CalmlEvaluator"  
  version="2.0">
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Copy the port type into input Decls.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on CannonicalizePorts</li>
      </ul>
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: AddInputTypes.xslt 1009 2005-11-04 04:32:13Z davep $</xd:cvsId>
  </xd:doc>
 
  <xd:doc>
    Copy the Actor port type into Input Decls.
  </xd:doc>
  <xsl:template match="Decl[@kind='Input']">

    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>
      <xsl:if test="not(Type)">
        <xsl:variable name="port" select="../@port"/>
        <xsl:copy-of select="../../../Port[@kind='Input'][@name=$port]/Type"/>
      </xsl:if>
    </xsl:copy>
  </xsl:template>
  
  <xd:doc>
    Default just copies.
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