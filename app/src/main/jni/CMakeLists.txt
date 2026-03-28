cmake_minimum_required(VERSION 3.22.1)

project(lifeograph_core)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Define ANDROID macro
add_definitions(-D__ANDROID__)

include(FetchContent)
include(ExternalProject)

# Fetch Abseil (RE2 dependency)
FetchContent_Declare(
  absl
  GIT_REPOSITORY https://github.com/abseil/abseil-cpp.git
  GIT_TAG        20240116.2
)
# Fetch RE2
FetchContent_Declare(
  re2
  GIT_REPOSITORY https://github.com/google/re2.git
  GIT_TAG        2024-03-01
)

# Configure dependencies before making them available
set(ABSL_PROPAGATE_CXX_STD ON CACHE BOOL "" FORCE)
set(ABSL_ENABLE_INSTALL OFF CACHE BOOL "" FORCE)
set(RE2_BUILD_TESTING OFF CACHE BOOL "" FORCE)

# Disable installation rules for dependencies to avoid export set errors in Android build
set(CMAKE_SKIP_INSTALL_RULES ON)

# RE2 needs to find Abseil. When using FetchContent, we need to make sure
# Abseil targets are available before RE2 looks for them.
FetchContent_MakeAvailable(absl re2)

# --- Build libgpg-error and libgcrypt ---
# Mapping ABIs to source directory suffixes and Autotools host triples
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(ABI_SUFFIX "armv7a")
    set(AUTOCONF_HOST "arm-linux-androideabi")
    set(CLANG_TARGET "armv7a-linux-androideabi${ANDROID_PLATFORM_LEVEL}")
elseif(ANDROID_ABI STREQUAL "arm64-v8a")
    set(ABI_SUFFIX "arm64")
    set(AUTOCONF_HOST "aarch64-linux-android")
    set(CLANG_TARGET "aarch64-linux-android${ANDROID_PLATFORM_LEVEL}")
elseif(ANDROID_ABI STREQUAL "x86")
    set(ABI_SUFFIX "x86")
    set(AUTOCONF_HOST "i686-linux-android")
    set(CLANG_TARGET "i686-linux-android${ANDROID_PLATFORM_LEVEL}")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(ABI_SUFFIX "x86_64")
    set(AUTOCONF_HOST "x86_64-linux-android")
    set(CLANG_TARGET "x86_64-linux-android${ANDROID_PLATFORM_LEVEL}")
else()
    set(ABI_SUFFIX ${ANDROID_ABI})
    set(CLANG_TARGET "${ANDROID_LLVM_TRIPLE}${ANDROID_PLATFORM_LEVEL}")
endif()

set(EXTERNAL_SRCS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../external")

# Logic to automatically create architecture-specific source directories if they don't exist
foreach(LIB_NAME libgpg-error libgcrypt)
    set(BASE_DIR "${EXTERNAL_SRCS_DIR}/${LIB_NAME}")
    set(ABI_DIR "${EXTERNAL_SRCS_DIR}/${LIB_NAME}-${ABI_SUFFIX}")
    if(EXISTS "${BASE_DIR}" AND NOT EXISTS "${ABI_DIR}")
        message(STATUS "Creating ${ABI_DIR} from ${BASE_DIR}")
        file(COPY "${BASE_DIR}/" DESTINATION "${ABI_DIR}")
    endif()
endforeach()

set(GPG_ERROR_SRC "${EXTERNAL_SRCS_DIR}/libgpg-error-${ABI_SUFFIX}")
set(GCRYPT_SRC "${EXTERNAL_SRCS_DIR}/libgcrypt-${ABI_SUFFIX}")

# Fallback to base directory if architecture-specific one is still missing
if(NOT EXISTS "${GPG_ERROR_SRC}")
    set(GPG_ERROR_SRC "${EXTERNAL_SRCS_DIR}/libgpg-error")
endif()
if(NOT EXISTS "${GCRYPT_SRC}")
    set(GCRYPT_SRC "${EXTERNAL_SRCS_DIR}/libgcrypt")
endif()

set(INSTALL_DIR "${CMAKE_CURRENT_BINARY_DIR}/external_install")
file(MAKE_DIRECTORY ${INSTALL_DIR})
file(MAKE_DIRECTORY ${INSTALL_DIR}/include)
file(MAKE_DIRECTORY ${INSTALL_DIR}/lib)

set(GPG_ERROR_LIB "${INSTALL_DIR}/lib/libgpg-error.so")
set(GCRYPT_LIB "${INSTALL_DIR}/lib/libgcrypt.so")

execute_process(
    COMMAND gcc -dumpmachine
    OUTPUT_VARIABLE BUILD_HOST_TRIPLE
    OUTPUT_STRIP_TRAILING_WHITESPACE
)



# libgpg-error lock file
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(LOCK_OBJ_SRC "lock-obj-pub.aarch64-unknown-linux-androideabi.h")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(LOCK_OBJ_SRC "lock-obj-pub.arm-unknown-linux-androideabi.h")
elseif(ANDROID_ABI STREQUAL "x86")
    set(LOCK_OBJ_SRC "lock-obj-pub.i686-unknown-linux-androideabi.h")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(LOCK_OBJ_SRC "lock-obj-pub.x86_64-unknown-linux-androideabi.h")
endif()

set(SYSCFG_DIR "${GPG_ERROR_SRC}/src/syscfg")
set(LOCK_OBJ_DST "${SYSCFG_DIR}/lock-obj-pub.linux-android.h")

if(NOT EXISTS "${LOCK_OBJ_DST}")
    message(STATUS "Copying ${LOCK_OBJ_SRC} -> lock-obj-pub.linux-android.h")
    configure_file(
        "${SYSCFG_DIR}/${LOCK_OBJ_SRC}"
        "${LOCK_OBJ_DST}"
        COPYONLY
    )
endif()

# Build libgpg-error
ExternalProject_Add(gpg_error_build
    DOWNLOAD_COMMAND ""
    SOURCE_DIR ${GPG_ERROR_SRC}
    BUILD_BYPRODUCTS ${GPG_ERROR_LIB}
    CONFIGURE_COMMAND ${GPG_ERROR_SRC}/configure
        --host=${AUTOCONF_HOST}
        --build=${BUILD_HOST_TRIPLE}
        --prefix=${INSTALL_DIR}
        --with-sysroot=${CMAKE_SYSROOT}
        --disable-tests
        --disable-doc
        --disable-languages
        "CC=${CMAKE_C_COMPILER} --target=${CLANG_TARGET}"
        "AS=${CMAKE_C_COMPILER} --target=${CLANG_TARGET}"
        "AR=${CMAKE_AR}"
        "RANLIB=${CMAKE_RANLIB}"
        "NM=${CMAKE_NM}"
        "STRIP=${CMAKE_STRIP}"
        "CFLAGS=${CMAKE_C_FLAGS} --sysroot=${CMAKE_SYSROOT}"
        "LDFLAGS=${CMAKE_SHARED_LINKER_FLAGS}"
        "CC_FOR_BUILD=gcc"
    BUILD_COMMAND make -j4
    INSTALL_COMMAND make install
)

# Build libgcrypt
ExternalProject_Add(gcrypt_build
    DEPENDS gpg_error_build
    DOWNLOAD_COMMAND ""
    SOURCE_DIR ${GCRYPT_SRC}
    BUILD_BYPRODUCTS ${GCRYPT_LIB}
    CONFIGURE_COMMAND ${GCRYPT_SRC}/configure
        --host=${AUTOCONF_HOST}
        --prefix=${INSTALL_DIR}
        --with-gpg-error-prefix=${INSTALL_DIR}
        --with-sysroot=${CMAKE_SYSROOT}
        --disable-asm
        --disable-doc
        --disable-languages
        "CC=${CMAKE_C_COMPILER} --target=${CLANG_TARGET}"
        "AS=${CMAKE_C_COMPILER} --target=${CLANG_TARGET}"
        "AR=${CMAKE_AR}"
        "RANLIB=${CMAKE_RANLIB}"
        "NM=${CMAKE_NM}"
        "STRIP=${CMAKE_STRIP}"
        "CFLAGS=${CMAKE_C_FLAGS}"
        "LDFLAGS=${CMAKE_SHARED_LINKER_FLAGS}"
        "CC_FOR_BUILD=gcc"
    BUILD_COMMAND make -j4
    INSTALL_COMMAND make install
)

# Define the libraries as imported targets
add_library(gpg-error SHARED IMPORTED GLOBAL)
set_target_properties(gpg-error PROPERTIES
    IMPORTED_LOCATION "${GPG_ERROR_LIB}"
    INTERFACE_INCLUDE_DIRECTORIES "${INSTALL_DIR}/include"
)
add_dependencies(gpg-error gpg_error_build)

add_library(gcrypt SHARED IMPORTED GLOBAL)
set_target_properties(gcrypt PROPERTIES
    IMPORTED_LOCATION "${GCRYPT_LIB}"
    INTERFACE_INCLUDE_DIRECTORIES "${INSTALL_DIR}/include"
)
add_dependencies(gcrypt gcrypt_build)

# --- Main project ---
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/diaryelements
    ${INSTALL_DIR}/include
)

add_library(lifeograph_core SHARED
    logid.cpp
    helpers.cpp
    diaryelements/diary.cpp
    diaryelements/entry.cpp
    diaryelements/paragraph.cpp
    diaryelements/filter.cpp
    diaryelements/diarydata.cpp
    diaryelements/tableelem.cpp
    python/code_languages.cpp
    undo.cpp
    parsers/parser_background.cpp
    parsers/parser_base.cpp
    parsers/parser_stripper.cpp
    parsers/parser_upgrader.cpp
    jni_diary.cpp
    jni_entry.cpp
    jni_paragraph.cpp
    jni_diary_element.cpp
    jni_date.cpp
    jni_chartdata.cpp
    jni_filter.cpp
    jni_str.cpp
    jni_native_bridge.cpp
)

target_link_libraries(lifeograph_core
    android
    log
    re2
    gcrypt
    gpg-error
)
