<template>
  <div class="art-card h-100 p-5 mb-5 max-sm:mb-4 flex flex-col">
    <div class="art-card-header">
      <div class="title">
        <h4>社区与知识库动态</h4>
        <p>新增<span class="text-success">+{{ newCount }}</span></p>
      </div>
    </div>

    <div class="flex-1 min-h-0 mt-2">
      <ElScrollbar class="h-full">
        <div
          class="h-17.5 leading-17.5 border-b border-g-300 text-sm overflow-hidden last:border-b-0"
          v-for="(item, index) in list"
          :key="index"
        >
          <span class="text-g-800 font-medium">{{ item.username }}</span>
          <span class="mx-2 text-g-600">{{ item.type }}</span>
          <span class="text-theme">{{ item.target }}</span>
        </div>
      </ElScrollbar>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue'
  import { fetchDashboardDynamics } from '@/api/dashboard'

  interface DynamicItem {
    username: string
    type: string
    target: string
  }

  const list = ref<DynamicItem[]>([])
  const newCount = ref<number>(0)

  const loadDynamics = async () => {
    const data = await fetchDashboardDynamics()
    list.value = data.list ?? []
    newCount.value = data.newCount ?? 0
  }

  onMounted(() => {
    void loadDynamics()
  })
</script>
