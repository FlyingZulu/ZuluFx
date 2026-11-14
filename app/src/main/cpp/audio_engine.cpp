#include "audio_engine.h"
#include <cmath>
#include <cstring>

static inline float clamp01(float v) { return v < 0.f ? 0.f : (v > 1.f ? 1.f : v); }

static int autoDecim(float dur) {
    if (dur <= 7.f) return 1;
    if (dur <= 10.f) return 2;
    if (dur <= 14.f) return 3;
    return 4;
}

static int flavorDecim(int f) {
    switch (f) {
        case 1: return 6;
        case 2: return 3;
        case 3: return 8;
        default: return 1;
    }
}

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

    if (t == 16) {
        if (envAtkRate > 0.f) {
            env += envAtkRate * fsr;
            if (env >= 1.f) { env = 1.f; envAtkRate = 0.f; }
        } else if (env > 0.f) env -= envRate * fsr;
        if (env < 0.f) env = 0.f;
        if (env < 0.0005f) return 0.f;
        int ht = genHitType;
        float lvl = 0.4f + 1.2f * fxP(62, 3);
        float s = 0.f;
        if (ht <= 2) {
            float f = genKickF * (0.7f + 0.6f * env);
            oscPhase += f * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            s = sinf(2.f * 3.14159265f * oscPhase) * env * 0.8f;
            s = tanhf(s * (1.f + fxP(62, 2))) * (1.f / (1.f + 0.5f * fxP(62, 2)));
        } else if (ht == 3) {
            float f = genKickF * (1.f + 0.25f * (1.f - env));
            oscPhase += f * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            s = (sinf(2.f * 3.14159265f * oscPhase) * 0.8f +
                sinf(4.f * 3.14159265f * oscPhase) * 0.2f) * env * 0.85f;
        } else if (ht <= 5) {
            oscPhase += genKickF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            fmPhase += genKickF * 2.02f * fsr;
            if (fmPhase >= 1.f) fmPhase -= 1.f;
            s = (sinf(2.f * 3.14159265f * oscPhase) +
                0.5f * sinf(2.f * 3.14159265f * fmPhase)) * env * 0.5f;
        } else if (ht == 6) {
            oscPhase += 560.f * genF2 * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            fmPhase += 845.f * genF2 * fsr;
            if (fmPhase >= 1.f) fmPhase -= 1.f;
            float band = (oscPhase < 0.5f ? 1.f : -1.f) * 0.6f +
                (fmPhase < 0.5f ? 1.f : -1.f) * 0.4f;
            float hp = band - prevNoise;
            prevNoise = band;
            genMetLp += 0.35f * (hp - genMetLp);
            s = genMetLp * env * 2.f;
        } else {
            float f0 = genKickF;
            oscPhase += f0 * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            fmPhase += f0 * 2.76f * fsr;
            if (fmPhase >= 1.f) fmPhase -= 1.f;
            s = sinf(2.f * 3.14159265f * oscPhase +
                2.4f * sinf(2.f * 3.14159265f * fmPhase * 1.4f)) * env * 0.5f;
        }
        return s * lvl;
    }

    if (t == 17 || t == 18 || t == 19) {
        int gslot = (t == 17) ? 69 : (t == 18) ? 70 : 71;
        if (arpOn.load()) {
            if (envAtkRate > 0.f) {
                env += envAtkRate * fsr;
                if (env >= 1.f) { env = 1.f; envAtkRate = 0.f; }
            } else if (env > 0.f) {
                env -= envRate * fsr;
                if (env < 0.f) env = 0.f;
            }
        } else if (genTouch.load()) {
            env += fsr / (0.01f + 0.3f * fxP(gslot, 1));
            if (env > 1.f) env = 1.f;
        } else {
            env -= envRate * fsr;
            if (env < 0.f) env = 0.f;
        }
        if (env < 0.0005f) { plukPrev = 0.f; return 0.f; }
        float s = 0.f;
        if (t == 17) {
            if (!arpOn.load()) {
                int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
                baseF = 55.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                    powf(2.f, (fxP(69, 0) - 0.5f));
            }
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            float saw = (oscPhase * 2.f - 1.f) * 0.6f;
            float f1 = 220.f * powf(9.f, fxP(69, 1));
            float c1 = 1.f - expf(-6.2832f * f1 * fsr);
            vox1 += c1 * (saw - vox1);
            vox1b += c1 * (vox1 - vox1b);
            float f2 = f1 * (2.2f + 1.2f * fxP(69, 2));
            float c2 = 1.f - expf(-6.2832f * f2 * fsr);
            vox2 += c2 * (saw - vox2);
            vox2b += c2 * (vox2 - vox2b);
            noiseState = noiseState * 1103515245u + 12345u;
            float air = ((float)(noiseState >> 16) / 32768.f - 1.f) * fxP(69, 2) * 0.2f;
            s = ((vox1 - vox1b) * 2.4f + (vox2 - vox2b) * 1.7f + air) * env;
        } else if (t == 18) {
            if (!arpOn.load()) {
                int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
                baseF = 55.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                    powf(2.f, (fxP(70, 0) - 0.5f));
            }
            bool re = plukTrig || (env > 0.6f && plukPrev <= 0.6f);
            int n = (int)(fs / (baseF < 20.f ? 20.f : baseF));
            if (n < 8) n = 8;
            if ((int)plukBuf.size() != n) { plukBuf.assign(n, 0.f); plukPos = 0; re = true; }
            if (re) {
                for (int i2 = 0; i2 < n; i2++) {
                    noiseState = noiseState * 1103515245u + 12345u;
                    plukBuf[i2] = ((float)(noiseState >> 16) / 32768.f - 1.f) *
                        (0.4f + 0.6f * fxP(70, 2));
                }
                plukPos = 0;
                plukTrig = false;
            }
            float damp = 0.995f - 0.05f * fxP(70, 1);
            if (damp < 0.9f) damp = 0.9f;
            float y = plukBuf[plukPos];
            float lp = 0.5f * (y + plukLast);
            plukBuf[plukPos] = damp * lp;
            plukLast = y;
            plukPos = (plukPos + 1) % n;
            s = y * env * 0.9f;
        } else {
            if (!arpOn.load()) {
                int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
                baseF = 55.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                    powf(2.f, (fxP(71, 0) - 0.5f));
            }
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            float ph = oscPhase;
            float sn = sinf(6.2832f * ph);
            float tri = 1.f - 4.f * fabsf(ph - 0.5f);
            float sw = 2.f * ph - 1.f;
            float m = fxP(71, 1);
            float s2 = (m < 0.5f) ? (sn + (tri - sn) * (m * 2.f))
                                  : (tri + (sw - tri) * ((m - 0.5f) * 2.f));
            float x = s2 * (1.f + fxP(71, 2) * 5.f);
            for (int f2 = 0; f2 < 3; f2++) {
                if (x > 1.f) x = 2.f - x;
                else if (x < -1.f) x = -2.f - x;
                else break;
            }
            s = x * env * 0.8f;
        }
        plukPrev = env;
        float lvl = 0.4f + 1.2f * fxP(gslot, 3);
        genNoteLp += 0.4f * (s * lvl - genNoteLp);
        return genNoteLp;
    }

    if (t == 20 || t == 21 || t == 22 || t == 23) {
        int gslot = (t == 20) ? 80 : (t == 21) ? 81 : (t == 22) ? 82 : 83;
        if (arpOn.load()) {
            if (envAtkRate > 0.f) {
                env += envAtkRate * fsr;
                if (env >= 1.f) { env = 1.f; envAtkRate = 0.f; }
            } else if (env > 0.f) {
                env -= envRate * fsr;
                if (env < 0.f) env = 0.f;
            }
        } else if (genTouch.load()) {
            env += fsr / (0.004f + 0.2f * fxP(gslot, 1));
            if (env > 1.f) env = 1.f;
        } else {
            env -= envRate * fsr;
            if (env < 0.f) env = 0.f;
        }
        if (env < 0.0005f) { plukPrev = 0.f; return 0.f; }
        float s = 0.f;
        if (t == 20) {
            if (!arpOn.load()) {
                int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
                baseF = 38.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                    powf(2.f, fxP(80, 0) * 2.f);
            }
            float punch = 0.25f + 1.75f * fxP(80, 1);
            float f = baseF * (1.f + punch * env * env * env * 3.5f);
            oscPhase += f * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            float body = sinf(6.2831853f * oscPhase);
            body = tanhf(body * (1.5f + 2.5f * fxP(80, 1))) * 0.75f;
            kik8Click *= 1.f - fminf(1.f, 900.f * fsr);
            noiseState = noiseState * 1103515245u + 12345u;
            float nz = (float)(noiseState >> 16) / 32768.f - 1.f;
            float hp = nz - kik8Lp;
            kik8Lp += 0.4f * (nz - kik8Lp);
            s = (body + hp * kik8Click * 0.9f) * env;
        } else if (t == 21) {
            if (!arpOn.load()) {
                int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
                baseF = 110.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                    powf(2.f, (fxP(81, 0) - 0.5f) * 2.f);
            }
            float mode = fxP(81, 2);
            float vw = fxP(81, 1);
            bool robot = mode < 0.5f;
            float vib = 0.f;
            if (!robot) {
                voyVibPh += 5.2f * fsr;
                if (voyVibPh >= 1.f) voyVibPh -= 1.f;
                vib = sinf(6.2831853f * voyVibPh) * 0.006f;
            }
            voyPh += (baseF * (1.f + vib)) * fsr;
            if (voyPh >= 1.f) voyPh -= 1.f;
            float exc = robot ? (voyPh < 0.5f ? 1.f : -1.f) : (voyPh * 2.f - 1.f);
            noiseState = noiseState * 1103515245u + 12345u;
            float breath = ((float)(noiseState >> 16) / 32768.f - 1.f) *
                (robot ? 0.02f : 0.10f);
            exc = exc * 0.55f + breath;
            float vs = robot ? vw * 4.99f : vw * 4.f;
            int vi = (int)vs;
            float fr = vs - (float)vi;
            static const float vowF[5][3] = {
                {800.f, 1150.f, 2900.f},
                {400.f, 2000.f, 2550.f},
                {350.f, 2800.f, 3300.f},
                {450.f, 800.f, 2830.f},
                {325.f, 700.f, 2700.f}
            };
            static const float vowAmp[5][3] = {
                {1.f, 0.5f, 0.2f},
                {1.f, 0.35f, 0.15f},
                {1.f, 0.3f, 0.15f},
                {1.f, 0.6f, 0.1f},
                {1.f, 0.7f, 0.08f}
            };
            float q = robot ? 22.f : 12.f;
            s = 0.f;
            for (int f3 = 0; f3 < 3; f3++) {
                float fc0 = vowF[vi][f3];
                float fc1 = vowF[vi + 1 < 5 ? vi + 1 : 4][f3];
                float fc = fc0 + (fc1 - fc0) * fr;
                float amp0 = vowAmp[vi][f3];
                float amp1 = vowAmp[vi + 1 < 5 ? vi + 1 : 4][f3];
                float c = 1.f - expf(-6.2831853f * fc * fsr);
                voyF1[f3] += c * (exc - voyF1[f3]);
                voyF1b[f3] += c * (voyF1[f3] - voyF1b[f3]);
                float bp = (voyF1[f3] - voyF1b[f3]) * q * 0.5f;
                s += bp * (amp0 + (amp1 - amp0) * fr);
            }
            s = tanhf(s * 0.6f) * env;
        } else if (t == 22) {
            if (!arpOn.load()) {
                int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
                baseF = 110.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                    powf(2.f, (fxP(82, 0) - 0.5f) * 2.f);
            }
            static const float malR[8] = {
                1.f, 2.32f, 4.05f, 6.2f, 8.9f, 12.1f, 15.8f, 19.9f
            };
            float spread = 1.f + fxP(82, 1) * 0.8f;
            malTrig *= 1.f - fminf(1.f, 2200.f * fsr);
            noiseState = noiseState * 1103515245u + 12345u;
            float exc = ((float)(noiseState >> 16) / 32768.f - 1.f) * malTrig;
            s = 0.f;
            for (int p3 = 0; p3 < 8; p3++) {
                float f = baseF * malR[p3] * (p3 == 0 ? 1.f : spread);
                if (f > 0.45f * (1.f / fsr)) break;
                float w = 6.2831853f * f * fsr;
                float dec = env * (0.995f - 0.02f * fxP(82, 1)) - 0.1f * p3 * 0.02f;
                if (dec < 0.9f) dec = 0.9f;
                float r = dec;
                float y = exc + 2.f * r * cosf(w) * malX[p3] - r * r * malY[p3];
                malY[p3] = malX[p3];
                malX[p3] = y;
                s += y / (1.f + (float)p3 * 0.9f);
            }
            s = tanhf(s * 0.35f) * env;
        } else {
            if (!arpOn.load()) {
                int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
                baseF = 220.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                    powf(2.f, (fxP(83, 0) - 0.5f) * 2.f);
            }
            float mt = fxP(83, 1);
            float ratio = 3.51f + mt * 1.53f;
            float idx = 1.2f + mt * 4.5f;
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            fmPhase += baseF * ratio * fsr;
            if (fmPhase >= 1.f) fmPhase -= 1.f;
            float c1 = sinf(6.2831853f * oscPhase +
                idx * sinf(6.2831853f * fmPhase));
            float c2 = sinf(6.2831853f * oscPhase * 2.76f +
                idx * 0.5f * sinf(6.2831853f * fmPhase * 1.41f));
            s = (c1 * 0.8f + c2 * 0.2f * mt) * env;
        }
        plukPrev = env;
        float lvl = 0.4f + 1.2f * fxP(gslot, 3);
        genNoteLp += 0.4f * (s * lvl - genNoteLp);
        return genNoteLp;
    }

    bool melodic = (t == 0 || t == 12 || t == 13 || t == 14);
    if (melodic) {
        int gslot = 46 + t;
        if (t == 12) gslot = 58; else if (t == 13) gslot = 59; else if (t == 14) gslot = 60;
        if (arpOn.load()) {
            if (envAtkRate > 0.f) {
                env += envAtkRate * fsr;
                if (env >= 1.f) { env = 1.f; envAtkRate = 0.f; }
            } else if (env > 0.f) {
                env -= envRate * fsr;
                if (env < 0.f) env = 0.f;
            }
        } else {
            int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
            baseF = 55.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                powf(2.f, (fxP(gslot, 0) - 0.5f));
            float atk = 0.002f + 0.3f * fxP(gslot, 1);
            if (genTouch.load()) {
                env += fsr / atk;
                if (env > 1.f) env = 1.f;
            } else env = 0.f;
        }
        float gt = genTouch.load() ? 1.f : 0.f;
        genGate += (0.001f + 0.01f * fxP(gslot, 2)) * (gt - genGate);
        float ayv = ay.load(std::memory_order_relaxed);
        float fc = 120.f * powf(100.f, ayv);
        float kk = 1.f - expf(-2.f * 3.14159265f * fc * fsr);
        float s = 0.f;
        if (t == 0) {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            s = (oscPhase * 2.f - 1.f) * 0.4f;
        } else if (t == 12) {
            noiseState = noiseState * 1103515245u + 12345u;
            s = ((float)(noiseState >> 16) / 32768.f - 1.f) * 0.35f;
        } else if (t == 13) {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            s = (oscPhase < 0.5f ? 1.f : -1.f) * 0.3f;
        } else {
            oscPhase += baseF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            fmPhase += baseF * 6.f * fsr;
            if (fmPhase >= 1.f) fmPhase -= 1.f;
            s = sinf(2.f * 3.14159265f * oscPhase +
                2.6f * sinf(2.f * 3.14159265f * fmPhase)) * 0.45f;
        }
        genNoteLp += kk * (s * env * genGate * (0.4f + 1.2f * fxP(gslot, 3)) - genNoteLp);
        return genNoteLp;
    }

    if (t == 15) {
        genNoteF += (baseF - genNoteF) * (0.0004f + 0.004f * fxP(61, 1));
        if (arpOn.load()) {
            if (envAtkRate > 0.f) {
                env += envAtkRate * fsr;
                if (env >= 1.f) { env = 1.f; envAtkRate = 0.f; }
            } else if (env > 0.f) {
                env -= envRate * fsr;
                if (env < 0.f) env = 0.f;
            }
        } else {
            int deg = (int)(ax.load(std::memory_order_relaxed) * 24.99f);
            baseF = 55.f * powf(2.f, degreeToSemi(deg) / 12.f) * pitchFactor() *
                powf(2.f, (fxP(61, 0) - 0.5f));
            env = genTouch.load() ? 1.f : 0.f;
        }
        float gt = genTouch.load() ? 1.f : 0.f;
        genGate += 0.003f * (gt - genGate);
        if (genGate < 0.0005f) { gen303Lp = 0.f; gen303Lp2 = 0.f; return 0.f; }
        oscPhase += genNoteF * fsr;
        if (oscPhase >= 1.f) oscPhase -= 1.f;
        float saw = (oscPhase * 2.f - 1.f) * 0.5f * env * genGate;
        float ayv = ay.load(std::memory_order_relaxed);
        float envm = 0.4f + 1.2f * fxP(61, 0);
        float fc = 80.f * powf(160.f, ayv + 0.55f * env * envm);
        if (fc > 14000.f) fc = 14000.f;
        float c = 1.f - expf(-2.f * 3.14159265f * fc * fsr);
        gen303Res = 0.3f + 0.6f * fxP(61, 2);
        float in = saw - gen303Res * gen303Lp2;
        float lp1 = gen303Lp + c * (in - gen303Lp);
        float lp2 = gen303Lp2 + c * (lp1 - gen303Lp2);
        if (lp2 > 4.f) lp2 = 4.f;
        if (lp2 < -4.f) lp2 = -4.f;
        gen303Lp = lp1; gen303Lp2 = lp2;
        return tanhf(lp2 * 1.6f) * 0.7f * (0.4f + 1.2f * fxP(61, 3));
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
            case 8: p = patClk; break;
            case 9: p = patZap; break;
        }
        if (p && p[seqStep] >= 0) {
            env = 1.f;
            fmPhase = 0.f;
            float gx = ax.load(std::memory_order_relaxed);
            float gy = ay.load(std::memory_order_relaxed);
            switch (t) {
                case 5: kickF = 50.f + gx * 130.f; drumA = gx; drumB = gy;
                    envRate = 1.f / ((0.1f + gy * 0.55f) * fs); break;
                case 6: drumA = gx; drumB = gy; envRate = 1.f / ((0.06f + gy * 0.18f) * fs); break;
                case 7: drumA = gx; drumB = gy; envRate = 1.f / ((0.02f + gy * 0.35f) * fs); break;
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
            kickF += (genKickF - kickF) * 0.02f;
            oscPhase += kickF * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            float s = sinf(2.f * 3.14159265f * oscPhase) * env * 0.9f;
            if (genF2 > 1.01f) s = tanhf(s * genF2) * (0.9f / sqrtf(genF2));
            return s + env * env * env * genF1 * 0.3f;
        }
        case 6: {
            noiseState = noiseState * 1103515245u + 12345u;
            float n = (float)(noiseState >> 16) / 32768.f - 1.f;
            oscPhase += genF1 * fsr;
            if (oscPhase >= 1.f) oscPhase -= 1.f;
            return (n * (0.3f + 0.45f * drumB) +
                sinf(2.f * 3.14159265f * oscPhase) * (0.5f - 0.3f * drumB)) * env * 0.9f;
        }
        case 7: {
            static const float hatF[6] = {263.f, 400.f, 421.f, 474.f, 587.f, 845.f};
            float band = 0.f;
            for (int h2 = 0; h2 < 6; h2++) {
                hatPh[h2] += hatF[h2] * genF2 * fsr;
                if (hatPh[h2] >= 1.f) hatPh[h2] -= 1.f;
                band += (hatPh[h2] < 0.5f ? 1.f : -1.f);
            }
            band *= (1.f / 6.f);
            float hp = band - prevNoise;
            prevNoise = band;
            float bright = 0.35f + 0.65f * drumA;
            return (hp * bright + band * (1.f - bright) * 0.3f) * env * 0.45f;
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
    if ((t >= 1 && t <= 4) || t == 0 || t == 12 || t == 13 || t == 14 || t == 15 ||
        t == 17 || t == 18 || t == 19 || t == 20 || t == 21 || t == 22 || t == 23) {
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

    static const float arpPatV[4] = {0.2f, 0.75f, 0.45f, 0.95f};
    arpModV = arpPatV[arpIdx & 3];

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
        stReset(lb);
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
      ->setDataCallback(this)
      ->setErrorCallback(this);
    if (ob.openStream(out) != oboe::Result::OK) {
        ob.setSharingMode(oboe::SharingMode::Shared);
        if (ob.openStream(out) != oboe::Result::OK) return false;
    }

    out->setBufferSizeInFrames(out->getFramesPerBurst() * 4);
    fs = (float)out->getSampleRate();
    reverbL.init(fs, 1.f);
    reverbR.init(fs, 1.017f);
    plateL.init(fs, 0.62f);
    plateR.init(fs, 0.637f);
    shimL.init(fs, 1.15f);
    shimR.init(fs, 1.167f);
    gateL.init(fs, 0.85f);
    gateR.init(fs, 0.867f);
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

void AudioEngine::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    if (error == oboe::Result::ErrorClosed || stream == nullptr) return;
    if (stream != out.get()) return;
    if (restarting.exchange(true)) return;
    stop();
    start(mic);
    restarting.store(false);
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

void AudioEngine::setFx(int fx, bool on) {
    if (fx < 0 || fx >= kNumFx) return;
    static const float defWet[kNumFx] = {
        0.4f, 0.45f, 0.6f, 0.5f, 0.6f, 0.5f, 0.5f, 0.4f, 0.7f, 0.8f, 1.0f, 0.6f, 1.0f,
        0.7f, 0.7f, 0.6f, 0.5f, 0.5f, 0.8f, 0.6f, 0.85f, 0.8f, 0.5f, 0.75f,
        0.8f, 1.0f, 0.7f,
        0.7f, 0.8f, 0.7f, 0.7f, 0.65f, 0.6f,
        0.6f,
        0.8f, 0.8f, 0.8f,
        0.8f, 0.8f, 0.8f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f,
        0.55f, 0.55f, 0.65f, 0.5f, 0.6f
    };
    fxWet[fx].store(on ? defWet[fx] : 0.f);
}

void AudioEngine::drumHit(int bank, int slice, int total, float pitch) {
    if (bank < 0 || bank >= kNumBanks) return;
    LoopBank& lb = banks[bank];
    if (!lb.buf || lb.samples < 8) return;
    int s0 = 0, s1 = lb.samples;
    if (total > 0) {
        int sl = lb.samples / total;
        if (slice < 0) {
            noiseState = noiseState * 1103515245u + 12345u;
            slice = (int)((noiseState >> 16) / 65536.f * total);
            if (slice >= total) slice = total - 1;
        }
        s0 = slice * sl;
        s1 = (slice == total - 1) ? lb.samples : s0 + sl;
    }
    drumBank.store(bank);
    drumPos = (float)s0;
    drumEnd = (float)s1;
    drumRate = powf(2.f, pitch / 12.f);
    if (drumRate < 0.12f) drumRate = 0.12f;
    if (drumRate > 8.f) drumRate = 8.f;
    drumEnv = 1.f;
}

void AudioEngine::genHit(int type, float x, float y) {
    if (x < 0.f) x = 0.f;
    if (x > 0.999f) x = 0.999f;
    if (y < 0.f) y = 0.f;
    if (y > 0.999f) y = 0.999f;
    int col = (int)(x * 3.f);
    int row = 2 - (int)(y * 3.f);
    if (col < 0) col = 0;
    if (col > 2) col = 2;
    if (row < 0) row = 0;
    if (row > 2) row = 2;
    env = 1.f;
    envAtkRate = 0.f;
    fmPhase = 0.f;
    if (type == 5) {
        float base = col == 0 ? 48.f : col == 1 ? 55.f : 64.f;
        genKickF = base * powf(2.f, (fxP(51, 0) - 0.5f) * 2.f) *
            (row == 0 ? 1.f : row == 1 ? 0.82f : 0.68f);
        float dec = (0.12f + 0.5f * fxP(51, 1)) *
            (row == 0 ? 1.15f : row == 1 ? 0.8f : 0.5f);
        envRate = 1.f / (dec * fs);
        genF1 = fxP(51, 2);
        genF2 = row == 0 ? 2.f : 1.f;
        kickF = genKickF * 2.2f;
        oscPhase = 0.f;
    } else if (type == 6) {
        genF1 = (150.f + 45.f * (float)col) * powf(2.f, (fxP(52, 0) - 0.5f) * 2.f);
        float dec = (0.09f + 0.12f * fxP(52, 1)) *
            (row == 0 ? 0.7f : row == 1 ? 1.f : 1.6f);
        envRate = 1.f / (dec * fs);
        drumB = 0.3f + 0.6f * fxP(52, 2);
        drumA = (float)col / 2.f;
        oscPhase = 0.f;
    } else if (type == 7) {
        float dec = (col == 0 ? 0.04f : col == 1 ? 0.12f : 0.35f) * (0.5f + fxP(53, 1));
        envRate = 1.f / (dec * fs);
        genF2 = powf(2.f, (fxP(53, 2) - 0.5f) * 2.f) *
            (row == 0 ? 1.f : row == 1 ? 1.26f : 1.5f);
        drumA = fxP(53, 0);
        for (int h2 = 0; h2 < 6; h2++) hatPh[h2] = 0.f;
        prevNoise = 0.f;
    } else if (type == 16) {
        int st = col * 3 + row;
        genHitType = st;
        float tune = powf(2.f, (fxP(62, 0) - 0.5f) * 2.f);
        float dmul = 0.5f + fxP(62, 1);
        if (st <= 2) {
            genKickF = (st == 0 ? 95.f : st == 1 ? 150.f : 230.f) * tune;
            envRate = 1.f / ((0.34f - 0.06f * (float)st) * dmul * fs);
        } else if (st == 3) {
            genKickF = 62.f * tune;
            envRate = 1.f / (0.5f * dmul * fs);
        } else if (st == 4) {
            genKickF = 320.f * tune;
            envRate = 1.f / (0.35f * dmul * fs);
        } else if (st == 5) {
            genKickF = 560.f * tune;
            envRate = 1.f / (0.12f * dmul * fs);
        } else if (st == 6) {
            genF2 = tune;
            envRate = 1.f / (0.25f * dmul * fs);
        } else {
            genKickF = (st == 7 ? 520.f : 880.f) * tune;
            envRate = 1.f / ((st == 7 ? 1.1f : 1.4f) * dmul * fs);
        }
        oscPhase = 0.f;
        fmPhase = 0.f;
        genMetLp = 0.f;
        prevNoise = 0.f;
    }
    if (type == 17) {
        envRate = 1.f / ((0.3f + 0.9f * fxP(69, 1)) * fs);
    } else if (type == 18) {
        envRate = 1.f / ((0.3f + 1.2f * fxP(70, 1)) * fs);
        plukTrig = true;
    } else if (type == 19) {
        envRate = 1.f / ((0.4f + 1.0f * fxP(71, 1)) * fs);
    } else if (type == 20) {
        envRate = 1.f / ((0.12f + 0.88f * fxP(80, 2)) * fs);
        oscPhase = 0.f;
        kik8Click = 1.f;
        kik8Lp = 0.f;
    } else if (type == 21) {
        envRate = 1.f / ((0.25f + 0.9f * fxP(81, 1)) * fs);
        voyPh = 0.f;
        voyVibPh = 0.f;
    } else if (type == 22) {
        envRate = 1.f / ((0.08f + 1.3f * fxP(82, 2)) * fs);
        malTrig = 1.f;
    } else if (type == 23) {
        envRate = 1.f / ((0.4f + 2.6f * fxP(83, 2)) * fs);
        oscPhase = 0.f;
        fmPhase = 0.f;
    }
}

void AudioEngine::setParam(int p, float v, bool on) {
    if (p < 0 || p > 7) return;
    if (v < 0.f) v = 0.f; if (v > 1.f) v = 1.f;
    unsigned int m = paramManualMask.load();
    if (on) {
        paramManual[p].store(v);
        m |= (1u << p);
    } else {
        m &= ~(1u << p);
    }
    paramManualMask.store(m);
}

void AudioEngine::setLink(int idx, int src, int dst, float amount, float prob, int chop, float delay, float soft, bool on) {
    if (idx < 0 || idx >= 16) return;
    links[idx].src.store(src);
    links[idx].dst.store(dst);
    links[idx].amount.store(amount);
    links[idx].prob.store(prob < 0.f ? 0.f : (prob > 1.f ? 1.f : prob));
    links[idx].chop.store(chop < 0 ? 0 : (chop > 5 ? 5 : chop));
    links[idx].delay.store(delay < 0.f ? 0.f : (delay > 0.25f ? 0.25f : delay));
    links[idx].soft.store(soft < 0.f ? 0.f : (soft > 1.f ? 1.f : soft));
    links[idx].on.store(on && src >= 8 && dst >= 8);
    linkPhase[idx] = 0.f;
    linkPass[idx] = true;
    linkSm[idx] = 0.f;
}

void AudioEngine::setFxParam(int fx, int k, float v) {
    if (fx < 0 || fx >= kNumSlots || k < 0 || k > 7) return;
    if (v < 0.f) v = 0.f;
    if (v > 1.f) v = 1.f;
    fxParam[fx][k].store(v);
}

float AudioEngine::fxP(int fx, int k) {
    float v;
    if (fxAxisMask[fx] & (unsigned char)(1u << k)) v = fxAxisVal[fx][k];
    else v = fxParam[fx][k].load(std::memory_order_relaxed) + fxMod[fx][k];
    if (v < 0.f) v = 0.f;
    if (v > 1.f) v = 1.f;
    return v;
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
    float dur = (60.f / b) * steps;
    int fl = recFlavor.load();
    if (fl < 0 || fl > 3) fl = 0;
    int D = (fl == 0) ? autoDecim(dur) : flavorDecim(fl);
    int n = (int)(dur * fs / (float)D);
    if (n < 2400) n = 2400;
    LoopBank& lb = banks[bank];
    if (lb.samples > 0 && lb.buf) {
        delete[] undoBuf;
        undoBuf = new float[(size_t)lb.samples * 2];
        memcpy(undoBuf, lb.buf, (size_t)lb.samples * 2 * sizeof(float));
        undoSamples = lb.samples;
        undoDecim = lb.decim;
        undoSteps = lb.steps;
        undoRecN = n;
        for (int s = 0; s < kMaxSteps; s++) {
            undoStepPitch[s] = lb.stepPitch[s];
            undoStepVel[s] = lb.stepVel[s];
        }
        undoBank = bank;
    } else {
        undoBank = -1;
    }
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
    lb.decim = D;
    for (int s = 0; s < kMaxSteps; s++) { lb.stepPitch[s] = 0.f; lb.stepVel[s] = 1.f; }
    lb.acc = 0.f;
    recBank = bank;
    recWet = wet;
    recAccum = 0.f;
    recDiv = 0;
    recWarmLp = 0.f;
    recFizzLp = 0.f;
}

void AudioEngine::cancelRec() {
    if (recBank < 0) return;
    LoopBank& lb = banks[recBank];
    lb.playing = false;
    lb.samples = 0;
    lb.pos = 0;
    recBank = -1;
}

void AudioEngine::stopRec() {
    if (recBank < 0) return;
    LoopBank& lb = banks[recBank];
    if (lb.pos > 0) {
        lb.samples = lb.pos;
        lb.playing = true;
        lb.pos = 0;
        stReset(lb);
    } else {
        lb.playing = false;
        lb.samples = 0;
        lb.pos = 0;
    }
    recBank = -1;
}

float AudioEngine::inputChain(float x, float fsr) {
    int prof = inProfile.load(std::memory_order_relaxed);
    if (prof < 0 || prof > 3) prof = 0;
    if (inCoefProf != prof || inCoefFs != fsr) {
        inCoefProf = prof;
        inCoefFs = fsr;
        float hp = (prof == 1) ? 60.f : (prof == 2) ? 95.f : (prof == 3) ? 70.f : 20.f;
        inA = expf(-6.2831853f * hp * fsr);
        float atk = (prof == 3) ? 5.f : (prof == 2) ? 15.f : (prof == 1) ? 25.f : 2.f;
        float rel = (prof == 3) ? 120.f : (prof == 2) ? 180.f : (prof == 1) ? 300.f : 60.f;
        inCa = expf(-1000.f * fsr / atk);
        inCr = expf(-1000.f * fsr / rel);
        inHpfX = 0.f; inHpfY = 0.f; inEnv = 0.f; inGr = 1.f;
    }
    float y = inA * (inHpfY + x - inHpfX);
    inHpfX = x;
    inHpfY = y;
    x = y;
    if (prof == 0) {
        if (x > 0.85f) x = 0.85f + tanhf((x - 0.85f) * 4.f) * 0.12f;
        else if (x < -0.85f) x = -0.85f + tanhf((x + 0.85f) * 4.f) * 0.12f;
        return x;
    }
    float th = (prof == 3) ? 0.07f : (prof == 2) ? 0.08f : 0.12f;
    float rate = (prof == 3) ? 6.f : (prof == 2) ? 4.f : 3.f;
    float makeup = (prof == 3) ? 1.5f : (prof == 2) ? 1.4f : 1.25f;
    float ax = fabsf(x);
    inEnv = (ax > inEnv) ? inCa * inEnv + (1.f - inCa) * ax
                         : inCr * inEnv + (1.f - inCr) * ax;
    float gr = 1.f;
    if (inEnv > th) gr = (th + (inEnv - th) / rate) / inEnv;
    inGr += (gr - inGr) * 0.004f;
    x *= inGr * makeup;
    if (prof == 3) {
        x = tanhf(x * 1.2f) * 0.85f;
    } else if (x > 0.95f) x = 0.95f; else if (x < -0.95f) x = -0.95f;
    return x;
}

void AudioEngine::eqSetCoefs(float fs) {
    const float dbL = 2.5f, dbM = -1.f, dbH = 2.f;
    const float fc[3] = {120.f, 1000.f, 7000.f};
    EqBiquad* ch[3] = {eqXL, 0, eqXR};
    (void)ch;
    struct Cfg { float db; float q; int type; };
    const Cfg cfg[3] = {{dbL, 0.7f, 0}, {dbM, 0.8f, 1}, {dbH, 0.7f, 2}};
    for (int b = 0; b < 3; b++) {
        float A = powf(10.f, cfg[b].db / 40.f);
        float w0 = 6.2831853f * fc[b] / fs;
        float cw = cosf(w0), sw = sinf(w0);
        float al, b0, b1, b2, a0, a1, a2;
        if (cfg[b].type == 1) {
            al = sw / (2.f * cfg[b].q);
            b0 = 1.f + al * A; b1 = -2.f * cw; b2 = 1.f - al * A;
            a0 = 1.f + al / A; a1 = -2.f * cw; a2 = 1.f - al / A;
        } else {
            al = sw / 2.f * sqrtf((A + 1.f / A) * (1.f / 0.7f - 1.f) + 2.f);
            b0 = A * ((A + 1.f) - (A - 1.f) * cw + 2.f * sqrtf(A) * al);
            b1 = 2.f * A * ((A - 1.f) - (A + 1.f) * cw);
            b2 = A * ((A + 1.f) - (A - 1.f) * cw - 2.f * sqrtf(A) * al);
            a0 = (A + 1.f) + (A - 1.f) * cw + 2.f * sqrtf(A) * al;
            a1 = -2.f * ((A - 1.f) + (A + 1.f) * cw);
            a2 = (A + 1.f) + (A - 1.f) * cw - 2.f * sqrtf(A) * al;
        }
        EqBiquad* targets[3] = {&eqIn[b], &eqXL[b], &eqXR[b]};
        for (int t = 0; t < 3; t++) {
            EqBiquad* q = targets[t];
            q->b0 = b0 / a0; q->b1 = b1 / a0; q->b2 = b2 / a0;
            q->a1 = a1 / a0; q->a2 = a2 / a0;
        }
    }
    eqFs = fs;
    eqCoefOk = true;
}

float AudioEngine::eqProc(EqBiquad* ch, float x) {
    for (int b = 0; b < 3; b++) x = ch[b].proc(x);
    return x;
}

void AudioEngine::setAxisTargets(int axis, const float* idxs, const float* modes, int n) {
    if (axis < 0 || axis > 1) return;
    for (int i = 0; i < 8; i++) {
        int t = (i < n) ? (int)idxs[i] : -1;
        axisTg[axis][i].store(t, std::memory_order_relaxed);
        axisMode[axis][i] = (i < n) ? (unsigned char)((int)modes[i] & 3) : 0;
        axisHold[axis][i] = 0.f;
    }
}

void AudioEngine::eq3SetCoefs(float fs, float gl, float gm, float gh) {
    float g[3] = {gl * 24.f - 12.f, gm * 24.f - 12.f, gh * 24.f - 12.f};
    float fc[3] = {150.f, 900.f, 6000.f};
    for (int b = 0; b < 3; b++) {
        float A = powf(10.f, g[b] / 40.f);
        float w0 = 6.2831853f * fc[b] / fs;
        float cw = cosf(w0), sw = sinf(w0);
        float al, b0, b1, b2, a0, a1, a2;
        if (b == 1) {
            al = sw / 1.8f;
            b0 = 1.f + al * A; b1 = -2.f * cw; b2 = 1.f - al * A;
            a0 = 1.f + al / A; a1 = -2.f * cw; a2 = 1.f - al / A;
        } else {
            al = sw / 2.f * sqrtf((A + 1.f / A) * (1.f / 0.7f - 1.f) + 2.f);
            b0 = A * ((A + 1.f) - (A - 1.f) * cw + 2.f * sqrtf(A) * al);
            b1 = 2.f * A * ((A - 1.f) - (A + 1.f) * cw);
            b2 = A * ((A + 1.f) - (A - 1.f) * cw - 2.f * sqrtf(A) * al);
            a0 = (A + 1.f) + (A - 1.f) * cw + 2.f * sqrtf(A) * al;
            a1 = -2.f * ((A - 1.f) + (A + 1.f) * cw);
            a2 = (A + 1.f) + (A - 1.f) * cw - 2.f * sqrtf(A) * al;
        }
        EqBiquad* ts[2] = {&eq3L[b], &eq3R[b]};
        for (int t = 0; t < 2; t++) {
            ts[t]->b0 = b0 / a0; ts[t]->b1 = b1 / a0; ts[t]->b2 = b2 / a0;
            ts[t]->a1 = a1 / a0; ts[t]->a2 = a2 / a0;
        }
    }
    eq3Fs = fs;
}

void AudioEngine::undoBankRec() {
    if (undoBank < 0 || !undoBuf) return;
    if (recBank == undoBank) {
        banks[recBank].playing = false;
        recBank = -1;
    }
    LoopBank& lb = banks[undoBank];
    int n = undoSamples;
    if (n <= 0) {
        undoBank = -1;
        delete[] undoBuf;
        undoBuf = nullptr;
        return;
    }
    if (!lb.buf || lb.cap < n) {
        delete[] lb.buf;
        lb.cap = n;
        lb.buf = new float[(size_t)n * 2];
    }
    memcpy(lb.buf, undoBuf, (size_t)n * 2 * sizeof(float));
    if (n > lb.samples) totalFrames += n - lb.samples;
    else if (undoRecN > n && lb.samples >= undoRecN) totalFrames -= undoRecN - n;
    lb.samples = n;
    lb.pos = 0;
    lb.playing = true;
    lb.decim = undoDecim > 0 ? undoDecim : 1;
    lb.steps = undoSteps;
    for (int s = 0; s < kMaxSteps; s++) {
        lb.stepPitch[s] = undoStepPitch[s];
        lb.stepVel[s] = undoStepVel[s];
    }
    lb.acc = 0.f;
    undoBank = -1;
    delete[] undoBuf;
    undoBuf = nullptr;
}

void AudioEngine::setBankFxSend(int bank, bool on) {
    if (bank < 0 || bank >= kNumBanks) return;
    bankFxSend[bank] = on;
}

void AudioEngine::startResample(int bank, int steps) {
    if (bank < 0 || bank >= kNumBanks) return;
    if (resBank >= 0) return;
    if (steps < 1) steps = 1;
    if (steps > kMaxSteps) steps = kMaxSteps;
    float b = fmaxf(bpm.load(), 40.f);
    float dur = (60.f / b) * steps;
    int fl = recFlavor.load();
    if (fl < 0 || fl > 3) fl = 0;
    int D = (fl == 0) ? autoDecim(dur) : flavorDecim(fl);
    int n = (int)(dur * fs / (float)D);
    if (n < 2400) n = 2400;
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
    lb.decim = D;
    for (int s = 0; s < kMaxSteps; s++) { lb.stepPitch[s] = 0.f; lb.stepVel[s] = 1.f; }
    lb.acc = 0.f;
    resBank = bank;
    resPos = 0;
    resAccumL = 0.f;
    resAccumR = 0.f;
    resDiv = 0;
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
    if (banks[bank].playing) stReset(banks[bank]);
}

void AudioEngine::setStretchMode(int bank, int mode) {
    if (bank < 0 || bank >= kNumBanks) return;
    if (mode < 0 || mode > 3) mode = 0;
    banks[bank].stMode = mode;
    stReset(banks[bank]);
}

void AudioEngine::setSampleParam(int bank, int k, float v) {
    if (bank < 0 || bank >= kNumBanks) return;
    if (k < 0 || k > 5) return;
    if (v < 0.f) v = 0.f;
    if (v > 1.f) v = 1.f;
    fxParam[63 + k][0].store(v, std::memory_order_relaxed);
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
    lb.decim = 1;
    stReset(lb);
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

    float sPitch[kNumBanks], sAtkC[kNumBanks], sDecC[kNumBanks], sToneA[kNumBanks];
    int sW0[kNumBanks], sW1[kNumBanks];
    for (int k = 0; k < kNumBanks; k++) {
        float r0 = fxP(63, 0), r1 = fxP(64, 0), r2 = fxP(65, 0);
        float r3 = fxP(66, 0), r4 = fxP(67, 0), r5 = fxP(68, 0);
        sPitch[k] = powf(2.f, (r0 - 0.5f) * 2.f);
        float atk = 0.002f + r1 * 0.6f;
        sAtkC[k] = 1.f / (atk * fs + 1.f);
        float tc = 0.05f + r2 * r2 * 8.f;
        sDecC[k] = (r2 >= 0.999f) ? 1.f : 1.f - 1.f / (tc * fs + 1.f);
        float fc = 300.f * powf(120.f, r3);
        float fcMax = 0.45f * fs;
        if (fc > fcMax) fc = fcMax;
        sToneA[k] = 1.f - expf(-6.2831853f * fc / fs);
        LoopBank& lb = banks[k];
        if (lb.samples > 0) {
            int w0 = (int)(r4 * 0.95f * (float)lb.samples);
            int w1 = (int)((0.05f + r5 * 0.95f) * (float)lb.samples);
            if (w0 < 0) w0 = 0;
            if (w1 > lb.samples) w1 = lb.samples;
            if (w1 - w0 < 64) w1 = (w0 + 64 <= lb.samples) ? w0 + 64 : lb.samples;
            if (w1 <= w0) { w0 = 0; w1 = lb.samples; }
            sW0[k] = w0; sW1[k] = w1;
        } else { sW0[k] = 0; sW1[k] = 0; }
    }

    for (int i = 0; i < numFrames; i++) {
        float inS;
        if (mic) {
            inS = inputChain(src[i] * inGain.load(std::memory_order_relaxed) * inBoost.load(std::memory_order_relaxed), fsr);
            if (masterEq.load(std::memory_order_relaxed)) {
                if (!eqCoefOk || eqFs != 1.f / fsr) eqSetCoefs(1.f / fsr);
                inS = eqProc(eqIn, inS);
            }
        } else {
            inS = genSample(fsr, fmaxf(bpm.load(), 40.f)) * inGain.load(std::memory_order_relaxed);
            if (ecoMode.load(std::memory_order_relaxed)) {
                inS = floorf(inS * 96.f + 0.5f) / 96.f;
                inS = tanhf(inS * 1.7f) * 0.72f;
            }
        }
        {
            float pil = fabsf(inS);
            float cil = inLevel.load(std::memory_order_relaxed);
            inLevel.store(cil + (pil > cil ? 0.5f : 0.02f) * (pil - cil), std::memory_order_relaxed);
        }

        if ((coeffClock++ & 31) == 0) {
            float fcT = 80.f * powf(100.f, clamp01(ax.load()));
            float qT = 0.7f + clamp01(ay.load()) * 12.f;
            float lrT = 0.05f * powf(200.f, clamp01(bx.load()));
            float ldT = clamp01(by.load());
            float rwT = 0.f, btT = 16.f, rtT = 1.f, drT = 1.f;
            applyAssign(assignX.load(), ax.load(), fcT, qT, lrT, ldT, rwT, btT, rtT, drT);
            applyAssign(assignY.load(), ay.load(), fcT, qT, lrT, ldT, rwT, btT, rtT, drT);

            unsigned int pm = paramManualMask.load(std::memory_order_relaxed);
            if (pm != 0u) {
                int px = assignX.load(), py = assignY.load();
                for (int p = 0; p < 8; p++) {
                    if ((pm & (1u << p)) == 0u) continue;
                    if (p == px || p == py) continue;
                    applyAssign(p, paramManual[p].load(std::memory_order_relaxed),
                                fcT, qT, lrT, ldT, rwT, btT, rtT, drT);
                }
            }

            float baseNorm[8] = {
                clamp01(ax.load()), clamp01(ay.load()), 0.f, 1.f,
                0.f, clamp01(bx.load()), clamp01(by.load()), 0.f
            };
            int axn = assignX.load();
            int ayn = assignY.load();
            if (axn >= 0 && axn < 8) baseNorm[axn] = clamp01(ax.load());
            if (ayn >= 0 && ayn < 8) baseNorm[ayn] = clamp01(ay.load());
            for (int f2 = 0; f2 < kNumSlots; f2++) {
                fxAxisMask[f2] = 0;
                for (int k2 = 0; k2 < 8; k2++) fxMod[f2][k2] = 0.f;
            }
            if (axn >= 8) {
                int i2 = axn - 8;
                fxAxisMask[i2 / 8] |= (unsigned char)(1u << (i2 % 8));
                fxAxisVal[i2 / 8][i2 % 8] = clamp01(ax.load());
            }
            if (ayn >= 8) {
                int i2 = ayn - 8;
                fxAxisMask[i2 / 8] |= (unsigned char)(1u << (i2 % 8));
                fxAxisVal[i2 / 8][i2 % 8] = clamp01(ay.load());
            }
            for (int a2 = 0; a2 < 2; a2++) {
                float av = clamp01(a2 == 0 ? ax.load() : ay.load());
                for (int i2 = 0; i2 < 8; i2++) {
                    int tg = axisTg[a2][i2].load(std::memory_order_relaxed);
                    if (tg < 0) continue;
                    int md = axisMode[a2][i2];
                    float v = av;
                    if (md == 1) {
                        v = 1.f - av;
                    } else if (md >= 2) {
                        if (axisShClk == 0) {
                            if (md == 2) {
                                axisHold[a2][i2] = av;
                            } else {
                                noiseState = noiseState * 1103515245u + 12345u;
                                float r = (float)(noiseState >> 16) / 65536.f - 1.f;
                                float j = av + r * 0.22f;
                                axisHold[a2][i2] = j < 0.f ? 0.f : (j > 1.f ? 1.f : j);
                            }
                        }
                        v = axisHold[a2][i2];
                    }
                    if (tg < 8) {
                        applyAssign(tg, v, fcT, qT, lrT, ldT, rwT, btT, rtT, drT);
                    } else {
                        int i3 = tg - 8;
                        fxAxisMask[i3 / 8] |= (unsigned char)(1u << (i3 % 8));
                        fxAxisVal[i3 / 8][i3 % 8] = v;
                    }
                }
            }
            int nm = netMode.load(std::memory_order_relaxed);
            if (nm > 0) {
                float nch = clamp01(netChaos.load(std::memory_order_relaxed));
                float nmd = clamp01(netMod.load(std::memory_order_relaxed));
                float nal = clamp01(netAlias.load(std::memory_order_relaxed));
                int nsp = (int)(clamp01(netSpeed.load(std::memory_order_relaxed)) * 3.f + 0.5f);
                int div = 64 >> nsp;
                if (div < 8) div = 8;
                netClk++;
                float avv[2] = {clamp01(ax.load()), clamp01(ay.load())};
                if ((netClk & 7) == 0) {
                    for (int a2 = 0; a2 < 2; a2++) {
                        netBird[a2][netBirdPos[a2]] = avv[a2];
                        netBirdPos[a2] = (netBirdPos[a2] + 1) & 15;
                    }
                }
                float jit = 0.f;
                bool burst = (netClk % div) == 0;
                if (burst) {
                    noiseState = noiseState * 1103515245u + 12345u;
                    jit = ((float)(noiseState >> 16) / 65536.f - 1.f) * nch;
                }
                if (nm == 7) {
                    netPhase += 32.f / (fs * 9.f);
                    if (netPhase >= 1.f) netPhase -= 1.f;
                }
                for (int a2 = 0; a2 < 2; a2++) {
                    int aO = 1 - a2;
                    float bird = netBird[aO][(netBirdPos[aO] - 8 + 16) & 15];
                    float cross = (avv[aO] - 0.5f) * nmd;
                    for (int i2 = 0; i2 < 8; i2++) {
                        int tg = axisTg[a2][i2].load(std::memory_order_relaxed);
                        if (tg < 8) continue;
                        int i3 = tg - 8;
                        float v = fxAxisVal[i3 / 8][i3 % 8];
                        if (nm == 1) {
                            v += cross * 0.5f + jit * 0.12f;
                        } else if (nm == 2) {
                            v += cross * 0.3f + jit * 0.04f;
                        } else if (nm == 3) {
                            v += jit * 0.45f + cross * 0.2f;
                            if (nal > 0.001f) {
                                float st = 0.5f - nal * 0.42f;
                                v = floorf(v / st + 0.5f) * st;
                            }
                        } else if (nm == 4) {
                            v += jit * 0.85f + cross * 1.6f;
                        } else if (nm == 5) {
                            v += (bird - 0.5f) * nmd * 0.9f + jit * 0.1f;
                        } else if (nm == 6) {
                            if (burst) v += jit * 0.5f;
                            if ((netClk / 3) & 1) v += cross * 0.3f;
                        } else {
                            v += (sinf(netPhase * 6.2831853f) * 0.5f + 0.5f) * nmd * 0.4f
                                 + cross * 0.2f;
                        }
                        if (v < 0.f) v = 0.f;
                        if (v > 1.f) v = 1.f;
                        fxAxisVal[i3 / 8][i3 % 8] = v;
                    }
                }
            }
            axisShClk++;
            if (axisShClk >= 192) axisShClk = 0;
            static const float clfoRates[6] = {0.25f, 0.5f, 1.f, 2.f, 4.f, 8.f};
            for (int L = 0; L < 2; L++) {
                if (!clfoOn[L].load(std::memory_order_relaxed)) continue;
                int ri = clfoRate[L].load(std::memory_order_relaxed);
                if (ri < 0) ri = 0;
                if (ri > 5) ri = 5;
                clfoPhase[L] += clfoRates[ri] * 32.f / fs;
                if (clfoPhase[L] >= 1.f) clfoPhase[L] -= 1.f;
                int tg = clfoTarget[L].load(std::memory_order_relaxed);
                float dep = clfoDepth[L].load(std::memory_order_relaxed);
                float amt = clfoAmount[L].load(std::memory_order_relaxed);
                if (tg >= 8) {
                    int i2 = tg - 8;
                    fxMod[i2 / 8][i2 % 8] += dep * amt * clfoVal(L);
                    continue;
                }
                if (tg < 0) continue;
                applyAssign(tg, clamp01(baseNorm[tg] + dep * amt * clfoVal(L)),
                            fcT, qT, lrT, ldT, rwT, btT, rtT, drT);
            }

            for (int l = 0; l < 16; l++) {
                if (!links[l].on.load(std::memory_order_relaxed)) continue;
                int src = links[l].src.load(std::memory_order_relaxed);
                int dst = links[l].dst.load(std::memory_order_relaxed);
                int sS = src - 8, dS = dst - 8;
                if (sS < 0 || dS < 0 || sS >= kNumSlots * 8 || dS >= kNumSlots * 8) continue;
                float amt = links[l].amount.load(std::memory_order_relaxed);
                if (amt == 0.f) continue;
                int chop = links[l].chop.load(std::memory_order_relaxed);
                float b = fmaxf(bpm.load(std::memory_order_relaxed), 40.f);
                static const float cDivs[5] = {1.f, 0.5f, 0.25f, 0.125f, 0.33333f};
                float per = (60.f / b) * (chop > 0 ? cDivs[chop - 1] : 0.25f) * fs;
                if (per < 2.f) per = 2.f;
                linkPhase[l] += 1.f;
                if (linkPhase[l] >= per) {
                    linkPhase[l] -= per;
                    float pr = links[l].prob.load(std::memory_order_relaxed);
                    noiseState = noiseState * 1103515245u + 12345u;
                    linkPass[l] = ((float)(noiseState >> 16) / 65536.f) < pr;
                }
                if (!linkPass[l]) continue;
                if (chop > 0 && (linkPhase[l] / per) >= 0.5f) continue;
                float sv = fxP(sS / 8, sS % 8) - 0.5f;
                float dly = links[l].delay.load(std::memory_order_relaxed) * fs;
                int di = (int)dly;
                float dv;
                if (di >= 1 && di < kLinkBuf - 2) {
                    int rp = linkWr[l] - di;
                    if (rp < 0) rp += kLinkBuf;
                    float fr = dly - (float)di;
                    int rp2 = rp - 1;
                    if (rp2 < 0) rp2 += kLinkBuf;
                    dv = linkBuf[l][rp] * (1.f - fr) + linkBuf[l][rp2] * fr;
                } else {
                    dv = sv;
                }
                linkBuf[l][linkWr[l]] = sv;
                linkWr[l] = (linkWr[l] + 1) % kLinkBuf;
                float soft = links[l].soft.load(std::memory_order_relaxed);
                float coef = soft > 0.f ? (0.35f * (1.f - soft) * (1.f - soft) + 0.0008f) : 1.f;
                linkSm[l] += coef * (dv - linkSm[l]);
                fxMod[dS / 8][dS % 8] += linkSm[l] * amt * 2.f;
            }

            int atg = arpTarget.load(std::memory_order_relaxed);
            if (atg >= 0 && arpOn.load(std::memory_order_relaxed)) {
                float aamt = arpAmt.load(std::memory_order_relaxed);
                float av = (arpModV * 2.f - 1.f) * aamt;
                if (atg >= 8) {
                    int i4 = atg - 8;
                    fxMod[i4 / 8][i4 % 8] += av;
                } else {
                    applyAssign(atg, clamp01(baseNorm[atg] + av),
                                fcT, qT, lrT, ldT, rwT, btT, rtT, drT);
                }
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
            int D = lb.decim > 0 ? lb.decim : 1;
            recAccum += c;
            recDiv++;
            if (recDiv >= D) {
                float v = recAccum / (float)D;
                int fl = recFlavor.load();
                if (fl >= 1 && fl <= 3) {
                    recWarmLp += 0.02f * (v - recWarmLp);
                    v += 0.25f * recWarmLp;
                    float sat = (fl == 2) ? 1.2f : (fl == 3) ? 2.2f : 1.f;
                    if (sat > 1.f) v = tanhf(v * sat) * 0.85f;
                    recFizzLp += ((fl == 3) ? 0.25f : 0.4f) * (v - recFizzLp);
                    v = recFizzLp;
                    int bits = (fl == 1) ? 10 : (fl == 2) ? 12 : 8;
                    float q = powf(2.f, (float)bits - 1.f);
                    v = floorf(v * q + 0.5f) / q;
                    noiseState = noiseState * 1103515245u + 12345u;
                    float nz = (float)(noiseState >> 16) / 32768.f - 1.f;
                    v += nz * ((fl == 2) ? 0.004f : (fl == 3) ? 0.010f : 0.002f);
                }
                lb.buf[2 * lb.pos] = v;
                lb.buf[2 * lb.pos + 1] = v;
                recAccum = 0.f;
                recDiv = 0;
                lb.pos++;
                if (lb.pos >= lb.samples) {
                    lb.playing = true;
                    lb.pos = 0;
                    recBank = -1;
                }
            }
        }

        float loopL = 0.f, loopR = 0.f;
        float dryL = 0.f, dryR = 0.f;
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
            if (lb.playing && !bankWasOn[k]) bankEnv[k] = 0.f;
            bankWasOn[k] = lb.playing;
            if (!lb.playing || lb.samples <= 0) continue;
            if (sm != 0 && (sm & (1 << (k / 2))) == 0) continue;
            if (req >= 0) {
                int tot = loopSliceTotal.load();
                if (tot > 0) lb.pos = (int)((float)req / tot * lb.samples);
                if (lb.pos >= lb.samples) lb.pos = 0;
                stReset(lb);
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
            float sL = lb.buf[2 * lb.pos];
            float sR = lb.buf[2 * lb.pos + 1];
            if (lb.stMode != 0 && !xyT) {
                float sdec = lb.decim > 0 ? (float)lb.decim : 1.f;
                float sp = prate;
                if (sp < 0.03f) sp = 0.03f;
                if (sp > 8.f) sp = 8.f;
                int Gt = (lb.stMode == 2)
                    ? (lb.steps > 0 ? (int)((float)lb.samples * sdec / (float)lb.steps) : 1024)
                    : (lb.stMode == 3 ? 1024 : 2048);
                if (Gt < 64) Gt = 64;
                if (Gt > 16384) Gt = 16384;
                int H = (lb.stMode == 2) ? Gt : Gt / 2;
                float accL = 0.f, accR = 0.f;
                for (int gi = 0; gi < 2; gi++) {
                    if (!lb.stGrainOn[gi]) continue;
                    int t = lb.stGrainAge[gi];
                    float u = (float)t / (float)Gt;
                    float w = (lb.stMode == 2) ? 1.f : 0.5f - 0.5f * cosf(2.f * 3.14159265f * u);
                    float ip = lb.stGrainBase[gi] + (float)t / sdec;
                    while (ip >= lb.samples) ip -= lb.samples;
                    int i0 = (int)ip;
                    if (i0 >= lb.samples) i0 = 0;
                    int i1 = (i0 + 1 >= lb.samples) ? 0 : i0 + 1;
                    float gl, gr;
                    if (lb.stMode == 3) {
                        gl = floorf(lb.buf[2 * i0] * 63.f + 0.5f) / 63.f;
                        gr = floorf(lb.buf[2 * i0 + 1] * 63.f + 0.5f) / 63.f;
                    } else {
                        float fr = ip - floorf(ip);
                        gl = lb.buf[2 * i0] + (lb.buf[2 * i1] - lb.buf[2 * i0]) * fr;
                        gr = lb.buf[2 * i0 + 1] + (lb.buf[2 * i1 + 1] - lb.buf[2 * i0 + 1]) * fr;
                    }
                    accL += w * gl;
                    accR += w * gr;
                    lb.stGrainAge[gi] = t + 1;
                    if (t + 1 >= Gt) lb.stGrainOn[gi] = 0;
                }
                lb.stHopCnt++;
                if (lb.stHopCnt >= H) {
                    lb.stHopCnt = 0;
                    lb.stCur = 1 - lb.stCur;
                    lb.stGrainOn[lb.stCur] = 1;
                    lb.stGrainAge[lb.stCur] = 0;
                    lb.stGrainBase[lb.stCur] = lb.stInPos;
                    lb.stInPos += (float)H * sp / sdec;
                    while (lb.stInPos >= lb.samples) lb.stInPos -= lb.samples;
                }
                sL = accL;
                sR = accR;
            }
            bankEnv[k] += (1.f - bankEnv[k]) * sAtkC[k];
            bankEnv[k] *= sDecC[k];
            if (bankEnv[k] > 1.f) bankEnv[k] = 1.f;
            bankLpL[k] += (sL - bankLpL[k]) * sToneA[k];
            bankLpR[k] += (sR - bankLpR[k]) * sToneA[k];
            sL = bankLpL[k] * bankEnv[k];
            sR = bankLpR[k] * bankEnv[k];
            if (bankFxSend[k]) {
                loopL += sL * vel;
                loopR += sR * vel;
            } else {
                dryL += sL * vel;
                dryR += sR * vel;
            }
            float dec = lb.decim > 0 ? (float)lb.decim : 1.f;
            if (xyT) {
                xyAcc += rate / dec;
                lb.pos += (int)xyAcc;
                xyAcc -= (int)xyAcc;
            } else {
                lb.acc += prate * sPitch[k] / dec;
                int adv = (int)lb.acc;
                lb.acc -= (int)lb.acc;
                lb.pos += bankRev[k] ? -adv : adv;
            }
            if (xyT) {
                if (lb.pos >= sEnd) lb.pos = sStart + ((lb.pos - sStart) % (sEnd - sStart));
            } else if (bankRev[k]) {
                if (lb.pos < sW0[k] || lb.pos >= sW1[k]) lb.pos = sW1[k] - 1;
            } else {
                if (lb.pos >= sW1[k] || lb.pos < sW0[k]) lb.pos = sW0[k];
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
        if (!st) stutMix += 0.02f * (0.f - stutMix);

        if (st) {
            int idx = (stutterLoopStart + stutterPos) % kStutterMax;
            int fade = stutterWindow / 8;
            if (fade > 256) fade = 256;
            float g = 1.f;
            if (stutterPos < fade) g = (float)stutterPos / fade;
            else if (stutterPos > stutterWindow - fade)
                g = (float)(stutterWindow - stutterPos) / fade;
            float loop = stutterBuf[idx] * g;
            stutMix += 0.006f * (1.f - stutMix);
            float mg = stutMix * stutMix * (3.f - 2.f * stutMix);
            liveL = liveL * (1.f - mg) + loop * mg;
            liveR = liveR * (1.f - mg) + loop * mg;
            stutterPos = (stutterPos + 1) % stutterWindow;
        }

        float mTarget = mute.load() ? 0.f : 1.f;
        muteGain += 0.0025f * (mTarget - muteGain);
        liveL *= muteGain; liveR *= muteGain;

        float outL = liveL + loopL * kLoopGain;
        float outR = liveR + loopR * kLoopGain;

        {
            float w = fxWet[0].load();
            float b = fmaxf(bpm.load(), 40.f);
            int dly = (int)((60.f / b) * (0.25f + 2.75f * fxP(0, 0)) * fs);
            if (dly > kDelayMax - 2) dly = kDelayMax - 2;
            int rp = (delayPos - dly + kDelayMax) % kDelayMax;
            float dl = delayBuf[2 * rp], dr = delayBuf[2 * rp + 1];
            float fb0 = 0.2f + 0.6f * fxP(0, 1);
            float tn0 = 0.05f + 0.4f * fxP(0, 2);
            delayLpL += tn0 * (dl * fb0 - delayLpL);
            delayLpR += tn0 * (dr * fb0 - delayLpR);
            delayBuf[2 * delayPos] = outL + delayLpL;
            delayBuf[2 * delayPos + 1] = outR + delayLpR;
            delayPos = (delayPos + 1) % kDelayMax;
            if (w > 0.001f) {
                float lv = w * (0.4f + 1.2f * fxP(0, 3));
                outL = outL * (1.f - w * 0.5f) + dl * lv;
                outR = outR * (1.f - w * 0.5f) + dr * lv;
            }
        }
        {
            float w = fxWet[1].load();
            float b = fmaxf(bpm.load(), 40.f);
            int dly = (int)((60.f / b) * (0.375f + 2.25f * fxP(1, 0)) * fs);
            if (dly > kDelayMax - 2) dly = kDelayMax - 2;
            int rp = (echoPos - dly + kDelayMax) % kDelayMax;
            float dl = echoBuf[2 * rp], dr = echoBuf[2 * rp + 1];
            float fb1 = 0.2f + 0.7f * fxP(1, 1);
            float tn1 = 0.02f + 0.2f * fxP(1, 2);
            echoLpL += tn1 * (dl * fb1 - echoLpL);
            echoLpR += tn1 * (dr * fb1 - echoLpR);
            echoBuf[2 * echoPos] = outL + echoLpL;
            echoBuf[2 * echoPos + 1] = outR + echoLpR;
            echoPos = (echoPos + 1) % kDelayMax;
            if (w > 0.001f) {
                float lv = w * (0.4f + 1.2f * fxP(1, 3));
                outL = outL * (1.f - w * 0.5f) + dl * lv;
                outR = outR * (1.f - w * 0.5f) + dr * lv;
            }
        }
        {
            float w = fxWet[2].load();
            if (w > 0.001f) {
                phLfo += (0.1f + 0.9f * fxP(2, 0)) * fsr;
                if (phLfo >= 1.f) phLfo -= 1.f;
                float lf = 0.5f - 0.5f * cosf(2.f * 3.14159265f * phLfo);
                float fc = (80.f + 240.f * fxP(2, 1)) * powf(40.f, lf * (0.6f + 0.8f * fxP(2, 2)));
                float g = tanf(3.14159265f * fc * fsr);
                if (g > 15.f) g = 15.f;
                float c = (g - 1.f) / (g + 1.f);
                float y1 = c * outL + phAp1; phAp1 = outL - c * y1;
                float y2 = c * y1 + phAp2; phAp2 = y1 - c * y2;
                float y3 = c * y2 + phAp3; phAp3 = y2 - c * y3;
                float y4 = c * y3 + phAp4; phAp4 = y3 - c * y4;
                float z1 = c * outR + phApR1; phApR1 = outR - c * z1;
                float z2 = c * z1 + phApR2; phApR2 = z1 - c * z2;
                float z3 = c * z2 + phApR3; phApR3 = z2 - c * z3;
                float z4 = c * z3 + phApR4; phApR4 = z3 - c * z4;
                float lv = w * (0.4f + 1.2f * fxP(2, 3));
                outL = outL * (1.f - w) + y4 * lv;
                outR = outR * (1.f - w) + z4 * lv;
            }
        }
        {
            float w = fxWet[3].load();
            if (w > 0.001f) {
                tremPhase += (0.5f + 7.f * fxP(3, 0)) * fsr;
                if (tremPhase >= 1.f) tremPhase -= 1.f;
                float lfw = 0.5f - 0.5f * cosf(2.f * 3.14159265f * tremPhase);
                float shp = fxP(3, 2);
                lfw = lfw * (1.f - shp) + (tremPhase < 0.5f ? 1.f : 0.f) * shp;
                float g = 1.f - w * (0.2f + 0.6f * fxP(3, 1)) * (1.f - lfw);
                outL *= g; outR *= g;
            }
        }
        {
            float w = fxWet[4].load();
            if (w > 0.001f) {
                panLfo += (0.1f + 0.8f * fxP(4, 0)) * fsr;
                if (panLfo >= 1.f) panLfo -= 1.f;
                float p = 0.5f - 0.5f * cosf(2.f * 3.14159265f * panLfo);
                float shp4 = fxP(4, 1);
                p = p * (1.f - shp4) + (panLfo < 0.5f ? 1.f : 0.f) * shp4;
                float pd = w * (0.2f + 0.6f * fxP(4, 3));
                outL *= 1.f - pd * p;
                outR *= 1.f - pd * (1.f - p);
            }
        }
        {
            float w = fxWet[5].load();
            if (w > 0.001f) {
                flLfo += (0.05f + 0.5f * fxP(5, 0)) * fsr;
                if (flLfo >= 1.f) flLfo -= 1.f;
                float dep = (2.f + 4.f * (0.5f - 0.5f * cosf(2.f * 3.14159265f * flLfo))) * 0.001f * fs * (0.5f + 1.5f * fxP(5, 1));
                int d0 = (int)dep;
                float fr = dep - (float)d0;
                int rp = (modPos - d0 + kModMax) % kModMax;
                int rp1 = (rp - 1 + kModMax) % kModMax;
                float dl = modBuf[2 * rp] * (1.f - fr) + modBuf[2 * rp1] * fr;
                float dr = modBuf[2 * rp + 1] * (1.f - fr) + modBuf[2 * rp1 + 1] * fr;
                float lv5 = w * (0.4f + 1.2f * fxP(5, 3));
                outL += lv5 * dl; outR += lv5 * dr;
                modBuf[2 * modPos] = outL + (0.2f + 0.7f * fxP(5, 2)) * dl;
                modBuf[2 * modPos + 1] = outR + (0.2f + 0.7f * fxP(5, 2)) * dr;
                modPos = (modPos + 1) % kModMax;
            }
        }
        {
            float w = fxWet[6].load();
            if (w > 0.001f) {
                choLfo += (0.15f + 0.9f * fxP(6, 0)) * fsr;
                if (choLfo >= 1.f) choLfo -= 1.f;
                float base = 0.020f * fs;
                float dep = 0.005f * fs * (0.5f + 1.5f * fxP(6, 1));
                float pl = 2.f * 3.14159265f * choLfo;
                float dl = base + dep * sinf(pl);
                float dr = base + dep * sinf(pl + 0.2f + 1.f * fxP(6, 2));
                int l0 = (int)dl; float lfr = dl - (float)l0;
                int r0 = (int)dr; float rfr = dr - (float)r0;
                int lp0 = (chorusPos - l0 + kModMax) % kModMax;
                int lp1 = (lp0 - 1 + kModMax) % kModMax;
                int rp0 = (chorusPos - r0 + kModMax) % kModMax;
                int rp1 = (rp0 - 1 + kModMax) % kModMax;
                float sl = chorusBuf[2 * lp0] * (1.f - lfr) + chorusBuf[2 * lp1] * lfr;
                float sr = chorusBuf[2 * rp0] * (1.f - rfr) + chorusBuf[2 * rp1] * rfr;
                float lv6 = w * (0.4f + 1.2f * fxP(6, 3));
                outL += lv6 * sl;
                outR += lv6 * sr;
                chorusBuf[2 * chorusPos] = outL;
                chorusBuf[2 * chorusPos + 1] = outR;
                chorusPos = (chorusPos + 1) % kModMax;
            }
        }
        {
            float w = fxWet[7].load();
            if (w > 0.001f) {
                ringPhase += (110.f + 1100.f * fxP(7, 0)) * fsr;
                if (ringPhase >= 1.f) ringPhase -= 1.f;
                float car = sinf(2.f * 3.14159265f * ringPhase);
                float crp = fxP(7, 1);
                if (crp > 0.001f)
                    car = car * (1.f - crp) + (ringPhase < 0.5f ? 1.f : -1.f) * crp;
                float lv7 = w * (0.4f + 1.2f * fxP(7, 2));
                outL = outL * (1.f - w) + outL * car * lv7;
                outR = outR * (1.f - w) + outR * car * lv7;
            }
        }
        {
            float w = fxWet[8].load();
            if (w > 0.001f) {
                wowBuf[2 * wowPos] = outL;
                wowBuf[2 * wowPos + 1] = outR;
                wowPhase += (0.1f + 0.8f * fxP(8, 0)) * fsr;
                if (wowPhase >= 1.f) wowPhase -= 1.f;
                flutPhase += (1.f + 11.f * fxP(8, 1)) * fsr;
                if (flutPhase >= 1.f) flutPhase -= 1.f;
                float dep = 48.f + 24.f * sinf(2.f * 3.14159265f * wowPhase)
                          + 4.f * sinf(2.f * 3.14159265f * flutPhase);
                int d0 = (int)dep;
                float fr = dep - (float)d0;
                int rp = (wowPos - d0 + kWowMax) % kWowMax;
                int rp1 = (rp - 1 + kWowMax) % kWowMax;
                float dl = wowBuf[2 * rp] * (1.f - fr) + wowBuf[2 * rp1] * fr;
                float dr = wowBuf[2 * rp + 1] * (1.f - fr) + wowBuf[2 * rp1 + 1] * fr;
                outL = outL * (1.f - w) + dl * w;
                outR = outR * (1.f - w) + dr * w;
                float tn8 = 0.03f + 0.22f * fxP(8, 3);
                lofiLpL += tn8 * (outL - lofiLpL);
                lofiLpR += tn8 * (outR - lofiLpR);
                lofiNoise = lofiNoise * 1103515245u + 12345u;
                float hiss = ((float)(lofiNoise >> 16) / 32768.f - 1.f) * 0.012f * (0.2f + 1.6f * fxP(8, 2));
                outL = outL * (1.f - w) + (lofiLpL + hiss) * w;
                outR = outR * (1.f - w) + (lofiLpR + hiss) * w;
                wowPos = (wowPos + 1) % kWowMax;
            }
        }
        {
            float w = fxWet[9].load();
            if (w > 0.001f) {
                static const int8_t gpats[4][16] = {
                    {1,0,1,1,0,1,0,1,1,0,1,0,0,1,1,0},
                    {1,0,0,1,0,0,1,0,1,0,0,1,0,0,1,0},
                    {1,1,0,1,1,0,1,1,0,1,1,0,1,1,0,1},
                    {1,0,0,0,1,0,0,0,1,0,0,0,1,0,1,0}
                };
                float b = fmaxf(bpm.load(), 40.f);
                int per = (int)((60.f / b) * 0.25f * fs / (0.5f + 2.f * fxP(9, 0)));
                if (per < 32) per = 32;
                gateClock = (gateClock + 1) % (per * 16);
                int st = gateClock / per;
                int ip = gateClock % per;
                int gi9 = (int)(fxP(9, 2) * 3.99f);
                float g = gpats[gi9][st] ? 1.f : 0.f;
                int sm = (int)(16.f + 240.f * fxP(9, 1));
                if (ip < sm) g *= (float)ip / (float)sm;
                if (per - ip < sm) g *= (float)(per - ip) / (float)sm;
                float kw = w * (0.4f + 1.2f * fxP(9, 3));
                float k = 1.f - kw + kw * g;
                outL *= k; outR *= k;
            }
        }
        {
            float w = fxWet[10].load();
            if (w > 0.001f) {
                if (!prevFreeze) {
                    freezeFill = 0; freezeFull = false; freezePos = 0; freezePosF = 0.f;
                    freezeLen = (int)((float)kFreezeMax * (0.25f + 0.75f * fxP(10, 0)));
                    if (freezeLen < 64) freezeLen = 64;
                }
                if (!freezeFull) {
                    freezeBuf[2 * freezeFill] = outL;
                    freezeBuf[2 * freezeFill + 1] = outR;
                    freezeFill++;
                    if (freezeFill >= freezeLen) {
                        freezeFull = true;
                        freezePosF = (float)freezeLen * fxP(10, 1);
                        if (freezePosF >= (float)freezeLen) freezePosF = 0.f;
                        freezePos = (int)freezePosF;
                    }
                } else {
                    int i0f = (int)freezePosF;
                    float frf = freezePosF - (float)i0f;
                    int i1f = (i0f + 1) % freezeLen;
                    float fll = freezeBuf[2 * i0f] * (1.f - frf) + freezeBuf[2 * i1f] * frf;
                    float frn = freezeBuf[2 * i0f + 1] * (1.f - frf) + freezeBuf[2 * i1f + 1] * frf;
                    float lv10 = w * (0.4f + 1.2f * fxP(10, 3));
                    outL = outL * (1.f - w) + fll * lv10;
                    outR = outR * (1.f - w) + frn * lv10;
                    freezePosF += 0.5f + 1.f * fxP(10, 2);
                    if (freezePosF >= (float)freezeLen) freezePosF -= (float)freezeLen;
                    if (freezePosF < 0.f) freezePosF += (float)freezeLen;
                    freezePos = (int)freezePosF;
                }
            }
            prevFreeze = w > 0.001f;
        }
        {
            float w = fxWet[11].load();
            if (w > 0.001f) {
                float lvl = 0.5f * (fabsf(outL) + fabsf(outR));
                compEnv += (lvl > compEnv ? 0.02f : 0.0005f + 0.002f * fxP(11, 2)) * (lvl - compEnv);
                float g = 1.f;
                float thr = 0.1f + 0.5f * fxP(11, 0);
                if (compEnv > thr) g = thr / compEnv;
                g *= 0.7f + 0.9f * fxP(11, 1);
                g = 1.f + w * (g - 1.f);
                g *= 0.4f + 1.2f * fxP(11, 3);
                outL *= g; outR *= g;
            }
        }
        {
            float w = fxWet[12].load();
            stopBuf[2 * stopWrite] = outL;
            stopBuf[2 * stopWrite + 1] = outR;
            stopWrite = (stopWrite + 1) % kStopMax;
            if (w > 0.001f && !prevStop) {
                stopRead = (float)((stopWrite - 480 + kStopMax) % kStopMax);
                stopRate = 1.f;
            }
            prevStop = w > 0.001f;
            if (w > 0.001f) {
                stopRate -= (0.2f + 1.2f * fxP(12, 0)) * fsr;
                if (stopRate < 0.f) stopRate = 0.f;
            } else if (stopRate < 1.f) {
                stopRate += (0.5f + 3.f * fxP(12, 1)) * fsr;
                if (stopRate > 1.f) stopRate = 1.f;
            }
            if (stopRate < 0.999f) {
                if (w > 0.001f) {
                    float smin = (1.f - fxP(12, 2)) * 0.3f;
                    if (stopRate < smin) stopRate = smin;
                }
                float weff = w > 0.001f ? w * (0.4f + 1.2f * fxP(12, 3)) : 1.f;
                stopRead += stopRate;
                if (stopRead >= (float)kStopMax) stopRead -= (float)kStopMax;
                int i0 = (int)stopRead;
                float fr = stopRead - (float)i0;
                int i1 = (i0 + 1) % kStopMax;
                float dl = stopBuf[2 * i0] * (1.f - fr) + stopBuf[2 * i1] * fr;
                float dr = stopBuf[2 * i0 + 1] * (1.f - fr) + stopBuf[2 * i1 + 1] * fr;
                float fade = stopRate * 4.f;
                if (fade > 1.f) fade = 1.f;
                outL = outL * (1.f - weff) + dl * weff * fade;
                outR = outR * (1.f - weff) + dr * weff * fade;
            }
        }
        {
            float w = fxWet[13].load();
            if (w > 0.001f) {
                if (!prevBounce) {
                    float b = fmaxf(bpm.load(), 40.f);
                    bounceWin = (int)((60.f / b) * 0.125f * fs * (0.25f + 3.f * fxP(13, 0)));
                    if (bounceWin < 64) bounceWin = 64;
                    if (bounceWin > kBeatMax / 2) bounceWin = kBeatMax / 2;
                    bounceFill = 0;
                    bouncePos = 0;
                    bounceFull = false;
                }
                if (!bounceFull) {
                    bounceBuf[2 * bounceFill] = outL;
                    bounceBuf[2 * bounceFill + 1] = outR;
                    bounceFill++;
                    if (bounceFill >= bounceWin) {
                        bounceFull = true;
                        bouncePosF = (float)bounceWin * fxP(13, 1);
                        if (bouncePosF >= (float)bounceWin) bouncePosF = 0.f;
                        bouncePos = (int)bouncePosF;
                    }
                } else {
                    int i0b = (int)bouncePosF;
                    float frb = bouncePosF - (float)i0b;
                    int i1b = (i0b + 1) % bounceWin;
                    float bll = bounceBuf[2 * i0b] * (1.f - frb) + bounceBuf[2 * i1b] * frb;
                    float brn = bounceBuf[2 * i0b + 1] * (1.f - frb) + bounceBuf[2 * i1b + 1] * frb;
                    float lv13 = w * (0.4f + 1.2f * fxP(13, 3));
                    outL = outL * (1.f - w) + bll * lv13;
                    outR = outR * (1.f - w) + brn * lv13;
                    bouncePosF += 0.5f + 1.f * fxP(13, 2);
                    if (bouncePosF >= (float)bounceWin) bouncePosF -= (float)bounceWin;
                    if (bouncePosF < 0.f) bouncePosF += (float)bounceWin;
                    bouncePos = (int)bouncePosF;
                }
            }
            prevBounce = w > 0.001f;
        }
        {
            float w = fxWet[14].load();
            if (w > 0.001f) {
                if (!prevRev) {
                    int rlen = (int)((float)kRevMax * (0.25f + 0.5f * fxP(14, 0)));
                    if (rlen > kRevMax - 4) rlen = kRevMax - 4;
                    int rst = (int)((float)rlen * fxP(14, 1));
                    revRead = (float)((revWrite + rst) % kRevMax);
                }
                revBuf[2 * revWrite] = outL;
                revBuf[2 * revWrite + 1] = outR;
                revWrite = (revWrite + 1) % kRevMax;
                revRead -= 0.5f + 1.f * fxP(14, 2);
                if (revRead < 0.f) revRead += (float)kRevMax;
                int i0 = (int)revRead;
                float fr = revRead - (float)i0;
                int i1 = (i0 - 1 + kRevMax) % kRevMax;
                float dl = revBuf[2 * i0] * (1.f - fr) + revBuf[2 * i1] * fr;
                float dr = revBuf[2 * i0 + 1] * (1.f - fr) + revBuf[2 * i1 + 1] * fr;
                float lv14 = w * (0.4f + 1.2f * fxP(14, 3));
                outL = outL * (1.f - w) + dl * lv14;
                outR = outR * (1.f - w) + dr * lv14;
            }
            prevRev = w > 0.001f;
        }
        {
            float w = fxWet[15].load();
            if (w > 0.001f) {
                float f0 = 55.f * powf(2.f, (float)rootNote.load() / 12.f) * 2.f * powf(2.f, (fxP(15, 0) - 0.5f) * 2.f);
                float s = 0.5f * (outL + outR);
                float acc2 = 0.f;
                for (int h = 0; h < 4; h++) {
                    float fh = f0 * (float)(h + 1);
                    if (fh > 4000.f) fh = 4000.f;
                    int d = (int)(fs / fh);
                    if (d < 2) d = 2;
                    if (d > kResoMax) d = kResoMax;
                    float y = resoBuf[h][(resoPos - d + kResoMax) % kResoMax];
                    resoBuf[h][resoPos] = s + (0.92f + 0.08f * fxP(15, 1) - 0.01f * (float)h) * y;
                    acc2 += y;
                }
                acc2 *= 0.3f + 0.6f * fxP(15, 2);
                float lv15 = w * (0.4f + 1.2f * fxP(15, 3));
                outL = outL * (1.f - w) + acc2 * lv15;
                outR = outR * (1.f - w) + acc2 * lv15;
                resoPos = (resoPos + 1) % kResoMax;
            }
        }
        {
            float w = fxWet[16].load();
            if (w > 0.001f) {
                static const int sprT[3] = {331, 571, 877};
                float sc16 = 0.6f + 0.8f * fxP(16, 0);
                sprLfoPh += 1.7f * fsr;
                if (sprLfoPh >= 1.f) sprLfoPh -= 1.f;
                float wob = sinf(2.f * 3.14159265f * sprLfoPh) * 8.f * fxP(16, 2);
                float s = 0.5f * (outL + outR);
                float acc2 = 0.f;
                for (int c2 = 0; c2 < 3; c2++) {
                    int d16 = (int)((float)sprT[c2] * sc16 + wob);
                    if (d16 < 2) d16 = 2;
                    if (d16 >= kSprMax) d16 = kSprMax - 1;
                    float y = sprBuf[c2][(sprPos - d16 + kSprMax) % kSprMax];
                    sprFilt[c2] = y * 0.5f + sprFilt[c2] * 0.5f;
                    sprBuf[c2][sprPos] = s + sprFilt[c2] * (0.6f + 0.44f * fxP(16, 1));
                    acc2 += y;
                }
                acc2 *= 0.33f;
                float y1s = 0.6f * acc2 + sprAp1; sprAp1 = acc2 - 0.6f * y1s;
                float y2s = 0.6f * y1s + sprAp2; sprAp2 = y1s - 0.6f * y2s;
                float lv16 = w * (0.4f + 1.2f * fxP(16, 3));
                outL = outL * (1.f - w) + y2s * lv16;
                outR = outR * (1.f - w) + y2s * lv16;
                sprPos = (sprPos + 1) % kSprMax;
            }
        }
        {
            float w = fxWet[17].load();
            if (w > 0.001f) {
                float k = (1.f + w * 5.f) * (0.4f + 1.2f * fxP(17, 0)) * (0.4f + 1.2f * fxP(17, 2));
                float bias = (fxP(17, 1) - 0.5f) * 1.2f;
                float yl = sinf((outL + bias) * k * 1.5708f) - sinf(bias * k * 1.5708f);
                float yr = sinf((outR + bias) * k * 1.5708f) - sinf(bias * k * 1.5708f);
                float lv17 = w * (0.4f + 1.2f * fxP(17, 3));
                outL = outL * (1.f - w) + yl * lv17 * 0.9f;
                outR = outR * (1.f - w) + yr * lv17 * 0.9f;
            }
        }
        {
            float w = fxWet[18].load();
            if (w > 0.001f) {
                scrBuf[2 * scrWrite] = outL;
                scrBuf[2 * scrWrite + 1] = outR;
                scrWrite = (scrWrite + 1) % kScrMax;
                scrPh += (0.3f + 3.8f * fxP(18, 0)) * fsr;
                if (scrPh >= 1.f) scrPh -= 1.f;
                float sc18 = 0.4f + 1.2f * fxP(18, 1);
                float sens = fxP(18, 2) > 0.5f ? -1.f : 1.f;
                float lag = 16000.f * sc18 + 14000.f * sc18 * sens * cosf(2.f * 3.14159265f * scrPh);
                if (lag < 100.f) lag = 100.f;
                float rp = (float)scrWrite - lag;
                while (rp < 0.f) rp += (float)kScrMax;
                int i0 = (int)rp;
                float fr = rp - (float)i0;
                int i1 = (i0 + 1) % kScrMax;
                float dl = scrBuf[2 * i0] * (1.f - fr) + scrBuf[2 * i1] * fr;
                float dr = scrBuf[2 * i0 + 1] * (1.f - fr) + scrBuf[2 * i1 + 1] * fr;
                float lv18 = w * (0.4f + 1.2f * fxP(18, 3));
                outL = outL * (1.f - w) + dl * lv18;
                outR = outR * (1.f - w) + dr * lv18;
            }
        }
        {
            float w = fxWet[19].load();
            if (w > 0.001f) {
                grainBuf[2 * gWrite] = outL;
                grainBuf[2 * gWrite + 1] = outR;
                float gl = 0.f, gr = 0.f;
                for (int v = 0; v < 4; v++) {
                    if (!gOn[v] && !(ecoMode.load(std::memory_order_relaxed) && v >= 2)) {
                        noiseState = noiseState * 1103515245u + 12345u;
                        unsigned r = noiseState >> 16;
                        gLen[v] = (0.03f * fs + (float)(r % 1200u)) * (0.4f + 1.2f * fxP(19, 0));
                        gPos[v] = (float)((gWrite - (int)((4800u + r % 38400u) * (0.3f + 1.4f * fxP(19, 2))) + kGrainMax) % kGrainMax);
                        gAge[v] = 0.f;
                        noiseState = noiseState * 1103515245u + 12345u;
                        int semi = (int)(((float)((noiseState >> 16) % 13u) - 6.f) * (0.4f + 1.6f * fxP(19, 1)));
                        gRate[v] = powf(2.f, (float)semi / 12.f);
                        gOn[v] = true;
                    }
                    float ph = gAge[v] / gLen[v];
                    float env = sinf(3.14159265f * ph);
                    int i0 = (int)gPos[v];
                    float fr = gPos[v] - (float)i0;
                    int i1 = (i0 + 1) % kGrainMax;
                    gl += (grainBuf[2 * i0] * (1.f - fr) + grainBuf[2 * i1] * fr) * env;
                    gr += (grainBuf[2 * i0 + 1] * (1.f - fr) + grainBuf[2 * i1 + 1] * fr) * env;
                    gPos[v] += gRate[v];
                    if (gPos[v] >= (float)kGrainMax) gPos[v] -= (float)kGrainMax;
                    if (gPos[v] < 0.f) gPos[v] += (float)kGrainMax;
                    gAge[v] += fsr;
                    if (gAge[v] >= gLen[v]) gOn[v] = false;
                }
                float lv19 = w * (0.4f + 1.2f * fxP(19, 3));
                outL = outL * (1.f - w) + gl * 0.5f * lv19;
                outR = outR * (1.f - w) + gr * 0.5f * lv19;
                gWrite = (gWrite + 1) % kGrainMax;
            }
        }
        {
            float w = fxWet[20].load();
            if (w > 0.001f) {
                grainBuf[2 * gWrite] = outL;
                grainBuf[2 * gWrite + 1] = outR;
                float gl = 0.f, gr = 0.f;
                float fol = fxP(20, 1);
                for (int v = 0; v < 6; v++) {
                    if (!gOn2[v] && !(ecoMode.load(std::memory_order_relaxed) && v >= 2)) {
                        noiseState = noiseState * 1103515245u + 12345u;
                        unsigned r = noiseState >> 16;
                        gLen2[v] = (0.04f * fs + (float)(r % 2400u)) * (0.3f + 1.3f * fxP(20, 0));
                        noiseState = noiseState * 1103515245u + 12345u;
                        unsigned r2 = noiseState >> 16;
                        if ((r2 % 100u) < (unsigned)(30.f + 50.f * fol)) {
                            gOffV2[v] = gLastOff2;
                        } else {
                            gOffV2[v] = (int)((2400u + (r2 >> 6) % 40000u) * (0.25f + 1.5f * fxP(20, 2)));
                            gLastOff2 = gOffV2[v];
                        }
                        gPos2[v] = (float)((gWrite - gOffV2[v] + kGrainMax) % kGrainMax);
                        gAge2[v] = 0.f;
                        noiseState = noiseState * 1103515245u + 12345u;
                        unsigned r3 = noiseState >> 16;
                        static const int jungSemi[10] = {0, 12, -12, 7, 3, 10, -5, 15, 5, 24};
                        int semi = jungSemi[r3 % 10u];
                        if ((r3 >> 8) % 3u == 0u) semi = (int)(((float)(r3 % 25u) - 12.f) * (0.3f + 1.2f * fol));
                        gRate2[v] = powf(2.f, (float)semi / 12.f);
                        if ((noiseState >> 20) % (unsigned)(2 + (int)(3.f * fol)) == 0u) gRate2[v] = -gRate2[v];
                        gPan2[v] = 0.15f + 0.7f * (float)((noiseState >> 12) % 1000u) / 1000.f;
                        gOn2[v] = true;
                    }
                    float ph = gAge2[v] / gLen2[v];
                    float env = sinf(3.14159265f * ph);
                    env = env * env;
                    int i0 = (int)gPos2[v];
                    float fr = gPos2[v] - (float)i0;
                    int i1 = (i0 + 1) % kGrainMax;
                    float sl = (grainBuf[2 * i0] * (1.f - fr) + grainBuf[2 * i1] * fr) * env;
                    float sr = (grainBuf[2 * i0 + 1] * (1.f - fr) + grainBuf[2 * i1 + 1] * fr) * env;
                    float sm = (sl + sr) * 0.5f;
                    float pn = gPan2[v];
                    gl += sm * (1.f - pn) * 1.4f;
                    gr += sm * pn * 1.4f;
                    gPos2[v] += gRate2[v];
                    if (gPos2[v] >= (float)kGrainMax) gPos2[v] -= (float)kGrainMax;
                    if (gPos2[v] < 0.f) gPos2[v] += (float)kGrainMax;
                    gAge2[v] += fsr;
                    if (gAge2[v] >= gLen2[v]) gOn2[v] = false;
                }
                float lv20 = w * (0.5f + 1.3f * fxP(20, 3));
                outL = outL * (1.f - w) + tanhf(gl * 1.1f) * 0.9f * lv20;
                outR = outR * (1.f - w) + tanhf(gr * 1.1f) * 0.9f * lv20;
                gWrite = (gWrite + 1) % kGrainMax;
            }
        }
        {
            float w = fxWet[21].load();
            if (w > 0.001f) {
                if (!prevTransit) transitPh = 0.f;
                float b = fmaxf(bpm.load(), 40.f);
                float dur = 2.f * (60.f / b) * (0.5f + 3.f * fxP(21, 0));
                if (transitPh < 1.f) {
                    transitPh += fsr / dur;
                    if (transitPh > 1.f) transitPh = 1.f;
                }
                float e = transitPh;
                float fc = 300.f * powf(8.f, fxP(21, 1) - 0.5f) * powf(40.f, e);
                float kk = 1.f - expf(-2.f * 3.14159265f * fc * fsr);
                trLpL += kk * (outL - trLpL);
                trLpR += kk * (outR - trLpR);
                float rl = reverbL.process(outL) * e * (0.4f + 1.2f * fxP(21, 2));
                float rr = reverbR.process(outR) * e * (0.4f + 1.2f * fxP(21, 2));
                float wl = w * (0.3f + 0.7f * e);
                outL = outL * (1.f - wl) + (trLpL + rl) * wl;
                outR = outR * (1.f - wl) + (trLpR + rr) * wl;
            }
            prevTransit = w > 0.001f;
        }
        {
            float w = fxWet[22].load();
            if (w > 0.001f) {
                float k = (1.f + w * 6.f) * (0.4f + 1.2f * fxP(22, 0));
                float xl = outL * k + 0.15f * (0.4f + 1.2f * fxP(22, 2)) * outL * outL;
                float xr = outR * k + 0.15f * (0.4f + 1.2f * fxP(22, 2)) * outR * outR;
                float yl = tanhf(xl) * 0.85f;
                float yr = tanhf(xr) * 0.85f;
                float tn22 = 0.005f + 0.05f * fxP(22, 1);
                comWarmL += tn22 * (yl - comWarmL);
                comWarmR += tn22 * (yr - comWarmR);
                yl += 0.2f * comWarmL;
                yr += 0.2f * comWarmR;
                outL = outL * (1.f - w) + yl * w;
                outR = outR * (1.f - w) + yr * w;
            }
        }
        {
            float w = fxWet[23].load();
            if (w > 0.001f) {
                float inL = outL, inR = outR;
                float b = fmaxf(bpm.load(), 40.f);
                int per = (int)((60.f / b) * fs / (0.5f + 2.f * fxP(23, 0)));
                if (per < 64) per = 64;
                turnClock = (turnClock + 1) % (per * 8);
                if (turnClock % per == 0) {
                    noiseState = noiseState * 1103515245u + 12345u;
                    turnSlot = (int)((noiseState >> 16) % 8u);
                }
                switch (turnSlot) {
                    case 0: {
                        turnHold++;
                        if (turnHold >= 8) {
                            turnHold = 0;
                            turnHeldL = outL;
                            turnHeldR = outR;
                        }
                        float q = 16.f;
                        outL = floorf(turnHeldL * q + 0.5f) / q;
                        outR = floorf(turnHeldR * q + 0.5f) / q;
                        break;
                    }
                    case 1: {
                        int ph = turnClock % (per / 2);
                        float g = ((float)ph < (float)per * (0.15f + 0.35f * fxP(23, 2))) ? 1.f : 0.f;
                        outL *= g; outR *= g;
                        break;
                    }
                    case 2: {
                        turnPh += 440.f * fsr;
                        if (turnPh >= 1.f) turnPh -= 1.f;
                        float car = sinf(2.f * 3.14159265f * turnPh);
                        outL *= car; outR *= car;
                        break;
                    }
                    case 3: {
                        outL = sinf(outL * 4.f * (0.4f + 1.2f * fxP(23, 1)) * 1.5708f) * 0.9f;
                        outR = sinf(outR * 4.f * (0.4f + 1.2f * fxP(23, 1)) * 1.5708f) * 0.9f;
                        break;
                    }
                    case 4: {
                        turnPh += 8.f * fsr;
                        if (turnPh >= 1.f) turnPh -= 1.f;
                        float g = 0.5f - 0.5f * cosf(2.f * 3.14159265f * turnPh);
                        outL *= g; outR *= g;
                        break;
                    }
                    case 5: {
                        turnLpL += 0.05f * (outL - turnLpL);
                        turnLpR += 0.05f * (outR - turnLpR);
                        outL = turnLpL; outR = turnLpR;
                        break;
                    }
                    case 6: {
                        turnBuf[2 * turnWrite] = outL;
                        turnBuf[2 * turnWrite + 1] = outR;
                        turnWrite = (turnWrite + 1) % kTurnMax;
                        turnLag += 0.5f;
                        if (turnLag >= 12000.f) turnLag = 0.f;
                        float rp = (float)((turnWrite - (int)turnLag - 2400 + 2 * kTurnMax) % kTurnMax);
                        int i0 = (int)rp;
                        float fr = rp - (float)i0;
                        int i1 = (i0 + 1) % kTurnMax;
                        outL = turnBuf[2 * i0] * (1.f - fr) + turnBuf[2 * i1] * fr;
                        outR = turnBuf[2 * i0 + 1] * (1.f - fr) + turnBuf[2 * i1 + 1] * fr;
                        break;
                    }
                    default: {
                        float xl = tanhf(outL * 4.f * (0.4f + 1.2f * fxP(23, 1)));
                        float xr = tanhf(outR * 4.f * (0.4f + 1.2f * fxP(23, 1)));
                        float q = 8.f;
                        outL = floorf(xl * q + 0.5f) / q;
                        outR = floorf(xr * q + 0.5f) / q;
                        break;
                    }
                }
                outL = inL * (1.f - w) + outL * w;
                outR = inR * (1.f - w) + outR * w;
            }
        }
        {
            float w = fxWet[24].load();
            if (w > 0.001f) {
                if (!prevFall) fallPh = 0.f;
                float b = fmaxf(bpm.load(), 40.f);
                float dur = 2.f * (60.f / b) * (0.5f + 3.f * fxP(24, 0));
                if (fallPh < 1.f) {
                    fallPh += fsr / dur;
                    if (fallPh > 1.f) fallPh = 1.f;
                }
                float e = fallPh;
                float fc = 12000.f * powf(8.f, fxP(24, 1) - 0.5f) * powf(300.f / 12000.f, e);
                float kk = 1.f - expf(-2.f * 3.14159265f * fc * fsr);
                faLpL += kk * (outL - faLpL);
                faLpR += kk * (outR - faLpR);
                float rl = reverbL.process(outL) * (1.f - e) * 0.8f * (0.4f + 1.2f * fxP(24, 2));
                float rr = reverbR.process(outR) * (1.f - e) * 0.8f * (0.4f + 1.2f * fxP(24, 2));
                float wl = w * (0.3f + 0.7f * e);
                outL = outL * (1.f - wl) + (faLpL + rl) * wl;
                outR = outR * (1.f - wl) + (faLpR + rr) * wl;
            }
            prevFall = w > 0.001f;
        }
        {
            float w = fxWet[25].load();
            if (w > 0.001f) {
                if (!prevSwell) swellPh = 0.f;
                float b = fmaxf(bpm.load(), 40.f);
                float dur = 2.f * (60.f / b) * (0.5f + 3.f * fxP(25, 0));
                if (swellPh < 1.f) {
                    swellPh += fsr / dur;
                    if (swellPh > 1.f) swellPh = 1.f;
                }
                float g = powf(swellPh, 4.f - 4.f * fxP(25, 1));
                outL *= g; outR *= g;
                if (fxP(25, 2) > 0.001f) {
                    noiseState = noiseState * 1103515245u + 12345u;
                    float nz = ((float)(noiseState >> 16) / 32768.f - 1.f) * 0.01f * fxP(25, 2) * g;
                    outL += nz; outR += nz;
                }
            }
            prevSwell = w > 0.001f;
        }
        {
            float w = fxWet[26].load();
            if (w > 0.001f) {
                float b = fmaxf(bpm.load(), 40.f);
                int per = (int)((60.f / b) * fs);
                if (per < 64) per = 64;
                int ph = pumpClock % per;
                pumpClock = (pumpClock + 1) % per;
                float t = powf((float)ph / (float)per, 1.5f - fxP(26, 2));
                float g = 1.f - w * (0.3f + 1.f * fxP(26, 1)) * expf(-t * (3.f + 8.f * fxP(26, 0)));
                outL *= g; outR *= g;
            }
        }
        {
            float w = fxWet[27].load();
            if (w > 0.001f) {
                float p0 = fxP(27, 0), p1 = fxP(27, 1), p2 = fxP(27, 2), p3 = fxP(27, 3);
                float b = fmaxf(bpm.load(), 40.f);
                float mult = 0.25f + p0 * 2.75f;
                int d = (int)(mult * (60.f / b) * fs);
                if (d < 64) d = 64;
                if (d > kDubMax - 2) d = kDubMax - 2;
                int i0 = (dubPos - d + kDubMax) % kDubMax;
                float dl = dubBuf[2 * i0];
                float dr = dubBuf[2 * i0 + 1];
                float lpk = 0.05f + p2 * 0.45f;
                float fb = 0.3f + p1 * 0.65f;
                float drv = 1.f + p3 * 1.5f;
                dubLpL += lpk * (dl - dubLpL);
                dubLpR += lpk * (dr - dubLpR);
                dubBuf[2 * dubPos] = outL + tanhf(dubLpL * drv) * fb;
                dubBuf[2 * dubPos + 1] = outR + tanhf(dubLpR * drv) * fb;
                dubPos = (dubPos + 1) % kDubMax;
                outL = outL * (1.f - w) + tanhf(dubLpL * (drv + 0.1f)) * w;
                outR = outR * (1.f - w) + tanhf(dubLpR * (drv + 0.1f)) * w;
            }
        }
        {
            float w = fxWet[28].load();
            if (w > 0.001f) {
                float k1 = 0.01f + fxP(28, 0) * 0.08f;
                float k2 = 0.15f + fxP(28, 1) * 0.45f;
                float drv = 2.f + fxP(28, 2) * 6.f;
                float humA = fxP(28, 3) * 0.06f;
                megaLp1L += k1 * (outL - megaLp1L);
                megaLp1R += k1 * (outR - megaLp1R);
                float hpl = outL - megaLp1L;
                float hpr = outR - megaLp1R;
                megaLp2L += k2 * (hpl - megaLp2L);
                megaLp2R += k2 * (hpr - megaLp2R);
                megaPh += 120.f * fsr;
                if (megaPh >= 1.f) megaPh -= 1.f;
                float hum = humA * sinf(2.f * 3.14159265f * megaPh);
                float yl = tanhf((megaLp2L + hum) * drv) * 0.8f;
                float yr = tanhf((megaLp2R + hum) * drv) * 0.8f;
                outL = outL * (1.f - w) + yl * w;
                outR = outR * (1.f - w) + yr * w;
            }
        }
        {
            float w = fxWet[29].load();
            if (w > 0.001f) {
                tapeBuf[2 * tapePos] = outL;
                tapeBuf[2 * tapePos + 1] = outR;
                tapePos = (tapePos + 1) % kTapeMax;
                tapePhase += 0.4f * fsr;
                if (tapePhase >= 1.f) tapePhase -= 1.f;
                float lag = 900.f + (20.f + fxP(29, 0) * 140.f) * sinf(2.f * 3.14159265f * tapePhase);
                float rp = (float)tapePos - lag;
                while (rp < 0.f) rp += (float)kTapeMax;
                int i0 = (int)rp;
                float fr = rp - (float)i0;
                int i1 = (i0 + 1) % kTapeMax;
                float dl = tapeBuf[2 * i0] * (1.f - fr) + tapeBuf[2 * i1] * fr;
                float dr = tapeBuf[2 * i0 + 1] * (1.f - fr) + tapeBuf[2 * i1 + 1] * fr;
                if (tapeDropLen > 0) {
                    tapeDropLen--;
                    tapeGain += 0.01f * (0.25f - tapeGain);
                } else {
                    tapeGain += 0.02f * (1.f - tapeGain);
                    tapeDropClock++;
                    if (tapeDropClock > 33600) {
                        tapeDropClock = 0;
                        noiseState = noiseState * 1103515245u + 12345u;
                        if (((noiseState >> 16) % 100u) < (unsigned)(fxP(29, 1) * 80.f))
                            tapeDropLen = 2400 + (int)((noiseState >> 20) % 4800u);
                    }
                }
                float sat = 1.f + fxP(29, 2);
                float yl = tanhf(dl * tapeGain * sat);
                float yr = tanhf(dr * tapeGain * sat);
                noiseState = noiseState * 1103515245u + 12345u;
                float nz = ((float)((noiseState >> 16) % 65536u) / 32768.f - 1.f) * (fxP(29, 3) * 0.012f);
                outL = outL * (1.f - w) + (yl + nz) * w;
                outR = outR * (1.f - w) + (yr + nz) * w;
            }
        }
        {
            float w = fxWet[30].load();
            if (w > 0.001f) {
                if (!prevWarp) {
                    warpRead = (float)((warpWrite - 12000 + kWarpMax) % kWarpMax);
                    warpRate = 1.f;
                    warpTarget = 1.f;
                }
                warpBuf[2 * warpWrite] = outL;
                warpBuf[2 * warpWrite + 1] = outR;
                warpWrite = (warpWrite + 1) % kWarpMax;
                float b = fmaxf(bpm.load(), 40.f);
                int div = 1 + (int)(fxP(30, 1) * 7.f);
                int per = (int)((60.f / b) * fs * 0.5f / (float)div);
                if (per < 64) per = 64;
                warpClock = (warpClock + 1) % per;
                if (warpClock == 0) {
                    noiseState = noiseState * 1103515245u + 12345u;
                    int r = (int)((noiseState >> 16) % 7u) - 3;
                    warpTarget = powf(2.f, (float)r / 12.f * (fxP(30, 0) * 6.f));
                }
                warpRate += (0.0005f + fxP(30, 2) * 0.0095f) * (warpTarget - warpRate);
                warpRead += warpRate;
                while (warpRead >= (float)kWarpMax) warpRead -= (float)kWarpMax;
                while (warpRead < 0.f) warpRead += (float)kWarpMax;
                int i0 = (int)warpRead;
                float fr = warpRead - (float)i0;
                int i1 = (i0 + 1) % kWarpMax;
                float dl = warpBuf[2 * i0] * (1.f - fr) + warpBuf[2 * i1] * fr;
                float dr = warpBuf[2 * i0 + 1] * (1.f - fr) + warpBuf[2 * i1 + 1] * fr;
                float gl = 0.3f + fxP(30, 3) * 0.7f;
                outL = outL * (1.f - w) + dl * w * gl;
                outR = outR * (1.f - w) + dr * w * gl;
            }
            prevWarp = w > 0.001f;
        }
        {
            float w = fxWet[31].load();
            if (w > 0.001f) {
                static const float vwF1[5] = {800.f, 400.f, 300.f, 500.f, 320.f};
                static const float vwF2[5] = {1200.f, 1900.f, 2700.f, 900.f, 800.f};
                float b = fmaxf(bpm.load(), 40.f);
                int per = (int)((60.f / b) * fs);
                if (per < 64) per = 64;
                int div = 1 + (int)(fxP(31, 0) * 7.f);
                int step2 = per / div;
                if (step2 < 64) step2 = 64;
                tkClock = (tkClock + 1) % (per * 4);
                if (tkClock % step2 == 0) tkVowel = (tkVowel + 1) % 5;
                tkF1 += 0.0015f * (vwF1[tkVowel] * (0.7f + fxP(31, 1) * 0.6f) - tkF1);
                tkF2 += 0.0015f * (vwF2[tkVowel] * (0.6f + fxP(31, 2) * 1.f) - tkF2);
                float k1 = 1.f - expf(-2.f * 3.14159265f * tkF1 * fsr);
                float k2 = 1.f - expf(-2.f * 3.14159265f * tkF2 * fsr);
                tkLp1L += k1 * (outL - tkLp1L);
                tkLp2L += k1 * (tkLp1L - tkLp2L);
                float b1l = tkLp1L - tkLp2L;
                tkLp3L += k2 * (outL - tkLp3L);
                tkLp4L += k2 * (tkLp3L - tkLp4L);
                float b2l = tkLp3L - tkLp4L;
                tkLp1R += k1 * (outR - tkLp1R);
                tkLp2R += k1 * (tkLp1R - tkLp2R);
                float b1r = tkLp1R - tkLp2R;
                tkLp3R += k2 * (outR - tkLp3R);
                tkLp4R += k2 * (tkLp3R - tkLp4R);
                float b2r = tkLp3R - tkLp4R;
                float rg = 1.5f + fxP(31, 3) * 4.f;
                float yl = tanhf((b1l * rg + b2l * rg * 0.7f) * 1.2f);
                float yr = tanhf((b1r * rg + b2r * rg * 0.7f) * 1.2f);
                outL = outL * (1.f - w) + yl * w;
                outR = outR * (1.f - w) + yr * w;
            }
        }
        {
            float w = fxWet[32].load();
            if (w > 0.001f) {
                float f0 = 55.f * powf(2.f, (float)rootNote.load() / 12.f) * 2.f * (0.5f + fxP(32, 0) * 1.5f);
                int len = (int)(fs / f0);
                if (len < 8) len = 8;
                if (len > kRinMax) len = kRinMax;
                if (len != rinLen) {
                    rinLen = len;
                    if (rinPos >= rinLen) rinPos = 0;
                }
                float s = 0.5f * (outL + outR);
                float a = fabsf(s);
                rinEnv += 0.002f * (a - rinEnv);
                if (rinDebounce > 0) rinDebounce--;
                if (a > rinEnv * 2.5f + 0.06f * (1.f - fxP(32, 2)) && rinDebounce == 0) {
                    rinDebounce = 2400;
                    for (int i = 0; i < rinLen; i++) {
                        noiseState = noiseState * 1103515245u + 12345u;
                        rinBuf[i] = s * (0.6f + 0.4f * ((float)((noiseState >> 16) % 2000u) / 1000.f - 1.f));
                    }
                    rinPos = 0;
                }
                float y = rinBuf[rinPos];
                rinPrev = (0.985f + fxP(32, 1) * 0.0145f) * 0.5f * (y + rinPrev);
                rinBuf[rinPos] = rinPrev;
                rinPos = (rinPos + 1) % rinLen;
                float gl = 0.4f + fxP(32, 3) * 0.8f;
                outL = outL * (1.f - w) + y * w * gl;
                outR = outR * (1.f - w) + y * w * gl;
            }
        }

        {
            float w = fxWet[33].load();
            if (w > 0.001f) {
                LoopBank& lb = banks[playerBank.load(std::memory_order_relaxed)];
                if (lb.buf && lb.samples > 4) {
                    if (fxAxisMask[33] & 1u) {
                        plyPhase = fxAxisVal[33][0] * (float)(lb.samples - 1);
                    } else {
                        float rate = powf(2.f, (fxP(33, 1) - 0.5f) * 4.f);
                        plyPhase += rate;
                        if (plyPhase >= (float)lb.samples) plyPhase -= (float)lb.samples;
                        if (plyPhase < 0.f) plyPhase += (float)lb.samples;
                    }
                    int ip = (int)plyPhase;
                    float fr = plyPhase - (float)ip;
                    int ip2 = (ip + 1) % lb.samples;
                    float sl = lb.buf[2 * ip] * (1.f - fr) + lb.buf[2 * ip2] * fr;
                    float sr = lb.buf[2 * ip + 1] * (1.f - fr) + lb.buf[2 * ip2 + 1] * fr;
                    float gl = 0.6f + fxP(33, 3) * 0.8f;
                    outL = outL * (1.f - w) + sl * w * gl;
                    outR = outR * (1.f - w) + sr * w * gl;
                }
            } else {
                plyPhase = 0.f;
            }
        }
        {
            float w = fxWet[34].load();
            if (w > 0.001f) {
                LoopBank& lb = banks[playerBank.load(std::memory_order_relaxed)];
                if (lb.buf && lb.samples > 4) {
                    float gsz = (0.02f + fxP(34, 0) * 0.38f) * fs;
                    if (gsz < 64.f) gsz = 64.f;
                    float rate = powf(2.f, (fxP(34, 1) - 0.5f) * 4.f);
                    granPos += rate * 0.25f;
                    if (granPos >= (float)lb.samples) granPos -= (float)lb.samples;
                    if (granPos < 0.f) granPos += (float)lb.samples;
                    granT += 1.f;
                    if (granT >= gsz) {
                        granT = 0.f;
                        granStart = granPos;
                        if (fxP(34, 2) > 0.001f) {
                            noiseState = noiseState * 1103515245u + 12345u;
                            float spr = fxP(34, 2) * (float)lb.samples * 0.5f;
                            granStart += ((float)(noiseState >> 16) / 65536.f - 0.5f) * spr;
                            if (granStart >= (float)lb.samples) granStart -= (float)lb.samples;
                            if (granStart < 0.f) granStart += (float)lb.samples;
                        }
                    }
                    float rp = granStart + granT * rate;
                    if (rp >= (float)lb.samples) rp -= (float)lb.samples;
                    if (rp < 0.f) rp += (float)lb.samples;
                    int ip = (int)rp;
                    float fr = rp - (float)ip;
                    int ip2 = (ip + 1) % lb.samples;
                    float env = sinf(3.14159265f * (granT / gsz));
                    float gl = 0.6f + fxP(34, 3) * 0.8f;
                    float sl = (lb.buf[2 * ip] * (1.f - fr) + lb.buf[2 * ip2] * fr) * env * gl;
                    float sr = (lb.buf[2 * ip + 1] * (1.f - fr) + lb.buf[2 * ip2 + 1] * fr) * env * gl;
                    outL = outL * (1.f - w) + sl * w;
                    outR = outR * (1.f - w) + sr * w;
                }
            } else {
                granT = 0.f;
            }
        }
        {
            float w = fxWet[35].load();
            if (w > 0.001f) {
                LoopBank& lb = banks[playerBank.load(std::memory_order_relaxed)];
                if (lb.buf && lb.samples > 4) {
                    float gsz = (0.005f + fxP(35, 0) * 0.055f) * fs;
                    if (gsz < 32.f) gsz = 32.f;
                    jglT += 1.f;
                    if (jglT >= gsz) {
                        jglT = 0.f;
                        noiseState = noiseState * 1103515245u + 12345u;
                        float r1 = (float)(noiseState >> 16) / 65536.f;
                        jglStart = (float)((noiseState >> 8) % (unsigned)lb.samples);
                        jglRev = r1 < fxP(35, 2) ? -1.f : 1.f;
                        jglRate = powf(2.f, (r1 - 0.5f) * 2.f * fxP(35, 1) * 2.f);
                    }
                    float rp = jglStart + jglT * jglRate * jglRev;
                    if (rp >= (float)lb.samples) rp -= (float)lb.samples;
                    if (rp < 0.f) rp += (float)lb.samples;
                    int ip = (int)rp;
                    float fr = rp - (float)ip;
                    int ip2 = (ip + 1) % lb.samples;
                    float env = sinf(3.14159265f * (jglT / gsz));
                    float gl = 0.6f + fxP(35, 3) * 0.8f;
                    float sl = (lb.buf[2 * ip] * (1.f - fr) + lb.buf[2 * ip2] * fr) * env * gl;
                    float sr = (lb.buf[2 * ip + 1] * (1.f - fr) + lb.buf[2 * ip2 + 1] * fr) * env * gl;
                    outL = outL * (1.f - w) + sl * w;
                    outR = outR * (1.f - w) + sr * w;
                }
            } else {
                jglT = 0.f;
            }
        }
        {
            float w = fxWet[36].load();
            if (w > 0.001f) {
                LoopBank& lb = banks[playerBank.load(std::memory_order_relaxed)];
                if (lb.buf && lb.samples > 4) {
                    float b = fmaxf(bpm.load(), 40.f);
                    float stepLen = (60.f / b) * 0.25f * fs;
                    int ns = 2 + (int)(fxP(36, 0) * 14.f);
                    if (ns > 16) ns = 16;
                    if (rndLen != lb.samples / ns || rndOff >= lb.samples) {
                        rndLen = lb.samples / ns;
                        rndOff = rndCur * rndLen;
                        rndRead = 0.f;
                    }
                    rndPhase += 1.f;
                    if (rndPhase >= stepLen) {
                        rndPhase -= stepLen;
                        noiseState = noiseState * 1103515245u + 12345u;
                        float r = (float)(noiseState >> 16) / 65536.f;
                        if (r < fxP(36, 1)) rndCur = (int)(r * 997.f) % ns;
                        else rndCur = (rndCur + 1) % ns;
                        rndOff = rndCur * rndLen;
                        rndRead = 0.f;
                    }
                    float rate = powf(2.f, (fxP(36, 2) - 0.5f) * 3.f);
                    rndRead += rate;
                    if (rndRead >= (float)rndLen) rndRead = 0.f;
                    int rp = rndOff + (int)rndRead;
                    if (rp >= lb.samples) rp = lb.samples - 1;
                    float gl = 0.6f + fxP(36, 3) * 0.8f;
                    float sl = lb.buf[2 * rp] * gl;
                    float sr = lb.buf[2 * rp + 1] * gl;
                    outL = outL * (1.f - w) + sl * w;
                    outR = outR * (1.f - w) + sr * w;
                }
            } else {
                rndPhase = 0.f; rndRead = 0.f;
            }
        }

        {
            float w = fxWet[37].load();
            if (w > 0.001f) {
                LoopBank& lb = banks[playerBank.load(std::memory_order_relaxed)];
                if (lb.buf && lb.samples > 4) {
                    float b = fmaxf(bpm.load(), 40.f);
                    float stepLen = (60.f / b) * 0.25f * fs;
                    int ns = 8 + (int)(fxP(37, 0) * 8.f);
                    if (ns > 16) ns = 16;
                    float sLen = (float)lb.samples / (float)ns;
                    slcPhase += 1.f;
                    if (slcPhase >= stepLen) {
                        slcPhase -= stepLen;
                        noiseState = noiseState * 1103515245u + 12345u;
                        float r = (float)(noiseState >> 16) / 65536.f;
                        int ord = (int)(fxP(37, 2) * 2.99f);
                        if (ord == 0) slcCur = (slcCur + 1) % ns;
                        else if (ord == 1) slcCur = (int)(r * 997.f) % ns;
                        else {
                            slcCur += (r < 0.5f) ? 1 : ns - 1;
                            if (slcCur >= ns) slcCur -= ns;
                        }
                    }
                    float rate = powf(2.f, (fxP(37, 1) - 0.5f) * 3.f);
                    float pos = (float)slcCur * sLen + slcPhase * rate;
                    if (pos >= (float)lb.samples) pos -= (float)lb.samples;
                    if (pos < 0.f) pos += (float)lb.samples;
                    int ip = (int)pos;
                    float fr = pos - (float)ip;
                    int ip2 = (ip + 1) % lb.samples;
                    float gl = stepLen * (0.1f + fxP(37, 0) * 0.9f);
                    float env = 0.f;
                    if (slcPhase < gl) {
                        env = 1.f;
                        float atk = fminf(gl, 96.f);
                        if (slcPhase < atk) env = slcPhase / atk;
                        float rel = gl - slcPhase;
                        if (rel < 96.f) env = fminf(env, rel / 96.f);
                    }
                    float lvl = 0.6f + fxP(37, 3) * 0.8f;
                    float sl = (lb.buf[2 * ip] * (1.f - fr) + lb.buf[2 * ip2] * fr) * env * lvl;
                    float sr = (lb.buf[2 * ip + 1] * (1.f - fr) + lb.buf[2 * ip2 + 1] * fr) * env * lvl;
                    outL = outL * (1.f - w) + sl * w;
                    outR = outR * (1.f - w) + sr * w;
                }
            } else {
                slcPhase = 0.f;
            }
        }
        {
            float w = fxWet[38].load();
            if (w > 0.001f) {
                LoopBank& lb = banks[playerBank.load(std::memory_order_relaxed)];
                if (lb.buf && lb.samples > 4) {
                    float gsz = (0.03f + fxP(38, 1) * 0.3f) * fs;
                    if (gsz < 128.f) gsz = 128.f;
                    float disp = fxP(38, 2) * (float)lb.samples * 0.5f;
                    cldHead += 0.25f;
                    if (cldHead >= (float)lb.samples) cldHead -= (float)lb.samples;
                    float sl = 0.f, sr = 0.f;
                    for (int gi = 0; gi < 8; gi++) {
                        cldT[gi] += 1.f;
                        if (cldT[gi] >= cldSz[gi]) {
                            noiseState = noiseState * 1103515245u + 12345u;
                            float r = (float)(noiseState >> 16) / 65536.f;
                            cldSz[gi] = gsz * (0.5f + r);
                            cldT[gi] = 0.f;
                            cldPos[gi] = cldHead + ((float)(noiseState >> 8) / 65536.f - 0.5f) * disp;
                            if (cldPos[gi] >= (float)lb.samples) cldPos[gi] -= (float)lb.samples;
                            if (cldPos[gi] < 0.f) cldPos[gi] += (float)lb.samples;
                            cldRate[gi] = 1.f + ((float)(noiseState >> 12) / 65536.f - 0.5f) * 0.12f;
                        }
                        float rp = cldPos[gi] + cldT[gi] * cldRate[gi];
                        if (rp >= (float)lb.samples) rp -= (float)lb.samples;
                        if (rp < 0.f) rp += (float)lb.samples;
                        int ip = (int)rp;
                        float fr = rp - (float)ip;
                        int ip2 = (ip + 1) % lb.samples;
                        float env = sinf(3.14159265f * (cldT[gi] / cldSz[gi]));
                        sl += (lb.buf[2 * ip] * (1.f - fr) + lb.buf[2 * ip2] * fr) * env;
                        sr += (lb.buf[2 * ip + 1] * (1.f - fr) + lb.buf[2 * ip2 + 1] * fr) * env;
                    }
                    float lvl = (0.5f + fxP(38, 3) * 0.9f) / 4.f;
                    outL = outL * (1.f - w) + sl * w * lvl;
                    outR = outR * (1.f - w) + sr * w * lvl;
                }
            } else {
                for (int gi = 0; gi < 8; gi++) cldT[gi] = 0.f;
            }
        }
        {
            float w = fxWet[39].load();
            if (w > 0.001f) {
                LoopBank& lb = banks[playerBank.load(std::memory_order_relaxed)];
                if (lb.buf && lb.samples > 4) {
                    float wsz = (0.05f + fxP(39, 1) * 0.6f) * fs;
                    if (wsz < 256.f) wsz = 256.f;
                    float fpos = fxP(39, 0) * (float)lb.samples;
                    float scan = powf(2.f, (fxP(39, 2) - 0.5f) * 2.f);
                    frzT += scan;
                    if (frzT >= wsz) frzT -= wsz;
                    if (frzT < 0.f) frzT += wsz;
                    float rp = fpos + frzT;
                    if (rp >= (float)lb.samples) rp -= (float)lb.samples;
                    if (rp < 0.f) rp += (float)lb.samples;
                    int ip = (int)rp;
                    float fr = rp - (float)ip;
                    int ip2 = (ip + 1) % lb.samples;
                    float env = sinf(3.14159265f * (frzT / wsz));
                    float lvl = (0.6f + fxP(39, 3) * 0.8f) * 1.4f;
                    float sl = (lb.buf[2 * ip] * (1.f - fr) + lb.buf[2 * ip2] * fr) * env * lvl;
                    float sr = (lb.buf[2 * ip + 1] * (1.f - fr) + lb.buf[2 * ip2 + 1] * fr) * env * lvl;
                    outL = outL * (1.f - w) + sl * w;
                    outR = outR * (1.f - w) + sr * w;
                }
            } else {
                frzT = 0.f;
            }
        }


        {
            float w = fxWet[40].load();
            if (w > 0.001f) {
                if (!eq3Ok || eq3Fs != 1.f / fsr || (eq3Clk++ & 31) == 0) {
                    eq3SetCoefs(1.f / fsr, fxP(40, 0), fxP(40, 1), fxP(40, 2));
                    eq3Ok = true;
                }
                float lv = 0.5f + fxP(40, 3) * 1.3f;
                float sl = eqProc(eq3L, outL) * lv;
                float sr = eqProc(eq3R, outR) * lv;
                outL = outL * (1.f - w) + sl * w;
                outR = outR * (1.f - w) + sr * w;
            }
        }
        {
            float w = fxWet[41].load();
            if (w > 0.001f) {
                float th = 0.05f + fxP(41, 0) * 0.6f;
                float rt = 1.f + fxP(41, 1) * 9.f;
                float rel = expf(-1000.f * fsr / (50.f + fxP(41, 2) * 450.f));
                float mk = 1.f + fxP(41, 3) * 1.5f;
                float axm = fmaxf(fabsf(outL), fabsf(outR));
                cmpEnv = (axm > cmpEnv) ? cmpEnv + (axm - cmpEnv) * 0.004f
                                        : cmpEnv + (axm - cmpEnv) * rel;
                float gr = 1.f;
                if (cmpEnv > th) gr = (th + (cmpEnv - th) / rt) / cmpEnv;
                cmpGr += (gr - cmpGr) * 0.006f;
                float sl = outL * cmpGr * mk;
                float sr = outR * cmpGr * mk;
                outL = outL * (1.f - w) + sl * w;
                outR = outR * (1.f - w) + sr * w;
            }
        }
        {
            float w = fxWet[42].load();
            if (w > 0.001f) {
                float dr = 1.f + fxP(42, 0) * 5.f;
                float tn = 0.08f + fxP(42, 1) * 0.5f;
                float lv = 0.5f + fxP(42, 2) * 1.f;
                float nl = tanhf(dr), nr = tanhf(dr);
                float sl = tanhf(outL * dr) / nl;
                float sr = tanhf(outR * dr) / nr;
                warmLpL += tn * (sl - warmLpL);
                warmLpR += tn * (sr - warmLpR);
                outL = outL * (1.f - w) + warmLpL * lv * w;
                outR = outR * (1.f - w) + warmLpR * lv * w;
            }
        }
        {
            float w = fxWet[43].load();
            if (w > 0.001f) {
                float hp = expf(-6.2831853f * (20.f + fxP(43, 0) * 380.f) * fsr);
                float lp = 1.f - expf(-6.2831853f * (4000.f + fxP(43, 1) * 12000.f) * fsr);
                float gt = fxP(43, 2) * 0.12f;
                float lv = 0.5f + fxP(43, 3) * 1.f;
                float hl = hp * (clnHpYL + outL - clnHpXL);
                float hr = hp * (clnHpYR + outR - clnHpXR);
                clnHpXL = outL; clnHpYL = hl;
                clnHpXR = outR; clnHpYR = hr;
                clnLpL += lp * (hl - clnLpL);
                clnLpR += lp * (hr - clnLpR);
                float axm = fmaxf(fabsf(clnLpL), fabsf(clnLpR));
                clnEnv = (axm > clnEnv) ? clnEnv + (axm - clnEnv) * 0.01f
                                        : clnEnv + (axm - clnEnv) * 0.0005f;
                float g = clnEnv > gt ? 1.f : 0.f;
                clnGr += (g - clnGr) * 0.002f;
                outL = outL * (1.f - w) + clnLpL * clnGr * lv * w;
                outR = outR * (1.f - w) + clnLpR * clnGr * lv * w;
            }
        }
        {
            float w = fxWet[44].load();
            if (w > 0.001f) {
                float fz = 1.f + fxP(44, 0) * 24.f;
                float tn = 0.1f + fxP(44, 1) * 0.55f;
                float lv = 0.3f + fxP(44, 2) * 0.9f;
                float axm = fmaxf(fabsf(outL), fabsf(outR));
                fzEnv = (axm > fzEnv) ? fzEnv + (axm - fzEnv) * 0.05f
                                      : fzEnv + (axm - fzEnv) * 0.0006f;
                float gl = fz / (1.f + fzEnv * 1.5f);
                float sl = (outL > 0.f) ? tanhf(outL * gl * 1.6f) : tanhf(outL * gl * 0.55f) * 1.8f;
                float sr = (outR > 0.f) ? tanhf(outR * gl * 1.6f) : tanhf(outR * gl * 0.55f) * 1.8f;
                fzLpL += tn * (sl - fzLpL);
                fzLpR += tn * (sr - fzLpR);
                outL = outL * (1.f - w) + fzLpL * lv * w;
                outR = outR * (1.f - w) + fzLpR * lv * w;
            }
        }
        {
            float w = fxWet[45].load();
            if (w > 0.001f) {
                float gn = 1.f + fxP(45, 0) * 20.f;
                float tn = 0.12f + fxP(45, 1) * 0.55f;
                float lv = 0.3f + fxP(45, 2) * 0.9f;
                float dl = outL * gn, dr = outR * gn;
                dl = dl / (1.f + fabsf(dl)) * 1.6f;
                dr = dr / (1.f + fabsf(dr)) * 1.6f;
                dsLpL += tn * (dl - dsLpL);
                dsLpR += tn * (dr - dsLpR);
                outL = outL * (1.f - w) + dsLpL * lv * w;
                outR = outR * (1.f - w) + dsLpR * lv * w;
            }
        }

        {
            float w = fxWet[46].load();
            float b = fmaxf(bpm.load(), 40.f);
            static const float tDivs[8] = {
                0.25f, 0.33333f, 0.375f, 0.5f, 0.75f, 1.f, 1.5f, 2.f
            };
            int di = (int)(fxP(75, 0) * 7.99f);
            int dly = (int)((60.f / b) * tDivs[di] * fs);
            if (dly > kDelayMax - 2) dly = kDelayMax - 2;
            if (dly < 2) dly = 2;
            int rp = (tempoPos - dly + kDelayMax) % kDelayMax;
            float dl = tempoBuf[2 * rp], dr = tempoBuf[2 * rp + 1];
            float fb0 = 0.2f + 0.7f * fxP(75, 1);
            float tn0 = 0.05f + 0.4f * fxP(75, 2);
            tempoLpL += tn0 * (dl * fb0 - tempoLpL);
            tempoLpR += tn0 * (dr * fb0 - tempoLpR);
            tempoBuf[2 * tempoPos] = outL + tempoLpL;
            tempoBuf[2 * tempoPos + 1] = outR + tempoLpR;
            tempoPos = (tempoPos + 1) % kDelayMax;
            if (w > 0.001f) {
                float lv = w * (0.4f + 1.2f * fxP(75, 3));
                outL = outL * (1.f - w * 0.5f) + dl * lv;
                outR = outR * (1.f - w * 0.5f) + dr * lv;
            }
        }
        {
            float w = fxWet[47].load();
            int dly = (int)((0.02f + 0.73f * fxP(76, 0)) * fs);
            if (dly > kDelayMax - 2) dly = kDelayMax - 2;
            if (dly < 2) dly = 2;
            int rp = (pingPos - dly + kDelayMax) % kDelayMax;
            float dl = pingBuf[2 * rp], dr = pingBuf[2 * rp + 1];
            float fb1 = 0.2f + 0.7f * fxP(76, 1);
            float tn1 = 0.05f + 0.35f * fxP(76, 2);
            pingLpL += tn1 * (dr * fb1 - pingLpL);
            pingLpR += tn1 * (dl * fb1 - pingLpR);
            pingBuf[2 * pingPos] = outL + pingLpL;
            pingBuf[2 * pingPos + 1] = outL + pingLpR;
            pingPos = (pingPos + 1) % kDelayMax;
            if (w > 0.001f) {
                float lv = w * (0.4f + 1.2f * fxP(76, 3));
                outL = outL * (1.f - w * 0.5f) + dl * lv;
                outR = outR * (1.f - w * 0.5f) + dr * lv;
            }
        }
        {
            float w = fxWet[48].load();
            if (w > 0.001f) {
                float inL = outL, inR = outR;
                plateL.damp = 0.1f + 0.6f * fxP(77, 1);
                plateR.damp = plateL.damp;
                float wl = plateL.process(inL);
                float wr = plateR.process(inR);
                float sz = 0.4f + 0.8f * fxP(77, 0);
                float lv = w * (0.4f + 1.2f * fxP(77, 3)) * sz;
                outL = outL * (1.f - w * 0.5f) + wl * lv;
                outR = outR * (1.f - w * 0.5f) + wr * lv;
            }
        }
        {
            float w = fxWet[49].load();
            if (w > 0.001f) {
                float wl = shimL.process(outL);
                float wr = shimR.process(outR);
                shimWetL += (wl - shimWetL) * 0.02f;
                shimWetR += (wr - shimWetR) * 0.02f;
                float oc = fxP(78, 1);
                float octL = fabsf(shimWetL) * 1.2f;
                float octR = fabsf(shimWetR) * 1.2f;
                float wetL = shimWetL * (1.f - oc) + octL * oc;
                float wetR = shimWetR * (1.f - oc) + octR * oc;
                float sz = 0.4f + 0.9f * fxP(78, 0);
                float lv = w * (0.4f + 1.2f * fxP(78, 3)) * sz;
                outL = outL * (1.f - w * 0.5f) + wetL * lv;
                outR = outR * (1.f - w * 0.5f) + wetR * lv;
            }
        }
        {
            float w = fxWet[50].load();
            if (w > 0.001f) {
                float th = 0.02f + 0.3f * fxP(79, 1);
                float holdS = (0.01f + 0.29f * fxP(79, 0)) * fs;
                float peakL = fabsf(outL), peakR = fabsf(outR);
                if (peakL > th) { gateHeldL = (int)holdS; gateEnvL += (1.f - gateEnvL) * 0.5f; }
                else {
                    if (gateHeldL > 0) gateHeldL--; else gateEnvL += (0.f - gateEnvL) * 0.02f;
                }
                if (peakR > th) { gateHeldR = (int)holdS; gateEnvR += (1.f - gateEnvR) * 0.5f; }
                else {
                    if (gateHeldR > 0) gateHeldR--; else gateEnvR += (0.f - gateEnvR) * 0.02f;
                }
                float wl = gateL.process(outL) * gateEnvL;
                float wr = gateR.process(outR) * gateEnvR;
                float lv = w * (0.5f + 1.1f * fxP(79, 3));
                outL = outL * (1.f - w * 0.35f) + wl * lv;
                outR = outR * (1.f - w * 0.35f) + wr * lv;
            }
        }

        if (drumPos >= 0.f) {
            LoopBank& lb = banks[drumBank.load(std::memory_order_relaxed)];
            if (lb.buf && lb.samples > 4 && drumPos < drumEnd) {
                int ip = (int)drumPos;
                float fr = drumPos - (float)ip;
                int ip2 = (ip + 1) % lb.samples;
                float sl = lb.buf[2 * ip] * (1.f - fr) + lb.buf[2 * ip2] * fr;
                float sr = lb.buf[2 * ip + 1] * (1.f - fr) + lb.buf[2 * ip2 + 1] * fr;
                drumEnv *= 0.9995f;
                outL += sl * drumEnv * 0.8f;
                outR += sr * drumEnv * 0.8f;
                drumPos += drumRate;
            } else {
                drumPos = -1.f;
            }
        }

        outL += dryL * kLoopGain;
        outR += dryR * kLoopGain;

        {
            float ob = outBoost.load(std::memory_order_relaxed);
            if (ob != 1.f) { outL *= ob; outR *= ob; }
        }

        if (masterEq.load(std::memory_order_relaxed)) {
            if (!eqCoefOk || eqFs != 1.f / fsr) eqSetCoefs(1.f / fsr);
            outL = eqProc(eqXL, outL);
            outR = eqProc(eqXR, outR);
        }

        if (outLoud.load(std::memory_order_relaxed)) {
            float axm = fmaxf(fabsf(outL), fabsf(outR));
            loudEnv = (axm > loudEnv) ? loudEnv + (axm - loudEnv) * 0.02f
                                      : loudEnv + (axm - loudEnv) * 0.0008f;
            float gr = 1.f;
            if (loudEnv > 0.25f) gr = (0.25f + (loudEnv - 0.25f) / 3.f) / loudEnv;
            loudGr += (gr - loudGr) * 0.01f;
            outL *= loudGr * 1.6f;
            outR *= loudGr * 1.6f;
        }

        if (outL > 0.95f) outL = 0.95f + (outL - 0.95f) / (1.f + (outL - 0.95f) * (outL - 0.95f) * 40.f);
        else if (outL < -0.95f) outL = -0.95f + (outL + 0.95f) / (1.f + (outL + 0.95f) * (outL + 0.95f) * 40.f);
        if (outR > 0.95f) outR = 0.95f + (outR - 0.95f) / (1.f + (outR - 0.95f) * (outR - 0.95f) * 40.f);
        else if (outR < -0.95f) outR = -0.95f + (outR + 0.95f) / (1.f + (outR + 0.95f) * (outR + 0.95f) * 40.f);

        if (resBank >= 0) {
            LoopBank& rb = banks[resBank];
            if (rb.buf) {
                int D = rb.decim > 0 ? rb.decim : 1;
                resAccumL += outL;
                resAccumR += outR;
                resDiv++;
                if (resDiv >= D) {
                    rb.buf[2 * resPos] = resAccumL / (float)D;
                    rb.buf[2 * resPos + 1] = resAccumR / (float)D;
                    resAccumL = 0.f;
                    resAccumR = 0.f;
                    resDiv = 0;
                    resPos++;
                    if (resPos >= rb.samples) {
                        rb.playing = true;
                        rb.pos = 0;
                        resBank = -1;
                    }
                }
            }
        }

        float mv = masterVol.load(std::memory_order_relaxed);
        outL *= mv;
        outR *= mv;

        float pL = fabsf(outL), pR = fabsf(outR);
        float cl = levelL.load(), cr = levelR.load();
        levelL.store(cl + (pL > cl ? 0.5f : 0.02f) * (pL - cl));
        levelR.store(cr + (pR > cr ? 0.5f : 0.02f) * (pR - cr));

        o[2 * i] = outL;
        o[2 * i + 1] = outR;
    }

    return oboe::DataCallbackResult::Continue;
}
