<!--
    CanonicalizePortTags

    Adds a name tag to each input/output pattern that does not have one.
    It does so by positional matching with the actor's port declarations.

    author: JWJ

    ::  12-08-2002: CalML 1.0
    
    2005-11-03 DBP - Up XSLT version to 1.1
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:calext="caltrop.dom.xalan.Extension"
  version="1.1">
  <xsl:output method="xml"/>


    <xsl:template match="Input[not(@port)]">
        <xsl:variable name="pos" select="count(preceding-sibling::Input)+1"/>
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:attribute name="port"><xsl:value-of select="ancestor::Actor[1]/Port[@kind='Input'][$pos]/@name"/></xsl:attribute>

            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="Output[not(@port)]">
        <xsl:variable name="pos" select="count(preceding-sibling::Output)+1"/>
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:attribute name="port"><xsl:value-of select="ancestor::Actor[1]/Port[@kind='Output'][$pos]/@name"/></xsl:attribute>

            <xsl:apply-templates select="node() | text()"/>
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


