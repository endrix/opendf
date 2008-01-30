<?xml version="1.0" encoding="UTF-8"?>

<!-- 
    problemSummary.xslt
    Report the number of problems and terminate on error
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:callback="java:net.sf.opendf.xslt.util.XSLTProcessCallbacks"  
  version="2.0" >
  
  <xsl:template match="/*">

    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>
      <xsl:apply-templates/>
    </xsl:copy>
        
    <xsl:variable name="warnings" select="//Note[@kind='Report'][@severity='Warning'][ not( @reported ) ]"/>
    <xsl:variable name="errors" select="//Note[@kind='Report'][@severity='Error'][ not( @reported ) ]"/>

    <xsl:for-each select="$errors">
      <xsl:variable name="message">
        <xsl:text>Error: </xsl:text>
        <xsl:for-each select="text()">
          <xsl:value-of select="normalize-space()"/>
        </xsl:for-each>
        <xsl:text>: </xsl:text>
        <xsl:value-of select="@subject"/>
        <xsl:value-of select="@detail"/>
      </xsl:variable>
      <xsl:apply-templates select="callback:reportProblem(., $message)"/>
    </xsl:for-each>
     
    <xsl:for-each select="$warnings">
      <xsl:variable name="severity" select="@severity"/>
      <xsl:variable name="message">
        <xsl:text>Warning </xsl:text>
        <xsl:for-each select="text()">
          <xsl:value-of select="normalize-space()"/>
        </xsl:for-each>
        <xsl:text>: </xsl:text>
        <xsl:value-of select="@subject"/>
        <xsl:value-of select="@detail"/>
      </xsl:variable>
      <xsl:apply-templates select="callback:reportProblem(., $message)"/>
    </xsl:for-each>
    
  </xsl:template>
  
  <!-- Flag as reported -->  
  <xsl:template match="Note[@kind='Report']">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>
      <xsl:attribute name="reported">yes</xsl:attribute>
    
      <xsl:apply-templates/>    
    </xsl:copy>
  </xsl:template>
 
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>
      
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
   
</xsl:stylesheet>