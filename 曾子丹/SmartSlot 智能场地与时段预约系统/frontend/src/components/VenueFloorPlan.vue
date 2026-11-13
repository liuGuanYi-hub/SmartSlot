<template>
  <div class="venue-floor-plan-card card-shadow">
    <!-- 头部导航与分区切换 -->
    <div class="floor-plan-header">
      <div class="header-left">
        <div class="radar-tag">
          <span class="pulse-ring"></span>
          <span class="tag-text">2.5D 综合场馆全景交互俯视图</span>
        </div>
        <span class="header-sub">包含全域 16 大运动场馆；点击任意场地即可在下方时段矩阵中平滑居中聚焦</span>
      </div>

      <!-- 四大主题翼馆切换胶囊 -->
      <div class="zone-filter-tabs">
        <button 
          v-for="z in zones" 
          :key="z.key"
          class="zone-tab-btn"
          :class="{ active: currentZone === z.key }"
          @click="currentZone = z.key"
        >
          <span class="zone-icon">{{ z.icon }}</span>
          <span class="zone-name">{{ z.name }}</span>
          <span class="zone-count">{{ z.count }}</span>
        </button>
      </div>
    </div>

    <!-- 交互式 SVG 场馆立体建筑沙盘 (涵盖 1~16 全量场馆) -->
    <div class="svg-map-wrapper">
      <svg 
        viewBox="0 0 1160 520" 
        class="floor-plan-svg"
        xmlns="http://www.w3.org/2000/svg"
      >
        <!-- 纹理与渐变定义 -->
        <defs>
          <!-- 建筑微网格底纹 -->
          <pattern id="floor-grid" width="20" height="20" patternUnits="userSpaceOnUse">
            <path d="M 20 0 L 0 0 0 20" fill="none" stroke="rgba(226, 232, 240, 0.65)" stroke-width="0.75" />
          </pattern>

          <!-- 1. 羽毛球馆渐变 (绿) -->
          <linearGradient id="badmintonGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#ecfdf5" />
            <stop offset="100%" stop-color="#d1fae5" />
          </linearGradient>

          <!-- 2. 网球红土渐变 (橙红) -->
          <linearGradient id="clayGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#ffedd5" />
            <stop offset="100%" stop-color="#fed7aa" />
          </linearGradient>

          <!-- 3. 网球硬地/匹克球渐变 (澳网蓝) -->
          <linearGradient id="tennisHardGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#eff6ff" />
            <stop offset="100%" stop-color="#bfdbfe" />
          </linearGradient>

          <!-- 4. 篮球木地板渐变 (北美枫木原木金) -->
          <linearGradient id="basketGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#fffbeb" />
            <stop offset="100%" stop-color="#fde68a" />
          </linearGradient>

          <!-- 5. 恒温水上大池渐变 (蔚蓝水波) -->
          <linearGradient id="poolGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#e0f2fe" />
            <stop offset="50%" stop-color="#bae6fd" />
            <stop offset="100%" stop-color="#7dd3fc" />
          </linearGradient>

          <!-- 6. 亲子浅水池渐变 (温水浅碧) -->
          <linearGradient id="kidsPoolGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#f0fdfa" />
            <stop offset="100%" stop-color="#99f6e4" />
          </linearGradient>

          <!-- 7. 台球斯诺克墨绿呢面 -->
          <linearGradient id="snookerGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#064e3b" />
            <stop offset="100%" stop-color="#022c22" />
          </linearGradient>

          <!-- 8. 乒乓球赛场红地胶 -->
          <linearGradient id="pingpongGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#fee2e2" />
            <stop offset="100%" stop-color="#fca5a5" />
          </linearGradient>

          <!-- 9. 普拉提私教暖木色 -->
          <linearGradient id="pilatesGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#faf5ff" />
            <stop offset="100%" stop-color="#f3e8ff" />
          </linearGradient>

          <!-- 10. 会议室高贵紫 -->
          <linearGradient id="meetingGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#f5f3ff" />
            <stop offset="100%" stop-color="#ddd6fe" />
          </linearGradient>

          <!-- 11. 壁球馆透明玻璃 -->
          <linearGradient id="squashGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#f0f9ff" />
            <stop offset="100%" stop-color="#e0f2fe" />
          </linearGradient>
        </defs>

        <!-- 园区建筑地板基底 -->
        <rect x="10" y="10" width="1140" height="500" rx="20" fill="#f8fafc" stroke="#e2e8f0" stroke-width="1.5" class="svg-floor-bg" />
        <rect x="10" y="10" width="1140" height="500" rx="20" fill="url(#floor-grid)" />

        <!-- ======================================================== -->
        <!-- 第一层 (北翼): 荣耀挥拍与小球轻潮馆 (1, 2, 3, 11, 12, 4, 15) + 服务中心 -->
        <!-- ======================================================== -->

        <!-- 1. 羽毛球 1 号场 (奥运地胶) id:1 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 1, selected: selectedVenueId === 1, dimmed: isDimmed(1) }"
          @mouseenter="hoveredVenueId = 1"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(1)"
        >
          <rect x="25" y="25" width="96" height="120" rx="10" fill="url(#badmintonGrad)" stroke="#10b981" stroke-width="1.5" />
          <rect x="33" y="33" width="80" height="104" fill="none" stroke="rgba(16, 185, 129, 0.4)" stroke-width="1" />
          <line x1="33" y1="85" x2="113" y2="85" stroke="#10b981" stroke-width="1.6" stroke-dasharray="3 2" />
          <circle cx="73" cy="85" r="3" fill="#10b981" />
          <text x="73" y="55" text-anchor="middle" font-size="11" font-weight="700" fill="#065f46">羽毛球 1 号</text>
          <text x="73" y="70" text-anchor="middle" font-size="9" fill="#047857">奥运地胶</text>
          <text x="73" y="122" text-anchor="middle" font-size="10" font-weight="700" fill="#059669">￥60/h</text>
        </g>

        <!-- 2. 羽毛球 2 号场 (双打标准) id:2 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 2, selected: selectedVenueId === 2, dimmed: isDimmed(2) }"
          @mouseenter="hoveredVenueId = 2"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(2)"
        >
          <rect x="130" y="25" width="96" height="120" rx="10" fill="url(#badmintonGrad)" stroke="#10b981" stroke-width="1.5" />
          <rect x="138" y="33" width="80" height="104" fill="none" stroke="rgba(16, 185, 129, 0.4)" stroke-width="1" />
          <line x1="138" y1="85" x2="218" y2="85" stroke="#10b981" stroke-width="1.6" stroke-dasharray="3 2" />
          <circle cx="178" cy="85" r="3" fill="#10b981" />
          <text x="178" y="55" text-anchor="middle" font-size="11" font-weight="700" fill="#065f46">羽毛球 2 号</text>
          <text x="178" y="70" text-anchor="middle" font-size="9" fill="#047857">双打标准</text>
          <text x="178" y="122" text-anchor="middle" font-size="10" font-weight="700" fill="#059669">￥50/h</text>
        </g>

        <!-- 3. 羽毛球 3 号场 (进阶训练) id:3 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 3, selected: selectedVenueId === 3, dimmed: isDimmed(3) }"
          @mouseenter="hoveredVenueId = 3"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(3)"
        >
          <rect x="235" y="25" width="96" height="120" rx="10" fill="url(#badmintonGrad)" stroke="#10b981" stroke-width="1.5" />
          <rect x="243" y="33" width="80" height="104" fill="none" stroke="rgba(16, 185, 129, 0.4)" stroke-width="1" />
          <line x1="243" y1="85" x2="323" y2="85" stroke="#10b981" stroke-width="1.6" stroke-dasharray="3 2" />
          <circle cx="283" cy="85" r="3" fill="#10b981" />
          <text x="283" y="55" text-anchor="middle" font-size="11" font-weight="700" fill="#065f46">羽毛球 3 号</text>
          <text x="283" y="70" text-anchor="middle" font-size="9" fill="#047857">自动发球机</text>
          <text x="283" y="122" text-anchor="middle" font-size="10" font-weight="700" fill="#059669">￥50/h</text>
        </g>

        <!-- 11. 潮流匹克球 1 号场 id:11 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 11, selected: selectedVenueId === 11, dimmed: isDimmed(11) }"
          @mouseenter="hoveredVenueId = 11"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(11)"
        >
          <rect x="340" y="25" width="104" height="120" rx="10" fill="#ecfeff" stroke="#06b6d4" stroke-width="1.5" />
          <rect x="348" y="33" width="88" height="104" fill="none" stroke="rgba(6, 182, 212, 0.4)" stroke-width="1" />
          <line x1="348" y1="85" x2="436" y2="85" stroke="#0891b2" stroke-width="1.6" />
          <rect x="358" y="73" width="68" height="24" fill="#cffafe" stroke="#0891b2" stroke-width="0.8" opacity="0.6" />
          <text x="392" y="55" text-anchor="middle" font-size="11" font-weight="700" fill="#0e7490">潮流匹克球 1号</text>
          <text x="392" y="70" text-anchor="middle" font-size="9" fill="#0891b2">低冲击缓冲硬地</text>
          <text x="392" y="122" text-anchor="middle" font-size="10" font-weight="700" fill="#0891b2">￥58/h</text>
        </g>

        <!-- 12. 德国 ASB 全透玻璃壁球馆 id:12 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 12, selected: selectedVenueId === 12, dimmed: isDimmed(12) }"
          @mouseenter="hoveredVenueId = 12"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(12)"
        >
          <rect x="452" y="25" width="104" height="120" rx="10" fill="url(#squashGrad)" stroke="#0ea5e9" stroke-width="1.5" />
          <rect x="460" y="33" width="88" height="92" fill="none" stroke="#ef4444" stroke-width="1" stroke-dasharray="4 2" />
          <line x1="460" y1="125" x2="548" y2="125" stroke="#38bdf8" stroke-width="3" />
          <text x="504" y="55" text-anchor="middle" font-size="11" font-weight="700" fill="#0369a1">ASB 玻璃壁球</text>
          <text x="504" y="70" text-anchor="middle" font-size="9" fill="#0284c7">高弹回音减压</text>
          <text x="504" y="122" text-anchor="middle" font-size="10" font-weight="700" fill="#0284c7">￥70/h</text>
        </g>

        <!-- 4. 中心网球 1 号场 (罗兰加洛斯红土) id:4 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 4, selected: selectedVenueId === 4, dimmed: isDimmed(4) }"
          @mouseenter="hoveredVenueId = 4"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(4)"
        >
          <rect x="564" y="25" width="140" height="120" rx="10" fill="url(#clayGrad)" stroke="#ea580c" stroke-width="1.5" />
          <rect x="574" y="33" width="120" height="104" fill="none" stroke="rgba(234, 88, 12, 0.4)" stroke-width="1" />
          <line x1="634" y1="33" x2="634" y2="137" stroke="#ea580c" stroke-width="1.8" />
          <line x1="594" y1="85" x2="674" y2="85" stroke="#ea580c" stroke-width="1" stroke-dasharray="3 2" />
          <text x="634" y="55" text-anchor="middle" font-size="12" font-weight="700" fill="#9a3412">网球 1 号 (红土)</text>
          <text x="634" y="70" text-anchor="middle" font-size="9" fill="#c2410c">法网天然陶粒颗粒</text>
          <text x="634" y="122" text-anchor="middle" font-size="10" font-weight="700" fill="#c2410c">￥120/h</text>
        </g>

        <!-- 15. 中心网球 2 号场 (澳网快速硬地) id:15 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 15, selected: selectedVenueId === 15, dimmed: isDimmed(15) }"
          @mouseenter="hoveredVenueId = 15"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(15)"
        >
          <rect x="712" y="25" width="140" height="120" rx="10" fill="url(#tennisHardGrad)" stroke="#2563eb" stroke-width="1.5" />
          <rect x="722" y="33" width="120" height="104" fill="none" stroke="rgba(37, 99, 235, 0.4)" stroke-width="1" />
          <line x1="782" y1="33" x2="782" y2="137" stroke="#2563eb" stroke-width="1.8" />
          <line x1="742" y1="85" x2="822" y2="85" stroke="#2563eb" stroke-width="1" stroke-dasharray="3 2" />
          <text x="782" y="55" text-anchor="middle" font-size="12" font-weight="700" fill="#1e40af">网球 2 号 (澳网蓝)</text>
          <text x="782" y="70" text-anchor="middle" font-size="9" fill="#1d4ed8">快速硬地+测速雷达</text>
          <text x="782" y="122" text-anchor="middle" font-size="10" font-weight="700" fill="#1d4ed8">￥100/h</text>
        </g>

        <!-- 右上角: 综合服务中心 (SERVICES) -->
        <g class="reception-box">
          <rect x="860" y="25" width="275" height="120" rx="10" fill="#ffffff" stroke="#94a3b8" stroke-width="1.2" stroke-dasharray="3 3" class="svg-reception-bg" />
          <text x="997" y="52" text-anchor="middle" font-size="12" font-weight="800" fill="#334155">SERVICES · 园区综合运营中枢</text>
          <text x="997" y="72" text-anchor="middle" font-size="10" fill="#64748b">智能物联闸机核销 · 能量运动饮品站 · 淋浴更衣</text>
          <text x="997" y="90" text-anchor="middle" font-size="10" fill="#64748b">运动损伤应急救护箱 · 护具及拍线专业穿戴</text>
          <text x="997" y="120" text-anchor="middle" font-size="10" fill="#059669" font-weight="700">● 7x24小时智能物联网系统全天候守护</text>
        </g>

        <!-- 主过道 1 (北翼与中翼贯通主干动线) -->
        <path d="M 25 170 L 1135 170" stroke="#cbd5e1" stroke-width="2" stroke-dasharray="6 6" />
        <text x="580" y="165" text-anchor="middle" font-size="10" fill="#94a3b8" letter-spacing="4">MAIN CONCOURSE A · 荣耀挥拍与综合竞技动线主过道</text>

        <!-- ======================================================== -->
        <!-- 第二层 (中翼): 竞技对抗大馆 (5, 16, 14) + 身心会务尊享区 (9, 10, 13, 6) -->
        <!-- ======================================================== -->

        <!-- 5. 室内篮球半场 A (木地板) id:5 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 5, selected: selectedVenueId === 5, dimmed: isDimmed(5) }"
          @mouseenter="hoveredVenueId = 5"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(5)"
        >
          <rect x="25" y="195" width="145" height="135" rx="10" fill="url(#basketGrad)" stroke="#d97706" stroke-width="1.5" />
          <rect x="33" y="203" width="129" height="119" fill="none" stroke="rgba(217, 119, 6, 0.4)" stroke-width="1" />
          <circle cx="97" cy="262" r="22" fill="none" stroke="#d97706" stroke-width="1.2" />
          <path d="M 33 242 A 20 20 0 0 1 33 282 Z" fill="none" stroke="#d97706" stroke-width="1.2" />
          <text x="97" y="228" text-anchor="middle" font-size="11" font-weight="700" fill="#78350f">篮球半场 A</text>
          <text x="97" y="244" text-anchor="middle" font-size="9" fill="#92400e">NBA北美双龙骨枫木</text>
          <text x="97" y="308" text-anchor="middle" font-size="10" font-weight="700" fill="#b45309">￥80/h</text>
        </g>

        <!-- 16. 室内篮球赛事级全场 (奥运木地板) id:16 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 16, selected: selectedVenueId === 16, dimmed: isDimmed(16) }"
          @mouseenter="hoveredVenueId = 16"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(16)"
        >
          <rect x="178" y="195" width="220" height="135" rx="10" fill="url(#basketGrad)" stroke="#b45309" stroke-width="1.8" />
          <rect x="186" y="203" width="204" height="119" fill="none" stroke="rgba(180, 83, 9, 0.4)" stroke-width="1" />
          <line x1="288" y1="203" x2="288" y2="322" stroke="#b45309" stroke-width="1.4" />
          <circle cx="288" cy="262" r="24" fill="none" stroke="#b45309" stroke-width="1.2" />
          <path d="M 186 242 A 20 20 0 0 1 186 282 Z" fill="none" stroke="#b45309" stroke-width="1.2" />
          <path d="M 390 242 A 20 20 0 0 0 390 282 Z" fill="none" stroke="#b45309" stroke-width="1.2" />
          <text x="288" y="226" text-anchor="middle" font-size="12" font-weight="800" fill="#78350f">篮球赛事级全场 (16号)</text>
          <text x="288" y="242" text-anchor="middle" font-size="9" fill="#92400e">FIBA认证双龙骨 · 液压篮架</text>
          <text x="288" y="308" text-anchor="middle" font-size="11" font-weight="800" fill="#b45309">￥180/h (包场)</text>
        </g>

        <!-- 14. 绿茵天地五人制室内笼式足球场 id:14 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 14, selected: selectedVenueId === 14, dimmed: isDimmed(14) }"
          @mouseenter="hoveredVenueId = 14"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(14)"
        >
          <rect x="406" y="195" width="240" height="135" rx="10" fill="#15803d" stroke="#166534" stroke-width="1.8" />
          <rect x="414" y="203" width="224" height="119" fill="none" stroke="#ffffff" stroke-width="1.2" />
          <line x1="526" y1="203" x2="526" y2="322" stroke="#ffffff" stroke-width="1.2" />
          <circle cx="526" cy="262" r="25" fill="none" stroke="#ffffff" stroke-width="1.2" />
          <rect x="414" y="237" width="25" height="50" fill="none" stroke="#ffffff" stroke-width="1" />
          <rect x="613" y="237" width="25" height="50" fill="none" stroke="#ffffff" stroke-width="1" />
          <text x="526" y="226" text-anchor="middle" font-size="12" font-weight="800" fill="#f0fdf4">五人制室内笼式足球</text>
          <text x="526" y="242" text-anchor="middle" font-size="9" fill="#bbf7d0">FIFA认证免充砂环保草坪</text>
          <text x="526" y="308" text-anchor="middle" font-size="11" font-weight="800" fill="#ffffff">￥120/h</text>
        </g>

        <!-- 9. 乒乓球 1 号专业比赛场 (红双喜彩虹台) id:9 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 9, selected: selectedVenueId === 9, dimmed: isDimmed(9) }"
          @mouseenter="hoveredVenueId = 9"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(9)"
        >
          <rect x="654" y="195" width="110" height="135" rx="10" fill="url(#pingpongGrad)" stroke="#ef4444" stroke-width="1.5" />
          <rect x="674" y="237" width="70" height="40" rx="3" fill="#1e3a8a" stroke="#ffffff" stroke-width="1" />
          <line x1="709" y1="237" x2="709" y2="277" stroke="#ffffff" stroke-width="1.2" />
          <text x="709" y="224" text-anchor="middle" font-size="11" font-weight="700" fill="#991b1b">乒乓球 1 号场</text>
          <text x="709" y="295" text-anchor="middle" font-size="9" fill="#b91c1c">红双喜彩虹台</text>
          <text x="709" y="318" text-anchor="middle" font-size="10" font-weight="700" fill="#dc2626">￥35/h</text>
        </g>

        <!-- 10. 乔氏中式黑八尊享包厢 (金腿球台) id:10 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 10, selected: selectedVenueId === 10, dimmed: isDimmed(10) }"
          @mouseenter="hoveredVenueId = 10"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(10)"
        >
          <rect x="772" y="195" width="116" height="135" rx="10" fill="#042f2e" stroke="#0d9488" stroke-width="1.5" />
          <rect x="786" y="237" width="88" height="46" rx="4" fill="#0f766e" stroke="#d97706" stroke-width="2" />
          <circle cx="790" cy="241" r="3" fill="#111827" />
          <circle cx="870" cy="241" r="3" fill="#111827" />
          <circle cx="790" cy="279" r="3" fill="#111827" />
          <circle cx="870" cy="279" r="3" fill="#111827" />
          <circle cx="830" cy="240" r="2.5" fill="#111827" />
          <circle cx="830" cy="280" r="2.5" fill="#111827" />
          <text x="830" y="222" text-anchor="middle" font-size="11" font-weight="700" fill="#ccfbf1">乔氏黑八包厢</text>
          <text x="830" y="298" text-anchor="middle" font-size="9" fill="#5eead4">英国6811台呢</text>
          <text x="830" y="318" text-anchor="middle" font-size="10" font-weight="700" fill="#2dd4bf">￥68/h</text>
        </g>

        <!-- 13. 光影普拉提大器械核心床私教房 id:13 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 13, selected: selectedVenueId === 13, dimmed: isDimmed(13) }"
          @mouseenter="hoveredVenueId = 13"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(13)"
        >
          <rect x="896" y="195" width="116" height="135" rx="10" fill="url(#pilatesGrad)" stroke="#c084fc" stroke-width="1.5" />
          <rect x="911" y="240" width="86" height="36" rx="6" fill="#f3e8ff" stroke="#a855f7" stroke-width="1" />
          <line x1="925" y1="240" x2="925" y2="276" stroke="#a855f7" stroke-width="1.5" />
          <line x1="983" y1="240" x2="983" y2="276" stroke="#a855f7" stroke-width="1.5" />
          <text x="954" y="222" text-anchor="middle" font-size="11" font-weight="700" fill="#6b21a8">光影普拉提房</text>
          <text x="954" y="292" text-anchor="middle" font-size="9" fill="#9333ea">Reformer核心床</text>
          <text x="954" y="318" text-anchor="middle" font-size="10" font-weight="700" fill="#9333ea">￥90/h</text>
        </g>

        <!-- 6. 云端多媒体会议室 (20人) id:6 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 6, selected: selectedVenueId === 6, dimmed: isDimmed(6) }"
          @mouseenter="hoveredVenueId = 6"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(6)"
        >
          <rect x="1020" y="195" width="115" height="135" rx="10" fill="url(#meetingGrad)" stroke="#8b5cf6" stroke-width="1.5" />
          <circle cx="1077" cy="260" r="26" fill="none" stroke="#8b5cf6" stroke-width="1.2" stroke-dasharray="3 2" />
          <rect x="1047" y="250" width="60" height="20" rx="4" fill="#8b5cf6" opacity="0.25" />
          <text x="1077" y="222" text-anchor="middle" font-size="11" font-weight="700" fill="#4c1d95">云端会议室</text>
          <text x="1077" y="295" text-anchor="middle" font-size="9" fill="#6d28d9">4K双屏+无线投屏</text>
          <text x="1077" y="318" text-anchor="middle" font-size="10" font-weight="700" fill="#6d28d9">￥150/h</text>
        </g>

        <!-- 主过道 2 (水上中心动线) -->
        <path d="M 25 348 L 1135 348" stroke="#cbd5e1" stroke-width="1.5" stroke-dasharray="4 4" />
        <text x="580" y="344" text-anchor="middle" font-size="9" fill="#94a3b8" letter-spacing="3">AQUATICS PLAZA · 水上恒温水适能中心</text>

        <!-- ======================================================== -->
        <!-- 第三层 (南翼): 水上运动中心 (7: 50米8泳道大池, 8: 亲子浅水池) -->
        <!-- ======================================================== -->

        <!-- 7. 奥体国际标准恒温泳池 (50米8泳道) id:7 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 7, selected: selectedVenueId === 7, dimmed: isDimmed(7) }"
          @mouseenter="hoveredVenueId = 7"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(7)"
        >
          <rect x="25" y="365" width="760" height="135" rx="12" fill="url(#poolGrad)" stroke="#0284c7" stroke-width="1.8" />
          <!-- 8 条标准泳道与浮标线 -->
          <line x1="25" y1="380" x2="785" y2="380" stroke="rgba(2, 132, 199, 0.4)" stroke-width="1" stroke-dasharray="8 4" />
          <line x1="25" y1="396" x2="785" y2="396" stroke="rgba(2, 132, 199, 0.4)" stroke-width="1" stroke-dasharray="8 4" />
          <line x1="25" y1="412" x2="785" y2="412" stroke="rgba(2, 132, 199, 0.4)" stroke-width="1" stroke-dasharray="8 4" />
          <line x1="25" y1="428" x2="785" y2="428" stroke="rgba(2, 132, 199, 0.4)" stroke-width="1" stroke-dasharray="8 4" />
          <line x1="25" y1="444" x2="785" y2="444" stroke="rgba(2, 132, 199, 0.4)" stroke-width="1" stroke-dasharray="8 4" />
          <line x1="25" y1="460" x2="785" y2="460" stroke="rgba(2, 132, 199, 0.4)" stroke-width="1" stroke-dasharray="8 4" />
          <line x1="25" y1="476" x2="785" y2="476" stroke="rgba(2, 132, 199, 0.4)" stroke-width="1" stroke-dasharray="8 4" />
          
          <text x="405" y="416" text-anchor="middle" font-size="14" font-weight="800" fill="#0369a1">奥体国际标准恒温泳池 (50米8泳道)</text>
          <text x="405" y="440" text-anchor="middle" font-size="10" fill="#0284c7">28℃全天候恒温水质循环 · 臭氧+硅藻土双重净化 · 国家级专业救生员在岗</text>
          <text x="405" y="468" text-anchor="middle" font-size="12" font-weight="800" fill="#0284c7">￥45/小时 (含淋浴更衣与智能手环锁柜)</text>
        </g>

        <!-- 8. 亲子水适能与康复浅水池 id:8 -->
        <g 
          class="court-group" 
          :class="{ active: hoveredVenueId === 8, selected: selectedVenueId === 8, dimmed: isDimmed(8) }"
          @mouseenter="hoveredVenueId = 8"
          @mouseleave="hoveredVenueId = null"
          @click="selectVenue(8)"
        >
          <rect x="795" y="365" width="340" height="135" rx="12" fill="url(#kidsPoolGrad)" stroke="#0d9488" stroke-width="1.5" />
          <rect x="805" y="375" width="320" height="115" rx="8" fill="none" stroke="rgba(13, 148, 136, 0.3)" stroke-width="1" stroke-dasharray="4 2" />
          <text x="965" y="415" text-anchor="middle" font-size="13" font-weight="800" fill="#115e59">亲子水适能与康复浅水池</text>
          <text x="965" y="440" text-anchor="middle" font-size="10" fill="#0f766e">32℃亲子适宜暖水 · 0.8-1.1m 阶梯缓冲缓坡</text>
          <text x="965" y="468" text-anchor="middle" font-size="12" font-weight="800" fill="#0f766e">￥55/小时 (配备儿童浮具)</text>
        </g>
      </svg>
    </div>

    <!-- 底部状态指示与快速聚焦卡片栏 -->
    <div class="floor-plan-footer">
      <div class="footer-legend">
        <span class="legend-item"><span class="legend-color legend-badminton"></span> 挥拍竞技 (羽/网)</span>
        <span class="legend-item"><span class="legend-color legend-ball"></span> 热血大球 (篮/足)</span>
        <span class="legend-item"><span class="legend-color legend-trend"></span> 潮流轻运动 (匹克/壁球)</span>
        <span class="legend-item"><span class="legend-color legend-water"></span> 恒温水上馆 (泳池/亲子)</span>
        <span class="legend-item"><span class="legend-color legend-club"></span> 尊享身心 (台球/乒乓/普拉提/会议)</span>
      </div>
      <div class="footer-action-tip">
        <span v-if="selectedVenueId" class="selected-tip">
          🎯 已聚焦选择：<strong>{{ getVenueName(selectedVenueId) }}</strong>
        </span>
        <span v-else class="normal-tip">点击俯视图任意场馆卡片，下方日历矩阵将实时平滑定位</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  selectedVenueId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['select-venue'])
const hoveredVenueId = ref(null)
const currentZone = ref('ALL')

const zones = [
  { key: 'ALL', name: '全域全景 (全部 16 馆)', icon: '🏛️', count: '16' },
  { key: 'RACKET', name: 'A馆·荣耀挥拍', icon: '🏸', count: '5', venueIds: [1, 2, 3, 4, 15] },
  { key: 'ARENA', name: 'B馆·热血大球', icon: '🏀', count: '3', venueIds: [5, 16, 14] },
  { key: 'AQUA', name: 'C馆·水上与潮玩', icon: '🏊', count: '4', venueIds: [7, 8, 11, 12] },
  { key: 'CLUB', name: 'D馆·尊享身心', icon: '🎱', count: '4', venueIds: [9, 10, 13, 6] }
]

const venueNamesMap = {
  1: '羽毛球 1 号场 (奥运专业地胶)',
  2: '羽毛球 2 号场 (双打标准场)',
  3: '羽毛球 3 号场 (进阶训练场)',
  4: '中心网球 1 号场 (红土体验)',
  5: '室内篮球半场 A (木地板)',
  6: '云端多媒体会议室 (20人)',
  7: '奥体国际标准恒温泳池 (50米8泳道)',
  8: '亲子水适能与康复浅水池',
  9: '乒乓球 1 号专业比赛场 (红双喜彩虹台)',
  10: '乔氏中式黑八尊享包厢 (金腿球台)',
  11: '潮流匹克球 1 号场 (低冲击高弹硬地)',
  12: '德国 ASB 全透玻璃壁球馆',
  13: '光影普拉提大器械核心床私教房',
  14: '绿茵天地五人制室内笼式足球场',
  15: '中心网球 2 号场 (澳网同款快速硬地)',
  16: '室内篮球赛事级全场 (奥运木地板)'
}

function getVenueName(id) {
  return venueNamesMap[id] || `场馆 #${id}`
}

function isDimmed(venueId) {
  if (currentZone.value === 'ALL') return false
  const activeZone = zones.find(z => z.key === currentZone.value)
  if (!activeZone || !activeZone.venueIds) return false
  return !activeZone.venueIds.includes(venueId)
}

function selectVenue(id) {
  emit('select-venue', id)
}
</script>

<style scoped>
.venue-floor-plan-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 20px 24px;
  margin-bottom: 24px;
  border: 1px solid var(--border-subtle);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.floor-plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.radar-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--pill-bg);
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid var(--pill-border);
}

.pulse-ring {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: radarPulse 2s infinite;
}

@keyframes radarPulse {
  0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.tag-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--pill-text);
}

.header-sub {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.zone-filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.zone-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--border-subtle);
  background: var(--card-bg-elevated);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.zone-tab-btn:hover {
  background: var(--border-hover);
  color: var(--text-main);
}

.zone-tab-btn.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.35);
}

.zone-count {
  font-size: 10px;
  opacity: 0.8;
  background: rgba(0, 0, 0, 0.1);
  padding: 1px 6px;
  border-radius: 999px;
}

.svg-map-wrapper {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border-radius: 14px;
  background: var(--card-bg-elevated);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.04);
}

.floor-plan-svg {
  width: 100%;
  min-width: 980px;
  height: auto;
  display: block;
}

.court-group {
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: center;
}

.court-group:hover rect:first-child {
  stroke-width: 2.5px;
  filter: drop-shadow(0 6px 16px rgba(0, 0, 0, 0.22));
  transform: translateY(-2px);
}

.court-group.active rect:first-child {
  stroke-width: 3px;
}

.court-group.selected rect:first-child {
  stroke-width: 3.5px !important;
  stroke: #4f46e5 !important;
  filter: drop-shadow(0 0 16px rgba(79, 70, 229, 0.85)) !important;
  transform: translateY(-2px) scale(1.01);
}

.court-group.dimmed {
  opacity: 0.28;
  filter: grayscale(80%);
}

.floor-plan-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
  font-size: 12px;
}

.footer-legend {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}

.legend-badminton { background: #10b981; }
.legend-ball { background: #f59e0b; }
.legend-trend { background: #06b6d4; }
.legend-water { background: #0284c7; }
.legend-club { background: #8b5cf6; }

.footer-action-tip {
  font-weight: 500;
}

.selected-tip strong {
  color: #4f46e5;
  font-weight: 700;
}

.normal-tip {
  color: var(--text-muted);
}

:global(html.dark) .court-group.selected rect:first-child {
  stroke: #818cf8 !important;
  filter: drop-shadow(0 0 18px rgba(129, 140, 248, 0.9)) !important;
}

:global(html.dark) .svg-floor-bg {
  fill: #0b1120;
  stroke: #1e293b;
}

:global(html.dark) .svg-reception-bg {
  fill: #1e293b;
  stroke: #334155;
}
</style>
