<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  extension-element-prefixes="xsl"
  version="1.0">
  <xsl:output method="xml"/>

    <xsl:template match="Stmt[@kind='If']">
        <Expr kind="Application">
            <Expr kind="Var" name="$IfProcedure" old="false"/>
            <Expr kind="Tuple">
                <xsl:apply-templates select="Expr"/>
                <Expr kind="Proc">
                    <xsl:apply-templates select="Stmt[1]"/>
                </Expr>
                <Expr kind="Proc">
                    <xsl:apply-templates select="Stmt[2]"/>
                </Expr>
            </Expr>
        </Expr>
    </xsl:template>

    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:apply-templates select="*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
