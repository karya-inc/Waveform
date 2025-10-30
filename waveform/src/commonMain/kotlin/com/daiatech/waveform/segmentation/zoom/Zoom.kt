package com.daiatech.waveform.segmentation.zoom

// FIXME: Disabled other zoom levels as calculating amplitudes at that level
//        takes too much computation time
enum class Zoom(val value: Int) {
    X1(1),
    X2(2);
//    X3(3),
//    X4(4),
//    X5(5);

    fun increment(): Zoom {
        return when (this) {
            X1 -> X2
            X2 -> X2
//           X3 -> X4
//           X4 -> X5
//           X5 -> X5
        }
    }

    fun decrement(): Zoom {
        return when (this) {
            X1 -> X1
            X2 -> X1
//            X3 -> X2
//            X4 -> X3
//            X5 -> X4
        }
    }

    companion object {
        val max get() = X2
        val min get() = X1
    }
}