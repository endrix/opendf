
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    
    extension-element-prefixes="xsl xd"
    
    version="2.0">
    
    <xsl:output method="xml"/>

    <xsl:template match="Instance">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            <xsl:variable name="cname" select="./Note[@kind='className'][1]/@value"/>
            <Note kind="UID" value="{concat($cname,'_',count(preceding::Instance/Note[@kind='className' and @value=$cname]))}"/>
            
            <xsl:apply-templates select="* | text()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="Note[@kind='UID']"/>
    
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