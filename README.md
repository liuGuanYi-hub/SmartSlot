# SmartSlot 智能场地与时段预约系统

<p align="center">
  <b>基于 Spring Boot 3 + MyBatis-Plus + Redis + Vue 3 + Element Plus 的商业级时段预约系统</b>
</p>

---

## 🌟 项目亮点与五大技术要素覆盖

- **前端组件化开发**：
  - `SlotMatrix.vue`：日历时段矩阵网格组件（矩阵网格直观展示空闲、锁定、已售、维护等状态）
  - `BookingDrawer.vue`：时段核对与快速锁定抽屉组件
  - `VerifyModal.vue`：前台快捷 6 位核销码核验模态框组件
  - `StatCharts.vue`：ECharts 可视化走势与各场地热度玫瑰图
- **拦截器 (Interceptor)**：
  - `JwtAuthInterceptor`：JWT 登录身份解析与 ThreadLocal 上下文注入
  - `RateLimitInterceptor`：基于 Redis/本地原子计数的防刷限流拦截器
- **MyBatis-Plus 分页插件**：
  - 配置 `PaginationInnerInterceptor`，全面支撑用户端订单历史、管理端场地列表与订单的多条件组合分页
- **缓存与防超卖机制**：
  - 基于 Redis 分布式锁（SETNX 15分钟 TTL）进行时段库存预占，超时自动释放，杜绝时段冲突
  - 具备本地 Caffeine 缓存平滑降级支持
- **复杂表单交互**：
  - JSR-303 表单数据校验、手机号正则验证、场地新增与排期编辑、评价打分交互

---

## 🚀 快速启动

### 1. 数据库配置
执行 `smart-slot-backend/sql/init.sql` 初始化 MySQL 8.0 数据库与测试数据。

### 2. 后端服务启动
```bash
cd smart-slot-backend
mvn spring-boot:run
```
- API 接口文档：`http://localhost:8080/doc.html`

### 3. 前端服务启动
```bash
cd smart-slot-frontend
npm install
npm run dev
```
- 前台访问：`http://localhost:5173`

---

## 📖 详细文档
- 详细开发计划与系统架构图：[docs/开发计划.md](docs/开发计划.md)
