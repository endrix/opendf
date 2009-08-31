
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:opendf="java:net.sf.opendf.util.xml.Util"
    version="2.0">
  
  <!--
      This stylesheet contains utilities for generating HDL from XDF
  -->

  <!-- When no clock domain is specified use this one -->
  <xsl:variable name="defaultClockDomain">CLK</xsl:variable>


  <xsl:template name="findClockDomains">
    <xsl:param name="root" select="_undefined_"/>
    <xsl:variable name="clocks" select=
      "$root//Attribute[@name='clockDomain'][not(some $a in preceding::Attribute[@name='clockDomain']
      satisfies $a/@value = @value )]"/>
    <xsl:for-each select="$clocks">
      <clock name="{@value}" index="{position()}"/>       
    </xsl:for-each>
    
    <!-- Create a default clock domain if
         * there is an actor instance with no clock domain
         * or, there is a port with no clock domain that is the child of
           a network that has no clock domain -->
    <xsl:if test="$root//Actor/parent::Instance[not(Attribute[@name='clockDomain'])]
      or /XDF/Port[not(Attribute[@name='clockDomain'] or ../Attribute[@name='clockDomain'])]">
      <!-- Create the default clock if it does not exist already -->
      <xsl:if test="not(//Attribute[@name='clockDomain'][@value='CLK'])">
        <clock name="{$defaultClockDomain}" index="{count($clocks) + 1}"/>        
      </xsl:if>
    </xsl:if>
  </xsl:template>
  

</xsl:stylesheet>



