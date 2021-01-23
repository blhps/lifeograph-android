# CONSTANTS ========================================================================================
ABS_PATH    := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))



# ADJUSTABLE VARIABLES =============================================================================
NDK_PATH        ?= $(ABS_PATH)/android-ndk
NDK_ABI         ?= arm
# NDK platform level, aka APP_PLATFORM, is equivalent to minSdkVersion
API             ?= 16

PLATFORM_PREFIX ?= $(ABS_PATH)/prefix/$(NDK_ABI)

ifeq ($(NDK_ABI),x86)
 TRIPLE          := i686-linux-android
 HOST            := i686-linux-android$(API)
 HOST_GPG_ERROR  := i486-linux-gnu
 NDK_TOOLCHAIN   := x86-4.9
 CONFIG_FLAGS    := --disable-asm
 AR              := $(PLATFORM_PREFIX)/bin/i686-linux-android-ar
 LD              := $(PLATFORM_PREFIX)/bin/i686-linux-android-ld
 RANLIB          := $(PLATFORM_PREFIX)/bin/i686-linux-android-ranlib
else ifeq ($(NDK_ABI),x86_64)
 API             := 21
 TRIPLE          := x86_64-linux-android
 HOST            := x86_64-linux-android
 HOST_GPG_ERROR  := x86_64-linux-android
 NDK_TOOLCHAIN   := x86_64-4.9
 CONFIG_FLAGS    := --disable-asm
 AR              := $(PLATFORM_PREFIX)/bin/x86_64-linux-android-ar
 LD              := $(PLATFORM_PREFIX)/bin/x86_64-linux-android-ld
 RANLIB          := $(PLATFORM_PREFIX)/bin/x86_64-linux-android-ranlib
else ifeq ($(NDK_ABI),arm64)
 API             := 21
 TRIPLE          := aarch64-linux-android
 HOST            := aarch64-linux-android
 HOST_GPG_ERROR  := aarch64-linux-android
 NDK_TOOLCHAIN   := aarch64-linux-androideabi-4.9
 AR              := $(PLATFORM_PREFIX)/bin/aarch64-linux-android-ar
 LD              := $(PLATFORM_PREFIX)/bin/aarch64-linux-android-ld
 RANLIB          := $(PLATFORM_PREFIX)/bin/aarch64-linux-android-ranlib
else #ifeq ($(NDK_ABI),arm)
 TRIPLE          := arm-linux-androideabi
 HOST            := arm-linux-androideabi
 HOST_GPG_ERROR  := arm-linux-androideabi
 NDK_TOOLCHAIN   := arm-linux-androideabi-4.9
 AR              := $(PLATFORM_PREFIX)/bin/arm-linux-androideabi-ar
 LD              := $(PLATFORM_PREFIX)/bin/arm-linux-androideabi-ld
 RANLIB          := $(PLATFORM_PREFIX)/bin/arm-linux-androideabi-ranlib
endif

NDK_PLATFORM    := android-$(API)

# OTHER VARIABLES ==================================================================================
NDK_SYSROOT     := $(PLATFORM_PREFIX)/sysroot

CC      := $(PLATFORM_PREFIX)/bin/$(HOST)-clang
CXX     := $(PLATFORM_PREFIX)/bin/$(HOST)-clang++
CPP     := $(PLATFORM_PREFIX)/bin/$(HOST)-cpp
CFLAGS  = -fPIE -fPIC -D__ANDROID_API__=$(API) -DANDROID $(TARGET_CFLAGS) -isystem $(PLATFORM_PREFIX)/sysroot/usr/include/$(TRIPLE)
LDFLAGS = -latomic -pie


# include Android's build flags ====================================================================
TARGET_ARCH_ABI = $(APP_ABI)
#include $(NDK_PATH)/toolchains/$(NDK_TOOLCHAIN)/setup.mk



# INITIALIZE TOOLCHAIN =============================================================================
initialize-toolchain:
	$(NDK_PATH)/build/tools/make-standalone-toolchain.sh \
		--arch=$(NDK_ABI) --platform=$(NDK_PLATFORM) \
		--install-dir=$(PLATFORM_PREFIX) --toolchain=$(NDK_TOOLCHAIN)



# libgpg-error and libgcrypt configurations copied from
# https://github.com/guardianproject/gnupg-for-android
# libgpg-error =====================================================================================
libgpg-error-configure:
	cd libgpg-error-$(NDK_ABI) && \
		./configure \
			CC="$(CC)" AR=$(AR) RANLIB=$(RANLIB) \
			CFLAGS="$(CFLAGS)" \
			LDFLAGS="$(LDFLAGS)" \
			--host=$(HOST_GPG_ERROR) \
			--with-sysroot=$(PLATFORM_PREFIX)/sysroot\
			--prefix=$(PLATFORM_PREFIX)/sysroot
			#--disable-languages \
			#--enable-static 
	# brute force and ignorance to make libtool comply with android style
	# ls -l libgpg-error-$(NDK_ABI)/libtool
	# sed -i 's,^fast_install=.*,fast_install=needless,' libgpg-error-$(NDK_ABI)/libtool
	# sed -i 's,^version_type=.*,version_type=none,' libgpg-error-$(NDK_ABI)/libtool
	# sed -i 's,^shlibpath_overrides_runpath=.*,shlibpath_overrides_runpath=yes,' libgpg-error-$(NDK_ABI)/libtool
	# sed -i 's,^library_names_spec=.*,library_names_spec="\\$$libname\\$$release\\$$shared_ext",' libgpg-error-$(NDK_ABI)/libtool
	# sed -i 's,^soname_spec=.*,soname_spec="\\$$libname\\$$release\\$$shared_ext",' libgpg-error-$(NDK_ABI)/libtool
	# sed -i 's,^finish_cmds=.*,finish_cmds="",' libgpg-error-$(NDK_ABI)/libtool
	# sed -i 's,^sys_lib_dlsearch_path_spec=.*,sys_lib_dlsearch_path_spec="/lib /usr/lib",' libgpg-error-$(NDK_ABI)/libtool

libgpg-error-make:
	$(MAKE) -C libgpg-error-$(NDK_ABI)

libgpg-error-install:
	$(MAKE) -C libgpg-error-$(NDK_ABI) install

libgpg-error-clean:
	$(MAKE) -C libgpg-error-$(NDK_ABI) clean



# libgcrypt ========================================================================================
libgcrypt-configure:
	cd libgcrypt-$(NDK_ABI) && \
		CC="$(CC)" AR="$(AR)" RANLIB=$(RANLIB) \
			./configure \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(PLATFORM_PREFIX)/sysroot \
				--with-sysroot=$(PLATFORM_PREFIX)/sysroot\
				--prefix=$(PLATFORM_PREFIX)/sysroot \
                $(CONFIG_FLAGS)
				#--enable-maintainer-mode \
				#--enable-static \

libgcrypt-make:
	$(MAKE) -C libgcrypt-$(NDK_ABI)

libgcrypt-install:
	$(MAKE) -C libgcrypt-$(NDK_ABI) install

libgcrypt-clean:
	$(MAKE) -C libgcrypt-$(NDK_ABI) clean



# NDK BUILD ========================================================================================
ndk-build:
	NDK_PROJECT_PATH=../app/src/main $(NDK_PATH)/ndk-build  

