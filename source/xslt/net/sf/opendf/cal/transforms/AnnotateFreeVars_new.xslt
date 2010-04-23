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
  	extension-element-prefixes="xsl set">

  	<xsl:output method="xml"/>
  	
    <xsl:template match="Expr | Stmt | Actor | Action | Output">
    	<xsl:variable name="I">
            <xsl:apply-templates select="*[not(self::Note[@kind='freeVar'])] | text()"/>
    	</xsl:variable>
    	<xsl:variable name="this" select="."/>
    	<xsl:variable name="decls" select="Decl | Input/Decl"/>

        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:if test="self::Expr[@kind='Var']">
            	<Note kind="freeVar" name="{@name}"/>
            </xsl:if>
            
            <xsl:copy-of select="$I/*"/>
            
            <xsl:for-each select="$I/*/Note[@kind='freeVar']">
            	<xsl:variable name="v" select="@name"/>
            	<xsl:if test="not(preceding-sibling::Note[@kind='freeVar'][@name=$v])">
	            	<xsl:if test="not($decls[@name=$v])">
		            	<xsl:copy-of select="."/>
		            </xsl:if>
		      	</xsl:if>
            </xsl:for-each>
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




</xsl:stylesheet>





