<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { X, Crown, Loader2, CheckCircle } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import MembershipQuotaCard from '@/components/member/MembershipQuotaCard.vue'
import { getUserInfo } from '@/api/auth'
import { getNoteStats } from '@/api/notes'
import { getMemberPlans, createOrder, syncOrderStatus } from '@/api/payment'
import { useMembershipQuota } from '@/composables/useMembershipQuota'

interface Props {
  visible: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const selectedPlanId = ref<number | null>(null)
const agreed = ref(false)
const statsLoading = ref(false)
const userCreateTime = ref<Date | null>(null)
const noteCount = ref(0)
const mostProductiveDate = ref<string>('--')
const mostProductiveCount = ref(0)

const alipayLogo = new URL('../../views/PaymentResult/支付宝支付.svg', import.meta.url).href

// 配额管理
const { quotaData, loading: quotaLoading, fetchQuota } = useMembershipQuota()
const currentUserId = ref<number | null>(null)

// 支付相关状态
const memberPlans = ref<any[]>([])
const paymentLoading = ref(false)
const currentOrderNo = ref('')
const pollingTimer = ref<number | null>(null)
const showSuccessModal = ref(false)

function close() {
  emit('update:visible', false)
  stopPolling()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible) {
    close()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  stopPolling()
})

// 获取会员套餐
async function fetchMemberPlans() {
  try {
    const res = await getMemberPlans()
    if (res.code === 0 && res.data) {
      memberPlans.value = res.data
      // 默认选中第一个套餐
      if (res.data.length > 0 && !selectedPlanId.value) {
        selectedPlanId.value = res.data[0].id
      }
    }
  } catch (e) {
    console.error('获取套餐失败', e)
  }
}

// 当前选中的套餐
const selectedPlan = computed(() => {
  return memberPlans.value.find(p => p.id === selectedPlanId.value)
})

const validUntilText = computed(() => {
  const months = Number(selectedPlan.value?.durationMonths)
  if (!months || Number.isNaN(months)) return '--'
  const d = new Date()
  d.setMonth(d.getMonth() + months)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
})

// 确认支付
async function confirmPayment() {
  if (!selectedPlanId.value || paymentLoading.value || !agreed.value) return
  
  paymentLoading.value = true
  try {
    const res = await createOrder(selectedPlanId.value)
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
  const div = document.createElement('div')
  div.innerHTML = formHtml
  document.body.appendChild(div)
  const form = div.querySelector('form')
  if (form) {
    form.target = '_blank'
    form.submit()
  }
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
      if (res.code === 0 && res.data && res.data.status === 1) {
        stopPolling()
        showSuccessModal.value = true
      }
    } catch (e) {
      console.error('查询订单状态失败', e)
    }
  }, 3000)
}

// 停止轮询
function stopPolling() {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

// 手动检查支付状态
async function checkPaymentStatus() {
  if (!currentOrderNo.value) return
  try {
    const res = await syncOrderStatus(currentOrderNo.value)
    if (res.code === 0 && res.data) {
      if (res.data.status === 1) {
        stopPolling()
        showSuccessModal.value = true
      } else {
        alert('支付尚未完成，请完成支付后再试')
      }
    }
  } catch (e) {
    console.error('查询订单状态失败', e)
    alert('查询失败，请稍后重试')
  }
}

// 关闭成功弹窗
function closeSuccessModal() {
  showSuccessModal.value = false
  currentOrderNo.value = ''
  // 支付成功后刷新配额信息
  if (currentUserId.value) {
    fetchQuota(currentUserId.value, true)
  }
  close()
}

// 处理升级按钮点击
function handleUpgradeClick() {
  // 滚动到支付区域
  const paymentSection = document.querySelector('.payment-section')
  if (paymentSection) {
    paymentSection.scrollIntoView({ behavior: 'smooth' })
  }
}

async function loadUserStats() {
  if (statsLoading.value) return
  statsLoading.value = true
  try {
    const userRes = await getUserInfo()
    if (userRes?.code === 0 && userRes.data?.createTime) {
      userCreateTime.value = new Date(userRes.data.createTime)
      // 保存用户ID并获取配额信息
      if (userRes.data.id) {
        currentUserId.value = userRes.data.id
        await fetchQuota(userRes.data.id)
      }
    }

    const statsRes = await getNoteStats()
    if (statsRes?.code === 0 && statsRes.data) {
      noteCount.value = statsRes.data.total || 0
      mostProductiveDate.value = statsRes.data.mostProductiveDate || '--'
      mostProductiveCount.value = statsRes.data.mostProductiveCount || 0
    }
  } catch (error) {
    console.error('[MemberCenterModal] 获取会员统计失败:', error)
  } finally {
    statsLoading.value = false
  }
}

const activeDays = computed(() => {
  if (!userCreateTime.value) return 0
  const diffMs = Date.now() - userCreateTime.value.getTime()
  return Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)))
})

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      loadUserStats()
      fetchMemberPlans()
    }
  }
)
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <div class="absolute inset-0 bg-black/40" @click="close"></div>
        <div
          class="relative w-full max-w-5xl overflow-hidden rounded-2xl bg-white shadow-2xl"
          @click.stop
        >
          <button
            class="absolute right-4 top-4 rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
            @click="close"
          >
            <X class="h-5 w-5" />
          </button>
          <div class="grid gap-0 lg:grid-cols-[1.1fr_1fr]">
            <div class="relative overflow-hidden p-8 lg:p-10">
              <div class="absolute inset-0 bg-gradient-to-br from-blue-50 via-white to-gray-50"></div>
              <div class="relative space-y-6">
                <div class="space-y-2 text-gray-800">
                  <h2 class="text-2xl font-semibold">
                    你已经在这里耕耘知识 {{ activeDays }} 天，
                  </h2>
                  <p class="text-lg">积累了 {{ noteCount }} 篇笔记</p>
                  <p class="text-sm text-gray-500">
                    最高产的一天在 {{ mostProductiveDate }}，更新了 {{ mostProductiveCount }} 篇笔记。
                  </p>
                  <p class="text-sm text-gray-500">
                    知识是宝贵的财富，愿你一直陪伴知识成长。
                  </p>
                </div>

                <div class="relative h-48">
                  <div class="absolute bottom-0 left-0 right-0 flex items-end gap-2">
                    <div class="h-20 w-7 rounded-full bg-blue-100"></div>
                    <div class="h-28 w-7 rounded-full bg-blue-100"></div>
                    <div class="h-16 w-7 rounded-full bg-blue-100"></div>
                    <div class="h-36 w-7 rounded-full bg-blue-200"></div>
                    <div class="h-12 w-7 rounded-full bg-blue-100"></div>
                    <div class="h-24 w-7 rounded-full bg-blue-100"></div>
                    <div class="h-40 w-7 rounded-full bg-blue-500/60 relative">
                      <span class="absolute -top-7 left-1/2 -translate-x-1/2 rounded-full bg-gray-900 px-2 py-0.5 text-xs text-white">
                        {{ noteCount }} 篇笔记
                      </span>
                    </div>
                    <div class="h-14 w-7 rounded-full bg-blue-100"></div>
                    <div class="h-10 w-7 rounded-full bg-blue-100"></div>
                  </div>
                  <div class="absolute bottom-0 left-0 right-0 h-8 bg-gradient-to-t from-white/80 to-transparent"></div>
                </div>
                <div class="text-sm text-gray-500">我的知识财富</div>
                
                <!-- 配额信息卡片 -->
                <div class="mt-6">
                  <MembershipQuotaCard 
                    :quota-data="quotaData"
                    :show-upgrade="true"
                    @upgrade="handleUpgradeClick"
                  />
                </div>
              </div>
            </div>

            <div class="border-t border-gray-100 p-6 lg:border-l lg:border-t-0 lg:p-8 payment-section">
              <div class="space-y-6">
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">选择最适合你的会员类型</h3>
                </div>

                <div class="space-y-3">
                  <button
                    v-for="plan in memberPlans"
                    :key="plan.id"
                    class="w-full rounded-xl border px-4 py-3 text-left transition"
                    :class="selectedPlanId === plan.id ? 'border-blue-400 ring-1 ring-blue-200 bg-blue-50/40' : 'border-gray-200 hover:border-gray-300'"
                    @click="selectedPlanId = plan.id"
                  >
                    <div class="flex items-center justify-between">
                      <div class="flex items-center gap-2 text-sm font-semibold text-gray-900">
                        <span class="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-blue-700">
                          <Crown class="h-4 w-4" />
                        </span>
                        {{ plan.planName }}
                      </div>
                      <div class="text-sm font-semibold">¥{{ plan.price }} / {{ plan.durationMonths }}个月</div>
                    </div>
                    <div class="mt-2 text-xs text-gray-500">
                      {{ plan.description }}
                    </div>
                  </button>
                  
                  <!-- 加载中提示 -->
                  <div v-if="memberPlans.length === 0" class="text-center py-4 text-gray-500 text-sm">
                    <Loader2 class="h-5 w-5 animate-spin mx-auto mb-2" />
                    正在加载套餐...
                  </div>
                </div>

                <div class="space-y-3 text-sm text-gray-700">

                  <div class="text-xs text-gray-500">可使用至 {{ validUntilText }}</div>
                </div>

                <div class="space-y-2 text-sm text-gray-700">
                  <div class="flex items-center justify-between font-medium">
                    <span>支付方式</span>
                    <div class="flex items-center gap-2 text-blue-600">
                      <img :src="alipayLogo" alt="支付宝" class="h-5 w-5 object-contain" />
                      <span class="text-sm text-gray-700">支付宝</span>
                    </div>
                  </div>
                </div>

                <div class="space-y-3 text-sm text-gray-700">
                  <div class="flex items-center justify-between font-medium">
                    <span>费用明细</span>
                    <span class="text-gray-500">
                      {{ selectedPlan?.planName || '请选择套餐' }}
                    </span>
                  </div>
                  <div class="flex items-center justify-between border-t border-gray-100 pt-3 text-base font-semibold">
                    <span>合计</span>
                    <span class="text-gray-900">
                      ¥{{ selectedPlan?.price || '0.00' }}
                    </span>
                  </div>
                </div>

                <div class="space-y-3">
                  <Button
                    class="w-full bg-blue-600 text-white hover:bg-blue-700 flex items-center justify-center gap-2"
                    :disabled="!agreed || !selectedPlanId || paymentLoading"
                    @click="confirmPayment"
                  >
                    <Loader2 v-if="paymentLoading" class="h-4 w-4 animate-spin" />
                    {{ paymentLoading ? '处理中...' : '确认续费' }}
                  </Button>
                  <label class="flex items-center gap-2 text-xs text-gray-500">
                    <input v-model="agreed" type="checkbox" class="h-4 w-4 rounded border-gray-300" />
                    同意《会员服务协议》
                  </label>
                  
                  <!-- 支付中状态提示 -->
                  <div v-if="currentOrderNo && !paymentLoading" class="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
                    <div class="flex items-center gap-2 text-sm text-blue-700">
                      <Loader2 class="h-4 w-4 animate-spin" />
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
          </div>
        </div>
      </div>
    </Transition>
    
    <!-- 支付成功弹窗 -->
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="showSuccessModal"
        class="fixed inset-0 z-[60] flex items-center justify-center p-4"
      >
        <div class="absolute inset-0 bg-black/50" @click="closeSuccessModal"></div>
        <div class="relative bg-white rounded-xl shadow-2xl w-full max-w-sm overflow-hidden" @click.stop>
          <div class="p-8 text-center">
            <div class="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <CheckCircle class="h-10 w-10 text-green-500" />
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-2">支付成功</h3>
            <p class="text-gray-600 mb-6">恭喜您已成功开通会员！</p>
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
            <button 
              @click="closeSuccessModal"
              class="w-full px-4 py-2.5 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors"
            >
              开始使用
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
