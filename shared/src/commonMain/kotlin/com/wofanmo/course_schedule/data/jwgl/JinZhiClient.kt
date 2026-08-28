package com.wofanmo.course_schedule.data.jwgl

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class JinZhiException(message: String) : Exception(message)

/**
 * 金智教务系统（jsxsd）直连客户端。
 * 登录为纯前端确定性算法，已按大连民族大学部署验证；
 * 算法与路线选型见 docs/adr/0001-import-via-direct-api.md。
 */
class JinZhiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    private val client = HttpClient {
        install(HttpCookies) { storage = AcceptAllCookiesStorage() }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
        }
        followRedirects = true
    }

    /**
     * 登录教务系统。成功返回 [Result.success]；
     * 失败时异常消息为教务系统返回的原文提示（如「用户名或密码错误」）。
     */
    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        // scode/sxh 常量按会话渲染进登录页，必须先取页面再加密
        val loginPage = client.get("$baseUrl/").bodyAsText()
        val scode = Regex("""var scode = "([^"]+)"""").find(loginPage)?.groupValues?.get(1)
            ?: throw JinZhiException("无法获取登录参数，教务系统页面可能已变更")
        val sxh = Regex("""var sxh = "([^"]+)"""").find(loginPage)?.groupValues?.get(1)
            ?: throw JinZhiException("无法获取登录参数，教务系统页面可能已变更")

        val encoded = encodeLogin(username, password, scode, sxh)
        val body = client.submitForm(
            url = "$baseUrl/xk/LoginToXkLdap",
            formParameters = Parameters.build {
                append("loginMethod", "LoginToXkLdap")
                append("userlanguage", "0")
                append("userAccount", username)
                append("userPassword", password)
                append("encoded", encoded)
            }
        ).bodyAsText()

        // 登录失败时服务端原样回显登录页，并在 showMsg 中给出原因
        val error = Regex("""id="showMsg"[^>]*>([^<]*)<""").find(body)
            ?.groupValues?.get(1)?.trim()
        if (!error.isNullOrEmpty()) throw JinZhiException(error)
        if (body.contains("""id="userAccount"""")) {
            throw JinZhiException("登录失败，请检查账号和密码")
        }
    }

    /**
     * 拉取「学期理论课表」页面 HTML。
     * 用途：读取学期（xnxq01id）与校区/时间模式（kbjcmsid）下拉选项。
     * 注意：该页的周视图网格在此部署中不承载课程数据，课程数据走 [fetchCourseListJson]。
     */
    suspend fun fetchSchedulePage(semesterId: String? = null, campusId: String? = null): String =
        client.get("$baseUrl/xskb/xskb_list.do") {
            url {
                parameters.append("viweType", "0")
                semesterId?.let { parameters.append("xnxq01id", it) }
                campusId?.let { parameters.append("kbjcmsid", it) }
            }
        }.bodyAsText()

    /**
     * 课程列表 JSON 接口（layui table 数据源）。
     * [viweType]：1=有课表课程（含时间地点），2=无课表课程。
     * [campusId] 必填：为空或不匹配的校区会返回空数据。
     * 必须带 X-Requested-With 与 Accept: application/json 头，否则返回 HTML 页面。
     */
    suspend fun fetchCourseListJson(
        viweType: Int,
        semesterId: String,
        campusId: String,
    ): String = client.get("$baseUrl/xskb/xskb_list.do") {
        url {
            parameters.append("viweType", viweType.toString())
            parameters.append("needData", "1")
            parameters.append("demoStr", "")
            parameters.append("baseUrl", "/jsxsd")
            parameters.append("sfykb", "2")
            parameters.append(
                "xsflMapListJsonStr",
                "讲课学时,实践学时,课外学时,实验学时,上机学时,讨论辅导学时,设计作业总学时,"
            )
            parameters.append("xnxq01id", semesterId)
            parameters.append("zc", "")
            parameters.append("kbjcmsid", campusId)
            parameters.append("pageNum", "1")
            parameters.append("pageSize", "500")
        }
        headers.append("X-Requested-With", "XMLHttpRequest")
        headers.append("Accept", "application/json, text/javascript, */*; q=0.01")
    }.bodyAsText()

    /** 学期的起止周（用于确定课表总周数）；失败返回 null */
    suspend fun fetchSemesterTotalWeeks(semesterId: String): Int? = runCatching {
        val text = client.get("$baseUrl/xskb/jxzlzc_xnxq_ajax") {
            url { parameters.append("xnxq01id", semesterId) }
        }.bodyAsText()
        Regex(""""jszc"\s*:\s*(\d+)""").find(text)?.groupValues?.get(1)?.toInt()
    }.getOrNull()

    fun close() = client.close()

    companion object {
        const val DEFAULT_BASE_URL = "http://jwxt.dlnu.edu.cn/jsxsd"

        @OptIn(ExperimentalEncodingApi::class)
        private fun b64(text: String): String = Base64.encode(text.encodeToByteArray())

        /**
         * 登录加密：base64(账号)%%%base64(密码)%%%base64(" ") 之后，
         * 前 55 个字符逐个与 sxh 指定长度的 scode 前缀交错拼接。
         */
        internal fun encodeLogin(account: String, password: String, scode: String, sxh: String): String {
            val code = b64(account) + "%%%" + b64(password) + "%%%" + b64(" ")
            val sb = StringBuilder()
            var sc = scode
            var i = 0
            while (i < code.length) {
                if (i < 55) {
                    val n = sxh[i].digitToInt()
                    sb.append(code[i]).append(sc.take(n))
                    sc = sc.drop(n)
                    i++
                } else {
                    sb.append(code.substring(i))
                    break
                }
            }
            return sb.toString()
        }
    }
}
