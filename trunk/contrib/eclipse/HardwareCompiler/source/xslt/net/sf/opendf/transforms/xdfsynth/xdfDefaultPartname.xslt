<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    
    extension-element-prefixes="xsl xd"

    version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Copies the XDF level partname Attribute elements to each Actor instance
    in a compliant form</xd:short>
    <xd:detail>
      Copy the top-level XDF partname attribute to every Actor instance using the
      correct xmlElement attribute form to ensure that it makes it through to the
      backend processes.
    </xd:detail>
    <xd:cvsId>$Id:$</xd:cvsId>
    <xd:author>IDM</xd:author>
    <xd:copyright>Xilinx, 2007</xd:copyright>
  </xd:doc>

  <xsl:template match="/XDF">
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          <xsl:choose>
            <xsl:when test="./Attribute[@name='partname']">
              <Note context="Actor" kind="Directive" name="xlim_tag">
                <config_option name="project.xflow.xilinx_part" value="{./Attribute[@name='partname']/Expr[@kind='Literal' and @literal-kind='String']/@value}"/>
              </Note>
            </xsl:when>
            <xsl:otherwise>
              <Note context="Actor" kind="Directive" name="xlim_tag">
                <config_option name="project.xflow.xilinx_part" value="xc2vp30-7-ff1152"/>
              </Note>
            </xsl:otherwise>
          </xsl:choose>
          <!--
          <xsl:if test="not( ./Attribute[@name='partname'] )">
            <Attribute kind="Value" name="partname" value="xc2vp30-7-ff1152">
            <Expr kind="Literal" literal-kind="String" value="xc2vp30-7-ff1152">
                <Note kind="exprType">
                  <Type name="string"/>
                </Note>
              </Expr>
            </Attribute>
          </xsl:if>
          -->
          <xsl:apply-templates/>
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



