package com.zulu

class AudioEngine {
    external fun nativeStart(mic: Boolean): Boolean
    external fun nativeStop()
    external fun nativeSetFilterType(t: Int)
    external fun nativeSetAx(v: Float)
    external fun nativeSetAy(v: Float)
    external fun nativeSetBx(v: Float)
    external fun nativeSetBy(v: Float)
    external fun nativeSetMix(v: Float)
    external fun nativeSetMute(m: Boolean)
    external fun nativeSetFly(v: Float)
    external fun nativeSetStutter(s: Boolean)
    external fun nativeSetBpm(b: Float)
    external fun nativeRecordBank(bank: Int, steps: Int, wet: Boolean)
    external fun nativeToggleBank(bank: Int)
    external fun nativeBankHasLoop(bank: Int): Boolean
    external fun nativeJumpSlice(slice: Int, total: Int)
    external fun nativeIsLoopRecording(): Boolean
    external fun nativeGetLevels(out: FloatArray)

    companion object {
        init { System.loadLibrary("zulu") }
    }
}
