<template>
  <div class="kb-quota-info" :class="{ 'kb-quota-warning': isWarning, 'kb-quota-unlimited': isUnlimited }">
    <div class="kb-quota-icon">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
        <path d="M20 6h-8l-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V6h5.17l2 2H20v10z"/>
      </svg>
    </div>
    <div class="kb-quota-content">
      <template v-if="isUnlimited">
        <span class="kb-quota-text">已创建 {{ used }} 个知识库</span>
        <span class="kb-quota-badge unlimited">无限制</span>
      </template>
      <template v-else>
        <span class="kb-quota-text">
          已创建 <strong>{{ used }}</strong> / {{ total }} 个知识库
        </span>
        <span v-if="remaining > 0" class="kb-quota-remaining">
          (还可创建 {{ remaining }} 个)
        </span>
        <span v-else class="kb-quota-limit">已达上限</span>
      </template>
    </div>
    <button 
      v-if="showUpgrade && !isUnlimited" 
      @click="$emit('upgrade')"
      class="kb-upgrade-btn"
      :class="{ 'upgrade-highlight': remaining === 0 }"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="upgrade-icon">
        <path d="M12 2L2 7v10c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-10-5z"/>
        <path d="M10 17l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
      </svg>
      升级获取更多
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  used: {
    type: Number,
    default: 0
  },
  total: {
    type: Number,
    default: 3
  },
  isUnlimited: {
    type: Boolean,
    default: false
  },
  showUpgrade: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['upgrade']);

const remaining = computed(() => {
  if (props.isUnlimited) return -1;
  return Math.max(0, props.total - props.used);
});

const isWarning = computed(() => {
  if (props.isUnlimited) return false;
  return remaining.value <= 1;
});
</script>

<style scoped>
.kb-quota-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fbf7f0;
  border: 1px solid #e7dcc9;
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.kb-quota-warning {
  background: #fef3c7;
  border-color: #fbbf24;
}

.kb-quota-unlimited {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-color: #f59e0b;
}

.kb-quota-icon {
  width: 24px;
  height: 24px;
  color: #8b745c;
  flex-shrink: 0;
}

.kb-quota-warning .kb-quota-icon {
  color: #f59e0b;
}

.kb-quota-unlimited .kb-quota-icon {
  color: #d97706;
}

.kb-quota-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.kb-quota-text {
  color: #6d5742;
}

.kb-quota-text strong {
  font-weight: 600;
  color: #241b14;
}

.kb-quota-remaining {
  color: #5c6e3b;
  font-size: 13px;
}

.kb-quota-limit {
  color: #a34238;
  font-weight: 500;
  font-size: 13px;
}

.kb-quota-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.kb-quota-badge.unlimited {
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  color: #92400e;
}

.kb-upgrade-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #b25f22, #8e4a1b);
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.kb-upgrade-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(178, 95, 34, 0.3);
}

.kb-upgrade-btn:active {
  transform: translateY(0);
}

.kb-upgrade-btn.upgrade-highlight {
  background: linear-gradient(135deg, #b4544a, #a34238);
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.upgrade-icon {
  width: 14px;
  height: 14px;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .kb-quota-info {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  
  .kb-upgrade-btn {
    justify-content: center;
  }
}
</style>
