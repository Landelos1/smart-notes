<!-- 文章列表页面 -->
<template>
  <div class="page-content !mb-5 art-full-height">
    <p class="mb-3 text-sm text-g-600">管理已发布到笔记广场的内容。下线会移出广场，作者的原始笔记仍可使用。</p>
    <ArtSearchBar v-model="searchParams" :items="searchFormItems" @search="handleSearch" @reset="handleReset" />

    <ElCard class="art-table-card" shadow="never">
      <!-- 表格头部 -->
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="toAddArticle" v-auth="'add'" v-ripple>
              <ElIcon>
                <Plus />
              </ElIcon>
              新增笔记
            </ElButton>

            <ArtExcelExport
              :data="data as any"
              :columns="exportColumns as any"
              filename="笔记数据"
              :auto-index="true"
              button-text="导出"
            />

            <ElButton
              @click="handleBatchUnpublish"
              :disabled="selectedRows.length === 0"
              v-ripple
            >
              <ElIcon>
                <Delete />
              </ElIcon>
              批量下线 ({{ selectedRows.length }})
            </ElButton>
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
        <template #squareStatus="{ row }">
          <ElTag :type="getSquareStatusConfig(row.squareStatus).type" effect="plain">
            {{ getSquareStatusConfig(row.squareStatus).text }}
          </ElTag>
        </template>

        <template #publishTime="{ row }">
          <!-- 列表里精确到分钟就够，带秒会让这一列折成两行、把整行撑高 -->
          <span class="whitespace-nowrap">{{ formatDateTime(row.publishTime || row.createTime).slice(0, 16) }}</span>
        </template>

        <template #tags="{ row }">
          <ElSpace wrap>
            <ElTag v-for="(t, idx) in (row.tags || []).slice(0, 3)" :key="`${row.id}-${idx}`" effect="plain">
              {{ t }}
            </ElTag>
            <ElTag v-if="(row.tags || []).length > 3" effect="plain">+{{ (row.tags || []).length - 3 }}</ElTag>
          </ElSpace>
        </template>

        <template #operation="{ row }">
          <div class="flex">
            <ArtButtonTable type="view" @click="toDetail(row)" />
            <ArtButtonTable type="edit" v-auth="'edit'" @click="toEdit(row)" />
            <ArtButtonTable type="delete" @click="handleUnpublish(row)" />
          </div>
        </template>
      </ArtTable>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, onMounted } from 'vue'
  import { Plus, Delete } from '@element-plus/icons-vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { router } from '@/router'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import { fetchGetNoteSquarePage, fetchUnpublishNoteSquare } from '@/api/note-square'

  defineOptions({ name: 'ArticleList' })

  type NoteRow = Api.NoteSquare.NoteSquareVO

  const searchFormItems = computed(() => [
    {
      label: '关键词',
      key: 'keyword',
      type: 'input',
      placeholder: '输入标题/摘要/内容关键词查询',
      clearable: true
    },
    {
      label: '排序',
      key: 'sortBy',
      type: 'select',
      props: {
        placeholder: '选择排序字段',
        clearable: true,
        options: [
          { label: '推荐', value: 'recommend' },
          { label: '最新', value: 'latest' },
          { label: '更新时间', value: 'update_time' }
        ]
      }
    },
    {
      label: '顺序',
      key: 'sortOrder',
      type: 'select',
      props: {
        placeholder: '选择排序顺序',
        clearable: true,
        options: [
          { label: '降序', value: 'desc' },
          { label: '升序', value: 'asc' }
        ]
      }
    }
  ])

  const selectedRows = ref<NoteRow[]>([])

  const exportColumns = computed(() => ({
    title: { title: '标题', width: 30 },
    authorName: { title: '作者', width: 14 },
    squareStatus: { title: '状态', width: 10 },
    viewCount: { title: '浏览', width: 10 },
    likeCount: { title: '点赞', width: 10 },
    favoriteCount: { title: '收藏', width: 10 },
    commentCount: { title: '评论', width: 10 },
    publishTime: { title: '发布时间', width: 18 }
  }))

  const {
    columns,
    columnChecks,
    data,
    loading,
    pagination,
    searchParams,
    getData,
    handleSizeChange,
    handleCurrentChange,
    refreshData,
    refreshRemove,
    refreshUpdate,
    resetColumns
  } = useTable({
    core: {
      apiFn: (params) => {
        console.log('[NoteList] request params:', params)
        return fetchGetNoteSquarePage({
          page: (params as any).page,
          size: (params as any).size,
          keyword: (params as any).keyword,
          sortBy: (params as any).sortBy,
          sortOrder: (params as any).sortOrder
        })
      },
      apiParams: {
        page: 1,
        size: 20,
        keyword: undefined,
        sortBy: 'recommend',
        sortOrder: 'desc'
      } as any,
      paginationKey: {
        current: 'page',
        size: 'size'
      },
      columnsFactory: () => [
        { type: 'selection', width: 44 },
        { type: 'globalIndex', width: 54, label: '序号' },
        { prop: 'title', label: '标题', minWidth: 200, sortable: true, showOverflowTooltip: true },
        { prop: 'authorName', label: '作者', minWidth: 110, showOverflowTooltip: true },
        { prop: 'tags', label: '标签', minWidth: 150, useSlot: true },
        { prop: 'squareStatus', label: '状态', width: 96, useSlot: true },
        { prop: 'viewCount', label: '浏览', width: 78, sortable: true },
        { prop: 'likeCount', label: '点赞', width: 78, sortable: true },
        { prop: 'favoriteCount', label: '收藏', width: 78, sortable: true },
        { prop: 'commentCount', label: '评论', width: 78, sortable: true },
        { prop: 'publishTime', label: '发布时间', minWidth: 150, sortable: true, useSlot: true },
        { prop: 'operation', label: '操作', width: 120, useSlot: true }
      ]
    },
    hooks: {
      onSuccess: (list, resp) => {
        console.log('[NoteList] success:', { count: list.length, resp })
      },
      onError: (err) => {
        console.error('[NoteList] error:', err)
        ElMessage.error(err.message)
      }
    },
    debug: {
      enableLog: true
    }
  })

  const handleSelectionChange = (selection: NoteRow[]) => {
    selectedRows.value = selection
  }

  const handleSearch = () => {
    getData()
  }

  const handleReset = () => {
    getData()
  }

  const SQUARE_STATUS_CONFIG = {
    0: { type: 'info' as const, text: '下线' },
    1: { type: 'success' as const, text: '发布' },
    2: { type: 'danger' as const, text: '禁用' }
  } as const

  const getSquareStatusConfig = (status?: number) => {
    if (status === undefined || status === null) {
      return { type: 'info' as const, text: '-' }
    }
    return (
      SQUARE_STATUS_CONFIG[status as keyof typeof SQUARE_STATUS_CONFIG] || {
        type: 'info' as const,
        text: '未知'
      }
    )
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

  const toDetail = (row: NoteRow) => {
    router.push({ name: 'ArticleDetail', params: { id: row.id } })
  }

  const toEdit = (row: NoteRow) => {
    if (!router.hasRoute('ArticlePublish')) {
      ElMessage.error('当前账号无权限访问“笔记发布/编辑”页面')
      return
    }
    router.push({ name: 'ArticlePublish', query: { id: row.id } })
  }

  const toAddArticle = () => {
    if (!router.hasRoute('ArticlePublish')) {
      ElMessage.error('当前账号无权限访问“笔记发布”页面')
      return
    }
    router.push({ name: 'ArticlePublish' })
  }

  const handleUnpublish = async (row: NoteRow) => {
    await ElMessageBox.confirm(`确定要将该笔记从笔记广场下线吗？`, '下线笔记', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    await fetchUnpublishNoteSquare(row.id)
    await refreshRemove()
  }

  const handleBatchUnpublish = async () => {
    await ElMessageBox.confirm(`确定要下线选中的 ${selectedRows.value.length} 条笔记吗？`, '批量下线', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    for (const row of selectedRows.value) {
      await fetchUnpublishNoteSquare(row.id)
    }

    selectedRows.value = []
    await refreshRemove()
  }

  onMounted(async () => {
    resetColumns?.()
    await Promise.resolve()
  })
</script>

<style lang="scss">
  .custom-segmented .el-segmented {
    height: 40px;
    padding: 6px;

    --el-border-radius-base: 8px;
  }
</style>
