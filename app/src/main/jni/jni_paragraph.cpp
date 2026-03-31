#include <jni.h>
#include <vector>
#include "diaryelements/paragraph.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

#define CHECK_PTR(ptr, return_value) if (ptr == 0) return return_value;

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

JNI_METHOD(jlong, Paragraph_nativeGetHost)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Paragraph*>(ptr)->m_host);
}

JNI_METHOD(jboolean, Paragraph_nativeIsEmpty)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Paragraph*>(ptr)->is_empty());
}

JNI_METHOD(jstring, Paragraph_nativeGetText)(JNIEnv* env, jobject obj, jlong ptr) {
    auto para = reinterpret_cast<LoG::Paragraph*>(ptr);
    return env->NewStringUTF(para->get_text().c_str());
}

JNI_METHOD(jint, Paragraph_nativeGetIndentLevel)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_indent_level());
}
JNI_METHOD(void, Paragraph_nativeSetIndentLevel)(JNIEnv*, jobject, jlong ptr, jint level) {
    reinterpret_cast<LoG::Paragraph*>(ptr)->set_indent_level( int( level ) );
}

JNI_METHOD(jboolean, Paragraph_nativeIsTitle)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Paragraph*>(ptr)->is_title());
}

JNI_METHOD(jchar, Paragraph_nativeGetHeadingLevel)(JNIEnv* env, jobject obj, jlong ptr) {
    int result = reinterpret_cast<LoG::Paragraph*>(ptr)->get_heading_level();
    return static_cast<jchar>(LoG::VT::get_v<LoG::VT::PHS, char, int>(result));
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
JNI_METHOD(void, Paragraph_nativeSetListType)(JNIEnv*, jobject, jlong ptr, jchar type) {
    int type_int = LoG::VT::get_v<LoG::VT::PLS, int, char>(char(type));
    reinterpret_cast<LoG::Paragraph*>(ptr)->set_para_type2(type_int);
}


JNI_METHOD(jstring, Paragraph_nativeGetListOrderStr)(JNIEnv* env, jobject obj, jlong ptr) {
    auto para = reinterpret_cast<LoG::Paragraph*>(ptr);
    return env->NewStringUTF(para->get_list_order_str().c_str());
}

JNI_METHOD(jchar, Paragraph_nativeGetQuotType)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jchar>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_quot_type());
}

JNI_METHOD(void, Paragraph_nativeSetQuotType)(JNIEnv* env, jobject obj, jlong ptr, jchar qt) {
    reinterpret_cast<LoG::Paragraph*>(ptr)->set_quot_type(char(qt));
}

JNI_METHOD(jboolean, Paragraph_nativeIsQuote)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Paragraph*>(ptr)->is_quote());
}

JNI_METHOD(jboolean, Paragraph_nativeIsCode)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Paragraph*>(ptr)->is_code());
}

JNI_METHOD(jint, Paragraph_nativeGetBgnOffsetInHost)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_bgn_offset_in_host());
}

JNI_METHOD(jint, Paragraph_nativeGetEndOffsetInHost)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Paragraph*>(ptr)->get_end_offset_in_host());
}

JNI_METHOD(jlong, Paragraph_nativeGetFormatAt)(JNIEnv* env, jobject obj, jlong ptr, jchar type,
        jint pos) {
    const int type_i = LoG::VT::get_v< LoG::VT::FMT, int, char >( char(type) );
    auto format = reinterpret_cast<LoG::Paragraph*>(ptr)->get_format_at(
            type_i, unsigned( pos ) );
    return reinterpret_cast<jlong>( format );
}
JNI_METHOD(jlong, Paragraph_nativeGetFormatOneOfAt)(JNIEnv* env, jobject obj, jlong ptr,
        jchar type, jint pos) {
    const int type_i = LoG::VT::get_v< LoG::VT::FMT, int, char >( char(type) );
    auto format = reinterpret_cast<LoG::Paragraph*>(ptr)->get_format_oneof_at(
            type_i, unsigned( pos ) );
    return reinterpret_cast<jlong>( format );
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

JNI_METHOD(void, Paragraph_nativeToggleFormat)(JNIEnv* env, jobject obj, jlong ptr, jchar type,
                                               jint pos_bgn, jint pos_end, jboolean fAlready) {
    const int type_i = LoG::VT::get_v< LoG::VT::FMT, int, char >( char(type) );
    reinterpret_cast<LoG::Paragraph*>(ptr)->toggle_format(type_i,
                                                          unsigned(pos_bgn),
                                                          unsigned(pos_end),
                                                          fAlready);
}

// HIDDEN FORMAT METHODS ===========================================================================

JNI_METHOD(jchar, HiddenFormat_nativeGetType)(JNIEnv* env, jobject obj, jlong ptr) {
    auto result = reinterpret_cast<LoG::HiddenFormat*>(ptr)->type;
    return static_cast<jchar>(LoG::VT::get_v<LoG::VT::FMT, char, int>(result));
}
JNI_METHOD(void, HiddenFormat_nativeSetType)(JNIEnv* env, jobject obj, jlong ptr, jchar type_ch) {
    auto type = LoG::VT::get_v<LoG::VT::PLS, int, char>(char(type_ch));
    reinterpret_cast<LoG::HiddenFormat*>(ptr)->type = type;
}

JNI_METHOD(jstring, HiddenFormat_nativeGetUri)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::HiddenFormat*>(ptr)->uri.c_str());
}
JNI_METHOD(void, HiddenFormat_nativeSetUri)(JNIEnv* env, jobject obj, jlong ptr, jstring uri) {
    const char* c_uri = env->GetStringUTFChars(uri, nullptr);
    reinterpret_cast<LoG::HiddenFormat*>(ptr)->uri = c_uri;
    env->ReleaseStringUTFChars(uri, c_uri);
}
JNI_METHOD(jint, HiddenFormat_nativeGetPosBgn)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jint>( int( reinterpret_cast<LoG::HiddenFormat*>(ptr)->pos_bgn ) );
}
JNI_METHOD(void, HiddenFormat_nativeSetPosBgn)(JNIEnv* env, jobject obj, jlong ptr, jint pos) {
    reinterpret_cast<LoG::HiddenFormat*>(ptr)->pos_bgn = int(pos);
}

JNI_METHOD(jint, HiddenFormat_nativeGetPosEnd)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>( reinterpret_cast<LoG::HiddenFormat*>(ptr)->pos_end );
}
JNI_METHOD(void, HiddenFormat_nativeSetPosEnd)(JNIEnv* env, jobject obj, jlong ptr, jint pos) {
    reinterpret_cast<LoG::HiddenFormat*>(ptr)->pos_end = pos;
}

JNI_METHOD(jlong, HiddenFormat_nativeGetRefId)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jlong>( reinterpret_cast<LoG::HiddenFormat*>(ptr)->ref_id );
}
JNI_METHOD(void, HiddenFormat_nativeSetRefId)(JNIEnv* env, jobject obj, jlong ptr, jlong id) {
    reinterpret_cast<LoG::HiddenFormat*>(ptr)->ref_id = id;
}

JNI_METHOD(jint, HiddenFormat_nativeGetIdLo)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::HiddenFormat*>(ptr)->get_id_lo().get_raw());
}

JNI_METHOD(jint, HiddenFormat_nativeGetIdHi)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::HiddenFormat*>(ptr)->get_id_hi().get_raw());
}

JNI_METHOD(jlong, HiddenFormat_nativeGetVarI)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jlong>( reinterpret_cast<LoG::HiddenFormat*>(ptr)->var_i );
}
JNI_METHOD(void, HiddenFormat_nativeSetVarI)(JNIEnv* env, jobject obj, jlong ptr, jlong var_i) {
    reinterpret_cast<LoG::HiddenFormat*>(ptr)->var_i = var_i;
}

JNI_METHOD(jlong, HiddenFormat_nativeGetVarD)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jlong>( reinterpret_cast<LoG::HiddenFormat*>(ptr)->var_d );
}
JNI_METHOD(void, HiddenFormat_nativeSetVarD)(JNIEnv* env, jobject obj, jlong ptr, jlong var_date) {
    reinterpret_cast<LoG::HiddenFormat*>(ptr)->var_d = var_date;
}

} // extern "C"
