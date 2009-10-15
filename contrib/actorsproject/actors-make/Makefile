# actors-make Makefile installs actors build system

# delete the built-in suffixes to avoid surprises
.SUFFIXES:   

# Contents of actors-make
BIN_FILES=elaborator saxon8 ssagenerator xlim2c xdf2par modelcompiler \
          checkxlim xlimnorm xlimvisual
MK_FILES=Makefile definitions.mk makemodel.mk
EXAMPLE_FILES=fib.nl AddUntilOverflow.cal art_Sink_txt.cal SingleDelay.cal

XSL_FILES=generateConfig.xsl generateDepend.xsl generateInstanceParameters.xsl
JAR_LIB_FILES=saxon8.jar saxon8-dom.jar
JAR_FILES=opendf.jar $(JAR_LIB_FILES)

# Directories

# Source directory (also: location of this Makefile)
srcdir?=.
export OPENDF_CLASSDIR:=$(abspath ./classes)

# Installation prefix and sub-directories
prefix?=.
exec_prefix?=$(prefix)
bindir?=$(exec_prefix)/bin
libdir?=$(exec_prefix)/lib
includedir?=$(prefix)/include
datarootdir?=$(prefix)/share
datadir?=$(datarootdir)
javadir?=$(datarootdir)/java
xsldir?=$(datarootdir)/xsl

BIN_FILES_DEST=$(BIN_FILES:%=$(bindir)/%)
MK_FILES_DEST=$(MK_FILES:%=$(datadir)/actors-make/%)
EXAMPLE_FILES_DEST=$(EXAMPLE_FILES:%=$(datadir)/actors-make/example/%)

# Generate sed scripts that substitutes @directory@ strings
SEDIFY=$(srcdir)/../install-scripts/sedify.sh
SEDCMD:=$(shell $(SEDIFY) \
          @bindir@     $(bindir)     @libdir@ $(libdir) \
          @includedir@ $(includedir) @datadir@ $(datadir) \
          @javadir@    $(javadir)    @xsldir@  $(xsldir))

.PHONY: all uninstall clean opendf
.PHONY: install install-dirs install-bin install-mk install-xsl install-java
.PHONY: $(BIN_FILES_DEST) $(MK_FILES_DEST) $(EXAMPLE_FILES_DEST)

all: opendf.jar

opendf.jar: opendf

opendf:
	@mkdir -p $(OPENDF_CLASSDIR)
	@$(MAKE) -C $(OPENDF_CLASSDIR) -f $(OPENDF_HOME)/source/make/Makefile \
                OPENDF_HOME=$(OPENDF_HOME) OPENDF_CLASSDIR=$(OPENDF_CLASSDIR)

clean:
	@rm -rf opendf.jar $(OPENDF_CLASSDIR)

install: install-dirs install-bin install-mk install-xsl install-java

install-dirs:
	@mkdir -p $(bindir)
	@mkdir -p $(datadir)/actors-make/example
	@mkdir -p $(javadir)
	@mkdir -p $(xsldir)

install-bin: $(BIN_FILES_DEST)

$(BIN_FILES_DEST): $(bindir)/%: $(srcdir)/bin/%.in
	@$(SEDCMD) $< > $@
	@chmod ugo+x $@

install-mk: $(MK_FILES_DEST) $(EXAMPLE_FILES_DEST)

$(MK_FILES_DEST): $(datadir)/actors-make/% : $(srcdir)/share/actors-make/%.in
	@$(SEDCMD) $< > $@

$(EXAMPLE_FILES_DEST): $(datadir)/% : $(srcdir)/share/%
	@cp $< $@

install-xsl: 
	@cp $(XSL_FILES:%=$(srcdir)/share/xsl/%) $(xsldir)/.

install-java: # opendf.jar  <-- TODO opendf Makefile doesn't check timestamps
	@cp opendf.jar $(JAR_LIB_FILES:%=$(OPENDF_HOME)/lib/%) $(javadir)/.

INSTALLED_FILES=$(BIN_FILES:%=$(bindir)/%) \
                $(MK_FILES:%=$(datadir)/actors-make/%) \
                $(XSL_FILES:%=$(xsldir)/%) \
                $(JAR_FILES:%=$(javadir)/jar/%)

uninstall:
	@rm -f $(INSTALLED_FILES)
