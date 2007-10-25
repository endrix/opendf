<?xml version="1.0" encoding="UTF-8"?>

<!-- 
    problemSummary.xslt
    Report the number of problems and terminate on error
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
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
       <xsl:message>
        <xsl:text>Error </xsl:text>
        <xsl:value-of select="@id"/><xsl:text>: </xsl:text>
        <xsl:value-of select="normalize-space( text() )"/>
        <xsl:text>: </xsl:text>
        <xsl:value-of select="@subject"/>
        <xsl:value-of select="@detail"/>
      </xsl:message>
    </xsl:for-each>
     
    <xsl:for-each select="$warnings">
      <xsl:message>
        <xsl:text>Warning </xsl:text>
        <xsl:value-of select="@id"/><xsl:text>: </xsl:text>
        <xsl:value-of select="normalize-space( text() )"/>
        <xsl:text>: </xsl:text>
        <xsl:value-of select="@subject"/>
        <xsl:value-of select="@detail"/>
      </xsl:message>
    </xsl:for-each>
    
    <xsl:if test="count( $warnings ) &gt; 0">
      <xsl:message>
        <xsl:value-of select="count( $warnings )"/>
        <xsl:text> warning</xsl:text>
        <xsl:if test="count( $warnings ) &gt; 1">
          <xsl:text>s</xsl:text>
        </xsl:if>
        <xsl:text> reported.</xsl:text>
      </xsl:message>
    </xsl:if>
      
    <xsl:if test="count( $errors ) &gt; 0">
      <xsl:message terminate="yes">
        <xsl:value-of select="count( $errors )"/>
        <xsl:text> error</xsl:text>
        <xsl:if test="count( $errors ) &gt; 1">
          <xsl:text>s</xsl:text>
        </xsl:if>
        <xsl:text> reported.</xsl:text>
      </xsl:message>
    </xsl:if>
    
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