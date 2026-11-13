#include <jni.h>
#include "audio_engine.h"

static AudioEngine* g = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_zulu_AudioEngine_nativeStart(JNIEnv*, jobject, jboolean mic) {
    if (!g) g = new AudioEngine();
    return g->start(mic);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeStop(JNIEnv*, jobject) {
    if (g) { g->stop(); delete g; g = nullptr; }
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetFilterType(JNIEnv*, jobject, jint t) { if (g) g->setFilterType(t); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetAx(JNIEnv*, jobject, jfloat v) { if (g) g->setAx(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetAy(JNIEnv*, jobject, jfloat v) { if (g) g->setAy(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetBx(JNIEnv*, jobject, jfloat v) { if (g) g->setBx(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetBy(JNIEnv*, jobject, jfloat v) { if (g) g->setBy(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetMix(JNIEnv*, jobject, jfloat v) { if (g) g->setMix(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetMute(JNIEnv*, jobject, jboolean m) { if (g) g->setMute(m); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetFly(JNIEnv*, jobject, jfloat v) { if (g) g->setFly(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetStutter(JNIEnv*, jobject, jboolean s) { if (g) g->setStutter(s); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetBpm(JNIEnv*, jobject, jfloat b) { if (g) g->setBpm(b); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeGetLevels(JNIEnv* env, jobject, jfloatArray arr) {
    if (!g || !arr) return;
    float tmp[2] = { g->getLevelL(), g->getLevelR() };
    env->SetFloatArrayRegion(arr, 0, 2, tmp);
}

}
