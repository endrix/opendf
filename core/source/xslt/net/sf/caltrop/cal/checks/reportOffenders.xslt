<?xml version="1.0" encoding="UTF-8"?>

<!-- 
    reportOffenders.xslt
    Identifying information for error reporting
-->

<xsl:stylesheet
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   version="2.0" >

<!-- Templates to report offender and context -->
<xsl:template match="Decl" mode="report-offender">
  <xsl:choose>
    <xsl:when test="@kind = 'Variable' and Type[@kind='Procedure' or @kind='Function']">
      <xsl:value-of select="Type/@kind"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="@kind"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text> '</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>' [</xsl:text>
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="Expr" mode="report-offender">
  <xsl:choose>
    <xsl:when test="parent::Expr[@kind='Application']">
      <xsl:text>Function </xsl:text>
    </xsl:when>
    <xsl:when test="parent::Stmt[@kind='Call']">
      <xsl:text>Procedure </xsl:text>
    </xsl:when>
  </xsl:choose>
  <xsl:text>'</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>' [</xsl:text>
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="Stmt[@kind='Assign']" mode="report-offender">
  <xsl:text>Assignment to '</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>' [</xsl:text>
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="*" mode="report-offender">
  <xsl:value-of select="name(.)"/>
  <xsl:text> '</xsl:text>
  <xsl:choose>
    <xsl:when test="@name"><xsl:value-of select="@name"/></xsl:when>
    <xsl:otherwise>?unknown?</xsl:otherwise>
  </xsl:choose>
  <xsl:text>'</xsl:text>
  <xsl:if test="parent::*">
    <xsl:text> [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="XDF" mode="report-offender-context">
  <xsl:text>in network </xsl:text>
  <xsl:choose>
    <xsl:when test="@name">
      <xsl:text>'</xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:text>'</xsl:text>
    </xsl:when>
    <xsl:otherwise><xsl:text>?unknown?</xsl:text></xsl:otherwise>
  </xsl:choose>
  <xsl:if test="parent::*">
    <xsl:text> [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="Instance" mode="report-offender-context">
  <xsl:text>in Instance </xsl:text>
  <xsl:choose>
    <xsl:when test="@id">
      <xsl:text>'</xsl:text>
      <xsl:value-of select="@id"/>
      <xsl:text>'</xsl:text>
    </xsl:when>
    <xsl:otherwise><xsl:text>?unknown?</xsl:text></xsl:otherwise>
  </xsl:choose>
  <xsl:text> of type </xsl:text>
  <xsl:choose>
    <xsl:when test="@instance-name">
      <xsl:text>'</xsl:text>
      <xsl:value-of select="@instance-name"/>
      <xsl:text>'</xsl:text>
    </xsl:when>
    <xsl:otherwise><xsl:text>?unknown?</xsl:text></xsl:otherwise>
  </xsl:choose>
  <xsl:text> [</xsl:text>
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="Actor" mode="report-offender-context">
  <xsl:text>at Actor-scope in '</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>'</xsl:text>
  <xsl:if test="parent::*">
    <xsl:text> [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="Action" mode="report-offender-context">
  <xsl:text>in Action at line </xsl:text><xsl:value-of select="@text-begin-line"/>
  <xsl:text>' [</xsl:text>
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="Decl[@kind='Variable']" mode="report-offender-context">
  <xsl:choose>
    <xsl:when test="Type[@kind='Procedure' or @kind='Function']">
      <xsl:text>in </xsl:text><xsl:value-of select="Type/@kind"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>in initializer for variable</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text> '</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>' [</xsl:text>
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="Input" mode="report-offender-context">
  <xsl:text>in input token for port '</xsl:text>
  <xsl:value-of select="@port"/>
  <xsl:text>' [</xsl:text>
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="Generator" mode="report-offender-context">
  <xsl:text>in Generator </xsl:text>
  <xsl:text> [</xsl:text>
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="*" mode="report-offender-context">
  <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
</xsl:template>

</xsl:stylesheet>