
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:callback="java:net.sf.opendf.xslt.util.XSLTProcessCallbacks"  
    version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:detail>
      Each Actor element is written to the Actors/ directory in a unique CALML filename based on the
      instance name attribute of the enclosing Instance element.  The Actor element is retained in the
      XDF along with its Port elements, but the remaining contents are omitted.
    </xd:detail>
    <xd:author>Ian Miller, Jorn Janneck</xd:author>
    <xd:copyright>Xilinx, 2006-2007</xd:copyright>
  </xd:doc>

  <xsl:template match="Actor[../Note[@kind='sourceLoaded' and @value='true']]">

    <xsl:copy>
      <xsl:attribute name="name"><xsl:value-of select="../@instance-name"/></xsl:attribute>
      <xsl:apply-templates select="Port | Note"/>
    </xsl:copy>

    <!-- <xsl:message>Instantiating actor to Actors/<xsl:value-of select="ancestor::Instance/@instance-name"/>.calml</xsl:message> -->
      <xsl:variable name="instance">
      <xsl:copy>
        <xsl:for-each select="@*">
          <xsl:if test="not(name() = 'name')">
            <xsl:attribute name="{name()}">
              <xsl:value-of select="."/>
            </xsl:attribute>
          </xsl:if>
        </xsl:for-each>
        <!-- Change the name of the Actor to be the particular instance name -->
        <xsl:attribute name="name"><xsl:value-of select="../@instance-name"/></xsl:attribute>

        <!-- Bring across any Directive notes -->
        <xsl:for-each select="../Note[@kind='Directive' or @kind='hwcached']">
          <xsl:copy-of select="."/>
        </xsl:for-each>
        
        <xsl:apply-templates select="* | text()"/>
      </xsl:copy>
      </xsl:variable>
      
      <xsl:apply-templates select="callback:instantiateActor($instance, ancestor::Instance/@instance-name)"/>
    
  </xsl:template>


  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}">
          <xsl:value-of select="."/>
        </xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>



