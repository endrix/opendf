<!--
    AnnotateFreeVars

    Computes the free variables of expressions, statements, actions, and the actor. At each
    level, it adds for each free variable <name> a note of the following format:

    <Note kind="freeVar" name="<name>"/>

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.1"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="set">

  <xsl:output method="xml"/>

    <xsl:template match="Expr | Stmt | Actor | Action">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <!-- suppress any prior annoatations and recalculate -->
            <xsl:apply-templates select="*[not(self::Note[@kind='freeVar'])] | text()"/>

            <xsl:call-template name="freeVars">
                <xsl:with-param name="N" select="."/>
            </xsl:call-template>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="* | text()"/>
        </xsl:copy>
    </xsl:template>



    <xsl:template name="freeVars">
        <xsl:param name="N"/>
        <!--
            First compute list of free variable references.
            Simply use variable annotations to locate those.
        -->
        <xsl:variable name="FV">
            <xsl:for-each select="$N/descendant-or-self::Expr[@kind='Var']">
                <xsl:variable name="intermediate" select="set:intersection($N/descendant-or-self::*, ./ancestor::*)"/>
                <xsl:variable name="name" select="@name"/>
                <xsl:choose>
                    <xsl:when test="$intermediate/Decl[@name=$name]"/>
                    <xsl:when test="$intermediate/Input/Decl[@name=$name]"/>
                    <xsl:otherwise><Note kind="freeVar" name="{$name}"/></xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:variable>

        <!--
            Copy the free variables list without duplicates.
        -->
        <xsl:for-each select="$FV/*">
            <xsl:variable name="name" select="@name"/>
            <xsl:if test="not(preceding-sibling::*[@name=$name])">
                <xsl:copy-of select="."/>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>


</xsl:stylesheet>





