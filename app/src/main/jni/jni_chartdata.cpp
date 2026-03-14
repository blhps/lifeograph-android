#include <jni.h>
#include "diaryelements/diarydata.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

JNI_METHOD(jlong, ChartData_nativeCreate)(JNIEnv*, jobject, jlong ptr_diary) {
    auto p2diary = reinterpret_cast<LoG::Diary*>(ptr_diary);
    return reinterpret_cast<jlong>(new LoG::ChartData(p2diary));
}
JNI_METHOD(void, ChartData_nativeDestroy)(JNIEnv*, jobject, jlong ptr) {
    delete reinterpret_cast<LoG::ChartData*>(ptr);
}


JNI_METHOD(void, ChartData_nativeClear)(JNIEnv*, jobject, jlong ptr) {
     reinterpret_cast<LoG::ChartData*>(ptr)->clear();
}

JNI_METHOD(void, ChartData_nativeCalculatePoints)(JNIEnv*, jobject, jlong ptr) {
    reinterpret_cast<LoG::ChartData*>(ptr)->calculate_points();
}

JNI_METHOD(void, ChartData_nativeSetFromString)(JNIEnv* env, jobject, jlong ptr, jstring str) {
    const char* c_str = env->GetStringUTFChars(str, nullptr);
    reinterpret_cast<LoG::ChartData*>(ptr)->set_from_string( c_str );
    env->ReleaseStringUTFChars(str, c_str);
}

JNI_METHOD(jint, ChartData_nativeGetSpan)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jint>( reinterpret_cast<LoG::ChartData*>(ptr)->m_span );
}

JNI_METHOD(jint, ChartData_nativeGetPeriod)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jint>( reinterpret_cast<LoG::ChartData*>(ptr)->get_period() );
}

JNI_METHOD(jstring, ChartData_nativeGetUnit)(JNIEnv* env, jobject, jlong ptr) {
    return env->NewStringUTF( reinterpret_cast<LoG::ChartData*>(ptr)->m_unit.c_str() );
}

} // extern "C"
