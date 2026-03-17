#include <jni.h>
#include <string>
#include <vector>
#include "diaryelements/diary.hpp"
#include "diaryelements/entry.hpp"
#include "diaryelements/paragraph.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

// Safety macro to prevent crashes on null pointers (common during slow emulator boots)
#define CHECK_PTR(ptr, return_value) if (ptr == 0) return return_value;

extern "C" {

JNI_METHOD(jlong, Entry_nativeGetId)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return static_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_id().get_raw());
}

JNI_METHOD(jlong, Entry_nativeGetParent)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_parent());
}

JNI_METHOD(jint, Entry_nativeGetGeneration)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jint>(reinterpret_cast<LoG::Entry*>(ptr)->get_generation());
}

JNI_METHOD(jlong, Entry_nativeGetNext)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_next());
}

JNI_METHOD(jlong, Entry_nativeGetNextStraight)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_next_straight());
}

JNI_METHOD(jlong, Entry_nativeGetChild1st)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_child_1st());
}

JNI_METHOD(jint, Entry_nativeGetChildCount)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<LoG::Entry*>(ptr)->get_child_count();
}

JNI_METHOD(jlongArray, Entry_nativeGetDescendants)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return env->NewLongArray(0);
    auto descendants = reinterpret_cast<LoG::Entry*>(ptr)->get_descendants();
    auto size = static_cast<jsize>(descendants.size());
    auto result = env->NewLongArray(size);
    std::vector<jlong> ptrs;
    ptrs.reserve(size);
    for (auto e : descendants) ptrs.push_back(reinterpret_cast<jlong>(e));
    env->SetLongArrayRegion(result, 0, size, ptrs.data());
    return result;
}

JNI_METHOD(jint, Entry_nativeGetDescendantDepth)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<LoG::Entry*>(ptr)->get_descendant_depth();
}

JNI_METHOD(jint, Entry_nativeGetSize)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return static_cast<jint>(reinterpret_cast<LoG::Entry*>(ptr)->get_size());
}

JNI_METHOD(jlong, Entry_nativeGetDate)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return static_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_date());
}

JNI_METHOD(void, Entry_nativeSetDate)(JNIEnv* env, jobject obj, jlong ptr, jlong date) {
    CHECK_PTR(ptr, )
    reinterpret_cast<LoG::Entry*>(ptr)->get_diary()->set_entry_date(reinterpret_cast<LoG::Entry*>(ptr), static_cast<LoG::DateV>(date));
}

JNI_METHOD(jboolean, Entry_nativeHasName)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->has_name());
}

JNI_METHOD(jstring, Entry_nativeGetName)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_name_std().c_str());
}

JNI_METHOD(jstring, Entry_nativeGetListStr)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_list_str().c_str());
}

JNI_METHOD(jstring, Entry_nativeGetInfoStr)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_info_str().c_str());
}

JNI_METHOD(jboolean, Entry_nativeIsFilteredOut)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return reinterpret_cast<LoG::Entry*>(ptr)->is_filtered_out();
}

JNI_METHOD(void, Entry_nativeUpdateName)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, )
    reinterpret_cast<LoG::Entry*>(ptr)->update_name();
}

JNI_METHOD(jboolean, Entry_nativeIsEmpty)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, JNI_TRUE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->is_empty());
}

JNI_METHOD(jstring, Entry_nativeGetText)(JNIEnv* env, jobject, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_text().c_str());
}
JNI_METHOD(jstring, Entry_nativeGetTextVisible)(JNIEnv* env, jobject, jlong ptr) {
    //if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_text_visible().c_str());
}

JNI_METHOD(void, Entry_nativeClearText)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, )
    reinterpret_cast<LoG::Entry*>(ptr)->clear_text();
}

JNI_METHOD(void, Entry_nativeSetText)(JNIEnv* env, jobject obj, jlong ptr, jstring text) {
    CHECK_PTR(ptr, )
    const char* c_text = env->GetStringUTFChars(text, nullptr);
    reinterpret_cast<LoG::Entry*>(ptr)->set_text(c_text, nullptr);
    env->ReleaseStringUTFChars(text, c_text);
}

JNI_METHOD(void, Entry_nativeInsertText)(JNIEnv* env, jobject obj, jlong ptr, jint pos, jstring text) {
    CHECK_PTR(ptr, )
    const char* c_text = env->GetStringUTFChars(text, nullptr);
    reinterpret_cast<LoG::Entry*>(ptr)->insert_text(static_cast<size_t>(pos), c_text, LoG::ParaInhClass::NONE);
    env->ReleaseStringUTFChars(text, c_text);
}

JNI_METHOD(void, Entry_nativeEraseText)(JNIEnv* env, jobject obj, jlong ptr, jint pos_bgn, jint pos_end) {
    CHECK_PTR(ptr, )
    reinterpret_cast<LoG::Entry*>(ptr)->erase_text(static_cast<size_t>(pos_bgn), static_cast<size_t>(pos_end - pos_bgn), false);
}

JNI_METHOD(jlong, Entry_nativeGetParagraphAtPos)(JNIEnv* env, jobject obj, jlong ptr, jint pos,
        jboolean flag_visible_only) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_paragraph( size_t(pos),
                                                                                      flag_visible_only));
}

JNI_METHOD(jlong, Entry_nativeGetParagraph1st)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_paragraph_1st());
}

JNI_METHOD(jlong, Entry_nativeGetParagraphLast)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_paragraph_last());
}

JNI_METHOD(jlong, Entry_nativeAddParagraphBefore)(JNIEnv* env, jobject obj, jlong ptr, jstring text, jlong ptr_para_after) {
    CHECK_PTR(ptr, 0)
    const char* c_text = env->GetStringUTFChars(text, nullptr);
    auto result = reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->add_paragraph_before(c_text, reinterpret_cast<LoG::Paragraph*>(ptr_para_after)));
    env->ReleaseStringUTFChars(text, c_text);
    return result;
}

JNI_METHOD(jlong, Entry_nativeAddParagraphsAfter)(JNIEnv* env, jobject obj, jlong ptr, jlong ptr_para_bgn, jlong ptr_para_after) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->add_paragraphs_after(reinterpret_cast<LoG::Paragraph*>(ptr_para_bgn), reinterpret_cast<LoG::Paragraph*>(ptr_para_after)));
}

JNI_METHOD(jstring, Entry_nativeGetDescription)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_description().c_str());
}

JNI_METHOD(jboolean, Entry_nativeIsFavorite)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->is_favorite());
}

JNI_METHOD(void, Entry_nativeToggleFavorite)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, )
    reinterpret_cast<LoG::Entry*>(ptr)->toggle_favored();
}

JNI_METHOD(jstring, Entry_nativeGetLangFinal)(JNIEnv* env, jobject obj, jlong ptr) {
    if (ptr == 0) return env->NewStringUTF("");
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_lang_final().c_str());
}

JNI_METHOD(jboolean, Entry_nativeIsTrashed)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->is_trashed());
}

JNI_METHOD(void, Entry_nativeSetTrashed)(JNIEnv* env, jobject obj, jlong ptr, jboolean trashed) {
    CHECK_PTR(ptr, )
    reinterpret_cast<LoG::Entry*>(ptr)->set_trashed(trashed);
}

JNI_METHOD(jboolean, Entry_nativeHasTag)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->has_tag(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(jboolean, Entry_nativeHasTagBroad)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->has_tag_broad(reinterpret_cast<LoG::Entry*>(tagPtr), true));
}

JNI_METHOD(jdouble, Entry_nativeGetTagValue)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr, jboolean average) {
    CHECK_PTR(ptr, 0.0)
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_tag_value(reinterpret_cast<LoG::Entry*>(tagPtr), average));
}

JNI_METHOD(jdouble, Entry_nativeGetTagValuePlanned)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr, jboolean average) {
    CHECK_PTR(ptr, 0.0)
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_tag_value_planned(reinterpret_cast<LoG::Entry*>(tagPtr), average));
}

JNI_METHOD(jdouble, Entry_nativeGetTagValueRemaining)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr, jboolean average) {
    CHECK_PTR(ptr, 0.0)
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_tag_value_remaining(reinterpret_cast<LoG::Entry*>(tagPtr), average));
}

JNI_METHOD(jlong, Entry_nativeGetSubTagFirst)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_sub_tag_first(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(jlong, Entry_nativeGetSubTagLast)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_sub_tag_last(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(jlong, Entry_nativeGetSubTagLowest)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_sub_tag_lowest(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(jlong, Entry_nativeGetSubTagHighest)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_sub_tag_highest(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(void, Entry_nativeAddTag)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr, jdouble value) {
    CHECK_PTR(ptr, )
    reinterpret_cast<LoG::Entry*>(ptr)->add_tag(reinterpret_cast<LoG::Entry*>(tagPtr), value);
}

JNI_METHOD(jlong, Entry_nativeGetTheme)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0)
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_theme());
}

JNI_METHOD(void, Entry_nativeSetTheme)(JNIEnv* env, jobject obj, jlong ptr, jlong themePtr) {
    CHECK_PTR(ptr, )
    reinterpret_cast<LoG::Entry*>(ptr)->set_theme(reinterpret_cast<LoG::Theme*>(themePtr));
}

JNI_METHOD(jboolean, Entry_nativeIsThemeSet)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->is_theme_set());
}

JNI_METHOD(jboolean, Entry_nativeUpdateTodoStatus)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->update_todo_status());
}

JNI_METHOD(jdouble, Entry_nativeGetCompletion)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0.0)
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_completion());
}

JNI_METHOD(jdouble, Entry_nativeGetCompleted)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0.0)
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_completed());
}

JNI_METHOD(jdouble, Entry_nativeGetWorkload)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0.0)
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_workload());
}

JNI_METHOD(jboolean, Entry_nativeHasLocation)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, JNI_FALSE)
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->has_location());
}

JNI_METHOD(jdouble, Entry_nativeGetMapPathLength)(JNIEnv* env, jobject obj, jlong ptr) {
    CHECK_PTR(ptr, 0.0)
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_map_path_length());
}

} // extern "C"
