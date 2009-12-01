# datadir and srcdir (this directory) required

ifeq "$datadir" ""
  $(error no datadir specified)
endif

ifeq "$srcdir" ""
  $(error no srcdir specified)
endif

DEST_DIR=$(datadir)/actors-test/RVC_MPEG4_SP_Decoder

TESTS=idct2dTest texture_16x16Test

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
          scaled_1d_idct.cal Transpose.cal \
          GEN_mgnt_DCSplit.cal MPEG4_algo_DCRaddressing_16x16.cal \
          MPEG4_algo_DCRinvpred_luma_16x16.cal MPEG4_algo_IAP_16x16.cal \
          MPEG4_algo_Inversequant.cal MPEG4_algo_IS.cal

# TODO: We should need no additional networks

XDF_FILES=idct2d_23002.xdf texture_16x16.xdf DC_Reconstruction_16x16.xdf

SUBDIRS=input gold $(TESTS:%=input/%) $(TESTS:%=gold/%)

INPUT_DATA=input/idct2dTest/IN.txt input/idct2dTest/SIGNED.txt \
           input/texture_16x16Test/BTYPE.txt \
           input/texture_16x16Test/QFS.txt \
           input/texture_16x16Test/QP.txt 

GOLDEN_DATA=gold/idct2dTest/OUT.txt \
            gold/texture_16x16Test/f.txt

COPY_FILES=$(CAL_FILES) $(XDF_FILES) $(INPUT_DATA) $(GOLDEN_DATA)

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
