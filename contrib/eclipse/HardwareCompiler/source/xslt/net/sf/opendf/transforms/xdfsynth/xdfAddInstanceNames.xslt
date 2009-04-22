
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    xmlns:sb="java:net.sf.opendf.xslt.util.XSLTProcessCallbacks"  
    version="2.0">
    
    <xsl:output method="xml"/>
    
    <xd:doc type="stylesheet">
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx, 2006</xd:copyright>
    </xd:doc>


    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:if test="self::Instance">
                <xsl:attribute name="instance-name">
                    <xsl:value-of select="./Note[@kind='UID']/@value"/>
<!--                    <xsl:value-of select="./Class/@name"/>_<xsl:value-of select="sb:hexify(@id)"/>-->
                </xsl:attribute>
            </xsl:if>


            <xsl:apply-templates select="* | text()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>



