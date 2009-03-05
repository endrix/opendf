#
# Build error checking transformations, apply tests
#

# OS-dependent setup
ifeq "$(OS)" "Windows_NT"
CPS = ;
else
CPS = :
endif

CORE = ../../../../../../..
CLASSPATH = $(CORE)/classes$(CPS)$(CORE)/lib/saxon8.jar$(CPS)$(CORE)/lib/saxon8-dom.jar$(CPS)$(CORE)/source/xslt$(CPS)$(CORE)/source/xslt

# Processing commands
JAVA = java
XSLT = $(JAVA) -classpath "$(CLASSPATH)" net.sf.caltrop.cal.main.ReadXMLWriteXML -r 
PARSECAL = $(JAVA) -classpath "$(CLASSPATH)" net.sf.caltrop.cal.main.Cal2CalML
PARSENL  = $(JAVA) -classpath "$(CLASSPATH)" net.sf.caltrop.nl.NL2XNL

CHECKS = semanticChecks.xslt

CALTESTS = tests/variableChecks.cal \
           tests/portChecks.cal \
           tests/priorityChecks.cal \
           tests/FSMChecks.cal

NLTESTS =  tests/variableChecks.nl

all: rules caltests nltests

rules: $(CHECKS)

caltests: rules $(CALTESTS:.cal=.calml)
	@for test in $(CALTESTS:.cal=.calml); do \
		for check in $(CHECKS); do \
			echo "**"; \
			echo "** Applying check $$check to test file $$test"; \
			echo "**"; \
			$(XSLT) $$test $$test.out.calml net/sf/caltrop/cal/checks/$$check net/sf/caltrop/cal/checks/problemSummary.xslt; \
		done; \
	done ;

nltests: rules $(NLTESTS:.nl=.xnl)
	@for test in $(NLTESTS:.nl=.xnl); do \
		for check in $(CHECKS); do \
			echo "**"; \
			echo "** Applying check $$check to test file $$test"; \
			echo "**"; \
			$(XSLT) $$test $$test.out.xnl $$check; \
		done; \
	done ;

%.xslt: %.xml schematron-xml.xsl reportOffenders.xslt
	$(XSLT) $*.xml $*.xslt net/sf/caltrop/cal/checks/schematron-xml.xsl

%.calml: %.cal
	$(PARSECAL) $<

%.xnl: %.nl
	$(PARSENL) $<
	