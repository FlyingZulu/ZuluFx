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
Java_com_zulu_AudioEngine_nativeSetGen(JNIEnv*, jobject, jint t) { if (g) g->setGen(t); }

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
Java_com_zulu_AudioEngine_nativeSetScale(JNIEnv*, jobject, jint s) { if (g) g->setScale(s); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetRoot(JNIEnv*, jobject, jint r) { if (g) g->setRoot(r); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetArp(JNIEnv*, jobject, jboolean on, jint mode, jint rate,
        jint oct, jint gate, jboolean octJump) {
    if (g) g->setArp(on, mode, rate, oct, gate, octJump);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetArpSlices(JNIEnv*, jobject, jint n) { if (g) g->setArpSlices(n); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetCustomLfo(JNIEnv* env, jobject, jint idx, jfloatArray arr,
                                             jboolean on, jint rateIdx, jfloat depth,
                                             jfloat amount, jint target, jint wave) {
    if (!g) return;
    jsize n = env->GetArrayLength(arr);
    if (n > 64) n = 64;
    float tmp[64];
    if (n > 0) env->GetFloatArrayRegion(arr, 0, n, tmp);
    for (int i = 0; i < 64; i++) tmp[i] = (i < n) ? tmp[i] : (n > 0 ? tmp[n - 1] : 0.5f);
    g->setCustomLfo(idx, tmp, on == JNI_TRUE, rateIdx, depth, amount, target, wave);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetArpXy(JNIEnv*, jobject, jfloat x, jboolean t) { if (g) g->setArpXy(x, t); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetAssign(JNIEnv*, jobject, jint axis, jint p) { if (g) g->setAssign(axis, p); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetBend(JNIEnv*, jobject, jboolean b, jfloat x, jfloat lv) { if (g) g->setBend(b, x, lv); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetSoloMask(JNIEnv*, jobject, jint m) { if (g) g->setSoloMask(m); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetSynth(JNIEnv*, jobject, jint wave, jfloat atk, jfloat dec,
        jint oct, jfloat pitch, jfloat tune) {
    if (g) g->setSynth(wave, atk, dec, oct, pitch, tune);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetFx(JNIEnv*, jobject, jint fx, jboolean on) {
    if (g) g->setFx(fx, on == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetRecFlavor(JNIEnv*, jobject, jint f) {
    if (g) g->setRecFlavor(f);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetParam(JNIEnv*, jobject, jint p, jfloat v, jboolean on) {
    if (g) g->setParam(p, v, on == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetFxParam(JNIEnv*, jobject, jint fx, jint k, jfloat v) {
    if (g) g->setFxParam(fx, k, v);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetBankFxSend(JNIEnv*, jobject, jint b, jboolean on) {
    if (g) g->setBankFxSend(b, on == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeUndo(JNIEnv*, jobject) { if (g) g->undoBankRec(); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeCancelRec(JNIEnv*, jobject) { if (g) g->cancelRec(); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeStopRec(JNIEnv*, jobject) { if (g) g->stopRec(); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetPlayerBank(JNIEnv*, jobject, jint b) {
    if (g) g->setPlayerBank(b);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetInGain(JNIEnv*, jobject, jfloat v) { if (g) g->setInGain(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetInProfile(JNIEnv*, jobject, jint p) { if (g) g->setInProfile(p); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetMasterEq(JNIEnv*, jobject, jboolean on) { if (g) g->setMasterEq(on == JNI_TRUE); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetInBoost(JNIEnv*, jobject, jfloat v) { if (g) g->setInBoost(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetOutBoost(JNIEnv*, jobject, jfloat v) { if (g) g->setOutBoost(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetOutLoud(JNIEnv*, jobject, jboolean on) { if (g) g->setOutLoud(on == JNI_TRUE); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetAxisTargets(JNIEnv* env, jobject, jint axis, jfloatArray idxs, jfloatArray modes) {
    if (!g) return;
    jsize n = env->GetArrayLength(idxs);
    jsize m = env->GetArrayLength(modes);
    if (m < n) n = m;
    if (n > 8) n = 8;
    jfloat* pi = env->GetFloatArrayElements(idxs, 0);
    jfloat* pm = env->GetFloatArrayElements(modes, 0);
    g->setAxisTargets(axis, pi, pm, n);
    env->ReleaseFloatArrayElements(idxs, pi, JNI_ABORT);
    env->ReleaseFloatArrayElements(modes, pm, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetNetworkMode(JNIEnv*, jobject, jint m) { if (g) g->setNetworkMode(m); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetNetworkParams(JNIEnv*, jobject, jfloat ch, jfloat md, jfloat al, jfloat sp) { if (g) g->setNetworkParams(ch, md, al, sp); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeDrumHit(JNIEnv*, jobject, jint bank, jint slice, jint total, jfloat pitch) {
    if (g) g->drumHit(bank, slice, total, pitch);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeRecordBank(JNIEnv*, jobject, jint bank, jint steps, jboolean wet) { if (g) g->recordBank(bank, steps, wet); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeResample(JNIEnv*, jobject, jint bank, jint steps) { if (g) g->startResample(bank, steps); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetStep(JNIEnv*, jobject, jint bank, jint step, jfloat pitch, jfloat vel) { if (g) g->setStep(bank, step, pitch, vel); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetStepCount(JNIEnv*, jobject, jint bank, jint n) { if (g) g->setStepCount(bank, n); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeToggleBank(JNIEnv*, jobject, jint bank) { if (g) g->toggleBank(bank); }

JNIEXPORT jboolean JNICALL
Java_com_zulu_AudioEngine_nativeBankHasLoop(JNIEnv*, jobject, jint bank) { return g ? g->bankHasLoop(bank) : JNI_FALSE; }

JNIEXPORT jint JNICALL
Java_com_zulu_AudioEngine_nativeBankFrames(JNIEnv*, jobject, jint bank) { return g ? g->bankFrames(bank) : 0; }

JNIEXPORT jint JNICALL
Java_com_zulu_AudioEngine_nativeCopyBank(JNIEnv* env, jobject, jint bank, jfloatArray dst) {
    if (!g || !dst) return 0;
    int n = env->GetArrayLength(dst);
    jfloat* p = env->GetFloatArrayElements(dst, nullptr);
    int got = g->copyBank(bank, p, n / 2);
    env->ReleaseFloatArrayElements(dst, p, 0);
    return got;
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeLoadBank(JNIEnv* env, jobject, jint bank, jfloatArray src, jint frames) {
    if (!g || !src || frames <= 0) return;
    jfloat* p = env->GetFloatArrayElements(src, nullptr);
    g->loadBank(bank, p, frames);
    env->ReleaseFloatArrayElements(src, p, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeJumpSlice(JNIEnv*, jobject, jint slice, jint total) { if (g) g->jumpSlice(slice, total); }

JNIEXPORT jboolean JNICALL
Java_com_zulu_AudioEngine_nativeIsLoopRecording(JNIEnv*, jobject) { return g ? g->isLoopRecording() : JNI_FALSE; }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeGetLevels(JNIEnv* env, jobject, jfloatArray arr) {
    if (!g || !arr) return;
    float tmp[2] = { g->getLevelL(), g->getLevelR() };
    env->SetFloatArrayRegion(arr, 0, 2, tmp);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeGetInLevels(JNIEnv* env, jobject, jfloatArray arr) {
    if (!g || !arr) return;
    float tmp[2] = { g->getInLevel(), g->getInLevel() };
    env->SetFloatArrayRegion(arr, 0, 2, tmp);
}

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetMasterVolume(JNIEnv*, jobject, jfloat v) { if (g) g->setMasterVolume(v); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetGenTouch(JNIEnv*, jobject, jboolean t) { if (g) g->setGenTouch(t == JNI_TRUE); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeGenHit(JNIEnv*, jobject, jint type, jfloat x, jfloat y) { if (g) g->genHit(type, x, y); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetEco(JNIEnv*, jobject, jboolean e) { if (g) g->setEco(e == JNI_TRUE); }

JNIEXPORT void JNICALL
Java_com_zulu_AudioEngine_nativeSetStretchMode(JNIEnv*, jobject, jint bank, jint mode) {
    if (g) g->setStretchMode(bank, mode);
}

}
