<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    
    extension-element-prefixes="xsl xd"
  
    version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Copies the XDF level partname Attribute elements to each Actor instance in a compliant form</xd:short>
    <xd:detail>
      Copy the top-level XDF partname attribute to every Actor instance using the correct xmlElement
      attribute form to ensure that it makes it through to the backend processes.
    </xd:detail>
    <xd:cvsId>$Id:$</xd:cvsId>
    <xd:author>IDM</xd:author>
    <xd:copyright>Xilinx, 2007</xd:copyright>
  </xd:doc>

  <xsl:template match="Actor">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <!-- Copy all xlim_tag notes into the Actor from any containing context. 
           It would be nice to uniquify the list, but the xlim_tag is a means of 
           passing anything into the resulting xlim.
       -->
      <xsl:for-each select="ancestor-or-self::*/Note[@kind='Directive' and @context='Actor' and @name='xlim_tag']">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="XXInstance/Actor[not(./Note[@name='xlim_tag']/config_option[@name='project.xflow.xilinx_part'])]">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <!-- copy the Attribute of the nearest partname Attribute -->
      <xsl:variable name="partnames" select="ancestor-or-self::*/Attribute[@name='partname']"/>
      
      <Note context="Actor" kind="Directive" name="xlim_tag">
        <config_option name="project.xflow.xilinx_part">
          <!-- <Expr kind="Literal" literal-kind="String" value="{/XDF/Attribute[@name='partname']/Expr[@kind='Literal' and @literal-kind='String']/@value}"> -->
          <!-- Is this a bug?  ancestor-or-last should return in reverse order but it appears to be forward order.  Change last() to 1 if this is fixed -->
          <Expr kind="Literal" literal-kind="String" value="{$partnames[last()]/Expr[@kind='Literal' and @literal-kind='String']/@value}">
            <Note kind="exprType">
              <Type name="string"/>
            </Note>
          </Expr>
        </config_option>
      </Note>
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



