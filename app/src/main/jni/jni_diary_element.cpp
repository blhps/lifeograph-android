#include <jni.h>
#include "diaryelements/diarydata.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

JNI_METHOD(jint, DiaryElement_nativeGetType)(JNIEnv*, jobject, jlong ptr) {
    if (ptr == 0) return 0;
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_type());
}

JNI_METHOD(jint, DiaryElement_nativeGetSize)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return 0;
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_size());
}

JNI_METHOD(jint, DiaryElement_nativeGetId)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return 0;
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_id().get_raw());
}

JNI_METHOD(jstring, DiaryElement_nativeGetName)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_name_std().c_str());
}

JNI_METHOD(void, DiaryElement_nativeSetName)(JNIEnv* env, jobject obj, jlong ptr, jstring name) {
    if (ptr == 0) return;
    const char* c_name = env->GetStringUTFChars(name, nullptr);
    reinterpret_cast<LoG::DiaryElement*>(ptr)->set_name( c_name );
    env->ReleaseStringUTFChars(name, c_name);
}

// DIARYELEMTAG METHODS ============================================================================
JNI_METHOD(jlong, DiaryElemTag_nativeGetDate)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return 0;
    return static_cast<jlong>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_date());
}

JNI_METHOD(jlong, DiaryElemTag_nativeGetDateEdited)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return 0;
    return static_cast<jlong>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_date_edited());
}

JNI_METHOD(jboolean, DiaryElemTag_nativeIsExpanded)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return false;
    return static_cast<jboolean>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->is_expanded());
}

JNI_METHOD(void, DiaryElemTag_nativeSetExpanded)(JNIEnv* env, jobject obj, jlong ptr,
           jboolean flag_expanded) {
    if (ptr == 0) return;
    reinterpret_cast<LoG::DiaryElemTag*>(ptr)->set_expanded(flag_expanded);
}

JNI_METHOD(jint, DiaryElemTag_nativeGetTodoStatus)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return 0;
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_todo_status());
}

JNI_METHOD(jint, DiaryElemTag_nativeGetTodoStatusEffective)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return 0;
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_todo_status_effective());
}

JNI_METHOD(void, DiaryElemTag_nativeSetTodoStatus)(JNIEnv* env, jobject obj, jlong ptr,
                                                   jint status) {
    if (ptr == 0) return;
    reinterpret_cast<LoG::DiaryElemTag*>(ptr)->set_todo_status(status);
}

JNI_METHOD(jstring, DiaryElemTag_nativeGetUnit)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::DiaryElemTag*>(ptr)->get_unit().c_str());
}

// THEME METHODS ===================================================================================
JNI_METHOD(jboolean, Theme_nativeIsSystem)(JNIEnv*, jobject, jlong ptr) {
    if (ptr == 0) return true;
    return static_cast<jboolean>(reinterpret_cast<LoG::Theme*>(ptr)->is_system());
}

JNI_METHOD(void, Theme_nativeCopyTo)(JNIEnv*, jobject, jlong ptr, jlong ptr_theme) {
    if (ptr == 0 || ptr_theme == 0) return;
    auto p2theme_other = reinterpret_cast<LoG::Theme*>(ptr_theme);
    reinterpret_cast<LoG::Theme*>(ptr)->copy_to(p2theme_other);
}

JNI_METHOD(jstring, Theme_nativeGetColorBase)(JNIEnv* env, jobject, jlong ptr) {
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_base;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorText)(JNIEnv* env, jobject, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_text;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorTitle)(JNIEnv* env, jobject, jlong ptr) {
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_title;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorHeadingL)(JNIEnv* env, jobject, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_heading_L;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorHighlight)(JNIEnv* env, jobject, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_highlight;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorHeadingM)(JNIEnv* env, jobject, jlong ptr) {
    //if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_heading_M;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorMid)(JNIEnv* env, jobject, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_mid;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorMatchBG)(JNIEnv* env, jobject, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_match_bg;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorInlineTag)(JNIEnv* env, jobject, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_inline_tag;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorOpen)(JNIEnv* env, jobject, jlong ptr) {
    //if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_open;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorDone)(JNIEnv* env, jobject, jlong ptr) {
    //if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_done;
    return env->NewStringUTF(color.to_string().c_str());
}
JNI_METHOD(jstring, Theme_nativeGetColorDoneBG)(JNIEnv* env, jobject, jlong ptr) {
    //if (ptr == 0) return env->NewStringUTF("#000000");
    auto color = reinterpret_cast<LoG::Theme*>(ptr)->color_done_bg;
    return env->NewStringUTF(color.to_string().c_str());
}

JNI_METHOD(void, Theme_nativeSetColorBase)(JNIEnv* env, jobject, jlong ptr, jstring color) {
    if (ptr == 0) return;
    const char* c_color = env->GetStringUTFChars(color, nullptr);
    reinterpret_cast<LoG::Theme*>(ptr)->color_base = LoG::Color( c_color );
    env->ReleaseStringUTFChars(color, c_color);
}
JNI_METHOD(void, Theme_nativeSetColorText)(JNIEnv* env, jobject, jlong ptr, jstring color) {
    if (ptr == 0) return;
    const char* c_color = env->GetStringUTFChars(color, nullptr);
    reinterpret_cast<LoG::Theme*>(ptr)->color_text = LoG::Color( c_color );
    env->ReleaseStringUTFChars(color, c_color);
}
JNI_METHOD(void, Theme_nativeSetColorTitle)(JNIEnv* env, jobject, jlong ptr, jstring color) {
    if (ptr == 0) return;
    const char* c_color = env->GetStringUTFChars(color, nullptr);
    reinterpret_cast<LoG::Theme*>(ptr)->color_title = LoG::Color( c_color );
    env->ReleaseStringUTFChars(color, c_color);
}
JNI_METHOD(void, Theme_nativeSetColorHeadingL)(JNIEnv* env, jobject, jlong ptr, jstring color) {
    if (ptr == 0) return;
    const char* c_color = env->GetStringUTFChars(color, nullptr);
    reinterpret_cast<LoG::Theme*>(ptr)->color_heading_L = LoG::Color( c_color );
    env->ReleaseStringUTFChars(color, c_color);
}
JNI_METHOD(void, Theme_nativeSetColorHighlight)(JNIEnv* env, jobject, jlong ptr, jstring color) {
    if (ptr == 0) return;
    const char* c_color = env->GetStringUTFChars(color, nullptr);
    reinterpret_cast<LoG::Theme*>(ptr)->color_highlight = LoG::Color( c_color );
    env->ReleaseStringUTFChars(color, c_color);
}
} // extern "C"
