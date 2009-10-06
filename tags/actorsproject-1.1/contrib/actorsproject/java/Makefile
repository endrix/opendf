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
JAVA_FILES=$(shell find $(srcdir)/eu/actorsproject -name *.java)
JASTADD_FILES=$(shell find $(srcdir)/modelcompiler -name *.jrag -name *.ast -name *.jjt -name *.java)

.PHONY: all clean install uninstall

all: actorsproject.jar

# model compiler commented out
#
# actorsproject.jar: $(JAVA_FILES) $(JASTADD_FILES)
#	@mkdir -p classes
#	@cd modelcompiler && ant
#	@$(JAVAC) -classpath $(srcdir) -d classes $(JAVA_FILES)
#	@$(JAR) -cf $@ -C classes . -C $(srcdir) COPYING

actorsproject.jar: $(JAVA_FILES) $(JASTADD_FILES)
	@mkdir -p classes
	@$(JAVAC) -classpath $(srcdir) -d classes $(JAVA_FILES)
	@$(JAR) -cf $@ -C classes . -C $(srcdir) COPYING

# model compiler commented out
#
# clean:
#	@rm -fr classes actorsproject.jar
#	@cd modelcompiler && ant clean

clean:
	@rm -fr classes actorsproject.jar

install: actorsproject.jar
	@mkdir -p $(javadir)
	@cp $< $(javadir)/.

uninstall:
	@rm -f $(javadir)/actorsproject.jar
