#include <jni.h>
#include <string>
#include "helpers.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL \
Java_net_sourceforge_lifeograph_helpers_Date_##name

extern "C" {

JNI_METHOD(void, nativeSetFormatOrder)(JNIEnv* env, jclass, jstring order) {
    const char* c_order = env->GetStringUTFChars(order, nullptr);
    HELPERS::Date::s_format_order = c_order;
    env->ReleaseStringUTFChars(order, c_order);
}

JNI_METHOD(jchar, nativeGetFormatSeparator)(JNIEnv* env, jclass) {
    return static_cast<jchar>(HELPERS::Date::s_format_separator);
}
JNI_METHOD(void, nativeSetFormatSeparator)(JNIEnv* env, jclass, jchar separator) {
    HELPERS::Date::s_format_separator = char( separator );
}

JNI_METHOD(jboolean, nativeIsValid)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jboolean>(HELPERS::Date::is_valid(d));
}

JNI_METHOD(jboolean, nativeIsSet)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jboolean>(HELPERS::Date::is_set(d));
}

JNI_METHOD(jlong, nativeMake)(JNIEnv* env, jclass clazz, jint y, jint m, jint d) {
    return static_cast<jlong>(HELPERS::Date::make(static_cast<unsigned>(y),
                                                  static_cast<unsigned>(m),
                                                  static_cast<unsigned>(d)));
}

JNI_METHOD(jlong, nativeMakeStr)(JNIEnv* env, jclass clazz, jstring str) {
    const char* c_str = env->GetStringUTFChars(str, nullptr);
    auto result = static_cast<jlong>(HELPERS::Date::make(c_str));
    env->ReleaseStringUTFChars(str, c_str);
    return result;
}


JNI_METHOD(jlong, nativeGetToday)(JNIEnv* env, jclass clazz) {
    return static_cast<jlong>(HELPERS::Date::get_today());
}

JNI_METHOD(jlong, nativeGetNow)(JNIEnv* env, jclass clazz) {
    return static_cast<jlong>(HELPERS::Date::get_now());
}

JNI_METHOD(jint, nativeGetYear)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_year(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jint, nativeGetMonth)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_month(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jint, nativeGetDay)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_day(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jlong, nativeSetYear)(JNIEnv* env, jclass clazz, jlong date, jint year) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::set_year(d, static_cast<unsigned int>(year));
    return static_cast<jlong>(d);
}

JNI_METHOD(jlong, nativeSetMonth)(JNIEnv* env, jclass clazz, jlong date, jint month) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::set_year(d, static_cast<unsigned int>(month));
    return static_cast<jlong>(d);
}

JNI_METHOD(jlong, nativeSetDay)(JNIEnv* env, jclass clazz, jlong date, jint day) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::set_day(d, static_cast<unsigned int>(day));
    return static_cast<jlong>(d);
}


JNI_METHOD(jlong, nativeIsolateYM)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jlong>(HELPERS::Date::isolate_YM(static_cast<HELPERS::DateV>(d)));
}
JNI_METHOD(jlong, nativeIsolateYMD)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jlong>(HELPERS::Date::isolate_YMD(static_cast<HELPERS::DateV>(d)));
}


JNI_METHOD(jint, nativeGetHours)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_hours(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jint, nativeGetMins)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_mins(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jint, nativeGetSecs)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_secs(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jlong, nativeForwardMonths)(JNIEnv* env, jclass clazz, jlong date, jint months) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::forward_months(d, static_cast<unsigned int>(months));
    return static_cast<jlong>(d);
}

JNI_METHOD(jlong, nativeBackwardMonths)(JNIEnv* env, jclass clazz, jlong date, jint months) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::backward_months(d, static_cast<unsigned int>(months));
    return static_cast<jlong>(d);
}

JNI_METHOD(jlong, nativeForwardDays)(JNIEnv* env, jclass clazz, jlong date, jint days) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::forward_days(d, static_cast<unsigned int>(days));
    return static_cast<jlong>(d);
}

JNI_METHOD(jlong, nativeBackwardDays)(JNIEnv* env, jclass clazz, jlong date, jint days) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::backward_days(d, static_cast<unsigned int>(days));
    return static_cast<jlong>(d);
}

JNI_METHOD(jlong, nativeBackwardToWeekStart)(JNIEnv* env, jclass clazz, jlong date) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::backward_to_week_start(d);
    return static_cast<jlong>(d);
}
JNI_METHOD(jlong, nativeBackwardToMonthStart)(JNIEnv* env, jclass clazz, jlong date) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::backward_to_month_start(d);
    return static_cast<jlong>(d);
}
JNI_METHOD(jlong, nativeBackwardToYearStart)(JNIEnv* env, jclass clazz, jlong date) {
    auto d = static_cast<HELPERS::DateV>(date);
    HELPERS::Date::backward_to_year_start(d);
    return static_cast<jlong>(d);
}

JNI_METHOD(jint, nativeGetWeekday)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_weekday(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jint, nativeGetYearday)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_yearday(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jboolean, nativeIsLeapYear)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jboolean>(HELPERS::Date::is_leap_year(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jint, nativeGetDaysInMonth)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_days_in_month(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jint, nativeGetDaysInYear)(JNIEnv* env, jclass clazz, jlong d) {
    return static_cast<jint>(HELPERS::Date::get_days_in_year(static_cast<HELPERS::DateV>(d)));
}

JNI_METHOD(jstring, nativeFormatStringCustom)(JNIEnv* env, jclass clazz, jlong d, jstring format,
 jchar separator) {
    const char* c_format = env->GetStringUTFChars(format, nullptr);
    auto result = HELPERS::Date::format_string(static_cast<HELPERS::DateV>(d), c_format, char
    (separator) );
    env->ReleaseStringUTFChars(format, c_format);
    return env->NewStringUTF(result.c_str());
}
JNI_METHOD(jstring, nativeFormatString)(JNIEnv* env, jclass clazz, jlong d) {
    auto result = HELPERS::Date::format_string(static_cast<HELPERS::DateV>(d));
    return env->NewStringUTF(result.c_str());
}

JNI_METHOD(jstring, nativeGetMonthStr)(JNIEnv* env, jclass clazz, jlong d) {
    auto result = HELPERS::Date::get_month_str(static_cast<HELPERS::DateV>(d));
    return env->NewStringUTF(result.c_str());
}

JNI_METHOD(jstring, nativeGetWeekDayStr)(JNIEnv* env, jclass clazz, jlong d) {
    auto result = HELPERS::Date::get_weekday_str(static_cast<HELPERS::DateV>(d));
    return env->NewStringUTF(result.c_str());
}

JNI_METHOD(jstring, nativeGetDayName)(JNIEnv* env, jclass clazz, jint no) {
    return env->NewStringUTF(HELPERS::Date::get_day_name(static_cast<int>(no)).c_str());
}

JNI_METHOD(jlong, nativeParseString)(JNIEnv* env, jclass clazz, jstring str) {
    const char* c_str = env->GetStringUTFChars(str, nullptr);
    HELPERS::DateV result = HELPERS::Date::make(std::string(c_str));
    env->ReleaseStringUTFChars(str, c_str);
    return static_cast<jlong>(result);
}

JNI_METHOD(jint, nativeCalculateDaysBetween)(JNIEnv* env, jclass clazz, jlong d1, jlong d2) {
    return HELPERS::Date::calculate_days_between(static_cast<HELPERS::DateV>(d1),
                                                 static_cast<HELPERS::DateV>(d2));
}

JNI_METHOD(void, nativeSetFormat)(JNIEnv* env, jclass clazz, jstring order, jchar separator) {
    const char* c_order = env->GetStringUTFChars(order, nullptr);
    HELPERS::Date::s_format_order = c_order;
    HELPERS::Date::s_format_separator = static_cast<char>(separator);
    env->ReleaseStringUTFChars(order, c_order);
}

} // extern "C"
