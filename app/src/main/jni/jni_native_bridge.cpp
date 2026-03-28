#include "android_shim.hpp"
#include <jni.h>
#include <string>
#include "diary.hpp"

// Global reference to the Java VM to attach threads if necessary
JavaVM* g_vm = nullptr;
jclass g_bridgeClass = nullptr;
//jclass g_diaryClass = nullptr;
jmethodID g_dispatchMethod = nullptr;
jmethodID g_handleSearchFinishedMethod = nullptr;
jmethodID g_getFileNameMethod = nullptr;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
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

//    clazz = env->FindClass("net/sourceforge/lifeograph/Diary");
//    if (clazz == nullptr) {
//        return JNI_ERR;
//    }
//    g_diaryClass = (jclass)env->NewGlobalRef(clazz);

    g_dispatchMethod = env->GetStaticMethodID(g_bridgeClass, "dispatchToMain", "(J)V");
    if (g_dispatchMethod == nullptr) {
        return JNI_ERR;
    }

    g_handleSearchFinishedMethod = env->GetStaticMethodID(g_bridgeClass, "handleSearchFinished", "()V");
    if (g_handleSearchFinishedMethod == nullptr) {
        return JNI_ERR;
    }

    g_getFileNameMethod = env->GetStaticMethodID(g_bridgeClass, "getFileName", "(Ljava/lang/String;)Ljava/lang/String;");
    if (g_getFileNameMethod == nullptr) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

void handle_search_thread_notification() {
    LoG::Diary::d->destroy_search_threads();

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
        env->CallStaticVoidMethod(g_bridgeClass, g_handleSearchFinishedMethod);
    }

    if (detached) {
        g_vm->DetachCurrentThread();
    }

}

} // extern "C"

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

std::string get_filename_from_android(const std::string& uri) {
    JNIEnv* env;
    bool detached = false;
    jint getEnvStat = g_vm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        if (g_vm->AttachCurrentThread(&env, nullptr) != 0) {
            return "";
        }
        detached = true;
    }

    std::string result;
    if (env && g_bridgeClass && g_getFileNameMethod) {
        jstring juri = env->NewStringUTF(uri.c_str());
        auto jfilename = (jstring)env->CallStaticObjectMethod(g_bridgeClass, g_getFileNameMethod,
                                                           juri);
        if (jfilename) {
            const char* cfilename = env->GetStringUTFChars(jfilename, nullptr);
            if (cfilename) {
                result = cfilename;
                env->ReleaseStringUTFChars(jfilename, cfilename);
            }
        }
        env->DeleteLocalRef(juri);
    }

    if (detached) {
        g_vm->DetachCurrentThread();
    }
    return result;
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
