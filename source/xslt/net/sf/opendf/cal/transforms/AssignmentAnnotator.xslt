<!--
    AssignmentAnnotator

    Computes the set of variables modified by a statement or action. 
	It adds a note of the following format:

    <Note kind="modifies" name="<name>"/>

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.1"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="xsl set">

  <xsl:output method="xml"/>

    <xsl:template match="Expr | Stmt | Actor | Action">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="* | text()"/>

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
					<!--  FIXME: 
						The line below does not take into account the *order* of generators
						and filters. For instance, in the expression
						[a * b : for a in F(b), for b in [1, 2, 3]]
						b *is* a free variable, but the way we express this below 
						we will (erroneously) consider even the b in F(b) bound by 
						the subsequent generator.
						A possible fix (apart from doing the right thing here)
						would be to update the language definitionto disallow the use of 
						variables before their point of definition in collections,
						even if they are defined in the context, or to disallow the 
						redefinition of bound variables by generators (or even in general,
						which is what Java does for method-local variables).
					-->
                    <xsl:when test="$intermediate/Generator/Decl[@name=$name]"/>
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





