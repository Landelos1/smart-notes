<template>
  <div class="art-card h-105 p-4 box-border mb-5 max-sm:mb-4">
    <ArtBarChart
      class="box-border p-2"
      barWidth="50%"
      height="13.7rem"
      :showAxisLine="false"
      :data="chartData"
      :xAxisData="xAxisLabels"
    />
    <div class="ml-1">
      <h3 class="mt-5 text-lg font-medium">新增用户</h3>
      <p class="mt-1 text-sm">本月新增 <span class="text-success font-medium">+{{ monthNew }}</span></p>
      <p class="mt-1 text-sm">观察拉新趋势与核心转化入口效果（注册 / 邀请 / 分享）</p>
    </div>
    <div class="flex-b mt-2">
      <div class="flex-1" v-for="(item, index) in list" :key="index">
        <p class="text-2xl text-g-900">{{ item.num }}</p>
        <p class="text-xs text-g-500">{{ item.name }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { fetchUserGrowth } from '@/api/dashboard'

  interface UserStatItem {
    name: string
    num: string
  }

  const xAxisLabels = ref<string[]>([])
  const chartData = ref<number[]>([])
  const monthNew = ref<number>(0)
  const weekNew = ref<number>(0)

  const list = computed<UserStatItem[]>(() => [
    { name: '本月新增', num: `${monthNew.value}` },
    { name: '本周新增', num: `${weekNew.value}` }
  ])

  const formatLabel = (period: string) => {
    const parts = period.split('-')
    if (parts.length !== 2) return period
    return `${parts[1]}月`
  }

  const loadGrowth = async () => {
    const data = await fetchUserGrowth()
    const periods = data.periods ?? []
    const counts = data.counts ?? []
    xAxisLabels.value = periods.map((period) => formatLabel(period))
    chartData.value = counts
    monthNew.value = data.monthNew ?? 0
    weekNew.value = data.weekNew ?? 0
  }

  onMounted(() => {
    void loadGrowth()
  })
</script>
