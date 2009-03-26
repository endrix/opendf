# actorsproj Makefile: builds and installs actors-rts, actors-make, 
#                      actor-test and the actorsproj java source
#
# Make targets: all install uninstall clean (usual meaning)
#               actors-rts actors-make actors-test (sub projects)
#               actors-rts/install actors-make/install actors-test/install
#               actors-rts/uninstall (etc.)
#               actors-rts/clean     (etc.)

# All sub-projects
SUBPROJECTS=actors-rts java actors-make actors-test

# Sub-projects that are built
BUILD=actors-rts java actors-make

# Sub-projects that are installed
INSTALL=actors-rts java actors-make actors-test


export SHELL=/bin/sh

# delete the built-in suffixes to avoid surprises
.SUFFIXES:   

# Source directory: a path to this directory
# To do out-of-source-tree builds 'cd' to the build directory and do
# make srcdir=<path-to-this-dir> -f <path-to-this-dir>/Makefile
srcdir?=.
override srcdir:=$(abspath $(srcdir))

export OPENDF_HOME:=$(abspath $(srcdir)/../../core)

# Install prefix (required by install/unstall targets only)
prefix?=.
override prefix:=$(abspath $(prefix))
export prefix
export exec_prefix?=$(prefix)
export bindir?=$(exec_prefix)/bin
export includedir?=$(prefix)/include
export libdir?=$(exec_prefix)/lib
export datarootdir?=$(prefix)/share
export datadir?=$(datarootdir)

#
# Top-level rules, "all subprojects in one go" 
#

.PHONY: all install uninstall clean help

all: $(BUILD:%=%/all)

clean: $(BUILD:%=%/clean)

install: $(INSTALL:%=%/install)

uninstall: $(INSTALL:%=%/uninstall)

#
# Rules for each of the sub-projects
#

.PHONY: $(SUBPROJECTS)

$(SUBPROJECTS): % : %/all

GENTARGETS=$(SUBPROJECTS:%=%/all) $(SUBPROJECTS:%=%/install) \
           $(SUBPROJECTS:%=%/uninstall) $(SUBPROJECTS:%=%/clean)

.PHONY: $(GENTARGETS)

# Make sure the build directory exists and do a recursive make

$(GENTARGETS):
	@mkdir -p $(@D)
	@$(MAKE) -C $(@D) -f $(srcdir)/$(@D)/Makefile \
                srcdir=$(srcdir)/$(@D) $(@F)

help:
	@echo
	@echo Usage:
	@echo 'make [srcdir=<path to contrib/actorsproject>]'
	@echo '     Builds run-time system and actorsproject.jar'
	@echo
	@echo 'make [srcdir=<path to contrib/actorsproject>]' \
                   '[prefix=<install-dir>] install'
	@echo '     Installs actorsproject in <install-dir>/{bin,include,lib,share}'
	@echo
	@echo 'make [srcdir=<path to contrib/actorsproject>] clean'
	@echo '     Remove build files'
	@echo
	@echo 'make [srcdir=<path to contrib/actorsproject>]' \
                   '[prefix=<install-dir>] uninstall'
	@echo '     Remove installed files'
	@echo
	@echo 'srcdir and prefix are the current directory by default'
	@echo
