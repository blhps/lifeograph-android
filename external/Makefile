# CONSTANTS ========================================================================================
ABS_PATH    := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# ADJUSTABLE VARIABLES =============================================================================
NDK_PATH        ?= $(ABS_PATH)/android-ndk
NDK_ABI         ?= armv7a
# NDK platform level, aka APP_PLATFORM, is equivalent to minSdkVersion
API             ?= 21

INSTALL_PREFIX  ?= $(ABS_PATH)/prefix/$(NDK_ABI)
PLATFORM_PREFIX ?= $(NDK_PATH)/toolchains/llvm/prebuilt/linux-x86_64

AR              := $(PLATFORM_PREFIX)/bin/llvm-ar
LD              := $(PLATFORM_PREFIX)/bin/ld
RANLIB          := $(PLATFORM_PREFIX)/bin/llvm-ranlib
STRIP           := $(PLATFORM_PREFIX)/bin/llvm-strip

ifeq ($(NDK_ABI),x86)
 TRIPLE          := i686-linux-android
 HOST            := i686-linux-android$(API)
 HOST_GPG_ERROR  := i486-linux-gnu
 CONFIG_FLAGS    := --disable-asm
else ifeq ($(NDK_ABI),x86_64)
 TRIPLE          := x86_64-linux-android
 HOST            := x86_64-linux-android$(API)
 HOST_GPG_ERROR  := x86_64-linux-android
 CONFIG_FLAGS    := --disable-asm
else ifeq ($(NDK_ABI),arm64)
 TRIPLE          := aarch64-linux-android
 HOST            := aarch64-linux-android$(API)
 HOST_GPG_ERROR  := arm-linux-androideabi
else ifeq ($(NDK_ABI),riscv64)
 API             := 35
 TRIPLE          := riscv64-linux-android
 HOST            := riscv64-linux-android$(API)
 HOST_GPG_ERROR  := riscv64-linux-android
else #ifeq ($(NDK_ABI),armv7a)
 TRIPLE          := armv7a-linux-androideabi
 HOST            := armv7a-linux-androideabi$(API)
 HOST_GPG_ERROR  := arm-linux-androideabi
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
# TARGET_ARCH_ABI = $(APP_ABI)
#include $(NDK_PATH)/toolchains/$(NDK_TOOLCHAIN)/setup.mk



# INITIALIZE TOOLCHAIN =============================================================================
# initialize-toolchain:
# 	$(NDK_PATH)/build/tools/make_standalone_toolchain.py \
# 		--arch=$(NDK_ABI) --platform=$(NDK_PLATFORM) \
# 		--install-dir=$(PLATFORM_PREFIX) --toolchain=$(NDK_TOOLCHAIN)



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
			--prefix=$(INSTALL_PREFIX)/sysroot
			#--enable-static

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
			  LDFLAGS="-Wl,--undefined-version " \
				--host=$(HOST) \
				--with-gpg-error-prefix=$(PLATFORM_PREFIX)/sysroot \
				--with-sysroot=$(PLATFORM_PREFIX)/sysroot\
				--prefix=$(INSTALL_PREFIX)/sysroot \
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

