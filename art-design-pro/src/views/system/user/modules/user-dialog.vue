<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '添加用户' : '编辑用户'"
    width="30%"
    align-center
  >
    <ElForm ref="formRef" :model="formData" :rules="rules" label-width="80px">
      <ElFormItem label="手机号" prop="phone">
        <ElInput
          v-model="formData.phone"
          placeholder="请输入手机号"
          :disabled="dialogType === 'edit'"
        />
      </ElFormItem>
      <ElFormItem label="用户名" prop="username">
        <ElInput v-model="formData.username" placeholder="请输入用户名" />
      </ElFormItem>
      <ElFormItem label="姓名" prop="realName">
        <ElInput v-model="formData.realName" placeholder="请输入姓名" />
      </ElFormItem>
      <ElFormItem label="密码" prop="password">
        <ElInput
          v-model="formData.password"
          placeholder="新增必填，编辑可留空"
          show-password
          type="password"
        />
      </ElFormItem>
      <ElFormItem label="性别" prop="gender">
        <ElSelect v-model="formData.gender">
          <ElOption label="未设置" :value="0" />
          <ElOption label="男" :value="1" />
          <ElOption label="女" :value="2" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="角色" prop="roleType">
        <ElSelect v-model="formData.roleType">
          <ElOption label="普通用户" :value="1" />
          <ElOption label="管理员" :value="2" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="状态" prop="status">
        <ElSelect v-model="formData.status">
          <ElOption label="启用" :value="1" />
          <ElOption label="禁用" :value="0" />
          <ElOption label="待审核" :value="2" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="会员状态" prop="memberActive">
        <ElSelect v-model="formData.memberActive" clearable placeholder="请选择会员状态">
          <ElOption label="有效" :value="true" />
          <ElOption label="无效" :value="false" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="到期时间" prop="memberExpireTime">
        <ElDatePicker
          v-model="formData.memberExpireTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择到期时间"
          clearable
        />
      </ElFormItem>
    </ElForm>
    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">提交</ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import type { FormInstance, FormRules } from 'element-plus'

  interface Props {
    visible: boolean
    type: string
    userData?: Partial<Api.SystemManage.UserListItem>
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'submit', payload: Api.SystemManage.AdminUserCreateParams | Api.SystemManage.AdminUserUpdateParams): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  // 对话框显示控制
  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const dialogType = computed(() => props.type)

  // 表单实例
  const formRef = ref<FormInstance>()

  // 表单数据
  const formData = reactive({
    phone: '',
    username: '',
    realName: '',
    password: '',
    gender: 0 as 0 | 1 | 2,
    roleType: 1 as 1 | 2,
    status: 1 as 0 | 1 | 2,
    memberActive: undefined as undefined | boolean,
    memberExpireTime: '' as string | null
  })

  // 表单验证规则
  const rules: FormRules = {
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^\d{11}$/, message: '手机号必须为11位数字', trigger: 'blur' }
    ],
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
    ],
    roleType: [{ required: true, message: '请选择角色', trigger: 'change' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }],
    password: [
      {
        validator: (_rule, value: string, callback) => {
          if (dialogType.value === 'add') {
            if (!value) return callback(new Error('请输入密码'))
            if (value.length < 6 || value.length > 20) return callback(new Error('密码长度必须在6-20位'))
            return callback()
          }
          if (!value) return callback()
          if (value.length > 20) return callback(new Error('密码长度不能超过20位'))
          return callback()
        },
        trigger: 'blur'
      }
    ]
  }

  /**
   * 初始化表单数据
   * 根据对话框类型（新增/编辑）填充表单
   */
  const initFormData = () => {
    const isEdit = props.type === 'edit' && props.userData
    const row = props.userData

    Object.assign(formData, {
      phone: isEdit && row ? row.userPhone || '' : '',
      username: isEdit && row ? row.userName || '' : '',
      realName: isEdit && row ? row.realName || row.nickName || '' : '',
      password: '',
      gender:
        isEdit && row
          ? typeof row.gender === 'number'
            ? (row.gender as 0 | 1 | 2)
            : row.userGender === '男'
              ? 1
              : row.userGender === '女'
                ? 2
                : 0
          : 0,
      roleType:
        isEdit && row
          ? (typeof row.roleType === 'number'
              ? (row.roleType as 1 | 2)
              : Array.isArray(row.userRoles) && row.userRoles.includes('R_ADMIN')
                ? 2
                : 1)
          : 1,
      status:
        isEdit && row
          ? (typeof row.statusCode === 'number'
              ? (row.statusCode as 0 | 1 | 2)
              : (Number(row.status) as 0 | 1 | 2))
          : 1,
      memberActive: isEdit && row ? row.memberActive ?? undefined : undefined,
      memberExpireTime: isEdit && row ? row.memberExpireTime || '' : ''
    })
  }

  /**
   * 监听对话框状态变化
   * 当对话框打开时初始化表单数据并清除验证状态
   */
  watch(
    () => [props.visible, props.type, props.userData],
    ([visible]) => {
      if (visible) {
        initFormData()
        nextTick(() => {
          formRef.value?.clearValidate()
        })
      }
    },
    { immediate: true }
  )

  /**
   * 提交表单
   * 验证通过后触发提交事件
   */
  const handleSubmit = async () => {
    if (!formRef.value) return

    await formRef.value.validate((valid) => {
      if (valid) {
        const payload: any = {
          username: formData.username,
          realName: formData.realName,
          gender: formData.gender,
          roleType: formData.roleType,
          status: formData.status,
          memberActive: formData.memberActive,
          memberExpireTime: formData.memberExpireTime || undefined
        }

        if (dialogType.value === 'add') {
          payload.phone = formData.phone
          payload.password = formData.password
        } else {
          if (formData.password) payload.password = formData.password
        }

        dialogVisible.value = false
        emit('submit', payload)
      }
    })
  }
</script>
