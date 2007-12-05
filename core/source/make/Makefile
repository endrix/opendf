# This makefile is generally called by the one in the parent directory. 
# These vars will be set by the parent makefile.

JAVAC = javac
JAR = jar

# OS-dependent classpath separator
ifeq "$(OS)" "Windows_NT"
CPS = ;
else
CPS = :
endif

OPENDF_HOME = ../..
JAR_NAME = opendf.jar
JFREECHART_CP = $(OPENDF_HOME)/../../jfreechart-1.0.6/jfreechart-1.0.6.jar$(CPS)$(OPENDF_HOME)/../../jfreechart-1.0.6/lib/jcommon-1.0.10.jar
OPENDF_CLASSDIR = $(OPENDF_HOME)/classes
OPENDF_CLASSPATH = $(OPENDF_CLASSDIR)$(CPS)$(OPENDF_HOME)/lib/saxon8.jar$(CPS)$(OPENDF_HOME)/lib/saxon8-dom.jar$(CPS)$(CONTRIB_DIR)/lib/PtPlot.jar
JAVA_CUP_SOURCES = $(shell find $(OPENDF_HOME)/lib/source/java_cup -name "*.java")
PTOLEMY_SOURCES = $(shell find $(OPENDF_HOME)/lib/source/ptolemy -name "*.java")
OPENDF_LIB_SOURCES =  $(shell find $(OPENDF_HOME)/source/java/net/sf/caltrop/lib -name "*.java")
OPENDF_ACTORC_SOURCES = $(shell find $(OPENDF_HOME)/source/java/net/sf/caltrop/actorc -name "*.java")
OPENDF_UTIL_SOURCES = $(shell find $(OPENDF_HOME)/source/java/net/sf/caltrop/util -name "*.java")
OPENDF_CAL_SOURCES = $(shell find $(OPENDF_HOME)/source/java/net/sf/caltrop/cal -name "*.java")
OPENDF_NL_SOURCES = $(shell find $(OPENDF_HOME)/source/java/net/sf/caltrop/nl -name "*.java")
OPENDF_HADES_SOURCES = $(shell find $(OPENDF_HOME)/source/java/net/sf/caltrop/hades -name "*.java")
OPENDF_CLI_SOURCES = $(shell find $(OPENDF_HOME)/source/java/net/sf/caltrop/cli -name "*.java")
OPENDF_XSLT_SOURCES = $(shell find $(OPENDF_HOME)/source/java/net/sf/caltrop/xslt -name "*.java")

CONTRIB_DIR = $(OPENDF_HOME)/../contrib/actorlib
CONTRIB_SOURCES = $(shell find $(CONTRIB_DIR) -name "*.java")

jar: $(OPENDF_CLASSDIR)
	cd $(OPENDF_CLASSDIR) ; $(JAR) -xvf ../../contrib/actorlib/lib/PtPlot.jar
	cd $(OPENDF_CLASSDIR) ; $(JAR) -cvf ../$(JAR_NAME) *

clean:
	rm -rf $(OPENDF_CLASSDIR)
	rm -f $(OPENDF_HOME)/$(JAR_NAME)

# Dependencies
# cal: util
# nl: cal, util
# hades: cal, nl, util
# cli: hades, cal, nl, util

cproot = $(OPENDF_CLASSDIR)/net/sf/caltrop

.PHONY = create_dir

$(OPENDF_CLASSDIR): create_dir util hades cli ptolemy xslt contrib


create_dir:
	mkdir -p $(OPENDF_CLASSDIR)

java_cup:
	$(JAVAC) -classpath "$(OPENDF_CLASSPATH)" -d $(OPENDF_CLASSDIR) $(JAVA_CUP_SOURCES)

ptolemy:
	$(JAVAC) -classpath "$(OPENDF_CLASSPATH)" -d $(OPENDF_CLASSDIR) $(PTOLEMY_SOURCES)

util: create_dir java_cup 
	$(JAVAC) -classpath "$(OPENDF_CLASSPATH)" -d $(OPENDF_CLASSDIR) $(OPENDF_LIB_SOURCES) $(OPENDF_UTIL_SOURCES) $(OPENDF_CAL_SOURCES) $(OPENDF_NL_SOURCES) $(OPENDF_ACTORC_SOURCES)
#	$(JAVAC) -classpath "$(OPENDF_CLASSPATH)" -d $(OPENDF_CLASSDIR) $(OPENDF_UTIL_SOURCES)

#cal: util java_cup
#	$(JAVAC) -classpath "$(OPENDF_CLASSPATH)" -d $(OPENDF_CLASSDIR) $(OPENDF_CAL_SOURCES)

#nl: util cal java_cup
#	$(JAVAC) -classpath "$(OPENDF_CLASSPATH)" -d $(OPENDF_CLASSDIR) $(OPENDF_NL_SOURCES)

hades: util
	$(JAVAC) -classpath "$(OPENDF_CLASSPATH)" -d $(OPENDF_CLASSDIR) $(OPENDF_HADES_SOURCES)

cli: util  hades java_cup xslt
	$(JAVAC) -classpath "$(OPENDF_CLASSPATH):$(JFREECHART_CP)" -d $(OPENDF_CLASSDIR) $(OPENDF_CLI_SOURCES)

xslt:
	$(JAVAC) -classpath "$(OPENDF_CLASSPATH)" -d $(OPENDF_CLASSDIR) $(OPENDF_XSLT_SOURCES)
	-cp -rp $(OPENDF_HOME)/source/xslt/* $(OPENDF_CLASSDIR)/

contrib:
	mkdir -p $(OPENDF_CLASSDIR)
	$(JAVAC) -d $(OPENDF_CLASSDIR) -classpath "$(OPENDF_CLASSPATH)" $(CONTRIB_SOURCES)
