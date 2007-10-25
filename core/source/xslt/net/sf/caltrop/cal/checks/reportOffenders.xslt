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
  
  <xsl:template match="Input | Output" mode="report-offender">
    <xsl:text> '</xsl:text>
    <xsl:value-of select="@port"/>
    <xsl:text>' [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="Expr[@kind='Var']" mode="report-offender">
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

  <xsl:template match="Op" mode="report-offender">
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
  
  <xsl:template match="Action" mode="report-offender">
    <xsl:text>Action at line </xsl:text><xsl:value-of select="@text-begin-line"/>
    <xsl:if test="QID">
      <xsl:text> (QID </xsl:text>
      <xsl:value-of select="QID/@name"/>
      <xsl:text>)</xsl:text>
    </xsl:if>
    <xsl:text> [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="Schedule" mode="report-offender">
    <xsl:value-of select="@kind"/>
    <xsl:text> schedule [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="Connection" mode="report-offender">
    <xsl:variable name="conn" select="."/>
    <xsl:value-of select="$conn/../Instance[@id=$conn/@src]/Actor/@name"/>:<xsl:value-of select="$conn/@src-port"/><xsl:text> to </xsl:text><xsl:value-of select="$conn/../Instance[@id=$conn/@dst]/Actor/@name"/>:<xsl:value-of select="$conn/@dst-port"/><xsl:text> [</xsl:text>
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
    <xsl:text> [</xsl:text>
    <xsl:choose>
      <xsl:when test="Note[@kind='instanceHierarchy']">
        <xsl:for-each select="Note[@kind='instanceHierarchy']/Note[@kind='hierElement']">
          <xsl:value-of select="@value"/><xsl:text>/</xsl:text>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="parent::*">
        <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text> ?unknown?</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>]</xsl:text>
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
    <xsl:choose>
      <xsl:when test="Note[@kind='instanceHierarchy']">
        <xsl:for-each select="Note[@kind='instanceHierarchy']/Note[@kind='hierElement']">
          <xsl:value-of select="@value"/><xsl:text>/</xsl:text>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="parent::*">
        <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text> ?unknown?</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
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
    <xsl:if test="QID">
      <xsl:text> (QID </xsl:text>
      <xsl:value-of select="QID/@name"/>
      <xsl:text>)</xsl:text>
    </xsl:if>
    <xsl:text> [</xsl:text>
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
  
  <xsl:template match="Decl[@kind='Parameter']" mode="report-offender-context">
    <xsl:text>for parameter '</xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>' [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="Input" mode="report-offender-context">
    <xsl:text>reading port '</xsl:text>
    <xsl:value-of select="@port"/>
    <xsl:text>' [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="Output" mode="report-offender-context">
    <xsl:text>writing port '</xsl:text>
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

  <xsl:template match="Schedule" mode="report-offender-context">
    <xsl:text>in </xsl:text>
    <xsl:value-of select="@kind"/>
    <xsl:text> schedule [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="Port" mode="report-offender-context">
    <xsl:text>in declaration of port '</xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>' [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="Type" mode="report-offender-context">
    <xsl:text>in Type '</xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text>' [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="Priority" mode="report-offender-context">
    <xsl:text>in priority relationship [</xsl:text>
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
    <xsl:text>]</xsl:text>
  </xsl:template>
  
  <xsl:template match="*" mode="report-offender-context">
    <xsl:apply-templates select="parent::*" mode="report-offender-context"/>
  </xsl:template>
 
  <xsl:template match="*" mode="annotate-location">
    <xsl:choose>
      <xsl:when test="@text-begin-line or @text-end-line or @text-begin-col or @text-end-col">
        <Note kind="report-location">
          <xsl:if test="@text-begin-line">
            <xsl:attribute name="text-begin-line"><xsl:value-of select="@text-begin-line"/></xsl:attribute>
          </xsl:if>
          <xsl:if test="@text-end-line">
            <xsl:attribute name="text-end-line"><xsl:value-of select="@text-end-line"/></xsl:attribute>
          </xsl:if>
          <xsl:if test="@text-begin-col">
            <xsl:attribute name="text-begin-col"><xsl:value-of select="@text-begin-col"/></xsl:attribute>
          </xsl:if>
          <xsl:if test="@text-end-col">
            <xsl:attribute name="text-end-col"><xsl:value-of select="@text-end-col"/></xsl:attribute>
          </xsl:if>
        </Note>
      </xsl:when>
      <xsl:when test="parent::*">
        <xsl:apply-templates select="parent::*" mode="annotate-location"/>
      </xsl:when>
    </xsl:choose>

  </xsl:template>
  
</xsl:stylesheet>