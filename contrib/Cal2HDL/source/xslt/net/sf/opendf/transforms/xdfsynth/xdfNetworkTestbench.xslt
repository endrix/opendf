
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    version="2.0">
    
    <xsl:output method="text"/>
    <xsl:output method="text" name="textFormat"/>
    <xsl:strip-space elements="*"/>
    
    <xd:doc type="stylesheet">
        <xd:author>IDM</xd:author>
        <xd:copyright>Xilinx, 2007</xd:copyright>
    </xd:doc>

    <xsl:include href="net/sf/opendf/transforms/xdfsynth/hdlUtil.xslt"/>

    <!-- A list of all the clock domains used by this network  -->    
    <xsl:variable name="clockDomains">
      <xsl:variable name="root" select="/XDF"/>
      <xsl:variable name="names">
        <xsl:call-template name="findClockDomains">
          <xsl:with-param name="root" select="$root"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:for-each select="$names/*">
        <xsl:variable name="domainName" select="@name"/>
        <xsl:copy>
          <xsl:for-each select="@*"><xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute></xsl:for-each>
          <inputs>
            <xsl:copy-of select="$root/Port[@kind='Input'][Attribute[@name='clockDomain' and @value=$domainName]]"/>
            <xsl:if test="position() = count($names/*)">
              <xsl:copy-of select="$root/Port[@kind='Input'][not(Attribute[@name='clockDomain'])]"/>
            </xsl:if>
          </inputs>
          <outputs>
            <xsl:copy-of select="$root/Port[@kind='Output'][Attribute[@name='clockDomain' and @value=$domainName]]"/>
            <xsl:if test="position() = count($names/*)">
              <xsl:copy-of select="$root/Port[@kind='Output'][not(./Attribute[@name='clockDomain'])]"/>
            </xsl:if>
          </outputs>
        </xsl:copy>
      </xsl:for-each>
    </xsl:variable>
    
    <xsl:template match="XDF">
      <xsl:variable name="root" select="."/>
    <xsl:variable name="fixtureName"><xsl:value-of select="@name"/>_fixture</xsl:variable>
`timescale 1ns/1ps
`define legacy_model // Some simulators cannot handle the syntax of the new memory models.

module <xsl:value-of select="$fixtureName"/>();
  // Define VERIFY_TOKEN_DATA if the internal queues should verify their data against vector files
  `undef VERIFY_TOKEN_DATA
  <xsl:for-each select="$clockDomains/clock">
  reg        <xsl:value-of select="@name"/>;</xsl:for-each>
  reg        reset;
  reg        LGSR;
  reg        startSimulation;
  <xsl:for-each select="$clockDomains/clock">
  reg        run_<xsl:value-of select="@name"/>, run1_<xsl:value-of select="@name"/>, run2_<xsl:value-of select="@name"/>;</xsl:for-each>
  integer    resultFile;
  reg [31:0] hangTimer;
  <xsl:for-each select="$clockDomains/clock">
  reg [31:0] clockCount_<xsl:value-of select="@name"/>;</xsl:for-each>
<xsl:apply-templates select="./Port" mode="declarePorts"/>  

  //always #25 clk = ~clk;
  <xsl:for-each select="$clockDomains/clock">
  always #25 <xsl:value-of select="@name"/> = ~<xsl:value-of select="@name"/>;
  </xsl:for-each>
  // initial begin $dumpfile("waves.vcd"); $dumpvars; end
  assign glbl.GSR=LGSR;

  <xsl:value-of select="@name"/> dut (
  <xsl:for-each select="$clockDomains/clock">
    <xsl:call-template name="connectPorts">
      <xsl:with-param name="ports" select=".//Port"/>
      <xsl:with-param name="domain" select="@name"/>
    </xsl:call-template>
  </xsl:for-each>
  //.CLK(clk),
  <xsl:for-each select="$clockDomains/clock">
  .<xsl:value-of select="@name"/>(<xsl:value-of select="@name"/>),
  </xsl:for-each>
  
  .RESET(reset)
  );
  
  initial begin
    //clk &lt;= 0;
    <xsl:for-each select="$clockDomains/clock">
    <xsl:value-of select="@name"/> &lt;= 0;
    </xsl:for-each>
    reset &lt;= 0;
    hangTimer &lt;= 0;
    startSimulation &lt;= 0;
    <xsl:for-each select="$clockDomains/clock">
    run_<xsl:value-of select="@name"/> &lt;= 0;
    run1_<xsl:value-of select="@name"/> &lt;= 0;
    run2_<xsl:value-of select="@name"/> &lt;= 0;
    </xsl:for-each>
    //run &lt;= 0;
    //run1 &lt;= 0;
    //run2 &lt;= 0;
    //clockCount &lt;= 0;
    <xsl:for-each select="$clockDomains/clock">
    clockCount_<xsl:value-of select="@name"/> &lt;= 0;
    </xsl:for-each>
		resultFile &lt;= $fopen("./<xsl:value-of select="@name"/>_network_sim.results");
    <xsl:apply-templates select="./Port" mode="initPorts">
      <xsl:with-param name="basename" select="@name"/>
    </xsl:apply-templates>
    LGSR &lt;= 1;
    #1 LGSR &lt;= 0;
    #100 reset &lt;= 1;
    #200 reset &lt;= 0;
    #500 startSimulation &lt;= 1;
  end

  <xsl:for-each select="$clockDomains/clock">
    <xsl:variable name="clockName" select="@name"/>
    always @(posedge <xsl:value-of select="@name"/>) begin
      run_<xsl:value-of select="@name"/> &lt;= startSimulation;
      run1_<xsl:value-of select="@name"/> &lt;= run_<xsl:value-of select="@name"/>;
      run2_<xsl:value-of select="@name"/> &lt;= run1_<xsl:value-of select="@name"/>;
      <xsl:for-each select="./outputs/*">
        <xsl:value-of select="@name"/>_ack &lt;= run_<xsl:value-of select="$clockName"/>;
      </xsl:for-each>
      <xsl:if test="count(.//Port) &gt; 0">
      if (run_<xsl:value-of select="$clockName"/>) begin
      <xsl:apply-templates select="./inputs/*" mode="operatePorts"/>
      <xsl:apply-templates select="./outputs/*" mode="operatePorts"/>
      end // if(run)
      </xsl:if>
    end
  </xsl:for-each>

  <xsl:for-each select="$clockDomains/clock">
    <xsl:variable name="domain" select="."/>
   <xsl:if test="count($domain/inputs/*) &gt; 0">
    always @(negedge <xsl:value-of select="@name"/>) begin
      <xsl:for-each select="$domain/inputs/*">$markOneActionFiring(<xsl:value-of select="@name"/>_id);</xsl:for-each>
    end
   </xsl:if>
    <!--  Skip output actions so that we do not need to load the entire file (no MARK lines exist in output files)
         $markActionFiring(); // mark all actions fired every cycle to facilitate saturation of inputs -->

    always @(posedge <xsl:value-of select="@name"/>) begin
      clockCount_<xsl:value-of select="@name"/> &lt;= clockCount_<xsl:value-of select="@name"/> + 1;
      if ( <xsl:for-each select="$domain/inputs/*"><xsl:value-of select="@name"/>_ack || </xsl:for-each>
           <xsl:for-each select="$domain/outputs/*"><xsl:value-of select="@name"/>_send || </xsl:for-each>
      1'b0) begin
        hangTimer &lt;= 0; // Clearly not synthesizable, but effective.
      end <xsl:if test="position() = 1"> else begin
        hangTimer &lt;= hangTimer + 1;
        if (hangTimer > 5000 ) begin
				  $display(<xsl:call-template name="reportCycles"><xsl:with-param name="prefix" select="'FAIL: Hang Timer expired after'"/></xsl:call-template>);
				  $fwrite (resultFile, <xsl:call-template name="reportCycles"><xsl:with-param name="prefix" select="'FAIL: Hang Timer expired after'"/></xsl:call-template>);
          $fwrite (resultFile, "\tPortName : TokenCount\n");
          <xsl:apply-templates select="$clockDomains//Port" mode="reportCounts"/>
          $finish;
        end
      end
      </xsl:if>
    end
  </xsl:for-each>


  <xsl:for-each select="$clockDomains/clock[1]"> <!-- default to the first clock domain for the decision -->
    always @(posedge <xsl:value-of select="@name"/>) begin
      if (<xsl:for-each select="$clockDomains/*/inputs/*">$isQueueEmpty(<xsl:value-of select="@name"/>_id)<xsl:if test="not(position() = last())"> &amp;&amp; </xsl:if></xsl:for-each> ) begin
        //$fwrite(resultFile, "PASSED (end of input) in %d cycles(<xsl:value-of select="@name"/>)\n", clockCount_<xsl:value-of select="@name"/>);
        $fwrite(resultFile, <xsl:call-template name="reportCycles"><xsl:with-param name="prefix" select="'PASSED (end of input) in'"/></xsl:call-template>);
        $display("PASSED (end of input)");
        $finish;
      end
    end
    
    always @(posedge <xsl:value-of select="@name"/>) begin
      if (
        <xsl:for-each select="$clockDomains/*/outputs/Port[not(Note[@kind='internalPort' and @value='true'])]">
          $isQueueEmpty(<xsl:value-of select="@name"/>_id)<xsl:if test="not(position() = last())"> &amp;&amp; </xsl:if>
        </xsl:for-each>
        ) begin
        $fwrite(resultFile, <xsl:call-template name="reportCycles"><xsl:with-param name="prefix" select="'PASSED (end of output) in'"/></xsl:call-template>);
        $display("PASSED (end of input)");
        $finish;
      end
    end

    always @(posedge <xsl:value-of select="@name"/>) begin
      if ((clockCount_<xsl:value-of select="@name"/> % 4095) == 0) begin
        $display(<xsl:call-template name="reportCycles"><xsl:with-param name="prefix" select="'Total tokens transferred'"/></xsl:call-template>);
        <xsl:for-each select="$clockDomains/clock">
          <xsl:variable name="clkName" select="@name"/>
          <xsl:for-each select=".//Port">
            <xsl:variable name="count"><xsl:value-of select="@name"/><xsl:if test="@kind='Input'">_din</xsl:if><xsl:if test="@kind='Output'">_dout</xsl:if>_count</xsl:variable>
            $display("%d tokens transferred on <xsl:value-of select="@name"/> (<xsl:value-of select="$clkName"/>)", <xsl:value-of select="$count"/>);</xsl:for-each>
        </xsl:for-each>
      end
    end
  </xsl:for-each>

  
endmodule

    <xsl:variable name="simFile"><xsl:value-of select="@name"/>_sim.sh</xsl:variable>
    <xsl:result-document href="{$simFile}" format="textFormat">
rm -rf systembuilder
rm -rf work
vlib systembuilder
vlib work
vcom -work systembuilder lib/sbtypes.vhdl lib/sbfifo.vhdl lib/sbfifo_behavioral.vhdl
vlog ${XILINX}/verilog/src/glbl.v
vcom <xsl:value-of select="@name"/>_sim.vhd
vlog <xsl:for-each select="Instance">Actors/<xsl:value-of select="@instance-name"/>.v </xsl:for-each>
vlog <xsl:value-of select="$fixtureName"/>.v 
vlog ${XILINX}/verilog/src/unisims/FD*.v
vlog ${XILINX}/verilog/src/unisims/RAM*.v
vsim -t ps -f vhdl.sim <xsl:value-of select="$fixtureName"/> glbl
    </xsl:result-document>
    
    </xsl:template>



    <xsl:template match="Port" mode="declarePorts">
  <xsl:variable name="size">
    <xsl:choose>
      <xsl:when test="./Type[@name='bool']">1</xsl:when>
      <xsl:when test="./Type[@name='int']"><xsl:value-of select="./Type/Entry[@kind='Expr' and @name='size']/Expr[@kind='Literal']/@value"/></xsl:when>
    </xsl:choose>
  </xsl:variable>
<xsl:choose>
  <xsl:when test="@kind='Input'">
reg  [<xsl:value-of select="$size - 1"/>:0] <xsl:value-of select="@name"/>_din;
wire        <xsl:value-of select="@name"/>_ack;
reg         <xsl:value-of select="@name"/>_send;
reg  [31:0] <xsl:value-of select="@name"/>_din_count;
integer     <xsl:value-of select="@name"/>_id;
  </xsl:when>
  <xsl:when test="@kind='Output'">
wire [<xsl:value-of select="$size - 1"/>:0] <xsl:value-of select="@name"/>_dout;
wire        <xsl:value-of select="@name"/>_send;
reg         <xsl:value-of select="@name"/>_ack;
reg  [<xsl:value-of select="$size - 1"/>:0] <xsl:value-of select="@name"/>_dout_expected;
reg         <xsl:value-of select="@name"/>_expected_exists;
reg  [31:0] <xsl:value-of select="@name"/>_dout_count;
integer     <xsl:value-of select="@name"/>_id;
  </xsl:when>
</xsl:choose>

    </xsl:template>




    
    <xsl:template name="connectPorts">
      <xsl:param name="ports" select="_undefined_"/>
      <xsl:param name="domain" select="_undefined_"/>

      <xsl:for-each select="$ports">
      <xsl:variable name="dir">
        <xsl:choose>
          <xsl:when test="@kind='Input'">din</xsl:when>
          <xsl:when test="@kind='Output'">dout</xsl:when>
        </xsl:choose>
      </xsl:variable>
  .<xsl:value-of select="@name"/>_DATA(<xsl:value-of select="@name"/>_<xsl:value-of select="normalize-space($dir)"/>),
  .<xsl:value-of select="@name"/>_SEND(<xsl:value-of select="@name"/>_send),
  .<xsl:value-of select="@name"/>_ACK(<xsl:value-of select="@name"/>_ack),
  /* .<xsl:value-of select="@name"/>_COUNT(), */
  <xsl:if test="@kind='Output'">
    .<xsl:value-of select="@name"/>_RDY(run2_<xsl:value-of select="$domain"/>), 
  </xsl:if>
      </xsl:for-each>
    </xsl:template>





    
    <xsl:template match="Port" mode="initPorts">
      <xsl:param name="basename"/>
<xsl:choose>
  <xsl:when test="@kind='Input'">
    <xsl:value-of select="@name"/>_din_count &lt;= 0;
    <xsl:value-of select="@name"/>_id &lt;= $registerVectorFile("<xsl:value-of select="$basename"/>VecFiles/<xsl:value-of select="@name"/>.vec", <xsl:value-of select="@name"/>_din, <xsl:value-of select="@name"/>_send);
    <xsl:value-of select="@name"/>_send &lt;= 0;
    <xsl:value-of select="@name"/>_din &lt;= 0;
  </xsl:when>
  <xsl:when test="@kind='Output'">
    <xsl:choose>
      <xsl:when test="./Note[@kind='internalPort' and @value='true']">
    `ifdef VERIFY_TOKEN_DATA
    <xsl:value-of select="@name"/>_id &lt;= $registerVectorFile("<xsl:value-of select="$basename"/>VecFiles/<xsl:value-of select="@name"/>.vec", <xsl:value-of select="@name"/>_dout_expected, <xsl:value-of select="@name"/>_expected_exists);
    `endif
      </xsl:when>
      <xsl:otherwise>
    // The regular (non internal queue) outputs need to be registered in order to detect end of simulation
    <xsl:value-of select="@name"/>_id &lt;= $registerVectorFile("<xsl:value-of select="$basename"/>VecFiles/<xsl:value-of select="@name"/>.vec", <xsl:value-of select="@name"/>_dout_expected, <xsl:value-of select="@name"/>_expected_exists);
      </xsl:otherwise>
    </xsl:choose>
    <xsl:value-of select="@name"/>_dout_count &lt;= 0;
    <xsl:value-of select="@name"/>_expected_exists &lt;= 0;
    <xsl:value-of select="@name"/>_dout_expected &lt;= 0;
    <xsl:value-of select="@name"/>_ack &lt;= 0;
  </xsl:when>
</xsl:choose>
    </xsl:template>
    

    <xsl:template match="Port" mode="operatePorts">
      <xsl:choose>
        <xsl:when test="@kind='Input'">
        if (<xsl:value-of select="@name"/>_ack) begin
          if (!<xsl:value-of select="@name"/>_send) begin
            $fwrite(resultFile, "FAIL: Illegal read from empty queue, port <xsl:value-of select="@name"/> on %d token\n", <xsl:value-of select="@name"/>_din_count);
            #100 $finish;
          end
          <xsl:value-of select="@name"/>_din_count &lt;= <xsl:value-of select="@name"/>_din_count + 1;
          $vectorPop(<xsl:value-of select="@name"/>_id);
        end else begin
          $vectorPeek(<xsl:value-of select="@name"/>_id);
        end
        </xsl:when>
        <xsl:when test="@kind='Output'">
        `ifdef VERIFY_TOKEN_DATA
        if (<xsl:value-of select="@name"/>_send) begin
          if (<xsl:value-of select="@name"/>_ack &amp;&amp; (!<xsl:value-of select="@name"/>_expected_exists)) begin
            $fwrite(resultFile, "FAIL: Token output from port <xsl:value-of select="@name"/> when no output was expected.  Output token %x at count %d\n", <xsl:value-of select="@name"/>_dout, <xsl:value-of select="@name"/>_dout_count);
            #100 $finish;
          end else if (<xsl:value-of select="@name"/>_dout !== <xsl:value-of select="@name"/>_dout_expected) begin
            $fwrite(resultFile, "FAIL: Incorrect result on port <xsl:value-of select="@name"/>.  output token count %d expected %d found %d\n", <xsl:value-of select="@name"/>_dout_count, <xsl:value-of select="@name"/>_dout_expected, <xsl:value-of select="@name"/>_dout);
           #100 $finish;
          end
          <xsl:value-of select="@name"/>_dout_count &lt;= <xsl:value-of select="@name"/>_dout_count + 1;
          $vectorPop(<xsl:value-of select="@name"/>_id);
        end else begin
          $vectorPeek(<xsl:value-of select="@name"/>_id);
        end
        `else
        if (<xsl:value-of select="@name"/>_send) begin
          <xsl:value-of select="@name"/>_dout_count &lt;= <xsl:value-of select="@name"/>_dout_count + 1;
        end
        `endif
        </xsl:when>
      </xsl:choose>
    </xsl:template>


    <xsl:template match="Port" mode="reportCounts">
      <xsl:variable name="dir">
        <xsl:choose>
          <xsl:when test="@kind='Input'">din</xsl:when>
          <xsl:when test="@kind='Output'">dout</xsl:when>
        </xsl:choose>
      </xsl:variable>
<xsl:text>$fwrite (resultFile, "\t</xsl:text><xsl:value-of select="@name"/> %d\n", <xsl:value-of select="@name"/>_<xsl:value-of select="normalize-space($dir)"/>_count);
    </xsl:template>

    <xsl:template name="reportCycles">
      <xsl:param name="prefix" select="''"/>
      "<xsl:value-of select="$prefix"/> in <xsl:for-each select="$clockDomains/clock">%d (<xsl:value-of select="@name"/>) </xsl:for-each> cycles", <xsl:for-each select="$clockDomains/clock">clockCount_<xsl:value-of select="@name"/><xsl:if test="position() &lt; count($clockDomains/clock)">, </xsl:if></xsl:for-each>
    </xsl:template>
    
    
</xsl:stylesheet>