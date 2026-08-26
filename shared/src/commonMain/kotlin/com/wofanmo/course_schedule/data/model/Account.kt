package com.wofanmo.course_schedule.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val platform: String,        // 平台名（正方、学习通等）
    val username: String,
    val password: String,
    val url: String = "",        // 教务系统地址
)
