<?xml version="1.0" encoding="UTF-8"?>

<!-- 
    problemSummary.xslt
    Report the number of problems and terminate on error
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="2.0" >
  
  <xsl:template match="*">
    
    <xsl:copy-of select="."/>
    
    <xsl:variable name="warnings" select="count( //Note[@kind='Report'][@severity='Warning'] )"/>
    <xsl:variable name="errors" select="count( //Note[@kind='Report'][@severity='Errors'] )"/>
 
    <xsl:if test="$warnings &gt; 0">
      <xsl:message>
        <xsl:value-of select="$warnings"/>
        <xsl:text> warning</xsl:text>
          <xsl:if test="$warnings &gt; 1">
          <xsl:text>s</xsl:text>
        </xsl:if>
      </xsl:message>
    </xsl:if>
      
    <xsl:if test="$errors &gt; 0">
      <xsl:message terminate="yes">
        <xsl:value-of select="$errors"/>
        <xsl:text> warning</xsl:text>
          <xsl:if test="$errors &gt; 1">
            <xsl:text>s</xsl:text>
          </xsl:if>
      </xsl:message>
    </xsl:if>

  </xsl:template>
  
</xsl:stylesheet>