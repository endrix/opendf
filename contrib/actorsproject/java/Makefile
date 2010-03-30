# java Makefile builds ACTORS java source and installs actorsproject.jar

# delete the built-in suffixes to avoid surprises
.SUFFIXES:   

# Tools
JAVAC=javac
JAR=jar

# Directories

# Source directory (also: location of this Makefile)
srcdir?=.

# Installation prefix and sub-directories
prefix?=.
datarootdir?=$(prefix)/share
datadir?=$(datarootdir)
javadir?=$(datarootdir)/java


# Source files
# Commented out Model Compiler (build file is broken?)
# JAVA_FILES=$(shell find $(srcdir)/eu/actorsproject $(srcdir)/modelcompiler -name *.java)
JAVA_FILES=$(shell find $(srcdir)/eu/actorsproject -name *.java)
AST_FILES=$(shell find $(srcdir)/modelcompiler -name *.ast)
JJT_FILES=$(shell find $(srcdir)/modelcompiler -name *.jjt)
JRAG_FILES=$(shell find $(srcdir)/modelcompiler -name *.jrag)
JASTADD_FILES=$(AST_FILES) $(JJT_FILES) $(JRAG_FILES)

.PHONY: all clean install uninstall jastadd

all: actorsproject.jar

# Commented out Model Compiler (build file is broken?)
# actorsproject.jar: $(JAVA_FILES) $(JASTADD_FILES)
actorsproject.jar: $(JAVA_FILES) 
	@mkdir -p classes
	# Commented out Model Compiler (build file is broken?)
	# @ant -f $(srcdir)/modelcompiler/build.xml  -Dbuilddir=$(CURDIR)
	@$(JAVAC) -classpath $(srcdir) -d classes $(JAVA_FILES)
	@$(JAR) -cf $@ -C classes . -C $(srcdir) COPYING

clean:
	@rm -fr classes actorsproject.jar
	# Commented out Model Compiler (build file is broken?)
	# @ant -f $(srcdir)/modelcompiler/build.xml -Dbuilddir=$(CURDIR) clean


install: actorsproject.jar
	@mkdir -p $(javadir)
	@cp $< $(javadir)/.

uninstall:
	@rm -f $(javadir)/actorsproject.jar
