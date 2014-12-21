# ADJUSTABLE VARIABLES =============================================================================
PLATFORM_PREFIX=/home/ahmet/Desktop/android-ext

NDK_PATH=/home/ahmet/bin/android-ndk
NDK_ABI=arm
NDK_TOOLCHAIN=arm-linux-androideabi-4.9



# OTHER VARIABLES ==================================================================================
NDK_PLATFORM=android-14

# NDK platform level, aka APP_PLATFORM, is equivalent to minSdkVersion
NDK_SYSROOT=$(NDK_PATH)/platforms/$(NDK_PLATFORM)/arch-$(NDK_ABI)

HOST = $(NDK_ABI)-linux-androideabi

CC := $(PLATFORM_PREFIX)/bin/$(HOST)-gcc --sysroot=$(NDK_SYSROOT)
CXX := $(PLATFORM_PREFIX)/bin/$(HOST)-g++
CPP := $(PLATFORM_PREFIX)/bin/$(HOST)-cpp
LD := $(PLATFORM_PREFIX)/bin/$(HOST)-ld
AR := $(PLATFORM_PREFIX)/bin/$(HOST)-ar
RANLIB := $(PLATFORM_PREFIX)/bin/$(HOST)-ranlib



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
			--disable-languages \
			--enable-static \
			--host=$(HOST) \
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
				--prefix=$(PLATFORM_PREFIX)

libgcrypt-make:
	$(MAKE) -C libgcrypt

libgcrypt-install:
	$(MAKE) -C libgcrypt install

libgcrypt-clean:
	$(MAKE) -C libgcrypt clean



# NDK BUILD ========================================================================================
ndk-build:
	NDK_PROJECT_PATH=../app/src/main $(NDK_PATH)/ndk-build  

