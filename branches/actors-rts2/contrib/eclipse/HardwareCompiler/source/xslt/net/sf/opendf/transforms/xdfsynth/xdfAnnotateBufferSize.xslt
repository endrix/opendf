
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


    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:if test="self::Port[@kind='Input'][../self::Actor]">
            	<xsl:variable name="id" select="../../@id"/>
            	<xsl:variable name="portName" select="@name"/>
            	<Note kind="bufferSize" size="{max(../../../Connection[@dst=$id][@dst-port=$portName]/Attribute[@name='bufferSize']/@value)}"/>
            </xsl:if>
            
            <xsl:apply-templates select="* | text()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>



