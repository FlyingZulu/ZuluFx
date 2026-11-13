#include <jni.h>
#include "audio_engine.h"

static AudioEngine* engine = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_zulu_AudioEngine_nativeStart(
    JNIEnv*,
    jobject
) {
    if (!engine) {
        engine = new AudioEngine();
    }

    return engine->start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeStop(
    JNIEnv*,
    jobject
) {
    if (engine) {
        engine->stop();
        delete engine;
        engine = nullptr;
    }
}

}
