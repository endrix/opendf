<!--
    VariableSorter

    Sorts variable declarations according to their dependencies.

    assumes: DependencyAnnotator

    author: JWJ
    
    DBP: Change ordering to be
            Decl[ @kind='Parameter' ]
            Input
            Decl[ all others ]
            everything else but Output
            Ouput
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.1"
  xmlns:exsl="http://exslt.org/common"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="exsl set">

  <xsl:output method="xml"/>


    <xsl:template match="Actor | Action | Expr[@kind='Proc'] | Expr[@kind='Lambda'] | Expr[@kind='Let'] | Stmt[@kind='Block']">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="Decl[@kind='Parameter']" mode="copy"/>
            <xsl:apply-templates select="Input" mode="copy"/>

            <!--
                Sorts declarations. The basic idea is that the number of non-lazy dependencies
                indicates the precedence. If a variable v1 depends on v2, then that number is
                strictly higher for v1 than v2, as v1 depends on everything v2 depends on, plus v2.
            -->
            <xsl:apply-templates select="Decl[@kind='Variable']" mode="copy">
                <xsl:sort select="count(Note[@kind='dependency'][@lazy='no'])"/>
            </xsl:apply-templates>

            <xsl:apply-templates/>
            <xsl:apply-templates select="Output" mode="copy"/>
            
        </xsl:copy>
    </xsl:template>

    <!-- Swallow VarDecl elements in the first pass. -->
    <xsl:template match="Decl[@kind='Variable' or @kind='Parameter' ]"/>
    <xsl:template match="Input"/>
    <xsl:template match="Output"/>
    
    <!-- Copy VarDecl elements in the second pass. -->
    <xsl:template match="*" mode="copy">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates/>
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
