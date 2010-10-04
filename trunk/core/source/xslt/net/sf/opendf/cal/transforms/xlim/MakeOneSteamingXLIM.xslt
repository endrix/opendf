<!--
	MakeOneSteamingXLIM.xslt
BEGINCOPYRIGHT X
	
	Copyright (c) 2008, Ericsson Inc.
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT

-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:math="http://exslt.org/math"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  extension-element-prefixes="xsl math xd"  >
  
  <xsl:output method="xml" indent="yes"/>
            
  <xsl:template match="Actor">
    
    <xsl:comment>Actor <xsl:value-of select="@name"/>
    </xsl:comment>
    <design name="{@name}">
      <xsl:apply-templates select="Port"/>
      <xsl:apply-templates select="//Decl//Type[@name='List']" mode="typedef"/>
      <xsl:apply-templates select="//Decl//Note[@kind='exprType']//Type[@name='List']" mode="typedef"/>
      <xsl:apply-templates select="//Stmt//Note[@kind='exprType']//Type[@name='List']" mode="typedef"/>      
      <xsl:apply-templates select="//Output//Note[@kind='exprType']//Type[@name='List']" mode="typedef"/>      
      <xsl:apply-templates select="//Output//Type[@name='List']" mode="typedef"/>            

      <xsl:apply-templates select="Decl[ @kind='Variable' ]"/>
      <xsl:apply-templates select="Action"/>
      <!-- The default template is to ignore the node so most notes will be ignored -->      
      <xsl:apply-templates select="Note"/>
      <xsl:apply-templates select="config_option"/>
      <xsl:call-template name="scheduler">
        <xsl:with-param name="actor" select="."/>
      </xsl:call-template>
    </design>   
  </xsl:template>

  <xsl:template match="Type[@name='List']" mode="typedef">       
      <typeDef name="{concat(@id, '$typedef')}">
        <type name="List">            
          <valuePar name="size" value="{./Entry[@name='size']/Expr/@value}"/>
          <typePar name="type">
             <xsl:apply-templates select="./Entry[@kind='Type']/Type" mode="definition"/>
          </typePar>             
        </type> 
      </typeDef> 
  </xsl:template>
  
  <xsl:template match="Type[@name='int']" mode="definition">      
    <type name="int">
      <valuePar name="size" value="{./Entry/Expr/@value}"/>
    </type>    
  </xsl:template>
  
  <xsl:template match="Type[@name='uint']" mode="definition">      
    <type name="int">
      <valuePar name="size" value="{./Entry/Expr/@value}"/>
    </type>    
  </xsl:template>
  
  <xsl:template match="Type[@name='bool']" mode="definition">
    <type name="bool"/>  
  </xsl:template>

  <xsl:template match="Type[@name='real']" mode="definition">
    <type name="real"/>  
  </xsl:template>
  
  <xsl:template match="Type[@name='List']" mode="definition">
    <type name="{concat(@id, '$typedef')}"/>
  </xsl:template>
  
  <xsl:template match="Type/Entry[@kind='Type']" mode="definition">
    <xsl:apply-templates select="Entry[ @kind='Type' ]/Type"/>    
  </xsl:template>
  
  <xsl:template match="Type[@name='int']">
    <attr name="typeName" value="int"/>
    <xsl:for-each select="Entry[@kind='Expr']">
       <attr name="{@name}" value="{Expr/@value}"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="Type[@name='uint']">
    <attr name="typeName" value="int"/>
    <xsl:for-each select="Entry[@kind='Expr']">
       <attr name="{@name}" value="{Expr/@value}"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="Type[@name='bool']">
    <attr name="typeName" value="bool"/>
  </xsl:template>
  
  <xsl:template match="Type[@name='real']">
    <attr name="typeName" value="real"/>
  </xsl:template>
  
  <xsl:template match="Type[@name='List']">
    <attr name="typeName" value="{concat(@id, '$typedef')}"/>
  </xsl:template>
 
  <xsl:template match="Type" mode="init-var">
    <xsl:choose>
      <xsl:when test="Entry[ @kind='Type' ]">
        <xsl:apply-templates select="Entry[ @kind='Type' ]/Type" mode="init-var"/>
      </xsl:when>
      
      <xsl:otherwise>
        <xsl:apply-templates select="."/>        
      </xsl:otherwise>
      
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="Type">
    <!-- Missing type info, could be critical.
    <xsl:message>
      Error Here:<xsl:value-of select="."/>  
    </xsl:message>
    -->
  </xsl:template>
  
  <xsl:template match="Port">

    <xsl:variable name="name" select="@name"/>
      
    <xsl:variable name="dir">
      <xsl:choose>
        <xsl:when test="@kind='Output'">out</xsl:when>
        <xsl:otherwise>in</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="type-attrs">
      <xsl:apply-templates select="Type"/>
    </xsl:variable>
    
    <actor-port name="{@name}" dir="{$dir}">
      <xsl:for-each select="$type-attrs/attr">
        <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
      </xsl:for-each>
      <!--
      <xsl:choose>
        <xsl:when test="$dir = 'in'"/>
        <xsl:when test="../Action/Output[@port=$name][ count( Expr ) > 1]">
          <xsl:message>
            Note: multi-token write on port <xsl:value-of select="$name"/>
          </xsl:message>
        </xsl:when>
        <xsl:otherwise>
        </xsl:otherwise>
      </xsl:choose>
      -->
    </actor-port>

  </xsl:template>


  <xsl:template match="Decl[@kind='Variable']">

    <xsl:variable name="var-type-attrs">
      <xsl:apply-templates select="Type"/>
    </xsl:variable>
    
    <xsl:choose>
      <xsl:when test="parent::Actor">
        <xsl:comment>Actor state variable <xsl:value-of select="@name"/></xsl:comment>
        <stateVar name="{@id}" sourceName="{@name}">
          <xsl:for-each select="$var-type-attrs/attr">
            <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute> 
          </xsl:for-each>  
          <xsl:apply-templates select="Expr" mode="init-var"/>
          <!--
          <xsl:if test="not( Expr )">            
            <initValue value="0">
              <xsl:for-each select="$var-type-attrs/attr">
                <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute> 
              </xsl:for-each>              
            </initValue>
          </xsl:if>
          -->
        </stateVar>
      </xsl:when>

      <xsl:when test="Expr">
        <xsl:comment>Initialized local variable <xsl:value-of select="@name"/></xsl:comment>
        <xsl:apply-templates select="Expr"/>
        <operation kind="cast">
          <port source="{Expr/@id}" dir="in"/>
          <port source="{@id}" dir="out">
            <xsl:for-each select="$var-type-attrs/attr">
              <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
            </xsl:for-each>              
          </port>
        </operation>
      </xsl:when>
      
      <xsl:otherwise>
        <xsl:comment>Local variable <xsl:value-of select="@name"/> is not initialized
        </xsl:comment>
        <operation kind="$valloc">
          <port source="{@id}" dir="out">
            <xsl:for-each select="$var-type-attrs/attr">
              <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
            </xsl:for-each>              
          </port>
        </operation>
      </xsl:otherwise>
    </xsl:choose>
  
  </xsl:template>

  <xsl:template match="Action">
    <xsl:comment>Action <xsl:value-of select="@id"/></xsl:comment>

    <xsl:variable name="actionName">
      <xsl:call-template name="generateActionName">
        <xsl:with-param name="action" select="."/>
      </xsl:call-template>
    </xsl:variable>
    
    <module kind="action" name="{$actionName}" autostart="false">
      
      <!-- Attribute with maxGateDepth parameter" -->
      <xsl:variable name="mgd" select="ancestor-or-self::*/Note[ @kind='Directive' ]
        [ @name='maxGateDepth' ][1]/Expr/@value"/>
      <xsl:if test="$mgd">
        <xsl:attribute name="maxGateDepth"><xsl:value-of select="$mgd"/></xsl:attribute>
      </xsl:if>
      
      <xsl:apply-templates/>
      
      <!-- Write back scalar state variable results -->
      <xsl:for-each select="Note[@kind='var-used'][@mode='write'][@scalar='yes']">
        <xsl:comment>Write back actor state variable <xsl:value-of select="@name"/></xsl:comment>
        <operation kind="assign" target="{@decl-id}">
          <port dir="in" source="{@true-source}"/>
        </operation>
      </xsl:for-each>
    </module>
  </xsl:template>
 
  <!-- Any number of reads supported --> 
  <xsl:template match="Input">
    <xsl:variable name="port" select="@port"/>
    <xsl:choose>    
      <xsl:when test="Decl/Note[@kind='Repeat-applied']">    
        <note kind="consumptionRates" name="{$port}" value="{Decl/Note[@kind='Repeat-applied']/@value * count(Decl) }"/>  
      </xsl:when>
      <xsl:otherwise>
        <note kind="consumptionRates" name="{$port}" value="{count(Decl)}"/>       
      </xsl:otherwise>
    </xsl:choose>
               
    <xsl:for-each select="Decl">
      <xsl:variable name="type-info">
        <xsl:apply-templates select="Type" mode="definition"/>
      </xsl:variable>
 
      <operation  kind="pinRead" portName="{../@port}" removable="no" style="simple">        
      <port source="{@id}" dir="out">
        <xsl:attribute name="typeName"><xsl:value-of select="$type-info/type/@name"/></xsl:attribute>
        <xsl:for-each select="$type-info/type/valuePar">
          <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
        </xsl:for-each>
      </port>
      </operation>      
    </xsl:for-each>
      
  </xsl:template>

  <!-- Pin peeks and ready flags for all actor inputs -->
  <xsl:template match="Input" mode="peek">
    <xsl:variable name="index-size">
      <xsl:choose>
        <xsl:when test="count(Decl) &lt; 2">1</xsl:when>
        <xsl:otherwise><xsl:value-of select="ceiling( math:log(count(Decl)) div  math:log(2))"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>    
      <xsl:when test="Decl/Note[@kind='Repeat-applied']">    
        <operation kind="$literal_Integer" value="{Decl/Note[@kind='Repeat-applied']/@value * count(Decl) }"> 
          <port dir="out" size="32" source="{concat(./@id, '$',./@port, '$tokenCount')}" typeName="int"/> 
        </operation>         
      </xsl:when>
      <xsl:otherwise>
        <operation kind="$literal_Integer" value="{count(Decl)}"> 
          <port dir="out" size="32" source="{concat(./@id, '$',./@port, '$tokenCount')}" typeName="int"/> 
        </operation>         
      </xsl:otherwise>
    </xsl:choose>

    <operation kind="$ge"> 
      <port dir="in" source="{concat(./@port, '$pinAvail')}"/> 
      <port dir="in" source="{concat(./@id, '$',./@port, '$tokenCount')}"/> 
      <port dir="out" size="1" source="{concat(@id, '$ready')}" typeName="bool"/> 
    </operation>            
           
    <xsl:for-each select="Decl">
      <xsl:variable name="type-attrs">
        <xsl:apply-templates select="Type"/>
      </xsl:variable>
      <xsl:variable name="index" select="concat(@id,'$index$',string(position()) )"/>
      
      <operation kind="$literal_Integer" value="{position() - 1}">        
        <port source="{$index}" dir="out" typeName="int" size="{$index-size}"/>
      </operation>     
      <operation kind="pinPeek" portName="{../@port}">
        <port source="{$index}" dir="in"/>
        <port source="{@id}" dir="out">
          <xsl:for-each select="$type-attrs/attr">
            <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
          </xsl:for-each>
        </port>
      </operation>      
    </xsl:for-each>
    
  </xsl:template>
  
  <xsl:template match="Expr">
 
    <xsl:variable name="type-attrs">      
      <xsl:choose> 
        <!-- Somewhat of a hack to force use the typedefs for lists -->
        <xsl:when test="@kind='List' and ancestor::Decl">
          <xsl:apply-templates select="ancestor::Decl/Type"/>
        </xsl:when>
     
        <xsl:when test="@kind='Indexer'">
          <xsl:variable name="name" select="Expr[@kind='Var']/@name"/>
          <xsl:variable name="decl-id" select="Expr[@kind='Var']/Note[@kind='var-used' and @name=$name]/@decl-id"/>
          <xsl:variable name="nrOfIndices" select="count(Args/Expr)" />
          <xsl:choose>
            <xsl:when test="$nrOfIndices=1">
              <xsl:apply-templates select="//Decl[@id=$decl-id]/Type/Entry[@kind='Type']/Type"/>              
            </xsl:when>
            <xsl:when test="$nrOfIndices=2">
              <xsl:variable name="type1" select="//Decl[@id=$decl-id]/Type/Entry[@kind='Type']/Type"/>              
              <xsl:apply-templates select="$type1/Entry[@kind='Type']/Type"/>              
            </xsl:when>
            <xsl:when test="$nrOfIndices=3">
              <xsl:variable name="type1" select="//Decl[@id=$decl-id]/Type/Entry[@kind='Type']/Type"/>              
              <xsl:variable name="type2" select="$type1/Entry[@kind='Type']/Type"/>              
              <xsl:apply-templates select="$type2/Entry[@kind='Type']/Type"/>                            
            </xsl:when>
            <xsl:otherwise> 
              <xsl:message terminate="yes">
                more than 3 level indices not supported
              </xsl:message>
            </xsl:otherwise>            
          </xsl:choose>  
        </xsl:when>

        <xsl:when test="@kind='Let' and ./Expr[@kind='List'] and ancestor::Decl">         
          <xsl:apply-templates select="ancestor::Decl/Type"/>
        </xsl:when>
        
        <xsl:when test="Note[@kind='exprType']">
          <!-- Get the type assigned by the expr evaluator -->    
          <xsl:apply-templates select="Note[@kind='exprType']/Type"/>      
        </xsl:when>
        
        <xsl:when test="../Note[@kind='exprType']">
          <xsl:apply-templates select="../Note[@kind='exprType']/Type/Entry[@kind='Type']/Type"/>
        </xsl:when>   
        
        <xsl:when test="../Type and ancestor::Output">
          <xsl:apply-templates select="../Type"/>
        </xsl:when>

        <xsl:when test="../../Type and ancestor::Output">
          <xsl:apply-templates select="../../Type"/>
        </xsl:when>
        

        <xsl:when test="@kind='Literal'">
          <xsl:apply-templates select="."  mode="flatten-expr"/>
        </xsl:when>  
        
        <xsl:otherwise>
          <xsl:message>
            <xsl:value-of select="@id"/> has no exprType
          </xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <!-- Get the flattened version of this expression -->  
    <xsl:variable name="expr">
      <xsl:apply-templates select="." mode="flatten-expr"/>
    </xsl:variable>
    
    <!-- Copy all the preceding expressions -->
    <xsl:copy-of select="$expr/expr/(* | comment())"/>
    
    <!-- Copy all the preceding operations -->
    <xsl:copy-of select="$expr/operation"/>
    
    <!-- Create the final expression -->
    <xsl:comment>Expr <xsl:value-of select="@id"/></xsl:comment>
    <operation>
      <!-- Add operation attributes provided by the flattener template -->
      <xsl:for-each select="$expr/attr">
        <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
      </xsl:for-each>
      
      <!-- Add the ports created by the flattener -->
      <xsl:for-each select="$expr/port">
        <xsl:copy>                
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>
          
          <!-- Output ports of the operation get the expr type added -->
          <xsl:if test="@dir = 'out'">
            <xsl:for-each select="$type-attrs/attr">
              <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
            </xsl:for-each>
          </xsl:if>
          
        </xsl:copy>
      </xsl:for-each>
    </operation>
  </xsl:template>

  <xsl:template match="Expr[@kind = 'Literal' ]" mode="flatten-expr">
    <!-- TODO fix hard-wired literal type -->
    <attr name="kind" value="$literal_Integer"/>
    <attr name="value" value="{@value}"/>
    <xsl:choose>
      <xsl:when test="@literal-kind='Integer'">
        <port source="{@id}" dir="out" typeName="int" size="32"/>
      </xsl:when>
      <xsl:otherwise>
        <port source="{@id}" dir="out" />        
      </xsl:otherwise>
    </xsl:choose>    
  </xsl:template>
  
  <xsl:template match="Expr[@kind = 'List' ]" mode="flatten-expr">
    <xsl:variable name="type-attrs">
       <xsl:apply-templates select="ancestor::Decl/Type" mode="init-var"/>
    </xsl:variable>
    
    <attr name="kind" value="$vcons"/>
    <xsl:for-each select="./Expr">     
      <xsl:apply-templates select="." />
      <port dir="in" source="{./@id}">
        <xsl:for-each select="$type-attrs/attr">
          <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
        </xsl:for-each>   
      </port>
    </xsl:for-each>
    <port source="{@id}" dir="out"/>
  </xsl:template>
  
  <xsl:template match="Expr[@kind = 'Application' ]" mode="flatten-expr">
    <expr>
      <xsl:apply-templates select="Args/Expr"/>
    </expr>
    <attr name="kind" value="{Expr/@name}"/>
    <xsl:for-each select="Args/Expr">
      <port source="{@id}" dir="in"/>
    </xsl:for-each>
    <port source="{@id}" dir="out"/>
  </xsl:template>

  <xsl:template match="Expr[@kind = 'If' ]" mode="flatten-expr">
    <expr>
      <xsl:apply-templates select="Expr"/>
    </expr>
    <attr name="kind" value="$selector"/>
    <xsl:for-each select="Expr">
      <port source="{@id}" dir="in"/>
    </xsl:for-each>
    <port source="{@id}" dir="out"/>
  </xsl:template>
  
  <xsl:template match="Expr[@kind = 'Let' ]" mode="flatten-expr">
    <expr>
      <xsl:apply-templates select="Decl"/>
      <xsl:apply-templates select="Expr"/>
    </expr>
    <attr name="kind" value="noop"/>
    <port source="{Expr/@id}" dir="in"/>
    <port source="{@id}" dir="out"/>
  </xsl:template>
  
  <xsl:template match="Expr[@kind = 'Var' ]" mode="flatten-expr">
    <xsl:choose>
      <xsl:when test="Note[@actor-scope='yes'] and Note[@scalar='no']">
        <attr name="kind" value="noop"/>
        <port source="{Note[@kind='var-used']/@decl-id}" dir="in"/>
        <port source="{@id}" dir="out"/>        
      </xsl:when>
      <xsl:otherwise>
        <attr name="kind" value="noop"/>
        <port source="{Note[@kind='var-used']/@true-source}" dir="in"/>
        <port source="{@id}" dir="out"/>        
      </xsl:otherwise>      
    </xsl:choose>
  </xsl:template>

  <xsl:template match="Expr[@kind = 'Indexer' ]" mode="flatten-expr">
    <expr>
      <xsl:apply-templates select="." mode="generate-indexers"/> 
    </expr>
    <xsl:variable name="target-name" select="Expr[@kind='Var']/@name"/>      
    <attr name="kind" value="var_ref"/>
    <xsl:choose>
      <xsl:when test="Expr/Note[@actor-scope='yes']">
        <!-- Note that the true source for a memory is the original decl, not the previous writer --> 
        <attr name="name" value="{Expr/Note[@kind='var-used']/@decl-id}"/>        
      </xsl:when>
      <xsl:otherwise>
        <attr name="name" value="{Expr/Note[@kind='var-used'][@name=$target-name]/@true-source}"/>  -
      </xsl:otherwise>
    </xsl:choose>
    <port source="{concat(Args/Expr[last()]/@id,'$ADDR')}" dir="in"/>
    <port source="{@id}" dir="out"/>
  </xsl:template>
  
  <xsl:template match="*" mode="flatten-expr">
    <xsl:message>
      Unhandled Expr kind="<xsl:value-of select="@kind"/>", id="<xsl:value-of select="@id"/>
    </xsl:message> 
  </xsl:template>
  
  <xsl:template match="Output">
    <xsl:apply-templates select="Expr"/>
    <xsl:variable name="type-attrs">      
      <xsl:apply-templates select="Type"/>
    </xsl:variable>
    <xsl:for-each select="Expr">
      
      <operation kind="cast">
        <port dir="in" source="{@id}"/>
        <port dir="out" source="{concat(@id,'$output')}">
          <xsl:for-each select="$type-attrs/attr">
            <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
          </xsl:for-each>          
        </port>        
      </operation>
      
      <operation kind="pinWrite" portName="{../@port}" style="simple">
        <port dir="in" source="{concat(@id,'$output')}"/>
      </operation>
    </xsl:for-each>
    <xsl:choose>
      <xsl:when test="Note[@kind='Repeat-applied']">
        <note kind="productionRates" name="{@port}" value="{count(Expr) * Note[@kind='Repeat-applied']/@value}"/>    
      </xsl:when>
      <xsl:otherwise>
        <note kind="productionRates" name="{@port}" value="{count(Expr)}"/>    
      </xsl:otherwise>              
    </xsl:choose>
  </xsl:template>

  <xsl:template match="Stmt[ @kind='Assign' ]">
    
    <xsl:variable name="type-attrs">
      <xsl:apply-templates select="Note[@kind='varMod']/Type"/>
    </xsl:variable>
    
    <xsl:apply-templates select="Expr"/>
    
    <xsl:comment>Statement <xsl:value-of select="@id"/></xsl:comment>
    
    <xsl:variable name="target-name" select="@name"/>
    
    <xsl:choose>
      <!-- Indexed assign -->
   
      <xsl:when test="Args and Note[@actor-scope='no']" >
        <!-- We treat element wise writes to local lists as read access to the list, not write -->
        <xsl:apply-templates select="." mode="generate-indexers"/>
        <xsl:comment>Assign values to local lists <xsl:value-of select="@name"/></xsl:comment>        
        <operation kind="assign" target="{Note[@kind='var-used'][@name=$target-name][@mode='mutate']/@true-source}">
          <port source="{concat(Args/Expr[last()]/@id,'$ADDR')}" dir="in"/>
          <port source="{Expr/@id}" dir="in"/>
        </operation>          
      </xsl:when>
      
      <xsl:when test="Args" >
        <xsl:apply-templates select="." mode="generate-indexers"/>
        <xsl:comment>Write back actor state variable <xsl:value-of select="@name"/></xsl:comment>
        <operation kind="assign" target="{Note[@kind='varMod']/@decl-id}">
          <port source="{concat(Args/Expr[last()]/@id,'$ADDR')}" dir="in"/>
          <port source="{Expr/@id}" dir="in"/>
        </operation>          
      </xsl:when>

      <xsl:when test="Note[@actor-scope='yes'] and Note[@kind='var-used'][@mode='write'][@scalar='no']" >               
        <xsl:comment>Assign global list <xsl:value-of select="@name"/></xsl:comment>        
        <operation kind="assign" target="{Note[@kind='varMod']/@decl-id}">          
          <port source="{Expr/@id}" dir="in"/>
        </operation>          
      </xsl:when>
                      
      <!-- Scalar assignment: the Expr becomes the true-source for this variable -->
      <xsl:otherwise> 
        <!-- FIX ME : introduce cast to match type of lhs -->
        <operation kind="noop">
          <port dir="in" source="{Expr/@id}"/>
          <port source="{@id}" dir="out">
            <xsl:for-each select="$type-attrs/attr">
              <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
            </xsl:for-each>
          </port>
        </operation>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>

  <xsl:template match="Stmt[@kind='If']">
    <xsl:variable name="stmt-id" select="@id"/>

    <module kind="if">
      <module kind="test" decision="{Expr/@id}">
        <xsl:apply-templates select="Expr"/>
      </module>
      <module kind="then">
        <xsl:apply-templates select="Stmt[1]"/>
        <xsl:call-template name="balance-if-writes">
          <xsl:with-param name="stmt" select="."/>
          <xsl:with-param name="branch">THEN</xsl:with-param>
        </xsl:call-template>
      </module>
<!--      <xsl:if test="Stmt[2] or Note[@kind='var-used'][@mode='write'][@scalar='yes']"> -->
      <xsl:if test="Stmt[2] or Note[@kind='var-used'][@mode='write']">          
        <!-- We need an else block if there is one in the source, or if there are
         writes to scalars in the then block -->
        <module kind="else">
          <xsl:apply-templates select="Stmt[2]"/>
          <xsl:call-template name="balance-if-writes">
            <xsl:with-param name="stmt" select="."/>
            <xsl:with-param name="branch">ELSE</xsl:with-param>
          </xsl:call-template>
        </module>
      </xsl:if>
            
      <xsl:for-each select="Note[@kind='var-used'][@mode='write']">
        <xsl:variable name="decl-id" select="@decl-id"/>
        <xsl:if test="@scalar='yes' or not(//Actor/Decl[@id=$decl-id])">
          <xsl:variable name="decl-id" select="@decl-id"/>
          <xsl:variable name="then" select="../Stmt[1]/Note[@kind='var-used'][@mode='write'][@decl-id=$decl-id]"/>
          <xsl:variable name="else" select="../Stmt[2]/Note[@kind='var-used'][@mode='write'][@decl-id=$decl-id]"/>
          <PHI>
            <xsl:choose>
              <xsl:when test="$then and $else">
                <port source="{$then/@true-source}" qualifier="then" dir="in"/>
                <port source="{$else/@true-source}" qualifier="else" dir="in"/>
              </xsl:when>
              <xsl:when test="$then">
                <port source="{$then/@true-source}" qualifier="then" dir="in"/>
                <port source="{concat($stmt-id,'$ELSE$',$decl-id)}" qualifier="else" dir="in"/>
              </xsl:when>
              <xsl:when test="$else">
                <port source="{concat($stmt-id,'$THEN$',$decl-id)}" qualifier="then" dir="in"/>
                <port source="{$else/@true-source}" qualifier="else" dir="in"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:message>
                  Fatal inconsistency in If block notes
                </xsl:message>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:variable name="phi" select="concat($stmt-id,'$PHI$',$decl-id)"/>
            <port source="{$phi}" dir="out">
              <xsl:variable name="type-attrs">
                <xsl:apply-templates select="..//Note[@kind='varMod'][@decl-id=$decl-id][1]/Type"/>
              </xsl:variable>
              <xsl:for-each select="$type-attrs/attr">
                <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
              </xsl:for-each>
            </port>
          </PHI>
        </xsl:if>  
      </xsl:for-each>

<!--
      <xsl:for-each select="Note[@kind='var-used'][@mode='write'][@scalar='yes']">
        <xsl:variable name="decl-id" select="@decl-id"/>
        <xsl:variable name="then" select="../Stmt[1]/Note[@kind='var-used'][@mode='write'][@decl-id=$decl-id]"/>
        <xsl:variable name="else" select="../Stmt[2]/Note[@kind='var-used'][@mode='write'][@decl-id=$decl-id]"/>
        <PHI>
          <xsl:choose>
            <xsl:when test="$then and $else">
              <port source="{$then/@true-source}" qualifier="then" dir="in"/>
              <port source="{$else/@true-source}" qualifier="else" dir="in"/>
            </xsl:when>
            <xsl:when test="$then">
              <port source="{$then/@true-source}" qualifier="then" dir="in"/>
              <port source="{concat($stmt-id,'$ELSE$',$decl-id)}" qualifier="else" dir="in"/>
            </xsl:when>
            <xsl:when test="$else">
              <port source="{concat($stmt-id,'$THEN$',$decl-id)}" qualifier="then" dir="in"/>
              <port source="{$else/@true-source}" qualifier="else" dir="in"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:message>
                Fatal inconsistency in If block notes
              </xsl:message>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:variable name="phi" select="concat($stmt-id,'$PHI$',$decl-id)"/>
          <port source="{$phi}" dir="out">
            <xsl:variable name="type-attrs">
              <xsl:apply-templates select="..//Note[@kind='varMod'][@decl-id=$decl-id][1]/Type"/>
            </xsl:variable>
            <xsl:for-each select="$type-attrs/attr">
              <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
            </xsl:for-each>
          </port>
        </PHI>
      </xsl:for-each>
-->
      
    </module>
  </xsl:template>
  
  <xsl:template match="Stmt[@kind='While']">
    <xsl:variable name="stmt-id" select="@id"/>
       
    <module kind="loop">
      <xsl:for-each select="Note[@kind='var-used'][@mode='write'][@scalar='yes']">
        <xsl:variable name="decl-id" select="@decl-id"/>
        <PHI>
          <port source="{../Note[@kind='var-used'][@mode='read'][@decl-id=$decl-id]/@true-source}" dir="in"/>
          <port source="{../Stmt/Note[@kind='var-used'][@mode='write'][@decl-id=$decl-id]/@true-source}" dir="in"/>
          <xsl:variable name="phi" select="concat($stmt-id,'$PHI$',$decl-id)"/>
          <port source="{$phi}" dir="out">
            <xsl:variable name="type-attrs">
              <xsl:apply-templates select="..//Note[@kind='varMod'][@decl-id=$decl-id][1]/Type"/>
            </xsl:variable>
            <xsl:for-each select="$type-attrs/attr">
              <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
            </xsl:for-each>
          </port>
        </PHI>
      </xsl:for-each>
      <module kind="test" decision="{Expr/@id}">
        <xsl:apply-templates select="Expr"/>
      </module>
      <module kind="body">
        <xsl:apply-templates select="Stmt"/>
      </module>
    </module>
    
  </xsl:template>
  
  <!-- When variables are modified in only one branch, we must create a
       noop in the other branch to provide an input to the PHI. This
       applies only to scalar writes. -->
  <xsl:template name="balance-if-writes">
    <xsl:param name="stmt"/>
    <xsl:param name="branch"/>
  
    <xsl:variable name="unbalanced-writes">
      <xsl:for-each select="$stmt/Note[@kind='var-used'][@mode='write']">
        <xsl:variable name="decl-id" select="@decl-id"/>
        <xsl:if test="@scalar='yes' or not(//Actor/Decl[@id=$decl-id])">          
          <xsl:choose>
            <xsl:when test="$branch = 'THEN'">
              <xsl:if test="not( $stmt/Stmt[1]/Note[@kind='var-used'][@mode='write'][@decl-id=$decl-id] )">
                <xsl:copy-of select="."/>
              </xsl:if>
            </xsl:when>
            <xsl:otherwise>
              <xsl:if test="not( $stmt/Stmt[2]/Note[@kind='var-used'][@mode='write'][@decl-id=$decl-id] )">
                <xsl:copy-of select="."/>
              </xsl:if>          
            </xsl:otherwise>      
          </xsl:choose>
        </xsl:if>  
      </xsl:for-each>      
    </xsl:variable>

    <xsl:for-each select="$unbalanced-writes/Note">

      <xsl:variable name="decl-id" select="@decl-id"/>
      <xsl:variable name="stmt-id" select="$stmt/@id"/>
      <xsl:variable name="type-attrs">
        <xsl:apply-templates select="$stmt//Note[@kind='varMod'][@decl-id=$decl-id][1]/Type"/>
      </xsl:variable>
      <operation kind="noop">
        <port dir='in' source="{$stmt/Note[@kind='var-used'][@mode='read'][@decl-id=$decl-id]/@true-source}"/>
        <port dir='out' source="{concat($stmt-id,'$',$branch,'$',$decl-id)}">
          <xsl:for-each select="$type-attrs/attr">
            <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
          </xsl:for-each>
        </port>
      </operation>
    </xsl:for-each>
  
  </xsl:template>
    
  <xsl:template match="Stmt[ @kind='Block' ]">
    <xsl:comment>Statement <xsl:value-of select="@id"/></xsl:comment>
    <xsl:apply-templates select="Decl"/>
    <xsl:apply-templates select="Stmt"/>
  </xsl:template>
    
  <xsl:template match="Stmt">
    <xsl:message>
      Unhandled Stmt kind="<xsl:value-of select="@kind"/>", id="<xsl:value-of select="@id"/>
    </xsl:message>
  </xsl:template>

  <xd:doc>
    <xd:short>Copy any Directive note with the name xlim_tag into the xlim</xd:short>
    <xd:detail>
      Any Directive with the name 'xlim_tag' will have its entire contents copied
      directly into the xlim at whatever point it is encountered.
    </xd:detail>
  </xd:doc>
  <xsl:template match="Note[@kind='Directive' and @name='xlim_tag']">
    <xsl:for-each select="./*">
      <xsl:apply-templates select="." mode="copyall"/>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template match="*" mode="copyall">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates mode="copyall"/>   
    </xsl:copy>
  </xsl:template>
  
  <!-- If a config_option does not have a value attribute, attempt to copy it from a child expr. -->
  <xsl:template match="config_option" mode="copyall">

    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:if test="not(@value)">
        <xsl:attribute name="value"><xsl:value-of select="./Expr[@kind='Literal']/@value"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates mode="copyall"/>   
    </xsl:copy>
    
  </xsl:template>
  
  <!-- Default is to ignore the element -->
  <xsl:template match="*"/>

  <xsl:template name="scheduler">
    <xsl:param name="actor"/>

    <!-- Allocate an FSM state variable -->
    <xsl:variable name="nstates" select="count($actor/Note[@kind='state-enum'])"/>
    
    <xsl:variable name="stateEnumerations">
       <xsl:for-each select="$actor/Note[@kind='state-enum']">
            <operation kind="$literal_Integer" value="{position()}">
              <port source="{concat($actor/@id,'$fsm$',@name)}" dir="out" typeName="int" size="32"/>
           </operation>
       </xsl:for-each>    	
    </xsl:variable>
     
    <!-- Starting state is the first one in the enumeration -->
    <stateVar name="currentState">
      <initValue typeName="int" size="32" value="1"/>
    </stateVar>

    <!-- Create a list of states and the actions fireable in priority order -->
    <xsl:variable name="state-actions">
      <!-- First do stateless actions -->
      <state>
        <xsl:for-each select="$actor/Note[@kind='MergedPriorityInequalities']/Note">
          <xsl:variable name="action-id" select="@id"/>
          <xsl:variable name="action-name">
            <xsl:call-template name="generateActionName">
              <xsl:with-param name="action" select="$actor/Action[@id = $action-id]"/>
            </xsl:call-template>
          </xsl:variable>
          <xsl:choose>
            <!-- If there are no states, then all actions are stateless. Duh. -->
            <xsl:when test="$nstates = 0">
              <action id="{$action-id}" name="{$action-name}"/>
            </xsl:when>
          
            <!-- Only unnamed actions are stateless -->
            <xsl:otherwise>
              <xsl:if test="$actor/Action[@id=$action-id][not( QID )]">
                <action id="{$action-id}" name="{$action-name}"/>
              </xsl:if>
            </xsl:otherwise>            
          </xsl:choose>
        </xsl:for-each>
      </state>

      <!-- Now do the states and their actions -->    
      <xsl:for-each select="$actor/Note[@kind='state-enum']">
        <xsl:variable name="state-name" select="@name"/>
        <state name="{@name}" index="{@index}">
          <xsl:for-each select="../Note[ @kind='MergedPriorityInequalities' ]/Note">
            <xsl:variable name="action-id" select="@id"/>
            <xsl:for-each select="$actor/Schedule/Transition[@from=$state-name]">
              <xsl:if test="ActionTags/QID/Note[@kind='ActionId']/@id = $action-id">
                <xsl:variable name="action-name">
                  <xsl:call-template name="generateActionName">
                    <xsl:with-param name="action" select="$actor/Action[@id = $action-id]"/>
                  </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="to" select="@to"/>
                <action id="{$action-id}" name="{$action-name}" to-name="{$to}" to-index="{../../Note[@kind='state-enum'][@name=$to]/@index}"/>
              </xsl:if>
            </xsl:for-each>
          </xsl:for-each>
        </state>
      </xsl:for-each>
    </xsl:variable>
                     
    <module kind="action-scheduler" name="action-scheduler" sourceName="action-scheduler" autostart="true">
      
      <xsl:variable name="true" select="concat($actor/@id,'$sched$true')"/>              
      <operation kind="$literal_Integer" value="1">
        <port source="{$true}" dir="out" size="1" typeName="bool"/>
      </operation>
      
      <xsl:for-each select="$stateEnumerations/operation">
        <xsl:copy-of select="."/>   
      </xsl:for-each>
           
      <module kind="loop">
        <xsl:variable name="loop-test" select="concat($true,'$loop')"/>
        <module kind="test" decision="{$loop-test}">
          <operation kind="noop">
            <port source="{$true}" dir="in"/>
            <port source="{$loop-test}" dir="out" typeName="bool" size="1"/>
          </operation>
        </module>
        
        <module kind="body">
          <xsl:for-each select="$actor/Port[@kind='Input']">
            <operation kind="pinAvail" portName="{@name}"> 
              <port dir="out" source="{concat(@name, '$pinAvail')}" typeName="int" size="32"/> 
            </operation>
          </xsl:for-each>
          <!-- end of pinAvail hack --> 
          
          <!-- Evaluate guards -->
          <xsl:apply-templates select="$actor/Action" mode="guard"/>                    

          <!-- Test fireability of each action based on output queue status -->
          <xsl:for-each select="$actor/Port[@kind='Output']">
            <xsl:variable name="status" select="concat(@id,'$status')"/>
            <xsl:variable name="name" select="@name"/>
                <operation kind="pinAvail" portName="{@name}">
                  <port source="{$status}" dir="out" size="32" typeName="int" />
                </operation>
          </xsl:for-each>
          
          <xsl:for-each select="$actor/Action">
            <xsl:variable name="actionId" select="@id"/>
            <xsl:variable name="fireable" select="concat(@id,'$fireable')"/>

            <xsl:for-each select="Output">
              <xsl:variable name="port" select="@port"/>
                <xsl:choose>
                  <xsl:when test="Note[@kind='Repeat-applied']">
                    <operation kind="$literal_Integer" value="{count(Expr) * Note[@kind='Repeat-applied']/@value}">
                      <port source="{concat($actionId, '$', $port, '$exprCount')}" dir="out" typeName="int" size="32"/>
                    </operation>                                                      
                  </xsl:when>
                  <xsl:otherwise>
                    <operation kind="$literal_Integer" value="{count(Expr)}">
                      <port source="{concat($actionId, '$', $port, '$exprCount')}" dir="out" typeName="int" size="32"/>
                    </operation>                                                      
                  </xsl:otherwise>              
                </xsl:choose>
                            
                <operation kind="$ge">                       
                  <port source="{concat(../../Port[@name=$port]/@id,'$status')}" dir="in"/>
                  <port source="{concat($actionId, '$', $port, '$exprCount')}" dir="in"/>
                  <port source="{concat($actionId, '$', $port)}" dir="out" typeName="bool" size="1"/>
                </operation>   
            </xsl:for-each>

            <operation kind="$literal_Integer" value="1">
              <port source="{concat($actionId, '$true')}" dir="out" typeName="bool" size="1"/>
            </operation>                                      
            <operation kind="$and">
               <port source="{concat($actionId, '$true')}" dir="in"/>
               <xsl:for-each select="Output">
                  <xsl:variable name="port" select="@port"/>
                  <port source="{concat($actionId, '$', $port)}" dir="in"/>
                </xsl:for-each>
               <port source="{$fireable}" dir="out" size="1" typeName="bool"/>
            </operation>
          </xsl:for-each>  
          
          <!-- Implement action firings -->
          <xsl:call-template name="choose-action">
            <!-- First do the stateless actions -->
            <xsl:with-param name="actions"><xsl:copy-of select="$state-actions/state[1]/action"/></xsl:with-param>
            <xsl:with-param name="actor-id" select="$actor/@id"/>
            <xsl:with-param name="state-name" select="'always'"/>
            <xsl:with-param name="other-stuff">
              <!-- Read all the state variables before any firing/update 
              <xsl:for-each select="$actor/Note[@kind='state-enum']">
                <operation kind="noop">
                  <port source="{concat($actor/@id,'$fsm$',@name)}" dir="in"/>
                  <port source="{concat($actor/@id,'$fsm$copy$',@name)}" dir="out" typeName="bool" size="1"/>
                </operation>
              </xsl:for-each>
              The stateful actions will all fall in the last else module of the stateless ones -->
             <xsl:variable name="states" select="$state-actions/state[@name]"/>
              <xsl:if test="count($states) > 0">
                <xsl:call-template name="IfThenElse">
                  <xsl:with-param name="states" select="$state-actions/state[@name]"/>
                  <xsl:with-param name="actor" select="$actor"/> 
                </xsl:call-template>
              </xsl:if>    
            </xsl:with-param>
          </xsl:call-template>
        </module>
      </module>
    </module>

  </xsl:template>

  <xsl:template name="IfThenElse">
    <xsl:param name="states"/>
    <xsl:param name="actor"/>    
    <module kind="if">        
      <module kind="test" decision="{concat($states[1]/@name,'$enabled')}">
        <operation kind="$eq">
          <port source="{concat($actor/@id,'$fsm$',$states[1]/@name)}" dir="in"/>
          <port source="currentState" dir="in"/>         
          <port source="{concat($states[1]/@name,'$enabled')}" dir="out" typeName="bool" size="1"/>
        </operation>
      </module>
      <module kind="then">
        <xsl:call-template name="choose-action">
          <xsl:with-param name="actions"><xsl:copy-of select="$states[1]/action"/></xsl:with-param>
          <xsl:with-param name="actor-id" select="$actor/@id"/>
          <xsl:with-param name="state-name" select="$states[1]/@name"/>
        </xsl:call-template>
      </module> 
      <xsl:choose>
        <!-- recursivity condition -->
        <xsl:when test="count($states) > 1">
        <module kind="else">
          <xsl:call-template name="IfThenElse">
            <xsl:with-param name="states" select="$states[position() &gt; 1]" />
            <xsl:with-param name="actor" select="$actor"/> 
          </xsl:call-template>
        </module>
        </xsl:when>
        <!-- stop recursivity condition -->
        <xsl:otherwise>
          <!-- Do nothing -->
        </xsl:otherwise>
      </xsl:choose>    
    </module>
  </xsl:template>
  
  
  <xsl:template match="Action" mode="guard">
 
    <!-- make inputs, local variables available -->
    <xsl:comment>Input peeks for action <xsl:value-of select="@id"/></xsl:comment>
    <xsl:apply-templates select="Input" mode="peek"/>
    <xsl:comment>Local variables used in guards of action <xsl:value-of select="@id"/></xsl:comment>
    <xsl:apply-templates select="Decl[@kind='Variable'][ Note[@kind='used-in-guard'] ]"/>
    <xsl:comment>Guard for action <xsl:value-of select="@id"/></xsl:comment>
    <xsl:apply-templates select="Guards/Expr"/>
       
    <xsl:variable name="guard-result" select="concat(@id,'$guard')"/>
    <xsl:choose>
      <xsl:when test="count(Input | Guards/Expr) = 0">
       <operation kind="$literal_Integer" value="1">
         <port source="{$guard-result}" dir="out" typeName="bool" size="1"/>
        </operation>
      </xsl:when>
      <xsl:otherwise>
        <operation kind="$and">
          <xsl:for-each select="Input"> 
            <port source="{concat(@id,'$ready')}" dir="in"/>
          </xsl:for-each>
          <xsl:for-each select="Guards/Expr">
            <port source="{@id}" dir="in"/>
          </xsl:for-each>
          <port source="{$guard-result}" dir="out" typeName="bool" size="1"/>
        </operation>          
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>

  <xsl:template match="Expr[ @kind='Literal' ]" mode="init-var">
     <xsl:variable name="type-attrs">
        <xsl:apply-templates select="ancestor::Decl/Type" mode="init-var"/>
    </xsl:variable>
    <initValue value="{@value}">
      <xsl:for-each select="$type-attrs/attr">
        <xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
      </xsl:for-each>              
    </initValue>
  </xsl:template>

  <xsl:template match="*" mode="init-var">
    <initValue typeName="{@kind}">
      <xsl:apply-templates select="Expr" mode="init-var"/>            
    </initValue>
  </xsl:template>

  <xsl:template name="choose-action">
    <xsl:param name="actions"/>
    <xsl:param name="actor-id"/>
    <xsl:param name="state-name"/>
    <xsl:param name="other-stuff" select="_default_to_empty_list_"/>

    <xsl:choose>
      <xsl:when test="$actions/action">
        <xsl:variable name="action-id" select="$actions/action[1]/@id"/>
        <xsl:variable name="action-name" select="$actions/action[1]/@name"/>
        <module kind="if">
          <xsl:variable name="guard-dec" select="concat($state-name,'$state$',$action-id,'$guard$read')"/>
          <module kind="test" decision="{$guard-dec}">
            <operation kind="noop">
              <port source="{concat($action-id,'$guard')}" dir="in"/>
              <port source="{$guard-dec}" dir="out" typeName="bool" size="1"/>
            </operation>
          </module>
          <module kind="then">
            <module kind="if">
              <xsl:variable name="fireable" select="concat($state-name,'$state$',$action-id,'$fireable')"/>
              <module kind="test" decision="{$fireable}">
                <operation kind="noop">
                  <port source="{concat($action-id,'$fireable')}" dir="in"/>
                  <port source="{$fireable}" dir="out" typeName="bool" size="1"/>
                </operation>
              </module>
              <module kind="then">
                <operation kind="taskCall" target="{$action-name}"/>
                <xsl:if test="$actions/action[1]/@to-name">
                  <operation kind="assign" target="currentState">
                    <port source="{concat($actor-id,'$fsm$',$actions/action[1]/@to-name)}" dir="in"/>
                  </operation>
                </xsl:if>
              </module>
            </module>
          </module>
          <xsl:if test="$actions/action[position() > 1] or $other-stuff/*">
            <module kind="else">
              <xsl:call-template name="choose-action">
                <xsl:with-param name="actions"><xsl:copy-of select="$actions/action[position()>1]"/></xsl:with-param>
                <xsl:with-param name="actor-id" select="$actor-id"/>
                <xsl:with-param name="state-name" select="$state-name"/>
                <xsl:with-param name="other-stuff">
                  <xsl:copy-of select="$other-stuff/*"/>
                </xsl:with-param>
              </xsl:call-template>
            </module>
          </xsl:if>
        </module>
      </xsl:when>
      
      <xsl:otherwise>
        <xsl:copy-of select="$other-stuff/*"/>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xd:doc>
    <xd:short>Annotate Actions with name attribute derived from QID or generated if no QID available</xd:short>
    <xd:detail>
      Action names are annotated as attributes as derived from the QID or as generated
      from the start line number attribute if there is no QID matched to the action.
      <b>Note:</b> This template has been largely (entirely?) obviated by the same functionality in CopyQIDToAction.xslt
    </xd:detail>
  </xd:doc>
  <xsl:template name="generateActionName">
    <xsl:param name="action"/>
    <xsl:choose>
      <xsl:when test="$action/@name"><xsl:value-of select="$action/@name"/></xsl:when>
      <xsl:when test="$action/QID/Note/@kind='ActionId'"><xsl:value-of select="$action/QID/@name"/></xsl:when>
      <xsl:otherwise><xsl:value-of select="concat('actionAtLine_',$action/@text-begin-line)"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- Generate the indexer implied by the Args/Expr in the current context.
       Can be applied to a Stmt or Expr[@kind='Indexer'] as long as they
       have Args and Note[@kind='dimension']. The size of the underlying
       scalar (in bits) must be provided. -->
  <xsl:template match="*" mode="generate-indexers">
        
    <xsl:variable name="address-bits" select="32"/>
    <xsl:variable name="last" select="concat(Args/Expr[last()]/@id,'$ADDR')"/>

    <!-- Construct the component expressions -->
    <xsl:apply-templates select="Args/Expr"/>
    <xsl:for-each select="Args/Expr">
      <xsl:variable name="pos" select="position()"/>
      <xsl:variable name="const" select="../../Note[@kind='dimension'][@index=$pos][1]/@multiplier"/>
      <xsl:variable name="const-name" select="concat(@id,'$CONST')"/>
      <xsl:variable name="mul-name" select="concat(@id,'$MUL')"/>
      <xsl:variable name="this" select="concat(@id,'$ADDR')"/>
      <xsl:comment>Address multiplier for index <xsl:value-of select="$pos"/></xsl:comment>
        <operation kind="$literal_Integer" value="{$const}">
        <port source="{$const-name}" dir="out" typeName="int"
          size="{$address-bits}"/>
      </operation>    
      
      <operation kind="$mul">
        <port source="{$const-name}" dir="in"/>
        <port source="{@id}" dir="in"/>
        <port source="{$mul-name}" dir="out" typeName="int" size="{$address-bits}"/>
      </operation>
      <xsl:choose>
        <xsl:when test="$pos = 1">
          <operation kind="noop">
            <port source="{$mul-name}" dir="in"/>
            <port source="{$this}" dir="out" typeName="int" size="{$address-bits}"/>
          </operation>
        </xsl:when>
        <xsl:otherwise>
          <operation kind="$add">
            <port source="{$mul-name}" dir="in"/>
            <port source="{concat(../Expr[$pos - 1]/@id,'$ADDR')}" dir="in"/>
            <port source="{$this}" dir="out" typeName="int" size="{$address-bits}"/>
          </operation>            
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
    
  </xsl:template> 

</xsl:stylesheet>
