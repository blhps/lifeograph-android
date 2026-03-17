#include <jni.h>
#include <string>
#include <vector>
#include "diaryelements/diary.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

JNI_METHOD(jboolean, Diary_initCipher)(JNIEnv* env, jclass) {
    return static_cast<jboolean>(HELPERS::Cipher::init());
}

JNI_METHOD(jlong, Diary_nativeCreate)(JNIEnv* env, jclass) {
    return reinterpret_cast<jlong>(new LoG::Diary());
}

JNI_METHOD(void, Diary_nativeDestroy)(JNIEnv* env, jobject obj, jlong ptr) {
    delete reinterpret_cast<LoG::Diary*>(ptr);
}

JNI_METHOD(void, Diary_nativeCreateMain)(JNIEnv* env, jclass) {
    LoG::Diary::d = new LoG::Diary();
}

JNI_METHOD(jlong, Diary_nativeGetMain)(JNIEnv* env, jclass) {
    return reinterpret_cast<jlong>(LoG::Diary::d);
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

JNI_METHOD(jint, Diary_nativeCreateNewId)(JNIEnv* env, jobject obj, jlong ptr, jobject element) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    LoG::DiaryElement* elem = nullptr;

    if (element != nullptr) {
        jclass clazz = env->GetObjectClass(element);
        jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
        if (fid != nullptr) {
            elem = reinterpret_cast<LoG::DiaryElement*>(env->GetLongField(element, fid));
        }
    }

    return static_cast<jint>(diary->create_new_id(elem).get_raw());
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

JNI_METHOD(jint, Diary_nativeReadHeader)(JNIEnv* env, jobject obj, jlong ptr, jbyteArray data,
        jstring uri) {
    jbyte* bufferPtr = env->GetByteArrayElements(data, nullptr);
    jsize size = env->GetArrayLength(data);
    const char* uriStr = env->GetStringUTFChars(uri, nullptr);

    // Access your Diary instance
    LoG::Result result = reinterpret_cast<LoG::Diary*>(ptr)->read_header((char*)bufferPtr, size, uriStr);

    env->ReleaseByteArrayElements(data, bufferPtr, JNI_ABORT);
    env->ReleaseStringUTFChars(uri, uriStr);

    return (jint)result;
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

JNI_METHOD(jstring, Diary_nativeGetUri)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Diary*>(ptr)->get_uri().c_str());
}

JNI_METHOD(jstring, Diary_nativeGetUriUnsaved)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Diary*>(ptr)->get_uri_unsaved().c_str());
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

JNI_METHOD(jint, Diary_nativeWriteUri)(JNIEnv* env, jobject, jlong ptr, jstring uri) {
    const char* c_uri = env->GetStringUTFChars(uri, nullptr);
    auto result = reinterpret_cast<LoG::Diary*>(ptr)->write(c_uri);
    env->ReleaseStringUTFChars(uri, c_uri);
    return static_cast<jint>(result);
}

JNI_METHOD(jint, Diary_nativeWriteLock)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->write_lock());
}

JNI_METHOD(jint, Diary_nativeWriteTxt)(JNIEnv* env, jobject obj, jlong ptr, jstring uri, jlong
ptr_filter) {
    const char* c_uri = env->GetStringUTFChars(uri, nullptr);
    auto filter = reinterpret_cast<LoG::Filter*>(ptr_filter);
    auto result = reinterpret_cast<LoG::Diary*>(ptr)->write_txt(c_uri, filter);
    env->ReleaseStringUTFChars(uri, c_uri);
    return static_cast<jint>(result);
}

JNI_METHOD(jint, Diary_nativeWriteTo)(JNIEnv* env, jobject obj, jlong ptr, jstring uri) {
    const char* c_uri = env->GetStringUTFChars(uri, nullptr);
    LoG::Result res = reinterpret_cast<LoG::Diary*>(ptr)->write(c_uri);
    env->ReleaseStringUTFChars(uri, c_uri);
    return static_cast<jint>(res);
}

JNI_METHOD(jboolean, Diary_nativeRemoveLockIfNecessary)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<LoG::Diary*>(ptr)->remove_lock_if_necessary();
}

JNI_METHOD(void, Diary_nativeSetContinueFromLock)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Diary*>(ptr)->set_continue_from_lock();
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

JNI_METHOD(jlong, Diary_nativeGetEntryById)(JNIEnv* env, jobject obj, jlong ptr, jint id) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_entry_by_id(LoG::LoGID32 (id)));
}

JNI_METHOD(jlong, Diary_nativeGetParagraphById)(JNIEnv* env, jobject obj, jlong ptr, jint id) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_paragraph_by_id(LoG::LoGID32 (id)));
}

JNI_METHOD(jlong, Diary_nativeGetEntryByDate)(JNIEnv* env, jobject obj, jlong ptr, jlong date) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_entry_by_date(static_cast<LoG::DateV>(date)));
}

JNI_METHOD(jlong, Diary_nativeGetEntryByName)(JNIEnv* env, jobject obj, jlong ptr, jstring name) {
    const char* c_name = env->GetStringUTFChars(name, nullptr);
    auto result = reinterpret_cast<LoG::Diary*>(ptr)->get_entry_by_name(c_name);
    env->ReleaseStringUTFChars(name, c_name);
    return reinterpret_cast<jlong>( result );
}

JNI_METHOD(jlongArray, Diary_nativeGetEntriesByFilter)(JNIEnv* env, jobject obj, jlong ptr, jlong ptr_filter) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    auto filter = reinterpret_cast<LoG::Filter*>(ptr_filter);
    auto entries = diary->get_entries_by_filter(filter);
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

JNI_METHOD(jlong, Diary_nativeDuplicateEntry)(JNIEnv* env, jobject, jlong ptr, jlong ptr_entry) {
    auto p2entry = reinterpret_cast<LoG::Entry*>(ptr_entry);
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->duplicate_entry(p2entry));
}

JNI_METHOD(jlong, Diary_nativeDismissEntry)(JNIEnv* env, jobject obj, jlong ptr, jobject entry) {
    jclass clazz = env->GetObjectClass(entry);
    jfieldID fid = env->GetFieldID(clazz, "mNativePtr", "J");
    auto entry_ptr = reinterpret_cast<LoG::Entry*>(env->GetLongField(entry, fid));
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->dismiss_entry(entry_ptr));
}

JNI_METHOD(jlongArray, Diary_nativeGetFilters)(JNIEnv* env, jobject obj, jlong ptr) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    const auto& filters = diary->get_filters();
    auto size = static_cast<jsize>(filters.size());
    jlongArray result = env->NewLongArray(size);
    std::vector<jlong> ptrs;
    ptrs.reserve(size);
    for (auto f : filters) ptrs.push_back(reinterpret_cast<jlong>(f.second));
    env->SetLongArrayRegion(result, 0, size, ptrs.data());
    return result;
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

JNI_METHOD(jlong, Diary_nativeGetElement)(JNIEnv* env, jobject obj, jlong ptr, jint id) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_element(LoG::LoGID32(id )));
}

JNI_METHOD(jlong, Diary_nativeGetTagById)(JNIEnv* env, jobject obj, jlong ptr, jint id) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_tag_by_id(LoG::LoGID32(id )));
}

JNI_METHOD(jlongArray, Diary_nativeGetCharts)(JNIEnv* env, jobject obj, jlong ptr) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    const auto& charts = diary->get_charts();
    auto size = static_cast<jsize>(charts.size());
    jlongArray result = env->NewLongArray(size);
    std::vector<jlong> ptrs;
    ptrs.reserve(size);
    for (auto c : charts) ptrs.push_back(reinterpret_cast<jlong>(c.second));
    env->SetLongArrayRegion(result, 0, size, ptrs.data());
    return result;
}

JNI_METHOD(jlongArray, Diary_nativeGetThemes)(JNIEnv* env, jobject obj, jlong ptr) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    const auto& themes = diary->get_p2themes();
    auto size = static_cast<jsize>(themes->size());
    jlongArray result = env->NewLongArray(size);
    std::vector<jlong> ptrs;
    ptrs.reserve(size);
    for (auto e : *themes) ptrs.push_back(reinterpret_cast<jlong>(e.second));
    env->SetLongArrayRegion(result, 0, size, ptrs.data());
    return result;
}

JNI_METHOD(jlong, Diary_nativeCreateTheme)(JNIEnv* env, jobject obj, jlong ptr, jstring name0) {
    const char* c_name = env->GetStringUTFChars(name0, nullptr);
    auto result = reinterpret_cast<LoG::Diary*>(ptr)->create_theme(c_name);
    env->ReleaseStringUTFChars(name0, c_name);
    return reinterpret_cast<jlong>(result);
}

JNI_METHOD(jlong, Diary_nativeGetTheme)(JNIEnv* env, jobject obj, jlong ptr, jstring name) {
    const char* c_name = env->GetStringUTFChars(name, nullptr);
    auto result = reinterpret_cast<LoG::Diary*>(ptr)->get_theme(c_name);
    env->ReleaseStringUTFChars(name, c_name);
    return reinterpret_cast<jlong>(result);
}

JNI_METHOD(void, Diary_nativeSetSearchStr)(JNIEnv* env, jobject obj, jlong ptr, jstring str) {
    const char* c_str = env->GetStringUTFChars(str, nullptr);
    reinterpret_cast<LoG::Diary*>(ptr)->set_search_str(c_str);
    env->ReleaseStringUTFChars(str, c_str);
}

JNI_METHOD(jboolean, Diary_nativeIsSearchInProgress)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->is_search_in_progress());
}

JNI_METHOD(jlongArray, Diary_nativeGetMatches)(JNIEnv* env, jobject obj, jlong ptr) {
    auto matches = reinterpret_cast<LoG::Diary*>(ptr)->get_matches();
    auto size = static_cast<jsize>(matches->size());
    auto result = env->NewLongArray(size);
    std::vector<jlong> ptrs;
    ptrs.reserve(size);
    for (auto m : *matches) ptrs.push_back(reinterpret_cast<jlong>(m));
    env->SetLongArrayRegion(result, 0, size, ptrs.data());
    return result;
}

} // extern "C"
