# examples Makefile: installs examples

# delete the built-in suffixes to avoid surprises
.SUFFIXES:   

# datadir and srcdir (this directory) required

ifeq "$datadir" ""
  $(error no datadir specified)
endif

ifeq "$srcdir" ""
  $(error no srcdir specified)
endif


DEST_DIR=$(datadir)/cal-examples

EXAMPLES=rvcDemo pipelineDemo

# Files that need to be modified when installed (excluding .in suffix)
IN_FILES=Makefile $(TESTS:%=%.nl) decoderDemo.nl

# Generate sed scripts that substitutes @datadir@
SEDIFY=$(srcdir)/../install-scripts/sedify.sh
SEDCMD:=$(shell $(SEDIFY) @datadir@ $(datadir))

C_FILES  = $(shell cd $(srcdir); find $(EXAMPLES) -name *.c)
H_FILES  = $(shell cd $(srcdir); find $(EXAMPLES) -name *.h)
CAL_FILES  = $(shell cd $(srcdir); find $(EXAMPLES) -name *.cal)
NL_FILES   = $(shell cd $(srcdir); find $(EXAMPLES) -name *.nl)
XDF_FILES  = $(shell cd $(srcdir); find $(EXAMPLES) -name *.xdf)
XCF_FILES  = $(shell cd $(srcdir); find $(EXAMPLES) -name *.xcf)
COPY_FILES = $(CAL_FILES) $(NL_FILES) $(XDF_FILES) $(C_FILES) $(H_FILES) $(XCF_FILES)

IN_FILES  = $(basename $(shell cd $(srcdir); find $(EXAMPLES) -name *.in ) )

.PHONY: all clean install uninstall $(EXAMPLES)

all: 

clean:

install: $(EXAMPLES)
	@for f in $(COPY_FILES) ; do \
           cp $(srcdir)/$$f $(DEST_DIR)/$$f; \
         done
	@for f in $(IN_FILES) ; do \
           $(SEDCMD) $(srcdir)/$$f.in > $(DEST_DIR)/$$f; \
         done

$(EXAMPLES):
	@mkdir -p $(DEST_DIR)/$@


uninstall:
	@rm -fR $(DEST_DIR)

