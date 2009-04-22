
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:opendf="java:net.sf.opendf.util.xml.Util"
    version="2.0">
  
  <!--
      This stylesheet parses a partname attribute and returns a PART element with
      arch, part, package, and speed child elements.
  -->
  <xsl:template name="captureAndParsePartName">
    <xsl:param name="xdf"/>
    <xsl:choose>
      <xsl:when test="$xdf[@partname]">
        <xsl:call-template name="parsePartName">
          <xsl:with-param name="partname" select="$xdf/@partname"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$xdf/Attribute[@name='partname']">
        <xsl:call-template name="parsePartName">
          <xsl:with-param name="partname" select="$xdf/Attribute[@name='partname']/@value"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="parsePartName"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


<xsl:template name="parsePartName">
  <xsl:param name="partname" select="'xc2vp30-7-ff1152'"/>

  <xsl:variable name="testPart" select="upper-case($partname)"/>

  <xsl:variable name="arch">
    <xsl:choose>
      <xsl:when test="starts-with($testPart, 'XC5V')">
        VIRTEX5
      </xsl:when>
      <xsl:when test="starts-with($testPart, 'XC4V')">
        VIRTEX4
      </xsl:when>
      <xsl:when test="starts-with($testPart, 'XC2VP')">
        VIRTEX2P
      </xsl:when>
      <xsl:when test="starts-with($testPart, 'XC2V')">
        VIRTEX2P
      </xsl:when>
      <xsl:when test="starts-with($testPart, 'XCV')">
        VIRTEX
      </xsl:when>
      <xsl:when test="starts-with($testPart, 'XC2S')">
        SPARTAN2
      </xsl:when>
      <xsl:when test="starts-with($testPart, 'XC3S')">
        SPARTAN3
      </xsl:when>
      <xsl:otherwise>
        UNKNOWN
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="part">
    <xsl:value-of select="substring-before($testPart, '-')"/>
  </xsl:variable>

  <xsl:variable name="testPackage" select="substring-after($testPart, '-')"/>

  <xsl:variable name="speed">
    <xsl:value-of select="substring-before($testPackage, '-')"/>
  </xsl:variable>
  
  <xsl:variable name="package">
    <xsl:value-of select="substring-after($testPackage, '-')"/>
  </xsl:variable>

  <!-- normalize space gets rid of leading and trailing whitespace.
       Necessary for the architecture because of the formatting above.
  -->
  <PART>
    <arch value="{normalize-space($arch)}"/>
    <part value="{normalize-space($part)}"/>
    <package value="{normalize-space($package)}"/>
    <speed value="-{normalize-space($speed)}"/>
  </PART>
</xsl:template>

</xsl:stylesheet>



