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
    void setFx(int fx, bool on);
    void setParam(int p, float v, bool on);
    void setFxParam(int fx, int k, float v);
    void setBankFxSend(int bank, bool on);
    void undoBankRec();
    void cancelRec();
    void setRecFlavor(int f) { recFlavor.store(f); }

    AudioEngine() {
        for (int i = 0; i < kNumBanks; i++) bankFxSend[i] = true;
        for (int s = 0; s < kMaxSteps; s++) { undoStepPitch[s] = 0.f; undoStepVel[s] = 1.f; }
    }
    void setCustomLfo(int idx, const float* table, bool on, int rateIdx, float depth,
                      float amount, int target, int wave) {
        if (idx < 0 || idx > 1) return;
        for (int i = 0; i < 64; i++)
            clfoShape[idx * 64 + i].store(table[i], std::memory_order_relaxed);
        clfoOn[idx].store(on, std::memory_order_relaxed);
        clfoRate[idx].store(rateIdx, std::memory_order_relaxed);
        clfoDepth[idx].store(depth, std::memory_order_relaxed);
        clfoAmount[idx].store(amount, std::memory_order_relaxed);
        clfoTarget[idx].store(target, std::memory_order_relaxed);
        clfoWave[idx].store(wave, std::memory_order_relaxed);
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
    std::atomic<int> recFlavor{0};

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
    std::atomic<float> fxWet[37] = {};
    std::atomic<float> paramManual[8] = {};
    std::atomic<unsigned int> paramManualMask{0};
    static constexpr int kDelayMax = 144000;
    float delayBuf[kDelayMax * 2] = {0.f};
    float echoBuf[kDelayMax * 2] = {0.f};
    int delayPos = 0;
    int echoPos = 0;
    float delayLpL = 0.f, delayLpR = 0.f;
    float echoLpL = 0.f, echoLpR = 0.f;
    static constexpr int kModMax = 4096;
    float modBuf[kModMax * 2] = {0.f};
    float chorusBuf[kModMax * 2] = {0.f};
    int modPos = 0;
    int chorusPos = 0;
    float phAp1 = 0.f, phAp2 = 0.f, phAp3 = 0.f, phAp4 = 0.f;
    float phApR1 = 0.f, phApR2 = 0.f, phApR3 = 0.f, phApR4 = 0.f;
    float phLfo = 0.f, tremPhase = 0.f, panLfo = 0.f, flLfo = 0.f, choLfo = 0.f;
    float ringPhase = 0.f;
    int gateClock = 0;
    unsigned int lofiNoise = 22222u;
    float lofiLpL = 0.f, lofiLpR = 0.f;
    static constexpr int kWowMax = 2400;
    float wowBuf[kWowMax * 2] = {0.f};
    int wowPos = 0;
    float wowPhase = 0.f, flutPhase = 0.f;
    static constexpr int kStopMax = 72000;
    float stopBuf[kStopMax * 2] = {0.f};
    int stopWrite = 0;
    float stopRead = 0.f;
    float stopRate = 1.f;
    bool prevStop = false;
    static constexpr int kFreezeMax = 57600;
    float freezeBuf[kFreezeMax * 2] = {0.f};
    int freezeFill = 0;
    int freezePos = 0;
    bool freezeFull = false;
    bool prevFreeze = false;
    float compEnv = 0.f;
    static constexpr int kNumFx = 37;
    std::atomic<float> fxParam[kNumFx][8] = {};
    float fxMod[kNumFx][8] = {};
    float fxAxisVal[kNumFx][8] = {};
    unsigned char fxAxisMask[kNumFx] = {};
    std::atomic<int> playerBank{0};
    float plyPhase = 0.f;
    float granPos = 0.f, granT = 0.f, granStart = 0.f;
    float jglT = 0.f, jglStart = 0.f, jglRate = 1.f, jglRev = 0.f;
    float rndPhase = 0.f, rndRead = 0.f;
    int rndCur = 0, rndOff = 0, rndLen = 0;

    void setPlayerBank(int b) {
        if (b >= 0 && b < kNumBanks) playerBank.store(b);
    }
    static constexpr int kTurnMax = 24000;
    float turnBuf[kTurnMax * 2] = {0.f};
    int turnWrite = 0;
    int turnClock = 0;
    int turnSlot = 0;
    int turnHold = 0;
    float turnHeldL = 0.f, turnHeldR = 0.f;
    float turnPh = 0.f;
    float turnLpL = 0.f, turnLpR = 0.f;
    float turnLag = 0.f;
    float transitPh = 0.f;
    bool prevTransit = false;
    float trLpL = 0.f, trLpR = 0.f;
    float comWarmL = 0.f, comWarmR = 0.f;
    float fallPh = 0.f;
    bool prevFall = false;
    float faLpL = 0.f, faLpR = 0.f;
    float swellPh = 0.f;
    bool prevSwell = false;
    int pumpClock = 0;
    static constexpr int kDubMax = 96000;
    float dubBuf[kDubMax * 2] = {0.f};
    int dubPos = 0;
    float dubLpL = 0.f, dubLpR = 0.f;
    float megaLp1L = 0.f, megaLp1R = 0.f, megaLp2L = 0.f, megaLp2R = 0.f;
    float megaPh = 0.f;
    static constexpr int kTapeMax = 2400;
    float tapeBuf[kTapeMax * 2] = {0.f};
    int tapePos = 0;
    float tapePhase = 0.f;
    int tapeDropClock = 0;
    int tapeDropLen = 0;
    float tapeGain = 1.f;
    static constexpr int kWarpMax = 24000;
    float warpBuf[kWarpMax * 2] = {0.f};
    int warpWrite = 0;
    float warpRead = 0.f;
    float warpRate = 1.f;
    float warpTarget = 1.f;
    int warpClock = 0;
    bool prevWarp = false;
    float tkLp1L = 0.f, tkLp2L = 0.f, tkLp3L = 0.f, tkLp4L = 0.f;
    float tkLp1R = 0.f, tkLp2R = 0.f, tkLp3R = 0.f, tkLp4R = 0.f;
    float tkF1 = 800.f, tkF2 = 1200.f;
    int tkClock = 0;
    int tkVowel = 0;
    static constexpr int kRinMax = 2400;
    float rinBuf[kRinMax] = {0.f};
    int rinPos = 0;
    int rinLen = 480;
    float rinPrev = 0.f;
    float rinEnv = 0.f;
    int rinDebounce = 0;
    float drumA = 0.5f;
    float drumB = 0.5f;
    static constexpr int kGrainMax = 48000;
    float grainBuf[kGrainMax * 2] = {0.f};
    int gWrite = 0;
    float gPos[4] = {0.f, 0.f, 0.f, 0.f};
    float gLen[4] = {0.f, 0.f, 0.f, 0.f};
    float gAge[4] = {0.f, 0.f, 0.f, 0.f};
    float gRate[4] = {1.f, 1.f, 1.f, 1.f};
    bool gOn[4] = {false, false, false, false};
    float gPos2[4] = {0.f, 0.f, 0.f, 0.f};
    float gLen2[4] = {0.f, 0.f, 0.f, 0.f};
    float gAge2[4] = {0.f, 0.f, 0.f, 0.f};
    float gRate2[4] = {1.f, 1.f, 1.f, 1.f};
    bool gOn2[4] = {false, false, false, false};
    int gOff2 = 4800;
    static constexpr int kScrMax = 48000;
    float scrBuf[kScrMax * 2] = {0.f};
    int scrWrite = 0;
    float scrPh = 0.f;
    static constexpr int kRevMax = 24000;
    float revBuf[kRevMax * 2] = {0.f};
    int revWrite = 0;
    float revRead = 0.f;
    bool prevRev = false;
    static constexpr int kBeatMax = 48000;
    float bounceBuf[kBeatMax * 2] = {0.f};
    int bounceFill = 0;
    int bouncePos = 0;
    int bounceWin = 6000;
    bool bounceFull = false;
    bool prevBounce = false;
    static constexpr int kResoMax = 1200;
    float resoBuf[4][kResoMax] = {};
    int resoPos = 0;
    static constexpr int kSprMax = 1024;
    float sprBuf[3][kSprMax] = {};
    float sprFilt[3] = {0.f, 0.f, 0.f};
    int sprPos = 0;
    float sprAp1 = 0.f, sprAp2 = 0.f;
    std::atomic<float> clfoShape[128] = {};
    std::atomic<bool> clfoOn[2] = {};
    std::atomic<int> clfoRate[2] = {};
    std::atomic<float> clfoDepth[2] = {};
    std::atomic<float> clfoAmount[2] = {};
    std::atomic<int> clfoTarget[2] = {};
    std::atomic<int> clfoWave[2] = {};
    float clfoPhase[2] = {0.f, 0.f};
    float clfoVal(int idx);
    float fxP(int fx, int k);
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
        int decim = 1;
        float stepPitch[kMaxSteps];
        float stepVel[kMaxSteps];
        float acc = 0.f;
        LoopBank() {
            for (int s = 0; s < kMaxSteps; s++) { stepPitch[s] = 0.f; stepVel[s] = 1.f; }
        }
    };
    static constexpr int kTotalFrameBudget = 6553600;
    LoopBank banks[kNumBanks];
    bool bankFxSend[kNumBanks] = {};
    int undoBank = -1;
    float* undoBuf = nullptr;
    int undoSamples = 0;
    int undoDecim = 1;
    int undoSteps = 0;
    int undoRecN = 0;
    float undoStepPitch[kMaxSteps] = {};
    float undoStepVel[kMaxSteps] = {};
    int totalFrames = 0;
    int recBank = -1;
    bool recWet = false;
    float recAccum = 0.f;
    int recDiv = 0;
    float recWarmLp = 0.f;
    float recFizzLp = 0.f;
    int resBank = -1;
    int resPos = 0;
    float resAccumL = 0.f, resAccumR = 0.f;
    int resDiv = 0;
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
