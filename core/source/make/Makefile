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

CALTROP_HOME = ../..
JAR_NAME = caltrop.jar
CALTROP_CLASSDIR = $(CALTROP_HOME)/classes
CALTROP_CLASSPATH = $(CALTROP_CLASSDIR)$(CPS)$(CALTROP_HOME)/lib/saxon8.jar$(CPS)$(CALTROP_HOME)/lib/saxon8-dom.jar$(CPS)$(CONTRIB_DIR)/lib/PtPlot.jar
JAVA_CUP_SOURCES = $(shell find $(CALTROP_HOME)/lib/source/java_cup -name "*.java")
PTOLEMY_SOURCES = $(shell find $(CALTROP_HOME)/lib/source/ptolemy -name "*.java")
CALTROP_UTIL_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/util -name "*.java")
CALTROP_CAL_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/cal -name "*.java")
CALTROP_NL_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/nl -name "*.java")
CALTROP_HADES_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/hades -name "*.java")
CALTROP_CLI_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/cli -name "*.java")
CALTROP_XSLT_SOURCES = $(shell find $(CALTROP_HOME)/source/java/net/sf/caltrop/xslt -name "*.java")

CALTROP_TRANSFORMS = $(shell find $(CALTROP_HOME)/source/xslt -name "*.xslt")

CONTRIB_DIR = $(CALTROP_HOME)/../contrib/actorlib
CONTRIB_SOURCES = $(shell find $(CONTRIB_DIR) -name "*.java")

jar: $(CALTROP_CLASSDIR)
	cd $(CALTROP_CLASSDIR) ; $(JAR) -xvf ../../contrib/actorlib/lib/PtPlot.jar
	cd $(CALTROP_CLASSDIR) ; $(JAR) -cvf ../$(JAR_NAME) *

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

$(CALTROP_CLASSDIR): create_dir util cal nl hades cli ptolemy xslt contrib


create_dir:
	mkdir -p $(CALTROP_CLASSDIR)

java_cup:
	$(JAVAC) -classpath "$(CALTROP_CLASSPATH)" -d $(CALTROP_CLASSDIR) $(JAVA_CUP_SOURCES)

ptolemy:
	$(JAVAC) -classpath "$(CALTROP_CLASSPATH)" -d $(CALTROP_CLASSDIR) $(PTOLEMY_SOURCES)

util:
	$(JAVAC) -classpath "$(CALTROP_CLASSPATH)" -d $(CALTROP_CLASSDIR) $(CALTROP_UTIL_SOURCES)

cal: util java_cup
	$(JAVAC) -classpath "$(CALTROP_CLASSPATH)" -d $(CALTROP_CLASSDIR) $(CALTROP_CAL_SOURCES)

nl: util cal java_cup
	$(JAVAC) -classpath "$(CALTROP_CLASSPATH)" -d $(CALTROP_CLASSDIR) $(CALTROP_NL_SOURCES)

hades: util cal nl
	$(JAVAC) -classpath "$(CALTROP_CLASSPATH)" -d $(CALTROP_CLASSDIR) $(CALTROP_HADES_SOURCES)

cli: util cal nl hades java_cup xslt
	$(JAVAC) -classpath "$(CALTROP_CLASSPATH)" -d $(CALTROP_CLASSDIR) $(CALTROP_CLI_SOURCES)

xslt:
	$(JAVAC) -classpath "$(CALTROP_CLASSPATH)" -d $(CALTROP_CLASSDIR) $(CALTROP_XSLT_SOURCES)
	@for transform in $(CALTROP_TRANSFORMS); do \
	  echo Copying $$transform; \
	  cp -rp $$transform $(CALTROP_CLASSDIR); \
	done;
		
#	cp -rp $(CALTROP_HOME)/source/xslt/* $(CALTROP_CLASSDIR)/

contrib:
	mkdir -p $(CALTROP_CLASSDIR)
	$(JAVAC) -d $(CALTROP_CLASSDIR) -classpath "$(CALTROP_CLASSPATH)" $(CONTRIB_SOURCES)
