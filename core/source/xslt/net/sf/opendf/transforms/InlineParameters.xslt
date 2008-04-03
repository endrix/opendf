
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    
    xmlns:loading="java:net.sf.opendf.util.Loading"  
    xmlns:xnlext="java:net.sf.opendf.xslt.nl.Elaborating"  
    xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator"  

    extension-element-prefixes="xsl xd loading xnlext cal"
  
    version="2.0">
    
    <xsl:output method="xml"/>

    <xsl:param name="actorParameters"><Parameters/></xsl:param>
    
    <xd:doc type="stylesheet">
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx, 2007</xd:copyright>
    </xd:doc>
    
    <xsl:template match="/XDF | /Actor | /Network">
        <xsl:variable name="top" select="."/>
        <xsl:element name="{name()}">
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:for-each select="*">
                <xsl:choose>
                    <xsl:when test="self::Decl[@kind='Parameter']">
                    </xsl:when>
                    <xsl:when test="self::Decl[@kind='Param']">
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <xsl:for-each select="$actorParameters">
                <xsl:variable name="v" select="@name"/>
                <Decl kind="Variable" name="{@name}">
                    <xsl:copy-of select="cal:parseExpression(@value)"/>
                    <xsl:copy-of select="$top/Decl[@kind='Param'][@name=$v]/Type"/>
                    <xsl:copy-of select="$top/Decl[@kind='Parameter'][@name=$v]/Type"/>
                </Decl>
            </xsl:for-each>
        </xsl:element>
    </xsl:template>
    
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



