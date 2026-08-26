package com.wofanmo.course_schedule

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform