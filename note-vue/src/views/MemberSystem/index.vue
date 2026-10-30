<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import Button from '@/components/ui/Button.vue'
import Avatar from '@/components/ui/Avatar.vue'
import Badge from '@/components/ui/Badge.vue'
import Progress from '@/components/ui/Progress.vue'
import { Check, Crown, Zap, Database, X, CheckCircle, Loader2 } from 'lucide-vue-next'
import { getMemberPlans, createOrder, syncOrderStatus } from '@/api/payment'
import { getUserInfo } from '@/api/auth'

const activeTab = ref<'membership' | 'usage'>('membership')
const currentPlan = ref<'free' | 'pro'>('free')

// 模拟数据：会员状态与用量
const usageStats = ref({
  aiQueries: { used: 345, limit: 1000 },
  kbCount: { used: 2, limit: 5 },
})

// 模拟数据：月度统计
const monthlyStats = [
  { month: '1月', queries: 120 },
  { month: '2月', queries: 250 },
  { month: '3月', queries: 345 },
]

// 支付相关状态
const memberPlans = ref<any[]>([])
const selectedPlan = ref<any>(null)
const showPaymentModal = ref(false)
const showSuccessModal = ref(false)
const paymentLoading = ref(false)
const currentOrderNo = ref('')
const pollingTimer = ref<number | null>(null)

async function refreshUserInfo() {
  try {
    const r = await getUserInfo()
    if (r && r.code === 0 && r.data) {
      window.dispatchEvent(new CustomEvent('user-info-updated', { detail: r.data }))
    }
  } catch (e) {
    console.error('刷新用户信息失败', e)
  }
}

// 获取会员套餐
async function fetchMemberPlans() {
  try {
    const res = await getMemberPlans()
    if (res.code === 0) {
      memberPlans.value = res.data || []
    }
  } catch (e) {
    console.error('获取套餐失败', e)
  }
}

// 打开支付弹窗
function openPaymentModal(plan: any) {
  selectedPlan.value = plan
  showPaymentModal.value = true
}

// 关闭支付弹窗
function closePaymentModal() {
  showPaymentModal.value = false
  selectedPlan.value = null
  stopPolling()
}

// 确认续费/升级
async function confirmPayment() {
  if (!selectedPlan.value || paymentLoading.value) return
  
  paymentLoading.value = true
  try {
    const res = await createOrder(selectedPlan.value.id)
    if (res.code === 0 && res.data) {
      currentOrderNo.value = res.data.orderNo
      // 打开支付宝支付页面
      openAlipayPage(res.data.payForm)
      // 开始轮询订单状态
      startPolling()
    } else {
      alert(res.message || '创建订单失败')
    }
  } catch (e) {
    console.error('创建订单失败', e)
    alert('创建订单失败，请重试')
  } finally {
    paymentLoading.value = false
  }
}

// 打开支付宝支付页面
function openAlipayPage(formHtml: string) {
  // 创建一个隐藏的div来渲染表单
  const div = document.createElement('div')
  div.innerHTML = formHtml
  document.body.appendChild(div)
  // 自动提交表单
  const form = div.querySelector('form')
  if (form) {
    form.target = '_blank' // 在新窗口打开
    form.submit()
  }
  // 移除临时div
  setTimeout(() => {
    document.body.removeChild(div)
  }, 100)
}

// 开始轮询订单状态
function startPolling() {
  stopPolling()
  pollingTimer.value = window.setInterval(async () => {
    if (!currentOrderNo.value) return
    
    try {
      const res = await syncOrderStatus(currentOrderNo.value)
      if (res.code === 0 && res.data) {
        if (res.data.status === 1) {
          // 支付成功
          stopPolling()
          showPaymentModal.value = false
          showSuccessModal.value = true
          currentPlan.value = 'pro'
          refreshUserInfo()
        }
      }
    } catch (e) {
      console.error('查询订单状态失败', e)
    }
  }, 3000) // 每3秒查询一次
}

// 停止轮询
function stopPolling() {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

// 关闭成功弹窗
function closeSuccessModal() {
  showSuccessModal.value = false
  currentOrderNo.value = ''
}

// 手动检查支付状态
async function checkPaymentStatus() {
  if (!currentOrderNo.value) return
  
  try {
    const res = await syncOrderStatus(currentOrderNo.value)
    if (res.code === 0 && res.data) {
      if (res.data.status === 1) {
        stopPolling()
        showPaymentModal.value = false
        showSuccessModal.value = true
        currentPlan.value = 'pro'
        refreshUserInfo()
      } else {
        alert('支付尚未完成，请完成支付后再试')
      }
    }
  } catch (e) {
    console.error('查询订单状态失败', e)
    alert('查询失败，请稍后重试')
  }
}

function upgradeToPro() {
  // 查找专业版套餐
  const proPlan = memberPlans.value.find(p => p.planName?.includes('专业') || p.planName?.includes('Pro'))
  if (proPlan) {
    openPaymentModal(proPlan)
  } else if (memberPlans.value.length > 0) {
    openPaymentModal(memberPlans.value[0])
  } else {
    alert('暂无可用套餐')
  }
}

onMounted(() => {
  fetchMemberPlans()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="flex-1 h-full wc-page flex flex-col overflow-hidden">
    <!-- 顶部标题栏 -->
    <div class="wc-page-head">
      <div>
        <h1 class="wc-page-title">会员与运营中心</h1>
        <p class="wc-page-sub">管理你的会员订阅，查看 AI 用量统计</p>
      </div>
      <div class="flex items-center gap-3 bg-gradient-to-r from-amber-50 to-orange-50/50 px-4 py-2 rounded-xl border border-amber-100 shrink-0">
        <Avatar class="h-9 w-9">
          <div class="w-full h-full bg-amber-500 rounded-full flex items-center justify-center text-white font-bold text-[11px] tracking-wide">VIP</div>
        </Avatar>
        <div>
          <div class="flex items-center gap-2">
            <span class="font-semibold text-[13.5px] text-amber-900">高级会员</span>
            <Badge variant="secondary" class="bg-amber-100 text-amber-700 border-amber-200 text-[10px] px-1.5 py-0 h-4 font-semibold">PRO</Badge>
          </div>
          <p class="text-[11.5px] text-amber-700/80 mt-0.5">有效期至 2025-03-15</p>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="wc-page-body">
      <!-- 标签页 -->
      <div class="space-y-5">
        <div class="flex gap-1 w-full max-w-[300px] bg-gray-100 p-1 rounded-xl">
          <button 
            @click="activeTab = 'membership'" 
            :class="['flex-1 px-4 py-1.5 text-[13px] font-medium rounded-lg transition-colors', activeTab === 'membership' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-700']"
          >会员订阅</button>
          <button 
            @click="activeTab = 'usage'" 
            :class="['flex-1 px-4 py-1.5 text-[13px] font-medium rounded-lg transition-colors', activeTab === 'usage' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-700']"
          >用量统计</button>
        </div>

        <!-- 会员套餐展示 -->
        <div v-if="activeTab === 'membership'" class="grid md:grid-cols-2 gap-5 max-w-4xl">
          <!-- Free Plan -->
          <div :class="['wc-panel p-6 relative', currentPlan === 'free' ? '!border-blue-400 ring-1 ring-blue-500/10' : '']">
            <div class="mb-4">
              <div class="flex justify-between items-center">
                <h3 class="text-[15px] font-semibold">基础版 <span class="text-gray-400 font-normal">Free</span></h3>
                <Badge v-if="currentPlan === 'free'" variant="secondary" class="text-[11px] font-normal">当前使用</Badge>
              </div>
              <p class="text-[12.5px] text-gray-500 mt-1">适合个人尝试和轻度使用</p>
            </div>
            <div class="text-[26px] font-semibold tracking-tight mb-4">¥0 <span class="text-[12.5px] font-normal text-gray-400">/ 月</span></div>
            <ul class="space-y-2.5 mb-6">
              <li class="flex items-center gap-2 text-[13px] text-gray-700"><Check class="h-3.5 w-3.5 shrink-0 text-green-600" /><span>每月 <b>50</b> 次 AI 问答</span></li>
              <li class="flex items-center gap-2 text-[13px] text-gray-700"><Check class="h-3.5 w-3.5 shrink-0 text-green-600" /><span>最多 <b>3</b> 个知识库</span></li>
              <li class="flex items-center gap-2 text-[13px] text-gray-400"><Check class="h-3.5 w-3.5 shrink-0 text-gray-300" /><span>基础社区支持</span></li>
            </ul>
            <Button class="w-full h-9" variant="outline" :disabled="currentPlan === 'free'">{{ currentPlan === 'free' ? '当前方案' : '降级到基础版' }}</Button>
          </div>

          <!-- Pro Plan -->
          <div :class="['wc-panel p-6 relative bg-gradient-to-br from-white to-blue-50/60', currentPlan === 'pro' ? '!border-blue-400 ring-1 ring-blue-500/10' : '']">
            <div class="absolute -top-2.5 right-5 bg-blue-600 text-white text-[10px] px-2 py-0.5 rounded-full font-semibold tracking-wide">推荐</div>
            <div class="mb-4">
              <div class="flex justify-between items-center">
                <div class="flex items-center gap-2">
                  <Crown class="h-4 w-4 text-blue-600" />
                  <h3 class="text-[15px] font-semibold">专业版 <span class="text-gray-400 font-normal">Pro</span></h3>
                </div>
                <Badge v-if="currentPlan === 'pro'" class="bg-blue-50 text-blue-700 text-[11px] font-normal">当前使用</Badge>
              </div>
              <p class="text-[12.5px] text-gray-500 mt-1">为专业人士和团队打造</p>
            </div>
            <div class="text-[26px] font-semibold tracking-tight text-gray-900 mb-4">¥29 <span class="text-[12.5px] font-normal text-gray-400">/ 月</span></div>
            <ul class="space-y-2.5 mb-6">
              <li class="flex items-center gap-2 text-[13px] text-gray-700"><Check class="h-3.5 w-3.5 shrink-0 text-blue-600" /><span>每月 <b>1000</b> 次 AI 问答</span></li>
              <li class="flex items-center gap-2 text-[13px] text-gray-700"><Check class="h-3.5 w-3.5 shrink-0 text-blue-600" /><span><b>无限</b> 知识库数量</span></li>
              <li class="flex items-center gap-2 text-[13px] text-gray-700"><Check class="h-3.5 w-3.5 shrink-0 text-blue-600" /><span>优先客户支持</span></li>
            </ul>
            <Button class="w-full h-9" @click="upgradeToPro" :disabled="currentPlan === 'pro'">{{ currentPlan === 'pro' ? '已订阅' : '升级到专业版' }}</Button>
          </div>
        </div>

        <!-- 用量统计 -->
        <div v-if="activeTab === 'usage'" class="space-y-6">
          <div class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            <!-- AI 问答余额 -->
            <div class="wc-panel p-5">
              <div class="flex items-center justify-between mb-2">
                <h4 class="text-sm font-medium text-gray-700">AI 问答余额</h4>
                <Zap class="h-4 w-4 text-gray-400" />
              </div>
              <div class="text-2xl font-bold">{{ usageStats.aiQueries.limit - usageStats.aiQueries.used }} <span class="text-sm font-normal text-gray-500">/ {{ usageStats.aiQueries.limit }}</span></div>
              <Progress :value="usageStats.aiQueries.used" :max="usageStats.aiQueries.limit" class="h-2 mt-4" />
              <p class="text-xs text-gray-500 mt-2">本月已使用 {{ usageStats.aiQueries.used }} 次，将在 15 天后重置</p>
            </div>
            <!-- 知识库数量 -->
            <div class="wc-panel p-5">
              <div class="flex items-center justify-between mb-2">
                <h4 class="text-sm font-medium text-gray-700">知识库数量</h4>
                <Database class="h-4 w-4 text-gray-400" />
              </div>
              <div class="text-2xl font-bold">{{ usageStats.kbCount.used }} <span class="text-sm font-normal text-gray-500">/ {{ usageStats.kbCount.limit }}</span></div>
              <Progress :value="usageStats.kbCount.used" :max="usageStats.kbCount.limit" class="h-2 mt-4" />
              <p class="text-xs text-gray-500 mt-2">还可以创建 {{ usageStats.kbCount.limit - usageStats.kbCount.used }} 个知识库</p>
            </div>
          </div>

          <!-- 月度使用趋势 -->
          <div class="wc-panel p-5">
            <h4 class="text-[14px] font-semibold text-gray-900 mb-1">本月使用趋势</h4>
            <p class="text-[12.5px] text-gray-500 mb-4">过去三个月的 AI 问答消耗情况</p>
            <div class="h-[200px] w-full flex items-end justify-around p-4 border-b border-l border-gray-200">
              <div v-for="(stat, idx) in monthlyStats" :key="idx" class="flex flex-col items-center gap-2 group cursor-pointer">
                <div class="relative">
                  <div class="w-16 bg-blue-500 rounded-t-[5px] transition-all group-hover:bg-blue-600" :style="{ height: `${(stat.queries / 500) * 150}px` }" />
                  <div class="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">{{ stat.queries }}次</div>
                </div>
                <span class="text-sm text-gray-500 font-medium">{{ stat.month }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 支付确认弹窗 -->
    <div v-if="showPaymentModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
        <!-- 弹窗头部 -->
        <div class="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4 flex items-center justify-between">
          <h3 class="text-lg font-semibold text-white">确认支付</h3>
          <button @click="closePaymentModal" class="text-white/80 hover:text-white transition-colors">
            <X class="h-5 w-5" />
          </button>
        </div>
        
        <!-- 弹窗内容 -->
        <div class="p-6">
          <div v-if="selectedPlan" class="space-y-4">
            <!-- 套餐信息 -->
            <div class="bg-gray-50 rounded-lg p-4">
              <div class="flex items-center justify-between mb-2">
                <span class="text-gray-600">套餐名称</span>
                <span class="font-medium text-gray-900">{{ selectedPlan.planName }}</span>
              </div>
              <div class="flex items-center justify-between mb-2">
                <span class="text-gray-600">有效期</span>
                <span class="font-medium text-gray-900">{{ selectedPlan.durationMonths }} 个月</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-gray-600">支付金额</span>
                <span class="text-2xl font-bold text-blue-600">¥{{ selectedPlan.price }}</span>
              </div>
            </div>
            
            <!-- 支付方式 -->
            <div class="flex items-center gap-3 p-3 border border-blue-200 bg-blue-50 rounded-lg">
              <div class="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center">
                <svg class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21.422 15.358c-3.83 1.153-6.055 1.84-6.678 2.062-1.304.464-2.163.693-2.832.693-1.076 0-1.629-.693-2.163-2.062-.267-.693-1.244-4.158-2.93-10.396h3.75c1.157 3.87 1.996 6.693 2.516 8.468.534 1.876.89 2.062 1.069 2.062.178 0 .534-.186.534-.693 0-.321-1.069-4.158-1.069-4.158l3.214-.693s1.958 6.032 2.035 6.263c.089.267.178.454.178.454h2.376z"/>
                </svg>
              </div>
              <div class="flex-1">
                <div class="font-medium text-gray-900">支付宝支付</div>
                <div class="text-sm text-gray-500">推荐使用支付宝完成支付</div>
              </div>
              <Check class="h-5 w-5 text-blue-600" />
            </div>
            
            <!-- 提示信息 -->
            <p class="text-sm text-gray-500 text-center">
              点击确认后将跳转至支付宝完成支付
            </p>
          </div>
        </div>
        
        <!-- 弹窗底部 -->
        <div class="px-6 py-4 bg-gray-50 flex gap-3">
          <button 
            @click="closePaymentModal"
            class="flex-1 px-4 py-2.5 border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-100 transition-colors"
          >
            取消
          </button>
          <button 
            @click="confirmPayment"
            :disabled="paymentLoading"
            class="flex-1 px-4 py-2.5 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            <Loader2 v-if="paymentLoading" class="h-4 w-4 animate-spin" />
            {{ paymentLoading ? '处理中...' : '确认续费' }}
          </button>
        </div>
        
        <!-- 支付中状态 -->
        <div v-if="currentOrderNo && !paymentLoading" class="px-6 py-4 border-t border-gray-200">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2 text-sm text-gray-600">
              <Loader2 class="h-4 w-4 animate-spin text-blue-600" />
              <span>等待支付完成...</span>
            </div>
            <button 
              @click="checkPaymentStatus"
              class="text-sm text-blue-600 hover:text-blue-700 font-medium"
            >
              我已完成支付
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 支付成功弹窗 -->
    <div v-if="showSuccessModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-sm mx-4 overflow-hidden">
        <div class="p-8 text-center">
          <!-- 成功图标 -->
          <div class="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <CheckCircle class="h-10 w-10 text-green-500" />
          </div>
          
          <!-- 成功信息 -->
          <h3 class="text-xl font-bold text-gray-900 mb-2">支付成功</h3>
          <p class="text-gray-600 mb-6">恭喜您已成功升级为专业版会员！</p>
          
          <!-- 会员权益提示 -->
          <div class="bg-amber-50 rounded-lg p-4 mb-6 text-left">
            <div class="flex items-center gap-2 text-amber-800 font-medium mb-2">
              <Crown class="h-5 w-5 text-amber-600" />
              <span>专业版权益已生效</span>
            </div>
            <ul class="text-sm text-amber-700 space-y-1">
              <li>• 每月 1000 次 AI 问答</li>
              <li>• 无限知识库数量</li>
              <li>• 优先客户支持</li>
            </ul>
          </div>
          
          <!-- 确认按钮 -->
          <button 
            @click="closeSuccessModal"
            class="w-full px-4 py-2.5 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors"
          >
            开始使用
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
