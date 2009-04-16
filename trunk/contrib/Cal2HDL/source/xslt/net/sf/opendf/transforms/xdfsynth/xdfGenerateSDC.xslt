
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    version="2.0">
    
  <xsl:output method="text"/>
    
  <xd:doc type="stylesheet">
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2006-2007</xd:copyright>
  </xd:doc>
    
  <xsl:variable name="libPackage">SystemBuilder</xsl:variable>
  <xsl:variable name="workPackage">work</xsl:variable>
  <xsl:variable name="architecture">rtl</xsl:variable>
  
  <!-- When no clock domain is specified use this one -->
  <xsl:variable name="defaultClockDomain">CLK</xsl:variable>
  
  <!-- A list of all the clock domains used by this network  -->    
  <xsl:variable name="clockDomains">
    <xsl:variable name="clocks" select=
      "//Attribute[@name='clockDomain'][not(some $a in preceding::Attribute[@name='clockDomain']
      satisfies $a/@value = @value )]"/>
    <xsl:for-each select="$clocks">
      <clock name="{@value}" index="{position()}"/>       
    </xsl:for-each>
    
    <!-- Create a default clock domain if
         * there is an actor instance with no clock domain
         * or, there is a port with no clock domain that is the child of
           a network that has no clock domain -->
    <xsl:if test="//Actor/parent::Instance[not(Attribute[@name='clockDomain'])]
      or /XDF/Port[not(Attribute[@name='clockDomain'] or ../Attribute[@name='clockDomain'])]">
      <!-- Create the default clock if it does not exist already -->
      <xsl:if test="not(//Attribute[@name='clockDomain'][@value='CLK'])">
        <clock name="{$defaultClockDomain}" index="{count($clocks) + 1}"/>        
      </xsl:if>
    </xsl:if>
  </xsl:variable>
  
  <xsl:template match="/XDF">

    <xsl:text>########################################################################&#10;</xsl:text>
    <xsl:text>#&#10;# Synplicity timing contraint file for </xsl:text><xsl:value-of select="@name"/>
    <xsl:text>&#10;# Generated: </xsl:text>
    <xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]([z])')"/>
    <xsl:text>&#10;#&#10;</xsl:text>  

    <!-- Copy any SDC constrains in the NL --> 
    <xsl:for-each select="Attribute[@name='sdcConstraint']/Expr/Expr">
      <xsl:value-of select="@value"/>
      <xsl:text>&#10;</xsl:text>
    </xsl:for-each>
 
    <!-- Create false paths for any async FIFO controllers -->      
    <xsl:for-each select="Instance/Actor/Port[@kind='Input'][Note[@kind='fanin']/@width &gt;= 1] |
                          Port[@kind='Output'][Note[@kind='fanin']/@width &gt;= 1]">

      <!-- find the connection that ends at this port -->  
      <xsl:variable name="c" select=" if( @kind='Input' ) then 
         ../../../Connection[@dst=current()/../../@id][@dst-port=current()/@name ] else
         ../Connection[@dst=''][@dst-port=current()/@name ]"/>
 
      <!-- get the base name of this input signal -->
      <xsl:variable name="dst-base-name">
        <xsl:choose>
        
          <!-- actor input port -->
          <xsl:when test="@kind = 'Input'">
            <xsl:text>ai_</xsl:text>
            <xsl:value-of select="../../@instance-name"/>
            <xsl:text>_</xsl:text>
            <xsl:value-of select="@name"/>
          </xsl:when>
        
          <!-- Source for this connection is an actor output -->
          <xsl:otherwise>
            <xsl:text>no_</xsl:text>
            <xsl:value-of select="@name"/>
          </xsl:otherwise>
        
        </xsl:choose>
      </xsl:variable>
      
      <!-- get the index of the clock domain of the source -->
      <xsl:variable name="src-clock">
        <xsl:choose>
          
          <!-- Source for this connection is a network input port -->
          <xsl:when test="$c/@src = ''">
            <xsl:apply-templates select="/XDF/Port[@name=$c/@src-port]" mode="find-clock-domain"/>
          </xsl:when>
          
          <!-- Source for this connection is an actor output -->
          <xsl:otherwise>
            <xsl:apply-templates select="/XDF/Instance[@id=$c/@src]/Actor/Port[@name=$c/@src-port]" mode="find-clock-domain"/>
          </xsl:otherwise>
            
        </xsl:choose>
      </xsl:variable>
      
      <!-- get the index of the clock domain of the destination -->
      <xsl:variable name="dst-clock">
        <xsl:apply-templates select="." mode="find-clock-domain"/>
      </xsl:variable>
  
      <xsl:if test="not( $src-clock = $dst-clock )">

        <!-- full signal -->      
        <xsl:text>&#10;define_false_path -from { i:q_</xsl:text><xsl:value-of select="$dst-base-name"/>
        <xsl:text>.fifo.ctl.read_lastgray* } -to { i:q_</xsl:text><xsl:value-of select="$dst-base-name"/>
        <xsl:text>.fifo.ctl.full }</xsl:text>

        <!-- address collision  Synplicity does not recognize this!
        
        <xsl:text>&#10;define_false_path -from { c:</xsl:text>
        <xsl:value-of select="$clockDomains/clock[position()=$src-clock]/@name"/>
        <xsl:text>:r } -through { i:q_</xsl:text><xsl:value-of select="$dst-base-name"/>
        <xsl:text>.fifo.ram.* } -to { c:</xsl:text>
        <xsl:value-of select="$clockDomains/clock[position()=$dst-clock]/@name"/>
        <xsl:text>:r }</xsl:text>
        
        <xsl:text>&#10;define_false_path -from { c:</xsl:text>
        <xsl:value-of select="$clockDomains/clock[position()=$dst-clock]/@name"/>
        <xsl:text>:r } -through { i:q_</xsl:text><xsl:value-of select="$dst-base-name"/>
        <xsl:text>.fifo.ram.* } -to { c:</xsl:text>
        <xsl:value-of select="$clockDomains/clock[position()=$src-clock]/@name"/>
        <xsl:text>:r }</xsl:text>
        
        -->     
        
        <!-- empty signal -->      
        <xsl:text>&#10;define_false_path -from { i:q_</xsl:text><xsl:value-of select="$dst-base-name"/>
        <xsl:text>.fifo.ctl.write_addrgray* } -to { i:q_</xsl:text><xsl:value-of select="$dst-base-name"/>
        <xsl:text>.fifo.ctl.empty }</xsl:text>
      
      </xsl:if>
      
    </xsl:for-each>

    <xsl:text>&#10;#&#10;########################################################################&#10;</xsl:text> 
    
  </xsl:template>  

  <!-- Get the index of the clock domain -->
  <xsl:template match="Port" mode="find-clock-domain">
    <xsl:variable name="name">
      <xsl:choose>
        
        <!-- all actor ports are in the clock domain of the actor instance -->
        <xsl:when test="parent::Actor">
          <xsl:value-of select="../../Attribute[@name='clockDomain']/@value"/>
        </xsl:when>
        
        <!-- network ports can have their own clockDomain attribute -->        
        <xsl:when test="Attribute[@name='clockDomain']">
          <xsl:value-of select="Attribute[@name='clockDomain']/@value"/>          
        </xsl:when>
        
        <!-- take the clock domain from the network -->
        <xsl:otherwise>
          <xsl:value-of select="../Attribute[@name='clockDomain']/@value"/>
        </xsl:otherwise>
        
      </xsl:choose>
    </xsl:variable>
    
    <xsl:choose>
      <xsl:when test="string-length($name) &gt; 0">
        <xsl:value-of select="$clockDomains/clock[@name = $name]/@index"/>
      </xsl:when>
      <xsl:otherwise>
        <!-- the default clock is the last one in the list -->
        <xsl:value-of select="$clockDomains/clock[@name = $defaultClockDomain]/@index"/>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>
  
</xsl:stylesheet>



