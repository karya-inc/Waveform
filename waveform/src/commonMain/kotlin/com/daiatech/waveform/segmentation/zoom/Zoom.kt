package com.daiatech.waveform.segmentation.zoom

/**
 * Waveform zoom levels
 *
 * @property value zoom multiplier
 */
// FIXME: Disabled other zoom levels as calculating amplitudes at that level
//        takes too much computation time
enum class Zoom(val value: Int) {
    /** 1x zoom level */
    X1(1),

    /** 2x zoom level */
    X2(2),
    X3(3);
//    X4(4),
//    X5(5);

    /**
     * Returns next higher zoom level
     *
     * @return increased zoom level, or same if already at maximum
     */
    fun increment(): Zoom {
        return when (this) {
            X1 -> X2
            X2 -> X3
            X3 -> X3
//           X4 -> X5
//           X5 -> X5
        }
    }

    /**
     * Returns next lower zoom level
     *
     * @return decreased zoom level, or same if already at minimum
     */
    fun decrement(): Zoom {
        return when (this) {
            X1 -> X1
            X2 -> X1
            X3 -> X2
//            X4 -> X3
//            X5 -> X4
        }
    }

    companion object {
        /** Maximum zoom level */
        val max get() = X3

        /** Minimum zoom level */
        val min get() = X1
    }
}