<!-- 用户管理页面 -->
<!-- art-full-height 自动计算出页面剩余高度 -->
<!-- art-table-card 一个符合系统样式的 class，同时自动撑满剩余高度 -->
<!-- 更多 useTable 使用示例请移步至 功能示例 下面的高级表格示例或者查看官方文档 -->
<!-- useTable 文档：https://www.artd.pro/docs/zh/guide/hooks/use-table.html -->
<template>
  <div class="user-page art-full-height">
    <!-- 搜索栏 -->
    <UserSearch v-model="searchForm" @search="handleSearch" @reset="resetSearchParams"></UserSearch>

    <ElCard class="art-table-card" shadow="never">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton @click="showDialog('add')" v-ripple>新增用户</ElButton>
          </ElSpace>
        </template>
      </ArtTableHeader>

      <!-- 表格 -->
      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
        @selection-change="handleSelectionChange"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handleCurrentChange"
      >
      </ArtTable>

      <!-- 用户弹窗 -->
      <UserDialog
        v-model:visible="dialogVisible"
        :type="dialogType"
        :user-data="currentUserData"
        @submit="handleDialogSubmit"
      />
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import {
    fetchCreateUserByAdmin,
    fetchGetUserList,
    fetchHardDeleteUserByAdmin,
    fetchUpdateUserByAdmin
  } from '@/api/system-manage'
  import UserSearch from './modules/user-search.vue'
  import UserDialog from './modules/user-dialog.vue'
  import { ElTag, ElMessageBox, ElImage, ElMessage } from 'element-plus'
  import { DialogType } from '@/types'
  import defaultAvatar from '@imgs/user/avatar.webp'

  defineOptions({ name: 'User' })

  type UserListItem = Api.SystemManage.UserListItem

  // 弹窗相关
  const dialogType = ref<DialogType>('add')
  const dialogVisible = ref(false)
  const currentUserData = ref<Partial<UserListItem>>({})

  // 选中行
  const selectedRows = ref<UserListItem[]>([])

  // 搜索表单
  const searchForm = ref({
    userName: undefined,
    userGender: undefined,
    userPhone: undefined,
    status: undefined
  })

  // 用户状态配置
  const USER_STATUS_CONFIG = {
    '0': { type: 'danger' as const, text: '禁用' },
    '1': { type: 'success' as const, text: '启用' },
    '2': { type: 'warning' as const, text: '待审核' }
  } as const

  // 会员状态配置
  const MEMBER_STATUS_CONFIG = {
    true: { type: 'success' as const, text: '有效' },
    false: { type: 'danger' as const, text: '无效' }
  } as const

  /**
   * 获取用户状态配置
   */
  const getUserStatusConfig = (status: string) => {
    return (
      USER_STATUS_CONFIG[status as keyof typeof USER_STATUS_CONFIG] || {
        type: 'info' as const,
        text: '未知'
      }
    )
  }

  /**
   * 获取会员状态配置
   */
  const getMemberStatusConfig = (status?: boolean) => {
    if (status === undefined || status === null) {
      return { type: 'info' as const, text: '-' }
    }

    return MEMBER_STATUS_CONFIG[status ? 'true' : 'false']
  }

  const formatDateTime = (value?: string) => {
    if (!value) return '-'
    const d = new Date(value)
    if (Number.isNaN(d.getTime())) return value
    const pad2 = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(
      d.getMinutes()
    )}:${pad2(d.getSeconds())}`
  }

  const {
    columns,
    columnChecks,
    data,
    loading,
    pagination,
    getData,
    searchParams,
    resetSearchParams,
    handleSizeChange,
    handleCurrentChange,
    refreshData
  } = useTable({
    // 核心配置
    core: {
      apiFn: fetchGetUserList,
      apiParams: {
        current: 1,
        size: 20,
        ...searchForm.value
      },
      // 自定义分页字段映射，未设置时将使用全局配置 tableConfig.ts 中的 paginationKey
      // paginationKey: {
      //   current: 'pageNum',
      //   size: 'pageSize'
      // },
      columnsFactory: () => [
        { type: 'selection', width: 44 }, // 勾选列
        { type: 'index', width: 54, label: '序号' }, // 序号
        {
          prop: 'userInfo',
          label: '用户名',
          width: 240,
          // visible: false, // 默认是否显示列
          formatter: (row) => {
            return h('div', { class: 'user flex-c' }, [
              // 用 ElAvatar 而不是 ElImage：图片失效时 ElImage 会在格子里渲染
              // 一行「加载失败」红字，把用户名挤下去；ElAvatar 只会留空底。
              h(ElAvatar, {
                class: 'size-9.5 rounded-md',
                shape: 'square',
                src: row.avatar || defaultAvatar
              }),
              h('div', { class: 'ml-2' }, [
                h('p', { class: 'user-name' }, row.userName),
              ])
            ])
          }
        },
        {
          prop: 'userGender',
          label: '性别',
          width: 70,
          sortable: true,
          formatter: (row) => row.userGender
        },
        { prop: 'userPhone', label: '手机号', width: 130 },
        {
          prop: 'status',
          label: '账号状态',
          width: 100,
          formatter: (row) => {
            const statusConfig = getUserStatusConfig(row.status)
            return h(ElTag, { type: statusConfig.type }, () => statusConfig.text)
          }
        },
        {
          prop: 'memberActive',
          label: '会员状态',
          width: 100,
          formatter: (row) => {
            const memberConfig = getMemberStatusConfig(row.memberActive)
            return h(ElTag, { type: memberConfig.type }, () => memberConfig.text)
          }
        },
        {
          prop: 'createTime',
          label: '创建日期',
          width: 150,
          sortable: true,
          // 精确到分钟即可，带秒会让这一列折行、把行高撑开
          formatter: (row) => formatDateTime(row.createTime).slice(0, 16)
        },
        {
          prop: 'operation',
          label: '操作',
          width: 130,
          fixed: 'right', // 固定列
          formatter: (row) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'edit',
                onClick: () => showDialog('edit', row)
              }),
              h(ArtButtonTable, {
                type: 'delete',
                onClick: () => hardDeleteUser(row)
              })
            ])
        }
      ]
    },
    // 数据处理
    transform: {
      // 数据转换器 - 替换头像
      dataTransformer: (records) => {
        // 类型守卫检查
        if (!Array.isArray(records)) {
          console.warn('数据转换器: 期望数组类型，实际收到:', typeof records)
          return []
        }

        return records.map((item) => ({
          ...item,
          avatar: item.avatar || defaultAvatar
        }))
      }
    }
  })

  /**
   * 搜索处理
   * @param params 参数
   */
  const handleSearch = (params: Record<string, any>) => {
    console.log(params)
    // 搜索参数赋值
    Object.assign(searchParams, params)
    getData()
  }

  /**
   * 显示用户弹窗
   */
  const showDialog = (type: DialogType, row?: UserListItem): void => {
    console.log('打开弹窗:', { type, row })
    dialogType.value = type
    currentUserData.value = row || {}
    nextTick(() => {
      dialogVisible.value = true
    })
  }

  /**
   * 删除用户
   */
  const hardDeleteUser = (row: UserListItem): void => {
    ElMessageBox.confirm(`确定要删除该用户吗？此操作不可恢复。`, '删除用户', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    }).then(async () => {
      await fetchHardDeleteUserByAdmin(row.id)
      ElMessage.success('删除成功')
      refreshData()
    })
  }

  /**
   * 处理弹窗提交事件
   */
  const handleDialogSubmit = async (
    payload: Api.SystemManage.AdminUserCreateParams | Api.SystemManage.AdminUserUpdateParams
  ) => {
    try {
      if (dialogType.value === 'add') {
        await fetchCreateUserByAdmin(payload as Api.SystemManage.AdminUserCreateParams)
      } else {
        const userId = currentUserData.value?.id
        if (typeof userId === 'number') {
          await fetchUpdateUserByAdmin(userId, payload as Api.SystemManage.AdminUserUpdateParams)
        }
      }

      dialogVisible.value = false
      currentUserData.value = {}
      refreshData()
    } catch (error) {
      console.error('提交失败:', error)
    }
  }

  /**
   * 处理表格行选择变化
   */
  const handleSelectionChange = (selection: UserListItem[]): void => {
    selectedRows.value = selection
    console.log('选中行数据:', selectedRows.value)
  }
</script>
