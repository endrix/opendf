<!--
    ReplaceGenerators

    uses: context

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.1"
  xmlns:exsl="http://exslt.org/common"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="xsl exsl set">

  <xsl:output method="xml"/>

    <xsl:param name="context-uri" select="'opendf/context/Context.xml'"/>
    <xsl:variable name="context" select="document($context-uri)/Context"/>
    <!-- If you have no internet connection, use this line for debugging purposes instead:
        <xsl:param name="context" select="document('../../web/data/platforms/DefaultContext.xml')/Context"/>
    -->


    <!--
        Process foreach-statements.
    -->

    <xsl:template match="Stmt[@kind='Foreach']">
        <xsl:call-template name="replaceForeachGenerators">
            <xsl:with-param name="gens" select="Generator"/>
            <xsl:with-param name="body" select="Body/*"/>
        </xsl:call-template>
    </xsl:template>



    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>


    <xsl:template name="replaceForeachGenerators">
        <xsl:param name="gens"/>
        <xsl:param name="body"/>

        <xsl:variable name="g" select="$gens[1]"/>

        <xsl:choose>
        <xsl:when test="not($gens)">
            <Stmt kind="Block"><xsl:apply-templates select="$body"/></Stmt>
        </xsl:when>
        <xsl:otherwise>
            <Stmt kind="Block">
                <Decl kind="Variable" name="$Collection${generate-id($g)}">
                    <Type name="Collection">
                        <TypePars><xsl:copy-of select="$g/Type"/></TypePars>
                    </Type>
                    <xsl:copy-of select="$g/Expr"/>
                </Decl>
                <!-- ... then replace the variables in the generator one by one.-->
                <xsl:call-template name="replaceForeachGeneratorVars">
                    <xsl:with-param name="vars" select="$g/Decl"/>
                    <xsl:with-param name="type" select="$g/Type"/>
                    <xsl:with-param name="filters" select="$g/Filters/Expr"/>
                    <xsl:with-param name="collection">$Collection$<xsl:value-of select="generate-id($g)"/></xsl:with-param>
                    <xsl:with-param name="gens" select="$gens[position() > 1]"/>
                    <xsl:with-param name="body" select="$body"/>
                </xsl:call-template>
            </Stmt>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="replaceForeachGeneratorVars">
        <xsl:param name="vars"/>
        <xsl:param name="type"/>
        <xsl:param name="filters"/>
        <xsl:param name="collection"/>
        <xsl:param name="gens"/>
        <xsl:param name="body"/>

        <xsl:choose>
        <!-- If there are no (more) variables... -->
        <xsl:when test="not($vars)">
            <!-- ... iterate over filters. -->
            <xsl:call-template name="replaceForeachGeneratorFilters">
                <xsl:with-param name="filters" select="$filters"/>
                <xsl:with-param name="gens" select="$gens"/>
                <xsl:with-param name="body" select="$body"/>
            </xsl:call-template>
        </xsl:when>
        <!-- If there is at least one variable... -->
        <xsl:otherwise>
            <!-- construct a statement of the form
                iterate(C, proc (v) B end)
                where iterate is the iterator function, C is the collection variable,
                v is the current element variable, and B is the
                statement sequence representing the body
            -->
            <Stmt kind="Call">
                <Expr kind="Var" name="{$context/Procedure[@name='iterate']/@procedure}"/>
                <Args>
                    <Expr kind="Var" name="{$collection}"/>
                    <Expr kind="Proc">
                        <Decl kind="Parameter" name="{$vars[1]/@name}">
                            <xsl:copy-of select="$type"/>
                        </Decl>
                        <xsl:call-template name="replaceForeachGeneratorVars">
                            <xsl:with-param name="vars" select="$vars[position() > 1]"/>
                            <xsl:with-param name="type" select="$type"/>
                            <xsl:with-param name="filters" select="$filters"/>
                            <xsl:with-param name="collection" select="$collection"/>
                            <xsl:with-param name="gens" select="$gens"/>
                            <xsl:with-param name="body" select="$body"/>
                        </xsl:call-template>
                    </Expr>
                </Args>
            </Stmt>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        Recursively iterate over filters.
    -->
    <xsl:template name="replaceForeachGeneratorFilters">
        <xsl:param name="filters"/>
        <xsl:param name="gens"/>
        <xsl:param name="body"/>

        <xsl:choose>
        <!-- If there are no (more) filters... -->
        <xsl:when test="not($filters)">
            <!-- ... process the next generator. -->
            <xsl:call-template name="replaceForeachGenerators">
                <xsl:with-param name="gens" select="$gens"/>
                <xsl:with-param name="body" select="$body"/>
            </xsl:call-template>
        </xsl:when>
        <!-- If there is at least one filter... -->
        <xsl:otherwise>
            <!-- construct a statement of the form
                if F then S end
                where F is the current filter, S is the statement representing the rest.
            -->
            <Stmt kind="If">
                <xsl:copy-of select="$filters[1]"/>
                <Body>
                    <xsl:call-template name="replaceForeachGeneratorFilters">
                        <xsl:with-param name="filters" select="$filters[position() > 1]"/>
                        <xsl:with-param name="gens" select="$gens"/>
                        <xsl:with-param name="body" select="$body"/>
                    </xsl:call-template>
                </Body>
            </Stmt>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>



</xsl:stylesheet>