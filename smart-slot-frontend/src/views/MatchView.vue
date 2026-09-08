<template>
  <div class="match-hall-page">
    <div class="page-container">
      <!-- 1. 顶部氛围感 Hero 横幅 -->
      <div class="match-hero-banner card-shadow">
        <div class="hero-left">
          <div class="hero-tag">
            <span class="pulse-dot"></span>
            <span>球友社交 · AA 极速拼场</span>
          </div>
          <h1 class="hero-heading">运动找搭子，约球不孤单</h1>
          <p class="hero-subtext">
            羽毛球、网球、篮球、匹克球全品类招募！费用透明 AA 均摊，满员自动锁场出票并分发专属核销码，未成团全额秒退！
          </p>
          <div class="hero-btn-row">
            <el-button 
              type="warning" 
              size="large" 
              round 
              class="create-match-btn shimmer-badge"
              @click="openCreateDialog"
            >
              <el-icon><Plus /></el-icon> 我要发起拼场招募
            </el-button>
            <el-button 
              size="large" 
              round 
              plain 
              @click="$router.push('/profile?tab=matches')"
            >
              <el-icon><User /></el-icon> 我的拼场记录
            </el-button>
          </div>
        </div>

        <div class="hero-right-metrics">
          <div class="metric-card">
            <span class="m-val">4.0+</span>
            <span class="m-lbl">主流球友水平</span>
          </div>
          <div class="metric-card">
            <span class="m-val">98%</span>
            <span class="m-lbl">成团履约率</span>
          </div>
          <div class="metric-card">
            <span class="m-val">AA制</span>
            <span class="m-lbl">零押金透明分摊</span>
          </div>
        </div>
      </div>

      <!-- 2. 运动分类标签切换栏与搜索过滤器 -->
      <div class="filter-sticky-bar card-shadow">
        <!-- 运动分类胶囊 -->
        <div class="category-capsules">
          <button 
            class="capsule-btn" 
            :class="{ active: !selectedCategory }"
            @click="selectCategory('')"
          >
            全部运动
          </button>
          <button 
            v-for="cat in categories" 
            :key="cat"
            class="capsule-btn"
            :class="{ active: selectedCategory === cat }"
            @click="selectCategory(cat)"
          >
            {{ cat }}
          </button>
        </div>

        <!-- 筛选与搜索工具条 -->
        <div class="filter-tools-row">
          <el-radio-group v-model="statusFilter" size="small" @change="loadMatches">
            <el-radio-button :label="null">全部状态</el-radio-button>
            <el-radio-button :label="0">招募中</el-radio-button>
            <el-radio-button :label="1">已满员成团</el-radio-button>
          </el-radio-group>

          <div class="search-input-wrap">
            <el-input 
              v-model="keyword" 
              placeholder="搜索拼场主题、场馆名称或标签..." 
              clearable 
              size="small"
              @clear="loadMatches"
              @keyup.enter="loadMatches"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button size="small" type="primary" @click="loadMatches">搜索</el-button>
          </div>
        </div>
      </div>

      <!-- 3. 拼场活动卡片网格 -->
      <div class="matches-grid-section" v-loading="loading">
        <div v-if="matchList.length === 0" class="empty-matches-wrap">
          <el-empty description="当前分类暂无拼场招募，快来成为第一个发起人吧！">
            <el-button type="primary" round @click="openCreateDialog">立即发起拼场</el-button>
          </el-empty>
        </div>

        <div v-else class="matches-grid">
          <div 
            v-for="m in matchList" 
            :key="m.id" 
            class="match-card card-shadow"
            :class="{ 'card-full': m.status === 1, 'card-mine': m.isJoined }"
          >
            <!-- 卡片顶部：分类、状态与时间 -->
            <div class="card-top">
              <div class="card-tag-row">
                <span class="sport-badge">{{ m.categoryName }}</span>
                <span class="tag-pill">{{ m.sportTag || 'AA畅打' }}</span>
              </div>
              <div class="status-pill" :class="getStatusClass(m.status)">
                <span class="status-dot"></span>
                <span>{{ getStatusText(m) }}</span>
              </div>
            </div>

            <!-- 卡片标题与场馆 -->
            <h3 class="card-title" @click="openDetail(m)">{{ m.title }}</h3>
            <div class="card-venue-info">
              <el-icon><Location /></el-icon>
              <span class="v-name">{{ m.venueName }}</span>
            </div>
            <div class="card-time-info">
              <el-icon><Calendar /></el-icon>
              <span>{{ m.bookDate }} · <strong>{{ m.timeSlot }}</strong></span>
            </div>

            <!-- 发起人简略 -->
            <div class="card-creator-row">
              <el-avatar :size="28" :src="m.creatorAvatar" />
              <span class="creator-name">{{ m.creatorName }} (发起人)</span>
              <span class="creator-credit" title="发起人信用评级">
                ⭐ {{ m.creatorCreditScore || 100 }}分
              </span>
            </div>

            <!-- 参团进度条与头像空位插槽 -->
            <div class="card-members-zone">
              <div class="members-progress-header">
                <span class="progress-lbl">成团进度</span>
                <span class="progress-ratio">
                  <strong>{{ m.currentMembers }}</strong> / {{ m.targetMembers }} 人
                  <small v-if="m.status === 0" class="need-text">（还差 {{ m.targetMembers - m.currentMembers }} 人）</small>
                </span>
              </div>

              <!-- 参团成员头像/虚线空位占位符 (21st.dev 风格) -->
              <div class="avatar-slots-row">
                <div 
                  v-for="(p, idx) in m.participants" 
                  :key="idx" 
                  class="slot-avatar-item filled"
                  :title="p.nickname"
                >
                  <el-avatar :size="32" :src="p.avatar" />
                  <span v-if="p.isCreator === 1" class="crown-badge">👑</span>
                </div>
                <!-- 空位占位圈 -->
                <div 
                  v-for="i in Math.max(0, m.targetMembers - (m.participants?.length || m.currentMembers))" 
                  :key="'empty-' + i" 
                  class="slot-avatar-item empty"
                  title="虚位以待，快来上车！"
                >
                  <el-icon><Plus /></el-icon>
                </div>
              </div>
            </div>

            <!-- 底部费用与上车按钮 -->
            <div class="card-footer">
              <div class="price-box">
                <div class="aa-price">
                  <span class="sym">￥</span>
                  <span class="val">{{ m.costPerPerson }}</span>
                  <span class="unit">/人 AA</span>
                </div>
                <div class="total-hint">场馆原价 ￥{{ m.totalAmount }}</div>
              </div>

              <div class="action-box">
                <!-- 搭子实时微聊入口 -->
                <el-button 
                  size="small" 
                  round
                  class="chat-trigger-btn"
                  @click.stop="openChat(m)"
                  title="进入搭子群聊微室交流带球与战术"
                >
                  <el-icon><ChatDotRound /></el-icon> 实时微聊
                </el-button>

                <!-- 场景 1: 已满员且我已在车上 -> 查看核销码 -->
                <el-button 
                  v-if="m.isJoined && m.status === 1" 
                  type="success" 
                  size="small" 
                  round
                  @click="openPassModal(m)"
                >
                  <el-icon><Ticket /></el-icon> 我的入场码
                </el-button>

                <!-- 场景 2: 未满员且我已在车上 -> 等待中 -->
                <el-button 
                  v-else-if="m.isJoined && m.status === 0" 
                  type="info" 
                  plain 
                  size="small" 
                  round
                  @click="openDetail(m)"
                >
                  已在车上 (等球友)
                </el-button>

                <!-- 场景 3: 未满员且未加入 -> 立即上车 -->
                <el-button 
                  v-else-if="m.status === 0" 
                  type="primary" 
                  size="small" 
                  round 
                  class="join-now-btn"
                  @click="handleQuickJoin(m)"
                >
                  立即上车
                </el-button>

                <!-- 场景 4: 已满员且未加入 -> 满员 -->
                <el-button 
                  v-else 
                  disabled 
                  size="small" 
                  round
                >
                  已成团满员
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 4. 拼场详情抽屉 / 模态框 -->
      <el-dialog
        v-model="detailVisible"
        title="拼场搭子详情与成员名册"
        width="540px"
        append-to-body
        class="match-detail-dialog"
      >
        <div v-if="currentDetail" class="detail-content">
          <div class="detail-header-card">
            <div class="d-sport-tag">{{ currentDetail.categoryName }} · {{ currentDetail.sportTag }}</div>
            <h3 class="d-title">{{ currentDetail.title }}</h3>
            <div class="d-venue-meta">
              <p><strong>场馆地点：</strong>{{ currentDetail.venueName }}</p>
              <p><strong>活动时段：</strong>{{ currentDetail.bookDate }} ({{ currentDetail.timeSlot }})</p>
              <p><strong>费用规则：</strong>场地费 ￥{{ currentDetail.totalAmount }}，{{ currentDetail.targetMembers }} 人平摊，<strong>人均 ￥{{ currentDetail.costPerPerson }}</strong></p>
            </div>
            <div v-if="currentDetail.description" class="d-desc-box">
              <span class="desc-tag">球友寄语 / 要求：</span>
              <p>{{ currentDetail.description }}</p>
            </div>
          </div>

          <!-- 参团球友成员列表 -->
          <div class="detail-members-section">
            <h4 class="d-sec-title">已上车搭子名册 ({{ currentDetail.currentMembers }}/{{ currentDetail.targetMembers }})</h4>
            <div class="member-list">
              <div 
                v-for="p in currentDetail.participants" 
                :key="p.id" 
                class="member-item-row"
              >
                <el-avatar :size="36" :src="p.avatar" />
                <div class="m-info">
                  <div class="m-name-line">
                    <span class="m-name">{{ p.nickname }}</span>
                    <el-tag v-if="p.isCreator === 1" size="small" type="warning" effect="dark">发起人</el-tag>
                    <el-tag v-else size="small" type="success" effect="plain">搭子球友</el-tag>
                  </div>
                  <span class="m-time">加入时间：{{ formatDateTime(p.joinTime) }}</span>
                </div>
                <div class="m-pay-val">已付 ￥{{ p.payAmount }}</div>
              </div>
            </div>
          </div>

          <!-- 我的入场码展示 (若已成团且已加入) -->
          <div v-if="currentDetail.isJoined && currentDetail.myVerifyCode" class="pass-box card-shadow">
            <div class="pass-caption">🎟️ 您的专属道闸通行核销凭证</div>
            <div class="pass-code-number">{{ currentDetail.myVerifyCode }}</div>
            <p class="pass-hint">到场后出示此 6 位码给前台扫码或于闸机键盘输入，即可秒级核验入场！</p>
          </div>
        </div>

        <template #footer>
          <div class="dialog-footer-actions">
            <el-button @click="detailVisible = false">关闭</el-button>
            <el-button 
              type="primary" 
              plain 
              round
              @click="openChat(currentDetail)"
            >
              <el-icon><ChatDotRound /></el-icon> 进入搭子微室
            </el-button>
            <el-button 
              v-if="canCancel(currentDetail)" 
              type="danger" 
              plain 
              @click="handleCancelMatch(currentDetail)"
            >
              解散拼场并退款
            </el-button>
            <el-button 
              v-if="!currentDetail?.isJoined && currentDetail?.status === 0" 
              type="primary" 
              class="join-btn"
              @click="handleQuickJoin(currentDetail)"
            >
              支付 ￥{{ currentDetail?.costPerPerson }} 立即上车
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 5. 发起拼场招募对话框 -->
      <el-dialog
        v-model="createDialogVisible"
        title="发起运动拼场招募 (AA 制)"
        width="520px"
        append-to-body
        class="create-match-dialog"
      >
        <el-form 
          ref="createFormRef"
          :model="createForm" 
          :rules="createRules" 
          label-position="top"
        >
          <el-form-item label="选择预约场馆" prop="venueId">
            <el-select 
              v-model="createForm.venueId" 
              placeholder="请选择想要拼场的场馆" 
              style="width: 100%;"
              filterable
              @change="onVenueSelect"
            >
              <el-option 
                v-for="v in venueOptions" 
                :key="v.id" 
                :label="`${v.name} (￥${v.pricePerHour}/小时)`" 
                :value="v.id" 
              />
            </el-select>
          </el-form-item>

          <div class="grid-2-col">
            <el-form-item label="活动日期" prop="bookDate">
              <el-date-picker 
                v-model="createForm.bookDate" 
                type="date" 
                placeholder="选择日期" 
                value-format="YYYY-MM-DD"
                :disabled-date="disabledDate"
                style="width: 100%;"
              />
            </el-form-item>

            <el-form-item label="活动时段" prop="timeSlot">
              <el-select v-model="createForm.timeSlot" placeholder="选择时段" style="width: 100%;">
                <el-option v-for="s in standardSlots" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="拼场主题" prop="title">
            <el-input 
              v-model="createForm.title" 
              placeholder="如：周三晚羽球李宁双打进阶局缺2人！AA制畅打" 
              maxlength="50" 
              show-word-limit 
            />
          </el-form-item>

          <div class="grid-2-col">
            <el-form-item label="运动标签" prop="sportTag">
              <el-select v-model="createForm.sportTag" placeholder="选择标签" style="width: 100%;">
                <el-option label="双打进阶·AA畅打" value="双打进阶·AA畅打" />
                <el-option label="新手友好·破冰娱乐" value="新手友好·破冰娱乐" />
                <el-option label="半场3v3对抗赛" value="半场3v3对抗赛" />
                <el-option label="单打稳定对拉切磋" value="单打稳定对拉切磋" />
                <el-option label="高阶暴汗狂练" value="高阶暴汗狂练" />
              </el-select>
            </el-form-item>

            <el-form-item label="期望成团总人数 (含您)" prop="targetMembers">
              <el-input-number 
                v-model="createForm.targetMembers" 
                :min="2" 
                :max="12" 
                style="width: 100%;"
                @change="calcCost" 
              />
            </el-form-item>
          </div>

          <!-- AA 费用试算卡片 -->
          <div v-if="selectedVenue" class="cost-preview-card">
            <div class="cp-row">
              <span>场地总费用：</span>
              <strong>￥{{ selectedVenue.pricePerHour }}</strong>
            </div>
            <div class="cp-row">
              <span>平摊人数：</span>
              <span>{{ createForm.targetMembers }} 人</span>
            </div>
            <div class="cp-row cp-highlight">
              <span>您当前首付 AA 份额：</span>
              <span class="aa-val">￥{{ calculatedCostPerPerson }}</span>
            </div>
            <div class="cp-tip">💡 发起成功后系统将锁定该时段，并从您的虚拟钱包中划扣 ￥{{ calculatedCostPerPerson }}，若未成团可随时全额解散退款。</div>
          </div>

          <el-form-item label="活动说明与要求 (选填)">
            <el-input 
              v-model="createForm.description" 
              type="textarea" 
              rows="3" 
              placeholder="说明自备球拍/球具、集合地点或技术要求..." 
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            :loading="submittingCreate"
            class="submit-create-btn"
            @click="submitCreateMatch"
          >
            支付首付并立即发起
          </el-button>
        </template>
      </el-dialog>

      <!-- 6. 专属入场凭证弹窗 -->
      <el-dialog
        v-model="passModalVisible"
        title="拼场专属到场核销凭证"
        width="420px"
        append-to-body
        class="ticket-pass-dialog"
      >
        <div v-if="activePassItem" class="ticket-stub-body card-shadow">
          <div class="stub-venue-tag">{{ activePassItem.venueName }}</div>
          <h3 class="stub-title">{{ activePassItem.title }}</h3>
          <div class="stub-time-row">{{ activePassItem.bookDate }} ({{ activePassItem.timeSlot }})</div>
          
          <div class="stub-perforation"></div>

          <div class="stub-code-zone">
            <div class="stub-code-label">专属入场核销码</div>
            <div class="stub-glow-code">{{ activePassItem.myVerifyCode }}</div>
            <div class="totp-badge">🛡️ 智能物联门禁 · 扫码通行</div>
          </div>

          <el-button 
            type="primary" 
            plain 
            round 
            style="width: 100%; margin-top: 18px;"
            @click="copyVerifyCode(activePassItem.myVerifyCode)"
          >
            复制 6 位核销码
          </el-button>
        </div>
      </el-dialog>

      <!-- 6. 拼场搭子实时聊天微室抽屉 -->
      <MatchChatDrawer ref="chatDrawerRef" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, User, Search, Location, Calendar, Ticket, ChatDotRound } from '@element-plus/icons-vue'
import MatchChatDrawer from '@/components/MatchChatDrawer.vue'
import { getMatchPage, getMatchDetail, joinMatch, cancelMatch, createMatch } from '@/api/match'
import { getVenues } from '@/api/venue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const matchList = ref([])
const selectedCategory = ref('')
const statusFilter = ref(null)
const keyword = ref('')

const categories = [
  '羽毛球馆', '网球中心', '篮球全场/半场', '恒温游泳水上馆', 
  '乒乓球中心', '台球/斯诺克俱乐部', '潮流匹克球/壁球', '五人制室内足球场'
]

const standardSlots = [
  "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
  "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00",
  "17:00-18:00", "18:00-19:00", "19:00-20:00", "20:00-21:00",
  "21:00-22:00"
]

// 详情抽屉
const detailVisible = ref(false)
const currentDetail = ref(null)

// 搭子微聊抽屉
const chatDrawerRef = ref(null)
const openChat = (match) => {
  if (!match) return
  chatDrawerRef.value?.openDrawer(match)
}

// 凭证弹窗
const passModalVisible = ref(false)
const activePassItem = ref(null)

// 发起拼场表单
const createDialogVisible = ref(false)
const submittingCreate = ref(false)
const createFormRef = ref(null)
const venueOptions = ref([])
const selectedVenue = ref(null)

const createForm = reactive({
  venueId: null,
  bookDate: '',
  timeSlot: '19:00-20:00',
  title: '',
  sportTag: '双打进阶·AA畅打',
  targetMembers: 4,
  description: ''
})

const createRules = {
  venueId: [{ required: true, message: '请选择场地', trigger: 'change' }],
  bookDate: [{ required: true, message: '请选择活动日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择活动时段', trigger: 'change' }],
  title: [{ required: true, message: '请输入拼场主题', trigger: 'blur' }],
  targetMembers: [{ required: true, message: '请设置目标招募人数', trigger: 'change' }]
}

const calculatedCostPerPerson = computed(() => {
  if (!selectedVenue.value || !createForm.targetMembers) return '0.00'
  const price = selectedVenue.value.pricePerHour || 50
  return (price / createForm.targetMembers).toFixed(2)
})

function disabledDate(time) {
  return time.getTime() < Date.now() - 8.64e7
}

function selectCategory(cat) {
  selectedCategory.value = cat
  loadMatches()
}

function getStatusClass(status) {
  if (status === 1) return 'status-success'
  if (status === 3) return 'status-canceled'
  return 'status-recruiting'
}

function getStatusText(m) {
  if (m.status === 1) return '🎉 已满员成团'
  if (m.status === 2) return '已核销完成'
  if (m.status === 3) return '已解散'
  const need = m.targetMembers - m.currentMembers
  return need === 1 ? '🔥 仅差1人' : `招募中 缺${need}人`
}

function canCancel(m) {
  if (!m) return false
  return m.creatorId === userStore.userInfo?.id && m.status === 0
}

function formatDateTime(str) {
  if (!str) return ''
  return str.replace('T', ' ').substring(0, 16)
}

async function loadMatches() {
  loading.value = true
  try {
    const res = await getMatchPage({
      categoryName: selectedCategory.value || undefined,
      status: statusFilter.value !== null ? statusFilter.value : undefined,
      keyword: keyword.value || undefined,
      pageNum: 1,
      pageSize: 20
    })
    matchList.value = res?.records || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function openDetail(m) {
  try {
    const res = await getMatchDetail(m.id)
    currentDetail.value = res
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

function openPassModal(m) {
  activePassItem.value = m
  passModalVisible.value = true
}

function copyVerifyCode(code) {
  if (!code) return
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success(`核销码 ${code} 已成功复制！`)
  })
}

async function handleQuickJoin(m) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录会员账户')
    router.push('/login')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定加入拼场【${m.title}】？将从您的账户余额中划扣 AA 费用 ￥${m.costPerPerson}。`,
      '确认加入拼场',
      { confirmButtonText: '确认支付并加入', cancelButtonText: '再想想', type: 'info' }
    )

    await joinMatch(m.id)
    ElMessage.success('成功上车加入拼场！')
    await userStore.fetchCurrentUser?.()
    detailVisible.value = false
    loadMatches()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleCancelMatch(m) {
  try {
    await ElMessageBox.confirm(
      '确定解散该拼场？解散后已支付成员的 AA 款项将原路退回各成员钱包，时段将恢复空闲。',
      '确认解散拼场',
      { confirmButtonText: '确认解散', cancelButtonText: '暂不解散', type: 'warning' }
    )

    await cancelMatch(m.id)
    ElMessage.success('拼场已解散，款项已退还')
    await userStore.fetchCurrentUser?.()
    detailVisible.value = false
    loadMatches()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function openCreateDialog() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录会员账户')
    router.push('/login')
    return
  }

  createDialogVisible.value = true
  try {
    const venues = await getVenues()
    venueOptions.value = venues || []
    if (venueOptions.value.length > 0 && !createForm.venueId) {
      createForm.venueId = venueOptions.value[0].id
      selectedVenue.value = venueOptions.value[0]
    }
    // 默认明天日期
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    createForm.bookDate = tomorrow.toISOString().split('T')[0]
  } catch (e) {
    console.error(e)
  }
}

function onVenueSelect(id) {
  selectedVenue.value = venueOptions.value.find(v => v.id === id) || null
}

function calcCost() {
  // computed handles it
}

async function submitCreateMatch() {
  if (!createFormRef.value) return
  await createFormRef.value.validate()

  submittingCreate.value = true
  try {
    await createMatch(createForm)
    ElMessage.success('恭喜！拼场招募发起成功，时段已为您锁定！')
    await userStore.fetchCurrentUser?.()
    createDialogVisible.value = false
    loadMatches()
  } catch (e) {
    console.error(e)
  } finally {
    submittingCreate.value = false
  }
}

onMounted(() => {
  loadMatches()
})
</script>

<style scoped>
.match-hall-page {
  padding: 30px 20px 60px;
  background-color: var(--bg-primary);
  min-height: calc(100vh - 70px);
}

.page-container {
  max-width: 1240px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* Hero 横幅 */
.match-hero-banner {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  border-radius: 20px;
  padding: 38px 48px;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.35);
  color: #f87171;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 12px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: #ef4444;
  border-radius: 50%;
  animation: pulse-dot 1.6s infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.3); }
}

.hero-heading {
  font-size: 32px;
  font-weight: 900;
  margin: 0 0 10px 0;
  letter-spacing: -0.5px;
}

.hero-subtext {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
  max-width: 620px;
  line-height: 1.6;
  margin: 0 0 24px 0;
}

.hero-btn-row {
  display: flex;
  gap: 16px;
}

.create-match-btn {
  font-weight: 800;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: none;
}

.hero-right-metrics {
  display: flex;
  gap: 20px;
}

.metric-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 16px 20px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100px;
}

.metric-card .m-val {
  font-size: 24px;
  font-weight: 900;
  color: #38bdf8;
}

.metric-card .m-lbl {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4px;
}

/* 粘性筛选工具栏 */
.filter-sticky-bar {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 18px 24px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.category-capsules {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.capsule-btn {
  padding: 7px 16px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.capsule-btn:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}

.capsule-btn.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.filter-tools-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.search-input-wrap {
  display: flex;
  gap: 8px;
  width: 360px;
}

/* 卡片网格 */
.matches-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(370px, 1fr));
  gap: 22px;
}

.match-card {
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border-color);
  padding: 22px;
  display: flex;
  flex-direction: column;
  transition: all 0.25s ease;
}

.match-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.08);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-tag-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sport-badge {
  font-size: 11px;
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 700;
}

.tag-pill {
  font-size: 11px;
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.status-pill {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 12px;
  font-weight: 700;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-recruiting {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.status-recruiting .status-dot {
  background: #ef4444;
}

.status-success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-success .status-dot {
  background: #10b981;
}

.status-canceled {
  background: rgba(148, 163, 184, 0.1);
  color: #64748b;
}

.card-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 10px 0;
  line-height: 1.4;
  cursor: pointer;
  transition: color 0.2s ease;
}

.card-title:hover {
  color: #4f46e5;
}

.card-venue-info, .card-time-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.card-creator-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0 16px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-color);
}

.creator-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.creator-credit {
  font-size: 11px;
  color: #f59e0b;
  font-weight: 700;
}

/* 参团进度与插槽 */
.card-members-zone {
  background: var(--bg-primary);
  padding: 12px 14px;
  border-radius: 12px;
  margin-bottom: 18px;
}

.members-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  margin-bottom: 10px;
}

.progress-lbl {
  color: var(--text-secondary);
}

.progress-ratio strong {
  color: #4f46e5;
  font-size: 14px;
}

.need-text {
  color: #ef4444;
  font-weight: 700;
}

.avatar-slots-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.slot-avatar-item {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slot-avatar-item.empty {
  border: 1.5px dashed #cbd5e1;
  color: #94a3b8;
  font-size: 12px;
}

.crown-badge {
  position: absolute;
  top: -6px;
  right: -4px;
  font-size: 11px;
}

/* 底部价格与按钮 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--border-color);
}

.aa-price {
  display: flex;
  align-items: baseline;
  color: #ef4444;
}

.aa-price .sym {
  font-size: 14px;
  font-weight: 700;
}

.aa-price .val {
  font-size: 22px;
  font-weight: 900;
}

.aa-price .unit {
  font-size: 12px;
  font-weight: 600;
  margin-left: 2px;
}

.total-hint {
  font-size: 11px;
  color: var(--text-secondary);
}

.action-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-trigger-btn {
  border-color: #e2e8f0;
  color: #475569;
  background: #f8fafc;
  font-weight: 500;
  transition: all 0.2s ease;
}

.chat-trigger-btn:hover {
  border-color: #818cf8;
  color: #4f46e5;
  background: #eef2ff;
  transform: translateY(-1px);
}

.join-now-btn {
  font-weight: 700;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  border: none;
}

/* 详情抽屉 */
.detail-header-card {
  background: var(--bg-primary);
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.d-sport-tag {
  font-size: 12px;
  color: #4f46e5;
  font-weight: 700;
  margin-bottom: 6px;
}

.d-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.d-venue-meta p {
  margin: 4px 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.d-desc-box {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-color);
  font-size: 13px;
}

.desc-tag {
  color: #d97706;
  font-weight: 700;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.member-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-primary);
  padding: 10px 14px;
  border-radius: 10px;
}

.m-info {
  flex: 1;
}

.m-name-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.m-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.m-time {
  font-size: 11px;
  color: var(--text-secondary);
  display: block;
  margin-top: 2px;
}

.m-pay-val {
  font-size: 14px;
  font-weight: 800;
  color: #ef4444;
}

/* 入场凭证票根 */
.pass-box {
  margin-top: 24px;
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 100%);
  color: #ffffff;
  padding: 20px;
  border-radius: 14px;
  text-align: center;
}

.pass-caption {
  font-size: 13px;
  color: #cbd5e1;
}

.pass-code-number {
  font-size: 38px;
  font-weight: 900;
  letter-spacing: 6px;
  color: #fbbf24;
  margin: 10px 0;
  font-family: monospace;
}

.pass-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

/* 拟物入场票据弹窗 */
.ticket-stub-body {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  border: 1px solid var(--border-color);
}

.stub-venue-tag {
  font-size: 12px;
  color: #4f46e5;
  font-weight: 700;
}

.stub-title {
  font-size: 16px;
  font-weight: 800;
  margin: 6px 0 8px;
}

.stub-time-row {
  font-size: 13px;
  color: var(--text-secondary);
}

.stub-perforation {
  margin: 18px 0;
  border-top: 2px dashed var(--border-color);
}

.stub-glow-code {
  font-size: 36px;
  font-weight: 900;
  letter-spacing: 6px;
  color: #4f46e5;
  margin: 8px 0;
  font-family: monospace;
}

.totp-badge {
  font-size: 11px;
  color: #10b981;
  font-weight: 600;
}

.grid-2-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.cost-preview-card {
  background: rgba(79, 70, 229, 0.05);
  border: 1px solid rgba(79, 70, 229, 0.15);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 18px;
}

.cp-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 4px;
}

.cp-highlight {
  font-weight: 700;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed rgba(79, 70, 229, 0.2);
}

.aa-val {
  font-size: 18px;
  color: #ef4444;
  font-weight: 900;
}

.cp-tip {
  font-size: 11px;
  color: #6366f1;
  line-height: 1.4;
  margin-top: 6px;
}

@media (max-width: 768px) {
  .match-hero-banner {
    flex-direction: column;
    padding: 28px 20px;
    align-items: flex-start;
  }
  .hero-right-metrics {
    margin-top: 20px;
    width: 100%;
    justify-content: space-between;
  }
  .filter-tools-row {
    flex-direction: column;
    align-items: stretch;
  }
  .search-input-wrap {
    width: 100%;
  }
  .matches-grid {
    grid-template-columns: 1fr;
  }
  .grid-2-col {
    grid-template-columns: 1fr;
  }
}
</style>
