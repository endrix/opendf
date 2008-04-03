<!--
    ReplaceOperators

    Replaces unary and binary operators with the appropriate function calls.

    assumes: ContextInfoAnnotator

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="2.0"
  xmlns:exsl="http://exslt.org/common"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="xsl exsl set">

  <xsl:output method="xml"/>


    <xsl:template match="Expr[@kind='UnaryOp']">
        <Expr kind="Application">
            <Expr kind="Var" name="{Op/@function}" old="no"/>
            <Args>
                <xsl:apply-templates select="Expr"/>
            </Args>
        </Expr>
    </xsl:template>

    <xsl:template match="Expr[@kind='BinOpSeq']">
        <xsl:call-template name="replaceBinOps">
            <xsl:with-param name="ops" select="Op"/>
            <xsl:with-param name="exprs" select="Expr"/>
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
        Compute the expression that corresponds to a BinOpSeq expression.
    -->
    <xsl:template name="replaceBinOps">
        <xsl:param name="ops"/>
        <xsl:param name="exprs"/>

        <!-- Turn $ops into proper tree. -->
        <xsl:variable name="oplist"><Ops><xsl:copy-of select="$ops"/></Ops></xsl:variable>
        <!-- First compute the lowest precedence of all operators. -->
        <xsl:variable name="minPrec" select="min($ops/@precedence)"/>

        <!-- Now compute the index of its last occurrence. Count the number of nodes which do not have a
             minPrec node as one of their preceding siblings.
        -->
        <xsl:variable name="pos" select="count($oplist/Ops/Op[some $x in following-sibling::Op satisfies $x/@precedence = $minPrec]) + 1"/>
        
        <!-- If no operator, just process the (single) expression. Otherwise, split operator and operand lists,
             and create an application of the appropriate function with the left and the right subexpressions as its
             arguments.
        -->
        <!-- <AAA prec="{$minPrec}" pos="{$pos}"/> -->
        <xsl:choose>
        <xsl:when test="count($ops)=0">
            <xsl:apply-templates select="$exprs"/></xsl:when>
        <xsl:otherwise>
            <Expr kind="Application">
                <Expr kind="Var" name="{$ops[position()=$pos]/@function}" old="no"/>
                <Args>
                    <xsl:call-template name="replaceBinOps">
                        <xsl:with-param name="ops" select="$ops[position() lt $pos]"/>
                        <xsl:with-param name="exprs" select="$exprs[position() le $pos]"/>
                    </xsl:call-template>
                    <xsl:call-template name="replaceBinOps">
                        <xsl:with-param name="ops" select="$ops[position() gt $pos]"/>
                        <xsl:with-param name="exprs" select="$exprs[position() gt $pos]"/>
                    </xsl:call-template>
                </Args>
            </Expr>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


</xsl:stylesheet>
