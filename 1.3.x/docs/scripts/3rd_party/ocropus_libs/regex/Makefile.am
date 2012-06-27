#  
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without 
# modifications, as long as this notice is preserved.
# 
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

# Where to install the library and object files.
libdir = $(exec_prefix)/lib
inst_libdir = $(libdir)

resdir=@top_builddir@/resource
VPATH=@srcdir@ $(resdir)
SUFFIXES=.c .o .rc .exe .dll
MAJOR=$(shell echo $(VERSION) | sed -e "s/\..*$$//")
MINOR=$(shell echo $(VERSION) | sed -e "s/^[^\.]*\.0*\([0-9]\+\).*$$/\1/") 
VERSADD=-Wl,--major-image-version=$(MAJOR) -Wl,--minor-image-version=$(MINOR)

# Rule to make compiled resource (Windows)
.rc.o:
	$(RCTOOL) --include-dir $(resdir) -i $< -o $@
.rc.lo:
	$(RCTOOL) --include-dir $(resdir) -i $< -o ${@:.lo=.o}
	sed -e 's/regex\.lo/'$@'/' -e 's/regex\.o/'${@:.lo=.o}'/' regex.lo > $@


lib_LTLIBRARIES = libregex.la

include_HEADERS = regex.h
noinst_HEADERS = re_comp.h regex_internal.h

libregex_la_SOURCES =  regex.c regex-dllversion.c regex-dll-res.rc

libregex_la_LDFLAGS = -no-undefined -version-info 2:7:0 $(VERSADD)
