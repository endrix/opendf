<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cal="java:net.sf.caltrop.xslt.cal.CalmlEvaluator"
                xmlns:sch="http://www.ascc.net/xml/schematron"
                version="2.0">
   <xsl:output method="xml" indent="yes"/>
   <xsl:include href="net/sf/caltrop/cal/checks/reportOffenders.xslt"/>
   <xsl:template match="*|@*" mode="schematron-get-full-path">
      <xsl:apply-templates select="parent::*" mode="schematron-get-full-path"/>
      <xsl:text>/</xsl:text>
      <xsl:if test="count(. | ../@*) = count(../@*)">@</xsl:if>
      <xsl:value-of select="name()"/>
      <xsl:text>[</xsl:text>
      <xsl:value-of select="1+count(preceding-sibling::*[name()=name(current())])"/>
      <xsl:text>]</xsl:text>
   </xsl:template>
   <xsl:template match="/">
      <xsl:variable name="contents">
         <xsl:apply-templates select="/" mode="M1"/>
         <xsl:apply-templates select="/" mode="M2"/>
         <xsl:apply-templates select="/" mode="M3"/>
         <xsl:apply-templates select="/" mode="M4"/>
      </xsl:variable>
      <xsl:for-each select="/*">
         <xsl:copy>
            <xsl:for-each select="@*">
               <xsl:attribute name="{name()}">
                  <xsl:value-of select="."/>
               </xsl:attribute>
            </xsl:for-each>
            <xsl:copy-of select="$contents"/>
            <xsl:copy-of select="*"/>
         </xsl:copy>
      </xsl:for-each>
   </xsl:template>
  
   
 
  
    
    
    
    <xsl:template match="Decl" priority="4000" mode="M1">
      
      
      <xsl:choose>
         <xsl:when test="not( some $decl in         ( parent::Generator/preceding-sibling::* |         parent::Input/preceding-sibling::* |         preceding-sibling::* )         /(self::Decl | self::Input/Decl | self::Generator/Decl)         satisfies $decl/@name = current()/@name )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.declaration.duplicate"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Duplicate declarations in the same scope</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      
      <xsl:if test="some $decl in (          ancestor::*[ position() &gt; (if (current()/parent::Input or current()/parent::Generator) then 2 else 1) ] )         /(Decl | Input/Decl | Generator/Decl)         satisfies $decl/@name = current()/@name">
         <Note kind="Report" severity="Warning" id="variableChecks.declaration.shadows"
               subject="">
            <xsl:attribute name="subject">
               <xsl:apply-templates select="." mode="report-offender"/>
            </xsl:attribute>A declaration shadows another declaration in a containing scope</Note>
      </xsl:if>
      
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
    
    
    

    

    <xsl:template match="Stmt[@kind='Assign']" priority="3996" mode="M1">
      
      <xsl:variable name="env">
        <Env>
            <xsl:copy-of select="ancestor::Actor[1]/Import"/>
        </Env>
      </xsl:variable>
      <xsl:choose>
         <xsl:when test="(some $decl in ancestor::*/(Decl | Input/Decl | Generator/Decl)         satisfies $decl/@name = current()/@name )         or cal:isDefined(@name, $env/Env) "/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.name.undefined" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Undefined variable reference</Note>
         </xsl:otherwise>
      </xsl:choose>
    
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
    <xsl:template match="Expr[@kind='Var']" priority="3995" mode="M1">
      
      <xsl:variable name="env">
        <Env>
            <xsl:copy-of select="ancestor::Actor[1]/Import"/>
        </Env>
      </xsl:variable>
      <xsl:choose>
         <xsl:when test="(some $decl in ancestor::*/(Decl | Input/Decl | Generator/Decl)         satisfies $decl/@name = current()/@name )         or cal:isDefined(@name, $env/Env) "/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.name.undefined" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Undefined variable reference</Note>
         </xsl:otherwise>
      </xsl:choose>
    
      
      <xsl:if test="some $decl in ancestor::Decl satisfies ( $decl/@name = current/@name         and ($decl/Type/@kind='Procedure' or $decl/Type/@kind='Function') )">
         <Note kind="Report" severity="Warning" id="variableChecks.recursion" subject="">
            <xsl:attribute name="subject">
               <xsl:apply-templates select="." mode="report-offender"/>
            </xsl:attribute>Function or procedure recursion detected</Note>
      </xsl:if>
    
      
      <xsl:choose>
         <xsl:when test="not( some $decl in ancestor::Decl satisfies ( $decl/@name = current/@name         and not($decl/Type/@kind='Procedure') and not($decl/Type/@kind='Function') ) )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.selfReference" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Self reference in a variable declaration</Note>
         </xsl:otherwise>
      </xsl:choose>
    
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
    <xsl:template match="Expr[@kind='BinOpSeq']" priority="3994" mode="M1">
      <xsl:choose>
         <xsl:when test="contains( ' and or = != &lt; &lt;= &gt; &gt;= in + - div mod * / ^ .. &gt;&gt; &lt;&lt; | &amp; ^ ',                 concat(' ',Op/@name,' ') )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.binaryOperator.undefined"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Undefined binary operator</Note>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
    <xsl:template match="Expr[@kind='UnaryOp']" priority="3993" mode="M1">
      <xsl:choose>
         <xsl:when test="contains( ' not # dom rng - ~ ', concat(' ',Op/@name,' ') )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="variableChecks.unaryOperator.undefined"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Undefined unary operator</Note>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates mode="M1"/>
   </xsl:template>
    
  <xsl:template match="text()" priority="-1" mode="M1"/>
  
  
     
    <xsl:template match="Input" priority="4000" mode="M2">
      
      <xsl:choose>
         <xsl:when test="some $port in ../../Port satisfies $port/@name = current()/@port"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="portChecks.portRead.undefined" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Reference to non-existent input port</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
         <xsl:when test="not( some $port in ../../Port[@kind='Output'] satisfies $port/@name = current()/@port )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="portChecks.portRead.output" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Attempt to read output port</Note>
         </xsl:otherwise>
      </xsl:choose>

      <xsl:choose>
         <xsl:when test="not( following-sibling::Input[@port = current()/@port ] )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="portChecks.portRead.multiple" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Multiple reads from same input port - use multi-token read syntax instead</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:apply-templates mode="M2"/>
   </xsl:template>
    
    <xsl:template match="Output" priority="3999" mode="M2">
      
      <xsl:choose>
         <xsl:when test="some $port in ../../Port satisfies $port/@name = current()/@port"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="portChecks.portWrite.undefined" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Reference to non-existent output port</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
         <xsl:when test="not( some $port in ../../Port[@kind='Input'] satisfies $port/@name = current()/@port )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="portChecks.portWrite.input" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Attempt to write to input port</Note>
         </xsl:otherwise>
      </xsl:choose>

      <xsl:choose>
         <xsl:when test="not( following-sibling::Output[@port = current()/@port ] )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="portChecks.portWrite.multiple" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Multiple writes to same output port - use multi-token write syntax instead</Note>
         </xsl:otherwise>
      </xsl:choose>
            
      <xsl:apply-templates mode="M2"/>
   </xsl:template>
        
  <xsl:template match="text()" priority="-1" mode="M2"/>

  

     <xsl:template match="QID[ parent::Priority ]" priority="4000" mode="M3">
      
       <xsl:choose>
         <xsl:when test="some $action in ../../Action satisfies $action/QID/@name = current()/@name           or starts-with( $action/QID/@name, concat(current()/@name, '.'))"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="priorityChecks.priorityQID.undefined"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>QID in a priority relationship does not match any action</Note>
         </xsl:otherwise>
      </xsl:choose>

       <xsl:if test="for $high-QID in . return                  for $low-QID in $high-QID/following-sibling::QID[1] return                    some $high-action in $high-QID/../../Action[ QID/@name = $high-QID/@name                                          or starts-with( QID/@name, concat( $high-QID/@name, '.' ) ) ] satisfies                      some $low-action in $low-QID/../../Action[ QID/@name = $low-QID/@name                                           or starts-with( QID/@name, concat( $low-QID/@name, '.' ) ) ] satisfies                        some $high-port in $high-action/Input/@port satisfies                          not( $low-action/Input[ @port = $high-port ] ) ">
         <Note kind="Report" severity="Warning"
               id="priorityChecks.priorityQID.timingDependent"
               subject="">
            <xsl:attribute name="subject">
               <xsl:text>higher-</xsl:text>
               <xsl:apply-templates select="." mode="report-offender"/>
               <xsl:text>, lower-</xsl:text>
               <xsl:apply-templates select="./following-sibling::QID[1]" mode="report-offender"/>
            </xsl:attribute>Priority relationship may introduce timing-dependent behavior because an action reads input port(s) not read by all lower priority action(s). (Reorder priorities, or ensure that the combination of state machine and guard expressions cannot produce this behavior - unless is is intended)</Note>
      </xsl:if>

       
      <xsl:apply-templates mode="M3"/>
   </xsl:template>
    
  <xsl:template match="text()" priority="-1" mode="M3"/>
  
  

    <xsl:template match="Schedule" priority="4000" mode="M4">
      
      <xsl:if test="@kind != 'fsm'">
         <Note kind="Report" severity="Warning" id="FSMChecks.schedule.noFSM" subject="">
            <xsl:attribute name="subject">
               <xsl:apply-templates select="." mode="report-offender"/>
            </xsl:attribute>Only FSM schedules are fully supported</Note>
      </xsl:if>

      <xsl:if test="preceding-sibling::Schedule">
         <Note kind="Report" severity="Warning" id="FSMChecks.schedule.multiple" subject="">
            <xsl:attribute name="subject">
               <xsl:apply-templates select="." mode="report-offender"/>
            </xsl:attribute>Multiple schedules are declared</Note>
      </xsl:if>

      <xsl:choose>
         <xsl:when test="Transition[ @from = current()/@initial-state ]"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="FSMChecks.schedule.undefinedStart"
                  subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="." mode="report-offender"/>
               </xsl:attribute>Starting state '<xsl:value-of select="current()/@initial-state"/>' has no exit transitions</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:if test="../Action[ not( QID ) ]">
         <Note kind="Report" severity="Warning" id="FSMChecks.schedule.freeRunning" subject="">
            <xsl:attribute name="subject">
               <xsl:apply-templates select="." mode="report-offender"/>
            </xsl:attribute>Unnamed actions will run independently of the declared fsm schedule</Note>
      </xsl:if>
      
      <xsl:apply-templates mode="M4"/>
   </xsl:template>
    
    <xsl:template match="Transition" priority="3999" mode="M4">
      <xsl:choose>
         <xsl:when test="for $this in . return                  some $arc in $this/../Transition satisfies $arc/@from = $this/@to"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Warning" id="FSMChecks.states.deadEnd" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select=".." mode="report-offender"/>
               </xsl:attribute>Detected transition from '<xsl:value-of select="@from"/>' to dead-end state '<xsl:value-of select="@to"/>'</Note>
         </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
         <xsl:when test="for $this in . return                 some $arc in $this/../Transition satisfies $arc/@to = $this/@from"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Warning" id="FSMChecks.states.unreachable" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select=".." mode="report-offender"/>
               </xsl:attribute>Detected unreachable state '<xsl:value-of select="@from"/>'</Note>
         </xsl:otherwise>
      </xsl:choose>  
      <xsl:apply-templates mode="M4"/>
   </xsl:template>
    
    <xsl:template match="QID[ parent::ActionTags ]" priority="3998" mode="M4">
      <xsl:choose>
         <xsl:when test="for $this in . return some $action in ../../../../Action satisfies          $action/QID/@name = $this/@name or starts-with( $action/QID/@name, concat( $this/@name, '.' ) )"/>
         <xsl:otherwise>
            <Note kind="Report" severity="Error" id="FSMChecks.actions.undefined" subject="">
               <xsl:attribute name="subject">
                  <xsl:apply-templates select="../../.." mode="report-offender"/>
               </xsl:attribute>No actions match QID '<xsl:value-of select="@name"/>' in transition from '<xsl:value-of select="../../@from"/>' to '<xsl:value-of select="../../@to"/>'</Note>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates mode="M4"/>
   </xsl:template>
            
  <xsl:template match="text()" priority="-1" mode="M4"/>
  
   <xsl:template match="text()" priority="-1"/>
</xsl:stylesheet>