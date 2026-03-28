#include <jni.h>
#include "helpers.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL \
Java_net_sourceforge_lifeograph_helpers_STR_##name

extern "C" {

JNI_METHOD(jboolean, nativeIsCharSpace)(JNIEnv*, jclass, jchar c) {
    return static_cast<jboolean>( HELPERS::STR::is_char_space( HELPERS::Wchar(c) ) );
}

JNI_METHOD(jboolean, nativeIsCharName)(JNIEnv*, jclass, jchar c) {
    return static_cast<jboolean>( HELPERS::STR::is_char_name( HELPERS::Wchar(c) ) );
}

} // extern "C"
