<template>
  <div class="quota-badge" :class="{ 'quota-warning': isLowQuota, 'quota-critical': isCritical }">
    <div class="quota-icon">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z"/>
        <path d="M12 6v6l4 2"/>
      </svg>
    </div>
    <div class="quota-text">
      <span class="quota-label">AI对话剩余</span>
      <span class="quota-count">{{ remaining }}</span>
      <span class="quota-total">/ {{ total }}</span>
    </div>
    <button 
      v-if="showUpgrade && !isUnlimited" 
      @click="$emit('upgrade')"
      class="quota-upgrade-btn"
    >
      升级
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  remaining: {
    type: Number,
    default: 0
  },
  total: {
    type: Number,
    default: 50
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

// 低配额警告 (剩余 < 20%)
const isLowQuota = computed(() => {
  if (props.isUnlimited) return false;
  return props.remaining < props.total * 0.2;
});

// 危险配额 (剩余 < 10%)
const isCritical = computed(() => {
  if (props.isUnlimited) return false;
  return props.remaining < props.total * 0.1;
});
</script>

<style scoped>
.quota-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #fbf0e5;
  border: 1px solid #edc49f;
  border-radius: 20px;
  font-size: 13px;
  transition: all 0.3s ease;
}

.quota-warning {
  background: #fdf1de;
  border-color: #e8c088;
}

.quota-critical {
  background: #fbf0ee;
  border-color: #e3c4bf;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
}

.quota-icon {
  width: 16px;
  height: 16px;
  color: #b25f22;
  flex-shrink: 0;
}

.quota-warning .quota-icon {
  color: #c98a2e;
}

.quota-critical .quota-icon {
  color: #a34238;
}

.quota-text {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.quota-label {
  color: #8b745c;
  font-size: 12px;
}

.quota-count {
  font-weight: 600;
  color: #b25f22;
  font-size: 15px;
}

.quota-warning .quota-count {
  color: #c98a2e;
}

.quota-critical .quota-count {
  color: #a34238;
}

.quota-total {
  color: #a9957f;
  font-size: 12px;
}

.quota-upgrade-btn {
  padding: 2px 10px;
  background: linear-gradient(135deg, #b25f22, #8e4a1b);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quota-upgrade-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(178, 95, 34, 0.3);
}

.quota-upgrade-btn:active {
  transform: translateY(0);
}
</style>
