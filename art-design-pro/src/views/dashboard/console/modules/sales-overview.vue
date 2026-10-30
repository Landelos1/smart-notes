<template>
  <div class="art-card h-105 p-5 mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>API Tokens 消耗</h4>
        <p>本月消耗 <span class="text-success">{{ monthTokensText }}</span></p>
      </div>
    </div>
    <ArtLineChart
      height="calc(100% - 56px)"
      :data="data"
      :xAxisData="xAxisData"
      :showAreaColor="true"
      :showAxisLine="false"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { fetchTokenStats } from '@/api/chat'

  const data = ref<number[]>([])
  const xAxisData = ref<string[]>([])
  const monthTokens = ref<number>(0)

  const monthTokensText = computed(() => (monthTokens.value ? `${monthTokens.value}` : '--'))

  const formatTrendLabel = (period: string) => {
    const parts = period.split('-')
    if (parts.length !== 2) return period
    return `${parts[0]}-${parts[1]}`
  }

  const loadTokenStats = async () => {
    try {
      const stats = await fetchTokenStats()
      const trend = stats.monthlyTrend ?? []
      monthTokens.value = stats.monthTokens ?? 0
      data.value = trend.map((item) => item.totalTokens)
      xAxisData.value = trend.map((item) => formatTrendLabel(item.period))
    } catch (error) {
      console.warn('Token stats load failed:', error)
    }
  }

  onMounted(() => {
    void loadTokenStats()
  })
</script>
