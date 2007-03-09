<!--
    DependencyAnnotator

    Computes for each variable declaration those variables (in the same scope)
    that this declaration depends on. It adds to each variable declaration notes of the form

    <Note kind="dependency" name="<name>" lazy="yes" | "no"/>

    for each variable in the same scope that this variable's declaration depends on. A dependency is lazy if
    the variable need not be defined at time of the declaration of the dependent variable, but only
    at the time of its first use.

    assumes: AnnotateFreeVars

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.1"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="set">

  <xsl:output method="xml"/>


    <xsl:template match="Decl[@kind='Variable']">

        <xsl:variable name="dependencies">
            <xsl:call-template name="dependencies">
                <xsl:with-param name="V" select="."/>
                <xsl:with-param name="visited" select="/.."/> <!-- empty node set -->
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="lazy">
            <xsl:choose>
            <xsl:when test="Expr[@kind='Lambda'] | Expr[@kind='Proc']">yes</xsl:when>
            <xsl:otherwise>no</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="* | text()"/>

            <xsl:for-each select="$dependencies/Var">
                <Note kind="dependency" name="{@name}" lazy="{$lazy}"/>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>


    <xsl:template name="dependencies">
        <xsl:param name="V"/>
        <xsl:param name="visited"/>

        <xsl:variable name="decls" select="$V/../Decl[@kind='Variable']"/>
        <!-- Compute dependency set (restricted to variables in same scope).
             First, take the free variables in the defining expression. -->
        <xsl:variable name="deps">
            <xsl:for-each select="$V/Expr/Note[@kind='freeVar']">
                <xsl:variable name="depname" select="@name"/>

                <xsl:if test="$decls[@name=$depname]">
                    <Var name="{$depname}"/>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <!-- Compute dependency set (restricted to variables in same scope).
             Then recursively call this template on them. -->
        <xsl:variable name="recdeps">
            <xsl:if test="not($visited/*[@name=$V/@name])">
                <xsl:copy-of select="$deps"/>
                <xsl:for-each select="$deps/*">
                    <xsl:variable name="vname" select="@name"/>
                    <xsl:if test="not($vname=$V/@name)">
                        <xsl:call-template name="dependencies">
                            <xsl:with-param name="V" select="$decls[@name=$vname]"/>
                            <xsl:with-param name="visited">
                                <xsl:copy-of select="$visited"/>
                                <Var name="{$V/@name}"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:for-each>
            </xsl:if>
        </xsl:variable>

        <xsl:for-each select="$recdeps/Var">
            <xsl:variable name="name" select="@name"/>
            <xsl:if test="not(preceding-sibling::*[@name=$name])">
                <xsl:copy-of select="."/>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>



</xsl:stylesheet>

