#include "audio_engine.h"
#include <cmath>
#include <cstring>

static inline float clamp01(float v) { return v < 0.f ? 0.f : (v > 1.f ? 1.f : v); }

static const int8_t scaleTable[6][12] = {
    {0,3,5,7,10,-1,-1,-1,-1,-1,-1,-1},
    {0,2,4,7,9,-1,-1,-1,-1,-1,-1,-1},
    {0,2,3,7,8,-1,-1,-1,-1,-1,-1,-1},
    {0,1,5,7,10,-1,-1,-1,-1,-1,-1,-1},
    {0,1,4,7,8,-1,-1,-1,-1,-1,-1,-1},
    {0,1,2,3,4,5,6,7,8,9,10,11},
};

float AudioEngine::degreeToSemi(int deg) const {
    int s = scale.load();
    if (s < 0 || s > 5) s = 0;
    int len = 0;
    while (len < 12 && scaleTable[s][len] >= 0) len++;
    if (len == 0) len = 12;
    int oct = 0;
    while (deg < 0) { deg += len; oct--; }
    oct += deg / len;
    deg %= len;
    return (float)(scaleTable[s][deg] + 12 * oct + rootNote.load());
}

static const int8_t patBass1[16] = {0,-1,0,-1, 2,-1,0,-1, 0,-1,1,-1, 0,-1,3,-1};
static const int8_t patBass2[16] = {0,0,-1,0, -1,-1,1,-1, 2,-1,2,-1, 1,-1,-1,-1};
static const int8_t patBass3[16] = {0,-1,-1,-1, 0,-1,-1,-1, 0,-1,-1,-1, 0,-1,1,-1};
static const int8_t patAcid[16]  = {0,-1,2,-1, 0,-1,1,-1, 0,-1,3,-1, 2,-1,4,-1};
static const int8_t patKick[16]  = {1,-1,-1,-1, -1,-1,-1,-1, 1,-1,-1,-1, -1,-1,1,-1};
static const int8_t patSnr[16]   = {-1,-1,-1,-1, 1,-1,-1,-1, -1,-1,-1,-1, 1,-1,-1,1};
static const int8_t patHat[16]   = {1,-1,1,-1, 1,-1,1,1, 1,-1,1,-1, 1,1,1,-1};
static const int8_t patClk[16]   = {-1,-1,1,-1, -1,-1,-1,-1, -1,-1,1,-1, -1,1,-1,-1};
static const int8_t patZap[16]   = {1,-1,-1,-1, -1,-1,-1,1, -1,-1,-1,-1, 1,-1,-1,-1};

float AudioEngine::genSample(float fsr, float bpm) {
    int t = genType.load();
    if (t == 0) {
        float s = (sawPhase * 2.f - 1.f) * 0.4f;
        sawPhase += 110.f * fsr;
        if (sawPhase >= 1.f) sawPhase -= 1.f;
        return s;
    }

    if (t == 10 || t == 11) {
        float rate = (t == 10) ? 0.9f : 5.f;
        sirenPhase += rate * fsr;
        if (sirenPhase >= 1.f) sirenPhase -= 1.f;
        float tri = sirenPhase < 0.5f ? sirenPhase * 4.f - 1.f : 3.f - 4.f * sirenPhase;
        float f = 55.f * powf(2.f, (float)rootNote.load() / 12.f) * 4.f * powf(2.f, tri * 0.6f);
        oscPhase += f * fsr;
        if (oscPhase >= 1.f) oscPhase -= 1.f;
        return (oscPhase < 0.5f ? 1.f : -1.f) * 0.2f;
    }

    if (t == 12) {
        noiseState = noiseState * 1103515245u + 12345u;
        return ((float)(noiseState >> 16) / 32768.f - 1.f) * 0.22f;
    }

    bool tonal = (t >= 1 && t <= 4);
    if (tonal && arpOn.load()) {
        return synthSample(t, fsr);
    }

    float stepDur = (60.f / bpm) / 4.f;
    stepClock += fsr;
    if (stepClock >= stepDur) {
        stepClock -= stepDur;
        seqStep = (seqStep + 1) & 15;
        const int8_t* p = nullptr;
        switch (t) {
            case 1: p = patBass1; break;
            case 2: p = patBass2; break;
            case 3: p = patBass3; break;
            case 4: p = patAcid; break;
            case 5: p = patKick; break;
            case 6: p = patSnr; break;
            case 7: p = patHat; break;
            case 8: p = patClk; break;
            case 9: p = patZap; break;
        }
        if (p && p[seqStep] >= 0) {
            env = 1.f;
            fmPhase = 0.f;
            switch (t) {
                case 5: kickF = 130.f; envRate = 1.f / (0.26f * fs); break;
                case 6: envRate = 1.f / (0.13f * fs); break;
                case 7: envRate = 1.f / (0.045f * fs); break;
                case 8: envRate = 1.f / (0.16f * fs); break;
                case 9: zapF = 900.f; envRate = 1.f / (0.15f * fs); break;
                default: envRate = 1.f / (0.28f * fs); break;
            }
            baseF = 55.f * powf(2.f, degreeToSemi(p[seqStep]) / 12.f);
            if (t <= 4) {
                baseF *= pitchFactor();
                float dec = synDecay.load();
                if (dec < 0.25f) dec = 0.25f;
                if (dec > 4.f) dec = 4.f;
                envRate /= dec;
                float atk = synAtk.load();
                if (atk > 0.002f) {
                    env = 0.f;
                    envAtkRate = 1.f / (atk * 0.5f * fs);
                } else {
                    env = 1.f;
                    envAtkRate = 0.f;
                }
            }
        }
    }

    if (envAtkRate > 0.f) {
        env += envAtkRate * fsr;
        if (env >= 1.f) { env = 1.f; envAtkRate = 0.f; }
    } else if (env > 0.f) env -= envRate * fsr;
    if (env < 0.f) env = 0.f;

    switch (t) {
        case 1: {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return (oscPhase * 2.f - 1.f) * 0.45f * env;
        }
        case 2: {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return (oscPhase < 0.5f ? 1.f : -1.f) * 0.32f * env;
        }
        case 3: {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return sinf(2.f * 3.14159265f * oscPhase) * 0.55f * env;
        }
        case 4: {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return (oscPhase * 2.f - 1.f) * 0.5f * env;
        }
        case 5: {
            kickF += (45.f - kickF) * 0.0025f;
            oscPhase += kickF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return sinf(2.f * 3.14159265f * oscPhase) * env * 0.9f;
        }
        case 6: {
            noiseState = noiseState * 1103515245u + 12345u;
            float n = (float)(noiseState >> 16) / 32768.f - 1.f;
            oscPhase += 185.f * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return (n * 0.55f + sinf(2.f * 3.14159265f * oscPhase) * 0.35f) * env;
        }
        case 7: {
            noiseState = noiseState * 1103515245u + 12345u;
            float n = (float)(noiseState >> 16) / 32768.f - 1.f;
            float hp = n - prevNoise;
            prevNoise = n;
            return hp * env * 0.45f;
        }
        case 8: {
            oscPhase += 214.f * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            fmPhase += 637.f * fsr;
            if (fmPhase >= 1.f) fmPhase -= 1.f;
            return sinf(2.f * 3.14159265f * oscPhase +
                2.6f * sinf(2.f * 3.14159265f * fmPhase)) * env * 0.55f;
        }
        case 9: {
            zapF += (70.f - zapF) * 0.004f;
            oscPhase += zapF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return sinf(2.f * 3.14159265f * oscPhase) * env * 0.7f;
        }
    }
    return 0.f;
}

float AudioEngine::pitchFactor() const {
    float f = powf(2.f, (float)synOct.load());
    f *= powf(2.f, synPitch.load() / 12.f);
    f *= powf(2.f, synTune.load() * 100.f / 1200.f);
    return f;
}

float AudioEngine::synthSample(int t, float fsr) {
    if (envAtkRate > 0.f) {
        env += envAtkRate * fsr;
        if (env >= 1.f) { env = 1.f; envAtkRate = 0.f; }
    } else if (env > 0.f) {
        env -= envRate * fsr;
        if (env < 0.f) env = 0.f;
    }

    int w = synWave.load();
    if (w >= 0) {
        oscPhase += baseF * fsr;
        if (oscPhase >= 1.f) oscPhase -= 1.f;
        float a = 0.f;
        switch (w) {
            case 1: a = (oscPhase < 0.5f ? 1.f : -1.f) * 0.32f; break;
            case 2: a = sinf(2.f * 3.14159265f * oscPhase) * 0.55f; break;
            case 3: a = (oscPhase < 0.5f ? oscPhase * 4.f - 1.f : 3.f - 4.f * oscPhase) * 0.45f; break;
            default: a = (oscPhase * 2.f - 1.f) * 0.45f; break;
        }
        return a * env;
    }

    switch (t) {
        case 1: {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return (oscPhase * 2.f - 1.f) * 0.45f * env;
        }
        case 2: {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return (oscPhase < 0.5f ? 1.f : -1.f) * 0.32f * env;
        }
        case 3: {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return sinf(2.f * 3.14159265f * oscPhase) * 0.55f * env;
        }
        default: {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return (oscPhase * 2.f - 1.f) * 0.5f * env;
        }
    }
}

void AudioEngine::arpFire() {
    int mode = arpMode.load(); if (mode < 0 || mode > 4) mode = 0;
    int oct = arpOct.load(); if (oct < 1) oct = 1; if (oct > 4) oct = 4;
    int s = scale.load(); if (s < 0 || s > 5) s = 0;
    int len = 0;
    while (len < 12 && scaleTable[s][len] >= 0) len++;
    if (len == 0) len = 12;
    int count = len * oct;

    switch (mode) {
        case 0: arpIdx = (arpIdx + 1) % count; break;
        case 1: arpIdx = (arpIdx - 1 + count) % count; break;
        case 2:
            if (count > 1) {
                arpIdx += arpDir;
                if (arpIdx >= count) { arpIdx = count - 2; arpDir = -1; }
                else if (arpIdx < 0) { arpIdx = 1; arpDir = 1; }
            } else arpIdx = 0;
            break;
        case 4: {
            float xy = arpXy.load();
            int i = (int)(xy * count);
            if (i >= count) i = count - 1;
            arpIdx = i;
            break;
        }
        default:
            noiseState = noiseState * 1103515245u + 12345u;
            arpIdx = (int)((noiseState >> 16) % (unsigned)count);
            break;
    }

    int t = genType.load();
    if (t >= 1 && t <= 4) {
        int deg = arpIdx;
        if (arpOctJump.load()) {
            noiseState = noiseState * 1103515245u + 12345u;
            if ((noiseState >> 16) % 4u == 0u) deg += len;
        }
        baseF = 55.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor();
        fmPhase = 0.f;
        static const float gates[4] = {0.25f, 0.5f, 0.75f, 1.f};
        int gi = arpGate.load();
        if (gi < 0 || gi > 3) gi = 1;
        static const float divs[4] = {1.f, 2.f, 4.f, 8.f};
        int ri = arpRate.load();
        if (ri < 0 || ri > 3) ri = 1;
        float stepDur = (60.f / fmaxf(bpm.load(), 40.f)) / divs[ri];
        float dec = synDecay.load();
        if (dec < 0.25f) dec = 0.25f;
        if (dec > 4.f) dec = 4.f;
        envRate = 1.f / (gates[gi] * stepDur * fs) / dec;
        float atk = synAtk.load();
        if (atk > 0.002f) {
            env = 0.f;
            envAtkRate = 1.f / (atk * 0.5f * fs);
        } else {
            env = 1.f;
            envAtkRate = 0.f;
        }
    }

    if (mode == 4) return;

    int slices = arpSlices.load();
    if (slices < 2) slices = 2;
    if (slices > 16) slices = 16;
    int sIdx = arpIdx % slices;
    for (int k = 0; k < kNumBanks; k++) {
        LoopBank& lb = banks[k];
        if (!lb.playing || lb.samples <= 0) continue;
        int target = (int)((float)sIdx / slices * lb.samples);
        if (target >= lb.samples) target = lb.samples - 1;
        lb.pos = target;
    }
}

bool AudioEngine::start(bool useMic) {
    mic = useMic;

    if (mic) {
        oboe::AudioStreamBuilder ib;
        ib.setDirection(oboe::Direction::Input)
          ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
          ->setSampleRate(48000)
          ->setChannelCount(1)
          ->setFormat(oboe::AudioFormat::Float);
        if (ib.openStream(in) != oboe::Result::OK) return false;
    }

    oboe::AudioStreamBuilder ob;
    ob.setDirection(oboe::Direction::Output)
      ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
      ->setSharingMode(oboe::SharingMode::Exclusive)
      ->setSampleRate(48000)
      ->setChannelCount(2)
      ->setFormat(oboe::AudioFormat::Float)
      ->setDataCallback(this);
    if (ob.openStream(out) != oboe::Result::OK) {
        ob.setSharingMode(oboe::SharingMode::Shared);
        if (ob.openStream(out) != oboe::Result::OK) return false;
    }

    out->setBufferSizeInFrames(out->getFramesPerBurst() * 2);
    fs = (float)out->getSampleRate();
    reverbL.init(fs, 1.f);
    reverbR.init(fs, 1.017f);
    micBuf.assign(4096, 0.f);

    lfo.reset();
    lfo.setF0(2.f, 1.f / fs);
    filter.flush();

    if (in) in->requestStart();
    return out->requestStart() == oboe::Result::OK;
}

void AudioEngine::stop() {
    if (out) { out->stop(); out->close(); out.reset(); }
    if (in)  { in->stop();  in->close();  in.reset(); }
}

float AudioEngine::runFilter(float xn) {
    return filter.process(xn);
}

void AudioEngine::applyAssign(int p, float v, float& fc, float& q, float& lr, float& ld,
                              float& rw, float& bt, float& rt, float& dr) {
    if (v < 0.f) v = 0.f; if (v > 1.f) v = 1.f;
    switch (p) {
        case 0: fc = 80.f * powf(100.f, v); break;
        case 1: q = 0.7f + v * 12.f; break;
        case 2: rw = v; break;
        case 3: bt = 1.f + (float)((int)(v * 15.f + 0.5f)); break;
        case 4: rt = 1.f + (float)((int)(v * 31.f + 0.5f)); break;
        case 5: lr = 0.05f * powf(200.f, v); break;
        case 6: ld = v; break;
        case 7: dr = 1.f + v * v * 8.f; break;
        default: break;
    }
}

float AudioEngine::clfoVal(int idx) {
    float ph = clfoPhase[idx];
    if (ph < 0.f) ph = 0.f;
    if (ph > 0.9999f) ph = 0.9999f;
    switch (clfoWave[idx].load(std::memory_order_relaxed)) {
        case 0: return sinf(2.f * 3.14159265f * ph);
        case 1: return 1.f - 4.f * fabsf(ph - 0.5f);
        case 2: return 2.f * ph - 1.f;
        case 3: return (ph < 0.5f) ? -1.f : 1.f;
        default: {
            float pos = ph * 63.f;
            int i0 = (int)pos;
            float fr = pos - (float)i0;
            float a = clfoShape[idx * 64 + i0].load(std::memory_order_relaxed);
            float b = clfoShape[idx * 64 + i0 + 1].load(std::memory_order_relaxed);
            return 2.f * (a + (b - a) * fr) - 1.f;
        }
    }
}

void AudioEngine::recordBank(int bank, int steps, bool wet) {
    if (bank < 0 || bank >= kNumBanks) return;
    if (steps < 1) steps = 1;
    if (steps > kMaxSteps) steps = kMaxSteps;
    float b = fmaxf(bpm.load(), 40.f);
    int n = (int)((60.f / b) * steps * fs);
    if (n < 4800) n = 4800;
    LoopBank& lb = banks[bank];
    if (!lb.buf || lb.cap < n) {
        int extra = n - (lb.samples > 0 ? lb.samples : 0);
        if (totalFrames + extra > kTotalFrameBudget) return;
        totalFrames += extra;
        delete[] lb.buf;
        lb.cap = n;
        lb.buf = new float[(size_t)n * 2];
    }
    lb.samples = n;
    lb.pos = 0;
    lb.playing = false;
    lb.steps = steps;
    for (int s = 0; s < kMaxSteps; s++) { lb.stepPitch[s] = 0.f; lb.stepVel[s] = 1.f; }
    lb.acc = 0.f;
    recBank = bank;
    recWet = wet;
}

void AudioEngine::startResample(int bank, int steps) {
    if (bank < 0 || bank >= kNumBanks) return;
    if (resBank >= 0) return;
    if (steps < 1) steps = 1;
    if (steps > kMaxSteps) steps = kMaxSteps;
    float b = fmaxf(bpm.load(), 40.f);
    int n = (int)((60.f / b) * steps * fs);
    if (n < 4800) n = 4800;
    LoopBank& lb = banks[bank];
    if (lb.samples > 0) return;
    if (!lb.buf || lb.cap < n) {
        if (totalFrames + n > kTotalFrameBudget) return;
        totalFrames += n;
        delete[] lb.buf;
        lb.cap = n;
        lb.buf = new float[(size_t)n * 2];
    }
    lb.samples = n;
    lb.pos = 0;
    lb.playing = false;
    lb.steps = steps;
    for (int s = 0; s < kMaxSteps; s++) { lb.stepPitch[s] = 0.f; lb.stepVel[s] = 1.f; }
    lb.acc = 0.f;
    resBank = bank;
    resPos = 0;
}

void AudioEngine::setStep(int bank, int step, float pitch, float vel) {
    if (bank < 0 || bank >= kNumBanks) return;
    if (step < 0 || step >= kMaxSteps) return;
    LoopBank& lb = banks[bank];
    if (lb.steps <= step) lb.steps = step + 1;
    lb.stepPitch[step] = pitch;
    lb.stepVel[step] = vel;
}

void AudioEngine::setStepCount(int bank, int n) {
    if (bank < 0 || bank >= kNumBanks) return;
    if (n < 0) n = 0;
    if (n > kMaxSteps) n = kMaxSteps;
    LoopBank& lb = banks[bank];
    lb.steps = n;
    for (int s = 0; s < kMaxSteps; s++) { lb.stepPitch[s] = 0.f; lb.stepVel[s] = 1.f; }
    lb.acc = 0.f;
}

void AudioEngine::toggleBank(int bank) {
    if (bank < 0 || bank >= kNumBanks) return;
    if (banks[bank].samples > 0) banks[bank].playing = !banks[bank].playing;
}

bool AudioEngine::bankHasLoop(int bank) const {
    if (bank < 0 || bank >= kNumBanks) return false;
    return banks[bank].samples > 0;
}

int AudioEngine::bankFrames(int bank) const {
    if (bank < 0 || bank >= kNumBanks) return 0;
    return banks[bank].samples;
}

int AudioEngine::copyBank(int bank, float* dst, int maxFrames) const {
    if (bank < 0 || bank >= kNumBanks || !dst) return 0;
    const LoopBank& lb = banks[bank];
    int n = lb.samples < maxFrames ? lb.samples : maxFrames;
    if (n > 0 && lb.buf) memcpy(dst, lb.buf, (size_t)n * 2 * sizeof(float));
    return n;
}

void AudioEngine::loadBank(int bank, const float* src, int frames) {
    if (bank < 0 || bank >= kNumBanks || !src || frames <= 0) return;
    LoopBank& lb = banks[bank];
    if (!lb.buf || lb.cap < frames) {
        int extra = frames - (lb.samples > 0 ? lb.samples : 0);
        if (totalFrames + extra > kTotalFrameBudget) return;
        totalFrames += extra;
        delete[] lb.buf;
        lb.cap = frames;
        lb.buf = new float[(size_t)frames * 2];
    }
    memcpy(lb.buf, src, (size_t)frames * 2 * sizeof(float));
    lb.samples = frames;
    lb.pos = 0;
    lb.playing = true;
}

void AudioEngine::jumpSlice(int slice, int total) {
    if (total <= 0) return;
    loopSliceTotal.store(total);
    loopSliceReq.store(slice - 1);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
        oboe::AudioStream*, void* audioData, int32_t numFrames) {
    float* o = static_cast<float*>(audioData);

    float* src = nullptr;
    if (mic && in) {
        if ((int)micBuf.size() < numFrames) micBuf.resize(numFrames);
        auto r = in->read(micBuf.data(), numFrames, 0);
        int32_t got = (r.error() == oboe::Result::OK) ? r.value() : 0;
        if (got < numFrames)
            memset(micBuf.data() + got, 0, (numFrames - got) * sizeof(float));
        src = micBuf.data();
    }

    const float fsr = 1.f / fs;
    const int fType = filterType.load();

    if (arpOn.load()) {
        float b = fmaxf(bpm.load(), 40.f);
        static const float divs[4] = {1.f, 2.f, 4.f, 8.f};
        int r = arpRate.load();
        if (r < 0 || r > 3) r = 1;
        float stepDur = (60.f / b) / divs[r];
        arpClock += fsr;
        if (arpClock >= stepDur) {
            arpClock -= stepDur;
            arpFire();
        }
    }

    for (int i = 0; i < numFrames; i++) {
        float inS;
        if (mic) {
            inS = src[i];
        } else {
            inS = genSample(fsr, fmaxf(bpm.load(), 40.f));
        }

        if ((coeffClock++ & 31) == 0) {
            float fcT = 80.f * powf(100.f, clamp01(ax.load()));
            float qT = 0.7f + clamp01(ay.load()) * 12.f;
            float lrT = 0.05f * powf(200.f, clamp01(bx.load()));
            float ldT = clamp01(by.load());
            float rwT = 0.f, btT = 16.f, rtT = 1.f, drT = 1.f;
            applyAssign(assignX.load(), ax.load(), fcT, qT, lrT, ldT, rwT, btT, rtT, drT);
            applyAssign(assignY.load(), ay.load(), fcT, qT, lrT, ldT, rwT, btT, rtT, drT);

            float baseNorm[8] = {
                clamp01(ax.load()), clamp01(ay.load()), 0.f, 1.f,
                0.f, clamp01(bx.load()), clamp01(by.load()), 0.f
            };
            int axn = assignX.load();
            int ayn = assignY.load();
            if (axn >= 0 && axn < 8) baseNorm[axn] = clamp01(ax.load());
            if (ayn >= 0 && ayn < 8) baseNorm[ayn] = clamp01(ay.load());
            static const float clfoRates[6] = {0.25f, 0.5f, 1.f, 2.f, 4.f, 8.f};
            for (int L = 0; L < 2; L++) {
                if (!clfoOn[L].load(std::memory_order_relaxed)) continue;
                int ri = clfoRate[L].load(std::memory_order_relaxed);
                if (ri < 0) ri = 0;
                if (ri > 5) ri = 5;
                clfoPhase[L] += clfoRates[ri] * 32.f / fs;
                if (clfoPhase[L] >= 1.f) clfoPhase[L] -= 1.f;
                int tg = clfoTarget[L].load(std::memory_order_relaxed);
                if (tg < 0 || tg > 7) continue;
                float dep = clfoDepth[L].load(std::memory_order_relaxed);
                float amt = clfoAmount[L].load(std::memory_order_relaxed);
                applyAssign(tg, clamp01(baseNorm[tg] + dep * amt * clfoVal(L)),
                            fcT, qT, lrT, ldT, rwT, btT, rtT, drT);
            }

            if (bend.load()) {
                float bxv = clamp01(bendX.load());
                float byv = clamp01(bendLevel.load());
                float wSoft = (1.f - bxv) * (1.f - byv);
                float wBlip = bxv * (1.f - byv);
                float wRand = (1.f - bxv) * byv;
                float wStorm = bxv * byv;
                float chaos = wRand + wStorm;
                blipW = wBlip;

                noiseState = noiseState * 1103515245u + 12345u;
                float fr = (float)(noiseState >> 16) / 65535.f;
                bendWalk += (fr - 0.5f) * (0.02f + 0.14f * chaos);
                if (bendWalk > 0.7f) bendWalk = 0.7f;
                if (bendWalk < -0.7f) bendWalk = -0.7f;
                fcT *= 1.f + bendWalk * (0.15f + 0.85f * chaos);
                qT += fabsf(bendWalk) * (2.f + 10.f * wStorm);
                drT += wStorm * 6.f;
                blipInc = (3.f + 25.f * bxv) / fs;

                if (++bendTicks >= bendNext) {
                    bendTicks = 0;
                    noiseState = noiseState * 1103515245u + 12345u;
                    unsigned int r = noiseState >> 16;
                    bendNext = 20 + (int)((1.f - chaos) * 400.f) + (int)(r % 200u);
                    if (chaos > 0.2f && (r % 100u) < (unsigned)(chaos * 100.f)) {
                        filterType.store((int)(r % 4u));
                        fcT = 80.f * powf(100.f, (float)(r % 97u) / 97.f);
                    }
                    if (wRand > 0.15f) {
                        btT = fminf(btT, 1.f + (float)(r % 12u));
                        rtT = fmaxf(rtT, (float)(r % 20u) + 1.f);
                    }
                    if (wStorm > 0.15f) {
                        btT = fminf(btT, 2.f + (float)(r % 3u));
                        rtT = fmaxf(rtT, (float)(r % 16u) + 1.f);
                    }
                    rwT = fmaxf(rwT, chaos * 0.8f * (float)(r % 89u) / 89.f);
                }
            } else {
                bendTicks = 0;
                bendNext = 100;
                blipInc = 0.f;
                blipW = 0.f;
            }
            bendDriveS = drT;

            fcSmoothed += 0.2f * (fcT - fcSmoothed);
            qSmoothed  += 0.2f * (qT - qSmoothed);
            lfoRateSmoothed  += 0.2f * (lrT - lfoRateSmoothed);
            lfoDepthSmoothed += 0.2f * (ldT - lfoDepthSmoothed);
            revWetS += 0.2f * (rwT - revWetS);
            crushBitsS = btT;
            crushRateS = rtT;

            float fc = fcSmoothed * (1.f + lfoDepthSmoothed * lfo.sine_bi());
            fc = fminf(fmaxf(fc, 20.f), fs * 0.45f);
            float k = tanf(3.14159265f * fc * fsr);
            if (k > 0.99f) k = 0.99f;
            float q = qSmoothed;

            switch (fType) {
                case 1: filter.mCoeffs.setSOHP(k, q); break;
                case 2: filter.mCoeffs.setSOBP(k, q); break;
                case 3: filter.mCoeffs.setSOBR(k, q); break;
                default: filter.mCoeffs.setSOLP(k, q); break;
            }
        }
        float m = clamp01(mix.load());
        float smp = inS;
        if (m > 0.001f) {
            if (lfoDepthSmoothed > 0.001f) lfo.cycle();
            float wet = runFilter(inS);
            wet = tanhf(wet * 1.5f) * 0.8f;
            smp = inS * (1.f - m) + wet * m;
        }

        if (bendDriveS > 1.01f) smp = tanhf(smp * bendDriveS) * 0.8f;

        if (blipInc > 0.f) {
            blipPhase += blipInc;
            if (blipPhase >= 1.f) blipPhase -= 1.f;
            float g = (blipPhase < 0.55f) ? 1.f : 0.12f;
            smp *= 1.f - blipW * (1.f - g);
        }

        crushHold++;
        if (crushHold >= (int)crushRateS) {
            crushHold = 0;
            crushHeld = smp;
        }
        if (crushBitsS < 15.5f || crushRateS > 1.5f) {
            float lvq = powf(2.f, crushBitsS - 1.f);
            smp = floorf(crushHeld * lvq + 0.5f) / lvq;
            if (smp > 1.f) smp = 1.f;
            if (smp < -1.f) smp = -1.f;
        }

        if (revWetS > 0.002f) {
            float rl = reverbL.process(smp);
            float rr = reverbR.process(smp);
            smp = smp * (1.f - revWetS) + (rl + rr) * revWetS;
        }

        float liveL = smp, liveR = smp;

        if (recBank >= 0) {
            LoopBank& lb = banks[recBank];
            float c = recWet ? smp : inS;
            lb.buf[2 * lb.pos] = c;
            lb.buf[2 * lb.pos + 1] = c;
            lb.pos++;
            if (lb.pos >= lb.samples) {
                lb.playing = true;
                lb.pos = 0;
                recBank = -1;
            }
        }

        float loopL = 0.f, loopR = 0.f;
        int req = loopSliceReq.exchange(-1);
        int sm = soloMask.load();
        bool xyT = arpOn.load() && arpMode.load() == 4 && arpXyTouch.load();
        float xy = arpXy.load();
        int xySlices = arpSlices.load();
        if (xySlices < 2) xySlices = 2;
        if (xySlices > 16) xySlices = 16;
        int xySlice = -1;
        if (xyT) {
            xySlice = (int)(xy * xySlices);
            if (xySlice >= xySlices) xySlice = xySlices - 1;
            if (xySlice != xyLastSlice) xyLastSlice = xySlice;
        } else {
            xyLastSlice = -1;
        }
        float rate = xyT ? powf(2.f, (ay.load() - 0.5f) * 2.f) : 1.f;
        for (int k = 0; k < kNumBanks; k++) {
            LoopBank& lb = banks[k];
            if (!lb.playing || lb.samples <= 0) continue;
            if (sm != 0 && (sm & (1 << (k / 2))) == 0) continue;
            if (req >= 0) {
                int tot = loopSliceTotal.load();
                if (tot > 0) lb.pos = (int)((float)req / tot * lb.samples);
                if (lb.pos >= lb.samples) lb.pos = 0;
            }
            int sStart = 0, sEnd = lb.samples;
            if (xyT) {
                sStart = (int)((float)xySlice / xySlices * lb.samples);
                sEnd = (int)((float)(xySlice + 1) / xySlices * lb.samples);
                if (sEnd <= sStart) sEnd = sStart + 1;
                if (lb.pos < sStart || lb.pos >= sEnd) lb.pos = sStart;
            }
            int si = -1;
            if (!xyT && lb.steps > 0) {
                si = (int)((float)lb.pos / lb.samples * lb.steps);
                if (si >= lb.steps) si = lb.steps - 1;
            }
            float vel = si >= 0 ? lb.stepVel[si] : 1.f;
            float prate = 1.f;
            if (si >= 0 && lb.stepPitch[si] != 0.f) prate = powf(2.f, lb.stepPitch[si] / 12.f);
            loopL += lb.buf[2 * lb.pos] * vel;
            loopR += lb.buf[2 * lb.pos + 1] * vel;
            if (xyT) {
                if (rate != 1.f) {
                    xyAcc += rate;
                    lb.pos += (int)xyAcc;
                    xyAcc -= (int)xyAcc;
                } else {
                    lb.pos += 1;
                }
            } else {
                lb.acc += prate;
                lb.pos += (int)lb.acc;
                lb.acc -= (int)lb.acc;
            }
            if (xyT) {
                if (lb.pos >= sEnd) lb.pos = sStart + ((lb.pos - sStart) % (sEnd - sStart));
            } else if (lb.pos >= lb.samples) {
                lb.pos %= lb.samples;
            }
        }

        float history = liveL;
        stutterBuf[stutterWrite] = history;
        stutterWrite = (stutterWrite + 1) % kStutterMax;

        bool st = stutter.load();
        if (st && !prevStutter) {
            float b = fmaxf(bpm.load(), 40.f);
            stutterWindow = (int)(0.5f * (60.f / b) * fs);
            if (stutterWindow > kStutterMax / 2) stutterWindow = kStutterMax / 2;
            if (stutterWindow < 64) stutterWindow = 64;
            stutterLoopStart = (stutterWrite - stutterWindow + kStutterMax) % kStutterMax;
            stutterPos = 0;
        }
        prevStutter = st;

        if (st) {
            int idx = (stutterLoopStart + stutterPos) % kStutterMax;
            int fade = stutterWindow / 8;
            if (fade > 256) fade = 256;
            float g = 1.f;
            if (stutterPos < fade) g = (float)stutterPos / fade;
            else if (stutterPos > stutterWindow - fade)
                g = (float)(stutterWindow - stutterPos) / fade;
            float loop = stutterBuf[idx] * g;
            liveL = loop; liveR = loop;
            stutterPos = (stutterPos + 1) % stutterWindow;
        }

        float mTarget = mute.load() ? 0.f : 1.f;
        muteGain += 0.0025f * (mTarget - muteGain);
        liveL *= muteGain; liveR *= muteGain;

        float outL = liveL + loopL * kLoopGain;
        float outR = liveR + loopR * kLoopGain;
        if (outL > 1.f) outL = 1.f; if (outL < -1.f) outL = -1.f;
        if (outR > 1.f) outR = 1.f; if (outR < -1.f) outR = -1.f;

        if (resBank >= 0) {
            LoopBank& rb = banks[resBank];
            if (rb.buf) {
                rb.buf[2 * resPos] = outL;
                rb.buf[2 * resPos + 1] = outR;
            }
            resPos++;
            if (resPos >= rb.samples) {
                rb.playing = true;
                rb.pos = 0;
                resBank = -1;
            }
        }

        float pL = fabsf(outL), pR = fabsf(outR);
        float cl = levelL.load(), cr = levelR.load();
        levelL.store(cl + (pL > cl ? 0.5f : 0.02f) * (pL - cl));
        levelR.store(cr + (pR > cr ? 0.5f : 0.02f) * (pR - cr));

        o[2 * i] = outL;
        o[2 * i + 1] = outR;
    }

    return oboe::DataCallbackResult::Continue;
}
