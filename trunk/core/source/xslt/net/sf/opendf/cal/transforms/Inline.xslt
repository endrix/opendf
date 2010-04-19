<!--
    Inline

    Xilinx Confidential

    Copyright (c) 2005 Xilinx Inc.
-->

<xsl:stylesheet
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    extension-element-prefixes="xsl xd"
    version="2.0">
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Inline function applications and procedure calls.</xd:short>
    <xd:detail>
      Inline function applications and procedure calls. This transformation makes
      the following assumptions:
      <ul>
        <li>All functions and procedures are defined as constants with actor scope.</li>
        <li>No function or procedure variable is shadowed by a local definition.</li>
        <li>All functions and procedures are fully typed.</li>
        <li>There is no recursion.</li>
      </ul>
    </xd:detail>
    <xd:author>JWJ</xd:author>
    <xd:copyright>Xilinx Inc., 2005</xd:copyright>
    <xd:cvsId>$Id: Inline.xslt 1034 2005-11-06 18:59:05Z jornj $</xd:cvsId>
  </xd:doc>
  
  
  <xsl:template match="Actor">
    <xsl:param name="actor" select="UNDEFINED"/>
    
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <!-- Use this actor as the scope for the contained elements. -->
      <xsl:apply-templates select="node() | text()">
        <xsl:with-param name="actor" select="."/>
      </xsl:apply-templates>
    </xsl:copy>        
  </xsl:template>
  
  <!--<xsl:template match="Decl[Expr[@kind='Lambda']]">-->
  <xsl:template match="Decl[Expr[@kind='Let' and Expr[@kind='Lambda']]]">
    <!-- do nothing, just remove every declaration of a function variable. -->
  </xsl:template>
  
  <!--<xsl:template match="Decl[Expr[@kind='Proc']]">-->
  <xsl:template match="Decl[Expr[@kind='Let' and Expr[@kind='Proc']]]">
    <!-- do nothing, just remove every declaration of a function variable. -->
  </xsl:template>
  
  <xsl:template match="Expr[@kind='Application'][Expr[@kind='Var']]">
    <xsl:param name="actor" select="UNDEFINED"/>
    
    <xsl:variable name="args" select="Args"/>
    <!-- get name of function -->        
    <xsl:variable name="fname" select="Expr[@kind='Var']/@name"/>
    
    <xsl:choose>
      <xsl:when test="$actor/Decl[@name=$fname]">
        <!-- get function definition -->
        <!--<xsl:variable name="lambda" select="$actor/Decl[@name=$fname]/Expr"/>-->
        <xsl:variable name="lambda" select="$actor/Decl[@name=$fname]//Expr[@kind='Lambda'][1]"/>
        
        <xsl:variable name="result">
          <Expr kind="Let">
            <xsl:for-each select="$lambda/Decl[@kind='Parameter']">
              <xsl:variable name="n" select="position()"/>
              <Decl kind="Variable" name="$param${$fname}${@name}">
                <xsl:copy-of select="*"/>
                <xsl:copy-of select="$args/Expr[$n]"/>
              </Decl>
            </xsl:for-each>
            <xsl:apply-templates select="$lambda/Expr | $lambda/Decl[@kind='Variable']" mode="alpha-reduce">
              <xsl:with-param name="S">
                <xsl:for-each select="$lambda/Decl[@kind='Parameter']">
                  <substitution name="{@name}" new-name="$param${$fname}${@name}"/>
                </xsl:for-each>
              </xsl:with-param>
            </xsl:apply-templates>
          </Expr>
        </xsl:variable>                
        <xsl:apply-templates select="$result/*">
          <xsl:with-param name="actor" select="$actor"/>
        </xsl:apply-templates>
      </xsl:when>
      
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          <xsl:apply-templates select="node() | text()">
            <xsl:with-param name="actor" select="$actor"/>
          </xsl:apply-templates>
        </xsl:copy>
      </xsl:otherwise>
      
    </xsl:choose>
    
  </xsl:template>


  <xsl:template match="Stmt[@kind='Call']">
    <xsl:param name="actor" select="UNDEFINED"/>
    
    <xsl:variable name="args" select="Args/Expr"/>
    <!-- get name of procedure -->     
    <xsl:variable name="pname">
      <xsl:choose>
        <xsl:when test="Expr[@kind='Let']/Expr[@kind='Var']/@name">
          <xsl:value-of select="Expr[@kind='Let']/Expr[@kind='Var']/@name"/>
        </xsl:when>
        <xsl:when test="Expr[@kind='Var']/@name">
          <xsl:value-of select="Expr[@kind='Var']/@name"/>
        </xsl:when>
      
        <xsl:otherwise>
           <xsl:message terminate="yes">
             Unnamed procedure in Inline transformtion
           </xsl:message> 
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <!-- <xsl:variable name="pname" select="Expr[@kind='Let']/Expr[@kind='Var']/@name"/> -->
    
    <xsl:choose>
      <xsl:when test="$actor/Decl[@name=$pname]">
        <!-- get procedure definition -->
        <!--<xsl:variable name="proc" select="$actor/Decl[@name=$pname]/Expr"/>-->
        <xsl:variable name="proc" select="$actor/Decl[@name=$pname]//Expr[@kind='Proc'][1]"/>
        
        <xsl:variable name="result">
          <Stmt kind="Block">
            <xsl:for-each select="$proc/Decl">
              <xsl:variable name="n" select="position()"/>
              <Decl kind="Variable" name="$param${$pname}${@name}">
                <xsl:copy-of select="*"/>
                <xsl:copy-of select="$args[$n]"/>
              </Decl>
            </xsl:for-each>
            <xsl:apply-templates select="$proc/Stmt" mode="alpha-reduce">
              <xsl:with-param name="S">
                <xsl:for-each select="$proc/Decl">
                  <substitution name="{@name}" new-name="$param${$pname}${@name}"/>
                </xsl:for-each>
              </xsl:with-param>
            </xsl:apply-templates>
          </Stmt>
        </xsl:variable>                
        <xsl:apply-templates select="$result/*">
          <xsl:with-param name="actor" select="$actor"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          <xsl:apply-templates select="node() | text()">
            <xsl:with-param name="actor" select="$actor"/>
          </xsl:apply-templates>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>
  
  <xsl:template match="*">
    <xsl:param name="actor" select="UNDEFINED"/>

    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates select="node() | text()">
        <xsl:with-param name="actor" select="$actor"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  
  
  <!-- 
       mode: alpha-reduce
       
       These templates perform alpha reduction on a tree. The substitution
       list is given to them as a parameter.
  -->
  <xsl:template match="Expr[@kind='Var']" mode="alpha-reduce">
    <xsl:param name="S"/>
    
    <xsl:choose>
      <xsl:when test="@name = $S/substitution/@name">
        <xsl:variable name="varName" select="@name"/>
        <Expr kind="Var" name="{$S/substitution[@name=$varName]/@new-name}">
          <xsl:copy-of select="*"/>
        </Expr>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="Stmt[@kind='Assign']" mode="alpha-reduce">
    <xsl:param name="S"/>
    
    <xsl:choose>
      <xsl:when test="@name = $S/substitution/@name">
        <xsl:variable name="varName" select="@name"/>
        <Stmt kind="Assign" name="{$S/substitution[@name=$varName]/@new-name}">        
             <xsl:apply-templates mode="alpha-reduce">
              <xsl:with-param name="S" select="$S"/>
            </xsl:apply-templates>
        </Stmt>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          <xsl:apply-templates mode="alpha-reduce">
            <xsl:with-param name="S" select="$S"/>
          </xsl:apply-templates>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="*" mode="alpha-reduce">
    <xsl:param name="S"/>
    
    <xsl:variable name="localS">
      <xsl:for-each select="$S/substitution">
        <xsl:variable name="v" select="@name"/>
        <xsl:if test="not(Decl/@name = $v) and not(Input/Decl/@name = $v) and not(Generator/Decl/@name = $v)">
          <xsl:copy-of select="."/>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates mode="alpha-reduce">
        <xsl:with-param name="S" select="$localS"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  
  
  
</xsl:stylesheet>
