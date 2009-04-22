
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator"  
    xmlns:calcb="java:net.sf.opendf.xslt.util.XSLTProcessCallbacks"  
    version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:author>JWJ</xd:author>
    <xd:copyright>Xilinx, 2006</xd:copyright>
  </xd:doc>
  

  <xsl:template match="Connection">
    <xsl:variable name="c" select="."/>
    <xsl:variable name="ts" 
		  select="(if ($c/@src = '') 
			  then ../Port[@kind='Input'][@name=$c/@src-port]
			  else ../Instance[@id=$c/@src]/Actor/Port[@kind='Output'][@name=$c/@src-port]
			  )/Type"/>
    <xsl:variable name="td" 
		  select="(if ($c/@dst = '') 
			  then ../Port[@kind='Output'][@name=$c/@dst-port]
			  else ../Instance[@id=$c/@dst]/Actor/Port[@kind='Input'][@name=$c/@dst-port]
			  )/Type"/>
    <xsl:if test="not($ts)">
      <xsl:variable name="msg" select="concat('Missing type for connection source.  Instance:',//Instance[@id=$c/@src]/Class/@name,' Port:',$c/@src-port)"/>
      <xsl:variable name="errTmp" select="calcb:reportProblem($c,$msg)"/>
      <xsl:message><xsl:value-of select="$msg"/></xsl:message>
    </xsl:if>
    <xsl:if test="not($td)">
      <xsl:variable name="msg" select="concat('Missing type for connection destination.  Instance:',//Instance[@id=$c/@dst]/Class/@name,' Port:',$c/@dst-port)"/>
      <xsl:variable name="errTmp" select="calcb:reportProblem($c,$msg)"/>
      <xsl:message><xsl:value-of select="$msg"/></xsl:message>
    </xsl:if>
    <!-- <xsl:message>checking types <xsl:copy-of select="$c"/></xsl:message> -->
    <xsl:if test="count($ts) = 0">
      <xsl:variable name="where" select="concat('Connection has no source attributes',//Instance[@id=$c/@src]/Class/@name)"/>
      <xsl:variable name="err"><Note kind="Report" severity="ERROR" id="synthesis.typecheck.source"/></xsl:variable>
      <xsl:variable name="errTmp" select="calcb:reportProblem($err,$where)"/>
    </xsl:if>
    <xsl:if test="count($td) = 0">
      <xsl:variable name="where" select="concat('Connection has no target attributes',//Instance[@id=$c/@dst]/Class/@name)"/>
      <xsl:variable name="err"><Note kind="Report" severity="ERROR" id="synthesis.typecheck.dest"/></xsl:variable>
      <xsl:variable name="errTmp" select="calcb:reportProblem($err,$where)"/>
    </xsl:if>
    <xsl:variable name="checkResult" select="cal:checkTypes($ts, $td)"/>
    
    <xsl:copy>
      <xsl:for-each select="@*">
	<xsl:attribute name="{name()}">
	  <xsl:value-of select="."/>
	</xsl:attribute>
      </xsl:for-each>

      <xsl:copy-of select="$checkResult"/>
      
      <xsl:apply-templates select="* | text()"/>
    </xsl:copy>
  </xsl:template>    
  
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
	<xsl:attribute name="{name()}">
	  <xsl:value-of select="."/>
	</xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates select="* | text()"/>
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>



