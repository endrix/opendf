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

ifeq ($(ARM),y)
export CC=arm-none-linux-gnueabi-gcc
export LD=arm-none-linux-gnueabi-ld
export CXX=arm-none-linux-gnueabi-g++
ARMDIR=$(HOME)/work/actor/arm/usr/local
endif

ifneq "$(PROFILE)" ""
CFLAGS += -pg -fno-omit-frame-pointer
endif

ifeq ($(FB), y)
CFLAGS += -DFB
endif

ifeq ($(GTK), y)
CFLAGS += `gtk-config --cflags` -DGTK
endif

ifeq ($(SDL), y)
ifeq ($(ARM), y)
CFLAGS += -I$(ARMDIR)/include/SDL -D_GNU_SOURCE=1 -D_REENTRANT -DSDL
else
CFLAGS += `sdl-config --cflags` -DSDL
endif	
endif

ifeq ($(RM), y)
CFLAGS += -DRM
endif

ifeq ($(EDF), y)
CFLAGS += -DEDF
endif

ifeq ($(TRACE), y)
CFLAGS += -DTRACE
endif

CFLAGS += -O3
#CFLAGS += -g

INSTALL_H=actors-rts.h actors-fifo.h actors-config.h dll.h
INSTALL_SYSACTORS=art_DDRModel.cal art_Display_yuv.cal art_Sink_yuv.cal \
                  art_Sink_bin.cal art_Sink_txt.cal art_Sink_real.cal \
                  art_Source_bin.cal art_Source_txt.cal art_Source_real.cal \
                  art_DBus_test.cal

OBJECTS=actors-rts.o \
        art_Sink_bin.o art_Sink_txt.o art_Sink_real.o \
	art_Source_bin.o art_Source_txt.o art_Source_real.o \
        art_DDRModel.o art_Display_yuv.o 
#        xmlTrace.o

ifeq ($(EDF),y)
OBJECTS += edf_syscall.o
endif

ifeq ($(TRACE),y)
OBJECTS += xmlTrace.o
endif

ifeq ($(RM), y)
CXXOBJECTS =  art_DBus_test.o genericdbushandler.o
endif

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
#ARMDBUSDIR=$(HOME)/work/actor/arm-rm/usr/local

ifeq ($(RM), y)
#LDLIBS=-lpthread -lc -L$(ARMDBUSDIR)/lib -ldbus-1 $(ARMDBUSDIR)/lib/libgcc_s.so.1 $(ARMDBUSDIR)/lib/libstdc++.so.6
LDLIBS=-lpthread -lc -ldbus-1 /lib/libgcc_s.so.1 /usr/lib/libstdc++.so.6
else
LDLIBS=-lpthread -lc
endif	

ifeq ($(RM), y)
#CXX_INCLUDES = -I$(srcdir)/dbus -I$(ARMDBUSDIR)/include/dbus-1.0 -I$(ARMDBUSDIR)/lib/dbus-1.0/include
CXX_INCLUDES = -I$(srcdir)/dbus -I/usr/include/dbus-1.0 -I/usr/lib/dbus-1.0/include
endif

.PHONY: all uninstall clean
.PHONY: install install-lib install-include install-sysactors 

all: $(REAL_NAME)

ifeq ($(RM), y)
$(REAL_NAME): $(OBJECTS) $(CXXOBJECTS)
else
$(REAL_NAME): $(OBJECTS)
endif

ifeq "$(STATIC)" ""
	$(LD) -shared -soname=$(SONAME) -o $@ $(LDFLAGS) $^ $(LDLIBS)
else
	$(AR) rcs $@ $^
endif

$(OBJECTS): %.o : $(srcdir)/%.c
	$(CC) -fPIC -c -Wall $(CFLAGS) -o $@ $<

$(CXXOBJECTS): %.o : $(srcdir)/%.cpp
	$(CXX) -fPIC -c -Wall $(CFLAGS) $(CXX_INCLUDES) -o $@ $<

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
	@rm -f *.o $(REAL_NAME) *.a *~

