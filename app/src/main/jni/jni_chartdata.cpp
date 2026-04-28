#include <jni.h>
#include "diaryelements/diarydata.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

extern "C" {

JNI_METHOD(jlong, ChartData_nativeCreate)(JNIEnv*, jobject, jlong ptr_diary) {
    auto p2diary = reinterpret_cast<LoG::Diary*>(ptr_diary);
    return reinterpret_cast<jlong>(new LoG::ChartData(p2diary));
}
JNI_METHOD(void, ChartData_nativeDestroy)(JNIEnv*, jobject, jlong ptr) {
    delete reinterpret_cast<LoG::ChartData*>(ptr);
}


JNI_METHOD(void, ChartData_nativeClear)(JNIEnv*, jobject, jlong ptr) {
     reinterpret_cast<LoG::ChartData*>(ptr)->clear();
}

JNI_METHOD(void, ChartData_nativeCalculatePoints)(JNIEnv*, jobject, jlong ptr) {
    reinterpret_cast<LoG::ChartData*>(ptr)->calculate_points();
}

JNI_METHOD(void, ChartData_nativeSetFromString)(JNIEnv* env, jobject, jlong ptr, jstring str) {
    const char* c_str = env->GetStringUTFChars(str, nullptr);
    reinterpret_cast<LoG::ChartData*>(ptr)->set_from_string( c_str );
    env->ReleaseStringUTFChars(str, c_str);
}

JNI_METHOD(jint, ChartData_nativeGetType)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jint>( reinterpret_cast<LoG::ChartData*>(ptr)->get_type() );
}

JNI_METHOD(jint, ChartData_nativeGetStyle)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jint>( reinterpret_cast<LoG::ChartData*>(ptr)->get_style() );
}

JNI_METHOD(jint, ChartData_nativeGetSpan)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jint>( reinterpret_cast<LoG::ChartData*>(ptr)->m_span );
}

JNI_METHOD(jint, ChartData_nativeGetPeriod)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jint>( reinterpret_cast<LoG::ChartData*>(ptr)->get_period() );
}

JNI_METHOD(jstring, ChartData_nativeGetUnit)(JNIEnv* env, jobject, jlong ptr) {
    return env->NewStringUTF( reinterpret_cast<LoG::ChartData*>(ptr)->m_unit.c_str() );
}

JNI_METHOD(jboolean, ChartData_nativeIsUnderlayPrevYear)(JNIEnv* env, jobject, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::ChartData*>(ptr)->m_tcidu ==
        LoG::ChartData::COLUMN_PREV_YEAR);
}
JNI_METHOD(jboolean, ChartData_nativeHasUnderlay)(JNIEnv* env, jobject, jlong ptr) {
    return static_cast<jboolean>(reinterpret_cast<LoG::ChartData*>(ptr)->m_tcidu !=
                                 LoG::ChartData::COLUMN_NONE);
}

JNI_METHOD(jdouble, ChartData_nativeGetVMin)(JNIEnv* env, jobject, jlong ptr) {
    return static_cast<jdouble>(reinterpret_cast<LoG::ChartData*>(ptr)->v_min);
}
JNI_METHOD(jdouble, ChartData_nativeGetVMax)(JNIEnv* env, jobject, jlong ptr) {
    return static_cast<jdouble>(reinterpret_cast<LoG::ChartData*>(ptr)->v_max);
}
JNI_METHOD(jdouble, ChartData_nativeGetVGridStep)(JNIEnv* env, jobject, jlong ptr) {
    return static_cast<jdouble>(reinterpret_cast<LoG::ChartData*>(ptr)->v_grid_step);
}
JNI_METHOD(jdouble, ChartData_nativeGetVGridMin)(JNIEnv* env, jobject, jlong ptr) {
    return static_cast<jdouble>(reinterpret_cast<LoG::ChartData*>(ptr)->v_grid_min);
}

// VALUE LISTS =====================================================================================
JNI_METHOD(jobject, ChartData_nativeGetValuesNum)(JNIEnv* env, jobject, jlong ptr) {
    auto chartData = reinterpret_cast<LoG::ChartData*>(ptr);

    // find the LinkedList class and its mapCtor:
    jclass linkedHashMapClass = env->FindClass("java/util/LinkedHashMap");
    jmethodID mapCtor = env->GetMethodID(linkedHashMapClass, "<init>", "()V");
    jmethodID putMethod = env->GetMethodID(linkedHashMapClass, "put",
                                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    // create a new instance of java map:
    jobject jmap = env->NewObject(linkedHashMapClass, mapCtor);

    // key: double must be wrapped them in 'java.lang.Double' objects:
    jclass doubleClass = env->FindClass("java/lang/Double");
    jmethodID dblCtor = env->GetMethodID(doubleClass, "<init>", "(D)V");
    // value: YValues must be wrapped them in 'java.lang.Double' objects:
    jclass yValuesClass = env->FindClass("net/sourceforge/lifeograph/YValues");
    jmethodID yValuesCtor = env->GetMethodID(yValuesClass, "<init>", "(DDI)V");

    // iterate through the native C++ data and add to the Java map:
    const auto& nativeValues = chartData->values_num;
    for (const auto& [key, val] : nativeValues) {
        jobject jkey = env->NewObject(doubleClass, dblCtor, key);
        jobject jval = env->NewObject(yValuesClass, yValuesCtor,
                                    (jdouble) val.v,
                                    (jdouble) val.u,
                                    (jint)    val.c);

        // add it to the jmap:
        env->CallObjectMethod(jmap, putMethod, jkey, jval);

        // clean up local reference to avoid memory pressure in long loops:
        env->DeleteLocalRef(jkey);
        env->DeleteLocalRef(jval);
    }

    return jmap;
}

JNI_METHOD(jobject, ChartData_nativeGetValuesDate)(JNIEnv* env, jobject, jlong ptr) {
    auto chartData = reinterpret_cast<LoG::ChartData*>(ptr);

    // find the LinkedList class and its mapCtor:
    jclass linkedHashMapClass = env->FindClass("java/util/LinkedHashMap");
    jmethodID mapCtor = env->GetMethodID(linkedHashMapClass, "<init>", "()V");
    jmethodID putMethod = env->GetMethodID(linkedHashMapClass, "put",
                                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    // create a new instance of java map:
    jobject jmap = env->NewObject(linkedHashMapClass, mapCtor);

    // key: DateV must be wrapped them in 'java.lang.Long' objects:
    jclass longClass = env->FindClass("java/lang/Long");
    jmethodID longCtor = env->GetMethodID(longClass, "<init>", "(J)V");
    // value: YValues must be wrapped them in 'java.lang.Double' objects:
    jclass yValuesClass = env->FindClass("net/sourceforge/lifeograph/YValues");
    jmethodID yValuesCtor = env->GetMethodID(yValuesClass, "<init>", "(DDI)V");

    // iterate through the native C++ data and add to the Java map:
    const auto& nativeValues = chartData->values_date;
    for (const auto& [key, val] : nativeValues) {
        jobject jkey = env->NewObject(longClass, longCtor, key);
        jobject jval = env->NewObject(yValuesClass, yValuesCtor,
                                      (jdouble) val.v,
                                      (jdouble) val.u,
                                      (jint)    val.c);

        // add it to the jmap:
        env->CallObjectMethod(jmap, putMethod, jkey, jval);

        // clean up local reference to avoid memory pressure in long loops:
        env->DeleteLocalRef(jkey);
        env->DeleteLocalRef(jval);
    }

    return jmap;
}

JNI_METHOD(jobject, ChartData_nativeGetValuesStr)(JNIEnv* env, jobject, jlong ptr) {
    auto chartData = reinterpret_cast<LoG::ChartData*>(ptr);

    // find the LinkedList class and its mapCtor:
    jclass linkedHashMapClass = env->FindClass("java/util/LinkedHashMap");
    jmethodID mapCtor = env->GetMethodID(linkedHashMapClass, "<init>", "()V");
    jmethodID putMethod = env->GetMethodID(linkedHashMapClass, "put",
                                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    // create a new instance of java map:
    jobject jmap = env->NewObject(linkedHashMapClass, mapCtor);

    // key: DateV must be wrapped them in 'java.lang.Long' objects:
    jclass intClass = env->FindClass("java/lang/Integer");
    jmethodID intCtor = env->GetMethodID(intClass, "<init>", "(I)V");
    // value: YValues must be wrapped them in 'java.lang.Double' objects:
    jclass yValuesClass = env->FindClass("net/sourceforge/lifeograph/YValues");
    jmethodID yValuesCtor = env->GetMethodID(yValuesClass, "<init>", "(DDI)V");

    // iterate through the native C++ data and add to the Java map:
    const auto& nativeValues = chartData->values_str;
    for (const auto& [key, val] : nativeValues) {
        jobject jkey = env->NewObject(intClass, intCtor, key);
        jobject jval = env->NewObject(yValuesClass, yValuesCtor,
                                      (jdouble) val.v,
                                      (jdouble) val.u,
                                      (jint)    val.c);

        // add it to the jmap:
        env->CallObjectMethod(jmap, putMethod, jkey, jval);

        // clean up local reference to avoid memory pressure in long loops:
        env->DeleteLocalRef(jkey);
        env->DeleteLocalRef(jval);
    }

    return jmap;
}

JNI_METHOD(jobject, ChartData_nativeGetValuesIndex2Str)(JNIEnv* env, jobject, jlong ptr) {
    auto chartData = reinterpret_cast<LoG::ChartData*>(ptr);

    // find the LinkedList class and its mapCtor:
    jclass linkedHashMapClass = env->FindClass("java/util/LinkedHashMap");
    jmethodID mapCtor = env->GetMethodID(linkedHashMapClass, "<init>", "()V");
    jmethodID putMethod = env->GetMethodID(linkedHashMapClass, "put",
                                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    // create a new instance of java map:
    jobject jmap = env->NewObject(linkedHashMapClass, mapCtor);

    // key: DateV must be wrapped them in 'java.lang.Long' objects:
    jclass intClass = env->FindClass("java/lang/Integer");
    jmethodID intCtor = env->GetMethodID(intClass, "<init>", "(I)V");

    // iterate through the native C++ data and add to the Java map:
    const auto& nativeValues = chartData->values_index2str;
    for (const auto& [key, val] : nativeValues) {
        jobject jkey = env->NewObject(intClass, intCtor, key);
        jobject jval = env->NewStringUTF(val.c_str());

        // add it to the jmap:
        env->CallObjectMethod(jmap, putMethod, jkey, jval);

        // clean up local reference to avoid memory pressure in long loops:
        env->DeleteLocalRef(jkey);
        env->DeleteLocalRef(jval);
    }

    return jmap;
}

// CONSTANTS =======================================================================================
JNI_METHOD(jint, ChartData_nativePERIOD_1MONTHLY)(JNIEnv* env, jclass ) {
    return static_cast<jint>(LoG::ChartData::PERIOD_MONTHLY);
}
JNI_METHOD(jint, ChartData_nativePERIOD_1YEARLY)(JNIEnv* env, jclass ) {
    return static_cast<jint>(LoG::ChartData::PERIOD_YEARLY);
}

JNI_METHOD(jint, ChartData_nativeNUM_1Y_1STEPS)(JNIEnv* env, jclass ) {
    return static_cast<jint>(LoG::ChartData::NUM_Y_STEPS);
}
JNI_METHOD(jint, ChartData_nativeSTYLE_1LINE)(JNIEnv* env, jclass ) {
    return static_cast<jint>(LoG::ChartData::STYLE_LINE);
}
JNI_METHOD(jint, ChartData_nativeSTYLE_1BARS)(JNIEnv* env, jclass ) {
    return static_cast<jint>(LoG::ChartData::STYLE_BARS);
}

JNI_METHOD(jint, ChartData_nativeTYPE_1DATE)(JNIEnv* env, jclass ) {
    return static_cast<jint>(LoG::ChartData::TYPE_DATE);
}
JNI_METHOD(jint, ChartData_nativeTYPE_1NUMBER)(JNIEnv* env, jclass ) {
    return static_cast<jint>(LoG::ChartData::TYPE_NUMBER);
}
JNI_METHOD(jint, ChartData_nativeTYPE_1STRING)(JNIEnv* env, jclass ) {
    return static_cast<jint>(LoG::ChartData::TYPE_STRING);
}

} // extern "C"
