
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator"  
  version="2.0">
  
  <xsl:output method="xml"/>
  <xsl:include href="net/sf/opendf/transforms/CopyRequiredDecls.xslt"/>
  
  <xd:doc type="stylesheet">
    <xd:author>JWJ</xd:author>
    <xd:copyright>Xilinx, 2006</xd:copyright>
  </xd:doc>

  <xsl:template match="/">
    <xsl:apply-templates>
      <xsl:with-param name="prefix" select="''"/>
      <xsl:with-param name="env">
        <Env/>
      </xsl:with-param>
    </xsl:apply-templates>
  </xsl:template>
  
  <xsl:template match="XDF">
    <xsl:param name="prefix"/>
    <xsl:param name="env"/>
    
    <xsl:variable name="localEnv">
      <Env>
        <xsl:for-each select="Decl[@kind='Param']">
          <xsl:variable name="nm" select="@name"/>
          <Decl kind="Variable" name="{$nm}">
            <xsl:copy-of select="$env/Decl[@name=$nm]/Expr"/>
            <xsl:copy-of select="Type"/>
          </Decl>
        </xsl:for-each>
        <xsl:copy-of select="Decl[@kind='Variable']"/>
      </Env>
    </xsl:variable>
    
    <xsl:variable name="I">
      <xsl:apply-templates select="Instance">
        <xsl:with-param name="prefix" select="$prefix"/>
        <xsl:with-param name="env" select="$localEnv"/>
      </xsl:apply-templates>
    </xsl:variable>
    <xsl:variable name="C">
      <xsl:apply-templates select="Connection">
        <xsl:with-param name="prefix" select="$prefix"/>
        <xsl:with-param name="env" select="$localEnv"/>
      </xsl:apply-templates>
    </xsl:variable>
    
    
    <XDF>
      <xsl:if test="@name">
        <xsl:attribute name="name">
          <xsl:value-of select="@name"/>
        </xsl:attribute>   
      </xsl:if> 	
      
      <!-- remove declarations --> 
      <xsl:copy-of select="Package | Port | Attribute"/>
      
      <!-- unfold embedded networks -->     
      <xsl:for-each select="$I/Instance[XDF]">
        <xsl:copy-of select="XDF/Instance"/>  <!-- all instances are atomic actors -->
        <xsl:copy-of select="XDF/Connection[@src ne ''][@dst ne '']"/>  <!-- internal arcs -->
      </xsl:for-each>
      
      <!-- copy atomic actors -->
      <xsl:copy-of select="$I/Instance[not(XDF)]"/>
      
      <xsl:for-each select="$C/Connection">
        <xsl:call-template name="substituteConnection">
          <xsl:with-param name="c" select="."/>
          <xsl:with-param name="I" select="$I"/>
        </xsl:call-template>
      </xsl:for-each>
    </XDF>
  </xsl:template>
  
  <xsl:template match="Instance[XDF]">
    <xsl:param name="prefix"/>
    <xsl:param name="env"/>
    
    <xsl:param name="instEnv">
      <Env>
        <xsl:for-each select="Parameter">
          <Decl kind="Variable" name="@name">
            <xsl:apply-templates select="Expr">
              <xsl:with-param name="prefix" select="$prefix"/>
              <xsl:with-param name="env" select="$env"/>              
            </xsl:apply-templates>
          </Decl>
        </xsl:for-each>
      </Env>
    </xsl:param>
    
    <Instance id="{concat($prefix, @id)}">
      <xsl:copy-of select="Class"/>
      
      <xsl:apply-templates select="XDF">
        <xsl:with-param name="prefix" select="concat($prefix, concat(@id, '$'))"/>
        <xsl:with-param name="env" select="$instEnv"/>
      </xsl:apply-templates>
    </Instance>
  </xsl:template>
  
  <xsl:template match="Instance">
    <xsl:param name="prefix"/>
    <xsl:param name="env"/>
    
    <Instance id="{concat($prefix, @id)}">      
      <xsl:copy-of select="Class | Note | Attribute"/>
      
      <xsl:apply-templates select="Parameter">
        <xsl:with-param name="prefix" select="$prefix"/>
        <xsl:with-param name="env" select="$env"/>
      </xsl:apply-templates>      
    </Instance>
  </xsl:template>
  
  <xsl:template match="Connection">
    <xsl:param name="prefix"/>
    <xsl:param name="env"/>
    
    <Connection 
      src="{if(@src='') then '' else concat($prefix, @src)}"
      dst="{if(@dst='') then '' else concat($prefix, @dst)}"
      src-port="{@src-port}"
      dst-port="{@dst-port}">
      
      <xsl:apply-templates>
        <xsl:with-param name="prefix" select="$prefix"/>
        <xsl:with-param name="env" select="$env"/>
      </xsl:apply-templates>
    </Connection>
  </xsl:template>
  
  <xsl:template match="Expr">
    <xsl:param name="prefix"/>
    <xsl:param name="env"/>
    <xsl:variable name="expr" select="."/>

    <Expr kind="Let">
      <!-- intelligently copy the decls -->
      <xsl:call-template name="copyAllRequiredDecls">
        <xsl:with-param name="expr" select="."/>
        <xsl:with-param name="env" select="$env"/>
      </xsl:call-template>

      <!-- Copy the expr -->
      <xsl:copy-of select="."/>
    </Expr>
    
  </xsl:template>
  
  <xsl:template match="*">
    <xsl:param name="prefix"/>
    <xsl:param name="env"/>
    
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}">
          <xsl:value-of select="if (name() = 'id' and not(ancestor::QID)) then concat($prefix, .) else ."/>
        </xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates>
        <xsl:with-param name="prefix" select="$prefix"/>
        <xsl:with-param name="env" select="$env"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="substituteConnection">
    <xsl:param name="c"/>
    <xsl:param name="I"/>
    
    <!--        <III><xsl:copy-of select="$I"/></III> -->
    
    <xsl:variable name="S">
      <xsl:call-template name="findSources">
        <xsl:with-param name="c" select="$c"/>
        <xsl:with-param name="I" select="$I"/>
      </xsl:call-template>
    </xsl:variable>
    
    <xsl:variable name="D">
      <xsl:call-template name="findDestinations">
        <xsl:with-param name="c" select="$c"/>
        <xsl:with-param name="I" select="$I"/>
      </xsl:call-template>
    </xsl:variable>
    
    <xsl:for-each select="$S/src">
      <xsl:variable name="s" select="."/>
      <xsl:for-each select="$D/dst">
        <xsl:variable name="d" select="."/>
        <Connection src="{$s/@src}" src-port="{$s/@src-port}"
          dst="{$d/@dst}" dst-port="{$d/@dst-port}">
          <xsl:copy-of select="$s/*"/>
          <xsl:copy-of select="$d/*"/>
        </Connection>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template name="findSources">
    <xsl:param name="c"/>
    <xsl:param name="I"/>
    
    <xsl:choose>
      <xsl:when test="$c/@src = ''">
        <src src="" src-port="{$c/@src-port}">
          <xsl:copy-of select="$c/*"/>
        </src>
      </xsl:when>
      <xsl:when test="$I/Instance[@id = $c/@src]/XDF">
        <xsl:variable name="composite" select="$I/Instance[@id = $c/@src]/XDF"/>
        <xsl:for-each select="$composite/Connection[@dst=''][@dst-port = $c/@src-port]">
          <src src="{@src}" src-port="{@src-port}">
            <xsl:copy-of select="$c/*"/>
            <xsl:copy-of select="./*"/>
          </src>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="$I/Instance[@id = $c/@src]">
        <src src="{$c/@src}" src-port="{$c/@src-port}">
          <xsl:copy-of select="$c/*"/>
        </src>
      </xsl:when>
      <xsl:otherwise>
        <ERROR source="xdfFlatten">Cannot find connection source id <xsl:value-of select="$c/@src"/>.</ERROR>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template name="findDestinations">
    <xsl:param name="c"/>
    <xsl:param name="I"/>
    
    <xsl:choose>
      <xsl:when test="$c/@dst = ''">
        <dst dst="" dst-port="{$c/@dst-port}">
        </dst>
      </xsl:when>
      <xsl:when test="$I/Instance[@id = $c/@dst]/XDF">
        <xsl:variable name="composite" select="$I/Instance[@id = $c/@dst]/XDF"/>
        <xsl:for-each select="$composite/Connection[@src=''][@src-port = $c/@dst-port]">
          <dst dst="{@dst}" dst-port="{@dst-port}">
            <xsl:copy-of select="./*"/>
          </dst>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="$I/Instance[@id = $c/@dst]">
        <dst dst="{$c/@dst}" dst-port="{$c/@dst-port}">
        </dst>
      </xsl:when>
      <xsl:otherwise>
        <ERROR source="xdfFlatten">Cannot find connection destination id <xsl:value-of select="$c/@src"/>.</ERROR>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>



