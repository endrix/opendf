# datadir and srcdir (this directory) required

ifeq "$datadir" ""
  $(error no datadir specified)
endif

ifeq "$srcdir" ""
  $(error no srcdir specified)
endif

DEST_DIR=$(datadir)/actors-test/RVC_MPEG4_SP_Decoder

TESTS=idct2dTest

# Files that need to be modified when installed (excluding .in suffix)
IN_FILES=Makefile $(TESTS:%=%.nl)

# Generate sed scripts that substitutes @datadir@
SEDIFY=$(srcdir)/../../install-scripts/sedify.sh
SEDCMD:=$(shell $(SEDIFY) @datadir@ $(datadir))

# TODO: we should only provide the files that are neither in opendf/models
#       nor in share/sysactors: ParseHeaders.cal (patched), DDRSeq.cal
#       and Idct2dSeq.cal

CAL_FILES=art_Sink_bin.cal art_Sink_txt.cal art_Source_txt.cal \
          art_Source_bin.cal Clip.cal rightshift.cal Scale.cal \
          scaled_1d_idct.cal Transpose.cal

# TODO: We should need no additional networks

NL_FILES=idct2d_23002.nl

SUBDIRS=input gold $(TESTS:%=input/%) $(TESTS:%=gold/%)
 
INPUT_DATA=input/idct2dTest/in.txt input/idct2dTest/signed.txt

GOLDEN_DATA=gold/idct2dTest/out.txt

COPY_FILES=$(CAL_FILES) $(NL_FILES) $(INPUT_DATA) $(GOLDEN_DATA)

.PHONY: install install_dir copy_files in_files bitstreams

install: install_dirs copy_files in_files # bitstreams

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

bitstreams:
	@mkdir -p $(DEST_DIR)/input/decoderDemo
	@cp $(srcdir)/input/decoderDemo/foreman_qcif_30.bit \
            $(DEST_DIR)/input/decoderDemo
