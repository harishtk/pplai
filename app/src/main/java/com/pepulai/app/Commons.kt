package com.pepulai.app

import java.util.Random

fun getRandomHexCode(): String {
    val random = Random()
    val int = random.nextInt(0xffffff + 1)
    return String.format("#%06x", int)
}