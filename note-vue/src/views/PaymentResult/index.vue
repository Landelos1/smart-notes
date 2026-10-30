<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircle, XCircle, Loader2, Crown } from 'lucide-vue-next'
import { syncOrderStatus } from '@/api/payment'
import { getUserInfo } from '@/api/auth'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const paymentSuccess = ref(false)
const orderInfo = ref<any>(null)
const errorMessage = ref('')

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

async function checkOrderStatus() {
  loading.value = true
  errorMessage.value = ''
  const orderNo = route.query.out_trade_no as string
  
  if (!orderNo) {
    loading.value = false
    errorMessage.value = '订单信息不存在'
    return
  }
  
  try {
    const res = await syncOrderStatus(orderNo)
    if (res.code === 0 && res.data) {
      orderInfo.value = res.data
      paymentSuccess.value = res.data.status === 1
      if (paymentSuccess.value) {
        refreshUserInfo()
      }
    } else {
      errorMessage.value = res.message || '查询订单失败'
    }
  } catch (e) {
    console.error('查询订单失败', e)
    errorMessage.value = '查询订单失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function goToMemberCenter() {
  router.push('/member-system')
}

function goHome() {
  router.push('/')
}

onMounted(() => {
  checkOrderStatus()
})
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex items-center justify-center p-4">
    <div class="bg-white rounded-xl shadow-lg w-full max-w-md overflow-hidden">
      <!-- 加载中 -->
      <div v-if="loading" class="p-12 text-center">
        <Loader2 class="h-12 w-12 text-blue-600 animate-spin mx-auto mb-4" />
        <p class="text-gray-600">正在查询支付结果...</p>
      </div>
      
      <!-- 支付成功 -->
      <div v-else-if="paymentSuccess" class="p-8 text-center">
        <div class="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <CheckCircle class="h-10 w-10 text-green-500" />
        </div>
        
        <h2 class="text-2xl font-bold text-gray-900 mb-2">支付成功</h2>
        <p class="text-gray-600 mb-6">恭喜您已成功升级为专业版会员！</p>
        
        <!-- 订单信息 -->
        <div v-if="orderInfo" class="bg-gray-50 rounded-lg p-4 mb-6 text-left">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">订单编号</span>
            <span class="font-medium text-gray-900 text-sm">{{ orderInfo.orderNo }}</span>
          </div>
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">套餐名称</span>
            <span class="font-medium text-gray-900">{{ orderInfo.planName }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-gray-600">支付金额</span>
            <span class="font-bold text-green-600">¥{{ orderInfo.amount }}</span>
          </div>
        </div>
        
        <!-- 会员权益 -->
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
        
        <div class="flex gap-3">
          <button 
            @click="goHome"
            class="flex-1 px-4 py-2.5 border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-100 transition-colors"
          >
            返回首页
          </button>
          <button 
            @click="goToMemberCenter"
            class="flex-1 px-4 py-2.5 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors"
          >
            会员中心
          </button>
        </div>
      </div>
      
      <!-- 支付失败/待支付 -->
      <div v-else class="p-8 text-center">
        <div class="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <XCircle class="h-10 w-10 text-red-500" />
        </div>
        
        <h2 class="text-2xl font-bold text-gray-900 mb-2">
          {{ errorMessage || '支付未完成' }}
        </h2>
        <p class="text-gray-600 mb-6">
          {{ orderInfo?.status === 0 ? '订单等待支付确认。如果已付款，请稍后刷新查询。' : '请返回会员中心查看订单' }}
        </p>
        
        <!-- 订单信息 -->
        <div v-if="orderInfo" class="bg-gray-50 rounded-lg p-4 mb-6 text-left">
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">订单编号</span>
            <span class="font-medium text-gray-900 text-sm">{{ orderInfo.orderNo }}</span>
          </div>
          <div class="flex items-center justify-between mb-2">
            <span class="text-gray-600">订单状态</span>
            <span class="font-medium text-orange-600">{{ orderInfo.statusName }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-gray-600">订单金额</span>
            <span class="font-bold text-gray-900">¥{{ orderInfo.amount }}</span>
          </div>
        </div>
        
        <div class="flex gap-3">
          <button v-if="orderInfo?.status === 0" @click="checkOrderStatus" class="px-4 py-2.5 border border-gray-300 rounded-lg">刷新状态</button>
          <button 
            @click="goHome"
            class="flex-1 px-4 py-2.5 border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-100 transition-colors"
          >
            返回首页
          </button>
          <button 
            @click="goToMemberCenter"
            class="flex-1 px-4 py-2.5 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
          >
            会员中心
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
