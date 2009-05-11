# actors-rts Makefile: creates/installs actors run-time

MAJOR=1
MINOR=0
BUILD=

ifeq "$(BUILD)" ""
MINOR_DOT_BUILD=$(MINOR)
else
MINOR_DOT_BUILD=$(MINOR).$(BUILD)
endif

BASENAME=libactors-rts
ifeq "$(STATIC)" ""
LINKER_NAME=$(BASENAME).so
SONAME=$(LINKER_NAME).$(MAJOR)
REAL_NAME=$(SONAME).$(MINOR_DOT_BUILD)
else
REAL_NAME=$(BASENAME).a
endif

ifneq "$(PROFILE)" ""
CFLAGS += -pg
endif

ifeq ($(FB), y)
CFLAGS += -DFB
endif

ifeq ($(GTK), y)
CFLAGS += -I/usr/include/gtk-2.0 -I/usr/lib/gtk-2.0/include -I/usr/include/atk-1.0 -I/usr/include/cairo -I/usr/include/pango-1.0 -I/usr/include/glib-2.0 -I/usr/lib/glib-2.0/include -I/usr/include/pixman-1 -I/usr/include/freetype2 -I/usr/include/libpng12 -DGTK
endif

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
ifeq "$(STATIC)" ""
	$(LD) -shared -soname=$(SONAME) -o $@ $(LDFLAGS) $^ $(LDLIBS)
else
	$(AR) rcs $@ $^
endif

$(OBJECTS): %.o : $(srcdir)/%.c
	$(CC) -fPIC -c -g -Wall $(CFLAGS) -o $@ $<

install: install-lib install-include install-sysactors

install-lib:
	@mkdir -p $(libdir)
	@cp $(REAL_NAME) $(libdir)/.
ifeq "$(STATIC)" ""
	@cd $(libdir) && ln -sf $(REAL_NAME) $(SONAME)
	@cd $(libdir) && ln -sf $(SONAME) $(LINKER_NAME)
endif

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

