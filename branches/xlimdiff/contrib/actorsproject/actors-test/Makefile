# actors-test Makefile installs actors test system

# delete the built-in suffixes to avoid surprises
.SUFFIXES:   

# Contents of actors-make
TEST_DIRS=SystemActors MPEG4_SP_Decoder
MK_FILE_DEST=$(datadir)/actors-test/Makefile

# Directories

# Source directory (also: location of this Makefile)
srcdir?=.

# Installation prefix and sub-directories
prefix?=.
datarootdir?=$(prefix)/share
datadir?=$(datarootdir)
export datadir

# Generate sed scripts that substitutes @datadir@
SEDIFY=$(srcdir)/../install-scripts/sedify.sh
SEDCMD:=$(shell $(SEDIFY) @datadir@ $(datadir))

.PHONY: all install uninstall clean $(TEST_DIRS) $(MK_FILE_DEST)

all:

install: $(TEST_DIRS) $(MK_FILE_DEST)

$(TEST_DIRS):
	@$(MAKE) -f $(srcdir)/$@/Makefile --no-print-directory \
                 srcdir=$(srcdir)/$@ install

$(MK_FILE_DEST):
	@$(SEDCMD) $(srcdir)/Makefile.in > $@

INSTALLED_FILES=$(TEST_DIRS:%=$(datadir)/actors-test/%) $(MK_FILE_DEST)

uninstall:
	@rm -fR $(INSTALLED_FILES)

clean:
