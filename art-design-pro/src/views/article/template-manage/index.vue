<template>
  <div class="template-manage-page art-full-height">
    <TemplateSearch v-model="searchForm" @search="handleSearch" @reset="resetSearchParams" />

    <ElCard class="art-table-card" shadow="never">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton type="primary" @click="showPublishDialog('add')" v-auth="'add'" v-ripple>
              <ElIcon><Plus /></ElIcon>
              发布模板
            </ElButton>
          </ElSpace>
        </template>
      </ArtTableHeader>

      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
        @selection-change="handleSelectionChange"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handleCurrentChange"
      >
        <template #category="{ row }">
          <ElTag :type="getCategoryType(row.category)" effect="plain">
            {{ getCategoryLabel(row.category) }}
          </ElTag>
        </template>

        <template #tags="{ row }">
          <ElSpace wrap>
            <ElTag
              v-for="(tag, idx) in (row.tags || []).slice(0, 3)"
              :key="`${row.id}-${idx}`"
              size="small"
              effect="plain"
            >
              {{ tag }}
            </ElTag>
            <ElTag v-if="(row.tags || []).length > 3" size="small" effect="plain">
              +{{ (row.tags || []).length - 3 }}
            </ElTag>
          </ElSpace>
        </template>

        <template #status="{ row }">
          <ElTag :type="getStatusType(row.status)" effect="plain">
            {{ getStatusLabel(row.status) }}
          </ElTag>
        </template>

        <template #isPublic="{ row }">
          <ElTag :type="row.isPublic ? 'success' : 'info'" effect="plain">
            {{ row.isPublic ? '公开' : '私有' }}
          </ElTag>
        </template>

        <template #operation="{ row }">
          <div class="flex">
            <ArtButtonTable type="view" @click="handlePreview(row)" />
            <ArtButtonTable type="edit" @click="showPublishDialog('edit', row)" />
            <ArtButtonTable type="delete" @click="handleDelete(row)" />
          </div>
        </template>
      </ArtTable>

      <TemplatePublishDialog
        v-model:visible="publishDialogVisible"
        :type="dialogType"
        :template-data="currentTemplateData"
        @submit="handleDialogSubmit"
      />

      <ElDialog v-model="previewVisible" title="模板预览" width="60%" align-center>
        <div v-loading="previewLoading">
          <div class="text-lg font-medium">{{ previewData?.title || '-' }}</div>
          <div class="mt-2 text-sm text-g-600">{{ previewData?.description || '-' }}</div>
          <div class="mt-3">
            <ElTag effect="plain" class="mr-2">
              {{ previewData?.category ? getCategoryLabel(previewData.category) : '-' }}
            </ElTag>
            <ElTag v-for="(t, idx) in (previewData?.tags || [])" :key="idx" effect="plain" class="mr-2">
              {{ t }}
            </ElTag>
          </div>
          <ElDivider />
          <div class="preview-content" v-html="previewHtml"></div>
        </div>
      </ElDialog>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue'
  import MarkdownIt from 'markdown-it'
  import { Plus } from '@element-plus/icons-vue'
  import { ElMessageBox } from 'element-plus'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import {
    fetchSearchTemplates,
    fetchGetTemplateDetail,
    fetchCreateTemplate,
    fetchUpdateTemplate,
    fetchDeleteTemplate
  } from '@/api/template'
  import TemplateSearch from './modules/template-search.vue'
  import TemplatePublishDialog from './modules/template-publish-dialog.vue'

  defineOptions({ name: 'NoteTemplateManage' })

  type TemplateRow = Api.Template.TemplateVO

  const publishDialogVisible = ref(false)
  const dialogType = ref<'add' | 'edit'>('add')
  const currentTemplateData = ref<Partial<TemplateRow>>({})

  const selectedRows = ref<TemplateRow[]>([])

  const previewVisible = ref(false)
  const previewLoading = ref(false)
  const previewData = ref<Api.Template.TemplateVO | null>(null)

  const markdown = new MarkdownIt({ html: true, linkify: true, breaks: true })

  const previewHtml = computed(() => {
    const content = previewData.value?.content || ''
    return content ? markdown.render(content) : ''
  })

  const searchForm = ref({
    keyword: undefined,
    category: 'all',
    status: undefined
  })

  const categoryMap = {
    work: { label: '工作汇报', type: 'primary' as const },
    study: { label: '学习笔记', type: 'success' as const },
    meeting: { label: '会议记录', type: 'warning' as const },
    project: { label: '项目管理', type: 'danger' as const },
    all: { label: '全部', type: 'info' as const }
  }

  const getCategoryLabel = (category: string) => {
    return categoryMap[category as keyof typeof categoryMap]?.label || category
  }

  const getCategoryType = (category: string) => {
    return categoryMap[category as keyof typeof categoryMap]?.type || 'info'
  }

  const getStatusLabel = (status?: number) => {
    const map: Record<number, string> = {
      0: '草稿',
      1: '已发布',
      2: '已下架'
    }
    return map[status ?? 1] || '已发布'
  }

  const getStatusType = (status?: number) => {
    const map: Record<number, any> = {
      0: 'info',
      1: 'success',
      2: 'warning'
    }
    return map[status ?? 1] || 'success'
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
    core: {
      apiFn: fetchSearchTemplates,
      apiParams: {
        page: 1,
        pageSize: 20,
        ...searchForm.value
      },
      paginationKey: {
        current: 'page',
        size: 'pageSize'
      },
      columnsFactory: () => [
        { type: 'selection', width: 44 },
        { type: 'index', width: 54, label: '序号' },
        { prop: 'title', label: '标题', minWidth: 170, showOverflowTooltip: true },
        { prop: 'description', label: '描述', minWidth: 170, showOverflowTooltip: true },
        { prop: 'category', label: '分类', width: 96, useSlot: true, slotName: 'category' },
        { prop: 'tags', label: '标签', minWidth: 140, useSlot: true, slotName: 'tags' },
        { prop: 'downloads', label: '下载', width: 66 },
        { prop: 'views', label: '浏览', width: 66 },
        { prop: 'rating', label: '评分', width: 66 },
        { prop: 'isPublic', label: '可见', width: 76, useSlot: true, slotName: 'isPublic' },
        { prop: 'status', label: '状态', width: 92, useSlot: true, slotName: 'status' },
        {
          prop: 'createTime',
          label: '创建时间',
          width: 150,
          formatter: (row) => formatDateTime(row.createTime || row.updatedAt).slice(0, 16)
        },
        { prop: 'operation', label: '操作', width: 92, useSlot: true }
      ]
    }
  })

  const handleSearch = () => {
    Object.assign(searchParams, searchForm.value, { page: 1 })
    getData()
  }

  const handleSelectionChange = (rows: TemplateRow[]) => {
    selectedRows.value = rows
  }

  const showPublishDialog = (type: 'add' | 'edit', row?: TemplateRow) => {
    dialogType.value = type
    currentTemplateData.value = type === 'edit' && row ? { ...row } : {}
    publishDialogVisible.value = true
  }

  const handleDialogSubmit = async (payload: Api.Template.TemplateCreateDTO) => {
    if (dialogType.value === 'add') {
      await fetchCreateTemplate(payload)
    } else {
      const id = currentTemplateData.value.id
      if (!id) return
      await fetchUpdateTemplate(id, payload)
    }
    refreshData()
  }

  const handleDelete = (row: TemplateRow) => {
    ElMessageBox.confirm(`确定删除模板“${row.title}”吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        await fetchDeleteTemplate(row.id)
        refreshData()
      })
      .catch(() => {})
  }

  const handlePreview = async (row: TemplateRow) => {
    previewVisible.value = true
    previewLoading.value = true
    try {
      previewData.value = await fetchGetTemplateDetail(row.id)
    } finally {
      previewLoading.value = false
    }
  }
</script>

<style scoped lang="scss">
  .template-manage-page {
    :deep(.el-image) {
      display: block;
    }

    :deep(.art-cell-wrap .cell) {
      white-space: normal;
      word-break: break-word;
    }
  }

  .preview-content {
    :deep(img) {
      max-width: 100%;
    }
  }
</style>
