<!--
    AddID

    Adds to each element an "id" attribute with a unique value. This may later be used to establish
    references between nodes, e.g. in the VariableAnnotator transformation.

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="2.0"
  xmlns:exsl="http://exslt.org/common"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="exsl set">

  <xsl:output method="xml"/>

  <xsl:variable name="prefix">
    <xsl:choose>
      <xsl:when test="//Note[@kind='counter']">
        <xsl:value-of select="1 + //Note[@kind='counter']/@value"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="0"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:template match="Note[@kind='counter']">
  </xsl:template>

  <xsl:template match="/*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>

      <!-- DBP add an ID to the top level as well -->
      <xsl:if test="not(name() = 'Note') and not(@id)">
        <xsl:attribute name="id"><xsl:value-of select="concat($prefix, '$id$', generate-id())"/></xsl:attribute>
      </xsl:if>
      
      <Note kind="counter" value="{$prefix}"/>
      <xsl:apply-templates select="node() | text()"/>
    </xsl:copy>    
  </xsl:template>

  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>

      <!-- <xsl:if test="(node-kind(.) = 'element') and not(name() = 'Note') and not(@id)"> -->
      <xsl:if test="not(name() = 'Note') and not(@id)">
        <xsl:attribute name="id"><xsl:value-of select="concat($prefix, '$id$', generate-id())"/></xsl:attribute>
      </xsl:if>

      <xsl:apply-templates select="node() | text()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
