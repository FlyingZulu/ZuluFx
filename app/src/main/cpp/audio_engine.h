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
    void setAx(float v) { ax.store(v); }
    void setAy(float v) { ay.store(v); }
    void setBx(float v) { bx.store(v); }
    void setBy(float v) { by.store(v); }
    void setMix(float v) { mix.store(v); }
    void setMute(bool m) { mute.store(m); }
    void setFly(float v) { fly.store(v); }
    void setStutter(bool s) { stutter.store(s); }
    void setBpm(float b) { bpm.store(b); }

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream, void* audioData, int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> out, in;
    dsp::BiQuad filter;
    dsp::SimpleLFO lfo;

    std::atomic<int> filterType{0};
    std::atomic<float> ax{0.5f}, ay{0.3f}, bx{0.5f}, by{0.2f};
    std::atomic<float> mix{1.0f};
    std::atomic<bool> mute{false};
    std::atomic<float> fly{0.0f};
    std::atomic<bool> stutter{false};
    std::atomic<float> bpm{120.0f};
    std::atomic<float> levelL{0.0f}, levelR{0.0f};

    bool mic = false;
    float fs = 48000.0f;
    float sawPhase = 0.0f;
    float fcSmoothed = 1000.0f;
    float qSmoothed = 0.7f;
    float lfoRateSmoothed = 2.0f;
    float lfoDepthSmoothed = 0.2f;
    float muteGain = 1.0f;
    int coeffClock = 0;

    bool prevStutter = false;
    int stutterPos = 0;
    int stutterLoopStart = 0;
    int stutterWindow = 0;
    static constexpr int kStutterMax = 48000 * 2;
    float stutterBuf[kStutterMax];
    std::vector<float> micBuf;

    float runFilter(float xn);
};
