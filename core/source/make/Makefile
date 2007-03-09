# This makefile is generally called by the one in the parent directory.  These vars
# will be set by the parent makefile.
# Linux environment
JAVAC = javac
JAR = jar
CPS = :
# Windows environment
#JAVAC = /cygdrive/c/Program\ Files/Java/jdk1.5.0_06/bin/javac
#JAVAC = /cygdrive/c/Program\ Files/Java/jdk1.5.0_06/bin/jar
#CPS = ;

CALTROP_HOME = ../..
JAR_NAME = caltrop.jar
CALTROP_CLASSDIR = $(CALTROP_HOME)/classes
JAVA_CUP_SOURCES = $(shell find $(CALTROP_HOME)/lib/source/java_cup -name "*.java")
PTOLEMY_SOURCES = $(shell find $(CALTROP_HOME)/lib/source/ptolemy -name "*.java")
CALTROP_UTIL_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/util -name "*.java")
CALTROP_CAL_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/cal -name "*.java")
CALTROP_NL_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/nl -name "*.java")
CALTROP_HADES_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/hades -name "*.java")
CALTROP_CLI_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/cli -name "*.java")

jar: $(CALTROP_CLASSDIR)
	cd $(CALTROP_CLASSDIR) ; jar -cvf ../$(JAR_NAME) *

clean:
	rm -rf $(CALTROP_CLASSDIR)
	rm -f $(CALTROP_HOME)/$(JAR_NAME)

# Dependencies
# cal: util
# nl: cal, util
# hades: cal, nl, util
# cli: hades, cal, nl, util

cproot = $(CALTROP_CLASSDIR)/net/sf/caltrop

.PHONY = create_dir

$(CALTROP_CLASSDIR): create_dir $(cproot)/util $(cproot)/cal $(cproot)/nl $(cproot)/hades $(cproot)/cli $(CALTROP_CLASSDIR)/ptolemy

create_dir:
	mkdir -p $(CALTROP_CLASSDIR)

$(CALTROP_CLASSDIR)/java_cup:
	$(JAVAC) -d $(CALTROP_CLASSDIR) $(JAVA_CUP_SOURCES)

$(CALTROP_CLASSDIR)/ptolemy:
	$(JAVAC) -d $(CALTROP_CLASSDIR) $(PTOLEMY_SOURCES)

$(cproot)/util:
	$(JAVAC) -d $(CALTROP_CLASSDIR) $(CALTROP_UTIL_SOURCES)

$(cproot)/cal: $(cproot)/util $(CALTROP_CLASSDIR)/java_cup
	$(JAVAC) -classpath $(CALTROP_CLASSDIR) -d $(CALTROP_CLASSDIR) $(CALTROP_CAL_SOURCES)

$(cproot)/nl: $(cproot)/util $(cproot)/cal $(CALTROP_CLASSDIR)/java_cup
	$(JAVAC) -classpath $(CALTROP_CLASSDIR) -d $(CALTROP_CLASSDIR) $(CALTROP_NL_SOURCES)

$(cproot)/hades: $(cproot)/util $(cproot)/cal $(cproot)/nl
	$(JAVAC) -classpath $(CALTROP_CLASSDIR) -d $(CALTROP_CLASSDIR) $(CALTROP_HADES_SOURCES)

$(cproot)/cli: $(cproot)/util $(cproot)/cal $(cproot)/nl $(cproot)/hades $(CALTROP_CLASSDIR)/java_cup
	$(JAVAC) -classpath $(CALTROP_CLASSDIR)$(CPS)$(CALTROP_HOME)/lib/saxon8.jar -d $(CALTROP_CLASSDIR) $(CALTROP_CLI_SOURCES)

