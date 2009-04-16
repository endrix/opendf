
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Converts Attribute elements to compliant forms</xd:short>
    <xd:detail>
      For any Attribute element with a single child Expr where that Expr is a literal with kind
      of String or Integer the value attribute of that literal will be copied to an attribute of
      the Attribute element.
    </xd:detail>
    <xd:cvsId>$Id:$</xd:cvsId>
    <xd:author>IDM</xd:author>
    <xd:copyright>Xilinx, 2007</xd:copyright>
  </xd:doc>

  <xsl:template match="Attribute">
    <!-- Pick up all child Expr literals except those in Type expressions.  -->
    <xsl:variable name="childExprs" select=".//Expr[@kind='Literal'][@literal-kind='Integer' or @literal-kind='String'][not(ancestor::Type)]"/>
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}">
          <xsl:value-of select="."/>
        </xsl:attribute>
      </xsl:for-each>
      
      <xsl:if test="count($childExprs)=1">
        <xsl:attribute name="value">
          <xsl:value-of select="$childExprs[1]/@value"/>
        </xsl:attribute>
      </xsl:if>
      
      <xsl:apply-templates select="* | text()"/>
    </xsl:copy>
  </xsl:template>


  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
  
</xsl:stylesheet>



