<!--
    ReplaceIfExpr

    Replaces an expression of the form
        if <e1> then <e2> else <e3> end
    with an expression of the form
        $IfFunction (<e1>, lambda () \-\-\> <t1> : <e1> end,
                           lambda () \-\-\> <t2> : <e2> end)

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">
  <xsl:output method="xml"/>

    <xsl:template match="Expr[@kind='If']">
        <Expr kind="Application">
            <Expr kind="Var" name="$IfFunction" old="false"/>
            <Args>
                <xsl:apply-templates select="Expr[1]"/>
                <Expr kind="Lambda">
                    <Type name="XYZ"/>
                    <xsl:apply-templates select="Expr[2]"/>
                </Expr>
                <Expr kind="Lambda">
                    <Type name="XYZ"/>
                    <xsl:apply-templates select="Expr[3]"/>
                </Expr>
            </Args>
        </Expr>
    </xsl:template>

    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
