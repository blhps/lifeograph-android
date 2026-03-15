#include "android_shim.hpp"
#include <jni.h>

// Global reference to the Java VM to attach threads if necessary
JavaVM* g_vm = nullptr;
jclass g_bridgeClass = nullptr;
jmethodID g_dispatchMethod = nullptr;

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_vm = vm;
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("net/sourceforge/lifeograph/NativeBridge");
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    g_bridgeClass = (jclass)env->NewGlobalRef(clazz);

    g_dispatchMethod = env->GetStaticMethodID(g_bridgeClass, "dispatchToMain", "(J)V");
    if (g_dispatchMethod == nullptr) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

namespace Glib {

void trigger_android_dispatcher(Dispatcher* d) {
    JNIEnv* env;
    bool detached = false;
    jint getEnvStat = g_vm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        if (g_vm->AttachCurrentThread(&env, nullptr) != 0) {
            return;
        }
        detached = true;
    }

    if (env && g_bridgeClass && g_dispatchMethod) {
        env->CallStaticVoidMethod(g_bridgeClass, g_dispatchMethod, (jlong)d);
    }

    if (detached) {
        g_vm->DetachCurrentThread();
    }
}

} // namespace Glib

// The actual callback that Java triggers:
extern "C" JNIEXPORT void JNICALL
Java_net_sourceforge_lifeograph_NativeBridge_nativePerform(JNIEnv* env, jclass clazz, jlong ptr) {
    auto* dispatcher = reinterpret_cast<Glib::Dispatcher*>(ptr);
    if (dispatcher && dispatcher->m_callback) {
        dispatcher->m_callback();
    }
}
