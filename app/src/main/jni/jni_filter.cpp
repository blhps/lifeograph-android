#include <jni.h>
#include <string>
#include <vector>
#include "diaryelements/diary.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

// FILTER ==========================================================================================
JNI_METHOD(jlong, Filter_nativeGetFiltererStack)(JNIEnv* env, jobject, jlong ptr) {
    return reinterpret_cast<jlong>( reinterpret_cast<LoG::Filter*>(ptr)->get_filterer_stack() );
}

JNI_METHOD(jboolean, Filter_nativeCanFilterEntries)(JNIEnv* env, jobject, jlong ptr) {
    return static_cast<jboolean>( reinterpret_cast<LoG::Filter*>(ptr)->can_filter_class(LoG::FOC::ENTRIES) );
}

JNI_METHOD(jboolean, Filter_nativeCanFilterParagraphs)(JNIEnv* env, jobject, jlong ptr) {
    return static_cast<jboolean>( reinterpret_cast<LoG::Filter*>(ptr)->can_filter_class(LoG::FOC::PARAGRAPHS) );
}

// FILTERER ========================================================================================
JNI_METHOD(jboolean, Filterer_nativeGetNot)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>( reinterpret_cast<LoG::Filterer*>(ptr)->is_not() );
}

JNI_METHOD(void, Filterer_nativeSetNot)(JNIEnv* env, jobject obj, jlong ptr, jboolean value)  {
    reinterpret_cast<LoG::Filterer*>(ptr)->set_not( bool( value ) );
}

// FILTERER CONTAINER ==============================================================================
JNI_METHOD(jstring, FiltererContainer_nativeGetAsString)(JNIEnv* env, jobject obj, jlong ptr) {
    Glib::ustring str;
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->get_as_string(str);
    return env->NewStringUTF( str.c_str());
}

JNI_METHOD(void, FiltererContainer_nativeSetFromString)(JNIEnv* env, jobject, jlong ptr, jstring str) {
    const char* c_str = env->GetStringUTFChars(str, nullptr);
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->set_from_string( c_str );
    env->ReleaseStringUTFChars(str, c_str);
}

JNI_METHOD(void, FiltererContainer_nativeToggleLogic)(JNIEnv* env, jobject, jlong ptr) {
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->toggle_logic();
}

JNI_METHOD(jboolean, FiltererContainer_nativeIsOr)(JNIEnv* env, jobject obj, jlong ptr) {
    return static_cast<jboolean>( reinterpret_cast<LoG::FiltererContainer*>(ptr)->is_or() );
}

// FILTERER ADDERS
JNI_METHOD(void, FiltererContainer_nativeAddFiltererFavorite)(JNIEnv* env, jobject, jlong ptr) {
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->add<LoG::FiltererFavorite>();
}
JNI_METHOD(void, FiltererContainer_nativeAddFiltererTrashed)(JNIEnv* env, jobject, jlong ptr) {
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->add<LoG::FiltererTrashed>();
}
JNI_METHOD(void, FiltererContainer_nativeAddFiltererStatus)(JNIEnv* env, jobject, jlong ptr) {
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->add<LoG::FiltererStatus>();
}
JNI_METHOD(void, FiltererContainer_nativeAddFiltererIs)(JNIEnv* env, jobject, jlong ptr) {
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->add<LoG::FiltererIs>();
}
JNI_METHOD(void, FiltererContainer_nativeAddFiltererHasTag)(JNIEnv* env, jobject, jlong ptr) {
    reinterpret_cast<LoG::FiltererContainer*>(ptr)->add<LoG::FiltererHasTag>();
}
// REMOVE FILTERER
JNI_METHOD(jboolean, FiltererContainer_nativeRemoveFilterer)(JNIEnv* env, jobject, jlong ptr,
                                                               jlong ptr_filterer) {
    auto filterer = reinterpret_cast<LoG::Filterer*>(ptr_filterer);
    return static_cast<jboolean>( reinterpret_cast<LoG::FiltererContainer*>(ptr)
            ->remove_filterer(filterer) );
}

// PIPELINE
JNI_METHOD(jlongArray, FiltererContainer_nativeGetPipeline)(JNIEnv* env, jobject, jlong ptr) {
    const auto& pipeline = reinterpret_cast<LoG::FiltererContainer*>(ptr)->m_pipeline;

    auto size = static_cast<jsize>(pipeline.size());
    jlongArray result = env->NewLongArray(size);

    if (size > 0) {
        // convert pointers to jlongs:
        std::vector<jlong> temp(size);
        for (jsize i = 0; i < size; ++i) {
            temp[i] = reinterpret_cast<jlong>(pipeline[i]);
        }

        // copy the temporary vector to the JNI array:
        env->SetLongArrayRegion(result, 0, size, temp.data());
    }

    return result;
}

JNI_METHOD(jchar, FiltererContainer_nativeGetFiltererType)(JNIEnv* env, jobject, jlong ptr) {
    auto* filterer = reinterpret_cast<LoG::Filterer*>(ptr);

    // use dynamic_cast to identify the subclass
    if (dynamic_cast<LoG::FiltererStatus*>(filterer)) return 's';
    if (dynamic_cast<LoG::FiltererFavorite*>(filterer)) return 'f';
    if (dynamic_cast<LoG::FiltererTrashed*>(filterer)) return 't';
    if (dynamic_cast<LoG::FiltererIs*>(filterer)) return 'i';
    if (dynamic_cast<LoG::FiltererHasTag*>(filterer)) return 'r';

    return '~'; // unsopprted
}
// FILTERER STATUS =================================================================================
JNI_METHOD(jint, FiltererContainer_00024FiltererStatus_nativeGetIncludedStatuses)(JNIEnv* env,
        jobject,
        jlong ptr) {
    return static_cast<jint>( reinterpret_cast<LoG::FiltererStatus*>(ptr)->m_included_statuses );
}
JNI_METHOD(void, FiltererContainer_00024FiltererStatus_nativeSetIncludedStatuses)(JNIEnv* env,
        jobject, jlong ptr, jint value) {
    reinterpret_cast<LoG::FiltererStatus*>(ptr)->m_included_statuses = LoG::ElemStatus( value );
}

// FILTERER IS =====================================================================================
JNI_METHOD(jint, FiltererContainer_00024FiltererIs_nativeGetId)(JNIEnv* env, jobject, jlong ptr) {
    auto filterer = reinterpret_cast<LoG::FiltererIs*>(ptr);
    auto tag = filterer->m_p2tag;
    return static_cast<jint>( tag ? tag->get_id().get_raw() : 0);
}
JNI_METHOD(void, FiltererContainer_00024FiltererIs_nativeSetId)(JNIEnv* env,
                                                                jobject,
                                                                jlong ptr, jint value) {
    auto filterer = reinterpret_cast<LoG::FiltererIs*>(ptr);
    filterer->m_p2tag = filterer->m_p2container->m_p2diary->get_tag_by_id( LoG::LoGID32(value) );
}

// FILTERER HAS TAG ================================================================================
JNI_METHOD(jint, FiltererContainer_00024FiltererHasTag_nativeGetId)(JNIEnv* env, jobject, jlong
ptr) {
    auto filterer = reinterpret_cast<LoG::FiltererHasTag*>(ptr);
    auto tag = filterer->m_p2tag;
    return static_cast<jint>( tag ? tag->get_id().get_raw() : 0);
}
JNI_METHOD(void, FiltererContainer_00024FiltererHasTag_nativeSetId)(JNIEnv* env,
                                                                jobject,
                                                                jlong ptr, jint value) {
    auto filterer = reinterpret_cast<LoG::FiltererHasTag*>(ptr);
    filterer->m_p2tag = filterer->m_p2container->m_p2diary->get_tag_by_id( LoG::LoGID32(value) );
}

} // extern "C"
