# actors-tools Makefile installs actors command-line tools

# delete the built-in suffixes to avoid surprises
.SUFFIXES:   

# Contents of actors-tools
BIN_FILES=actors-trace-diff actors-trace-split

# Directories

# Source directory (also: location of this Makefile)
srcdir?=.

# Installation prefix and sub-directories
prefix?=.
exec_prefix?=$(prefix)
bindir?=$(exec_prefix)/bin

BIN_FILES_DEST=$(BIN_FILES:%=$(bindir)/%)

# Generate sed scripts that substitutes @directory@ strings
SEDIFY=$(srcdir)/../install-scripts/sedify.sh
SEDCMD:=$(shell $(SEDIFY) 	@bindir@     $(bindir))

.PHONY: all uninstall clean
.PHONY: install install-dirs install-bin
.PHONY: $(BIN_FILES_DEST)

all:

clean:

install: install-dirs install-bin

install-dirs:
	@mkdir -p $(bindir)

install-bin: $(BIN_FILES_DEST)

$(BIN_FILES_DEST): $(bindir)/%: $(srcdir)/bin/%.in
	@$(SEDCMD) $< > $@
	@chmod ugo+x $@

INSTALLED_FILES=$(BIN_FILES:%=$(bindir)/%)

uninstall:
	@rm -f $(INSTALLED_FILES)
