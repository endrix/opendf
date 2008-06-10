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
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
  xmlns:rvc="urn:mpeg:2006:01-RVC-NS" 
  version="2.0">
  <xsl:include href="bsdlGenerator.xslt"/>
  <xsl:output method="xml" omit-xml-declaration="yes"/>  
  
  <xsl:variable name="setFunc" select="'setBitCount'"/>
  <xsl:variable name="resultFunc" select="'readResult'"/>
  <xsl:variable name="completeFunc" select="'readComplete'"/>
  
  <xsl:template name="bsdlActor">
    <xsl:param name="children"/>
    <xsl:param name="inputs"/>
    <xsl:param name="outputs"/>
    <xsl:param name="imports"/>
    <Actor name="bsdl">
      <xsl:copy-of select="$imports"/>
      <xsl:copy-of select="$inputs"/>
      <xsl:copy-of select="$outputs"/>
      <xsl:copy-of select="$children"/>  
    </Actor>      
  </xsl:template>
  
  <xsl:template name="rvcImport">
    <xsl:param name="name" required="yes"/>
    <xsl:call-template name="import">
      <xsl:with-param name="name" select="('org','iso','mpeg','rvc','objects',$name)"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="import">
    <!-- should be a sequence with each package in a slot-->
    <xsl:param name="name" required="yes"/>
    <xsl:param name="kind" select="'single'"/>
    <Import>
      <xsl:attribute name="kind" select="$kind"/>
      <xsl:call-template name="qid">
        <xsl:with-param name="name" select="$name"/>
      </xsl:call-template>
    </Import>
    <xsl:text xml:space="preserve">
    </xsl:text>
  </xsl:template>
  
  <xsl:template name="qid">
    <xsl:param name="name" required="yes"/>
    <QID>
      <xsl:attribute name="name" select="string-join($name,'.')"/>
      <xsl:for-each select="$name">
        <ID>
          <xsl:attribute name="name" select="."/>
        </ID>
      </xsl:for-each>
    </QID>
  </xsl:template>
  
  <xsl:template name="inputPort">
    <Port kind="Input" name="bits">
      <Type name="bool"/>
    </Port>
    <xsl:text xml:space="preserve">
    </xsl:text>
  </xsl:template>
  
  <xsl:template name="outputPort">
    <xsl:param name="name" required="yes"/>
    <Port kind="Output">
      <xsl:attribute name="name" select="$name"/>
    </Port>
    <xsl:text xml:space="preserve">
    </xsl:text>
  </xsl:template>
  
  <xsl:template name="bitAction">
    <Decl assignable="Yes" kind="Variable" name="bitCount">
      <Type name="int">
        <Entry kind="Expr" name="size">
          <Expr kind="Literal" literal-kind="Integer" value="7"/>
        </Entry>
      </Type>
      <Expr kind="UnaryOp">
        <Op name="-"/>
        <Expr kind="Literal" literal-kind="Integer" value="1"/>
      </Expr>
    </Decl>
    <Decl assignable="Yes" kind="Variable" name="result">
      <Type name="int">
        <Entry kind="Expr" name="size">
          <Expr kind="Literal" literal-kind="Integer" value="33"/>
        </Entry>
      </Type>
    </Decl>
    <Decl kind="Variable">
      <xsl:attribute name="name" select="$setFunc"/>
      <Type infer="true" kind="Procedure"/>
      <Expr kind="Proc">
        <Decl kind="Parameter" name="count">
          <Type name="int"/>
        </Decl>
        <Stmt kind="Assign" name="bitCount">
          <Expr kind="BinOpSeq">
            <Expr kind="Var" name="count"/>
            <Op name="-"/>
            <Expr kind="Literal" literal-kind="Integer" value="1"/>
          </Expr>
        </Stmt>
        <Stmt kind="Assign" name="result">
          <Expr kind="Literal" literal-kind="Integer" value="0"/>
        </Stmt>
      </Expr>
    </Decl>
    <Decl kind="Variable">
      <xsl:attribute name="name" select="$completeFunc"/>
      <Type infer="true" kind="Function"/>
      <Expr kind="Lambda">
        <Type name="bool"/>
        <Expr kind="BinOpSeq">
          <Expr kind="Var" name="bitCount"/>
          <Op name="&lt;"/>
          <Expr kind="Literal" literal-kind="Integer" value="0"/>
        </Expr>
      </Expr>
    </Decl>
    <Decl kind="Variable">
      <xsl:attribute name="name" select="$resultFunc"/>
      <Type infer="true" kind="Function"/>
      <Expr kind="Lambda">
        <Type name="int"/>
        <Expr kind="Var" name="result"/>
      </Expr>
    </Decl>
    <Action>
      <Input kind="Elements" port="bits">
        <Decl kind="Input" name="b"/>
      </Input>
      <Guards>
        <Expr kind="UnaryOp">
          <Op name="not"/>
          <Expr kind="Application">
            <Expr kind="Var" name="done_reading_bits"/>
            <Args/>
          </Expr>
        </Expr>
      </Guards>
      <Stmt kind="Assign" name="result">
        <Expr kind="Application">
          <Expr kind="Var" name="bitor"/>
          <Args>
            <Expr kind="Application">
              <Expr kind="Var" name="lshift"/>
              <Args>
                <Expr kind="Var" name="result"/>
                <Expr kind="Literal" literal-kind="Integer" value="1"/>
              </Args>
            </Expr>
            <Expr kind="If">
              <Expr kind="Var" name="b"/>
              <Expr kind="Literal" literal-kind="Integer" value="1"/>
              <Expr kind="Literal" literal-kind="Integer" value="0"/>
            </Expr>
          </Args>
        </Expr>
      </Stmt>
      <Stmt kind="Assign" name="bitCount">
        <Expr kind="BinOpSeq">
          <Expr kind="Var" name="bitCount"/>
          <Op name="-"/>
          <Expr kind="Literal" literal-kind="Integer" value="1"/>
        </Expr>
      </Stmt>
    </Action>
    <xsl:text xml:space="preserve">
    </xsl:text>
  </xsl:template>
  
  <xsl:template name="appDecl">
    <xsl:param name="name" required="yes"/>
    <xsl:param name="type" required="yes"/>
    <Decl kind="Variable">
      <xsl:attribute name="name" select="$name"/>
      <Expr kind="Application">
        <Expr kind="Var">
        <xsl:attribute name="name" select="$type"/>
        </Expr>
        <Args/>
      </Expr>
    </Decl>    
    <xsl:text xml:space="preserve">
    </xsl:text>
  </xsl:template>
  
  <xsl:template name="intDeclaration">
    <xsl:param name="name" required="yes"/>
    <xsl:param name="initialValue" as="xsd:integer" required="no" select="0"/>
    <Decl assignable="Yes" kind="Variable">
      <xsl:attribute name="name" select="$name"/>
      <Type name="int"/>
      <Expr kind="Literal" literal-kind="Integer">
        <xsl:attribute name="value" select="$initialValue"/>
      </Expr>
    </Decl>    
    <xsl:text xml:space="preserve">
    </xsl:text>
  </xsl:template>
  
  <xsl:template name="fsm">
    <xsl:param name="initialState"/>
    <xsl:param name="transitions"/>
        <Schedule kind="fsm">
      <xsl:attribute name="initial-state" select="$initialState"/>
      <xsl:text xml:space="preserve">
      </xsl:text>
          <xsl:copy-of select="$transitions"/>
</Schedule>      
  </xsl:template>
  
  <xsl:template name="transition">
    <xsl:param name="from" required="yes"/>
    <xsl:param name="to" required="yes"/>
    <xsl:param name="action" required="yes"/>
    <Transition>
      <xsl:attribute name="from" select="rvc:qid($from)"/>
      <xsl:attribute name="to" select="rvc:qid($to)"/>
      <ActionTags>
        <xsl:call-template name="qid">
          <xsl:with-param name="name" select="$action"/>
        </xsl:call-template>
      </ActionTags>
    </Transition>
    <xsl:text xml:space="preserve">
    </xsl:text>
  </xsl:template>

  <xsl:function name="rvc:qid">
     <xsl:param name="ids"/>
    <xsl:value-of select="string-join($ids,'_')"/>
  </xsl:function>
</xsl:stylesheet>