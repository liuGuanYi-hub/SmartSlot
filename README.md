# 🏟️ SmartSlot 智能场地与时段预约系统 (Enterprise Edition)

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg" alt="Spring Boot 3" />
  <img src="https://img.shields.io/badge/JDK-17-orange.svg" alt="JDK 17" />
  <img src="https://img.shields.io/badge/Vue-3.4-blue.svg" alt="Vue 3" />
  <img src="https://img.shields.io/badge/Vite-5.4-purple.svg" alt="Vite 5" />
  <img src="https://img.shields.io/badge/Redis-Redisson-red.svg" alt="Redisson" />
  <img src="https://img.shields.io/badge/EasyExcel-3.3.4-yellow.svg" alt="EasyExcel" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED.svg" alt="Docker" />
</p>

<p align="center">
  <b>面向现代体育综合体、智能场馆与会议空间的工业级全栈场地与时段预约调度平台</b><br>
  深度融合<b>分布式高并发防超卖架构</b>、<b>2.5D 交互式场馆视差平面</b>、<b>拟物化全息防伪电子票</b>、<b>RBAC 细粒度权限与 AOP 审计</b>与<b>全链路容器化交付</b>
</p>

---

## 🌟 核心技术亮点与企业级架构设计

```
                                  [ SmartSlot 系统架构全景图 ]
  
        +-------------------------------------------------------------------------+
        |                          Web / Mobile 客户端层                          |
        |  Vue 3 + Pinia + Element Plus + ECharts + View Transitions + GSAP 动效  |
        +-----------------------------------+-------------------------------------+
                                            |
                         HTTP RESTful / WSS | (反向代理与长连接)
                                            v
        +-------------------------------------------------------------------------+
        |                           Nginx 网关反代服务                            |
        |          Gzip 压缩 | 静态资源缓存 | SPA 路由回退 | WebSocket 桥接       |
        +-----------------------------------+-------------------------------------+
                                            |
                                            v
        +-------------------------------------------------------------------------+
        |                      Spring Boot 3 业务中台应用                         |
        |                                                                         |
        |  [安全鉴权层]     JWT 认证拦截器 + RBAC 细粒度切面 (@RequiresRoles)      |
        |  [并发防护层]     Lua 脚本原子锁预占 + Spring AOP 幂等防重 (@Idempotent) |
        |  [异步履约层]     Redisson 延迟队列关单 + STOMP WebSocket 全网毫秒广播  |
        |  [运营管理层]     AOP 操作审计日志 (@LogRecord) + 数据脱敏 (DataMaskUtil) |
        |  [报表引擎层]     Alibaba EasyExcel 流式高性能报表导出引擎              |
        |  [支付网关层]     支付宝沙箱(RSA2) / 微信支付(HMAC-SHA256)异步Webhook与对账 |
        |  [IoT 硬件层]     MQTT QoS 1 智能门禁道闸指令下发 + 2.5D 舵机仿真联动台   |
        |  [状态自愈层]     OrderReconciliationTask 僵死订单主动巡检与对账补偿    |
        +-------------------------+-----------------------+-----------------------+
                                  |                       |
                                  v                       v
               +-----------------------------+ +-----------------------------+
               |         Redis 7.0           | |          MySQL 8.0          |
               |  分布式锁 / 延迟队列 / 缓存  | | 订单/支付流水/硬件通信日志  |
               +-----------------------------+ +-----------------------------+
```

---

## 🚀 三周全栈企业级演进历程

### 📅 第 1 周：高并发抗瞬时洪峰与分布式防超卖架构
- **Lua 脚本原子锁预占**：自主研发 `LuaAtomicLockManager`，将时段状态检查、锁抢占及 15 分钟 TTL 注入原子 Lua 脚本一次性执行，彻底终结极端高并发超卖现象。
- **Redisson 延迟队列超时关单**：引入 `RDelayedQueue` 监听 15 分钟未付订单，精准到秒级触发订单自动取消并回补释放时段锁。
- **STOMP WebSocket 全网毫秒级广播**：场馆时段状态变更（抢占、支付成功、超时取消、退订）秒级推送到所有在线用户端，界面无缝平滑变色。
- **Spring AOP `@Idempotent` 幂等防重**：基于用户 Token + 业务路由构建原子防重令牌，杜绝网络抖动造成的恶意多次重复下单。
- **双层缓存削峰防穿透**：Caffeine 本地热点缓存 + Redis 集中缓存，搭配 Hystrix 思想保障极端断网下的平滑降级。

### 📅 第 2 周：全景沉浸式视觉交互与拟物化动效体验
- **场馆立体 2.5D 交互式平面图**：开发 `VenueFloorPlan.vue`，纯原生 SVG 矢量绘制羽毛球馆、网球场、篮球馆与云端会议室，配备呼吸光环状态脉冲与动态视差悬浮。
- **21st.dev 磁吸物理按钮与数字滚轮**：封装 `MagneticButton.vue` 真实力学弹性拉扯交互；引入 `NumberTicker.vue` 平滑补间增长动画展示营收与订单数字。
- **拟物化全息激光防伪卡**：仿真打造炫彩反光金箔（`holographic-foil-strip`）、动态 SVG 离线二维码、30 秒 TOTP 动态刷新机制与入场盖章印泥仿真动效。
- **View Transitions 优雅水波转场暗黑模式**：集成浏览器最新 View Transitions API，一键触发自点击坐标向外扩散的优雅圆形水波遮罩切换。
- **全端 60fps 硬件加速响应式适配**：时段矩阵开启 `-webkit-overflow-scrolling: touch` 手势横滑，全面兼容移动端、平板与桌面。

### 📅 第 3 周：企业级管理中台、运营看板与商业闭环
- **RBAC 细粒度 4 级角色控制**：
  - `ROLE_ADMIN`（超级管理员）：全平台管理权限、配置变更、审计日志、报表导出。
  - `ROLE_MANAGER`（运营店长）：数据看板、场地状态排期调整、流水导出。
  - `ROLE_VERIFIER`（前台核销员）：前台扫码核销、订单核验、敏感信息自动脱敏查看。
  - `ROLE_USER`（尊享会员）：场地矩阵选座、在线下单、余额充值与电子票据查看。
- **AOP 操作审计日志与敏感数据脱敏**：
  - 自定义 `@LogRecord` 注解，全程记录操作人、模块、方法、参数、耗时及 IP。
  - `DataMaskUtil` 严格对手机号（`139****5678`）和姓名（`李*龙`）安全脱敏，符合企业数据合规。
- **全周 7x13 小时时段坪效热力图 (Slot Heatmap)**：
  - ECharts 日历热力矩阵直观透视周一到周日 09:00-22:00 各时段上座密度。
  - 智能输出坪效决策建议（如早鸟闲时 7 折与黄金档防拥堵溢价策略）。
- **Alibaba EasyExcel 流式报表导出引擎**：
  - 摆脱传统 POI 内存消耗，实现海量《订单流水对账单.xlsx》与《场地排期总表.xlsx》秒级流式导出。
- **履约状态机自愈与定时对账补偿**：
  - `OrderReconciliationTask` 定时与手动触发自愈（`POST /api/orders/reconcile`），确保 Redis 锁与数据库订单绝对强一致。
- **容器化编排一键交付**：
  - 多阶段构建极简 Docker 镜像，`docker-compose.yml` 一键编排 MySQL 8.0、Redis 7.0、Spring Boot 与 Nginx 完整运行集群。

### 📅 企业级拓展实战：物联网智能道闸门禁联动与多渠道支付网关 (已落地)
- **物联网智能门禁与物理道闸中控台 (IotGateConsole)**：
  - **工业级通信标准**：采用标准 `/iot/smartslot/v1/gate/{gateId}/command` 主题与 MQTT QoS 1 报文，搭载设备唯一 MAC、固件版本、开闸保持时长 (5s) 与时间戳签名。
  - **2.5D 拟物化旋转中控交互**：自研 `IotGateConsole.vue`，实现闸机物理通道三维立体俯视视角、摆臂 0°~90° 伺服舵机旋转转场、LED 实时通行指示灯与门禁 LCD 液晶屏。
  - **全自动核销联动**：前台核销员核销 6 位凭证码或扫码成功后，后端立即发布 STOMP `GATE_UNLOCK` 事件，中控台自动捕获驱动物理闸机旋转放行，并实时抓包打印高亮 JSON 遥测报文。
  - **远程应急中控**：支持管理员一键紧急开闸、常开维护模式与通道状态遥测切换。
- **多渠道支付网关接入与财务对账中心 (PaymentGateway)**：
  - **主流全渠道收银台**：集成支付宝 (Alipay - 沙箱 RSA2 验签)、微信支付 (WeChat Pay - HMAC-SHA256 签名) 与平台虚拟余额。
  - **沙箱全链路扫码出票**：提供一键**“📱 模拟手机扫码扣款成功 (触发沙箱异步 Webhook)”**，服务端验证签名、防重幂等入账并自动生成 6 位专属核销码。
  - **财务对账中枢**：后台提供专属对账中心抽屉，提供总入账营收、支付宝/微信/余额渠道拆解，并与系统订单逐笔撮合比对平账状态。

---

## 🛠️ 技术栈清单

| 领域 | 核心技术与组件 | 版本 / 选型理由 |
| :--- | :--- | :--- |
| **基础后端框架** | Spring Boot | `3.2.3`（Jakarta EE、虚拟线程兼容） |
| **数据持久层** | MyBatis-Plus | `3.5.5`（代码极简、动态分页插件） |
| **数据库** | MySQL | `8.0+`（InnoDB 事务行锁、JSON 支持） |
| **分布式与缓存** | Redis & Redisson | `3.28.0`（延迟队列、分布式原子锁、Lua 脚本） |
| **本地高性能缓存** | Caffeine | `3.x`（W-TinyLFU 淘汰算法、防缓存击穿） |
| **数据报表引擎** | Alibaba EasyExcel | `3.3.4`（流式无 OOM 风险报表导出） |
| **安全与脱敏** | JWT (jjwt) + Spring AOP | `0.12.5`（无状态鉴权、敏感字段脱敏） |
| **接口文档** | Knife4j OpenAPI 3 | `4.4.0`（企业级在线接口调试平台） |
| **前端核心架构** | Vue 3 + Vite | `Vue 3.4` + `Vite 5.4`（组合式 API、急速 HMR） |
| **状态管理与路由** | Pinia + Vue Router | 全局状态响应式持久化与 RBAC 动态路由守卫 |
| **UI 组件库** | Element Plus | `2.6.x`（暗黑主题变量无缝对接） |
| **数据可视化** | ECharts | `5.5.0`（玫瑰图、趋势双轴图、时段坪效热力图） |
| **容器化与部署** | Docker + Docker Compose + Nginx | 多阶段构建、网络隔离、健康检查、Gzip 压缩 |

---

## 🔑 默认预置体验账号

所有测试账号的默认登录密码统一为：`123456`

| 账号用户名 | 角色标识 | 角色职能与分配权限 |
| :--- | :--- | :--- |
| **admin** | `ROLE_ADMIN` | **超级管理员**：拥有全部最高权限，包括操作审计日志、全站报表导出、场地管理与数据大屏 |
| **manager** | `ROLE_MANAGER` | **运营店长**：拥有运营大屏、场地排期调度、订单检索与财务对账单导出权限 |
| **verifier** | `ROLE_VERIFIER` | **前台核销员**：拥有订单查询与 6 位入场码快捷核验权限，客户电话自动脱敏加密 |
| **user** | `ROLE_USER` | **尊享会员**：全景场地选座、时段锁定、在线充值/支付、全息电子防伪票展示与订单取消 |

---

## 💻 快速本地运行指南

### 环境依赖准备
- **Java**: JDK 17+
- **Node.js**: v18.0+ 或 v20.0+
- **MySQL**: 8.0+ (端口 3306，默认库名 `smart_slot`)
- **Redis**: 6.0+ 或 7.0+ (端口 6379)

### 步骤 1：数据库初始化
导入执行工程目录下的初始化 SQL 脚本：
```bash
smart-slot-backend/sql/init.sql
```

### 步骤 2：启动后端应用
```bash
cd smart-slot-backend
mvn clean spring-boot:run
```
- 后端服务端口：`http://localhost:8088`
- Knife4j 接口调试文档：`http://localhost:8088/doc.html`

### 步骤 3：启动前端应用
```bash
cd smart-slot-frontend
npm install
npm run dev
```
- 前端访问入口：`http://localhost:5173`

---

## 🐳 Docker 一键容器化部署

本项目提供了完整的多阶段轻量构建 Dockerfile 与 Docker-Compose 编排文件，支持在云服务器或本地一键部署整套微服务架构：

```bash
# 根目录下执行一键构建并启动 (MySQL 8.0 + Redis 7.0 + Backend + Frontend Nginx)
docker-compose up -d --build

# 查看运行容器状态与健康检查
docker-compose ps

# 停止并清理集群
docker-compose down
```

启动完成后直接访问 `http://<服务器IP或localhost>` 即可进入完整生产级系统。

---

## 📂 项目目录结构一览

```
SmartSlot/
├── docker-compose.yml                      # 生产级多容器服务统一编排配置
├── docs/                                   # 详细项目规划与技术文档
│   └── 开发计划.md                         # 3 周企业级开发演进计划 (100% 完成)
├── smart-slot-backend/                     # Spring Boot 3 后端微服务
│   ├── Dockerfile                          # 多阶段构建轻量化 JRE 17 容器镜像
│   ├── pom.xml                             # Maven 依赖清单 (Redisson, EasyExcel, JWT)
│   ├── sql/
│   │   └── init.sql                        # 完整建表与多角色初始化测试数据
│   └── src/
│       ├── main/
│       │   ├── java/com/smartslot/
│       │   │   ├── annotation/             # 自定义注解 (@RequiresRoles, @LogRecord, @Idempotent)
│       │   │   ├── aspect/                 # AOP 切面 (RBAC鉴权切面、操作审计切面、接口幂等切面)
│       │   │   ├── common/                 # 统一返回结果 Result、UserContext 上下文
│       │   │   ├── config/                 # Redis、WebMvc、WebSocket、DatabaseInitializer 配置
│       │   │   ├── controller/             # REST 控制器 (Venue, BookingOrder, IotGate, PaymentGateway, Logs...)
│       │   │   ├── dto/                    # 请求 DTO 与 Prepay/EasyExcel 导出模型
│       │   │   ├── entity/                 # MyBatis-Plus 实体类 (BookingOrder, IotGateLog, PaymentRecord...)
│       │   │   ├── lock/                   # LuaAtomicLockManager 脚本原子锁管理器
│       │   │   ├── mapper/                 # MyBatis 数据访问层 (IotGateLogMapper, PaymentRecordMapper...)
│       │   │   ├── queue/                  # Redisson 延迟队列与超时监听服务
│       │   │   ├── service/                # 业务逻辑契约接口与实现类 (IotGateService, PaymentGatewayService...)
│       │   │   ├── task/                   # OrderReconciliationTask 对账自愈定时任务
│       │   │   ├── util/                   # DataMaskUtil 数据脱敏工具类、JwtUtil
│       │   │   └── vo/                     # 业务视图模型 (SlotMatrixVo, DashboardVo, ReconciliationSummaryVo)
│       │   └── resources/
│       │       ├── application.yml         # 开发环境配置
│       │       └── application-prod.yml    # 生产环境配置 (环境变量外部化隔离)
│       └── test/                           # 单元测试与高并发原子锁回归测试
└── smart-slot-frontend/                    # Vue 3 极致交互前端
    ├── Dockerfile                          # 前端多阶段打包与 Nginx 部署镜像
    ├── nginx.conf                          # 生产级 Nginx 反代与 WebSocket 桥接
    ├── package.json                        # 前端依赖配置
    └── src/
        ├── api/                            # Axios 封装接口 (auth, venue, booking, admin, iot, pay)
        ├── components/
        │   ├── BookingDrawer.vue           # 拟物化快速提单抽屉
        │   ├── CashierModal.vue            # 支付宝/微信多渠道扫码收银台与沙箱一键回调
        │   ├── MagneticButton.vue          # 21st.dev 磁吸物理按钮组件
        │   ├── NumberTicker.vue            # 数字平滑补间动画计数器
        │   ├── StatCharts.vue              # ECharts 趋势图、玫瑰图与 7x13 时段坪效热力图
        │   ├── VenueFloorPlan.vue          # 场馆立体 2.5D 交互式平面图
        │   └── VerifyModal.vue             # 前台快捷 6 位扫码核销模态框
        ├── composables/
        │   └── useTheme.js                 # View Transitions API 圆形水波暗黑转场
        ├── directives/
        │   └── permission.js               # v-permission 细粒度角色按钮指令
        ├── router/                         # Vue Router 路由守卫与 RBAC 动态过滤
        ├── stores/                         # Pinia 响应式状态库 (user, venue)
        └── views/
            ├── HomeView.vue                # 沉浸式前台门户首页
            ├── LoginView.vue               # 现代化登录/注册页 (多角色一键快速体验)
            ├── MatrixView.vue              # 核心时段选座日历矩阵 (WebSocket 毫秒联动)
            ├── MyBookingsView.vue          # 会员中心 (全息防伪电子票据展示与退订)
            └── admin/                      # 管理运营中台
                ├── AdminDashboard.vue      # 运营数据大屏与 AI 坪效决策卡片
                ├── AdminLayout.vue         # 管理控制台侧边导航布局
                ├── AdminLogs.vue           # AOP 操作审计日志查询台
                ├── AdminOrders.vue         # 订单检索核销、网关财务对账与 Excel 流式导出
                ├── AdminVenues.vue         # 场地配置管理与排期表导出
                └── IotGateConsole.vue      # 物联网门禁道闸 2.5D 舵机中控台与 MQTT 抓包台
```

---

## 📄 开源许可
本项目遵循 MIT 开源许可证。
