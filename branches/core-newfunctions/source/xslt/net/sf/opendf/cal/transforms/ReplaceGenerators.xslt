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
        Process set comprehensions
    -->
    <xsl:template match="Expr[@kind='Set']">
        <xsl:call-template name="replaceComprehensionGenerators">
            <xsl:with-param name="creator" select="$context/Function[@name='create-set']/@function"/>
            <xsl:with-param name="gens" select="Generator"/>
            <xsl:with-param name="elements" select="Expr"/>
            <xsl:with-param name="comprehensionKind" select="@kind"/>
        </xsl:call-template>
    </xsl:template>

    <!--
        Process list comprehensions.
    -->
    <xsl:template match="Expr[@kind='List']">
        <xsl:variable name="mainList">
            <xsl:call-template name="replaceComprehensionGenerators">
                <xsl:with-param name="creator" select="$context/Function[@name='create-list']/@function"/>
                <xsl:with-param name="gens" select="Generator"/>
                <xsl:with-param name="elements" select="Expr"/>
                <xsl:with-param name="comprehensionKind" select="@kind"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:choose>
        <xsl:when test="Tail">
            <Expr kind="BinOpSeq">
                <Op name="+"/>
                <xsl:copy-of select="$mainList"/>
                <xsl:apply-templates select="Tail/Expr"/>
            </Expr>
         </xsl:when>
         <xsl:otherwise>
            <xsl:copy-of select="$mainList"/>
         </xsl:otherwise>
         </xsl:choose>
    </xsl:template>

    <!--
        Process map comprehensions.
    -->
    <xsl:template match="Expr[@kind='Map']">
        <xsl:call-template name="replaceComprehensionGenerators">
            <xsl:with-param name="creator" select="$context/Function[@name='create-map']/@function"/>
            <xsl:with-param name="gens" select="Generator"/>
            <xsl:with-param name="elements" select="Mapping"/>
            <xsl:with-param name="comprehensionKind" select="@kind"/>
        </xsl:call-template>
    </xsl:template>


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


    <!--
        Computes the replacement expression for a comprehension.
    -->
    <xsl:template name="replaceComprehensionGenerators">
        <xsl:param name="creator"/>         <!-- The creator function. -->
        <xsl:param name="gens"/>            <!-- The list of generators. -->
        <xsl:param name="elements"/>       <!-- The element expressions. -->
        <xsl:param name="comprehensionKind"/> <!-- The kind-tag of the comprehension expression. -->

        <!-- The first generator. -->
        <xsl:variable name="g" select="$gens[1]"/>

        <xsl:choose>
        <!-- If there are no (more) generators ... -->
        <xsl:when test="not($gens)">
            <!-- ... just process the element expressions. -->
            <Expr kind="{$comprehensionKind}"><xsl:apply-templates select="$elements"/></Expr></xsl:when>
        <!-- If there is at least one generator ... -->
        <xsl:otherwise>
            <Expr kind="Let">  <!-- ... first evaluate the collection, store it in a variable ... -->
                <Decl kind="Variable" name="$Collection${generate-id($g)}">
                    <Type name="Collection">
                        <TypePars><xsl:copy-of select="$g/Type"/></TypePars>
                    </Type>
                    <xsl:copy-of select="$g/Expr"/>
                </Decl>
                <!-- ... then replace the variables in the generator one by one.-->
                <xsl:call-template name="replaceComprehensionGeneratorVars">
                    <xsl:with-param name="vars" select="$g/Decl"/>
                    <xsl:with-param name="type" select="$g/Type"/>
                    <xsl:with-param name="filters" select="$g/Filters/Expr"/>
                    <xsl:with-param name="collection">$Collection$<xsl:value-of select="generate-id($g)"/></xsl:with-param>
                    <xsl:with-param name="creator" select="$creator"/>
                    <xsl:with-param name="gens" select="$gens[position() > 1]"/>
                    <xsl:with-param name="elements" select="$elements"/>
                    <xsl:with-param name="comprehensionKind" select="$comprehensionKind"/>
                </xsl:call-template>
            </Expr>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        Recursively iterate over the variables of one generator.
    -->
    <xsl:template name="replaceComprehensionGeneratorVars">
        <xsl:param name="vars"/>
        <xsl:param name="type"/>
        <xsl:param name="filters"/>
        <xsl:param name="collection"/>
        <xsl:param name="creator"/>
        <xsl:param name="gens"/>
        <xsl:param name="elements"/>
        <xsl:param name="comprehensionKind"/>

        <xsl:choose>
        <!-- If there are no (more) variables... -->
        <xsl:when test="not($vars)">
            <!-- ... iterate over filters. -->
            <xsl:call-template name="replaceComprehensionGeneratorFilters">
                <xsl:with-param name="filters" select="$filters"/>
                <xsl:with-param name="creator" select="$creator"/>
                <xsl:with-param name="gens" select="$gens"/>
                <xsl:with-param name="elements" select="$elements"/>
                <xsl:with-param name="comprehensionKind" select="$comprehensionKind"/>
            </xsl:call-template>
        </xsl:when>
        <!-- If there is at least one variable... -->
        <xsl:otherwise>
            <!-- construct an expression of the form
                creator(C, lambda (v) E end)
                where creator is the creator function, C is the collection variable,
                v is the current element variable, and E is the
                replacement expression for the rest.
            -->
            <Expr kind="Application">
                <Expr kind="Var" name="{$creator}"/>
                <Args>
                    <Expr kind="Var" name="{$collection}"/>
                    <Expr kind="Lambda">
                        <Decl kind="Parameter" name="{$vars[1]/@name}">
                            <xsl:copy-of select="$type"/>
                        </Decl>
                        <Type name="Any"/>
                        <xsl:call-template name="replaceComprehensionGeneratorVars">
                            <xsl:with-param name="vars" select="$vars[position() > 1]"/>
                            <xsl:with-param name="type" select="$type"/>
                            <xsl:with-param name="filters" select="$filters"/>
                            <xsl:with-param name="collection" select="$collection"/>
                            <xsl:with-param name="creator" select="$creator"/>
                            <xsl:with-param name="gens" select="$gens"/>
                            <xsl:with-param name="elements" select="$elements"/>
                            <xsl:with-param name="comprehensionKind" select="$comprehensionKind"/>
                        </xsl:call-template>
                    </Expr>
                </Args>
            </Expr>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        Recursively iterate over filters.
    -->
    <xsl:template name="replaceComprehensionGeneratorFilters">
        <xsl:param name="filters"/>
        <xsl:param name="creator"/>
        <xsl:param name="gens"/>
        <xsl:param name="elements"/>
        <xsl:param name="comprehensionKind"/>

        <xsl:choose>
        <!-- If there are no (more) filters... -->
        <xsl:when test="not($filters)">
            <!-- ... process the next generator. -->
            <xsl:call-template name="replaceComprehensionGenerators">
                <xsl:with-param name="creator" select="$creator"/>
                <xsl:with-param name="gens" select="$gens"/>
                <xsl:with-param name="elements" select="$elements"/>
                <xsl:with-param name="comprehensionKind" select="$comprehensionKind"/>
            </xsl:call-template>
        </xsl:when>
        <!-- If there is at least one filter... -->
        <xsl:otherwise>
            <!-- construct an expression of the form
                if F then E else Empty end
                where F is the current filter, E is the expression representing the rest, and Empty is the
                empty collection of the corresponding kind.
            -->
            <Expr kind="If">
                <xsl:copy-of select="$filters[1]"/>
                <xsl:call-template name="replaceComprehensionGeneratorFilters">
                    <xsl:with-param name="filters" select="$filters[position() > 1]"/>
                    <xsl:with-param name="creator" select="$creator"/>
                    <xsl:with-param name="gens" select="$gens"/>
                    <xsl:with-param name="elements" select="$elements"/>
                    <xsl:with-param name="comprehensionKind" select="$comprehensionKind"/>
                </xsl:call-template>
                <Expr kind="{$comprehensionKind}"/>
            </Expr>
        </xsl:otherwise>
        </xsl:choose>
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
