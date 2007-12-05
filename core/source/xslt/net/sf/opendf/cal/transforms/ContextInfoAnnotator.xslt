
<!--
    ContextInfoAnnotator

    Adds information from the CAL context to the AST.

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.1"
  xmlns:exsl="http://exslt.org/common"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="exsl set">

  <xsl:output method="xml"/>

    <xsl:param name="context-uri" select="'net/sf/opendf/cal/transforms/Context.xml'"/>
    <xsl:variable name="context" select="document($context-uri)/Context"/>
    <!-- If you have no internet connection, use this line for debugging purposes instead:
        <xsl:param name="context" select="document('../../web/data/platforms/DefaultContext.xml')/Context"/>
    -->

    <xsl:template match="Expr[@kind='UnaryOp']/Op">
        <xsl:variable name="op" select="@name"/>
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()"/>

            <xsl:choose>
            <xsl:when test="$context/UnaryOp[@name=$op]">
                <xsl:attribute name="function"><xsl:value-of select="$context/UnaryOp[@name=$op]/@function"/></xsl:attribute></xsl:when>
            <xsl:otherwise>
                <Note kind="Error" message="Undefined unary operator '{$op}'"/></xsl:otherwise>
            </xsl:choose>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="Expr[@kind='BinOpSeq']/Op">
        <xsl:variable name="op" select="@name"/>
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()"/>

            <xsl:choose>
            <xsl:when test="$context/BinaryOp[@name=$op]">
                <xsl:attribute name="precedence"><xsl:value-of select="$context/BinaryOp[@name=$op]/@precedence"/></xsl:attribute>
                <xsl:attribute name="function"><xsl:value-of select="$context/BinaryOp[@name=$op]/@function"/></xsl:attribute></xsl:when>
            <xsl:otherwise>
                <Note kind="Error" message="Undefined binary operator '{$op}'"/></xsl:otherwise>
            </xsl:choose>
        </xsl:copy>
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
