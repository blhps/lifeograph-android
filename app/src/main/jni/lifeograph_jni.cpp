#include <jni.h>
#include <string>
#include "diaryelements/diary.hpp"
#include "diaryelements/entry.hpp"
#include "diaryelements/paragraph.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

// DIARY METHODS ===================================================================================

JNI_METHOD(jlong, Diary_nativeCreate)(JNIEnv* env, jobject obj) {
    return reinterpret_cast<jlong>(new LoG::Diary());
}

JNI_METHOD(void, Diary_nativeDestroy)(JNIEnv* env, jobject obj, jlong ptr) {
    delete reinterpret_cast<LoG::Diary*>(ptr);
}

JNI_METHOD(jint, Diary_nativeInitNew)(JNIEnv* env, jobject obj, jlong ptr, jstring path, jstring pw) {
    LoG::Diary* diary = reinterpret_cast<LoG::Diary*>(ptr);
    const char* c_path = env->GetStringUTFChars(path, nullptr);
    const char* c_pw = env->GetStringUTFChars(pw, nullptr);

    LoG::Result res = diary->init_new(c_path, c_pw ? c_pw : "");

    env->ReleaseStringUTFChars(path, c_path);
    if(c_pw) env->ReleaseStringUTFChars(pw, c_pw);

    return static_cast<jint>(res);
}

// Add more Diary methods as needed...

// ENTRY METHODS ===================================================================================

JNI_METHOD(jstring, Entry_nativeGetText)(JNIEnv* env, jobject obj, jlong ptr) {
    LoG::Entry* entry = reinterpret_cast<LoG::Entry*>(ptr);
    return env->NewStringUTF(entry->get_text().c_str());
}

// PARAGRAPH METHODS ===============================================================================

JNI_METHOD(jstring, Paragraph_nativeGetText)(JNIEnv* env, jobject obj, jlong ptr) {
    LoG::Paragraph* para = reinterpret_cast<LoG::Paragraph*>(ptr);
    return env->NewStringUTF(para->get_text().c_str());
}

} // extern "C"
