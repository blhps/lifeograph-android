#include <jni.h>
#include <vector>
#include "diaryelements/paragraph.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

JNI_METHOD(jlong, Paragraph_nativeGetPrev)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_prev());
}
JNI_METHOD(jlong, Paragraph_nativeGetNext)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_next());
}
JNI_METHOD(jlong, Paragraph_nativeGetNextVisible)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_next_visible());
}

JNI_METHOD(jstring, Paragraph_nativeGetText)(JNIEnv* env, jobject obj, jlong ptr) {
    auto para = reinterpret_cast<LoG::Paragraph*>(ptr);
    return env->NewStringUTF(para->get_text().c_str());
}

JNI_METHOD(jint, Paragraph_nativeGetIndentLevel)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_indent_level());
}

JNI_METHOD(jint, Paragraph_nativeGetHeadingLevel)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_heading_level());
}

JNI_METHOD(jchar, Paragraph_nativeGetAlignment)(JNIEnv* env, jobject obj, jlong ptr) {
    auto result = reinterpret_cast<LoG::Paragraph*>(ptr)->get_alignment();
    return static_cast<jchar>(LoG::VT::get_v<LoG::VT::PA, char, int>(result));
}

JNI_METHOD(void, Paragraph_nativeSetAlignment)(JNIEnv* env, jobject obj, jlong ptr, jchar alignment) {
    int alignment_int = LoG::VT::get_v<LoG::VT::PA, int, char>(char(alignment));
    reinterpret_cast<LoG::Paragraph*>(ptr)->set_alignment(alignment_int);
}

JNI_METHOD(jchar, Paragraph_nativeGetListType)(JNIEnv* env, jobject obj, jlong ptr) {
    auto result = reinterpret_cast<LoG::Paragraph*>(ptr)->get_list_type();
    return static_cast<jchar>(LoG::VT::get_v<LoG::VT::PLS, char, int>(result));
}

JNI_METHOD(jlongArray, Paragraph_nativeGetFormats)(JNIEnv* env, jobject obj, jlong ptr) {
    const auto& formats = reinterpret_cast<LoG::Paragraph*>(ptr)->m_formats;
    auto size = static_cast<jsize>(formats.size());
    auto result = env->NewLongArray(size);
    std::vector<jlong> ptrs;
    ptrs.reserve(size);
    for (auto f : formats) ptrs.push_back(reinterpret_cast<jlong>(f));
    env->SetLongArrayRegion(result, 0, size, ptrs.data());
    return result;
}

// HIDDEN FORMAT METHODS ===========================================================================

JNI_METHOD(jchar, HiddenFormat_nativeGetType)(JNIEnv* env, jobject obj, jlong ptr) {
    auto result = reinterpret_cast<LoG::HiddenFormat*>(ptr)->type;
    return static_cast<jchar>(LoG::VT::get_v<LoG::VT::PLS, char, int>(result));
}

} // extern "C"
