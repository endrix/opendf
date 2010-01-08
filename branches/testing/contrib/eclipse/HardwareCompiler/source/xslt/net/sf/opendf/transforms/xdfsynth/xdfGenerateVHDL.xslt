
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    version="2.0">
    
    <xsl:output method="text"/>
    
    <xd:doc type="stylesheet">
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx, 2006-2007</xd:copyright>
    </xd:doc>
    
    <xsl:include href="net/sf/opendf/transforms/xdfsynth/hdlUtil.xslt"/>

    <xsl:variable name="libPackage">SystemBuilder</xsl:variable>
    <xsl:variable name="workPackage">work</xsl:variable>
    <xsl:variable name="architecture">rtl</xsl:variable>
    
  <!-- A list of all the clock domains used by this network  -->    
  <xsl:variable name="clockDomains">
    <xsl:call-template name="findClockDomains">
      <xsl:with-param name="root" select="/XDF"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:template match="/XDF">

    <xsl:text>-- ----------------------------------------------------------------------------------&#10;</xsl:text> 
    <xsl:text>-- top level model for </xsl:text><xsl:value-of select="@name"/>
    <xsl:text>&#10;-- Generated: </xsl:text>
    <xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]([z])')"/>
    <xsl:text>&#10;-- ----------------------------------------------------------------------------------&#10;</xsl:text>  
    
    <xsl:call-template name="clock-domain-report"/>
    
    <xsl:text>&#10;library ieee, </xsl:text><xsl:value-of select="$libPackage"/>
    <xsl:text>;&#10;use ieee.std_logic_1164.all;&#10;</xsl:text>
 
    <xsl:text>&#10;entity </xsl:text><xsl:value-of select="@name"/>
    <xsl:text> is&#10;port (</xsl:text>
    <xsl:apply-templates select="Port" mode="port-decl"/>
    <xsl:text>);&#10;end entity </xsl:text><xsl:value-of select="@name"/><xsl:text>;&#10;</xsl:text>

    <xsl:text>&#10;architecture </xsl:text>
    <xsl:value-of select="$architecture"/>
    <xsl:text> of </xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text> is&#10;</xsl:text> 

    <!-- Declare the domain-specific clocks and resets -->
    <xsl:text>&#10;  signal clocks, resets: std_logic_vector(</xsl:text>
    <xsl:value-of select="count($clockDomains/clock)-1"/>
    <xsl:text> downto 0);&#10;</xsl:text>

    <!-- local signal name conventions 
    
        network inputs:
          ni_{port name}_*     connections to the ports
          nif_{port name}_*    connections to fanned-out versions
          
        network outputs:
          no_{port name}_*     connections to the ports
          
        actor inputs:
          ai_{instance name}_{port name}_*     connections to the ports
          
        actor outputs:
          ao_{instance name}_{port name}_*     connections to the ports
          aof_{instance name}_{port name}_*    connections to fanned-out versions
    -->
            
    <!-- Create the signal bundles for all network ports -->	  
    <xsl:apply-templates select="Port[@kind='Input']" mode="signal-bundle-decl"/>
    <xsl:apply-templates select="Port[@kind='Output'][Note[@kind='fanin']/@width &gt;= 1]" mode="signal-bundle-decl"/>
      
    <!-- Create the signal bundles for actor ports that are used. Unused ports will be connected
         to the appropriate values/left open in the component instantiation. -->
    <xsl:apply-templates select="Instance/Actor/Port[@kind='Output'][Note[@kind='fanout']/@width &gt;= 1]" mode="signal-bundle-decl"/>
    <xsl:apply-templates select="Instance/Actor/Port[@kind='Input'][Note[@kind='fanin']/@width &gt;= 1 ]" mode="signal-bundle-decl"/>

    <xsl:call-template name="ila-declarations"/>
  
    <!-- Declare a component for each instance -->
    <xsl:for-each select="Instance">      
      <xsl:text>&#10;  component </xsl:text><xsl:value-of select="@instance-name"/><xsl:text> is&#10;</xsl:text>
      <xsl:text>  port(</xsl:text>
      <xsl:apply-templates select="Actor/Port" mode="port-decl"/>
      <xsl:text>  );&#10;</xsl:text>
      <xsl:text>  end component </xsl:text><xsl:value-of select="@instance-name"/><xsl:text>;&#10;</xsl:text>
    </xsl:for-each>

    <!-- Create the architecture body -->
 
    <xsl:text>&#10;begin&#10;&#10;</xsl:text>

    <!-- Generate the domain-specific resets -->
    <xsl:text>  rcon: entity </xsl:text><xsl:value-of select="$libPackage"/><xsl:text>.resetController</xsl:text>
    <xsl:text>( behavioral )&#10;</xsl:text>
    <xsl:text>  generic map( count => </xsl:text><xsl:value-of select="count($clockDomains/clock)"/>
    <xsl:text> )&#10;</xsl:text>
    <xsl:text>  port map( clocks => clocks, reset_in => RESET, resets => resets );&#10;&#10;</xsl:text>
    <xsl:for-each select="$clockDomains/clock">
      <xsl:text>  clocks(</xsl:text><xsl:value-of select="position()-1"/>
      <xsl:text>) &lt;= </xsl:text><xsl:value-of select="@name"/><xsl:text>;&#10;</xsl:text>
    </xsl:for-each>

    <!-- Instantiate the chipscope ILA -->
    <xsl:call-template name="ila-instantiation"/>

    <!-- Instantiate all the actors -->
    <xsl:for-each select="Instance">
      <xsl:text>&#10;  i_</xsl:text>
      <xsl:value-of select="@instance-name"/>
      <xsl:choose>
        <xsl:when test="Attribute[@name='vhdlEntity']">
          <xsl:text> : entity </xsl:text><xsl:value-of select="Attribute[@name='vhdlEntity']/@value"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text> : component </xsl:text><xsl:value-of select="@instance-name"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>&#10;  port map (&#10;</xsl:text>
      <xsl:apply-templates select="Actor/Port" mode="connect-port"/>
      <xsl:text>  );&#10;</xsl:text>

      <!-- Add fanout blocks to actor outputs that are used -->
      <xsl:apply-templates select="Actor/Port[@kind='Output'][Note[@kind='fanout']/@width &gt;= 1]" mode="fanout"/>
      
      <!-- Instantiate queues for connected actor inputs -->
      <xsl:apply-templates select="Actor/Port[@kind='Input'][Note[@kind='fanin']/@width &gt;= 1]" mode="queue"/>
      
    </xsl:for-each>

    <!-- Add fanout blocks to network inputs that are used -->
    <xsl:apply-templates select="Port[@kind='Input'][Note[@kind='fanout']/@width &gt;= 1]" mode="fanout"/>

    <!-- Instantiate queues for connected network outputs -->
    <xsl:apply-templates select="Port[@kind='Output'][Note[@kind='fanin']/@width &gt;= 1]" mode="queue"/>

    <!-- Add connections to the network output ports -->
    <xsl:for-each select="Port[@kind='Output']">
      
      <!-- connect this network output port to its associated signals -->
      <xsl:text>&#10;  </xsl:text>	      
      <xsl:value-of select="@name"/><xsl:text>_DATA &lt;= no_</xsl:text><xsl:value-of select="@name"/>
      <xsl:text>_DATA;&#10;  </xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_SEND &lt;= no_</xsl:text><xsl:value-of select="@name"/>
      <xsl:text>_SEND;&#10;  no_</xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_ACK &lt;= </xsl:text><xsl:value-of select="@name"/>
      <xsl:text>_ACK;&#10;  </xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_COUNT &lt;= no_</xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:text>_COUNT;&#10;  no_</xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_RDY &lt;= </xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_RDY;&#10;</xsl:text>
      
      <!-- if unused, drive the outputs to zero -->       
      <xsl:if test="Note[@kind='fanin']/@width &lt; 1">
        <xsl:text>&#10;  no_</xsl:text><xsl:value-of select="@name"/><xsl:text>_DATA &lt;= </xsl:text>
        <xsl:choose>
          <xsl:when test="Note[@kind='dataWidth']/@width = 1">
            <xsl:value-of select="$libPackage"/><xsl:text>.sb_types.logic0</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>(others =&gt; </xsl:text><xsl:value-of select="$libPackage"/>
            <xsl:text>.sb_types.logic0)</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text>;&#10;  no_</xsl:text>
        <xsl:value-of select="@name"/><xsl:text>_SEND &lt;= </xsl:text><xsl:value-of select="$libPackage"/>
        <xsl:text>.sb_types.logic0;&#10;  no_</xsl:text>
        <xsl:value-of select="@name"/><xsl:text>_COUNT &lt;= (others =&gt; </xsl:text>
        <xsl:value-of select="$libPackage"/><xsl:text>.sb_types.logic0);&#10;</xsl:text>          
      </xsl:if>
      
    </xsl:for-each>

    <!-- Add connections to the network input ports -->
    <xsl:for-each select="Port[@kind='Input']">
 
      <!-- connect this network input port to its associated signals -->
      <xsl:text>&#10;  ni_</xsl:text>	      
      <xsl:value-of select="@name"/><xsl:text>_DATA &lt;= </xsl:text><xsl:value-of select="@name"/>
      <xsl:text>_DATA;&#10;  ni_</xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_SEND &lt;= </xsl:text><xsl:value-of select="@name"/>
      <xsl:text>_SEND;&#10;  </xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_ACK &lt;= ni_</xsl:text><xsl:value-of select="@name"/>
      <xsl:text>_ACK;&#10;  ni_</xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_COUNT &lt;= </xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:text>_COUNT;&#10;  </xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_RDY &lt;= ni_</xsl:text>
      <xsl:value-of select="@name"/><xsl:text>_RDY;&#10;</xsl:text>
      
      <!-- if unused, drive the control outputs to zero -->       
      <xsl:if test="Note[@kind='fanout']/@width &lt; 1">
        <xsl:text>&#10;  ni_</xsl:text><xsl:value-of select="@name"/>
        <xsl:text>_ACK &lt;= </xsl:text><xsl:value-of select="$libPackage"/>
        <xsl:text>.sb_types.logic1;&#10;  ni_</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>_RDY &lt;= </xsl:text><xsl:value-of select="$libPackage"/>
        <xsl:text>.sb_types.logic1;&#10;</xsl:text>
      </xsl:if>
      
    </xsl:for-each>
      
    <xsl:text>&#10;end architecture </xsl:text><xsl:value-of select="$architecture"/><xsl:text>;&#10;</xsl:text>

    <xsl:text>&#10;-- ----------------------------------------------------------------------------------</xsl:text> 
    <xsl:text>&#10;-- ----------------------------------------------------------------------------------</xsl:text> 
    <xsl:text>&#10;-- ----------------------------------------------------------------------------------&#10;</xsl:text> 
      
  </xsl:template>

  <!-- Print out a VHDL type declaration -->
  <xsl:template match="Type" mode="type-decl">
  	<xsl:choose>
  	  
  	  <xsl:when test="@name='bool'">
  	  	<xsl:text>std_logic</xsl:text>
  	  </xsl:when>
  	  
  	  <!-- note that one-bit vectors are mapped to the scalar type -->
  	  <xsl:when test="../Note[@kind='dataWidth']/@width = 1">
  		<xsl:text>std_logic</xsl:text>
  	  </xsl:when>
  	  
  	  <xsl:otherwise>
  	  	<xsl:text>std_logic_vector(</xsl:text>
  	  	<xsl:value-of select="../Note[@kind='dataWidth']/@width - 1"/>
  	  	<xsl:text> downto 0)</xsl:text>
  	  </xsl:otherwise>
  	  
  	</xsl:choose>
  </xsl:template>

  <!-- Create the signal declarations for a dataflow channel -->
  <xsl:template match="Port" mode="signal-bundle-decl">
    <xsl:choose>
      
      <!-- Network input port -->
      <xsl:when test="not(parent::Actor) and @kind='Input'">
        
        <!-- network input to fanout block -->
        <xsl:call-template name="signal-bundle-decl">
          <xsl:with-param name="base-name">
            <xsl:text>ni_</xsl:text><xsl:value-of select="@name"/>
          </xsl:with-param>
          <xsl:with-param name="fanned-control-type">
            <xsl:text>std_logic</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
        
        <!-- fanout block to queue -->
        <xsl:call-template name="signal-bundle-decl">
          <xsl:with-param name="base-name">
            <xsl:text>nif_</xsl:text><xsl:value-of select="@name"/>
          </xsl:with-param>
          <xsl:with-param name="fanned-control-type">
            <xsl:text>std_logic_vector(</xsl:text><xsl:value-of select="Note[@kind='fanout']/@width - 1"/><xsl:text> downto 0)</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      
      <!-- network output port -->
      <xsl:when test="not( parent::Actor ) and @kind='Output'">
        
        <!-- queue to network output -->
        <xsl:call-template name="signal-bundle-decl">
          <xsl:with-param name="base-name">
            <xsl:text>no_</xsl:text><xsl:value-of select="@name"/>
          </xsl:with-param>
          <xsl:with-param name="fanned-control-type">
            <xsl:text>std_logic</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
        
      </xsl:when>
        
      <!-- Actor output port -->
      <xsl:when test="parent::Actor and @kind='Output'">
        
        <!-- actor output to fanout block -->
         <xsl:call-template name="signal-bundle-decl">
          <xsl:with-param name="base-name">
            <xsl:text>ao_</xsl:text><xsl:value-of select="../../@instance-name"/>
            <xsl:text>_</xsl:text><xsl:value-of select="@name"/>
          </xsl:with-param>
          <xsl:with-param name="fanned-control-type">
            <xsl:text>std_logic</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
        
        <!-- fanout block to queue -->
        <xsl:call-template name="signal-bundle-decl">
          <xsl:with-param name="base-name">
            <xsl:text>aof_</xsl:text><xsl:value-of select="../../@instance-name"/>
            <xsl:text>_</xsl:text><xsl:value-of select="@name"/>
          </xsl:with-param>
          <xsl:with-param name="fanned-control-type">
            <xsl:text>std_logic_vector(</xsl:text><xsl:value-of select="Note[@kind='fanout']/@width - 1"/><xsl:text> downto 0)</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
            
      <!-- Actor input port -->
      <xsl:when test="parent::Actor and @kind='Input'">
        
        <!-- queue to actor input -->
        <xsl:call-template name="signal-bundle-decl">
          <xsl:with-param name="base-name">
            <xsl:text>ai_</xsl:text><xsl:value-of select="../../@instance-name"/>
            <xsl:text>_</xsl:text><xsl:value-of select="@name"/>
          </xsl:with-param>
          <xsl:with-param name="fanned-control-type">
            <xsl:text>std_logic</xsl:text>
          </xsl:with-param>
        </xsl:call-template>

      </xsl:when>
      
    </xsl:choose>
    
  </xsl:template>
  
  <!-- Create the electrical signals to represent a dataflow channel -->
  <xsl:template name="signal-bundle-decl">
    <xsl:param name="base-name"/>
    <xsl:param name="fanned-control-type"/>
    
    <xsl:variable name="fanout" select="Note[@kind='fanout']/@width"/>
    
    <xsl:text>&#10;  </xsl:text>
    <xsl:value-of select="concat( 'signal ', $base-name, '_DATA  : ' )"/>
    <xsl:apply-templates select="Type" mode="type-decl"/>
    <xsl:text>;&#10;  </xsl:text>  
    <xsl:value-of select="concat( 'signal ', $base-name, '_SEND  : ',$fanned-control-type,';' )"/>
    <xsl:text>&#10;  </xsl:text>
    <xsl:value-of select="concat( 'signal ', $base-name, '_ACK   : ',$fanned-control-type,';' )"/>
    <xsl:text>&#10;  </xsl:text>
    <xsl:value-of select="concat( 'signal ', $base-name, '_COUNT : std_logic_vector(15 downto 0);' )"/>
    <xsl:text>&#10;  </xsl:text>
    
    <xsl:if test="@kind='Output' or (not(parent::Actor) and @kind='Input')">
      <xsl:value-of select="concat( 'signal ', $base-name, '_RDY   : ',$fanned-control-type,';' )"/>
      <xsl:text>&#10;</xsl:text>
    </xsl:if>

  </xsl:template>

  <!-- Connect up the actor ports. If fanin/out is zero, tie the inputs to safe values -->  
  <xsl:template match="Port" mode="connect-port">
    
    <xsl:variable name="prefix">
      <xsl:choose>
        <xsl:when test="@kind='Output'">ao_</xsl:when>
        <xsl:otherwise>ai_</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:variable name="tie">
      <xsl:choose>
        <xsl:when test="@kind = 'Input' and Note[@kind='fanin']/@width &gt;= 1">0</xsl:when>
        <xsl:when test="@kind = 'Output' and Note[@kind='fanout']/@width &gt;= 1">0</xsl:when>
        <xsl:otherwise>1</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- terminals in the signal bundle and their tie-off value for fanout=0 -->     
    <xsl:variable name="terminals">
      <xsl:choose>
        <xsl:when test="@kind='Output'">
          <terminal name="_DATA " tie="open"/>
          <terminal name="_SEND " tie="open"/>
          <terminal name="_ACK  " tie="SystemBuilder.sb_types.logic1"/>
          <terminal name="_COUNT" tie="open"/>
          <terminal name="_RDY  " tie="SystemBuilder.sb_types.logic1"/>
        </xsl:when>
        <xsl:otherwise>
          <terminal name="_DATA ">
            <xsl:attribute name="tie">
              <!-- determine whether to tie input to a single 0 or a vector of zeros -->
              <xsl:choose>
                <xsl:when test="Note[@kind='dataWidth']/@width = 1">SystemBuilder.sb_types.logic0</xsl:when>
                <xsl:otherwise>(others =&gt; SystemBuilder.sb_types.logic0)</xsl:otherwise>
              </xsl:choose>
          </xsl:attribute>
          </terminal>
          <terminal name="_SEND " tie="SystemBuilder.sb_types.logic0"/>
          <terminal name="_ACK  " tie="open"/>
          <terminal name="_COUNT" tie="(others =&gt; SystemBuilder.sb_types.logic0)" />          
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:variable name="name" select="@name"/>
    <xsl:variable name="instance" select="../../@instance-name"/>
    
    <xsl:for-each select="$terminals/terminal">
      <xsl:value-of select="concat( '    ', $name, @name, ' => ' )"/>
      
      <xsl:choose>
        <xsl:when test="$tie = 1">
          <xsl:value-of select="@tie"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat( $prefix, $instance, '_', $name, @name )"/>
        </xsl:otherwise>
      </xsl:choose>
      
      <xsl:text>,&#10;</xsl:text>
    </xsl:for-each>
    <xsl:text>&#10;</xsl:text>
    
    <!-- finally, connect up the actor clock and reset -->    
    <xsl:if test="position() = last()">
      <xsl:variable name="clock">
        <xsl:apply-templates select="." mode="find-clock-domain"/>
      </xsl:variable>
      <xsl:text>    CLK   => clocks(</xsl:text><xsl:value-of select="$clock - 1"/>
      <xsl:text>),&#10;    RESET => resets(</xsl:text><xsl:value-of select="$clock - 1"/><xsl:text>)&#10;</xsl:text>
    </xsl:if>
    
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
  
  <!-- Generate the port declaration for the top-level entity or an actor component --> 
  <xsl:template match="Port" mode="port-decl">
    
   <xsl:choose>
      
     <xsl:when test="@kind = 'Output'">
       <xsl:text>&#10;    </xsl:text>
       <xsl:value-of select="@name"/><xsl:text>_DATA  : out </xsl:text>
       <xsl:apply-templates select="Type" mode="type-decl"/><xsl:text>;&#10;    </xsl:text>
       
       <xsl:value-of select="@name"/><xsl:text>_SEND  : out std_logic;&#10;    </xsl:text>
       <xsl:value-of select="@name"/><xsl:text>_ACK   : in  std_logic;&#10;    </xsl:text>
       <xsl:value-of select="@name"/><xsl:text>_COUNT : out std_logic_vector(15 downto 0);&#10;    </xsl:text>
       <xsl:value-of select="@name"/><xsl:text>_RDY   : in  std_logic;&#10;</xsl:text>
     </xsl:when>	    
     
     <xsl:otherwise>
       <xsl:text>&#10;    </xsl:text>
       <xsl:value-of select="@name"/><xsl:text>_DATA  : in  </xsl:text>
       <xsl:apply-templates select="Type" mode="type-decl"/><xsl:text>;&#10;    </xsl:text>
       
       <xsl:value-of select="@name"/><xsl:text>_SEND  : in  std_logic;&#10;    </xsl:text>
       <xsl:value-of select="@name"/><xsl:text>_ACK   : out std_logic;&#10;    </xsl:text>
       <xsl:value-of select="@name"/><xsl:text>_COUNT : in  std_logic_vector(15 downto 0);&#10;    </xsl:text>
       <!-- Network input ports generate "RDY" outputs, but Actor inputs do not -->
       <xsl:if test="parent::XDF"><xsl:value-of select="@name"/><xsl:text>_RDY   : out std_logic;&#10;    </xsl:text></xsl:if>
     </xsl:otherwise>
     
    </xsl:choose>

    <!-- When iterating over a list of ports, add the control signals at the end of the list -->    
    <xsl:if test="position() = last()">
      <xsl:text>&#10;</xsl:text>
      <xsl:choose>
        
        <!-- Actors have a single clock -->
        <xsl:when test="parent::Actor">
          <xsl:text>    CLK: in std_logic;&#10;</xsl:text>
        </xsl:when>
        
        <!-- Network must have a clock input for each domain -->
        <xsl:otherwise>
          <xsl:for-each select="$clockDomains/clock">
            <xsl:text>    </xsl:text><xsl:value-of select="@name"/><xsl:text>: in std_logic;&#10;</xsl:text>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>           
      <xsl:text>    RESET: in std_logic </xsl:text>
    </xsl:if>
    
  </xsl:template>

  <!-- Instantiate the queue before actor inputs or network outputs -->
  <xsl:template match="Port" mode="queue">    

    <!-- find the connection that ends at this port -->  
    <xsl:variable name="c" select=" if( @kind='Input' ) then 
       ../../../Connection[@dst=current()/../../@id][@dst-port=current()/@name ] else
       ../Connection[@dst=''][@dst-port=current()/@name ]"/>
    
    <!-- All internal signals are represented as vectors. In the case of length 1 data types
        we have to use an indexer to change from std_logic_vector to std_logic -->
    <xsl:variable name="indexer">
      <xsl:choose>
        <xsl:when test="Note[@kind='dataWidth']/@width = 1">
          <xsl:text>(0)</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <!-- get the base name of the signals driving this queue, ie the fanned output -->
    <xsl:variable name="src-base-name">
      <xsl:choose>
        
        <!-- Source for this connection is an input port -->
        <xsl:when test="$c/@src = ''">
          <xsl:text>nif_</xsl:text>
          <xsl:value-of select="$c/@src-port"/>
        </xsl:when>
        
        <!-- This an actor input driven by an actor output -->
        <xsl:when test="@kind='Input'">
          <xsl:text>aof_</xsl:text>
          <xsl:value-of select="../../../Instance[@id=$c/@src]/@instance-name"/>
          <xsl:text>_</xsl:text><xsl:value-of select="$c/@src-port"/>
        </xsl:when>

        <!-- This an network output driven by an actor output -->
        <xsl:otherwise>
          <xsl:text>aof_</xsl:text>
          <xsl:value-of select="../Instance[@id=$c/@src]/@instance-name"/>
          <xsl:text>_</xsl:text><xsl:value-of select="$c/@src-port"/>
        </xsl:otherwise>
        
      </xsl:choose>
    </xsl:variable>

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

    <!-- Get the fanout index -->    
    <xsl:variable name="n" select="count($c/preceding-sibling::Connection[@src=$c/@src][@src-port=$c/@src-port])"/>

    <!-- FIXME there should be a Note on output ports as well -->
    <xsl:variable name="bufferSize">
      <xsl:choose>
        <xsl:when test="Note[@kind='bufferSize']">
          <xsl:value-of select="Note[@kind='bufferSize']/@size"/>
        </xsl:when>
        <xsl:when test="$c/Attribute[@name='bufferSize']">
          <xsl:value-of select="$c/Attribute[@name='bufferSize']/@value"/>
        </xsl:when>
        <xsl:otherwise>1</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
        
    <xsl:text>&#10;  q_</xsl:text><xsl:value-of select="$dst-base-name"/><xsl:text> : entity </xsl:text>
    <xsl:value-of select="$libPackage"/>
    <xsl:value-of select="if( $src-clock = $dst-clock ) then '.Queue' else '.Queue_Async'"/>
    <xsl:text>( behavioral )&#10;  generic map( length => </xsl:text>
    <xsl:value-of select="$bufferSize"/>
    <xsl:text>, width => </xsl:text><xsl:value-of select="Note[@kind='dataWidth']/@width"/>
    <xsl:text> )&#10;  port map (&#10;</xsl:text>
    <xsl:text>    Out_DATA</xsl:text><xsl:value-of select="$indexer"/>
    <xsl:text>  => </xsl:text><xsl:value-of select="$dst-base-name"/>
    <xsl:text>_DATA,&#10;    Out_SEND  => </xsl:text><xsl:value-of select="$dst-base-name"/>
    <xsl:text>_SEND,&#10;    Out_ACK   => </xsl:text><xsl:value-of select="$dst-base-name"/>
    <xsl:text>_ACK,&#10;    Out_COUNT => </xsl:text><xsl:value-of select="$dst-base-name"/>
    
    <xsl:text>_COUNT,&#10;&#10;    In_DATA</xsl:text><xsl:value-of select="$indexer"/>
    <xsl:text>  => </xsl:text><xsl:value-of select="$src-base-name"/>
    <xsl:text>_DATA,&#10;    In_SEND  => </xsl:text><xsl:value-of select="$src-base-name"/>
    <xsl:text>_SEND( </xsl:text><xsl:value-of select="$n"/>
    <xsl:text> ),&#10;    In_ACK   => </xsl:text><xsl:value-of select="$src-base-name"/>
    <xsl:text>_ACK( </xsl:text><xsl:value-of select="$n"/>
    <xsl:text> ),&#10;    In_COUNT => </xsl:text><xsl:value-of select="$src-base-name"/>
    <xsl:text>_COUNT,&#10;    In_RDY   => </xsl:text><xsl:value-of select="$src-base-name"/>
    <xsl:text>_RDY( </xsl:text><xsl:value-of select="$n"/><xsl:text> ),&#10;</xsl:text>

    <xsl:choose>
      <!-- Sync fifo case -->
      <xsl:when test="$src-clock = $dst-clock">
        <xsl:text>&#10;    clk => clocks(</xsl:text><xsl:value-of select="$dst-clock - 1"/>
        <xsl:text>),&#10;    reset => resets(</xsl:text><xsl:value-of select="$dst-clock - 1"/>
        <xsl:text>)&#10;</xsl:text>
      </xsl:when>
      
      <!-- Async fifo case -->
      <xsl:otherwise>
        <xsl:text>&#10;    clk_i => clocks(</xsl:text><xsl:value-of select="$src-clock - 1"/>
        <xsl:text>),&#10;    reset_i => resets(</xsl:text><xsl:value-of select="$src-clock - 1"/>
        <xsl:text>),&#10;    clk_o => clocks(</xsl:text><xsl:value-of select="$dst-clock - 1"/>
        <xsl:text>),&#10;    reset_o => resets(</xsl:text><xsl:value-of select="$dst-clock - 1"/>
        <xsl:text>)&#10;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>    

    <xsl:text>  );&#10;</xsl:text>
    
  </xsl:template>
 
  <!-- Instantiate a fanout block for an actor output or a network input -->
  <xsl:template match="Port" mode="fanout">
    
     <xsl:variable name="indexer">
      <xsl:choose>
        <xsl:when test="Note[@kind='dataWidth']/@width = 1">
          <xsl:text>(0)</xsl:text>
        </xsl:when>
      </xsl:choose>
     </xsl:variable>
     
    <xsl:variable name="in">
      <xsl:choose>
        
        <!-- network input fanout -->
        <xsl:when test="@kind='Input'">
          <xsl:text>ni_</xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:when>
        
        <!-- actor output fanout -->
        <xsl:otherwise>
          <xsl:text>ao_</xsl:text>
          <xsl:value-of select="../../@instance-name"/>
          <xsl:text>_</xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
        
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="out">
      <xsl:choose>
        
        <!-- network input fanout -->
        <xsl:when test="@kind='Input'">
          <xsl:text>nif_</xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:when>
        
        <!-- actor output fanout -->
        <xsl:otherwise>
          <xsl:text>aof_</xsl:text>
          <xsl:value-of select="../../@instance-name"/>
          <xsl:text>_</xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:otherwise>
        
      </xsl:choose>
    </xsl:variable>
    
    <xsl:variable name="clock">
      <xsl:apply-templates select="." mode="find-clock-domain"/>
    </xsl:variable>

    <xsl:text>&#10;  f_</xsl:text><xsl:value-of select="$in"/>
    <xsl:text> : entity </xsl:text><xsl:value-of select="$libPackage"/>
    <xsl:text>.Fanout( behavioral )&#10;  generic map ( </xsl:text>
    <xsl:text>fanout => </xsl:text><xsl:value-of select="Note[@kind='fanout']/@width"/>
    <xsl:text>, width => </xsl:text><xsl:value-of select="Note[@kind='dataWidth']/@width"/>
    <xsl:text> )&#10;  port map (&#10;</xsl:text>
    
    <xsl:text>    In_DATA</xsl:text><xsl:value-of select="$indexer"/>
    <xsl:text>  => </xsl:text><xsl:value-of select="$in"/>
    <xsl:text>_DATA,&#10;    In_SEND  => </xsl:text><xsl:value-of select="$in"/>
    <xsl:text>_SEND,&#10;    In_ACK   => </xsl:text><xsl:value-of select="$in"/>
    <xsl:text>_ACK,&#10;    In_COUNT => </xsl:text><xsl:value-of select="$in"/>
    <xsl:text>_COUNT,&#10;    In_RDY   => </xsl:text><xsl:value-of select="$in"/>
    <xsl:text>_RDY,&#10;</xsl:text>
    
    <xsl:text>&#10;    Out_DATA</xsl:text><xsl:value-of select="$indexer"/><xsl:text>  => </xsl:text>
    <xsl:value-of select="$out"/>
    <xsl:text>_DATA,&#10;    Out_SEND  => </xsl:text><xsl:value-of select="$out"/>
    <xsl:text>_SEND,&#10;    Out_ACK   => </xsl:text><xsl:value-of select="$out"/>
    <xsl:text>_ACK,&#10;    Out_COUNT => </xsl:text><xsl:value-of select="$out"/>
    <xsl:text>_COUNT,&#10;    Out_RDY   => </xsl:text><xsl:value-of select="$out"/>
    <xsl:text>_RDY,&#10;</xsl:text>
    
    <xsl:text>&#10;    CLK   => clocks(</xsl:text><xsl:value-of select="$clock - 1"/>
    <xsl:text>),&#10;    RESET => resets(</xsl:text><xsl:value-of select="$clock - 1"/>
    <xsl:text>)&#10;  );&#10;</xsl:text>
    
  </xsl:template>

  <xsl:template name="ila-declarations">
    
    <xsl:if test=".//HDL[@id='ila']">
      signal icon_control : std_logic_vector(35 downto 0);
      
      function ila_vectorize(s: std_logic) return std_logic_vector is
      variable v: std_logic_vector(0 downto 0);
      begin
      v(0) := s;
      return v;
      end;
      
      function ila_vectorize(s: std_logic_vector) return std_logic_vector is
      begin
      return s;
      end;
    </xsl:if>
    
    
    <xsl:if test=".//HDL[@id='ila']">
      component icon is
      port ( control0 : out std_logic_vector(35 downto 0) );
      end component icon;
      
      attribute syn_black_box : boolean;
      attribute syn_black_box of icon : component is TRUE;
      attribute syn_noprune : boolean;
      attribute syn_noprune of icon : component is TRUE;
    </xsl:if>
    
    <xsl:for-each select="HDL">
      component <xsl:value-of select="@instance-name"/>
      port (
      <xsl:for-each select="Port[@kind='Output']">
        <xsl:value-of select="@name"/> : out <xsl:apply-templates select="Type" mode="type-decl"/>;
      </xsl:for-each>
      
      <xsl:for-each select="Port[@kind='Input' and @type='tap']">
        <xsl:variable name="index" select="(position()-1)*4"/>
        <!-- trig<xsl:value-of select="$index"/> : in <xsl:apply-templates mode="type"/>; -->
        trig<xsl:value-of select="$index"/> : in std_logic_vector(<xsl:value-of select="Note[@kind='dataWidth']/@width - 1"/> downto 0);
        trig<xsl:value-of select="$index + 1"/> : in std_logic_vector(0 downto 0);
        trig<xsl:value-of select="$index + 2"/> : in std_logic_vector(0 downto 0);
        trig<xsl:value-of select="$index + 3"/> : in std_logic_vector(0 downto 0);
      </xsl:for-each>
      <xsl:if test="count(Port[@kind='Input' and @type='queueDebug']) &gt; 0">
        <xsl:variable name="index" select="(count(Port[@kind='Input' and @type='queueDebug'])*2)-1"/>
        <!-- trig<xsl:value-of select="$index"/> : in <xsl:apply-templates mode="type"/>; -->
        trig0 : in std_logic_vector(<xsl:value-of select="$index"/> downto 0);
      </xsl:if>
      
      <xsl:if test="@id='ila'">
        control : in std_logic_vector(35 downto 0);
      </xsl:if>
      clk : in std_logic
      );
      end component <xsl:value-of select="@instance-name"/>;
      <xsl:if test="@id='ila'">
        attribute syn_black_box of <xsl:value-of select="@instance-name"/> : component is TRUE;
        attribute syn_noprune of <xsl:value-of select="@instance-name"/> : component is TRUE;
      </xsl:if>
    </xsl:for-each>
    
  </xsl:template>
    
  <xsl:template name="ila-instantiation">
    
    <xsl:if test=".//HDL[@id='ila']">
      Instance_icon : component icon
      port map (
      control0 => icon_control
      );
    </xsl:if>
    
    <xsl:for-each select="HDL">
      <xsl:variable name="id" select="@id"/>
      Instance_<xsl:value-of select="@instance-name"/> : component <xsl:value-of select="@instance-name"/>
      port map (
      <xsl:for-each select="Port[@kind='Output']">
        <xsl:value-of select="@name"/> => ,
      </xsl:for-each>
      <xsl:for-each select="Port[@kind='Input' and @type='tap']">
        <xsl:variable name="portname" select="@name"/>
        <xsl:variable name="index" select="(position()-1)*4"/>
        <xsl:variable name="conn" select="../../Connection[@dst=$id][@dst-port=$portname][1]"/>
        <xsl:variable name="source" select="../../Instance[@id=$conn/@src]"/>
        <xsl:variable name="prefix"><xsl:if test="$conn/@src != ''">ao_<xsl:value-of select="$source/@instance-name"/>_</xsl:if></xsl:variable>
        <xsl:variable name="rdy"><xsl:choose>
          <xsl:when test="$conn/@src != ''"><xsl:value-of select="$prefix"/><xsl:value-of select="$conn/@src-port"/>_RDY</xsl:when>
          <xsl:otherwise>'1'</xsl:otherwise>
        </xsl:choose></xsl:variable>
        trig<xsl:value-of select="$index"/> => ila_vectorize(<xsl:value-of select="$prefix"/><xsl:value-of select="$conn/@src-port"/>_DATA),
        trig<xsl:value-of select="$index + 1"/> => ila_vectorize(<xsl:value-of select="$prefix"/><xsl:value-of select="$conn/@src-port"/>_SEND),
        trig<xsl:value-of select="$index + 2"/> => ila_vectorize(<xsl:value-of select="$prefix"/><xsl:value-of select="$conn/@src-port"/>_ACK),
        trig<xsl:value-of select="$index + 3"/> => ila_vectorize(<xsl:value-of select="$rdy"/>),
      </xsl:for-each>
      <xsl:for-each select="Port[@kind='Input' and @type='queueDebug']">
        <xsl:variable name="portname" select="@name"/>
        <xsl:variable name="index" select="(position()-1)*2"/>
        <xsl:variable name="src" select="@src"/>
        <xsl:variable name="dst" select="@dst"/>
        <xsl:variable name="dst-port" select="@dst-port"/>
        <xsl:variable name="source" select="../../Instance[@id=$src]"/>
        <xsl:variable name="dest" select="../../Instance[@id=$dst]"/>
        <xsl:variable name="conn" select="../../Connection[@dst=$dst][@dst-port=$dst-port]"/>
        <xsl:variable name="n" select="count($conn/preceding-sibling::Connection[@src=$conn/@src][@src-port=$conn/@src-port])"/>
        <xsl:variable name="prefix1"><xsl:choose><xsl:when test="$src != ''">aof_<xsl:value-of select="$source/@instance-name"/>_</xsl:when><xsl:otherwise>sigfannedinput_</xsl:otherwise></xsl:choose></xsl:variable>
        <xsl:variable name="prefix2"><xsl:if test="$dst != ''">ai_<xsl:value-of select="$dest/@instance-name"/>_</xsl:if></xsl:variable>
        trig0 (<xsl:value-of select="$index"/>) => <xsl:value-of select="$prefix2"/><xsl:value-of select="@dst-port"/>_SEND,
        trig0 (<xsl:value-of select="$index + 1"/>) => <xsl:value-of select="$prefix1"/><xsl:value-of select="@src-port"/>_RDY ( <xsl:value-of select="$n"/> ),
      </xsl:for-each>
      <xsl:if test="@id='ila'">
        control => icon_control,
      </xsl:if>
      clk => clocks(
      <!-- this is probably wrong ... good luck Ian -->
      <xsl:apply-templates select="." mode="find-clock-domain"/> - 1     
      )
      );
    </xsl:for-each>

  </xsl:template>

  <xsl:template name="clock-domain-report">
    
    <xsl:text>&#10;-- ----------------------------------------------------------------------------------&#10;</xsl:text> 
    <xsl:text>--&#10;-- Clock domain report for network </xsl:text><xsl:value-of select="@name"/>
    <xsl:text> (</xsl:text><xsl:value-of select="count($clockDomains/clock)"/>
    <xsl:text> clock domains detected)</xsl:text>
    <xsl:for-each select="$clockDomains/clock">
      <xsl:text>&#10;--    </xsl:text><xsl:value-of select="@name"/>
    </xsl:for-each>
    
    <xsl:text>&#10;--&#10;--  Clock domains for top-level network ports:</xsl:text>
    <xsl:for-each select="/XDF/Port">
      <xsl:text>&#10;--    </xsl:text><xsl:value-of select="@name"/>
      <xsl:text> (</xsl:text><xsl:value-of select="@kind"/><xsl:text>) --> </xsl:text>
      <xsl:variable name="index"><xsl:apply-templates select="." mode="find-clock-domain"/></xsl:variable>
      <xsl:value-of select="$clockDomains/clock[position() = $index]/@name"/>
    </xsl:for-each>
    
    <xsl:text>&#10;--&#10;--  Clock domains for all actor instances:</xsl:text>
    <xsl:for-each select="/XDF/Instance">
      <xsl:text>&#10;--    </xsl:text><xsl:value-of select="@instance-name"/>
      <xsl:text> (</xsl:text><xsl:value-of select="Class/@name"/><xsl:text>) --> </xsl:text>
      <xsl:variable name="index"><xsl:apply-templates select="Actor/Port[1]" mode="find-clock-domain"/></xsl:variable>
      <xsl:value-of select="$clockDomains/clock[position() = $index]/@name"/>
    </xsl:for-each>
    <xsl:text>&#10;--&#10;-- ----------------------------------------------------------------------------------&#10;</xsl:text> 
    
  </xsl:template>  
  
</xsl:stylesheet>



