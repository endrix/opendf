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

.PHONY: all clean install uninstall

all: actorsproject.jar

actorsproject.jar: $(JAVA_FILES)
	@mkdir -p classes
	@$(JAVAC) -classpath $(srcdir) -d classes $(JAVA_FILES)
	@$(JAR) -cf $@ -C classes eu -C $(srcdir) COPYING

clean:
	@rm -fr classes

install: actorsproject.jar
	@mkdir -p $(javadir)
	@cp $< $(javadir)/.

uninstall:
	@rm -f $(javadir)/actorsproject.jar
