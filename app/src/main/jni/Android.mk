# CONSTANTS ========================================================================================
ABS_PATH    := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

LOCAL_PATH  := $(call my-dir)



# ADJUSTABLE VARIABLES =============================================================================
#NDK_ABI         ?= arm

PLATFORM_PREFIX ?= $(ABS_PATH)/../../../../external/prefix/$(TARGET_ARCH)



# libgcrypt
include $(CLEAR_VARS)
LOCAL_MODULE    := libgcrypt
LOCAL_SRC_FILES := $(PLATFORM_PREFIX)/lib/libgcrypt.so
include $(PREBUILT_SHARED_LIBRARY)


# libgpg-error
include $(CLEAR_VARS)
LOCAL_MODULE    := libgpg-error
LOCAL_SRC_FILES := $(PLATFORM_PREFIX)/lib/libgpg-error.so
LOCAL_EXPORT_C_INCLUDES := $(PLATFORM_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)


# JNI
include $(CLEAR_VARS)
LOCAL_MODULE    := lifeocrypt
LOCAL_SRC_FILES := lifeocrypt.c helpers.c
LOCAL_SHARED_LIBRARIES := libgpg-error libgcrypt
include $(BUILD_SHARED_LIBRARY)
