#pragma once

#include <oboe/Oboe.h>
#include <memory>

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    bool start();
    void stop();

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames
    ) override;

private:
    std::shared_ptr<oboe::AudioStream> outputStream;
    float phase = 0.0f;
    float sampleRate = 48000.0f;
};
