package com.daiatech.waveform.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform