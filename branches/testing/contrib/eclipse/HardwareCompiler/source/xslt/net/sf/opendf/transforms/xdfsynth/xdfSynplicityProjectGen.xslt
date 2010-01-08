
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:opendf="java:net.sf.opendf.util.xml.Util"
    version="2.0">
  
  <xsl:output method="text"/>
  <xsl:include href="net/sf/opendf/transforms/xdfsynth/parsePartName.xslt"/>

  <!--
      This stylesheet generates a synplicity project file as its output.  This project
      file was created for synplify_pro version 8.6.2.
  -->

  
  <xsl:template match="XDF">

    <xsl:variable name="designName" select="@name"/>
    <xsl:variable name="targetDir" select="'rev_1'"/>
#-- Synplicity, Inc.
#-- Version Synplify Pro 8.6.2
#-- Project file <xsl:value-of select="$designName"/>
#-- Written on <xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]([z])')"/>

# add_file options
add_file -vhdl -lib SystemBuilder "lib/sbtypes.vhdl"
add_file -vhdl -lib SystemBuilder "lib/sbfifo.vhdl"
add_file -vhdl -lib SystemBuilder "lib/sbfifo_behavioral.vhdl"
add_file -vhdl -lib work "<xsl:value-of select="$designName"/>.vhd"
<xsl:for-each select="//Instance">
  <xsl:variable name="includeFile">
    add_file -verilog "Actors/<xsl:value-of select="@instance-name"/>.v"
  </xsl:variable>
<xsl:value-of select="normalize-space($includeFile)"/><xsl:text>
</xsl:text>
</xsl:for-each>

#specify the revision
impl -add <xsl:value-of select="$targetDir"/> -type fpga

#device options
<xsl:variable name="partId">
  <xsl:call-template name="captureAndParsePartName">
    <xsl:with-param name="xdf" select="."/>
  </xsl:call-template>
</xsl:variable>
set_option -technology <xsl:value-of select="$partId/PART/arch/@value"/>
set_option -part <xsl:value-of select="$partId/PART/part/@value"/>
set_option -package <xsl:value-of select="$partId/PART/package/@value"/>
set_option -speed_grade <xsl:value-of select="$partId/PART/speed/@value"/>


#compilation/mapping options
set_option -default_enum_encoding default
set_option -symbolic_fsm_compiler 1
set_option -resource_sharing 1
set_option -use_fsm_explorer 0
set_option -top_module "<xsl:value-of select="$designName"/>"

#map options
set_option -frequency auto
set_option -run_prop_extract 1
set_option -fanout_limit 10000
set_option -disable_io_insertion 1
set_option -pipe 1
set_option -update_models_cp 0
set_option -verification_mode 0
set_option -modular 0
set_option -retiming 1
set_option -no_sequential_opt 0
set_option -fixgatedclocks 3

#simulation options
set_option -write_verilog 0
set_option -write_vhdl 0

#VIF options
set_option -write_vif 1

#automatic place and route (vendor) options
set_option -write_apr_constraint 1

#set result format/file last
project -result_file "<xsl:value-of select="$targetDir"/>/<xsl:value-of select="$designName"/>.edf"

#implementation attributes
set_option -vlog_std v2001
set_option -dup 0
set_option -project_relative_includes 1

#extension attributes
#set_option -fixgeneratedclocks 3

#par_1 attributes
set_option -job par_1 -add par
set_option -job par_1 -option run_backannotation 0

impl -active "<xsl:value-of select="$targetDir"/>"
  </xsl:template>



</xsl:stylesheet>



