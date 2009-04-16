<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Creates an output port at the top level XDF for every Actor instance output</xd:short>
    <xd:detail>
      Assumes that the XDF has been flattened and that there is only one XDF node in the document.
    </xd:detail>
    <xd:cvsId>$Id:$</xd:cvsId>
    <xd:author>IDM</xd:author>
    <xd:copyright>Xilinx, 2007</xd:copyright>
  </xd:doc>

  <xsl:template match="XDF">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>

      <xsl:for-each select="./Instance/Actor/Port[@kind='Output']">
        <Port kind="Output" name="{../../@instance-name}_{@name}">
          <xsl:copy-of select="./Type"/>
          <xsl:copy-of select="./Note"/> <!-- picks up dataWidth note among others -->
          <xsl:copy-of select="../../Attribute[@name='clockDomain']"/> <!-- pick up clock domain attribute from the instance -->
          <Note kind="fanin" width="1"/>
          <Note kind="internalPort" value="true"/>
        </Port>
      </xsl:for-each>
      
      <xsl:apply-templates/>

      <!-- Create the connections -->
      <xsl:for-each select="./Instance/Actor/Port[@kind='Output']">
        <Connection dst="" dst-port="{../../@instance-name}_{@name}" src="{../../@id}" src-port="{@name}">
          <!-- Set the buffer size to be extremely large -->
          <Attribute kind="Value" name="bufferSize" value="16">
            <Expr kind="Literal" literal-kind="Integer" value="16">
              <Note kind="exprType">
                <Type name="int">
                  <Entry kind="Expr" name="size">
                    <Expr kind="Literal" literal-kind="Integer" value="3"/>
                  </Entry>
                </Type>
              </Note>
            </Expr>
          </Attribute>
        </Connection>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="Actor/Port/Note[@kind='fanout']">
    <Note kind="fanout" width="{@width + 1}"/>
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



