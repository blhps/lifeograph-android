#include <jni.h>
#include "diaryelements/diarydata.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

JNI_METHOD(jstring, StringDefElem_nativeGetDefinition)(JNIEnv* env, jobject, jlong ptr) {
    return env->NewStringUTF( reinterpret_cast<LoG::StringDefElem*>(ptr)->get_definition().c_str() );
}
JNI_METHOD(void, StringDefElem_nativeSetDefinition)(JNIEnv* env, jobject, jlong ptr, jstring def) {
    const char* c_def = env->GetStringUTFChars(def, nullptr);
    reinterpret_cast<LoG::StringDefElem*>(ptr)->set_definition(c_def);
    env->ReleaseStringUTFChars(def, c_def);
}

} // extern "C"
