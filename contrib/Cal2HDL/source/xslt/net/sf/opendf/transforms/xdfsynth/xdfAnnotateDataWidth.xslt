
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator"  
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
            
            <xsl:if test="self::Port[Type]">
                <Note kind="dataWidth" width="{cal:dataWidth(Type)}"/>
            </xsl:if>
            
            <xsl:if test="self::Connection">
            	<xsl:variable name="src" select="@src"/>
            	<xsl:variable name="src-port" select="@src-port"/>
            	<xsl:variable name="dst" select="@dst"/>
            	<xsl:variable name="dst-port" select="@dst-port"/>
            	<xsl:variable name="srcType" select="if ($src = '') then ../Port[@kind='Input'][@name=$src-port]/Type else ../Instance[@id=$src]/Actor/Port[@kind='Output'][@name=$src-port]/Type"/>
            	<xsl:variable name="dstType" select="if ($dst = '') then ../Port[@kind='Output'][@name=$dst-port]/Type else ../Instance[@id=$dst]/Actor/Port[@kind='Input'][@name=$dst-port]/Type"/>
            	<xsl:if test="$srcType">
            		<Note kind="srcWidth" width="{cal:dataWidth($srcType)}"/>
            	</xsl:if>
            	<xsl:if test="$dstType">
	            	<Note kind="dstWidth" width="{cal:dataWidth($dstType)}"/>
	            </xsl:if>
            </xsl:if>
            
            <xsl:apply-templates select="* | text()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>



