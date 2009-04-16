<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:hwcache="java:net.sf.opendf.xslt.util.XSLTProcessCallbacks"
    extension-element-prefixes="xsl xd hwcache"
    version="2.0">
    <!--
       This transformation uses a callback to determine if each Instance 
       can be cached for hardware code generation.  If so, the Actor body
       is stripped out, leaving only the ports, declarations, and notes.
       
       This transformation relies on the Note[@kind='UID']/@value generated
       by the addInstanceUID transformation.
    -->  
    
    <xsl:output method="xml"/>

    <xsl:template match="Instance">

        <xsl:variable name="node" select="."/>
        <xsl:variable name="uid" select="./Note[@kind='UID']/@value"/>
        <xsl:variable name="checked" select="hwcache:checkCacheable( $node, $uid )"/>
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:copy-of select="$checked"/>
            <xsl:choose>
                <xsl:when test="$checked/@value='true'">
                    <xsl:apply-templates select="node() | text()" mode="caching"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="node() | text()"/>
                </xsl:otherwise>                    
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
    
    <xsl:template match="Actor" mode="caching">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:apply-templates select="*[self::Port or self::Decl or self::Note]"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="*" mode="caching">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>
    
    
</xsl:stylesheet>    