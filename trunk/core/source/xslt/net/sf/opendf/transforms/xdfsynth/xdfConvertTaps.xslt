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
                <xsl:if test="./Attribute[@name='debugtap'][@value='true'] or ./Attribute[@name='debugtap'][@value=1] or ./Attribute[@name='debugtap']/Expr[@kind='Literal'][@value=1]">
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
                  <xsl:variable name="dbgPortName" select="concat(translate(./@src,'$','_'), '_', ./@src-port, '_dbg')"/>
                  <xsl:if test="not ( $dbgTaps/*[ @src=$src and @src-port=$srcport and position() &lt; $pos ] )">
                    <Port kind="Input" type="tap" name="{$dbgPortName}">
                      <Note kind="dataWidth" width="{Note[@kind='srcWidth']/@width}"/>
                      <Type name="int"/>
                    </Port>
                  </xsl:if>
                </xsl:for-each>
              </HDL>
              
              <!-- Now the connections -->
              <xsl:for-each select="$dbgTaps/*">
                <xsl:variable name="pos" select="position()"/>
                <xsl:variable name="src" select="@src"/>
                <xsl:variable name="srcport" select="@src-port"/>
                <xsl:variable name="dbgPortName" select="concat(translate(./@src,'$','_'), '_', ./@src-port, '_dbg')"/>
                <xsl:if test="not ( $dbgTaps/*[ @src=$src and @src-port=$srcport and position() &lt; $pos ] )">
                  <Connection kind="DebugTap" src="{./@src}" src-port="{./@src-port}" dst="ila" dst-port="{$dbgPortName}">
                    <xsl:copy-of select="Note[./@kind='srcWidth']"/>
                    <Note kind="dstWidth" width="{Note[./@kind='srcWidth']/@width}"/>
                    <Okay identical="true">Types are identical.</Okay>
                    <Attribute name="bufferSize" value="0"/>
                    <Attribute name="bufferSize" value="0"/>
                  </Connection>
                </xsl:if>
              </xsl:for-each>

    <xsl:variable name="part">
      <xsl:call-template name="captureAndParsePartName">
        <xsl:with-param name="xdf" select="/XDF"/>
      </xsl:call-template>
    </xsl:variable>

    <!-- Create a script file for generation of the ILA and ICON instances -->
    <xsl:result-document href="gen_ila.sh" format="textFormat">
      <xsl:variable name="triggers">
        <xsl:for-each select="$dbgTaps/*">
          <xsl:variable name="index" select="(position()-1)*4"/>
-enabledatatrig<xsl:value-of select="$index"/> -trigportwidth<xsl:value-of select="$index"/>=<xsl:value-of select="Note[@kind='srcWidth']/@width"/> -mtrigport<xsl:value-of select="$index"/>=<xsl:value-of select="$index"/> -mtype<xsl:value-of select="$index"/>=4 \
-enabledatatrig<xsl:value-of select="$index + 1"/> -trigportwidth<xsl:value-of select="$index + 1"/>=1 -mtrigport<xsl:value-of select="$index + 1"/>=<xsl:value-of select="$index + 1"/> -mtype<xsl:value-of select="$index + 1"/>=1 \
-enabledatatrig<xsl:value-of select="$index + 2"/> -trigportwidth<xsl:value-of select="$index + 2"/>=1 -mtrigport<xsl:value-of select="$index + 2"/>=<xsl:value-of select="$index + 2"/> -mtype<xsl:value-of select="$index + 2"/>=1 \
-enabledatatrig<xsl:value-of select="$index + 3"/> -trigportwidth<xsl:value-of select="$index + 3"/>=1 -mtrigport<xsl:value-of select="$index + 3"/>=<xsl:value-of select="$index + 3"/> -mtype<xsl:value-of select="$index + 3"/>=1 \</xsl:for-each>
      </xsl:variable>
      <xsl:variable name="numTrigPorts" select="count($dbgTaps/*) * 4"/>
      <xsl:variable name="aggregateWidth" select="sum($dbgTaps/*/Note[@kind='srcWidth']/@width) + (count($dbgTaps/*) * 3)"/>
generate.sh icon -compname=icon -numports=1 -devicefamily=<xsl:value-of select="$part/PART/arch/@value"/> -bscanchain=1 -outputdirectory=.
generate.sh ila -compname=ila -outputdirectory=. \
-nummatchunits=<xsl:value-of select="$numTrigPorts"/> -enablestoragequal \
-devicefamily=<xsl:value-of select="$part/PART/arch/@value"/> -srl16type=2 \<xsl:for-each select="$triggers">
  <xsl:copy-of select="."/>
</xsl:for-each>
-datasameastrig -datadepth=2048 -datawidth=<xsl:value-of select="$aggregateWidth"/> \
-trigseqtype=1 -numtrigseqlevels=4 -numtrigports=<xsl:value-of select="$numTrigPorts"/>
<xsl:text>
</xsl:text>
    </xsl:result-document>
    
    <!-- Create a cdc file for ILA port naming in chipscope -->
    <xsl:result-document href="ila.cdc" format="textFormat">#ChipScope Core Generator Project File Version 3.0
SignalExport.type=ila
SignalExport.dataEqualsTrigger=true
SignalExport.triggerPortCount=<xsl:value-of select="count($dbgTaps/*) * 4"/>
      <xsl:for-each select="$dbgTaps/*">
        <xsl:variable name="pos" select="position()"/>
        <xsl:variable name="index" select="(position()-1)*4"/>
        <xsl:variable name="width" select="Note[@kind='srcWidth']/@width"/>
        <xsl:variable name="bitindex" select="sum($dbgTaps/Connection[position() &lt; $pos]/Note[@kind='srcWidth']/@width) + (count($dbgTaps/Connection[position() &lt; $pos]) * 3)"/>
        <xsl:variable name="source" select="$xdfContext//Instance[@id=./@src]"/>
        <xsl:variable name="out">$1_<xsl:value-of select="position()"/>_$2</xsl:variable>
<!--        <xsl:variable name="dbgPortName" select="concat(translate(./@src,'$','_'), '_', ./@src-port, '_dbg')"/>-->
            <xsl:variable name="dbgPortName" select="concat(replace(translate($source/@instance-name,'$','_'), '(.*)_[0-9]+([0-9]{3}?)', $out), '_', ./@src-port, '_dbg')"/>
        <xsl:call-template name="exportSignal">
          <xsl:with-param name="trigNum" select="$index"/>
          <xsl:with-param name="bitNum" select="$width"/>
          <xsl:with-param name="portname" select="$dbgPortName"/></xsl:call-template>
SignalExport.triggerChannel&lt;<xsl:value-of select="$index + 1"/>&gt;&lt;0&gt;=<xsl:value-of select="$dbgPortName"/>_SEND[0]
SignalExport.triggerChannel&lt;<xsl:value-of select="$index + 2"/>&gt;&lt;0&gt;=<xsl:value-of select="$dbgPortName"/>_ACK[0]
SignalExport.triggerChannel&lt;<xsl:value-of select="$index + 3"/>&gt;&lt;0&gt;=<xsl:value-of select="$dbgPortName"/>_RDY[0]
SignalExport.triggerPort&lt;<xsl:value-of select="$index"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>
SignalExport.triggerPort&lt;<xsl:value-of select="$index + 1"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>_SEND
SignalExport.triggerPort&lt;<xsl:value-of select="$index + 2"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>_ACK
SignalExport.triggerPort&lt;<xsl:value-of select="$index + 3"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>_RDY
SignalExport.triggerPortWidth&lt;<xsl:value-of select="$index"/>&gt;=<xsl:value-of select="$width"/>
SignalExport.triggerPortWidth&lt;<xsl:value-of select="$index + 1"/>&gt;=1
SignalExport.triggerPortWidth&lt;<xsl:value-of select="$index + 2"/>&gt;=1
SignalExport.triggerPortWidth&lt;<xsl:value-of select="$index + 3"/>&gt;=1
SignalExport.triggerPortIsData&lt;<xsl:value-of select="$index"/>&gt;=true
SignalExport.triggerPortIsData&lt;<xsl:value-of select="$index + 1"/>&gt;=true
SignalExport.triggerPortIsData&lt;<xsl:value-of select="$index + 2"/>&gt;=true
SignalExport.triggerPortIsData&lt;<xsl:value-of select="$index + 3"/>&gt;=true
SignalExport.bus&lt;<xsl:value-of select="$index"/>&gt;.channelList=<xsl:call-template name="countUp"><xsl:with-param name="start" select="$bitindex"/><xsl:with-param name="count" select="$width"/></xsl:call-template>
SignalExport.bus&lt;<xsl:value-of select="$index"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>_DATA
SignalExport.bus&lt;<xsl:value-of select="$index + 1"/>&gt;.channelList=<xsl:call-template name="countUp"><xsl:with-param name="start" select="$bitindex + $width"/><xsl:with-param name="count" select="1"/></xsl:call-template>
SignalExport.bus&lt;<xsl:value-of select="$index + 1"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>_SEND
SignalExport.bus&lt;<xsl:value-of select="$index + 2"/>&gt;.channelList=<xsl:call-template name="countUp"><xsl:with-param name="start" select="$bitindex + $width + 1"/><xsl:with-param name="count" select="1"/></xsl:call-template>
SignalExport.bus&lt;<xsl:value-of select="$index + 2"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>_ACK
SignalExport.bus&lt;<xsl:value-of select="$index + 3"/>&gt;.channelList=<xsl:call-template name="countUp"><xsl:with-param name="start" select="$bitindex + $width + 2"/><xsl:with-param name="count" select="1"/></xsl:call-template>
SignalExport.bus&lt;<xsl:value-of select="$index + 3"/>&gt;.name=<xsl:value-of select="$dbgPortName"/>_RDY
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