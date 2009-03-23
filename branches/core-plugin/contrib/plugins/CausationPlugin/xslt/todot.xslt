<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema" version="2.0">
    
    <!-- .dot file is text file -->
    <xsl:output method="text" /> 
    
    <xsl:key name="steps" use="resolve-QName(concat('a',@ID),.)" match="causation-trace/step"/>
    
    <xsl:template match="causation-trace">
        <xsl:call-template name="startFile"/>
        <xsl:apply-templates select="step">
          
        </xsl:apply-templates>
        <xsl:call-template name="endFile"/>
    </xsl:template>

    <xsl:template match="step" mode="print">
        <xsl:value-of select="@actor-name"/>
        <xsl:text>:</xsl:text>
        <xsl:value-of select="@ID"/>
    </xsl:template>

    <xsl:template match="step">
        <xsl:apply-templates select="dependency">
            <xsl:with-param name="destination">
                <xsl:apply-templates select="." mode="print"/>
            </xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="dependency">
        <xsl:param name="destination"/>
        <xsl:text>"</xsl:text>
        <xsl:apply-templates select="key('steps',resolve-QName(concat('a',@source),.))" mode="print"/>
        <xsl:text>" -> "</xsl:text>
        <xsl:value-of select="$destination"/>
        <xsl:text>"[label="</xsl:text>
        <xsl:value-of select="@port"/>
        <xsl:text>"];
        </xsl:text>
    </xsl:template>
    
    <!-- Print the start of the file -->
    <xsl:template name="startFile">
        <xsl:text>digraph G { 
        </xsl:text>
    </xsl:template>
    
    <!-- Print the end of the file -->
    <xsl:template name="endFile">
        <xsl:text>} 
        </xsl:text>
    </xsl:template>
    
</xsl:stylesheet>