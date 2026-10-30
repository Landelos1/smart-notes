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
        @click="handleClose"
      >
        <div class="absolute inset-0 bg-black/50"></div>
        <div
          class="relative w-full max-w-md overflow-hidden rounded-2xl bg-white shadow-2xl"
          @click.stop
        >
          <!-- 头部 -->
          <div class="p-6 pb-4">
            <div class="flex items-start gap-4">
              <div class="flex-shrink-0">
                <div class="flex h-12 w-12 items-center justify-center rounded-full bg-amber-100">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </div>
              </div>
              <div class="flex-1">
                <h3 class="text-lg font-semibold text-gray-900">{{ title }}</h3>
                <p class="mt-1 text-sm text-gray-600">{{ message }}</p>
              </div>
              <button
                @click="handleClose"
                class="flex-shrink-0 rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 配额信息 -->
          <div v-if="showQuotaInfo" class="px-6 pb-4">
            <div class="rounded-lg bg-gray-50 p-4">
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-600">当前配额</span>
                <span class="text-sm font-medium text-gray-900">
                  {{ quotaUsed }} / {{ quotaTotal }}
                </span>
              </div>
              <div class="mt-2">
                <div class="h-2 w-full overflow-hidden rounded-full bg-gray-200">
                  <div 
                    class="h-full bg-gradient-to-r from-amber-400 to-amber-600 transition-all"
                    :style="{ width: quotaPercentage + '%' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 建议 -->
          <div class="px-6 pb-4">
            <p class="text-sm text-gray-700">{{ suggestion }}</p>
          </div>

          <!-- 操作按钮 -->
          <div class="flex gap-3 bg-gray-50 px-6 py-4">
            <button
              @click="handleClose"
              class="flex-1 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              稍后再说
            </button>
            <button
              @click="handleUpgrade"
              class="flex-1 rounded-lg bg-gradient-to-r from-purple-600 to-purple-700 px-4 py-2 text-sm font-medium text-white hover:from-purple-700 hover:to-purple-800"
            >
              {{ actionText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '配额已达上限'
  },
  message: {
    type: String,
    default: '您的使用配额已用完'
  },
  suggestion: {
    type: String,
    default: '升级会员可获得更多配额'
  },
  actionText: {
    type: String,
    default: '立即升级'
  },
  quotaUsed: {
    type: Number,
    default: 0
  },
  quotaTotal: {
    type: Number,
    default: 0
  },
  showQuotaInfo: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['close', 'upgrade']);

const quotaPercentage = computed(() => {
  if (props.quotaTotal === 0) return 0;
  return Math.min(100, (props.quotaUsed / props.quotaTotal) * 100);
});

function handleClose() {
  emit('close');
}

function handleUpgrade() {
  emit('upgrade');
  emit('close');
}
</script>

<style scoped>
/* 所有样式已在 Tailwind classes 中定义 */
</style>
