#ifndef __stub_arm_math_h
#define __stub_arm_math_h

#include <stdint.h>

#define __SIMD32_TYPE int32_t

static inline int32_t __QADD(int32_t a, int32_t b) {
    int64_t r = (int64_t)a + (int64_t)b;
    if (r > 0x7FFFFFFFLL) r = 0x7FFFFFFFLL;
    if (r < -0x80000000LL) r = -0x80000000LL;
    return (int32_t)r;
}

static inline int32_t __QADD16(int32_t a, int32_t b) {
    int32_t lo = __QADD((int16_t)(a & 0xFFFF), (int16_t)(b & 0xFFFF)) & 0xFFFF;
    int32_t hi = __QADD((int16_t)(a >> 16), (int16_t)(b >> 16)) << 16;
    return hi | lo;
}

static inline int32_t __QSUB16(int32_t a, int32_t b) {
    int32_t lo = __QADD((int16_t)(a & 0xFFFF), (int16_t)(-(int16_t)(b & 0xFFFF))) & 0xFFFF;
    int32_t hi = __QADD((int16_t)(a >> 16), (int16_t)(-(int16_t)(b >> 16))) << 16;
    return hi | lo;
}

#endif
