PLATFORM_PREFIX := /home/ahmet/Desktop/android-ext

LOCAL_PATH := $(call my-dir)


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
