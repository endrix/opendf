
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    version="2.0">
    
    <xsl:output method="xml"/>
    
    <xd:doc type="stylesheet">
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx, 2006</xd:copyright>
    </xd:doc>
    

	<xsl:template match="Port[../self::XDF][@kind='Input']">
		<xsl:variable name="name" select="@name"/>
		<xsl:variable name="c" select="../Connection[@src = ''][@src-port = $name][1]"/>
		<Port kind="Input" name="{$name}">
			<xsl:if test="$c">
				<xsl:copy-of select="../Instance[@id = $c/@dst]/Actor/Port[@kind='Input'][@name=$c/@dst-port]/Type"/>
			</xsl:if>
		</Port>
	</xsl:template>    
    
	<xsl:template match="Port[../self::XDF][@kind='Output']">
		<xsl:variable name="name" select="@name"/>
		<xsl:variable name="c" select="../Connection[@dst = ''][@dst-port = $name][1]"/>
		<Port kind="Output" name="{$name}">
			<xsl:if test="$c">
				<xsl:copy-of select="../Instance[@id = $c/@src]/Actor/Port[@kind='Output'][@name=$c/@src-port]/Type"/>
			</xsl:if>
		</Port>
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



