<template>
  <div class="art-card p-5 h-100 overflow-hidden mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>会员与订单</h4>
        <p>本月收入<span class="text-success">{{ monthAmountText }}</span></p>
      </div>
    </div>

    <ArtLineChart
      :xAxisData="months"
      :showAxisLabel="false"
      :showAxisLine="false"
      :showSplitLine="false"
      :showAreaColor="true"
      :showLegend="true"
      :data="chartData"
      height="8rem"
    />

    <div class="mt-2 grid grid-cols-6 gap-1 text-xs text-g-500">
      <span v-for="m in months" :key="m" class="text-center">{{ m }}</span>
    </div>

    <div class="mt-4 space-y-3">
      <div v-for="item in orderList" :key="item.title" class="flex-c">
        <div class="size-9 flex-cc rounded-lg" :class="item.iconBgClass">
          <ArtSvgIcon :icon="item.icon" class="text-lg" />
        </div>
        <div class="ml-2">
          <p class="text-xs font-medium text-g-800">{{ item.title }}</p>
          <span class="text-xs text-g-600">{{ item.subtitle }}</span>
        </div>
        <div class="ml-auto px-2 py-1 text-xs text-center rounded" :class="item.valueBgClass">
          <span>+{{ item.value }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { fetchOrderStats } from '@/api/dashboard'
  import type { LineDataItem } from '@/types/component/chart'

  /**
   * 新用户表格数据
   * 包含用户基本信息和完成进度
   */
  const months = ref<string[]>([])

  const AREA_STYLE_CONFIG = {
    startOpacity: 0.08,
    endOpacity: 0
  } as const

  const monthlySeries = ref<number[]>([])
  const quarterlySeries = ref<number[]>([])
  const yearlySeries = ref<number[]>([])
  const monthAmount = ref<number>(0)

  const chartData = computed<LineDataItem[]>(() => [
    {
      name: '月度会员',
      data: monthlySeries.value,
      areaStyle: AREA_STYLE_CONFIG,
      color: '#B25F22'
    },
    {
      name: '季度会员',
      data: quarterlySeries.value,
      areaStyle: AREA_STYLE_CONFIG,
      color: '#E6A23C'
    },
    {
      name: '年度会员',
      data: yearlySeries.value,
      areaStyle: AREA_STYLE_CONFIG,
      color: '#67C23A'
    }
  ])

  const monthAmountText = computed(() => (monthAmount.value ? `¥${monthAmount.value}` : '--'))

  const sumArray = (arr: number[]) => arr.reduce((sum, val) => sum + val, 0)
  const monthlyOrders = computed(() => sumArray(monthlySeries.value))
  const quarterlyOrders = computed(() => sumArray(quarterlySeries.value))
  const yearlyOrders = computed(() => sumArray(yearlySeries.value))

  const formatLabel = (period: string) => {
    const parts = period.split('-')
    if (parts.length !== 2) return period
    return `${parts[1]}月`
  }

  const loadOrderStats = async () => {
    try {
      const stats = await fetchOrderStats()
      const periods = stats.periods ?? []
      months.value = periods.map((period) => formatLabel(period))
      monthlySeries.value = stats.monthlyCounts ?? []
      quarterlySeries.value = stats.quarterlyCounts ?? []
      yearlySeries.value = stats.yearlyCounts ?? []
      monthAmount.value = stats.monthAmount ?? 0
    } catch (error) {
      console.warn('Order stats load failed:', error)
    }
  }

  const orderList = computed(() => [
    {
      icon: 'ri:shopping-bag-3-line',
      title: '月度会员',
      subtitle: '订单数量',
      value: `${monthlyOrders.value}单`,
      iconBgClass: 'bg-theme/12 text-theme',
      valueBgClass: 'bg-theme/12 text-theme'
    },
    {
      icon: 'ri:calendar-2-line',
      title: '季度会员',
      subtitle: '订单数量',
      value: `${quarterlyOrders.value}单`,
      iconBgClass: 'bg-warning/12 text-warning',
      valueBgClass: 'bg-warning/12 text-warning'
    },
    {
      icon: 'ri:calendar-check-line',
      title: '年度会员',
      subtitle: '订单数量',
      value: `${yearlyOrders.value}单`,
      iconBgClass: 'bg-success/12 text-success',
      valueBgClass: 'bg-success/12 text-success'
    }
  ])

  onMounted(() => {
    void loadOrderStats()
  })
</script>

<style lang="scss" scoped>
  .art-card {
    :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
      color: var(--el-color-primary) !important;
      background: transparent !important;
    }
  }
</style>
