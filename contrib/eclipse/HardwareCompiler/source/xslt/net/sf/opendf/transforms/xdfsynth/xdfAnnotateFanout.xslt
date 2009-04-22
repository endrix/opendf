
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    version="2.0">
    
    <xsl:output method="xml"/>
    
    <xd:doc type="stylesheet">
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx, 2006-2007</xd:copyright>
    </xd:doc>


    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:if test="self::Port[../self::Actor][@kind='Output']">
                <xsl:variable name="id" select="../../@id"/>
                <xsl:variable name="port" select="@name"/>
            	<xsl:variable name="fanout" select="count(../../../Connection[@src=$id][@src-port=$port])"/>
          		<Note kind="fanout" width="{$fanout}"/>
            </xsl:if>

            <xsl:if test="self::Port[../self::Actor][@kind='Input']">
                <xsl:variable name="id" select="../../@id"/>
                <xsl:variable name="port" select="@name"/>
                <xsl:variable name="fanin" select="count(../../../Connection[@dst=$id][@dst-port=$port])"/>
                <Note kind="fanin" width="{$fanin}"/>
            </xsl:if>

            <xsl:if test="self::Port[../self::XDF][@kind='Input']">
                <xsl:variable name="port" select="@name"/>
            	<xsl:variable name="fanout" select="count(../Connection[@src=''][@src-port=$port])"/>
           		<Note kind="fanout" width="{$fanout}"/>
            </xsl:if>
            
            <xsl:if test="self::Port[../self::XDF][@kind='Output']">
                <xsl:variable name="port" select="@name"/>
                <xsl:variable name="fanin" select="count(../Connection[@dst=''][@dst-port=$port])"/>
                <Note kind="fanin" width="{$fanin}"/>
            </xsl:if>
                                    
            <xsl:apply-templates select="* | text()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>



