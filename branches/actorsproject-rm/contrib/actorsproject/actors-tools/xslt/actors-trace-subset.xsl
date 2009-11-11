<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" indent="yes"/>

<xsl:param name="step"/>

<xsl:template match="execution-trace">
  <execution-trace>
    <xsl:apply-templates select="network"/>
    <xsl:apply-templates select="trace"/>
  </execution-trace>
</xsl:template>

<xsl:template match="network">
  <xsl:copy-of select="."/>
</xsl:template>

<xsl:template match="trace">
  <xsl:if test="@step &lt;= $step">
    <xsl:copy-of select="."/>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
