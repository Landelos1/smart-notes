<template>
  <div class="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
    <div class="flex items-center justify-between gap-3 border-b border-gray-100 pb-3">
      <h3 class="text-base font-semibold text-gray-900">会员配额信息</h3>
      <span class="text-xs text-gray-500">下次重置: {{ quotaData.nextResetTime }}</span>
    </div>

    <div class="mt-4 space-y-4">
      <div class="flex items-start gap-3">
        <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="h-5 w-5">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
          </svg>
        </div>

        <div class="min-w-0 flex-1">
          <div class="flex items-center justify-between gap-3">
            <div class="text-sm font-medium text-gray-900">AI对话次数</div>
            <div class="text-xs text-gray-500">
              <span class="font-semibold text-gray-900">{{ quotaData.aiCallUsed || 0 }}</span>
              <span class="mx-1 text-gray-300">/</span>
              <span>{{ quotaData.aiCallQuota || 0 }}</span>
              <span class="ml-2 text-emerald-600">剩余 {{ quotaData.aiCallRemaining || 0 }}</span>
            </div>
          </div>

          <div class="mt-2 h-2 w-full overflow-hidden rounded-full bg-gray-100">
            <div
              class="h-full rounded-full bg-gradient-to-r from-blue-600 to-blue-400 transition-all duration-300"
              :style="{ width: aiPercentage + '%' }"
            ></div>
          </div>
        </div>
      </div>

      <div class="flex items-start gap-3">
        <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="h-5 w-5">
            <path d="M20 6h-8l-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V6h5.17l2 2H20v10z" />
          </svg>
        </div>

        <div class="min-w-0 flex-1">
          <div class="flex items-center justify-between gap-3">
            <div class="flex items-center gap-2 text-sm font-medium text-gray-900">
              <span>共享知识库</span>
              <span
                v-if="quotaData.isUnlimitedKb"
                class="inline-flex items-center rounded-full bg-amber-50 px-2 py-0.5 text-xs font-semibold text-amber-700 ring-1 ring-inset ring-amber-200"
              >
                无限制
              </span>
            </div>

            <div v-if="!quotaData.isUnlimitedKb" class="text-xs text-gray-500">
              <span class="font-semibold text-gray-900">{{ quotaData.kbUsed || 0 }}</span>
              <span class="mx-1 text-gray-300">/</span>
              <span>{{ quotaData.kbQuota || 0 }}</span>
              <span class="ml-2 text-emerald-600">剩余 {{ quotaData.kbRemaining || 0 }}</span>
            </div>
          </div>

          <div v-if="!quotaData.isUnlimitedKb" class="mt-2 h-2 w-full overflow-hidden rounded-full bg-gray-100">
            <div
              class="h-full rounded-full bg-gradient-to-r from-indigo-600 to-blue-400 transition-all duration-300"
              :style="{ width: kbPercentage + '%' }"
            ></div>
          </div>

          <div v-else class="mt-2 text-sm text-gray-600">已创建: {{ quotaData.kbUsed || 0 }} 个知识库</div>
        </div>
      </div>
    </div>

    <div v-if="showUpgrade && !quotaData.isUnlimitedKb" class="mt-5 border-t border-gray-100 pt-4">
      <Button class="w-full bg-blue-600 text-white hover:bg-blue-700" @click="handleUpgrade">
        升级会员获取更多配额
      </Button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import Button from '@/components/ui/Button.vue'

const props = defineProps({
  quotaData: {
    type: Object,
    default: () => ({
      aiCallQuota: 50,
      aiCallUsed: 0,
      aiCallRemaining: 50,
      nextResetTime: '',
      kbQuota: 3,
      kbUsed: 0,
      kbRemaining: 3,
      isUnlimitedKb: false
    })
  },
  showUpgrade: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['upgrade']);

// 计算AI对话使用百分比
const aiPercentage = computed(() => {
  if (!props.quotaData.aiCallQuota || props.quotaData.aiCallQuota === 0) return 0;
  return Math.min(100, (props.quotaData.aiCallUsed / props.quotaData.aiCallQuota) * 100);
});

// 计算知识库使用百分比
const kbPercentage = computed(() => {
  if (props.quotaData.isUnlimitedKb) return 0;
  if (!props.quotaData.kbQuota || props.quotaData.kbQuota === 0) return 0;
  return Math.min(100, (props.quotaData.kbUsed / props.quotaData.kbQuota) * 100);
});

// 处理升级按钮点击
const handleUpgrade = () => {
  emit('upgrade');
};
</script>
