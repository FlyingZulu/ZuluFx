#pragma once
#include <oboe/Oboe.h>
#include <atomic>
#include <vector>
#include "dsp/biquad.hpp"
#include "dsp/simplelfo.hpp"

struct FreeVerb {
    float* comb[8] = {nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr};
    int combSize[8] = {0};
    int combIdx[8] = {0};
    float combFilt[8] = {0};
    float* ap[4] = {nullptr, nullptr, nullptr, nullptr};
    int apSize[4] = {0};
    int apIdx[4] = {0};

    ~FreeVerb() {
        for (int i = 0; i < 8; i++) delete[] comb[i];
        for (int i = 0; i < 4; i++) delete[] ap[i];
    }

    void init(float fs, float scale) {
        static const int combT[8] = {1557, 1617, 1491, 1422, 1277, 1356, 1188, 1116};
        static const int apT[4] = {556, 441, 341, 225};
        float s = fs / 44100.f * scale;
        for (int i = 0; i < 8; i++) {
            combSize[i] = (int)(combT[i] * s);
            comb[i] = new float[combSize[i]]();
            combIdx[i] = 0;
            combFilt[i] = 0.f;
        }
        for (int i = 0; i < 4; i++) {
            apSize[i] = (int)(apT[i] * s);
            ap[i] = new float[apSize[i]]();
            apIdx[i] = 0;
        }
    }

    float process(float x) {
        const float damp1 = 0.2f, damp2 = 0.8f, fb = 0.72f;
        float input = x * 0.015f;
        float out = 0.f;
        for (int i = 0; i < 8; i++) {
            float y = comb[i][combIdx[i]];
            combFilt[i] = y * damp2 + combFilt[i] * damp1;
            comb[i][combIdx[i]] = input + combFilt[i] * fb;
            if (++combIdx[i] >= combSize[i]) combIdx[i] = 0;
            out += y;
        }
        out *= 0.25f;
        for (int i = 0; i < 4; i++) {
            float bufout = ap[i][apIdx[i]];
            float output = -out + bufout;
            ap[i][apIdx[i]] = out + bufout * 0.5f;
            if (++apIdx[i] >= apSize[i]) apIdx[i] = 0;
            out = output;
        }
        return out;
    }
};

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    bool start(bool useMic);
    void stop();

    void setFilterType(int t) { filterType.store(t); }
    void setGen(int t) { genType.store(t); }
    void setScale(int s) { scale.store(s); }
    void setRoot(int r) { rootNote.store(r); }
    void setAx(float v) { ax.store(v); }
    void setAy(float v) { ay.store(v); }
    void setBx(float v) { bx.store(v); }
    void setBy(float v) { by.store(v); }
    void setMix(float v) { mix.store(v); }
    void setMute(bool m) { mute.store(m); }
    void setFly(float v) { fly.store(v); }
    void setStutter(bool s) { stutter.store(s); }
    void setBpm(float b) { bpm.store(b); }
    void setArp(bool on, int mode, int rate, int oct, int gate, bool octJump) {
        arpOn.store(on); arpMode.store(mode); arpRate.store(rate);
        arpOct.store(oct); arpGate.store(gate); arpOctJump.store(octJump);
    }
    void setArpSlices(int n) { arpSlices.store(n); }
    void setArpXy(float x, bool t) { arpXy.store(x); arpXyTouch.store(t); }
    void setAssign(int axis, int p) { if (axis == 0) assignX.store(p); else assignY.store(p); }
    void setBend(bool b, float x, float level) {
        bend.store(b); bendX.store(x); bendLevel.store(level);
    }
    void setSoloMask(int m) { soloMask.store(m); }
    void setSynth(int wave, float atk, float decay, int oct, float pitch, float tune) {
        synWave.store(wave); synAtk.store(atk); synDecay.store(decay);
        synOct.store(oct); synPitch.store(pitch); synTune.store(tune);
    }

    void recordBank(int bank, int steps, bool wet);
    void startResample(int bank, int steps);
    void setStep(int bank, int step, float pitch, float vel);
    void setStepCount(int bank, int n);
    void toggleBank(int bank);
    bool bankHasLoop(int bank) const;
    void jumpSlice(int slice, int total);
    int bankFrames(int bank) const;
    int copyBank(int bank, float* dst, int maxFrames) const;
    void loadBank(int bank, const float* src, int frames);
    bool isLoopRecording() const { return recBank >= 0; }

    float getLevelL() const { return levelL.load(); }
    float getLevelR() const { return levelR.load(); }

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream, void* audioData, int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> out, in;
    dsp::BiQuad filter;
    dsp::SimpleLFO lfo;

    std::atomic<int> filterType{0};
    std::atomic<int> genType{0};
    std::atomic<int> scale{0};
    std::atomic<int> rootNote{9};
    std::atomic<float> ax{0.5f}, ay{0.3f}, bx{0.5f}, by{0.2f};
    std::atomic<float> mix{1.0f};
    std::atomic<bool> mute{false};
    std::atomic<float> fly{0.0f};
    std::atomic<bool> stutter{false};
    std::atomic<float> bpm{120.0f};
    std::atomic<bool> arpOn{false};
    std::atomic<int> arpMode{0};
    std::atomic<int> arpRate{1};
    std::atomic<int> arpOct{1};
    std::atomic<int> arpSlices{8};
    std::atomic<int> arpGate{1};
    std::atomic<bool> arpOctJump{false};
    std::atomic<int> soloMask{0};
    std::atomic<int> synWave{-1};
    std::atomic<float> synAtk{0.f};
    std::atomic<float> synDecay{1.f};
    std::atomic<int> synOct{0};
    std::atomic<float> synPitch{0.f};
    std::atomic<float> synTune{0.f};
    std::atomic<float> arpXy{0.5f};
    std::atomic<bool> arpXyTouch{false};
    std::atomic<int> assignX{0};
    std::atomic<int> assignY{1};
    std::atomic<bool> bend{false};
    std::atomic<float> bendX{0.5f};
    std::atomic<float> bendLevel{0.5f};
    std::atomic<float> levelL{0.0f}, levelR{0.0f};

    bool mic = false;
    float fs = 48000.0f;
    float sawPhase = 0.0f;
    int seqStep = 0;
    float stepClock = 0.f;
    float oscPhase = 0.f, fmPhase = 0.f, sirenPhase = 0.f;
    float env = 0.f, envRate = 0.f;
    float envAtkRate = 0.f;
    float baseF = 55.f;
    float kickF = 0.f, zapF = 0.f;
    float prevNoise = 0.f;
    unsigned int noiseState = 22222;

    FreeVerb reverbL, reverbR;
    float revWetS = 0.f;
    float crushBitsS = 16.f;
    float crushRateS = 1.f;
    float bendDriveS = 1.f;
    int crushHold = 0;
    float crushHeld = 0.f;
    int bendTicks = 0;
    int bendNext = 100;
    float bendWalk = 0.f;
    float blipPhase = 0.f;
    float blipInc = 0.f;
    float blipW = 0.f;
    void applyAssign(int p, float v, float& fc, float& q, float& lr, float& ld,
                     float& rw, float& bt, float& rt, float& dr);

    float genSample(float fsr, float bpm);
    float degreeToSemi(int deg) const;
    float pitchFactor() const;
    float synthSample(int t, float fsr);
    void arpFire();
    int arpIdx = 0;
    int arpDir = 1;
    float arpClock = 0.f;
    int xyLastSlice = -1;
    float xyAcc = 0.f;
    float fcSmoothed = 1000.0f;
    float qSmoothed = 0.7f;
    float lfoRateSmoothed = 2.0f;
    float lfoDepthSmoothed = 0.2f;
    float muteGain = 1.0f;
    int coeffClock = 0;

    bool prevStutter = false;
    int stutterWrite = 0;

    static constexpr int kNumBanks = 8;
    static constexpr int kMaxSteps = 14;
    struct LoopBank {
        float* buf = nullptr;
        int cap = 0;
        int samples = 0;
        int pos = 0;
        bool playing = false;
        int steps = 0;
        float stepPitch[kMaxSteps];
        float stepVel[kMaxSteps];
        float acc = 0.f;
        LoopBank() {
            for (int s = 0; s < kMaxSteps; s++) { stepPitch[s] = 0.f; stepVel[s] = 1.f; }
        }
    };
    static constexpr int kTotalFrameBudget = 6553600;
    LoopBank banks[kNumBanks];
    int totalFrames = 0;
    int recBank = -1;
    bool recWet = false;
    int resBank = -1;
    int resPos = 0;
    std::atomic<int> loopSliceReq{-1};
    std::atomic<int> loopSliceTotal{0};
    static constexpr float kLoopGain = 0.9f;
    int stutterPos = 0;
    int stutterLoopStart = 0;
    int stutterWindow = 0;
    static constexpr int kStutterMax = 48000 * 2;
    float stutterBuf[kStutterMax];
    std::vector<float> micBuf;

    float runFilter(float xn);
};
