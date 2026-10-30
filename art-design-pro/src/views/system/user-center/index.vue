<!-- 个人中心页面 -->
<template>
  <div class="w-full h-full p-0 bg-transparent border-none shadow-none">
    <div class="relative flex-b mt-2.5 max-md:block max-md:mt-1">
      <div class="w-112 mr-5 max-md:w-full max-md:mr-0">
        <div class="art-card-sm relative p-9 pb-6 overflow-hidden text-center">
          <img class="absolute top-0 left-0 w-full h-50 object-cover" src="@imgs/user/bg.webp" />
          <img
            class="relative z-10 w-20 h-20 mt-30 mx-auto object-cover border-2 border-white rounded-full"
            src="@imgs/user/avatar.webp"
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
                <ElInput v-model="form.phone" :disabled="!isEdit" />
              </ElFormItem>
            </ElRow>

            <ElRow>
              <ElFormItem label="真实姓名" prop="realName">
                <ElInput v-model="form.realName" :disabled="!isEdit" />
              </ElFormItem>
              <ElFormItem label="身份证号" prop="idCard" class="ml-5">
                <ElInput v-model="form.idCard" :disabled="!isEdit" />
              </ElFormItem>
            </ElRow>

            <ElRow>
              <ElFormItem label="性别" prop="gender">
                <ElSelect v-model="form.gender" placeholder="Select" :disabled="!isEdit">
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
                  placeholder="选择日期"
                  :disabled="!isEdit"
                  class="w-full"
                />
              </ElFormItem>
            </ElRow>

            <div class="flex-c justify-end [&_.el-button]:!w-27.5">
              <ElButton type="primary" class="w-22.5" v-ripple @click="edit">
                {{ isEdit ? '保存' : '编辑' }}
              </ElButton>
            </div>
          </ElForm>
        </div>

        <div class="art-card-sm my-5">
          <h1 class="p-4 text-xl font-normal border-b border-g-300">更改密码</h1>

          <ElForm :model="pwdForm" class="box-border p-5" label-width="86px" label-position="top">
            <ElFormItem label="当前密码" prop="password">
              <ElInput
                v-model="pwdForm.password"
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
              <ElButton type="primary" class="w-22.5" v-ripple @click="editPwd">
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
  import type { FormInstance, FormRules } from 'element-plus'

  defineOptions({ name: 'UserCenter' })

  const userStore = useUserStore()
  const userInfo = computed(() => userStore.getUserInfo)

  const isEdit = ref(false)
  const isEditPwd = ref(false)
  const date = ref('')
  const ruleFormRef = ref<FormInstance>()

  /**
   * 用户信息表单
   */
  const form = reactive({
    userName: userInfo.value.userName || 'admin',
    phone: userInfo.value.phone || '18888888888',
    realName: '张三',
    idCard: '440301199001011234',
    gender: '1',
    birthDate: '1990-01-01',
    status: 1,
    lastLoginTime: '2026-01-14 10:18:00'
  })

  /**
   * 密码修改表单
   */
  const pwdForm = reactive({
    password: '123456',
    newPassword: '123456',
    confirmPassword: '123456'
  })

  /**
   * 表单验证规则
   */
  const rules = reactive<FormRules>({
    userName: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
    realName: [
      { required: true, message: '请输入真实姓名', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
    gender: [{ required: true, message: '请选择性别', trigger: 'blur' }],
    birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }]
  })

  /**
   * 性别选项
   */
  const genderOptions = [
    { value: '1', label: '男' },
    { value: '2', label: '女' },
    { value: '0', label: '未知' }
  ]

  const roleTypeLabel = computed(() => {
    const roleType = userInfo.value.roleType
    if (roleType === 1) return '超级管理员'
    if (roleType === 2) return '管理员'
    return '普通用户'
  })

  const statusLabel = computed(() => (form.status === 1 ? '启用' : '禁用'))

  const formatDate = (value?: string) => {
    if (!value) return '-'
    return value
  }

  onMounted(() => {
    getDate()
  })

  /**
   * 根据当前时间获取问候语
   */
  const getDate = () => {
    const h = new Date().getHours()

    if (h >= 6 && h < 9) date.value = '早上好'
    else if (h >= 9 && h < 11) date.value = '上午好'
    else if (h >= 11 && h < 13) date.value = '中午好'
    else if (h >= 13 && h < 18) date.value = '下午好'
    else if (h >= 18 && h < 24) date.value = '晚上好'
    else date.value = '很晚了，早点睡'
  }

  /**
   * 切换用户信息编辑状态
   */
  const edit = () => {
    isEdit.value = !isEdit.value
  }

  /**
   * 切换密码编辑状态
   */
  const editPwd = () => {
    isEditPwd.value = !isEditPwd.value
  }
</script>
