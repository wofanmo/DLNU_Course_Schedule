# 课程表 Course Schedule

大连民族大学学生的课程表应用（Android）：支持从教务系统一键导入课表、手动添加课程、今日课程速览与桌面小部件。

## 功能特性

### 📅 课表视图
- 左右滑动 / 周次选择器切换周次，切换时带方向感知的滑动动画（左侧时间栏保持不动）
- 左侧固定节次时间栏，午休 / 晚休分隔线
- 点击课程卡片动画展开详情（教师、地点、节次）
- 同一天同一时段多门课自动水平平分显示
- 添加课程时自动检测时间冲突并禁止重复添加

### 🏠 今日课程
- 首页按上课时间展示当天课程列表
- 课程卡片带色条、节次徽章与上课地点

### 📥 教务导入
- 直连金智教育教务系统 API（jwxt.dlnu.edu.cn/jsxsd），应用内输入学号密码即可导入，无 WebView
- 教务密码经 AES/GCM 加密存储在本地（AndroidKeyStore），不经任何第三方中转
- 每次导入生成独立的课表快照，不与现有课表合并

### ✏️ 手动添加课程
- 周次选择支持逐周点选、滑动连选，以及「每周 / 仅单周 / 仅双周 / 清空」快捷操作
- 自定义课程颜色

### 🗂 多课表管理
- 持有多份课表（如多学期导入快照），随时切换
- 每份课表可单独设置开学日期（日期选择器）
- 支持删除课表，删除当前课表自动切换到剩余课表

### ⚙️ 个性化
- 主题：跟随系统 / 浅色 / 深色
- 显示 / 隐藏周末列
- 自适应启动器图标（含 Android 13+ 主题化图标）

### 🧩 桌面小部件
- 2×4 规格，显示今日剩余课程（含上课时间、节次、地点）
- 课程已结束自动从列表移除；应用内改动后回到桌面即时刷新

## 课表时间

内置大连民族大学作息（一天 12 节，每节 40 分钟）：

| 时段 | 节次 | 上课时间 |
|---|---|---|
| 上午 | 1–4 | 08:30 开始 |
| 下午 | 5–8 | 13:30 开始 |
| 晚上 | 9–12 | 18:30 开始 |

同一大节内课间 10 分钟（1-2、3-4、5-6、7-8、9-10、11-12），跨大节课间 20 分钟（2-3、6-7、10-11）。

## 技术栈

- **Kotlin Multiplatform** + **Compose Multiplatform**（Material3）— UI 与业务逻辑共享于 `shared` 模块
- **multiplatform-settings** — 配置与课表数据持久化（SharedPreferences + JSON）
- **kotlinx-serialization / kotlinx-datetime / kotlinx-coroutines**
- **Ktor Client** — 教务系统 API 通信
- **AndroidX Security Crypto** — 教务账号密码加密
- **Jetpack Glance** — 桌面小部件

要求：minSdk 24，targetSdk 36，Kotlin 2.4，AGP 9。

## 项目结构

```
├── androidApp/            # Android 应用入口
│   └── src/main/
│       ├── kotlin/.../MainActivity.kt
│       └── kotlin/.../widget/          # 今日剩余课程桌面小部件（Glance）
├── shared/                # KMP 共享模块
│   └── src/
│       ├── commonMain/kotlin/.../
│       │   ├── data/
│       │   │   ├── model/              # Course / Schedule / AppConfig 等领域模型
│       │   │   ├── storage/            # Settings / Schedule / Account 存储
│       │   │   ├── crypto/             # 账号密码加密（expect/actual）
│       │   │   └── jwgl/               # 金智教务系统客户端与解析
│       │   └── ui/                     # 今日 / 课表 / 添加课程 / 导入 / 设置界面
│       ├── androidMain/                # Android 平台实现（存储、加密、日期）
│       └── commonTest/                 # 共享单元测试
├── CONTEXT.md             # 领域语言词汇表（Ubiquitous Language）
└── docs/
    ├── adr/               # 架构决策记录（教务导入方式、快照语义）
    └── agents/            # 协作约定（issue 跟踪、标签、领域文档）
```

## 构建与运行

```bash
# 构建 Debug APK
./gradlew :androidApp:assembleDebug

# 运行单元测试
./gradlew :shared:allTests
```

APK 输出于 `androidApp/build/outputs/apk/debug/`。

## 隐私说明

所有数据（课表、配置、教务账号）仅存储在设备本地；教务密码使用 AndroidKeyStore 加密保存，不会上传至任何服务器。教务导入仅在用户主动操作时连接学校教务系统。
