#pragma once
#include <oboe/Oboe.h>
#include <atomic>
#include <vector>
#include "dsp/biquad.hpp"
#include "dsp/simplelfo.hpp"

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

    void recordBank(int bank, int steps, bool wet);
    void toggleBank(int bank);
    bool bankHasLoop(int bank) const;
    void jumpSlice(int slice, int total);
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
    std::atomic<float> levelL{0.0f}, levelR{0.0f};

    bool mic = false;
    float fs = 48000.0f;
    float sawPhase = 0.0f;
    int seqStep = 0;
    float stepClock = 0.f;
    float oscPhase = 0.f, fmPhase = 0.f, sirenPhase = 0.f;
    float env = 0.f, envRate = 0.f;
    float baseF = 55.f;
    float kickF = 0.f, zapF = 0.f;
    float prevNoise = 0.f;
    unsigned int noiseState = 22222;

    float genSample(float fsr, float bpm);
    float degreeToSemi(int deg) const;
    float synthSample(int t, float fsr);
    void arpFire();
    int arpIdx = 0;
    int arpDir = 1;
    float arpClock = 0.f;
    float fcSmoothed = 1000.0f;
    float qSmoothed = 0.7f;
    float lfoRateSmoothed = 2.0f;
    float lfoDepthSmoothed = 0.2f;
    float muteGain = 1.0f;
    int coeffClock = 0;

    bool prevStutter = false;
    int stutterWrite = 0;

    struct LoopBank {
        float* buf = nullptr;
        int cap = 0;
        int samples = 0;
        int pos = 0;
        bool playing = false;
    };
    static constexpr int kNumBanks = 8;
    static constexpr int kMaxSteps = 14;
    static constexpr int kTotalFrameBudget = 6553600;
    LoopBank banks[kNumBanks];
    int totalFrames = 0;
    int recBank = -1;
    bool recWet = false;
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
