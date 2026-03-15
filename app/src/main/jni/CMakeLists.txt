cmake_minimum_required(VERSION 3.22.1)

project(lifeograph_core)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Define ANDROID macro
add_definitions(-D__ANDROID__)

include(FetchContent)

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

# Restore install rules if needed for the main project (though not used here)
set(CMAKE_SKIP_INSTALL_RULES OFF)

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/diaryelements
)

# Prebuilt libraries paths
set(LIBS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../libs/${ANDROID_ABI})

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
    lifeocrypt.c
    parsers/parser_background.cpp
    parsers/parser_base.cpp
    parsers/parser_stripper.cpp
    parsers/parser_upgrader.cpp
    helpers.c
    # JNI Wrappers
    jni_diary.cpp
    jni_entry.cpp
    jni_paragraph.cpp
    jni_diary_element.cpp
    jni_date.cpp
    jni_chartdata.cpp
    jni_filter.cpp
    jni_native_bridge.cpp
)

# Link against NDK libraries, prebuilts and RE2
target_link_libraries(lifeograph_core
    android
    log
    re2
    ${LIBS_DIR}/libgcrypt.so
    ${LIBS_DIR}/libgpg-error.so
)
