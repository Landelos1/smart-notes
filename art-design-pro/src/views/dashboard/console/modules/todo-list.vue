<template>
  <div class="art-card h-100 p-5 mb-5 max-sm:mb-4">
    <div class="art-card-header">
      <div class="title">
        <h4>社区与分发（笔记广场）</h4>
      </div>
    </div>
    <ArtLineChart
      height="calc(100% - 30px)"
      :data="chartData"
      :xAxisData="xAxisData"
      :showLegend="true"
      :showAxisLabel="true"
      :showAxisLine="false"
      :showSplitLine="true"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { fetchNoteSquareStats } from '@/api/dashboard'
  import type { LineDataItem } from '@/types/component/chart'

  /**
   * 待办事项列表
   * 记录每日工作任务及完成状态
   */
  const AREA_STYLE_CONFIG = {
    startOpacity: 0.08,
    endOpacity: 0
  } as const

  const xAxisData = ref<string[]>([])
  const publishCounts = ref<number[]>([])
  const interactionCounts = ref<number[]>([])

  const chartData = computed<LineDataItem[]>(() => [
    {
      name: '发布量',
      data: publishCounts.value,
      areaStyle: AREA_STYLE_CONFIG
    },
    {
      name: '互动量',
      data: interactionCounts.value,
      areaStyle: AREA_STYLE_CONFIG
    }
  ])

  const formatLabel = (period: string) => {
    const parts = period.split('-')
    if (parts.length !== 3) return period
    return parts[2]
  }

  const loadStats = async () => {
    const data = await fetchNoteSquareStats()
    const periods = data.periods ?? []
    xAxisData.value = periods.map((period) => formatLabel(period))
    publishCounts.value = data.publishCounts ?? []
    interactionCounts.value = data.interactionCounts ?? []
  }

  onMounted(() => {
    void loadStats()
  })
</script>
