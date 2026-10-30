/**
 * 会员配额管理 Composable
 */
import { ref, computed } from 'vue';
import { getUserMembershipQuota, refreshUserQuota } from '@/api/membershipQuota';

const quotaData = ref({
  aiCallQuota: 50,
  aiCallUsed: 0,
  aiCallRemaining: 50,
  nextResetTime: '',
  kbQuota: 3,
  kbUsed: 0,
  kbRemaining: 3,
  isUnlimitedKb: false,
  memberExpireTime: '',
  isActive: false
});

const loading = ref(false);
const error = ref(null);
const lastFetchTime = ref(null);

// 缓存过期时间(5分钟)
const CACHE_EXPIRY_TIME = 5 * 60 * 1000;

export function useMembershipQuota() {
  // 计算是否需要刷新缓存
  const shouldRefresh = computed(() => {
    if (!lastFetchTime.value) return true;
    return Date.now() - lastFetchTime.value > CACHE_EXPIRY_TIME;
  });

  /**
   * 加载配额数据
   * @param {number} userId - 用户ID
   * @param {boolean} forceRefresh - 是否强制刷新
   */
  const fetchQuota = async (userId, forceRefresh = false) => {
    if (!userId) {
      error.value = '用户ID不能为空';
      return;
    }

    // 如果不是强制刷新且缓存未过期,直接返回
    if (!forceRefresh && !shouldRefresh.value) {
      return quotaData.value;
    }

    loading.value = true;
    error.value = null;

    try {
      const response = forceRefresh 
        ? await refreshUserQuota(userId)
        : await getUserMembershipQuota(userId);
      
      if (response && (response.code === 0 || response.code === 200)) {
        quotaData.value = response.data;
        lastFetchTime.value = Date.now();
      } else {
        error.value = response.message || '获取配额信息失败';
      }
    } catch (err) {
      console.error('获取配额信息失败:', err);
      error.value = err.message || '网络请求失败';
    } finally {
      loading.value = false;
    }

    return quotaData.value;
  };

  /**
   * 刷新配额数据
   * @param {number} userId - 用户ID
   */
  const refreshQuota = (userId) => {
    return fetchQuota(userId, true);
  };

  /**
   * 减少AI调用次数(本地立即更新,用于优化用户体验)
   */
  const decrementAIQuota = () => {
    if (quotaData.value.aiCallUsed < quotaData.value.aiCallQuota) {
      quotaData.value.aiCallUsed += 1;
      quotaData.value.aiCallRemaining -= 1;
    }
  };

  /**
   * 增加知识库使用数(本地立即更新)
   */
  const incrementKbUsed = () => {
    quotaData.value.kbUsed += 1;
    if (!quotaData.value.isUnlimitedKb) {
      quotaData.value.kbRemaining = Math.max(0, (quotaData.value.kbQuota || 0) - quotaData.value.kbUsed);
    } else {
      quotaData.value.kbRemaining = -1;
    }
  };

  /**
   * 减少知识库使用数(本地立即更新)
   */
  const decrementKbUsed = () => {
    if (quotaData.value.kbUsed > 0) {
      quotaData.value.kbUsed -= 1;
    }
    if (!quotaData.value.isUnlimitedKb) {
      quotaData.value.kbRemaining = Math.max(0, (quotaData.value.kbQuota || 0) - quotaData.value.kbUsed);
    } else {
      quotaData.value.kbRemaining = -1;
    }
  };

  /**
   * 检查AI配额是否可用
   */
  const checkAIQuotaAvailable = computed(() => {
    return quotaData.value.aiCallRemaining > 0;
  });

  /**
   * 检查知识库配额是否可用
   */
  const checkKbQuotaAvailable = computed(() => {
    return quotaData.value.isUnlimitedKb || quotaData.value.kbRemaining > 0;
  });

  /**
   * 重置本地状态(用于登出等场景)
   */
  const resetQuota = () => {
    quotaData.value = {
      aiCallQuota: 50,
      aiCallUsed: 0,
      aiCallRemaining: 50,
      nextResetTime: '',
      kbQuota: 3,
      kbUsed: 0,
      kbRemaining: 3,
      isUnlimitedKb: false,
      memberExpireTime: '',
      isActive: false
    };
    lastFetchTime.value = null;
    error.value = null;
  };

  return {
    // 状态
    quotaData,
    loading,
    error,
    
    // 计算属性
    checkAIQuotaAvailable,
    checkKbQuotaAvailable,
    
    // 方法
    fetchQuota,
    refreshQuota,
    decrementAIQuota,
    incrementKbUsed,
    decrementKbUsed,
    resetQuota
  };
}
