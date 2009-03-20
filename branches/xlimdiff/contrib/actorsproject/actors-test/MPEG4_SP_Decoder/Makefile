# datadir and srcdir (this directory) required

ifeq "$datadir" ""
  $(error no datadir specified)
endif

ifeq "$srcdir" ""
  $(error no srcdir specified)
endif

DEST_DIR=$(datadir)/actors-test/MPEG4_SP_Decoder

TESTS=acdcTest ddrTest decoderTest idct1dTest idct2dTest motionTest parserTest

# Files that need to be modified when installed (excluding .in suffix)
IN_FILES=Makefile $(TESTS:%=config-%.c) $(TESTS:%=%.nl)

# Generate sed scripts that substitutes @datadir@
SEDIFY=$(srcdir)/../../install-scripts/sedify.sh
SEDCMD:=$(shell $(SEDIFY) @datadir@ $(datadir))

# TODO: we should only provide the files that are neither in opendf/models
#       nor in share/sysactors: ParseHeaders.cal (patched) and DDRSeq.cal
 
CAL_FILES=ACPred.cal Add.cal art_DDRModel.cal art_Sink_bin.cal \
         art_Sink_txt.cal art_Source_bin.cal art_Source_txt.cal \
         BlockExpand.cal byte2bit.cal Clip.cal Combine.cal Compare.cal \
         DCPred.cal DCSplit.cal DDRSeq.cal Dequant.cal Downsample.cal \
         FairMerge.cal Final.cal Interpolate.cal MBPacker.cal \
         MemoryManager.cal MVReconstruct.cal MVSequence.cal \
         ParseHeaders.cal Retranspose.cal RowSort.cal Scale.cal \
         SearchWindow.cal SendDC.cal Separate.cal Sequence.cal \
         Shuffle.cal ShuffleFly.cal Transpose.cal Unpack.cal \
         ZigzagAddr.cal Zigzag.cal

# TODO: We should need no additional networks

NL_FILES=acdc.nl decoder.nl idct1d.nl idct2d.nl motion3.nl parser.nl

SUBDIRS=input gold $(TESTS:%=input/%) $(TESTS:%=gold/%)
 
INPUT_DATA=input/acdcTest/B.txt input/acdcTest/BTYPE.txt \
    input/ddrTest/BTYPE.txt input/ddrTest/MBA.txt input/ddrTest/MBD.txt \
    input/ddrTest/MCA.txt input/decoderTest/bits.bin \
    input/idct1dTest/ROW.txt input/idct1dTest/X0.txt input/idct1dTest/X1.txt \
    input/idct2dTest/in.txt input/idct2dTest/signed.txt \
    input/motionTest/BTYPE.txt input/motionTest/MCD.txt \
    input/motionTest/MV.txt input/motionTest/TEX.txt input/parserTest/bits.bin

GOLDEN_DATA=gold/acdcTest/out.txt gold/acdcTest/signed.txt \
    gold/ddrTest/RD.txt gold/decoderTest/VID.txt gold/idct1dTest/Y0.txt \
    gold/idct1dTest/Y1.txt gold/idct1dTest/Y2.txt gold/idct1dTest/Y3.txt \
    gold/idct2dTest/out.txt gold/motionTest/MBA.txt gold/motionTest/MBD.txt \
    gold/motionTest/MCA.txt gold/motionTest/VID.txt gold/parserTest/B.txt \
    gold/parserTest/BTYPE.txt gold/parserTest/MV.txt

COPY_FILES=$(CAL_FILES) $(NL_FILES) $(INPUT_DATA) $(GOLDEN_DATA)

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

