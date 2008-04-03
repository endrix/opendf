<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  extension-element-prefixes="xsl xd"
  version="2.0">
  
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:author>IDM</xd:author>
    <xd:copyright>Xilinx, 2007</xd:copyright>
    <xd:short>Create copies of all Decl elements referenced by Expr[@kind='Var']</xd:short>
    <xd:detail>
      For each Expr[@kind='Var'] element within the tree of the given element, the
      corresponding Decl (matched by name attribute) is found in the supplied environment
      and copied into the result document.  Each Decl is only copied one time.
    </xd:detail>
  </xd:doc>

  <xsl:template name="copyAllRequiredDecls">
    <xsl:param name="expr"/>
    <xsl:param name="env"/>

    <xsl:variable name="processed">
      <Expr/>
    </xsl:variable>
    <!-- intelligently copy the decls -->
    <xsl:variable name="discoveredDecls">
      <xsl:apply-templates select="$expr" mode="copyRequiredDecls">
        <xsl:with-param name="env" select="$env"/>
        <xsl:with-param name="processed" select="$processed"/>
      </xsl:apply-templates>
    </xsl:variable>

    <!-- uniquify so that the top level caller will get a unique list in return -->
    <xsl:for-each select="$discoveredDecls/Decl">
      <xsl:variable name="pos" select="position()"/>
      <xsl:variable name="currentName" select="@name"/>
      
      <xsl:if test="not( $discoveredDecls/Decl[@name=$currentName and position() &lt; $pos] )">
        <xsl:copy-of select="."/>
      </xsl:if>
    </xsl:for-each>
    
  </xsl:template>
  
  <xsl:template match="*" mode="copyRequiredDecls">
    <xsl:param name="env"/>
    <xsl:param name="processed"/>
    <xsl:apply-templates mode="copyRequiredDecls">
      <xsl:with-param name="env" select="$env"/>
      <xsl:with-param name="processed" select="$processed"/>
    </xsl:apply-templates>
  </xsl:template>
  
  <xsl:template match="Expr[@kind='Var']" mode="copyRequiredDecls">
    <xsl:param name="env"/>
    <!-- processed contains the Var refs (Expr nodes) that have already been processed.
         This allows us to only traverse each var ref once. -->
    <xsl:param name="processed"/>
    <xsl:variable name="nm" select="@name"/>

    <xsl:variable name="newProcessed">
      <xsl:copy-of select="$processed/*"/>
      <xsl:copy-of select="."/>
    </xsl:variable>

      <xsl:choose>
        <xsl:when test="$processed/Expr[@name=$nm]">
          <!-- this Var exp has already been seen.  do nothing -->
        </xsl:when>
        <xsl:otherwise>
          <!-- The var has not yet been processed.  Copy the Decl for that var and
               then search for var refs in both the decl and further down in this expr. -->
          <xsl:copy-of select="$env/Env/Decl[@name=$nm]"/>
          <xsl:apply-templates mode="copyRequiredDecls">
            <xsl:with-param name="env" select="$env"/>
            <xsl:with-param name="processed" select="$newProcessed"/>
          </xsl:apply-templates>
          <xsl:apply-templates select="$env/Env/Decl[@name=$nm]" mode="copyRequiredDecls">
            <xsl:with-param name="env" select="$env"/>
            <xsl:with-param name="processed" select="$newProcessed"/>
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>

  </xsl:template>
  
</xsl:stylesheet>