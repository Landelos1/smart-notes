<!-- 切换权限页面 -->
<template>
  <div class="py-2">
    <!-- 页面头部 -->
    <div class="mb-6">
      <h2 class="m-0 mb-2 text-xl font-medium">权限切换演示</h2>
      <p class="m-0 leading-[1.6] text-g-700">
        点击下方按钮切换不同用户身份，模拟不同用户登录系统的效果。切换后会影响整个系统的菜单显示和按钮权限。
      </p>
    </div>

    <!-- 当前用户信息 -->
    <div class="mb-6">
      <ElCard shadow="never" class="art-card-xs">
        <template #header>
          <div class="font-semibold">
            <span>当前登录用户</span>
          </div>
        </template>
        <div>
          <div>
            <div class="flex items-start mb-3 last:mb-0">
              <span class="min-w-30 font-semibold">用户名：</span>
              <span>{{ currentUser.userName || '未登录' }}</span>
            </div>
            <div class="flex items-start mb-3 last:mb-0">
              <span class="min-w-30 font-semibold">角色：</span>
              <ElTag :type="getRoleTagType(currentUser.roles?.[0])">
                {{ getRoleDisplayName(currentUser.roles?.[0]) }}
              </ElTag>
            </div>
            <div class="flex items-start mb-3 last:mb-0">
              <span class="min-w-30 font-semibold">权限码：</span>
              <div class="flex flex-wrap gap-2">
                <ElTag v-for="button in currentUser.buttons" :key="button" size="small" type="info">
                  {{ button }}
                </ElTag>
                <span v-if="!currentUser.buttons?.length" class="italic text-g-500">无权限码</span>
              </div>
            </div>
          </div>
        </div>
      </ElCard>
    </div>

    <!-- 用户角色切换 -->
    <div class="mb-6">
      <ElCard shadow="never" class="art-card-xs">
        <template #header>
          <div class="font-semibold">
            <span>账号切换</span>
          </div>
        </template>
        <div class="grid grid-cols-[repeat(auto-fit,minmax(300px,1fr))] gap-4">
          <div
            v-for="account in accounts"
            :key="account.key"
            class="p-5 border border-g-400 rounded-lg tad-300"
            :class="{
              'bg-theme/12 !border-theme': currentUser.phone === account.phone
            }"
          >
            <div class="mb-4">
              <div>
                <h4 class="m-0 mb-2 text-base font-semibold">{{ account.label }}</h4>
                <p class="m-0 mb-2 leading-[1.5] text-g-700">{{ account.description }}</p>
                <div class="flex flex-col gap-1">
                  <span class="text-xs text-g-600">手机号: {{ account.phone }}</span>
                  <span class="text-xs text-g-600">角色: {{ account.roles.join(', ') }}</span>
                </div>
              </div>
            </div>
            <div class="text-right">
              <ElButton
                v-if="currentUser.phone !== account.phone"
                type="primary"
                @click="switchRole(account)"
                :loading="switching"
              >
                切换到此角色
              </ElButton>
              <ElTag v-else type="success">当前用户</ElTag>
            </div>
          </div>
        </div>
      </ElCard>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue'
  import { useUserStore } from '@/store/modules/user'
  import { useI18n } from 'vue-i18n'
  import { fetchGetCaptcha, fetchLogin, fetchGetUserInfo } from '@/api/auth'
  import { ElMessageBox } from 'element-plus'

  defineOptions({ name: 'PermissionSwitchRole' })

  const { t } = useI18n()
  const userStore = useUserStore()

  // 响应式数据
  const switching = ref(false)

  // 当前用户信息
  const currentUser = computed(() => userStore.info)

  // 账号列表 - 与登录页面保持一致
  const accounts = computed(() => [
    {
      key: 'admin',
      label: t('login.roles.admin'),
      phone: '13800138001',
      password: '123456',
      roleType: 2 as const,
      roles: ['R_ADMIN'],
      color: '#B25F22',
      description: '拥有管理权限，可以管理用户和部分系统设置'
    },
    {
      key: 'user',
      label: t('login.roles.user'),
      phone: '13800138002',
      password: '123456',
      roleType: 1 as const,
      roles: ['R_USER'],
      color: '#67C23A',
      description: '普通用户权限，只能访问基础功能模块'
    }
  ])

  // 获取角色标签类型
  const getRoleTagType = (role?: string): 'info' | 'warning' | 'primary' | 'success' | 'danger' => {
    if (!role) return 'info'
    const roleMap: Record<string, 'info' | 'warning' | 'primary' | 'success' | 'danger'> = {
      R_ADMIN: 'primary',
      R_USER: 'success'
    }
    return roleMap[role] || 'info'
  }

  // 获取角色显示名称
  const getRoleDisplayName = (role?: string): string => {
    if (!role) return '未知角色'
    const roleMap: Record<string, string> = {
      R_ADMIN: '管理员',
      R_USER: '普通用户'
    }
    return roleMap[role] || '未知角色'
  }

  /**
   * 账号信息类型
   */
  interface AccountInfo {
    phone: string
    password: string
    roleType: 1 | 2
    roles: string[]
    [key: string]: any
  }

  /**
   * 切换角色
   * @param account 账号信息
   */
  const switchRole = async (account: AccountInfo) => {
    try {
      switching.value = true

      const { captchaId, imageBase64 } = await fetchGetCaptcha()
      const captchaSrc = imageBase64.startsWith('data:image')
        ? imageBase64
        : `data:image/png;base64,${imageBase64}`

      const { value: captcha } = await ElMessageBox.prompt(
        `<div style="display:flex;align-items:center;gap:12px;">
          <img src="${captchaSrc}" style="height:40px;width:120px;object-fit:cover;border-radius:6px;border:1px solid rgba(0,0,0,0.12);" />
          <div style="font-size:12px;color:rgba(0,0,0,0.6);line-height:1.4;">请输入图片验证码</div>
        </div>`,
        '验证码验证',
        {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入验证码'
        }
      )

      // 模拟登录请求
      const { token } = await fetchLogin({
        phone: account.phone,
        password: account.password,
        captcha,
        captchaId,
        roleType: account.roleType
      })

      // 验证token
      if (!token) {
        throw new Error('Login failed - no token received')
      }

      // 存储token和用户信息
      userStore.setToken(token)
      const userInfo = await fetchGetUserInfo()
      userStore.setUserInfo(userInfo)

      // 延迟刷新页面以应用新权限
      setTimeout(() => {
        window.location.reload()
      }, 100)
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('切换用户身份失败，请稍后重试')
        console.error('[SwitchRole] Error:', error)
      }
    } finally {
      switching.value = false
    }
  }
</script>
