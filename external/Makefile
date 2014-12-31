# CONSTANTS ========================================================================================
ABS_PATH    := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))



# ADJUSTABLE VARIABLES =============================================================================
NDK_PATH        ?= $(ABS_PATH)/android-ndk
NDK_ABI         ?= arm

PLATFORM_PREFIX ?= $(ABS_PATH)/prefix/$(NDK_ABI)

ifeq ($(NDK_ABI),x86)
 HOST            := i686-linux-android
 HOST_GPG_ERROR  := i486-linux-gnu
 NDK_TOOLCHAIN   := x86-4.9
 CONFIG_FLAGS    := --disable-asm
else ifeq ($(NDK_ABI),mips)
 HOST            := mipsel-linux-android
 HOST_GPG_ERROR  := mipsel-linux-android
 NDK_TOOLCHAIN   := mipsel-linux-android-4.9
else
 HOST            := arm-linux-androideabi
 HOST_GPG_ERROR  := arm-linux-androideabi
 NDK_TOOLCHAIN   := arm-linux-androideabi-4.9
endif



# OTHER VARIABLES ==================================================================================
NDK_PLATFORM    := android-14

# NDK platform level, aka APP_PLATFORM, is equivalent to minSdkVersion
NDK_SYSROOT     := $(NDK_PATH)/platforms/$(NDK_PLATFORM)/arch-$(NDK_ABI)

CC      := $(PLATFORM_PREFIX)/bin/$(HOST)-gcc --sysroot=$(NDK_SYSROOT)
CXX     := $(PLATFORM_PREFIX)/bin/$(HOST)-g++
CPP     := $(PLATFORM_PREFIX)/bin/$(HOST)-cpp
LD      := $(PLATFORM_PREFIX)/bin/$(HOST)-ld
AR      := $(PLATFORM_PREFIX)/bin/$(HOST)-ar
RANLIB  := $(PLATFORM_PREFIX)/bin/$(HOST)-ranlib

CFLAGS  = -DANDROID -I$(LOCAL)/include $(TARGET_CFLAGS)
LDFLAGS = -llog -L$(LOCAL)/lib $(TARGET_LDFLAGS)



# include Android's build flags ====================================================================
TARGET_ARCH_ABI = $(APP_ABI)
include $(NDK_PATH)/toolchains/$(NDK_TOOLCHAIN)/setup.mk



# INITIALIZE TOOLCHAIN =============================================================================
initialize-toolchain:
	mkdir $(PLATFORM_PREFIX)
	$(NDK_PATH)/build/tools/make-standalone-toolchain.sh --platform=$(NDK_PLATFORM) \
		--install-dir=$(PLATFORM_PREFIX) --toolchain=$(NDK_TOOLCHAIN)



# libgpg-error and libgcrypt configurations copied from
# https://github.com/guardianproject/gnupg-for-android
# libgpg-error =====================================================================================
libgpg-error-configure:
	cd libgpg-error && \
		./configure \
			CC="$(CC)" AR=$(AR) RANLIB=$(RANLIB) \
			CFLAGS="$(CFLAGS)" \
			LDFLAGS="$(LDFLAGS)" \
			--disable-languages \
			--enable-static \
			--host=$(HOST_GPG_ERROR) \
			--prefix=$(PLATFORM_PREFIX)
	ls -l libgpg-error/libtool
	# brute force and ignorance to make libtool comply with android style
	sed -i 's,^fast_install=.*,fast_install=needless,' libgpg-error/libtool
	sed -i 's,^version_type=.*,version_type=none,' libgpg-error/libtool
	sed -i 's,^shlibpath_overrides_runpath=.*,shlibpath_overrides_runpath=yes,' libgpg-error/libtool
	sed -i 's,^library_names_spec=.*,library_names_spec="\\$$libname\\$$release\\$$shared_ext",' libgpg-error/libtool
	sed -i 's,^soname_spec=.*,soname_spec="\\$$libname\\$$release\\$$shared_ext",' libgpg-error/libtool
	sed -i 's,^finish_cmds=.*,finish_cmds="",' libgpg-error/libtool
	sed -i 's,^sys_lib_dlsearch_path_spec=.*,sys_lib_dlsearch_path_spec="/lib /usr/lib",' libgpg-error/libtool

libgpg-error-make:
	$(MAKE) -C libgpg-error

libgpg-error-install:
	$(MAKE) -C libgpg-error install

libgpg-error-clean:
	$(MAKE) -C libgpg-error clean



# libgcrypt ========================================================================================
libgcrypt-configure:
	cd libgcrypt && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) \
			./configure \
				--enable-maintainer-mode \
				--enable-static \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(PLATFORM_PREFIX) \
				--prefix=$(PLATFORM_PREFIX)\
                $(CONFIG_FLAGS)

libgcrypt-make:
	$(MAKE) -C libgcrypt

libgcrypt-install:
	$(MAKE) -C libgcrypt install

libgcrypt-clean:
	$(MAKE) -C libgcrypt clean



# NDK BUILD ========================================================================================
ndk-build:
	NDK_PROJECT_PATH=../app/src/main $(NDK_PATH)/ndk-build  

