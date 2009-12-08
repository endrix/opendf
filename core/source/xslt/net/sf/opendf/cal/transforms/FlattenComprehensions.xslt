
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  xmlns:math="http://exslt.org/math"
  extension-element-prefixes="xsl xd math"
  version="2.0">
  <xsl:output method="xml" indent="yes"/>


  <xsl:template match="Stmt[@kind='Assign']">
    
    <xsl:variable name="name" select="@name"/>
    <xsl:variable name="id" select="@id"/>
   
    
    <xsl:choose>
      <xsl:when test="Expr[@kind='Let']/Expr['List']">        
        <xsl:for-each select="Expr[@kind='Let']/Expr['List']/Expr">
          <Stmt kind='Assign' name="{$name}" id="{concat($id,'$', position())}">
             <xsl:apply-templates select="Note"/>
             <Args>                 
               <xsl:apply-templates select="Decl/Expr" mode="strip-id"/>
             </Args>             
           <xsl:apply-templates select="Expr" mode="strip-id"/>             
          </Stmt>  
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          <xsl:apply-templates/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
      
  </xsl:template>
 
 <!-- This piece of dumb ass code is only to cleanse the code from some invalid type info -->    
  <xsl:template match="Note[@kind='exprType']" mode="strip-id">
  	<xsl:if test="not(.//Type[@name='ANY'])">
  	  <xsl:copy>
        <xsl:for-each select="@*">
          <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
        </xsl:for-each>
        <xsl:apply-templates mode="strip-id"/>   
      </xsl:copy>
  	</xsl:if>    
  </xsl:template> 
   
  <xsl:template match="*" mode="strip-id">     
    <xsl:copy>
      <xsl:for-each select="@*">
          <xsl:if test="name()!='id'">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates mode="strip-id"/>   
    </xsl:copy>
  </xsl:template> 
   
  <xsl:template match="*">     
     <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>   
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>