<!-- 留言管理页面 -->
<template>
  <div class="comment-page art-full-height">
    <ArtSearchBar v-model="searchParams" :items="searchFormItems" @search="handleSearch" @reset="handleReset" />

    <ElCard class="art-table-card" shadow="never">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData" />

      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handleCurrentChange"
      />

      <ElDrawer
        v-model="showDrawer"
        :lock-scroll="false"
        :size="420"
        modal-class="comment-modal"
      >
        <template #header>
          <h4>预览</h4>
        </template>
        <template #default>
          <div class="drawer-default">
            <div class="p-4">
              <div class="text-g-600 text-sm">{{ formatDateTime(clickItem.createdAt) }}</div>
              <div class="mt-4 text-sm text-gray-800 whitespace-pre-wrap break-words">{{ clickItem.content }}</div>
              <div class="mt-4 flex-cb">
                <div class="flex-c text-xs text-g-600">
                  <ArtSvgIcon icon="ri:heart-line" class="mr-1 text-base" />
                  <span>{{ clickItem.likes }}</span>
                </div>
                <div class="flex-c text-xs text-g-600">
                  <ArtSvgIcon icon="ri:star-line" class="mr-1 text-base" />
                  <span>{{ clickItem.rating }}</span>
                </div>
                <div class="text-sm text-gray-700">{{ clickItem.author || '-' }}</div>
              </div>
            </div>
          </div>
        </template>
      </ElDrawer>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { h } from 'vue'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import ArtSearchBar from '@/components/core/forms/art-search-bar/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchAdminCommentList, fetchAdminDeleteComment } from '@/api/template'
  import { ElMessageBox, ElMessage, ElTag } from 'element-plus'

  defineOptions({ name: 'ArticleComment' })

  interface CommentRow {
    id: number
    createdAt: string
    content: string
    likes: number
    rating: number
    author: string
    sourceType?: 'TEMPLATE' | 'NOTE'
    targetId?: number
    targetTitle?: string
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

  const showDrawer = ref(false)
  const clickItem = ref<CommentRow>({
    id: 0,
    createdAt: '',
    content: '',
    likes: 0,
    rating: 0,
    author: ''
  })

  const searchFormItems = computed(() => [
    {
      label: '来源类型',
      key: 'sourceType',
      type: 'select',
      props: {
        clearable: true,
        placeholder: '选择来源',
        options: [
          { label: '模板评论', value: 'TEMPLATE' },
          { label: '笔记广场评论', value: 'NOTE' }
        ]
      }
    },
    {
      label: '关键词',
      key: 'keyword',
      type: 'input',
      props: { clearable: true, placeholder: '评论内容关键字' }
    }
  ])

  const openDrawer = (row: CommentRow) => {
    clickItem.value = row
    showDrawer.value = true
  }

  const deleteComment = (row: CommentRow) => {
    ElMessageBox.confirm(`确定要删除该评论吗？此操作不可恢复。`, '删除评论', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    }).then(async () => {
      await fetchAdminDeleteComment(row.id, row.sourceType || 'TEMPLATE')
      ElMessage.success('删除成功')
      refreshData()
    })
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
    refreshData: refreshTableData
  } = useTable({
    core: {
      apiFn: (params) => fetchAdminCommentList(params as Api.Template.AdminCommentListParams),
      apiParams: {
        page: 1,
        size: 10,
        sourceType: undefined,
        keyword: undefined
      },
      paginationKey: { current: 'page', size: 'size' },
      columnsFactory: () => [
        { type: 'index', width: 54, label: '序号' },
        {
          prop: 'sourceType',
          label: '来源',
          width: 120,
          formatter: (row) =>
            h(ElTag, { type: row.sourceType === 'NOTE' ? 'info' : 'success', effect: 'plain' }, () =>
              row.sourceType === 'NOTE' ? '笔记广场' : '模板'
            )
        },
        { prop: 'targetTitle', label: '目标标题', minWidth: 180 },
        {
          prop: 'createdAt',
          label: '时间',
          width: 150,
          sortable: true,
          formatter: (row) => formatDateTime(row.createdAt).slice(0, 16)
        },
        {
          prop: 'content',
          label: '内容',
          minWidth: 260,
          formatter: (row) => h('div', { class: 'text-gray-800 line-clamp-2' }, row.content)
        },
        { prop: 'likes', label: '点赞', width: 80, sortable: true },
        { prop: 'rating', label: '评分', width: 80, sortable: true },
        { prop: 'author', label: '作者', width: 120 },
        {
          prop: 'operation',
          label: '操作',
          width: 120,
          fixed: 'right',
          formatter: (row) =>
            h('div', [
              h(ArtButtonTable, {
                type: 'view',
                onClick: () => openDrawer(row)
              }),
              h(ArtButtonTable, {
                type: 'delete',
                onClick: () => deleteComment(row)
              })
            ])
        }
      ]
    }
  })

  const handleSearch = () => {
    getData()
  }

  const handleReset = () => {
    resetSearchParams()
  }

  const refreshData = () => {
    refreshTableData()
  }

  onMounted(() => {
    getData()
  })
</script>
