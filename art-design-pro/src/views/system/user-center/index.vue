<!-- 个人中心页面 -->
<template>
  <div class="w-full h-full p-0 bg-transparent border-none shadow-none">
    <div class="relative flex-b mt-2.5 max-md:block max-md:mt-1">
      <div class="w-112 mr-5 max-md:w-full max-md:mr-0">
        <div class="art-card-sm relative p-9 pb-6 overflow-hidden text-center">
          <img class="absolute top-0 left-0 w-full h-50 object-cover" src="@imgs/user/bg.webp" />
          <img
            class="relative z-10 w-20 h-20 mt-30 mx-auto object-cover border-2 border-white rounded-full"
            :src="avatarUrl || defaultAvatar" @error="avatarUrl = defaultAvatar"
          />
          <h2 class="mt-5 text-xl font-normal">{{ userInfo.userName }}</h2>
          <p class="mt-5 text-sm">{{ form.realName || '未填写真实姓名' }}</p>

          <div class="w-75 mx-auto mt-7.5 text-left">
            <div class="mt-2.5">
              <ArtSvgIcon icon="ri:smartphone-line" class="text-g-700" />
              <span class="ml-2 text-sm">{{ form.phone || '-' }}</span>
            </div>
            <div class="mt-2.5">
              <ArtSvgIcon icon="ri:user-3-line" class="text-g-700" />
              <span class="ml-2 text-sm">{{ roleTypeLabel }}</span>
            </div>
            <div class="mt-2.5">
              <ArtSvgIcon icon="ri:shield-user-line" class="text-g-700" />
              <span class="ml-2 text-sm">{{ statusLabel }}</span>
            </div>
            <div class="mt-2.5">
              <ArtSvgIcon icon="ri:time-line" class="text-g-700" />
              <span class="ml-2 text-sm">{{ formatDate(form.lastLoginTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="flex-1 overflow-hidden max-md:w-full max-md:mt-3.5">
        <div class="art-card-sm">
          <h1 class="p-4 text-xl font-normal border-b border-g-300">基本设置</h1>

          <ElForm
            :model="form"
            class="box-border p-5 [&>.el-row_.el-form-item]:w-[calc(50%-10px)] [&>.el-row_.el-input]:w-full [&>.el-row_.el-select]:w-full"
            ref="ruleFormRef"
            :rules="rules"
            label-width="86px"
            label-position="top"
          >
            <ElRow>
              <ElFormItem label="用户名" prop="userName">
                <ElInput v-model="form.userName" :disabled="!isEdit" />
              </ElFormItem>
              <ElFormItem label="手机号" prop="phone" class="ml-5">
                <ElInput v-model="form.phone" disabled />
              </ElFormItem>
            </ElRow>

            <ElRow>
              <ElFormItem label="真实姓名" prop="realName">
                <ElInput v-model="form.realName" :disabled="!isEdit" />
              </ElFormItem>
            </ElRow>

            <ElRow>
              <ElFormItem label="性别" prop="gender">
                <ElSelect v-model="form.gender" placeholder="请选择性别" :disabled="!isEdit">
                  <ElOption
                    v-for="item in genderOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </ElSelect>
              </ElFormItem>
              <ElFormItem label="出生日期" prop="birthDate" class="ml-5">
                <ElDatePicker
                  v-model="form.birthDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :disabled-date="(date: Date) => date.getTime() > Date.now()"
                  placeholder="选择日期"
                  :disabled="!isEdit"
                  class="w-full"
                />
              </ElFormItem>
            </ElRow>

            <div class="flex-c justify-end [&_.el-button]:!w-27.5">
              <ElButton v-if="isEdit" :disabled="saving" @click="cancelEdit">取消</ElButton>
              <ElButton type="primary" :loading="saving" :disabled="loadingProfile" class="w-22.5" v-ripple @click="edit">
                {{ isEdit ? '保存' : '编辑' }}
              </ElButton>
            </div>
          </ElForm>
        </div>

        <div class="art-card-sm my-5">
          <h1 class="p-4 text-xl font-normal border-b border-g-300">更改密码</h1>

          <p class="px-5 pt-4 text-sm text-g-600">修改成功后需重新登录；请勿与其他网站共用密码。</p>
          <ElForm ref="passwordFormRef" :rules="passwordRules" :model="pwdForm" class="box-border p-5" label-width="86px" label-position="top">
            <ElFormItem label="当前密码" prop="currentPassword">
              <ElInput
                v-model="pwdForm.currentPassword"
                type="password"
                :disabled="!isEditPwd"
                show-password
              />
            </ElFormItem>

            <ElFormItem label="新密码" prop="newPassword">
              <ElInput
                v-model="pwdForm.newPassword"
                type="password"
                :disabled="!isEditPwd"
                show-password
              />
            </ElFormItem>

            <ElFormItem label="确认新密码" prop="confirmPassword">
              <ElInput
                v-model="pwdForm.confirmPassword"
                type="password"
                :disabled="!isEditPwd"
                show-password
              />
            </ElFormItem>

            <div class="flex-c justify-end [&_.el-button]:!w-27.5">
              <ElButton v-if="isEditPwd" :disabled="savingPassword" @click="cancelPassword">取消</ElButton>
              <ElButton type="primary" :loading="savingPassword" class="w-22.5" v-ripple @click="editPwd">
                {{ isEditPwd ? '保存' : '编辑' }}
              </ElButton>
            </div>
          </ElForm>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useUserStore } from '@/store/modules/user'
  import { fetchUserProfile, fetchUpdateUserProfile, fetchGetUserInfo, fetchChangePassword } from '@/api/auth'
  import { ElMessage } from 'element-plus'
  import type { FormInstance, FormRules } from 'element-plus'
  import defaultAvatar from '@imgs/user/avatar.webp'

  defineOptions({ name: 'UserCenter' })

  const userStore = useUserStore()
  const userInfo = computed(() => userStore.getUserInfo)
  const avatarUrl = ref('')
  const isEdit = ref(false)
  const isEditPwd = ref(false)
  const saving = ref(false)
  const savingPassword = ref(false)
  const loadingProfile = ref(false)
  const ruleFormRef = ref<FormInstance>()
  const passwordFormRef = ref<FormInstance>()
  const form = reactive({ userName: '', phone: '', realName: '', gender: 0,
    birthDate: '' as string | null, status: 1, lastLoginTime: '' })
  const pwdForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const rules: FormRules = {
    userName: [{ required: true, whitespace: true, message: '请输入用户名', trigger: 'blur' },
      { max: 50, message: '用户名不能超过50字', trigger: 'blur' }],
    realName: [{ max: 50, message: '姓名不能超过50字', trigger: 'blur' }]
  }
  const passwordRules: FormRules = {
    currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
    newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, max: 20, message: '新密码长度为6到20位', trigger: 'blur' }],
    confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' },
      { validator: (_rule, value, callback) => callback(value === pwdForm.newPassword ? undefined : new Error('两次密码不一致')), trigger: 'blur' }]
  }
  const genderOptions = [{ value: 0, label: '未设置' }, { value: 1, label: '男' }, { value: 2, label: '女' }]
  const roleTypeLabel = computed(() => userInfo.value.roleType === 2 ? '管理员' : '普通用户')
  const statusLabel = computed(() => ({ 0: '禁用', 1: '启用', 2: '待审核' })[form.status] || '未知')
  const formatDate = (value?: string) => {
    if (!value) return '-'
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN', { hour12: false })
  }
  const loadProfile = async () => {
    loadingProfile.value = true
    try {
      const profile = await fetchUserProfile()
      Object.assign(form, { userName: profile.username || '', phone: profile.phone || '',
        realName: profile.realName || '', gender: profile.gender ?? 0,
        birthDate: profile.birthDate?.slice(0, 10) || null, status: profile.status ?? 0,
        lastLoginTime: profile.lastLoginTime || '' })
      avatarUrl.value = profile.avatarUrl || ''
    } finally { loadingProfile.value = false }
  }
  const cancelEdit = async () => { isEdit.value = false; await loadProfile() }
  const edit = async () => {
    if (!isEdit.value) { isEdit.value = true; return }
    if (!(await ruleFormRef.value?.validate().catch(() => false))) return
    saving.value = true
    try {
      await fetchUpdateUserProfile({ username: form.userName.trim(), realName: form.realName,
        gender: form.gender, birthDate: form.birthDate || null })
      userStore.setUserInfo(await fetchGetUserInfo())
      isEdit.value = false
      ElMessage.success('个人资料已保存')
      await loadProfile()
    } finally { saving.value = false }
  }
  const cancelPassword = () => {
    isEditPwd.value = false
    Object.assign(pwdForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
    passwordFormRef.value?.clearValidate()
  }
  const editPwd = async () => {
    if (!isEditPwd.value) { isEditPwd.value = true; return }
    if (!(await passwordFormRef.value?.validate().catch(() => false))) return
    savingPassword.value = true
    try {
      await fetchChangePassword({ currentPassword: pwdForm.currentPassword, newPassword: pwdForm.newPassword })
      cancelPassword()
      ElMessage.success('密码已修改，请重新登录')
      userStore.logOut()
    } finally { savingPassword.value = false }
  }
  onMounted(loadProfile)
</script>
