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
     * 用途：读取学期（xnxq01id）下拉选项。
     */
    suspend fun fetchSchedulePage(semesterId: String? = null): String =
        client.get("$baseUrl/xskb/xskb_list.do") {
            url {
                parameters.append("viweType", "0")
                semesterId?.let { parameters.append("xnxq01id", it) }
            }
        }.bodyAsText()

    /**
     * 拉取课表周视图页面 HTML（课程数据的唯一来源）。
     * 服务端按 [week] 过滤网格，需遍历全部周次才能拿到完整课程；
     * 校区由服务端按登录身份决定，故 kbjcmsid 传空。
     */
    suspend fun fetchScheduleGrid(semesterId: String, week: Int): String =
        client.get("$baseUrl/$SCHEDULE_GRID_PATH") {
            url {
                parameters.append("zc", week.toString())
                parameters.append("kbjcmsid", "")
                parameters.append("xnxq01id", semesterId)
                parameters.append("xswk", "false")
            }
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

        /**
         * 课表周视图页面路径。
         *
         * 教务系统改版后课程数据只从这里返回：旧 JSON 接口
         * `xskb/xskb_list.do?viweType=1` 已改为返回空数据。
         * 路径末尾的 `12026` 是教务系统的菜单 ID，若学校调整菜单需同步更新。
         */
        private const val SCHEDULE_GRID_PATH = "framework/mainV_index_loadkb_12026.htmlx"

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
