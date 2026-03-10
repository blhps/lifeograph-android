#include <jni.h>
#include <string>
#include <vector>
#include "diaryelements/diary.hpp"
#include "diaryelements/entry.hpp"
#include "diaryelements/paragraph.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

// DIARY ELEMENT METHODS ===========================================================================

JNI_METHOD(jint, DiaryElement_nativeGetType)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_type());
}

JNI_METHOD(jint, DiaryElement_nativeGetSize)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_size());
}

JNI_METHOD(jlong, DiaryElement_nativeGetId)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jlong>(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_id().get_raw());
}

JNI_METHOD(jstring, DiaryElement_nativeGetName)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::DiaryElement*>(ptr)->get_name_std().c_str());
}

JNI_METHOD(void, DiaryElement_nativeSetName)(JNIEnv* env, jobject obj, jlong ptr, jstring name) {
    const char* c_name = env->GetStringUTFChars(name, nullptr);
    reinterpret_cast<LoG::DiaryElement*>(ptr)->set_name( c_name );
    env->ReleaseStringUTFChars(name, c_name);
}

JNI_METHOD(jlongArray, Diary_nativeGetThemes)(JNIEnv* env, jobject obj, jlong ptr) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    auto themes = diary->get_p2themes();
    auto size = static_cast<jsize>(themes->size());
    jlongArray result = env->NewLongArray(size);
    std::vector<jlong> ptrs;
    ptrs.reserve(size);
    for (auto e : *themes) ptrs.push_back(reinterpret_cast<jlong>(e.second));
    env->SetLongArrayRegion(result, 0, size, ptrs.data());
    return result;
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

// DIARY METHODS ===================================================================================

JNI_METHOD(jlong, Diary_nativeCreate)(JNIEnv* env, jobject obj) {
    return reinterpret_cast<jlong>(new LoG::Diary());
}

JNI_METHOD(void, Diary_nativeDestroy)(JNIEnv* env, jobject obj, jlong ptr) {
    delete reinterpret_cast<LoG::Diary*>(ptr);
}

JNI_METHOD(jint, Diary_nativeInitNew)(JNIEnv* env, jobject obj, jlong ptr, jstring path, jstring pw) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    const char* c_path = env->GetStringUTFChars(path, nullptr);
    const char* c_pw = env->GetStringUTFChars(pw, nullptr);

    LoG::Result res = diary->init_new(c_path, c_pw ? c_pw : "");

    env->ReleaseStringUTFChars(path, c_path);
    if(c_pw) env->ReleaseStringUTFChars(pw, c_pw);

    return static_cast<jint>(res);
}

JNI_METHOD(jlong, Diary_nativeCreateNewId)(JNIEnv* env, jobject obj, jlong ptr, jobject element) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    LoG::DiaryElement* elem = nullptr;

    if (element != nullptr) {
        jclass clazz = env->GetObjectClass(element);
        jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
        if (fid != nullptr) {
            elem = reinterpret_cast<LoG::DiaryElement*>(env->GetLongField(element, fid));
        }
    }

    return static_cast<jlong>(diary->create_new_id(elem).get_raw());
}

JNI_METHOD(void, Diary_nativeClear)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Diary*>(ptr)->clear();
}

JNI_METHOD(jint, Diary_nativeSetPath)(JNIEnv* env, jobject obj, jlong ptr, jstring path, jint type) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    const char* c_path = env->GetStringUTFChars(path, nullptr);
    LoG::Result res = diary->set_path(c_path, static_cast<LoG::Diary::SetPathType>(type));
    env->ReleaseStringUTFChars(path, c_path);
    return static_cast<jint>(res);
}

JNI_METHOD(jint, Diary_nativeReadHeader)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->read_header());
}

JNI_METHOD(jint, Diary_nativeReadBody)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->read_body());
}

JNI_METHOD(jstring, Diary_nativeGetPassphrase)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Diary*>(ptr)->get_passphrase().c_str());
}

JNI_METHOD(jboolean, Diary_nativeSetPassphrase)(JNIEnv* env, jobject obj, jlong ptr, jstring pw) {
    const char* c_pw = env->GetStringUTFChars(pw, nullptr);
    bool res = reinterpret_cast<LoG::Diary*>(ptr)->set_passphrase(c_pw);
    env->ReleaseStringUTFChars(pw, c_pw);
    return static_cast<jboolean>(res);
}

JNI_METHOD(jint, Diary_nativeGetReadVersion)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<LoG::Diary*>(ptr)->get_version();
}

JNI_METHOD(jint, Diary_nativeGetSize)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->get_size());
}

JNI_METHOD(jboolean, Diary_nativeIsOld)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->is_old());
}

JNI_METHOD(jboolean, Diary_nativeIsEncrypted)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->is_encrypted());
}

JNI_METHOD(jboolean, Diary_nativeIsOpen)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->is_open());
}

JNI_METHOD(jboolean, Diary_nativeIsInEditMode)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->is_in_edit_mode());
}

JNI_METHOD(jboolean, Diary_nativeCanEnterEditMode)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->can_enter_edit_mode());
}

JNI_METHOD(jstring, Diary_nativeGetLang)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Diary*>(ptr)->get_lang().c_str());
}

JNI_METHOD(void, Diary_nativeSetLang)(JNIEnv* env, jobject obj, jlong ptr, jstring lang) {
    const char* c_lang = env->GetStringUTFChars(lang, nullptr);
    reinterpret_cast<LoG::Diary*>(ptr)->set_lang(c_lang);
    env->ReleaseStringUTFChars(lang, c_lang);
}

JNI_METHOD(jint, Diary_nativeWrite)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->write());
}

JNI_METHOD(jint, Diary_nativeWriteLock)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->write_lock());
}

JNI_METHOD(jint, Diary_nativeWriteTo)(JNIEnv* env, jobject obj, jlong ptr, jstring uri) {
    const char* c_uri = env->GetStringUTFChars(uri, nullptr);
    LoG::Result res = reinterpret_cast<LoG::Diary*>(ptr)->write(c_uri);
    env->ReleaseStringUTFChars(uri, c_uri);
    return static_cast<jint>(res);
}

JNI_METHOD(jint, Diary_nativeEnableEditing)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->enable_editing());
}

JNI_METHOD(jlong, Diary_nativeGetEntry1st)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_entry_1st());
}

JNI_METHOD(jlong, Diary_nativeGetEntryToday)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_entry_today());
}

JNI_METHOD(jlong, Diary_nativeGetEntryMostCurrent)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_entry_most_current());
}

JNI_METHOD(jlong, Diary_nativeGetEntryById)(JNIEnv* env, jobject obj, jlong ptr, jlong id) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_entry_by_id(LoG::LoGID32 (id)));
}

JNI_METHOD(jlong, Diary_nativeGetEntryByDate)(JNIEnv* env, jobject obj, jlong ptr, jlong date) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_entry_by_date(static_cast<LoG::DateV>(date)));
}

JNI_METHOD(jlongArray, Diary_nativeGetEntriesByFilter)(JNIEnv* env, jobject obj, jlong ptr, jobject filter) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    LoG::Filter* filter_ptr = nullptr;

    if (filter != nullptr) {
        jclass clazz = env->GetObjectClass(filter);
        jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
        if (fid != nullptr) {
            filter_ptr = reinterpret_cast<LoG::Filter*>(env->GetLongField(filter, fid));
        }
    }

    auto entries = diary->get_entries_by_filter(filter_ptr);
    auto size = static_cast<jsize>(entries.size());
    jlongArray result = env->NewLongArray(size);
    std::vector<jlong> ptrs;
    ptrs.reserve(size);
    for (auto e : entries) ptrs.push_back(reinterpret_cast<jlong>(e));
    env->SetLongArrayRegion(result, 0, size, ptrs.data());
    return result;
}

JNI_METHOD(jint, Diary_nativeGetEntryCountOnDay)(JNIEnv* env, jobject obj, jlong ptr, jlong date) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->get_entry_count_on_day(static_cast<LoG::DateV>(date)));
}

JNI_METHOD(void, Diary_nativeSetEntryName)(JNIEnv* env, jobject obj, jlong ptr, jobject entry, jstring name) {
    jclass clazz = env->GetObjectClass(entry);
    jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
    auto entry_ptr = reinterpret_cast<LoG::Entry*>(env->GetLongField(entry, fid));
    const char* c_name = env->GetStringUTFChars(name, nullptr);
    entry_ptr->set_text(c_name, nullptr); // Using set_text as name is derived from first para
    env->ReleaseStringUTFChars(name, c_name);
}

JNI_METHOD(void, Diary_nativeSetEntryDate)(JNIEnv* env, jobject obj, jlong ptr, jobject entry, jlong date) {
    jclass clazz = env->GetObjectClass(entry);
    jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
    auto entry_ptr = reinterpret_cast<LoG::Entry*>(env->GetLongField(entry, fid));
    reinterpret_cast<LoG::Diary*>(ptr)->set_entry_date(entry_ptr, static_cast<LoG::DateV>(date));
}

JNI_METHOD(jlong, Diary_nativeCreateEntry)(JNIEnv* env, jobject obj, jlong ptr, jobject entry_rel, jboolean fParent, jlong date, jstring content) {
    LoG::Entry* rel_ptr = nullptr;
    if (entry_rel != nullptr) {
        jclass clazz = env->GetObjectClass(entry_rel);
        jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
        rel_ptr = reinterpret_cast<LoG::Entry*>(env->GetLongField(entry_rel, fid));
    }
    const char* c_content = env->GetStringUTFChars(content, nullptr);
    auto new_entry = reinterpret_cast<LoG::Diary*>(ptr)->create_entry(rel_ptr, fParent, static_cast<LoG::DateV>(date), c_content);
    env->ReleaseStringUTFChars(content, c_content);
    return reinterpret_cast<jlong>(new_entry);
}

JNI_METHOD(jlong, Diary_nativeDismissEntry)(JNIEnv* env, jobject obj, jlong ptr, jobject entry) {
    jclass clazz = env->GetObjectClass(entry);
    jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
    auto entry_ptr = reinterpret_cast<LoG::Entry*>(env->GetLongField(entry, fid));
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->dismiss_entry(entry_ptr));
}

JNI_METHOD(jboolean, Diary_nativeRenameFilter)(JNIEnv* env, jobject obj, jlong ptr, jobject filter,
        jstring name) {
    jclass clazz = env->GetObjectClass(filter);
    jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
    auto filter_ptr = reinterpret_cast<LoG::Filter*>(env->GetLongField(filter, fid));
    const char* c_name = env->GetStringUTFChars(name, nullptr);
    unsigned char result = reinterpret_cast<LoG::Diary*>(ptr)->rename_filter(filter_ptr, c_name);
    env->ReleaseStringUTFChars(name, c_name);
    return reinterpret_cast<jboolean>(result);
}

JNI_METHOD(jlong, Diary_nativeGetCompletionTag)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_completion_tag());
}

JNI_METHOD(jlong, Diary_nativeGetElement)(JNIEnv* env, jobject obj, jlong ptr, jlong id) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_element(LoG::LoGID32(id )));
}

// ENTRY METHODS ===================================================================================
JNI_METHOD(jlong, Entry_nativeGetId)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_id().get_raw());
}

JNI_METHOD(jlong, Entry_nativeGetParent)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_parent());
}

JNI_METHOD(jint, Entry_nativeGetChildCount)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<LoG::Entry*>(ptr)->get_child_count();
}

JNI_METHOD(jlongArray, Entry_nativeGetDescendants)(JNIEnv* env, jobject obj, jlong ptr) {
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
    return reinterpret_cast<LoG::Entry*>(ptr)->get_descendant_depth();
}

JNI_METHOD(jint, Entry_nativeGetSize)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Entry*>(ptr)->get_size());
}

JNI_METHOD(jlong, Entry_nativeGetDate)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_date());
}

JNI_METHOD(void, Entry_nativeSetDate)(JNIEnv* env, jobject obj, jlong ptr, jlong date) {
    reinterpret_cast<LoG::Entry*>(ptr)->get_diary()->set_entry_date(reinterpret_cast<LoG::Entry*>(ptr), static_cast<LoG::DateV>(date));
}

JNI_METHOD(jboolean, Entry_nativeHasName)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->has_name());
}

JNI_METHOD(jstring, Entry_nativeGetName)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_name_std().c_str());
}

JNI_METHOD(void, Entry_nativeUpdateName)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Entry*>(ptr)->update_name();
}

JNI_METHOD(jboolean, Entry_nativeIsEmpty)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->is_empty());
}

JNI_METHOD(jstring, Entry_nativeGetText)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_text().c_str());
}

JNI_METHOD(void, Entry_nativeClearText)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Entry*>(ptr)->clear_text();
}

JNI_METHOD(void, Entry_nativeSetText)(JNIEnv* env, jobject obj, jlong ptr, jstring text) {
    const char* c_text = env->GetStringUTFChars(text, nullptr);
    reinterpret_cast<LoG::Entry*>(ptr)->set_text(c_text, nullptr); // TODO: pass parser if needed
    env->ReleaseStringUTFChars(text, c_text);
}

JNI_METHOD(void, Entry_nativeInsertText)(JNIEnv* env, jobject obj, jlong ptr, jint pos, jstring text) {
    const char* c_text = env->GetStringUTFChars(text, nullptr);
    reinterpret_cast<LoG::Entry*>(ptr)->insert_text(static_cast<size_t>(pos), c_text, LoG::ParaInhClass::NONE);
    env->ReleaseStringUTFChars(text, c_text);
}

JNI_METHOD(void, Entry_nativeEraseText)(JNIEnv* env, jobject obj, jlong ptr, jint pos_bgn, jint pos_end) {
    reinterpret_cast<LoG::Entry*>(ptr)->erase_text(static_cast<size_t>(pos_bgn), static_cast<size_t>(pos_end - pos_bgn), false);
}

JNI_METHOD(jlong, Entry_nativeGetParagraphAtPos)(JNIEnv* env, jobject obj, jlong ptr, jint pos) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_paragraph(static_cast<size_t>(pos)));
}

JNI_METHOD(jlong, Entry_nativeAddParagraphBefore)(JNIEnv* env, jobject obj, jlong ptr,
           jstring text, jlong ptr_para_after) {
    const char* c_text = env->GetStringUTFChars(text, nullptr);
    auto result = reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->add_paragraph_before
                    (c_text, reinterpret_cast<LoG::Paragraph*>(ptr_para_after )));
    env->ReleaseStringUTFChars(text, c_text);
    return result;
}

JNI_METHOD(jlong, Entry_nativeAddParagraphsAfter)(JNIEnv* env, jobject obj, jlong ptr,
                                                  jlong ptr_para_bgn, jlong ptr_para_after) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->add_paragraphs_after
            (reinterpret_cast<LoG::Paragraph*>(ptr_para_bgn),
             reinterpret_cast<LoG::Paragraph*>(ptr_para_after)));
}

JNI_METHOD(jstring, Entry_nativeGetDescription)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_description().c_str());
}

JNI_METHOD(jboolean, Entry_nativeIsFavorite)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->is_favorite());
}

JNI_METHOD(void, Entry_nativeToggleFavorite)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Entry*>(ptr)->toggle_favored();
}

JNI_METHOD(jstring, Entry_nativeGetLangFinal)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Entry*>(ptr)->get_lang_final().c_str());
}

JNI_METHOD(jboolean, Entry_nativeIsTrashed)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->is_trashed());
}

JNI_METHOD(void, Entry_nativeSetTrashed)(JNIEnv* env, jobject obj, jlong ptr, jboolean trashed) {
    reinterpret_cast<LoG::Entry*>(ptr)->set_trashed(trashed);
}

JNI_METHOD(jboolean, Entry_nativeHasTag)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->has_tag(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(jboolean, Entry_nativeHasTagBroad)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->has_tag_broad(reinterpret_cast<LoG::Entry*>(tagPtr), true));
}

JNI_METHOD(jdouble, Entry_nativeGetTagValue)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr, jboolean average) {
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_tag_value(reinterpret_cast<LoG::Entry*>(tagPtr), average));
}

JNI_METHOD(jdouble, Entry_nativeGetTagValuePlanned)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr, jboolean average) {
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_tag_value_planned(reinterpret_cast<LoG::Entry*>(tagPtr), average));
}

JNI_METHOD(jdouble, Entry_nativeGetTagValueRemaining)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr, jboolean average) {
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_tag_value_remaining(reinterpret_cast<LoG::Entry*>(tagPtr), average));
}

JNI_METHOD(jlong, Entry_nativeGetSubTagFirst)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_sub_tag_first(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(jlong, Entry_nativeGetSubTagLast)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_sub_tag_last(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(jlong, Entry_nativeGetSubTagLowest)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_sub_tag_lowest(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(jlong, Entry_nativeGetSubTagHighest)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_sub_tag_highest(reinterpret_cast<LoG::Entry*>(tagPtr)));
}

JNI_METHOD(void, Entry_nativeAddTag)(JNIEnv* env, jobject obj, jlong ptr, jlong tagPtr, jdouble value) {
    reinterpret_cast<LoG::Entry*>(ptr)->add_tag(reinterpret_cast<LoG::Entry*>(tagPtr), value);
}

JNI_METHOD(jlong, Entry_nativeGetTheme)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_theme());
}

JNI_METHOD(void, Entry_nativeSetTheme)(JNIEnv* env, jobject obj, jlong ptr, jlong themePtr) {
    reinterpret_cast<LoG::Entry*>(ptr)->set_theme(reinterpret_cast<LoG::Theme*>(themePtr));
}

JNI_METHOD(jboolean, Entry_nativeIsThemeSet)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->is_theme_set());
}

JNI_METHOD(jboolean, Entry_nativeUpdateTodoStatus)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->update_todo_status());
}

JNI_METHOD(jdouble, Entry_nativeGetCompletion)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_completion());
}

JNI_METHOD(jdouble, Entry_nativeGetCompleted)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_completed());
}

JNI_METHOD(jdouble, Entry_nativeGetWorkload)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_workload());
}

JNI_METHOD(jboolean, Entry_nativeHasLocation)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Entry*>(ptr)->has_location());
}

JNI_METHOD(jdouble, Entry_nativeGetMapPathLength)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jdouble>(reinterpret_cast<LoG::Entry*>(ptr)->get_map_path_length());
}

JNI_METHOD(jlong, Entry_nativeGetNextStraight)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Entry*>(ptr)->get_next_straight());
}

// PARAGRAPH METHODS ===============================================================================

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
    int alignment_int = LoG::VT::get_v<LoG::VT::PA, int, char>(alignment);
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

} // extern "C"
