# actors-rts Makefile: creates/installs actors run-time

MAJOR=1
MINOR=0
BUILD=

ifeq "$(BUILD)" ""
MINOR_DOT_BUILD=$(MINOR)
else
MINOR_DOT_BUILD=$(MINOR).$(BUILD)
endif

LINKER_NAME=libactors-rts.so
SONAME=$(LINKER_NAME).$(MAJOR)
REAL_NAME=$(SONAME).$(MINOR_DOT_BUILD)

INSTALL_H=actors-rts.h circbuf.h dll.h
INSTALL_SYSACTORS=art_DDRModel.cal art_Display_yuv.cal \
                  art_Sink_bin.cal art_Sink_yuv.cal art_Sink_txt.cal \
                  art_Source_bin.cal art_Source_txt.cal

OBJECTS=actors-rts.o exec_network.o circbuf.o \
        art_Sink_bin.o art_Sink_txt.o art_Source_bin.o art_Source_txt.o \
        art_DDRModel.o art_Display_yuv.o dll.o

# delete the built-in suffixes to avoid surprises
.SUFFIXES:   

# Directories

srcdir?=.
prefix?=.
exec_prefix?=$(prefix)
includedir?=$(prefix)/include
libdir?=$(exec_prefix)/lib
datarootdir?=$(prefix)/share
datadir?=$(datarootdir)

LDLIBS=-lpthread -lc

.PHONY: all uninstall clean
.PHONY: install install-lib install-include install-sysactors 

all: $(REAL_NAME)

$(REAL_NAME): $(OBJECTS)
	$(LD) -shared -soname=$(SONAME) -o $@ $(LDFLAGS) $^ $(LDLIBS)

$(OBJECTS): %.o : $(srcdir)/%.c
	$(CC) -fPIC -c -g -Wall $(CFLAGS) -o $@ $<

install: install-lib install-include install-sysactors

install-lib:
	@mkdir -p $(libdir)
	@cp $(REAL_NAME) $(libdir)/.
	@cd $(libdir) && ln -sf $(REAL_NAME) $(SONAME)
	@cd $(libdir) && ln -sf $(SONAME) $(LINKER_NAME)

install-include:
	@mkdir -p $(includedir)
	@cp $(INSTALL_H:%=$(srcdir)/%) $(includedir)/.

install-sysactors:
	@mkdir -p $(datadir)/sysactors
	@cp $(INSTALL_SYSACTORS:%=$(srcdir)/sysactors/%) $(datadir)/sysactors/.

INSTALLED_FILES=$(libdir)/$(REAL_NAME) \
                $(libdir)/$(SONAME) \
                $(libdir)/$(LINKER_NAME) \
                $(INSTALL_H:%=$(includedir)/%) \
                $(INSTALL_SYSACTORS:%=$(datadir)/sysactors/%)

uninstall:
	@rm -f $(INSTALLED_FILES)

clean:
	@rm -f *.o $(REAL_NAME)

