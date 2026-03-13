#include <jni.h>
#include "diaryelements/diary.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

JNI_METHOD(jint, DiaryElement_nativeGetType)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_type());
}

JNI_METHOD(jint, DiaryElement_nativeGetSize)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_size());
}

JNI_METHOD(jint, DiaryElement_nativeGetId)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_id().get_raw());
}

JNI_METHOD(jstring, DiaryElement_nativeGetName)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_name_std().c_str());
}

JNI_METHOD(void, DiaryElement_nativeSetName)(JNIEnv* env, jobject obj, jlong ptr, jstring name) {
    const char* c_name = env->GetStringUTFChars(name, nullptr);
    reinterpret_cast<LoG::DiaryElement*>(ptr)->set_name( c_name );
    env->ReleaseStringUTFChars(name, c_name);
}

// DIARYELEMTAG METHODS ============================================================================
JNI_METHOD(jlong, DiaryElemTag_nativeGetDate)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jlong>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_date());
}

JNI_METHOD(jlong, DiaryElemTag_nativeGetDateEdited)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jlong>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_date_edited());
}

JNI_METHOD(jboolean, DiaryElemTag_nativeIsExpanded)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->is_expanded());
}

JNI_METHOD(void, DiaryElemTag_nativeSetExpanded)(JNIEnv* env, jobject obj, jlong ptr,
           jboolean flag_expanded) {
    reinterpret_cast<LoG::DiaryElemTag*>(ptr)->set_expanded(flag_expanded);
}

JNI_METHOD(jint, DiaryElemTag_nativeGetTodoStatus)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_todo_status());
}

JNI_METHOD(jint, DiaryElemTag_nativeGetTodoStatusEffective)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_todo_status_effective());
}

JNI_METHOD(void, DiaryElemTag_nativeSetTodoStatus)(JNIEnv* env, jobject obj, jlong ptr,
                                                   jint status) {
    reinterpret_cast<LoG::DiaryElemTag*>(ptr)->set_todo_status(status);
}

JNI_METHOD(jstring, DiaryElemTag_nativeGetUnit)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_unit().c_str());
}

} // extern "C"
