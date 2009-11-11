# datadir and srcdir (this directory) required

ifeq "$datadir" ""
  $(error no datadir specified)
endif

ifeq "$srcdir" ""
  $(error no srcdir specified)
endif

DEST_DIR=$(datadir)/actors-test/SystemActors

TESTS=txtTest binTest

# Files that need to be modified when installed (excluding .in suffix)
IN_FILES=Makefile $(TESTS:%=%.nl)

# Generate sed scripts that substitutes @datadir@
SEDIFY=$(srcdir)/../../install-scripts/sedify.sh
SEDCMD:=$(shell $(SEDIFY) @datadir@ $(datadir))

# TODO: we should only provide the files that are neither in opendf/models
 
CAL_FILES= art_Sink_bin.cal   art_Sink_txt.cal \
           art_Source_bin.cal art_Source_txt.cal

SUBDIRS=input gold $(TESTS:%=input/%) $(TESTS:%=gold/%)
 
INPUT_DATA=input/txtTest/In.txt input/binTest/In.bin

GOLDEN_DATA=gold/txtTest/Out.txt gold/binTest/Out.bin

COPY_FILES=$(CAL_FILES) $(INPUT_DATA) $(GOLDEN_DATA)

.PHONY: install install_dir copy_files in_files

install: install_dirs copy_files in_files

# Create directory
install_dirs:
	@mkdir -p $(SUBDIRS:%=$(DEST_DIR)/%)

# Simply copy these 
copy_files:
	@for f in $(COPY_FILES) ; do \
            cp -d $(srcdir)/$$f $(DEST_DIR)/$$f; \
         done

# Apply the sed command to the .in files
in_files:
	@for f in $(IN_FILES) ; do \
            $(SEDCMD) $(srcdir)/$${f}.in > $(DEST_DIR)/$$f; \
         done
