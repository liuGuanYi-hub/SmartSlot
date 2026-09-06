<template>
  <div class="iot-gate-page">
    <div class="page-header">
      <div>
        <h2 class="title">物联网智能门禁与道闸中控台</h2>
        <p class="subtitle">实时监听前台扫码核销放行指令，通过 MQTT / Webhook 驱动物理道闸舵机开合与通信遥测</p>
      </div>
      <div class="header-status">
        <el-tag type="success" effect="dark" class="iot-status-tag">
          <span class="live-pulse"></span> MQTT Broker 在线 (1883)
        </el-tag>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：3D/2.5D 拟物化智能道闸交互通道 -->
      <el-col :xs="24" :lg="11">
        <div class="turnstile-card card-shadow">
          <div class="card-top">
            <div class="card-title">
              <el-icon><Cpu /></el-icon> 物理道闸通道仿真 (Turnstile Servo)
            </div>
            <el-select v-model="selectedGateId" size="small" style="width: 170px;" @change="handleGateChange">
              <el-option
                v-for="d in devices"
                :key="d.gateId"
                :label="`${d.gateId} (${d.venueName.substring(0, 5)}...)`"
                :value="d.gateId"
              />
            </el-select>
          </div>

          <!-- 仿真闸机硬件外观 -->
          <div class="turnstile-visual-box">
            <!-- 闸机顶部 LCD 动态液晶屏幕 -->
            <div class="gate-lcd-panel">
              <div class="lcd-glow-text">
                <span class="lcd-dot"></span> {{ lcdMessage }}
              </div>
              <div class="lcd-sub">{{ selectedDevice?.venueName || '综合智慧场馆' }} · 闸机编号: {{ selectedGateId }}</div>
            </div>

            <!-- 闸机通道机箱与摆臂动画区域 -->
            <div class="gate-chassis-stage">
              <!-- 左侧机箱柱 -->
              <div class="chassis-pillar left">
                <div class="status-led-indicator" :class="isGateOpen ? 'led-green' : 'led-red'">
                  <div class="led-bulb"></div>
                  <span>{{ isGateOpen ? '通行 (PASS)' : '待验 (STOP)' }}</span>
                </div>
                <div class="sensor-eye"></div>
              </div>

              <!-- 中间通道与旋转机械摆臂 -->
              <div class="passage-lane">
                <div class="lane-ground">
                  <div class="ground-guide-line"></div>
                  <div class="person-shadow" :class="{ 'passing': isGateOpen }">
                    <span class="person-icon">🚶</span>
                  </div>
                </div>

                <!-- 舵机中心转轴 -->
                <div class="servo-joint"></div>

                <!-- 机械阻拦臂 (0°~90° 旋转动效) -->
                <div class="barrier-arm" :class="{ 'arm-opened': isGateOpen }">
                  <div class="arm-core"></div>
                  <div class="arm-stripe red-white"></div>
                </div>

                <!-- 倒计时放行悬浮提示 -->
                <transition name="fade">
                  <div v-if="isGateOpen" class="countdown-badge">
                    <span>放行中: {{ countdownSec }}s</span>
                  </div>
                </transition>
              </div>

              <!-- 右侧机箱柱 -->
              <div class="chassis-pillar right">
                <div class="nfc-scan-pad">
                  <span class="nfc-icon">📳</span>
                  <span class="nfc-label">扫码/刷卡区</span>
                </div>
              </div>
            </div>

            <!-- 通道控制与交互测试工具栏 -->
            <div class="gate-actions-bar">
              <el-button type="success" size="default" :loading="triggering" @click="testSimulateVerifyOpen">
                <el-icon><Ticket /></el-icon> 模拟扫码核销放行
              </el-button>
              <el-button type="danger" plain size="default" :loading="triggering" @click="emergencyOpen">
                <el-icon><WarningFilled /></el-icon> 紧急常开
              </el-button>
              <el-button size="default" @click="forceClose">
                <el-icon><Lock /></el-icon> 强制复位闭锁
              </el-button>
            </div>
          </div>
        </div>

        <!-- 硬件遥测属性卡 -->
        <div class="device-telemetry-card card-shadow">
          <div class="telemetry-grid">
            <div class="t-item">
              <span class="t-k">设备 IP:</span>
              <span class="t-v">{{ selectedDevice?.ip || '192.168.10.101' }}</span>
            </div>
            <div class="t-item">
              <span class="t-k">物理 MAC:</span>
              <span class="t-v">{{ selectedDevice?.mac || 'F8:E4:3B:11:2C:01' }}</span>
            </div>
            <div class="t-item">
              <span class="t-k">固件版本:</span>
              <span class="t-v">{{ selectedDevice?.firmware || 'SmartGate-v2.4.1' }}</span>
            </div>
            <div class="t-item">
              <span class="t-k">Wi-Fi 信号:</span>
              <span class="t-v text-emerald">{{ selectedDevice?.signalDbm || -42 }} dBm (极佳)</span>
            </div>
            <div class="t-item">
              <span class="t-k">累计过闸:</span>
              <span class="t-v text-indigo">{{ selectedDevice?.totalPassCount || 165 }} 人次</span>
            </div>
            <div class="t-item">
              <span class="t-k">运行工作模式:</span>
              <el-tag size="small" type="primary">自动核销放行</el-tag>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：MQTT / Webhook 报文抓包监视终端 -->
      <el-col :xs="24" :lg="13">
        <div class="terminal-card card-shadow">
          <div class="terminal-header">
            <div class="terminal-title">
              <span class="term-dot red"></span>
              <span class="term-dot yellow"></span>
              <span class="term-dot green"></span>
              <span class="term-text">MQTT / Webhook 硬件通信帧抓包监视器</span>
            </div>
            <div class="terminal-ops">
              <el-button size="small" plain @click="fetchLogs">
                <el-icon><Refresh /></el-icon> 刷新报文
              </el-button>
              <el-button size="small" @click="logPackets = []">清屏</el-button>
            </div>
          </div>

          <div class="terminal-body" ref="terminalBodyRef">
            <div v-if="logPackets.length === 0" class="term-empty">
              等待硬件指令或核销入场数据流...
            </div>
            <div
              v-for="(pkt, idx) in logPackets"
              :key="idx"
              class="packet-item"
            >
              <div class="packet-meta">
                <span class="pkt-time">{{ pkt.createTime || '12:00:00' }}</span>
                <el-tag size="small" :type="pkt.action?.includes('OPEN') ? 'success' : 'warning'">
                  {{ pkt.protocol || 'MQTT_QOS1' }}
                </el-tag>
                <span class="pkt-topic">{{ pkt.topic || '/iot/smartslot/v1/gate/cmd' }}</span>
                <span class="pkt-duration">{{ pkt.durationMs || 22 }}ms</span>
              </div>
              <pre class="packet-payload"><code>{{ formatJson(pkt.payloadJson) }}</code></pre>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { Cpu, Ticket, WarningFilled, Lock, Refresh } from '@element-plus/icons-vue'
import { getGateLogs, getGateDevices, manualOpenGate } from '@/api/iot'
import { ElMessage } from 'element-plus'

const devices = ref([])
const selectedGateId = ref('GATE_COURT_01')
const logPackets = ref([])
const isGateOpen = ref(false)
const countdownSec = ref(5)
const lcdMessage = ref('请出示 6 位专属核销码或电子门票')
const triggering = ref(false)
const terminalBodyRef = ref(null)

let countdownTimer = null

const selectedDevice = computed(() => {
  return devices.value.find(d => d.gateId === selectedGateId.value) || devices.value[0]
})

function formatJson(jsonStr) {
  if (!jsonStr) return '{}'
  try {
    const obj = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return jsonStr
  }
}

async function loadDevices() {
  try {
    const res = await getGateDevices()
    devices.value = res || []
    if (devices.value.length > 0 && !selectedGateId.value) {
      selectedGateId.value = devices.value[0].gateId
    }
  } catch (e) {
    console.error(e)
  }
}

async function fetchLogs() {
  try {
    const res = await getGateLogs(20)
    logPackets.value = res || []
  } catch (e) {
    console.error(e)
  }
}

function handleGateChange(val) {
  selectedGateId.value = val
  lcdMessage.value = `[${val}] 等待核验就绪`
}

function triggerArmOpen(duration = 5, customText = '核验成功！请通行') {
  isGateOpen.value = true
  countdownSec.value = duration
  lcdMessage.value = `[SMART-PASS 硬件已放行] ${customText}`

  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdownSec.value--
    if (countdownSec.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
      isGateOpen.value = false
      lcdMessage.value = '请出示 6 位专属核销码或电子门票'
    }
  }, 1000)
}

// 模拟扫码核销放行演示
async function testSimulateVerifyOpen() {
  triggering.value = true
  try {
    const res = await manualOpenGate(selectedGateId.value, 'BARRIER_OPEN')
    triggerArmOpen(5, '欢迎光临！订单 ORD' + Date.now().toString().slice(-6) + ' 已核验')
    ElMessage.success('开闸指令已通过 MQTT 成功下发，道闸摆臂开启 5 秒！')
    logPackets.value.unshift(res)
  } catch (e) {
    ElMessage.error(e.message || '下发指令失败')
  } finally {
    triggering.value = false
  }
}

async function emergencyOpen() {
  triggering.value = true
  try {
    const res = await manualOpenGate(selectedGateId.value, 'EMERGENCY_OPEN')
    isGateOpen.value = true
    if (countdownTimer) clearInterval(countdownTimer)
    lcdMessage.value = '⚠️ [中控指令] 紧急常开模式已激活'
    ElMessage.warning('道闸已设置为紧急常开状态')
    logPackets.value.unshift(res)
  } catch (e) {
    ElMessage.error(e.message || '应急开闸失败')
  } finally {
    triggering.value = false
  }
}

function forceClose() {
  if (countdownTimer) clearInterval(countdownTimer)
  isGateOpen.value = false
  lcdMessage.value = '请出示 6 位专属核销码或电子门票'
  ElMessage.info('道闸已强制闭锁复位')
}

// 监听 WebSocket 全网 GATE_UNLOCK 广播
function handleWsMessage(e) {
  try {
    const data = JSON.parse(e.data)
    if (data.eventType === 'GATE_UNLOCK') {
      triggerArmOpen(5, data.message || '核销成功放行')
      if (data.extra) {
        logPackets.value.unshift({
          id: Date.now(),
          gateId: selectedGateId.value,
          venueName: data.venueName || '指定场馆',
          action: 'BARRIER_OPEN',
          protocol: 'MQTT_QOS1',
          topic: '/iot/smartslot/v1/gate/' + selectedGateId.value + '/command',
          payloadJson: data.extra,
          durationMs: 24,
          createTime: new Date().toLocaleTimeString()
        })
      }
    }
  } catch (err) {
    // ignore
  }
}

onMounted(() => {
  loadDevices()
  fetchLogs()
  window.addEventListener('message', handleWsMessage)
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  window.removeEventListener('message', handleWsMessage)
})
</script>

<style scoped>
.iot-gate-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
}

.subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.iot-status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
}

.live-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.9); opacity: 0.7; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.7; }
}

.turnstile-card {
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
  padding: 20px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 仿真闸机硬件箱体与通道 */
.turnstile-visual-box {
  background: radial-gradient(circle at center, #1e293b 0%, #0f172a 100%);
  border-radius: 12px;
  padding: 20px;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.5);
}

.gate-lcd-panel {
  background: #020617;
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 10px 14px;
  font-family: monospace;
}

.lcd-glow-text {
  color: #38bdf8;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
  text-shadow: 0 0 6px rgba(56, 189, 248, 0.6);
}

.lcd-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #38bdf8;
}

.lcd-sub {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

/* 摆臂通道 */
.gate-chassis-stage {
  height: 220px;
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  position: relative;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.chassis-pillar {
  width: 70px;
  background: linear-gradient(180deg, #475569 0%, #334155 100%);
  border-radius: 6px;
  margin: 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  padding: 12px 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.status-led-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 700;
}

.led-bulb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.led-red .led-bulb {
  background: #ef4444;
  box-shadow: 0 0 10px #ef4444;
}
.led-red span { color: #f87171; }

.led-green .led-bulb {
  background: #10b981;
  box-shadow: 0 0 12px #10b981;
}
.led-green span { color: #34d399; }

.sensor-eye {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #0284c7;
  box-shadow: 0 0 6px #0284c7;
}

.nfc-scan-pad {
  background: #1e293b;
  border: 1px dashed #64748b;
  border-radius: 6px;
  padding: 8px 4px;
  text-align: center;
  width: 100%;
}

.nfc-icon { font-size: 16px; }
.nfc-label { font-size: 9px; color: #94a3b8; display: block; margin-top: 2px; }

/* 通道中央与机械臂 */
.passage-lane {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lane-ground {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  border-top: 2px dashed rgba(255, 255, 255, 0.15);
}

.person-shadow {
  position: absolute;
  bottom: 6px;
  left: 20%;
  font-size: 28px;
  transition: all 1.2s cubic-bezier(0.16, 1, 0.3, 1);
  opacity: 0.3;
}

.person-shadow.passing {
  left: 75%;
  opacity: 0.9;
  transform: scale(1.1);
}

.servo-joint {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #94a3b8;
  border: 3px solid #334155;
  position: absolute;
  left: 48%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 5;
  box-shadow: 0 0 8px rgba(0,0,0,0.5);
}

.barrier-arm {
  position: absolute;
  left: 48%;
  top: 50%;
  width: 120px;
  height: 12px;
  transform-origin: 0% 50%;
  transform: rotate(0deg);
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 4;
}

.barrier-arm.arm-opened {
  transform: rotate(-90deg);
}

.arm-core {
  width: 100%;
  height: 100%;
  background: #e2e8f0;
  border-radius: 6px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.4);
}

.arm-stripe.red-white {
  width: 100%;
  height: 100%;
  background: repeating-linear-gradient(
    45deg,
    #ef4444,
    #ef4444 10px,
    #ffffff 10px,
    #ffffff 20px
  );
}

.countdown-badge {
  position: absolute;
  top: 14px;
  background: rgba(16, 185, 129, 0.2);
  border: 1px solid #10b981;
  color: #34d399;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}

.gate-actions-bar {
  display: flex;
  gap: 10px;
  justify-content: center;
  padding-top: 4px;
}

/* 遥测信息卡 */
.device-telemetry-card {
  margin-top: 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 16px;
}

.telemetry-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.t-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.t-k { color: var(--text-muted); }
.t-v { font-weight: 600; color: var(--text-main); }
.text-emerald { color: #10b981; }
.text-indigo { color: #4f46e5; }

/* 右侧终端代码监视器 */
.terminal-card {
  background: #0f172a;
  border-radius: 14px;
  border: 1px solid #334155;
  display: flex;
  flex-direction: column;
  height: 600px;
  overflow: hidden;
}

.terminal-header {
  background: #1e293b;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #334155;
}

.terminal-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.term-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.term-dot.red { background: #ef4444; }
.term-dot.yellow { background: #f59e0b; }
.term-dot.green { background: #10b981; }

.term-text {
  font-size: 13px;
  font-weight: 700;
  color: #e2e8f0;
  margin-left: 6px;
}

.terminal-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  font-family: monospace;
}

.term-empty {
  color: #64748b;
  text-align: center;
  padding-top: 100px;
  font-size: 13px;
}

.packet-item {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.packet-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 11px;
}

.pkt-time { color: #94a3b8; }
.pkt-topic { color: #38bdf8; font-weight: 600; flex: 1; word-break: break-all; }
.pkt-duration { color: #a855f7; }

.packet-payload {
  margin: 0;
  background: #020617;
  padding: 8px 12px;
  border-radius: 6px;
  color: #86efac;
  font-size: 11px;
  line-height: 1.4;
  overflow-x: auto;
}
</style>
