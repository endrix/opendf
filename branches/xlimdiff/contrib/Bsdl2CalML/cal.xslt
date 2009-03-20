<?xml version="1.0" encoding="UTF-8"?>
<!--*
  * Copyright(c)2008, Samuel Keller, Christophe Lucarz, Joseph Thomas-Kerr 
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *     * Redistributions of source code must retain the above copyright
  *       notice, this list of conditions and the following disclaimer.
  *     * Redistributions in binary form must reproduce the above copyright
  *       notice, this list of conditions and the following disclaimer in the
  *       documentation and/or other materials provided with the distribution.
  *     * Neither the name of the EPFL, University of Wollongong nor the
  *       names of its contributors may be used to endorse or promote products
  *       derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY  Samuel Keller, Christophe Lucarz, 
  * Joseph Thomas-Kerr ``AS IS'' AND ANY 
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL  Samuel Keller, Christophe Lucarz, 
  * Joseph Thomas-Kerr BE LIABLE FOR ANY
  * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *-->
<!DOCTYPE stylesheet SYSTEM "entities.dtd">
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
  
  <xsl:output method="xml" indent="yes" /> 

  <xsl:variable name="setFunc" select="'setBitCount'"/>
  
  <xsl:variable name="resultFunc" select="'readResult'"/>
  
  <xsl:variable name="completeFunc" select="'readComplete'"/>
  
  
  <xsl:template name="actor">
    <xsl:param name="name" required="yes"/>
    
    <xsl:param name="children"/>
    
    <xsl:param name="inputs"/>
    
    <xsl:param name="outputs"/>
    
    <xsl:param name="imports"/>
    
    <xsl:param name="parameters"/>
    
    <!--   <?xml version="1.0" encoding="UTF-8"?>-->
    <xsl:comment> Generated by BSDL to CALML Parser version 1.0</xsl:comment>
    <xsl:text>&nl;</xsl:text>
    <Actor>
      <xsl:attribute name="name" >
        <xsl:value-of select="$name"/>
      </xsl:attribute>
      <xsl:copy-of select="$imports" copy-namespaces="no"/>
      <xsl:copy-of select="$parameters" copy-namespaces="no"/>
      <xsl:copy-of select="$inputs" copy-namespaces="no"/>
      <xsl:copy-of select="$outputs" copy-namespaces="no"/>
      <xsl:copy-of select="$children" copy-namespaces="no"/>  
    </Actor>
    
  </xsl:template>
  
  
  
  <xsl:template name="import">
    
    <!-- should be a sequence with each package in a slot-->
    
    <xsl:param name="name" required="yes"/>
    
    <xsl:param name="kind" select="'single'"/>
    
    <Import>
      <xsl:attribute name="kind">
        <xsl:choose>
          <xsl:when test="$kind">
            <xsl:value-of select="$kind"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>single</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <QID>
        <xsl:attribute name="name">
          <xsl:value-of select="string-join($name,'.')"/>
        </xsl:attribute>
        
        <xsl:for-each select="$name">
          <ID>
            <xsl:attribute name="name">
              <xsl:value-of select="."/>
            </xsl:attribute>
          </ID>
        </xsl:for-each>
      </QID>
    </Import>
  </xsl:template>
  
  <xsl:template name="port">
    
    <xsl:param name="name" required="yes"/>
    <xsl:param name="type" required="no"/>
    <xsl:param name="kind" required="no"/>
    
    <Port>
      <xsl:attribute name="kind">
        <xsl:choose>
          <xsl:when test="$kind">
            <xsl:value-of select="$kind"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>Output</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      
      <xsl:attribute name="name">
        <xsl:value-of select="$name"/>
      </xsl:attribute>
      
      <Type>
        <xsl:attribute name="name">
          <xsl:choose>
            <xsl:when test="not(empty($type)) and string-length($type) &gt; 0">
              <xsl:value-of select="$type"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>string</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </Type>
    </Port>
    
  </xsl:template>
  
  
  <xsl:template name="bitAction">
    
    <Decl assignable="Yes" kind="Variable" name="output">
      <Type name="int"/>
    </Decl>
    
    <Decl kind="Variable" name="bool2int">
      <Type infer="true" kind="Procedure"/>
      <Expr kind="Proc">
        <Decl kind="Parameter" name="b">
          <Type name="bool"/>
        </Decl>
        <Decl kind="Parameter" name="size">
          <Type name="int"/>
        </Decl>
        <Decl assignable="Yes" kind="Variable" name="i">
          <Type name="int"/>
          <Expr kind="Var" name="size"/>
        </Decl>
        <Stmt kind="Assign" name="output">
          <Expr kind="Literal" literal-kind="Integer" value="0"/>
        </Stmt>
        <Stmt kind="While">
          <Expr kind="BinOpSeq">
            <Expr kind="Var" name="i"/>
            <Op name="&gt;"/>
            <Expr kind="Literal" literal-kind="Integer" value="0"/>
          </Expr>
          <Stmt kind="Block">
            <Stmt kind="If">
              <Expr kind="Indexer">
                <Expr kind="Var" name="b"/>
                <Args>
                  <Expr kind="BinOpSeq">
                    <Expr kind="Var" name="size"/>
                    <Op name="-"/>
                    <Expr kind="Var" name="i"/>
                  </Expr>
                </Args>
              </Expr>
              <Stmt kind="Block">
                <Stmt kind="Assign" name="output">
                  <Expr kind="BinOpSeq">
                    <Expr kind="Var" name="output"/>
                    <Op name="+"/>
                    <Expr kind="Application">
                      <Expr kind="Var" name="lshift"/>
                      <Args>
                        <Expr kind="Literal" literal-kind="Integer" value="1"/>
                        <Expr kind="BinOpSeq">
                          <Expr kind="Var" name="i"/>
                          <Op name="-"/>
                          <Expr kind="Literal" literal-kind="Integer" value="1"/>
                        </Expr>
                      </Args>
                    </Expr>
                  </Expr>
                </Stmt>
              </Stmt>
            </Stmt>
            <Stmt kind="Assign" name="i">
              <Expr kind="BinOpSeq" >
                <Expr kind="Var" name="i"/>
                <Op name="-"/>
                <Expr kind="Literal" literal-kind="Integer" value="1"/>
              </Expr>
            </Stmt>
          </Stmt>
        </Stmt>
      </Expr>
    </Decl>
    
    <Decl kind="Variable" name="numbits">
      <Type infer="true" kind="Function"/>
      <Expr kind="Lambda">
        <Decl kind="Parameter" name="value"/>
        <Expr kind="If">
          <Expr kind="BinOpSeq">
            <Expr kind="Var" name="value"/>
            <Op name="&gt;"/>
            <Expr kind="Literal" literal-kind="Integer" value="1"/>
          </Expr>
          <Expr kind="BinOpSeq">
            <Expr kind="Literal" literal-kind="Integer" value="1"/>
            <Op name="+"/>
            <Expr kind="Application">
              <Expr kind="Var" name="numbits"/>
              <Args>
                <Expr kind="BinOpSeq">
                  <Expr kind="Var" name="value"/>
                  <Op name="/"/>
                  <Expr kind="Literal" literal-kind="Integer" value="2"/>
                </Expr>
              </Args>
            </Expr>
          </Expr>
          <Expr kind="Literal" literal-kind="Integer" value="1"/>
        </Expr>
      </Expr>
    </Decl>
    
    <Decl kind="Variable" name="max">
      <Type infer="true" kind="Function"/>
      <Expr kind="Lambda">
        <Decl kind="Parameter" name="value1"/>
        <Decl kind="Parameter" name="value2"/>
        <Expr kind="If">
          <Expr kind="BinOpSeq">
            <Expr kind="Var" name="value1"/>
            <Op name="&gt;"/>
            <Expr kind="Var" name="value2"/>
          </Expr>
          <Expr kind="Var" name="value1"/>
          <Expr kind="Var" name="value2"/>
        </Expr>
      </Expr>
    </Decl>
    
    <Decl kind="Variable" name="min">
      <Type infer="true" kind="Function"/>
      <Expr kind="Lambda">
        <Decl kind="Parameter" name="value1"/>
        <Decl kind="Parameter" name="value2"/>
        <Expr kind="If">
          <Expr kind="BinOpSeq">
            <Expr kind="Var" name="value1"/>
            <Op name="&lt;"/>
            <Expr kind="Var" name="value2"/>
          </Expr>
          <Expr kind="Var" name="value1"/>
          <Expr kind="Var" name="value2"/>
        </Expr>
      </Expr>
    </Decl>
    
    <xsl:call-template name="action">
      <xsl:with-param name="name">
        <xsl:call-template name="qid">
          <xsl:with-param name="name">
            <xsl:text>&skipAction;</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
    
    <xsl:call-template name="action">
      <xsl:with-param name="name">
        <xsl:call-template name="qid">
          <xsl:with-param name="name">
            <xsl:text>&errorAction;</xsl:text>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
      <xsl:with-param name="do">
        <Stmt kind="Call">
          <Expr kind="Var" name="println"/>
          <Args>
            <Expr kind="Literal" literal-kind="String" value="The bitstream is not conformed to the description"/>
          </Args>
        </Stmt>
      </xsl:with-param>
    </xsl:call-template>
    
    <xsl:call-template name="alignAction">
      <xsl:with-param name="name"><xsl:text>&align8Action;</xsl:text></xsl:with-param>
      <xsl:with-param name="size">8</xsl:with-param>
    </xsl:call-template>
    
    <xsl:call-template name="alignAction">
      <xsl:with-param name="name"><xsl:text>&align16Action;</xsl:text></xsl:with-param>
      <xsl:with-param name="size">16</xsl:with-param>
    </xsl:call-template>
    
    <xsl:call-template name="alignAction">
      <xsl:with-param name="name"><xsl:text>&align32Action;</xsl:text></xsl:with-param>
      <xsl:with-param name="size">32</xsl:with-param>
    </xsl:call-template>
    
  </xsl:template>
  
  <xsl:template name="alignAction">
    <xsl:param name="name" required="yes"/>
    <xsl:param name="size" required="yes"/>
    
    <xsl:call-template name="action">
      <xsl:with-param name="name">
        <xsl:call-template name="qid">
          <xsl:with-param name="name">
            <xsl:value-of select="$name"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
      <xsl:with-param name="inputs">
        <xsl:call-template name="input"/>
      </xsl:with-param>
      <xsl:with-param name="guard">
        <xsl:call-template name="guards">
          <xsl:with-param name="guard">
            <Expr kind="BinOpSeq">
              <Expr kind="Var" name="&bitNumber;"/>
              <Op name="mod"/>
              <Expr kind="Literal" literal-kind="Integer">
                <xsl:attribute name="value">
                  <xsl:value-of select="$size"/>
                </xsl:attribute>
              </Expr>
              <Op name="!="/>
              <Expr kind="Literal" literal-kind="Integer" value="0"/>
            </Expr>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
      
      <xsl:with-param name="do">
        <Stmt kind="Assign" name="&bitNumber;">
          <Expr kind="BinOpSeq">
            <Expr kind="Var" name="&bitNumber;"/>
            <Op name="+"/>  
            <Expr kind="Literal" literal-kind="Integer" value="1"/>
          </Expr>  
        </Stmt>  
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="guards">
    <xsl:param name="guard" required="no"/>
    <Guards>
      <xsl:copy-of select="$guard" copy-namespaces="no"/>
    </Guards>
  </xsl:template>
  
  <xsl:template name="action">
    <xsl:param name="name" required="no"/>
    <xsl:param name="inputs" required="no"/>
    <xsl:param name="outputs" required="no"/>
    <xsl:param name="guard" required="no"/>
    <xsl:param name="do" required="no"/>
    
    <Action>
      <xsl:copy-of select="$name" copy-namespaces="no"/>
      <xsl:copy-of select="$inputs" copy-namespaces="no"/>
      <xsl:copy-of select="$outputs" copy-namespaces="no"/>
      <xsl:copy-of select="$guard" copy-namespaces="no"/>
      <xsl:copy-of select="$do" copy-namespaces="no"/>
    </Action>
  </xsl:template>
  
  <xsl:template name="variableDeclaration">
    
    <xsl:param name="name" required="yes"/>
    
    <xsl:param name="initialValue"/>
    <xsl:param name="type"/>
    <xsl:variable name="initial0"><xsl:value-of select="$initialValue"/></xsl:variable> <!-- necessary to serialize sequences... -->
   
    <Decl kind="Variable"> 
      <xsl:attribute name="name" select="$name"/>
      
      <xsl:if test="$type">
        <Type> 
          <xsl:attribute name="name">
            <xsl:value-of select="$type"/>
          </xsl:attribute>
        </Type>
        <xsl:if test="string-length($initial0) &gt; 0">
          <xsl:if test="$type = 'int'">
            <Expr kind="Literal" literal-kind="Integer">
              <xsl:attribute name=" value" select="$initial0"/>
            </Expr>
          </xsl:if>
        </xsl:if>
      </xsl:if>
    </Decl>
    
  </xsl:template>
  
  
  
  <xsl:template name="fsm">
    
    <xsl:param name="initialState"/>
    
    <xsl:param name="transitions"/>
    
    <Schedule kind="fsm">
      <xsl:attribute name="initial-state" select="$initialState"/>
      
      <xsl:copy-of select="$transitions" copy-namespaces="no"/>
    </Schedule>
    
  </xsl:template>
  
  <xsl:template name="statement">
    <xsl:param name="expressions" required="no"/>
    <xsl:if test="$expressions">
      <xsl:copy-of select="$expressions" copy-namespaces="no"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="transition">
    <xsl:param name="from" required="yes"/>
    <xsl:param name="to" required="yes"/>
    <xsl:param name="action" required="yes"/>
    <Transition> 
      <xsl:attribute name="from" select="$from"/>
      <xsl:attribute name="to" select="$to"/>
      <ActionTags>
        <xsl:copy-of select="$action" copy-namespaces="no"/>
      </ActionTags>
    </Transition >
  </xsl:template>
  
  <xsl:template name="qid">
    <xsl:param name="name" required="yes"/>
    <xsl:param name="suffix" required="no"/>
    
    <QID> 
      <xsl:attribute name="name">
        <xsl:value-of select="$name"/>
        <xsl:if test="$suffix">
          <xsl:text>.</xsl:text>
          <xsl:value-of select="$suffix"/>
        </xsl:if>
      </xsl:attribute>
      
      <ID>
        <xsl:attribute name="name" select="$name"/>
      </ID>
      
      <xsl:if test="$suffix">
        <ID>
          <xsl:attribute name="name" select="$suffix"/>
        </ID>
      </xsl:if>
      
    </QID>
  </xsl:template>
  <!-- 
    <xsl:template name="priorities">
    <xsl:param name="priorities" required="yes"/>
    <xsl:text>&nl;&tab;priority&nl;</xsl:text>
    <xsl:value-of select="$priorities"/>
    <xsl:text>&tab;end&nl;</xsl:text>
    </xsl:template>-->
  
  <xsl:template name="priority">
    <xsl:param name="greater" required="yes"/>
    <xsl:param name="lesser" required="yes"/>
    <Priority>
      <xsl:copy-of select="$greater" copy-namespaces="no"/>
      <xsl:copy-of select="$lesser" copy-namespaces="no"/>
    </Priority>
  </xsl:template>
  
</xsl:stylesheet>

