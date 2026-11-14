#ifndef __cortexm4_h_portable
#define __cortexm4_h_portable

#include <stdint.h>

#define __SIMD32_TYPE int32_t
#define simd32_t __SIMD32_TYPE

static inline int32_t qadd(int32_t a, int32_t b) {
    int64_t r = (int64_t)a + (int64_t)b;
    if (r > 0x7FFFFFFFLL) r = 0x7FFFFFFFLL;
    if (r < -0x80000000LL) r = -0x80000000LL;
    return (int32_t)r;
}

static inline int32_t qsub(int32_t a, int32_t b) {
    int64_t r = (int64_t)a - (int64_t)b;
    if (r > 0x7FFFFFFFLL) r = 0x7FFFFFFFLL;
    if (r < -0x80000000LL) r = -0x80000000LL;
    return (int32_t)r;
}

static inline int32_t qadd16(int32_t a, int32_t b) {
    int32_t lo = qadd((int16_t)(a & 0xFFFF), (int16_t)(b & 0xFFFF)) & 0xFFFF;
    int32_t hi = qadd((int16_t)(a >> 16), (int16_t)(b >> 16)) << 16;
    return hi | lo;
}

static inline int32_t qsub16(int32_t a, int32_t b) {
    int32_t lo = qadd((int16_t)(a & 0xFFFF), (int16_t)(-(int16_t)(b & 0xFFFF))) & 0xFFFF;
    int32_t hi = qadd((int16_t)(a >> 16), (int16_t)(-(int16_t)(b >> 16))) << 16;
    return hi | lo;
}

static inline int32_t sel(int32_t a, int32_t b) {
    return a;
}

#endif
