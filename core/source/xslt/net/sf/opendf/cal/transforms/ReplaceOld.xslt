<!--
    ReplaceOld

    Replaces in each action all old-references to variables defined inside the entire action scope with
    references to a temporary variable $old$v (for a variable v). It also introduces the definition
    if $old$v into the action scope.

    assumes: VariableAnnotator   why does this need the annotator?
             DBP Dependency now removed!!

    author: YZ, JWJ
    
    2005-08-02 DBP Modified to search the design document directly for the declaration
                   of the variable being 'olded'. This removes the dependence on the
                   VariableAnnotator. This transformation should be called before the
                   variable annotator or AddId.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.1">
  <xsl:output method="xml"/>


  <xsl:template match="Action">
    <xsl:variable name="this" select="."/>
    <xsl:variable name="oldRefs">
      <xsl:copy-of select=".//Expr[@kind='Var'][@old='Yes']"/>
    </xsl:variable>
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:for-each select="$oldRefs/*"> 			
                     <xsl:variable name="myName" select="@name"/>
              <xsl:if test="not(preceding-sibling::*[@name=$myName])">
                  
                <!-- find all declarations of this name in scope -->
                <xsl:variable name="decls" select="ancestor-or-self::*/Decl[@name=$myName]"/>

                <Decl kind="Variable" name="$old${@name}" mutable="no" assignable="no">
                  <!-- Use the same type declaration (if any) -->
                  <xsl:copy-of select="$decls[1]/Type"/>
                  <Expr kind="Var" name="{@name}" old="no"/>
                </Decl>
              </xsl:if>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
  </xsl:template>

  <xsl:template match="Expr[@kind='Var'][@old='Yes']">
     <Expr kind="Var" name="$old${@name}" old="no"/>
  </xsl:template>


    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>


</xsl:stylesheet>


