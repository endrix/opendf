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
REAL_NAME=$(BASENAME).a

ifeq ($(ARM),y)
export CC=arm-none-linux-gnueabi-gcc
export LD=arm-none-linux-gnueabi-ld
export CXX=arm-none-linux-gnueabi-g++
export AR=arm-none-linux-gnueabi-ar
ARMDIR=$(HOME)/work/actor/arm/usr/local
endif

ifneq "$(PROFILE)" ""
override CFLAGS += -pg -fno-omit-frame-pointer
endif

ifeq ($(ARM), y)
override CFLAGS += -I$(ARMDIR)/include/libxml2
else
override CFLAGS += `xml2-config --cflags`
endif

# Enable action tracing in the system actors
override CFLAGS += -DTRACE

ifeq ($(RM), y)
override CFLAGS += -DRM
endif

override CFLAGS += -O3
#override CFLAGS += -g

INSTALL_H=actors-rts.h actors-fifo.h actors-config.h dll.h

INSTALL_SYSACTORS=art_DDRModel.cal art_Display_yuv.cal art_Sink_yuv.cal \
                  art_Sink_bin.cal art_Sink_txt.cal art_Sink_real.cal \
                  art_Source_bin.cal art_Source_txt.cal art_Source_real.cal \
                  art_DBus_test.cal art_consumer.cal art_producer.cal art_Display_yuv_rm.cal

INSTALL_C_SRC=display.h display-fb.c display-sdl.c display-gtk.c display-null.c \
              art_DBus_test.cpp genericdbushandler.cpp dbus/custommessages.h \
			  dbus/FastDelegate.h dbus/genericdbushandler.h dbus/message.h \
			  internal.h xmlParser.h

OBJECTS=actors-rts.o xmlParser.o xmlTrace.o termination-report.o \
        art_Sink_bin.o art_Sink_txt.o art_Sink_real.o \
	    art_Source_bin.o art_Source_txt.o art_Source_real.o \
        art_DDRModel.o art_Display_yuv.o display.o \
        art_consumer.o art_producer.o art_Display_yuv_rm.o

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

ifeq ($(RM), y)
ifeq ($(ARM),y)
override LDLIBS += -lpthread -lc -L$(ARMDIR)/lib -ldbus-1 $(ARMDIR)/lib/libgcc_s.so.1 $(ARMDIR)/lib/libstdc++.so.6
else
override LDLIBS += -lpthread -lc -ldbus-1 /lib/libgcc_s.so.1 /usr/lib/libstdc++.so.6
endif
else
override LDLIBS += -lpthread -lc
endif	

ifeq ($(RM), y)
ifeq ($(ARM),y)
override CXX_INCLUDES += -I$(srcdir)/dbus -I$(ARMDIR)/include/dbus-1.0 -I$(ARMDIR)/lib/dbus-1.0/include
else
override CXX_INCLUDES += -I$(srcdir)/dbus -I/usr/include/dbus-1.0 -I/usr/lib/dbus-1.0/include
endif
endif

.PHONY: all uninstall clean
.PHONY: install install-lib install-include install-sysactors 

all: $(REAL_NAME)

ifeq ($(RM), y)
$(REAL_NAME): $(OBJECTS) $(CXXOBJECTS)
else
$(REAL_NAME): $(OBJECTS)
endif
	$(AR) rcs $@ $^

$(OBJECTS): %.o : $(srcdir)/%.c
	$(CC) -fPIC -c -Wall $(CFLAGS) -o $@ $<

$(CXXOBJECTS): %.o : $(srcdir)/%.cpp
	$(CXX) -fPIC -c -Wall $(CFLAGS) $(CXX_INCLUDES) -o $@ $<

install: install-lib install-include install-sysactors

install-lib:
	@mkdir -p $(libdir)
	@cp $(REAL_NAME) $(libdir)/.

install-include:
	@mkdir -p $(includedir)
	@cp $(INSTALL_H:%=$(srcdir)/%) $(includedir)/.

install-sysactors:
	@mkdir -p $(datadir)/sysactors/cal
	@mkdir -p $(datadir)/sysactors/c
	@cp $(INSTALL_SYSACTORS:%=$(srcdir)/sysactors/%) $(datadir)/sysactors/cal/.
	@cp $(INSTALL_C_SRC:%=$(srcdir)/%) $(datadir)/sysactors/c/.

INSTALLED_FILES=$(libdir)/$(REAL_NAME) \
                $(INSTALL_H:%=$(includedir)/%) \
                $(INSTALL_SYSACTORS:%=$(datadir)/sysactors/cal/%) \
                $(INSTALL_C_SRC:%=$(datadir)/sysactors/c/%)

uninstall:
	@rm -f $(INSTALLED_FILES)

clean:
	@rm -f *.o $(REAL_NAME) *.a *~

