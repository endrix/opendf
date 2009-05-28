<!--
    ProduceSingleTokenForm
	
	Canonicalizes the actor to one that has the following properties:
	(1) No action consumes more than one input token on each port.
	(2) No guard depends on any input token.
	
	assumes: CanonicalizePortTags 
	         AnnotatePortTypeInfo 
			 ReplaceConstantRepeat 
			 (AnnotateInputDependencies)
			 DependencyAnnotator
	
	introduces:
			 Operators
	
    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:math="http://exslt.org/math"
  xmlns:fn="http://www.w3.org/2004/10/xpath-functions"
  xmlns:sb="systembuilder"
  extension-element-prefixes="xsl math fn sb"
  version="2.0">
  <xsl:output method="xml"/>


    <xsl:template match="Actor">
		<xsl:variable name="portBuffers">
			<xsl:call-template name="computeInputBuffers">
				<xsl:with-param name="actor" select="."/>
				<xsl:with-param name="bufferedPorts">
					<xsl:call-template name="computeBufferedInputPorts">
						<xsl:with-param name="actor" select="."/>
					</xsl:call-template>
				</xsl:with-param>
			</xsl:call-template>
		</xsl:variable>
		
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
						
			<xsl:for-each select="$portBuffers/buffer">
				<xsl:variable name="port" select="@port"/>
				<xsl:variable name="size" select="@bufferSize"/>
				<xsl:variable name="type" select="Type"/>
				
				<!-- declare counter variable -->
				<Decl kind="Variable" name="$inputBuffer${$port}$counter" assignable="yes">
					<!-- FIXME: make integer type parametric? -->
					<Type name="Integer">
						<Entry kind="Expr" name="minVal">
							<Expr kind="Literal" literal-kind="Integer" value="0"/>
						</Entry>
						<Entry kind="Expr" name="maxVal">
							<Expr kind="Literal" literal-kind="Integer" value="{$size}"/>
						</Entry>						
					</Type>
					<Expr kind="Literal" literal-kind="Integer" value="0"/>
				</Decl>
				
				<!-- declare buffer start index -->
				
				<Decl kind="Variable" name="$inputBuffer${$port}$head" assignable="yes">
					<!-- FIXME: make integer type parametric? -->
					<Type name="Integer">
						<Entry kind="Expr" name="minVal">
							<Expr kind="Literal" literal-kind="Integer" value="0"/>
						</Entry>
						<Entry kind="Expr" name="maxVal">
							<Expr kind="Literal" literal-kind="Integer" value="{$size}"/>
						</Entry>						
					</Type>
					<Expr kind="Literal" literal-kind="Integer" value="0"/>
				</Decl>
				
				<!-- declare buffer -->
				<Decl kind="Variable" name="$inputBuffer${$port}" mutable="yes">
					<Type name="List">
						<Entry kind="Type" name="element">
							<xsl:copy-of select="$type"/>
						</Entry>
						<Entry kind="Expr" name="size">
							<Expr kind="Literal" literal-kind="Integer" value="{$size}"/>
						</Entry>						
					</Type>
				</Decl>

				<!-- generate reader action -->
				<Action>
					<Input kind="Elements" port="{$port}">
						<Decl kind="Input" name="$v"/>
					</Input>
					<Guards>
						<Expr kind="BinOpSeq">
							<Expr kind="Var" name="$inputBuffer${$port}$counter"/>
							<Op name="&lt;="/>
							<Expr kind="Var" name="{$size}"/>
						</Expr>
					</Guards>
					<Stmt kind="Assign" name="$inputBuffer${$port}">
						<Args>
							<!-- do the ringbuffer thing:
								 write to buffer[(head + counter) mod size] --> 
							<Expr kind="BinOpSeq">
								<Expr kind="BinOpSeq">
									<Expr kind="Var" name="$inputBuffer${$port}$head"/>
									<Op name="+"/>
									<Expr kind="Var" name="$inputBuffer${$port}$counter"/>
								</Expr>
								<Op name="mod"/>
								<Expr kind="Literal" literal-kind="Integer" value="{$size}"/>
							</Expr>
						</Args>
						<Expr kind="Var" name="$v"/>
					</Stmt>
					<Stmt kind="Assign" name="$inputBuffer${$port}$counter">
						<Expr kind="BinOpSeq">
							<Expr kind="Var" name="$inputBuffer${$port}$counter"/>
							<Op name="+"/>
							<Expr kind="Literal" literal-kind="Integer" value="1"/>
						</Expr>
					</Stmt>
				</Action>
				
			</xsl:for-each>

            <xsl:apply-templates select="node() | text()">
				<xsl:with-param name="buffers" select="$portBuffers"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
	
	<xsl:template match="Input">
		<xsl:param name="buffers"/>
		<xsl:variable name="port" select="@port"/>
		
		<xsl:choose>
			<xsl:when test="$buffers/buffer[@port=$port]">
				<xsl:variable name="size" select="$buffers/buffer[@port=$port]/@bufferSize"/>
				<!-- declare token variables, assign them proper port elements -->
				<xsl:for-each select="Decl">
					<Decl kind="Variable" name="{@name}">
						<Expr kind="Indexer">
							<Expr kind="Var" name="$inputBuffer${$port}"/>
							<Args>
								<!-- ringbuffer indexing: 
									 get element i from buffer[(head + i) mod size] -->
								<Expr kind="BinOpSeq">
									<Expr kind="BinOpSeq">
										<Expr kind="Var" name="$inputBuffer${$port}$head"/>
										<Op name="+"/>
										<Expr kind="Literal" literal-kind="Integer" value="{position()-1}"/>
									</Expr>
									<Op name="mod"/>
									<Expr kind="Literal" literal-kind="Integer" value="{$size}"/>
								</Expr>
							</Args>
						</Expr>						
					</Decl>
				</xsl:for-each>
				
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy>
					<xsl:for-each select="@*">
						<xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
					</xsl:for-each>
		
					<xsl:apply-templates select="node() | text()">
						<xsl:with-param name="buffers" select="$buffers"/>
					</xsl:apply-templates>
				</xsl:copy>
			</xsl:otherwise>
		</xsl:choose>		
	</xsl:template>
	
	<xsl:template match="Guards">
		<xsl:param name="buffers"/>
		
		<xsl:variable name="inputs" select="../Input"/>
		<xsl:copy>
			<xsl:for-each select="$buffers/buffer">
				<xsl:variable name="port" select="@port"/>
				<!-- for each buffered input that occurs in the input patterns of this action... -->
				<xsl:if test="$inputs[@port=$port]">
					<!-- ... generate a guard testing for a sufficient number of tokens -->
					<Expr kind="BinOpSeq">
						<Expr kind="Var" name="$inputBuffer${$port}$counter"/>
						<Op name="&gt;="/>
						<Expr kind="Literal" literal-kind="Integer" name="{count($inputs[@port=$port]/Decl)}"/>
					</Expr>
				</xsl:if>
			</xsl:for-each>
			
			<xsl:apply-templates select="*">
				<xsl:with-param name="buffers" select="$buffers"/>
			</xsl:apply-templates>			
		</xsl:copy>
	</xsl:template>
	
	
	<xsl:template match="Action">
		<xsl:param name="buffers"/>
		
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()">
				<xsl:with-param name="buffers" select="$buffers"/>
            </xsl:apply-templates>
			
			<xsl:if test="not(Guards)">
				<xsl:variable name="dummyAction">
					<DUMMY>
						<xsl:copy-of select="../Input"/>
						<Guards/>						
					</DUMMY>
				</xsl:variable>
				<xsl:apply-templates select="$dummyAction/Guards">
					<xsl:with-param name="buffers" select="$buffers"/>
				</xsl:apply-templates>
			</xsl:if>
			
			<!-- update each buffer that was read from 
				 - reduce counter
				 - increment head (with wrap-around) -->
			<xsl:for-each select="Input[@port=$buffers/buffer/@port]">
				<xsl:variable name="port" select="@port"/>
				<xsl:variable name="size" select="$buffers/buffer[@port=$port]/@bufferSize"/>
				<xsl:variable name="n" select="count(Decl)"/>
				
				<Stmt kind="Assign" name="$inputBuffer${$port}$counter">
					<Expr kind="BinOpSeq">
						<Expr kind="Var" name="$inputBuffer${$port}$counter"/>
						<Op name="-"/>
						<Expr kind="Literal" literal-kind="Integer" value="{$n}"/>
					</Expr>
				</Stmt>
				<Stmt kind="Assign" name="$inputBuffer${$port}$head">
					<Expr kind="BinOpSeq">
						<Expr kind="BinOpSeq">
							<Expr kind="Var" name="$inputBuffer${$port}$counter"/>
							<Op name="+"/>
							<Expr kind="Literal" literal-kind="Integer" value="{$n}"/>
						</Expr>
						<Op name="mod"/>
						<Expr kind="Literal" literal-kind="Integer" value="{$size}"/>
					</Expr>
				</Stmt>
			</xsl:for-each>
        </xsl:copy>
    </xsl:template>
	
	<xsl:template name="computeInputBuffers">
		<xsl:param name="actor"/>
		<xsl:param name="bufferedPorts"/>
		
		<xsl:for-each select="$bufferedPorts/Port">
			<xsl:variable name="port" select="@name"/>
			<xsl:variable name="inputLengths">
				<xsl:for-each select="$actor/Action">
					<buffer size="{count(Input[@port=$port]/Decl)}"/>
				</xsl:for-each>
			</xsl:variable>

			<!-- round up buffer size to nearest power of 2 -->
			<buffer port="{$port}" minSize="{min($inputLengths/buffer/@size)}"
				                   bufferSize="{math:power(2, ceiling(math:log(max($inputLengths/buffer/@size)) div math:log(2)))}">
				<xsl:copy-of select="Type"/>
			</buffer>
		</xsl:for-each>
	</xsl:template>
	

	<xsl:template name="computeBufferedInputPorts">
		<xsl:param name="actor"/>		
			<xsl:for-each select="$actor/Port[@kind='Input']">
				<xsl:variable name="port" select="@name"/>
				<!-- 
					FIXME: does not take into account variable capture: 
						action P: [x] ...
						guard let x = 4: a < x end end
					will falsely lead to P being listed as port to be substituted.
				-->
				<xsl:if test="$actor/Action[count(Input[@port=$port]/Decl)>1] or 
					          $actor/Action/Guards/descendant-or-self::Expr[@kind='Var'][@name=ancestor::Action/Input[@port=$port]/Decl/@name]">
					<xsl:copy-of select="."/>
				</xsl:if>
			</xsl:for-each>			
	</xsl:template>


    <xsl:template match="*">
		<xsl:param name="buffers"/>
		
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()">
				<xsl:with-param name="buffers" select="$buffers"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>


</xsl:stylesheet>


