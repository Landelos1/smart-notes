<template>
  <ElRow :gutter="20" class="flex">
    <ElCol v-for="(item, index) in dataList" :key="index" :sm="12" :md="6" :lg="6">
      <div class="art-card relative flex flex-col justify-center h-35 px-5 mb-5 max-sm:mb-4">
        <span class="text-g-700 text-sm">{{ item.des }}</span>
        <ArtCountTo class="text-[26px] font-medium mt-2" :target="item.num" :duration="1300" />
        <div class="flex-c mt-1">
          <span class="text-xs text-g-600">较上周</span>
          <span
            class="ml-1 text-xs font-semibold"
            :class="[item.change.indexOf('+') === -1 ? 'text-danger' : 'text-success']"
          >
            {{ item.change }}
          </span>
        </div>
        <div
          class="absolute top-0 bottom-0 right-5 m-auto size-12.5 rounded-xl flex-cc bg-theme/10"
        >
          <ArtSvgIcon :icon="item.icon" class="text-xl text-theme" />
        </div>
      </div>
    </ElCol>
  </ElRow>
</template>

<script setup lang="ts">
  import { onMounted, reactive } from 'vue'
  import { fetchDashboardOverview } from '@/api/dashboard'
  interface CardDataItem {
    des: string
    icon: string
    startVal: number
    duration: number
    num: number
    change: string
  }

  /**
   * 卡片统计数据列表
   * 展示总访问次数、在线访客数、点击量和新用户等核心数据指标
   */
  const dataList = reactive<CardDataItem[]>([
    {
      des: '活跃用户（DAU）',
      icon: 'ri:user-heart-line',
      startVal: 0,
      duration: 1000,
      num: 1820,
      change: '+8%'
    },
    {
      des: '新增笔记（今日）',
      icon: 'ri:file-list-2-line',
      startVal: 0,
      duration: 1000,
      num: 356,
      change: '+12%'
    },
    {
      des: '请求次数（今日）',
      icon: 'ri:flashlight-line',
      startVal: 0,
      duration: 1000,
      num: 128000,
      change: '+18%'
    },
    {
      des: '付费订单（今日）',
      icon: 'ri:shopping-bag-3-line',
      startVal: 0,
      duration: 1000,
      num: 42,
      change: '+5%'
    }
  ])

  const loadOverview = async () => {
    try {
      const overview = await fetchDashboardOverview()
      if (!overview) return
      if (dataList[0]) {
        dataList[0].num = overview.activeUsers ?? 0
        dataList[0].change = overview.activeUsersChange ?? '0%'
      }
      if (dataList[1]) {
        dataList[1].num = overview.newNotes ?? 0
        dataList[1].change = overview.newNotesChange ?? '0%'
      }
      if (dataList[2]) {
        dataList[2].num = overview.requestCount ?? 0
        dataList[2].change = overview.requestCountChange ?? '0%'
      }
      if (dataList[3]) {
        dataList[3].num = overview.paidOrders ?? 0
        dataList[3].change = overview.paidOrdersChange ?? '0%'
      }
    } catch (error) {
      console.warn('Dashboard overview load failed:', error)
    }
  }

  onMounted(() => {
    void loadOverview()
  })
</script>
