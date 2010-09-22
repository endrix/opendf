<xsl:stylesheet 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   extension-element-prefixes="xsl"
   version="2.0">
   <xsl:output method="xml"/>

   <!--
     Transform foreah statements with constant (i.e. compile-time) lists
     to while loop
     
     original code:
     
     	foreach T i in E do
     		stmts
     	end
     
     transformed code:
     
    	 block begin
     		int $indexer = 0;
     		int $iterations = sizeof(E);
     		List[T] $data = E;
     		while $indexer < $iterations do
     	   		i = $data[$indexer]
     	   		$indexer := $indexer + 1;
     	   	stmts     	   
     		end     
     	block end     
   --> 


    <xsl:template match="Stmt[@kind='Foreach']">
      <Stmt kind="Block">
          <Decl kind="Variable" name="{Generator/Decl[@kind='Generator']/@name}">
              <xsl:copy-of select="Generator/Type"/>              
          </Decl>    
          <Decl kind="Variable" name="$indexer">
              <Type name="int">
                  <Entry kind="Expr" name="size">
                      <Expr kind="Literal" literal-kind="Integer" value="32"/>
                  </Entry>
              </Type>
              <Expr kind="Literal" literal-kind="Integer" value="0"/>
          </Decl>
          <Decl kind="Variable" name="$data">
              <xsl:copy-of select="Generator/Expr/Note[@kind='exprType']/Type"/>
              <xsl:copy-of select="Generator/Expr"/>
          </Decl>       
          <Decl kind="Variable" name="$iterations">
              <Type name="int">
                  <Entry kind="Expr" name="size">
                      <Expr kind="Literal" literal-kind="Integer" value="32"/>
                  </Entry>
              </Type>
              <Expr kind="Literal" literal-kind="Integer" value="{count(Generator/Expr/Expr)}"/>
          </Decl>
                    
          <Stmt kind="Assign" name="{Generator/Decl/@name}" >
              <Expr kind="Indexer">
                  <Expr kind="Var" name="$data" />                
                  <Args>
                      <Expr kind="Var" name="$indexer"/>            
                  </Args>                
              </Expr>        
          </Stmt>    
          <Stmt kind="While">    
             <Expr kind="Let">
             	 <Expr kind="Application">
                    <Expr kind="Var" name="$lt" old="no"/>
                     <Args>
                        <Expr kind="Var" name="$indexer"/>
                        <Expr kind="Var" name="$iterations"/>
                     </Args>
                   </Expr>     
             </Expr>         
             <Stmt kind="Block"> 
                <xsl:apply-templates select="Stmt/Stmt"/>
                <Stmt kind="Assign" name="{Generator/Decl[@kind='Generator']/@name}">
                     <Expr kind="Let">
                         <Expr kind="Indexer">
                             <Expr kind="Var" name="$data"/>
                             <Args>
                                 <Expr kind="Var" name="$indexer"/>
                             </Args>
                         </Expr>
                     </Expr>
                 </Stmt>
                 <Stmt kind="Assign" name="$indexer">
                     <Expr kind="Let">
                         <Expr kind="Application">
                             <Expr kind="Var" name="$add" old="no"/>
                             <Args>
                                 <Expr kind="Var" name="$indexer"/>
                                 <Expr kind="Literal" literal-kind="Integer" value="1"/>
                             </Args>
                         </Expr>
                     </Expr>
                 </Stmt>                 
              </Stmt>
          </Stmt> 
      </Stmt>    
          
    </xsl:template>
  
    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>


</xsl:stylesheet>


