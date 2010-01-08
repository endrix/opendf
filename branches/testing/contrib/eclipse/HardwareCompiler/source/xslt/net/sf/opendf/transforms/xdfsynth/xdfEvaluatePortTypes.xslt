<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator"  
    version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:author>IDM</xd:author>
    <xd:copyright>Xilinx, 2007</xd:copyright>
    <xd:detail>Evaluates Expr elements in Actor ports to resolve the types prior to synthesis checks.</xd:detail>
  </xd:doc>

  <xsl:template match="Actor/Port//Expr">

    <xsl:variable name="env">
      <env/>
    </xsl:variable>
    
    <xsl:variable name="eval">
      <xsl:copy-of select="cal:evaluateExpr( ., $env)"/>
    </xsl:variable>

    <xsl:variable name="error" select="$eval//Note[ @kind='Report' ][ @severity='Error' ][1]"/>
    
    <xsl:choose>
      <xsl:when test="$error">
        <xsl:message>Evaluating expr <xsl:value-of select="@name"/>@<xsl:value-of select="@id"/>
        <xsl:for-each select="$eval/*">
          <xsl:value-of select="name()"/>
          <xsl:for-each select="@*">
            <xsl:value-of select="name()"/>=<xsl:value-of select="."/>:
          </xsl:for-each>
        </xsl:for-each>
        </xsl:message>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$eval/Expr"/>
      </xsl:otherwise>
    </xsl:choose>    
  </xsl:template>
  
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}">
          <xsl:value-of select="."/>
        </xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates select="* | text()"/>
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>
