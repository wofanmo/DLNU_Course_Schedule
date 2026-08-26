package com.wofanmo.course_schedule.data.crypto

expect object Crypto {
    fun encrypt(plainText: String): String
    fun decrypt(encryptedText: String): String
}
