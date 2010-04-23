<!--
    DDL -> XDF
    
    Translates a DDL network to an XDF network by replacing the top-level
    Network element with an XDF element.

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="2.0"
  xmlns:exsl="http://exslt.org/common"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="exsl set">
  
  <xsl:output method="xml"/>
  
  <xsl:template match="Network">
    <XDF>
      <xsl:apply-templates/>
    </XDF>
  </xsl:template>
  
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>
      
      <xsl:apply-templates select="node() | text()"/>
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>
