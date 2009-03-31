<!--
    Generates ILA taps for all connections with a debugtap attribute (value true)
-->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    version="2.0">
    
    <xsl:output method="xml"/>
    <xsl:output method="text" name="textFormat"/>
    <xsl:include href="net/sf/opendf/transforms/xdfsynth/parsePartName.xslt"/>

    <xsl:template match="XDF">
      <xsl:variable name="xdfContext" select="."/>
      
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates select="* | text()"/>

            <xsl:variable name="dbgTaps">
              <xsl:for-each select=".//Connection">
                <xsl:if test="./Attribute[@name='queuetap'][@value='true'] or ./Attribute[@name='queuetap'][@value=1] or ./Attribute[@name='queuetap']/Expr[@kind='Literal'][@value=1]">
                  <xsl:copy-of select="."/>
                </xsl:if>
              </xsl:for-each>
            </xsl:variable>
            
            <!-- Create the ila with one input for each debug tap -->
            <xsl:if test="count($dbgTaps/*) > 0">
              <HDL id="ila" instance-name="ila">
                <Class name="ila">
                  <QID><ID id="hdl"/></QID>
                </Class>
                <xsl:for-each select="$dbgTaps/*">
                  <xsl:variable name="pos" select="position()"/>
                  <xsl:variable name="src" select="@src"/>
                  <xsl:variable name="srcport" select="@src-port"/>
                  <xsl:variable name="dst" select="@dst"/>
                  <xsl:variable name="dstport" select="@dst-port"/>
                  <xsl:variable name="dbgPortName" select="concat(translate(./@src,'$','_'), '_', ./@src-port, '_to_', translate(./@dst,'$','_'), '_', ./@dst-port, '_dbg')"/>
                  <xsl:if test="not ( $dbgTaps/*[ @src=$src and @src-port=$srcport and @dst=$dst and @dst-port=$dstport and position() &lt; $pos ] )">
                    <Port kind="Input" type="queueDebug" name="{$dbgPortName}" src="{$src}" dst="{$dst}" src-port="{$srcport}" dst-port="{$dstport}">
                      <Note kind="dataWidth" width="{Note[@kind='srcWidth']/@width}"/>
                      <Type name="int"/>
                    </Port>
                  </xsl:if>
                </xsl:for-each>
              </HDL>
              
    <xsl:variable name="aggregateWidth" select="count($dbgTaps/*) * 2"/>
    <xsl:variable name="part">
      <xsl:call-template name="captureAndParsePartName">
        <xsl:with-param name="xdf" select="/XDF"/>
      </xsl:call-template>
    </xsl:variable>
    <!-- Create a script file for generation of the ILA and ICON instances -->
    <xsl:result-document href="gen_ila.sh" format="textFormat">
generate.sh icon -compname=icon -numports=1 -devicefamily=<xsl:value-of select="$part/PART/arch/@value"/> -bscanchain=1 -outputdirectory=.
generate.sh ila -compname=ila -outputdirectory=. \
-nummatchunits=1 -enablestoragequal \
-devicefamily=<xsl:value-of select="$part/PART/arch/@value"/> -srl16type=2 \
-enabledatatrig0 -trigportwidth0=<xsl:value-of select="$aggregateWidth"/> -mtrigport0=0 -mtype0=1 \
-datasameastrig -datadepth=2048 -datawidth=<xsl:value-of select="$aggregateWidth"/> \
-trigseqtype=1 -numtrigseqlevels=4 -numtrigports=1
<xsl:text>
</xsl:text>
    </xsl:result-document>
    
    <!-- Create a cdc file for ILA port naming in chipscope -->
    <xsl:result-document href="ila.cdc" format="textFormat">#ChipScope Core Generator Project File Version 3.0
SignalExport.type=ila
SignalExport.dataEqualsTrigger=true
SignalExport.triggerPortCount=1
<xsl:for-each select="$dbgTaps/*">
  <xsl:variable name="conn" select="."/>
  <xsl:variable name="source" select="$xdfContext//Instance[@id=$conn/@src]"/>
  <xsl:variable name="dest" select="$xdfContext//Instance[@id=$conn/@dst]"/>
  <xsl:variable name="out">$1_<xsl:value-of select="position()"/>_$2</xsl:variable>
<!-- We use replace to get rid of the very lengthy identifiers.  We keep the last 3 digits for
     correlating with the HDL and add in a count value to ensure uniqueness. -->
  <xsl:variable name="dbgSourcePortName" select="concat(replace(translate($source/@instance-name,'$','_'), '(.*)_[0-9]+([0-9]{3}?)', $out), '_', ./@src-port)"/>
<xsl:variable name="dbgDestPortName" select="concat(replace(translate($dest/@instance-name,'$','_'), '(.*)_[0-9]+([0-9]{3}?)', $out), '_', ./@dst-port)"/>
<xsl:variable name="index" select="(position()-1) * 2"/>
SignalExport.triggerChannel&lt;0&gt;&lt;<xsl:value-of select="$index"/>&gt;=<xsl:value-of select="$dbgDestPortName"/>_SEND
SignalExport.triggerChannel&lt;0&gt;&lt;<xsl:value-of select="$index+1"/>&gt;=<xsl:value-of select="$dbgSourcePortName"/>_RDY</xsl:for-each>
SignalExport.triggerPort&lt;0&gt;.name=trig0
SignalExport.triggerPortWidth&lt;0&gt;=<xsl:value-of select="$aggregateWidth"/>
SignalExport.triggerPortIsData&lt;0&gt;=true
      <xsl:for-each select="$dbgTaps/*">
        <xsl:variable name="pos" select="position()"/>
        <xsl:variable name="out">$1_<xsl:value-of select="$pos"/>_$2</xsl:variable>
        <xsl:variable name="index" select="position()-1"/>
<!--        <xsl:variable name="width" select="Note[@kind='srcWidth']/@width"/>-->
        <xsl:variable name="bitindex" select="(position()-1)*2"/>
        <xsl:variable name="conn" select="."/>
        <xsl:variable name="dest" select="$xdfContext//Instance[@id=$conn/@dst]"/>
        <xsl:variable name="dbgPortName" select="concat(replace(translate($dest/@instance-name,'$','_'), '(.*)_[0-9]+([0-9]{3}?)', $out), '_', ./@dst-port, '_QStat')"/>
<!--        <xsl:call-template name="exportSignal">
          <xsl:with-param name="portname" select="$dbgPortName"/></xsl:call-template>-->
SignalExport.bus&lt;<xsl:value-of select="$index"/>&gt;.channelList=<xsl:call-template name="countUp"><xsl:with-param name="start" select="$bitindex"/><xsl:with-param name="count" select="2"/></xsl:call-template>
SignalExport.bus&lt;<xsl:value-of select="$index"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>
      </xsl:for-each>
    </xsl:result-document>
            </xsl:if>

            
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
    

  <xsl:template name="exportSignal">
    <xsl:param name="trigNum"/>
    <xsl:param name="bitNum"/>
    <xsl:param name="portname" select="TRIG"/>
<xsl:if test="$bitNum > 0">
SignalExport.triggerChannel&lt;<xsl:value-of select="$trigNum"/>&gt;&lt;<xsl:value-of select="$bitNum - 1"/>&gt;=<xsl:value-of select="$portname"/>[<xsl:value-of select="$bitNum - 1"/>]<xsl:call-template name="exportSignal">
    <xsl:with-param name="trigNum" select="$trigNum"/>
    <xsl:with-param name="bitNum" select="$bitNum - 1"/>
    <xsl:with-param name="portname" select="$portname"/>
  </xsl:call-template>
</xsl:if>
  </xsl:template>
  
  <xsl:template name="countUp">
    <xsl:param name="start"/>
    <xsl:param name="count"/>
<xsl:if test="$count > 0"><xsl:value-of select="$start"/><xsl:text> </xsl:text><xsl:call-template name="countUp"><xsl:with-param name="start" select="$start + 1"/><xsl:with-param name="count" select="$count - 1"/></xsl:call-template></xsl:if>
  </xsl:template>
  
    
</xsl:stylesheet>