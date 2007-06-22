<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cal="java:net.sf.caltrop.xslt.cal.CalmlEvaluator"
                xmlns:sch="http://www.ascc.net/xml/schematron"
                version="2.0">
    <xsl:output indent="yes" method="xml"/>
    <xsl:include href="net/sf/caltrop/cal/checks/reportOffenders.xslt"/>
    <xsl:template match="*|@*" mode="schematron-get-full-path">
        <xsl:apply-templates mode="schematron-get-full-path" select="parent::*"/>
        <xsl:text>/</xsl:text>
        <xsl:if test="count(. | ../@*) = count(../@*)">@</xsl:if>
        <xsl:value-of select="name()"/>
        <xsl:text>[</xsl:text>
        <xsl:value-of select="1+count(preceding-sibling::*[name()=name(current())])"/>
        <xsl:text>]</xsl:text>
    </xsl:template>
    <xsl:template match="/">
        <xsl:variable name="contents">
            <xsl:apply-templates mode="M1" select="/"/>
            <xsl:apply-templates mode="M2" select="/"/>
            <xsl:apply-templates mode="M3" select="/"/>
            <xsl:apply-templates mode="M4" select="/"/>
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
  
   
 
  
    
    
    
    <xsl:template match="Decl" mode="M1" priority="4000">
      
      
        <xsl:choose>
            <xsl:when test="not( some $decl in         ( parent::Generator/preceding-sibling::* |         parent::Input/preceding-sibling::* |         preceding-sibling::* )         /(self::Decl | self::Input/Decl | self::Generator/Decl)         satisfies $decl/@name = current()/@name )"/>
            <xsl:otherwise>
                <Note id="variableChecks.declaration.duplicate" kind="Report" severity="Error"
                      subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Duplicate declarations in the same scope</Note>
            </xsl:otherwise>
        </xsl:choose>
      
      
        <xsl:if test="some $decl in (          ancestor::*[ position() &gt; (if (current()/parent::Input or current()/parent::Generator) then 2 else 1) ] )         /(Decl | Input/Decl | Generator/Decl)         satisfies $decl/@name = current()/@name">
            <Note id="variableChecks.declaration.shadows" kind="Report" severity="Warning"
                  subject="">
                <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                    <apply-templates mode="report-offender" select="."/>
                </attribute>
                <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>A declaration shadows another declaration in a containing scope</Note>
        </xsl:if>
      
        <xsl:apply-templates mode="M1"/>
    </xsl:template>
    
    
    
    

    

    <xsl:template match="Stmt[@kind='Assign']" mode="M1" priority="3996">
      
        <variable xmlns="http://www.w3.org/1999/XSL/Transform" name="env">
            <Env xmlns="">
                <copy-of xmlns="http://www.w3.org/1999/XSL/Transform" select="ancestor::Actor[1]/Import"/>
            </Env>
        </variable>
        <xsl:choose>
            <xsl:when test="(some $decl in ancestor::*/(Decl | Input/Decl | Generator/Decl)         satisfies $decl/@name = current()/@name )         or cal:isDefined(@name, $env/Env) "/>
            <xsl:otherwise>
                <Note id="variableChecks.name.undefined" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Undefined variable reference</Note>
            </xsl:otherwise>
        </xsl:choose>
    
        <xsl:apply-templates mode="M1"/>
    </xsl:template>
    
    <xsl:template match="Expr[@kind='Var']" mode="M1" priority="3995">
      
        <variable xmlns="http://www.w3.org/1999/XSL/Transform" name="env">
            <Env xmlns="">
                <copy-of xmlns="http://www.w3.org/1999/XSL/Transform" select="ancestor::Actor[1]/Import"/>
            </Env>
        </variable>
        <xsl:choose>
            <xsl:when test="(some $decl in ancestor::*/(Decl | Input/Decl | Generator/Decl)         satisfies $decl/@name = current()/@name )         or cal:isDefined(@name, $env/Env) "/>
            <xsl:otherwise>
                <Note id="variableChecks.name.undefined" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Undefined variable reference</Note>
            </xsl:otherwise>
        </xsl:choose>
    
      
        <xsl:if test="some $decl in ancestor::Decl satisfies ( $decl/@name = current/@name         and ($decl/Type/@kind='Procedure' or $decl/Type/@kind='Function') )">
            <Note id="variableChecks.recursion" kind="Report" severity="Warning" subject="">
                <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                    <apply-templates mode="report-offender" select="."/>
                </attribute>
                <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Function or procedure recursion detected</Note>
        </xsl:if>
    
      
        <xsl:choose>
            <xsl:when test="not( some $decl in ancestor::Decl satisfies ( $decl/@name = current/@name         and not($decl/Type/@kind='Procedure') and not($decl/Type/@kind='Function') ) )"/>
            <xsl:otherwise>
                <Note id="variableChecks.selfReference" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Self reference in a variable declaration</Note>
            </xsl:otherwise>
        </xsl:choose>
    
        <xsl:apply-templates mode="M1"/>
    </xsl:template>
    
    <xsl:template match="Expr[@kind='BinOpSeq']" mode="M1" priority="3994">
        <xsl:choose>
            <xsl:when test="contains( ' and or = != &lt; &lt;= &gt; &gt;= in + - div mod * / ^ .. &gt;&gt; &lt;&lt; | &amp; ^ ',                 concat(' ',Op/@name,' ') )"/>
            <xsl:otherwise>
                <Note id="variableChecks.binaryOperator.undefined" kind="Report" severity="Error"
                      subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Undefined binary operator</Note>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates mode="M1"/>
    </xsl:template>
    
    <xsl:template match="Expr[@kind='UnaryOp']" mode="M1" priority="3993">
        <xsl:choose>
            <xsl:when test="contains( ' not # dom rng - ~ ', concat(' ',Op/@name,' ') )"/>
            <xsl:otherwise>
                <Note id="variableChecks.unaryOperator.undefined" kind="Report" severity="Error"
                      subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Undefined unary operator</Note>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates mode="M1"/>
    </xsl:template>
    
    <xsl:template match="text()" mode="M1" priority="-1"/>
  
  
     
    <xsl:template match="Input" mode="M2" priority="4000">
      
        <xsl:choose>
            <xsl:when test="some $port in ../../Port satisfies $port/@name = current()/@port"/>
            <xsl:otherwise>
                <Note id="portChecks.portRead.undefined" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Reference to non-existent input port</Note>
            </xsl:otherwise>
        </xsl:choose>
      
        <xsl:choose>
            <xsl:when test="not( some $port in ../../Port[@kind='Output'] satisfies $port/@name = current()/@port )"/>
            <xsl:otherwise>
                <Note id="portChecks.portRead.output" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Attempt to read output port</Note>
            </xsl:otherwise>
        </xsl:choose>

        <xsl:choose>
            <xsl:when test="not( following-sibling::Input[@port = current()/@port ] )"/>
            <xsl:otherwise>
                <Note id="portChecks.portRead.multiple" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Multiple reads from same input port - use multi-token read syntax instead</Note>
            </xsl:otherwise>
        </xsl:choose>
      
        <xsl:apply-templates mode="M2"/>
    </xsl:template>
    
    <xsl:template match="Output" mode="M2" priority="3999">
      
        <xsl:choose>
            <xsl:when test="some $port in ../../Port satisfies $port/@name = current()/@port"/>
            <xsl:otherwise>
                <Note id="portChecks.portWrite.undefined" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Reference to non-existent output port</Note>
            </xsl:otherwise>
        </xsl:choose>
      
        <xsl:choose>
            <xsl:when test="not( some $port in ../../Port[@kind='Input'] satisfies $port/@name = current()/@port )"/>
            <xsl:otherwise>
                <Note id="portChecks.portWrite.input" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Attempt to write to input port</Note>
            </xsl:otherwise>
        </xsl:choose>

        <xsl:choose>
            <xsl:when test="not( following-sibling::Output[@port = current()/@port ] )"/>
            <xsl:otherwise>
                <Note id="portChecks.portWrite.multiple" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Multiple writes to same output port - use multi-token write syntax instead</Note>
            </xsl:otherwise>
        </xsl:choose>
            
        <xsl:apply-templates mode="M2"/>
    </xsl:template>
        
    <xsl:template match="text()" mode="M2" priority="-1"/>

  

     <xsl:template match="QID[ parent::Priority ]" mode="M3" priority="4000">
      
       <xsl:choose>
            <xsl:when test="some $action in ../../Action satisfies $action/QID/@name = current()/@name           or starts-with( $action/QID/@name, concat(current()/@name, '.'))"/>
            <xsl:otherwise>
                <Note id="priorityChecks.priorityQID.undefined" kind="Report" severity="Error"
                      subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>QID in a priority relationship does not match any action</Note>
            </xsl:otherwise>
        </xsl:choose>

       <xsl:if test="for $high-QID in . return                  for $low-QID in $high-QID/following-sibling::QID[1] return                    some $high-action in $high-QID/../../Action[ QID/@name = $high-QID/@name                                          or starts-with( QID/@name, concat( $high-QID/@name, '.' ) ) ] satisfies                      some $low-action in $low-QID/../../Action[ QID/@name = $low-QID/@name                                           or starts-with( QID/@name, concat( $low-QID/@name, '.' ) ) ] satisfies                        some $high-port in $high-action/Input/@port satisfies                          not( $low-action/Input[ @port = $high-port ] ) ">
            <Note id="priorityChecks.priorityQID.timingDependent" kind="Report"
                  severity="Warning"
                  subject="">
                <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                    <text>higher-</text>
                    <apply-templates mode="report-offender" select="."/>
                    <text>, lower-</text>
                    <apply-templates mode="report-offender" select="./following-sibling::QID[1]"/>
                </attribute>
                <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Priority relationship may introduce timing-dependent behavior because an action reads input port(s) not read by all lower priority action(s).</Note>
        </xsl:if>

       
        <xsl:apply-templates mode="M3"/>
    </xsl:template>
    
    <xsl:template match="text()" mode="M3" priority="-1"/>
  
  

    <xsl:template match="Schedule" mode="M4" priority="4000">
      
        <xsl:if test="@kind != 'fsm'">
            <Note id="FSMChecks.schedule.noFSM" kind="Report" severity="Warning" subject="">
                <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                    <apply-templates mode="report-offender" select="."/>
                </attribute>
                <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Only FSM schedules are fully supported</Note>
        </xsl:if>

        <xsl:if test="preceding-sibling::Schedule">
            <Note id="FSMChecks.schedule.multiple" kind="Report" severity="Warning" subject="">
                <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                    <apply-templates mode="report-offender" select="."/>
                </attribute>
                <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Multiple schedules are declared</Note>
        </xsl:if>

        <xsl:choose>
            <xsl:when test="Transition[ @from = current()/@initial-state ]"/>
            <xsl:otherwise>
                <Note id="FSMChecks.schedule.undefinedStart" kind="Report" severity="Error"
                      subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Starting state '<value-of xmlns="http://www.w3.org/1999/XSL/Transform" select="current()/@initial-state"/>' has no exit transitions</Note>
            </xsl:otherwise>
        </xsl:choose>
      
        <xsl:if test="../Action[ not( QID ) ]">
            <Note id="FSMChecks.schedule.freeRunning" kind="Report" severity="Warning" subject="">
                <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                    <apply-templates mode="report-offender" select="."/>
                </attribute>
                <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Unnamed actions will run independently of the declared fsm schedule</Note>
        </xsl:if>
      
        <xsl:apply-templates mode="M4"/>
    </xsl:template>
    
    <xsl:template match="Transition" mode="M4" priority="3999">
        <xsl:choose>
            <xsl:when test="for $this in . return                  some $arc in $this/../Transition satisfies $arc/@from = $this/@to"/>
            <xsl:otherwise>
                <Note id="FSMChecks.states.deadEnd" kind="Report" severity="Warning" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select=".."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Detected transition from '<value-of xmlns="http://www.w3.org/1999/XSL/Transform" select="@from"/>' to dead-end state '<value-of xmlns="http://www.w3.org/1999/XSL/Transform" select="@to"/>'</Note>
            </xsl:otherwise>
        </xsl:choose>
      
        <xsl:choose>
            <xsl:when test="for $this in . return                 some $arc in $this/../Transition satisfies $arc/@to = $this/@from"/>
            <xsl:otherwise>
                <Note id="FSMChecks.states.unreachable" kind="Report" severity="Warning" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select=".."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>Detected unreachable state '<value-of xmlns="http://www.w3.org/1999/XSL/Transform" select="@from"/>'</Note>
            </xsl:otherwise>
        </xsl:choose>  
        <xsl:apply-templates mode="M4"/>
    </xsl:template>
    
    <xsl:template match="QID[ parent::ActionTags ]" mode="M4" priority="3998">
        <xsl:choose>
            <xsl:when test="for $this in . return some $action in ../../../../Action satisfies          $action/QID/@name = $this/@name or starts-with( $action/QID/@name, concat( $this/@name, '.' ) )"/>
            <xsl:otherwise>
                <Note id="FSMChecks.actions.undefined" kind="Report" severity="Error" subject="">
                    <attribute xmlns="http://www.w3.org/1999/XSL/Transform" name="subject">
                        <apply-templates mode="report-offender" select="../../.."/>
                    </attribute>
                    <apply-templates xmlns="http://www.w3.org/1999/XSL/Transform" mode="annotate-location" select="."/>No actions match QID '<value-of xmlns="http://www.w3.org/1999/XSL/Transform" select="@name"/>' in transition from '<value-of xmlns="http://www.w3.org/1999/XSL/Transform" select="../../@from"/>' to '<value-of xmlns="http://www.w3.org/1999/XSL/Transform" select="../../@to"/>'</Note>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates mode="M4"/>
    </xsl:template>
            
    <xsl:template match="text()" mode="M4" priority="-1"/>
  
    <xsl:template match="text()" priority="-1"/>
</xsl:stylesheet>