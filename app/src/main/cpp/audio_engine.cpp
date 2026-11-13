#include "audio_engine.h"
#include <cmath>
#include <cstring>

static inline float clamp01(float v) { return v < 0.f ? 0.f : (v > 1.f ? 1.f : v); }

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

void AudioEngine::recordLoop(int steps) {
    float b = fmaxf(bpm.load(), 40.f);
    int n = (int)((60.f / b) * steps * fs);
    if (n < 4800) n = 4800;
    if (!loopBuf || loopCap < n) {
        delete[] loopBuf;
        loopCap = n;
        loopBuf = new float[(size_t)n * 2];
    }
    loopSamples = n;
    loopRecPos = 0;
    loopPos = 0;
    loopPlaying.store(false);
    loopRecording.store(true);
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

    for (int i = 0; i < numFrames; i++) {
        float inS;
        if (mic) {
            inS = src[i];
        } else {
            inS = (sawPhase * 2.f - 1.f) * 0.4f;
            sawPhase += 110.f * fsr;
            if (sawPhase >= 1.f) sawPhase -= 1.f;
        }

        if ((coeffClock++ & 31) == 0) {
            float tAx = clamp01(ax.load());
            float tAy = clamp01(ay.load());
            float tBx = clamp01(bx.load());
            float tBy = clamp01(by.load());

            fcSmoothed += 0.2f * (80.f * powf(100.f, tAx) - fcSmoothed);
            qSmoothed  += 0.2f * (0.7f + tAy * 12.f - qSmoothed);
            lfoRateSmoothed  += 0.2f * (0.05f * powf(200.f, tBx) - lfoRateSmoothed);
            lfoDepthSmoothed += 0.2f * (tBy - lfoDepthSmoothed);

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
        lfo.cycle();

        float wet = runFilter(inS);
        wet = tanhf(wet * 1.5f) * 0.8f;

        float m = clamp01(mix.load());
        float smp = inS * (1.f - m) + wet * m;

        float liveL = smp, liveR = smp;

        if (loopRecording.load()) {
            loopBuf[2 * loopRecPos] = liveL;
            loopBuf[2 * loopRecPos + 1] = liveR;
            loopRecPos++;
            if (loopRecPos >= loopSamples) {
                loopRecording.store(false);
                loopPlaying.store(true);
                loopPos = 0;
            }
        }

        float loopL = 0.f, loopR = 0.f;
        if (loopPlaying.load()) {
            int req = loopSliceReq.exchange(-1);
            if (req >= 0) {
                int tot = loopSliceTotal.load();
                if (tot > 0) loopPos = (int)((float)req / tot * loopSamples);
                if (loopPos >= loopSamples) loopPos = 0;
            }
            loopL = loopBuf[2 * loopPos];
            loopR = loopBuf[2 * loopPos + 1];
            loopPos = (loopPos + 1) % loopSamples;
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

        float pL = fabsf(outL), pR = fabsf(outR);
        float cl = levelL.load(), cr = levelR.load();
        levelL.store(cl + (pL > cl ? 0.5f : 0.02f) * (pL - cl));
        levelR.store(cr + (pR > cr ? 0.5f : 0.02f) * (pR - cr));

        o[2 * i] = outL;
        o[2 * i + 1] = outR;
    }

    return oboe::DataCallbackResult::Continue;
}
