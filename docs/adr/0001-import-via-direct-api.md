# 教务导入采用直连 API（非内嵌 WebView）

教务导入在应用内直连教务系统 API：按会话从登录页提取 scode/sxh 常量，对账号密码做 Base64 + 置乱生成 `encoded`，POST 登录后用会话 Cookie 拉取课程数据。不引入 WebView。

课程数据来自「有课表课程」列表 JSON 接口（`xskb_list.do?viweType=1&needData=1`，需 layui 风格请求头与 `pageNum/pageSize` 分页参数），**校区参数（kbjcmsid）必填**，空值或他人校区返回空。该部署的周视图网格（viweType=0）不承载课程数据，仅用于读取学期/校区下拉选项。

## Considered Options

- **内嵌 WebView 抓取**（行业标准做法，如 WakeUp 课程表）：把各校登录差异外包给 WebView 内的用户操作。放弃，因为大连民族大学金智系统**无验证码、无滑块、无 CAS 跳转**，登录加密为纯前端确定性算法（2026-08-26 已用真实账号验证复刻成功，返回 302 跳转主页）。直连体验更好（应用内直接填学号密码）且零 WebView 依赖。
- **手动粘贴 HTML 解析**：体验差，放弃。

## Consequences

- 登录实现依赖金智登录页内嵌的 scode/sxh 置乱算法与 `LoginToXkLdap` 端点；学校升级教务系统时可能失效，需对照登录页重新校准。
- 教务账号密码经 Crypto 加密存于本地（见 Account 模型），不经任何第三方中转。
