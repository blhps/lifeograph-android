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
    extern void handle_search_thread_notification();
    LoG::Diary::d->m_dispatcher_search.connect( [](){ handle_search_thread_notification(); } );
}

JNI_METHOD(jlong, Diary_nativeGetMain)(JNIEnv* env, jclass) {
    return reinterpret_cast<jlong>(LoG::Diary::d);
}


JNI_METHOD(void, Diary_nativeInitNewPre)(JNIEnv* env, jobject, jlong ptr) {
    reinterpret_cast<LoG::Diary*>(ptr)->init_new_pre();
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

JNI_METHOD(jint, Diary_nativeReadHeader)(JNIEnv* env, jobject obj, jlong ptr, jbyteArray data) {
    jbyte* bufferPtr = env->GetByteArrayElements(data, nullptr);
    jsize size = env->GetArrayLength(data);

    // Access your Diary instance
    LoG::Result result = reinterpret_cast<LoG::Diary*>(ptr)->read_header((char*)bufferPtr, size);

    env->ReleaseByteArrayElements(data, bufferPtr, JNI_ABORT);

    return (jint)result;
}

JNI_METHOD(jint, Diary_nativeReadBody)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->read_body());
}

JNI_METHOD(void, Diary_nativeSetName)(JNIEnv* env, jobject obj, jlong ptr, jstring name) {
    const char* c_name = env->GetStringUTFChars(name, nullptr);
    reinterpret_cast<LoG::Diary*>(ptr)->set_name(c_name);
    env->ReleaseStringUTFChars(name, c_name);
}

JNI_METHOD(void, Diary_nativeSetReadOnly)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Diary*>(ptr)->set_read_only();
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

JNI_METHOD(void, Diary_nativeSetUri)(JNIEnv* env, jobject obj, jlong ptr, jstring uri) {
    const char* c_uri = env->GetStringUTFChars(uri, nullptr);
    reinterpret_cast<LoG::Diary*>(ptr)->set_uri(c_uri);
    env->ReleaseStringUTFChars(uri, c_uri);
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

JNI_METHOD(void, Diary_nativeSetLoggedInEdit)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Diary*>(ptr)->set_loggedin_edit();
}

JNI_METHOD(jstring, Diary_nativeGetLang)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Diary*>(ptr)->get_lang().c_str());
}

JNI_METHOD(void, Diary_nativeSetLang)(JNIEnv* env, jobject obj, jlong ptr, jstring lang) {
    const char* c_lang = env->GetStringUTFChars(lang, nullptr);
    reinterpret_cast<LoG::Diary*>(ptr)->set_lang(c_lang);
    env->ReleaseStringUTFChars(lang, c_lang);
}

JNI_METHOD(jboolean, Diary_nativeIsDayHoliday)(JNIEnv* env, jobject obj, jlong ptr, jlong date) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->is_day_holiday(date) );
}
JNI_METHOD(jboolean, Diary_nativeIsDayWorkday)(JNIEnv* env, jobject obj, jlong ptr, jlong date) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->is_day_workday(date) );
}

JNI_METHOD(jint, Diary_nativeWriteUri)(JNIEnv* env, jobject, jlong ptr, jstring uri) {
    const char* c_uri = env->GetStringUTFChars(uri, nullptr);
    auto result = reinterpret_cast<LoG::Diary*>(ptr)->write(c_uri);
    env->ReleaseStringUTFChars(uri, c_uri);
    return static_cast<jint>(result);
}

JNI_METHOD(jbyteArray, Diary_nativeGetStrStream)(JNIEnv *env, jobject thiz, jlong ptr) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    const std::stringstream* ss = diary->get_str_stream();
    std::string s = ss->str();
    const char* data = s.data(); // .data() is preferred for binary; .c_str() also works
    size_t data_size = s.size();

    if (data_size == 0) return nullptr;

    jbyteArray result = env->NewByteArray(static_cast<jsize>(data_size));
    if (result == nullptr) return nullptr;

    env->SetByteArrayRegion(result, 0, static_cast<jsize>(data_size),
                            reinterpret_cast<const jbyte*>(data));

    return result;
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

JNI_METHOD(jboolean, Diary_nativeGetContinueFromLock)(JNIEnv*, jobject, jlong ptr) {
    return reinterpret_cast<LoG::Diary*>(ptr)->get_continue_from_lock();
}
JNI_METHOD(void, Diary_nativeSetContinueFromLock)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Diary*>(ptr)->set_continue_from_lock();
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
    auto new_entry = reinterpret_cast<LoG::Diary*>(ptr)->create_entry(rel_ptr,
                                                                      fParent,
                                                                      static_cast<LoG::DateV>(date),
                                                                      c_content,
                                                                      LoG::VT::ETS::INHERIT::I);
    env->ReleaseStringUTFChars(content, c_content);
    return reinterpret_cast<jlong>(new_entry);
}

JNI_METHOD(jlong, Diary_nativeCreateEntryChild)(JNIEnv* env, jobject obj, jlong ptr, jobject,
        jlong date, jstring content) {
    auto entry_parent = reinterpret_cast< LoG::Entry* >(ptr);
    const char* c_content = env->GetStringUTFChars(content, nullptr);
    auto new_entry = reinterpret_cast<LoG::Diary*>(ptr)->create_entry_child(entry_parent,
                                                                      static_cast<LoG::DateV>(date),
                                                                      c_content,
                                                                      LoG::VT::ETS::INHERIT::I);
    env->ReleaseStringUTFChars(content, c_content);
    return reinterpret_cast<jlong>(new_entry);
}

JNI_METHOD(jlong, Diary_nativeCreateEntryParent)(JNIEnv* env, jobject obj, jlong ptr, jobjectArray
entries, jlong date, jstring content) {
    // variables:
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    LoG::EntrySelection entries_set; // a temporary set to pass to C++ core
    const char* c_content = env->GetStringUTFChars(content, nullptr);

    // fill the temporary set:
    if (entries != nullptr) {
        jsize length = env->GetArrayLength(entries);

        // cache class and field ID for performance
        jclass entry_class = nullptr;
        jfieldID fid = nullptr;

        for (jsize i = 0; i < length; i++) {
            jobject entry_obj = env->GetObjectArrayElement(entries, i);
            if (entry_obj != nullptr) {
                if (entry_class == nullptr) {
                    entry_class = env->GetObjectClass(entry_obj);
                    fid = env->GetFieldID(entry_class, "mNativePtr", "J");
                }

                auto entry_ptr = reinterpret_cast<LoG::Entry*>(env->GetLongField(entry_obj, fid));
                if (entry_ptr) {
                    entries_set.insert(entry_ptr);
                }
                env->DeleteLocalRef(entry_obj);
            }
        }
    }

    // call the core method:
    auto new_entry = diary->create_entry_parent(entries_set,
                                                static_cast<LoG::DateV>(date),
                                                c_content,
                                                LoG::VT::ETS::INHERIT::I);

    env->ReleaseStringUTFChars(content, c_content);
    return reinterpret_cast<jlong>(new_entry);
}

JNI_METHOD(jlong, Diary_nativeCreateEntryDated)(JNIEnv* env, jobject obj, jlong ptr, jlong
ptr_entry_parent, jlong date, jboolean fMileStone) {
    auto entry_parent = reinterpret_cast< LoG::Entry* >(ptr_entry_parent);
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    auto new_entry = diary->create_entry_dated(entry_parent,
                                                       static_cast<LoG::DateV>(date),
                                                       fMileStone);
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

JNI_METHOD(jlong, Diary_nativeGetFilterNonTrashed)(JNIEnv* env, jobject obj, jlong ptr) {
    auto trshd = reinterpret_cast<LoG::Diary*>(ptr)->get_filter_nontrashed();
    return reinterpret_cast<jlong>(trshd);
    //return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_filter_nontrashed());
}
JNI_METHOD(jlong, Diary_nativeGetFilterTrashed)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_filter_trashed());
}

JNI_METHOD(jlong, Diary_nativeGetFilterEntryList)(JNIEnv* env, jobject obj, jlong ptr) {
    return reinterpret_cast<jlong>(reinterpret_cast<LoG::Diary*>(ptr)->get_filter_list());
}

JNI_METHOD(void, Diary_nativeSetFilterEntryList)(JNIEnv* env, jobject obj, jlong ptr, jlong
ptrFilter) {
    auto filter = reinterpret_cast<LoG::Filter*>(ptrFilter);
    reinterpret_cast<LoG::Diary*>(ptr)->set_filter_list(filter);
}

JNI_METHOD(jint, Diary_nativeUpdateAllEntriesFilterStatus)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jint>(reinterpret_cast<LoG::Diary*>(ptr)->update_all_entries_filter_status
    ());
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

JNI_METHOD(jlong, Diary_nativeCreateFilter)(JNIEnv* env, jobject obj, jlong ptr, jstring name0) {
    const char* c_name = env->GetStringUTFChars(name0, nullptr);
    auto result = reinterpret_cast<LoG::Diary*>(ptr)->create_filter(c_name);
    env->ReleaseStringUTFChars(name0, c_name);
    return reinterpret_cast<jlong>(result);
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

JNI_METHOD(jstring, Diary_nativeGetSearchStr)(JNIEnv* env, jobject obj, jlong ptr) {
    return env->NewStringUTF(reinterpret_cast<LoG::Diary*>(ptr)->get_search_str().c_str());
}

JNI_METHOD(void, Diary_nativeSetSearchStr)(JNIEnv* env, jobject obj, jlong ptr, jstring str) {
    const char* c_str = env->GetStringUTFChars(str, nullptr);
    reinterpret_cast<LoG::Diary*>(ptr)->set_search_str(c_str);
    env->ReleaseStringUTFChars(str, c_str);
}

JNI_METHOD(void, Diary_nativeStartSearch)(JNIEnv* env, jobject obj, jlong ptr) {
    auto diary = reinterpret_cast<LoG::Diary*>(ptr);
    const int processor_count = int( std::thread::hardware_concurrency() );
    diary->start_search( std::max( processor_count - 2 , 1 ) );
}
JNI_METHOD(void, Diary_nativeStopSearch)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Diary*>(ptr)->stop_search();
}

JNI_METHOD(void, Diary_nativeDestroySearchThreads)(JNIEnv* env, jobject obj, jlong ptr) {
    reinterpret_cast<LoG::Diary*>(ptr)->destroy_search_threads();
}

JNI_METHOD(jboolean, Diary_nativeIsSearchInProgress)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::Diary*>(ptr)->is_search_in_progress());
}

JNI_METHOD(jint, Diary_nativeGetMatchCount)(JNIEnv* env, jobject obj, jlong ptr) {
    auto matches = reinterpret_cast<LoG::Diary*>(ptr)->get_matches();
    return static_cast<jint>(matches->size());
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
