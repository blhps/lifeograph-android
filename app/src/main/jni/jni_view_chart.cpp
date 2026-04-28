#include <jni.h>
#include <vector>
#include "widgets/chart_common.hpp"

#define JNI_METHOD(return_type, name) JNIEXPORT return_type JNICALL Java_net_sourceforge_lifeograph_##name

#define CHECK_PTR(ptr, return_value) if (ptr == 0) return return_value;

extern "C" {

JNI_METHOD(jlong, ViewChart_nativeCreate)(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new LoG::ChartCommon(false));
}
JNI_METHOD(void, ViewChart_nativeDestroy)(JNIEnv*, jobject, jlong ptr) {
    delete reinterpret_cast<LoG::ChartCommon*>(ptr);
}


JNI_METHOD(jfloat, ViewChart_nativeGetXOffset)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_x_offset );
}
JNI_METHOD(jfloat, ViewChart_nativeGetYOffset)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_y_offset );
}

JNI_METHOD(jfloat, ViewChart_nativeGetWidth)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_width );
}
JNI_METHOD(jfloat, ViewChart_nativeGetHeight)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_height );
}

JNI_METHOD(jfloat, ViewChart_nativeGetStepCount)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_step_count );
}
JNI_METHOD(jfloat, ViewChart_nativeGetStepStart)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_step_start );
}
JNI_METHOD(jfloat, ViewChart_nativeGetPreSteps)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_pre_steps );
}
JNI_METHOD(jfloat, ViewChart_nativeGetPostSteps)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_post_steps );
}

JNI_METHOD(jfloat, ViewChart_nativeGetZoomLevel)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_zoom_level );
}
JNI_METHOD(void, ViewChart_nativeSetZoom)(JNIEnv*, jobject, jlong ptr, jfloat level) {
    reinterpret_cast<LoG::ChartCommon*>(ptr)->set_zoom( level );
}

JNI_METHOD(jfloat, ViewChart_nativeGetBorderLabel)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_border_label );
}

JNI_METHOD(jfloat, ViewChart_nativeGetLabelSize)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_label_size );
}
JNI_METHOD(jfloat, ViewChart_nativeGetLabelHeight)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_label_height );
}

JNI_METHOD(jfloat, ViewChart_nativeGetHXValues)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_h_x_values );
}

JNI_METHOD(jfloat, ViewChart_nativeGetWidthColMin)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_width_col_min );
}

JNI_METHOD(jfloat, ViewChart_nativeGetXMin)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_x_min );
}
JNI_METHOD(jfloat, ViewChart_nativeGetYMax)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_y_max );
}

JNI_METHOD(jfloat, ViewChart_nativeGetY0)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_y_0 );
}

JNI_METHOD(jfloat, ViewChart_nativeGetStepX)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_step_x );
}

JNI_METHOD(jfloat, ViewChart_nativeGetCoefficient)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_coefficient );
}

JNI_METHOD(jfloat, ViewChart_nativeGetOVHeight)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_ov_height );
}
JNI_METHOD(jfloat, ViewChart_nativeGetStepXOV)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_step_x_ov );
}
JNI_METHOD(jfloat, ViewChart_nativeGetCoeffOV)(JNIEnv*, jobject, jlong ptr) {
    return static_cast<jfloat>(reinterpret_cast<LoG::ChartCommon*>(ptr)->m_coeff_ov );
}

JNI_METHOD(void, ViewChart_nativeUpdateHXValues)(JNIEnv*, jobject, jlong ptr) {
    reinterpret_cast<LoG::ChartCommon*>(ptr)->update_h_x_values();
}

JNI_METHOD(void, ViewChart_nativeUpdatePreAndPostSteps)(JNIEnv*, jobject, jlong ptr) {
    reinterpret_cast<LoG::ChartCommon*>(ptr)->update_pre_and_post_steps();
}

JNI_METHOD(jdouble, ViewChart_nativeGetValueAt)(JNIEnv*, jobject, jlong ptr, jint i) {
    return static_cast<jdouble>(reinterpret_cast<LoG::ChartCommon*>(ptr)->get_value_at( i ));
}

JNI_METHOD(void, ViewChart_nativeResize)(JNIEnv*, jobject, jlong ptr, jint w, jint h) {
    reinterpret_cast<LoG::ChartCommon*>(ptr)->resize( w, h );
}

JNI_METHOD(void, ViewChart_nativeScroll)(JNIEnv*, jobject, jlong ptr, jint offset) {
    reinterpret_cast<LoG::ChartCommon*>(ptr)->scroll( offset );
}
}
