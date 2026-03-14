#include <jni.h>
#include <string>
#include <vector>
#include "diaryelements/filter.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

//JNI_METHOD(void, Filter_nativeDestroy)(JNIEnv* env, jobject obj, jlong ptr) {
//    delete reinterpret_cast<LoG::FiltererContainer*>(ptr);
//}

JNI_METHOD(jlong, Filter_nativeGetFiltererStack)(JNIEnv* env, jobject, jlong ptr) {
    return reinterpret_cast<jlong>( reinterpret_cast<LoG::Filter*>(ptr)->get_filterer_stack() );
}

JNI_METHOD(jstring, FiltererContainer_nativeGetAsString)(JNIEnv* env, jobject obj, jlong ptr) {
    Glib::ustring str;
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->get_as_string(str);
    return env->NewStringUTF( str.c_str());
}

JNI_METHOD(void , FiltererContainer_nativeSetFromString)(JNIEnv* env, jobject, jlong ptr, jstring str) {
    const char* c_str = env->GetStringUTFChars(str, nullptr);
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->set_from_string( c_str );
    env->ReleaseStringUTFChars(str, c_str);
}

} // extern "C"
