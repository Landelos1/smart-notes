<!-- 文章列表页面 -->
<template>
  <div class="page-content !mb-5">
    <ElRow justify="space-between" :gutter="10">
      <ElCol :lg="6" :md="6" :sm="14" :xs="16">
        <ElInput
          v-model="searchVal"
          :prefix-icon="Search"
          clearable
          placeholder="输入知识库名称查询"
          @keyup.enter="searchArticle"
        />
      </ElCol>
      <ElCol :lg="12" :md="12" :sm="0" :xs="0">
        <div class="custom-segmented">
          <ElSegmented v-model="yearVal" :options="YEAR_OPTIONS" @change="searchArticleByYear" />
        </div>
      </ElCol>
      <ElCol :lg="6" :md="6" :sm="10" :xs="6" style="display: flex; justify-content: end">
        <ElButton @click="toAddNotebook" v-auth="'add'">新增知识库</ElButton>
      </ElCol>
    </ElRow>

    <div class="mt-5">
      <div class="grid grid-cols-4 gap-6 max-2xl:grid-cols-3 max-xl:grid-cols-2 max-sm:grid-cols-1">
        <div
          class="group c-p overflow-hidden border border-g-300/60 rounded-custom-sm"
          v-for="item in articleList"
          :key="item.id"
          @click="openFileDialog(item)"
        >
          <div class="relative aspect-[16/10]">
            <ElImage
              class="flex align-center justify-center w-full h-full object-cover bg-g-200"
              :src="item.home_img"
              lazy
              fit="cover"
            >
              <!-- 封面缺失时不要留一行「加载失败」，给个暖色占位 -->
              <template #error>
                <div class="flex items-center justify-center w-full h-full bg-g-200 text-g-500 text-xs">
                  暂无封面
                </div>
              </template>
            </ElImage>

            <!-- 原来是 bg-black/50 纯黑半透明，在奶油底上像块补丁，改暖褐 -->
            <span
              class="absolute top-1 right-1 bg-[#241b14]/55 rounded text-xs px-1.5 py-0.5 text-white backdrop-blur-[2px]"
              >{{ item.type_name }}</span
            >
          </div>
          <div class="px-2 py-1">
            <h2 class="text-base text-g-800 font-medium">{{ item.title }}</h2>
            <p class="mt-1 line-clamp-2 text-xs text-g-600">{{ item.description || '暂无描述' }}</p>
            <div class="flex-b w-full mt-2">
              <div class="flex-c text-g-500">
                <ArtSvgIcon icon="ri:time-line" class="mr-1 text-sm" />
                <span class="text-sm">{{ useDateFormat(item.create_time, 'YYYY-MM-DD') }}</span>
                <div class="w-px h-3 bg-g-400 mx-3.5"></div>
                <ArtSvgIcon icon="ri:group-line" class="mr-1 text-sm" />
                <span class="text-sm">{{ item.memberCount }}</span>
                <div class="w-px h-3 bg-g-400 mx-3.5"></div>
                <ArtSvgIcon icon="ri:file-list-3-line" class="mr-1 text-sm" />
                <span class="text-sm">{{ item.fileCount }}</span>
              </div>
              <div class="flex items-center gap-2">
                <ElButton size="small" @click.stop="toEdit(item)">编辑</ElButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ElDialog
      v-model="fileDialogVisible"
      width="1000px"
      top="5vh"
      :body-style="{ maxHeight: '70vh', overflow: 'auto' }"
      title="知识库文件管理"
    >
      <div class="flex items-center gap-3 mb-4">
        <ElInput
          v-model="fileKeyword"
          placeholder="输入文件名查询"
          clearable
          @keyup.enter="loadFiles()"
        />
        <ElButton @click="loadFiles({ resetPage: true })">查询</ElButton>
        <ElUpload :http-request="handleUpload" :show-file-list="false">
          <ElButton type="primary">上传文件</ElButton>
        </ElUpload>
      </div>

      <ElTable :data="fileList" border height="420" v-loading="fileLoading">
        <ElTableColumn prop="originalFilename" label="文件名" min-width="240" />
        <ElTableColumn prop="fileType" label="类型" width="120" />
        <ElTableColumn prop="fileSize" label="大小" width="120" />
        <ElTableColumn prop="uploaderName" label="上传人" width="120" />
        <ElTableColumn prop="uploadTime" label="上传时间" width="180" />
        <ElTableColumn label="操作" width="160" fixed="right">
          <template #default="scope">
            <ElButton size="small" @click="renameFile(scope.row)">重命名</ElButton>
            <ElButton size="small" type="danger" @click="deleteFile(scope.row)">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="mt-4 flex justify-end">
        <ElPagination
          v-model:current-page="filePage"
          :page-size="fileSize"
          layout="prev, pager, next, total"
          :total="fileTotal"
          @current-change="handleFilePageChange"
        />
      </div>
    </ElDialog>

    <ElDialog v-model="editDialogVisible" width="520px" :title="editForm.id ? '编辑知识库' : '新增知识库'">
      <ElForm label-width="90px">
        <ElFormItem label="名称">
          <ElInput v-model="editForm.name" placeholder="请输入知识库名称" />
        </ElFormItem>
        <ElFormItem label="描述">
          <ElInput
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述"
          />
        </ElFormItem>
        <ElFormItem label="封面图片">
          <div class="flex items-center gap-4">
            <ElImage
              v-if="editForm.coverUrl"
              :src="editForm.coverUrl"
              class="w-24 h-24 rounded border object-cover bg-gray-50"
            />
            <div
              v-else
              class="w-24 h-24 rounded border border-dashed flex flex-col items-center justify-center text-g-400 bg-gray-50"
            >
              <ArtSvgIcon icon="ri:image-line" class="text-2xl" />
              <span class="text-xs mt-1">暂无封面</span>
            </div>
            <ElUpload :http-request="handleCoverUpload" :show-file-list="false" accept="image/*">
              <ElButton type="primary" size="small">上传封面</ElButton>
            </ElUpload>
          </div>
        </ElFormItem>
        <ElFormItem label="公开状态">
          <ElSwitch v-model="editForm.isPublic" active-text="公开" inactive-text="私密" />
        </ElFormItem>
        <ElFormItem v-if="!editForm.isPublic" label="访问密码">
          <ElInput v-model="editForm.password" placeholder="可选" show-password />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="flex items-center justify-between w-full">
          <ElButton v-if="editForm.id" type="danger" @click="handleDeleteInDialog">删除</ElButton>
          <div class="flex items-center gap-2">
            <ElButton @click="editDialogVisible = false">取消</ElButton>
            <ElButton type="primary" :disabled="!editForm.name.trim()" @click="submitKnowledgeBase">
              {{ editForm.id ? '保存' : '创建' }}
            </ElButton>
          </div>
        </div>
      </template>
    </ElDialog>

    <div style="margin-top: 16vh" v-if="showEmpty">
      <ElEmpty :description="`未找到相关数据 ${EmojiText[0]}`" />
    </div>

    <div style="display: flex; justify-content: center; margin-top: 20px">
      <ElPagination
        size="default"
        background
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :pager-count="9"
        layout="prev, pager, next, total,jumper"
        :total="total"
        :hide-on-single-page="true"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { Search } from '@element-plus/icons-vue'
  import { router } from '@/router'
  import { useDateFormat } from '@vueuse/core'
  import EmojiText from '@/utils/ui/emojo'
  import {
    fetchCreateSharedKnowledgeBase,
    fetchDeleteKnowledgeBaseFile,
    fetchDeleteSharedKnowledgeBase,
    fetchGetAllSharedKnowledgeBasesForAdmin,
    fetchGetKnowledgeBaseFiles,
    fetchRenameKnowledgeBaseFile,
    fetchUpdateSharedKnowledgeBase,
    fetchUploadSharedKnowledgeBaseFile
  } from '@/api/notebook'
  import { useCommon } from '@/hooks/core/useCommon'
  import defaultCover from '@imgs/cover/img1.webp'
  import type { UploadRequestOptions } from 'element-plus'
  import { ElMessageBox } from 'element-plus'

  defineOptions({ name: 'NotebookManage' })

  interface Article {
    id: number
    home_img: string
    type_name: string
    title: string
    description?: string
    create_time: string
    memberCount: number
    fileCount: number
  }

  interface GetArticleListOptions {
    backTop?: boolean
  }

  const YEAR_OPTIONS = ['All', '2024', '2023', '2022', '2021', '2020', '2019']
  const PAGE_SIZE = 40

  const yearVal = ref('All')
  const searchVal = ref('')
  const articleList = ref<Article[]>([])
  const notebooks = ref<Api.SharedKnowledgeBase.SharedKnowledgeBaseVO[]>([])
  const currentPage = ref(1)
  const pageSize = ref(PAGE_SIZE)
  const total = ref(0)
  const isLoading = ref(true)

  const fileDialogVisible = ref(false)
  const activeKnowledgeBaseId = ref<number | null>(null)
  const fileList = ref<Api.SharedKnowledgeBase.KnowledgeBaseFileVO[]>([])
  const fileKeyword = ref('')
  const filePage = ref(1)
  const fileSize = ref(10)
  const fileTotal = ref(0)
  const fileLoading = ref(false)

  const editDialogVisible = ref(false)
  const editForm = reactive({
    id: null as number | null,
    name: '',
    description: '',
    coverUrl: '',
    isPublic: true,
    password: ''
  })

  const showEmpty = computed(() => articleList.value.length === 0 && !isLoading.value)

  const getArticleList = async ({ backTop = false }: GetArticleListOptions = {}) => {
    isLoading.value = true

    try {
      if (searchVal.value) {
        yearVal.value = 'All'
      }

      notebooks.value = await fetchGetAllSharedKnowledgeBasesForAdmin()

      let list = notebooks.value
      if (searchVal.value) {
        const keyword = searchVal.value.trim().toLowerCase()
        list = list.filter(
          (item) =>
            item.name?.toLowerCase().includes(keyword) ||
            item.description?.toLowerCase().includes(keyword)
        )
      }

      if (yearVal.value !== 'All') {
        list = list.filter((item) => item.createTime?.startsWith(yearVal.value))
      }

      total.value = list.length
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      articleList.value = list.slice(start, end).map((item) => ({
        id: item.id,
        home_img: item.coverUrl || defaultCover,
        type_name: item.isPublic ? '公开' : '私密',
        title: item.name,
        description: item.description,
        create_time: item.createTime,
        memberCount: item.memberCount ?? 0,
        fileCount: item.fileCount ?? 0
      }))

      if (backTop) {
        useCommon().scrollToTop()
      }
    } catch (error) {
      console.error('获取文章列表失败:', error)
    } finally {
      isLoading.value = false
    }
  }

  const searchArticle = () => {
    currentPage.value = 1
    getArticleList({ backTop: true })
  }

  const searchArticleByYear = () => {
    currentPage.value = 1
    getArticleList({ backTop: true })
  }

  const handleCurrentChange = (val: number) => {
    currentPage.value = val
    getArticleList({ backTop: true })
  }

  const toDetail = () => {}

  const toDelete = (item: Article) => {
    ElMessageBox.confirm('确认删除该知识库吗？删除后无法恢复。', '删除确认', {
      type: 'warning'
    })
      .then(async () => {
        await fetchDeleteSharedKnowledgeBase(item.id)
        getArticleList({ backTop: false })
      })
      .catch(() => {})
  }

  const openFileDialog = (item: Article) => {
    activeKnowledgeBaseId.value = item.id
    fileDialogVisible.value = true
    loadFiles({ resetPage: true })
  }

  const loadFiles = async ({ resetPage = false }: { resetPage?: boolean } = {}) => {
    if (!activeKnowledgeBaseId.value) return
    if (resetPage) filePage.value = 1
    fileLoading.value = true
    try {
      const result = await fetchGetKnowledgeBaseFiles(activeKnowledgeBaseId.value, {
        page: filePage.value,
        size: fileSize.value,
        keyword: fileKeyword.value || undefined
      })
      fileList.value = result.data || []
      fileTotal.value = result.total || 0
    } finally {
      fileLoading.value = false
    }
  }

  const handleFilePageChange = (page: number) => {
    filePage.value = page
    loadFiles()
  }

  const handleUpload = async (options: UploadRequestOptions) => {
    if (!activeKnowledgeBaseId.value) return
    const formData = new FormData()
    formData.append('file', options.file)
    await fetchUploadSharedKnowledgeBaseFile(activeKnowledgeBaseId.value, formData)
    loadFiles({ resetPage: true })
  }

  const renameFile = (file: Api.SharedKnowledgeBase.KnowledgeBaseFileVO) => {
    if (!activeKnowledgeBaseId.value || !file.fileId) return
    ElMessageBox.prompt('请输入新的文件名', '重命名文件', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: file.originalFilename || ''
    })
      .then(async ({ value }) => {
        const name = (value || '').trim()
        if (!name) return
        await fetchRenameKnowledgeBaseFile(activeKnowledgeBaseId.value, file.fileId!, name)
        loadFiles()
      })
      .catch(() => {})
  }

  const deleteFile = (file: Api.SharedKnowledgeBase.KnowledgeBaseFileVO) => {
    if (!activeKnowledgeBaseId.value || !file.fileId) return
    ElMessageBox.confirm('确认删除该文件吗？', '删除确认', { type: 'warning' })
      .then(async () => {
        await fetchDeleteKnowledgeBaseFile(activeKnowledgeBaseId.value!, file.fileId!)
        loadFiles({ resetPage: false })
      })
      .catch(() => {})
  }

  const resetEditForm = () => {
    editForm.id = null
    editForm.name = ''
    editForm.description = ''
    editForm.coverUrl = ''
    editForm.isPublic = true
    editForm.password = ''
  }

  const toEdit = (item: Article) => {
    const current = notebooks.value.find((n) => n.id === item.id)
    if (!current) return
    editForm.id = current.id
    editForm.name = current.name || ''
    editForm.description = current.description || ''
    editForm.coverUrl = current.coverUrl || ''
    editForm.isPublic = current.isPublic ?? true
    editForm.password = ''
    editDialogVisible.value = true
  }

  const toAddNotebook = () => {
    resetEditForm()
    editDialogVisible.value = true
  }

  const submitKnowledgeBase = async () => {
    const name = editForm.name.trim()
    if (!name) return
    const payload = {
      name,
      description: editForm.description || undefined,
      coverUrl: editForm.coverUrl || undefined,
      isPublic: editForm.isPublic,
      password: editForm.isPublic ? undefined : editForm.password || undefined
    }
    if (editForm.id) {
      await fetchUpdateSharedKnowledgeBase(editForm.id, payload)
      editDialogVisible.value = false
      getArticleList({ backTop: false })
      return
    }
    await fetchCreateSharedKnowledgeBase(payload)
    editDialogVisible.value = false
    currentPage.value = 1
    getArticleList({ backTop: true })
  }

  const handleCoverUpload = async (options: UploadRequestOptions) => {
    const formData = new FormData()
    formData.append('file', options.file)
    try {
      // 使用通用的文件上传接口
      const res = await request.post<any>({
        url: '/api/common/file/upload',
        params: formData
      })
      if (res && res.url) {
        editForm.coverUrl = res.url
      }
    } catch (error) {
      console.error('上传封面失败:', error)
    }
  }

  const handleDeleteInDialog = () => {
    const id = editForm.id
    if (!id) return
    const current = notebooks.value.find((n) => n.id === id)
    if (!current) return
    editDialogVisible.value = false
    toDelete({
      id: current.id,
      home_img: current.coverUrl || defaultCover,
      type_name: current.isPublic ? '公开' : '私密',
      title: current.name,
      description: current.description,
      create_time: current.createTime || '',
      memberCount: current.memberCount ?? 0,
      fileCount: current.fileCount ?? 0
    })
  }

  onMounted(() => {
    getArticleList()
  })
</script>

<style lang="scss">
  .custom-segmented .el-segmented {
    height: 40px;
    padding: 6px;

    --el-border-radius-base: 8px;
  }
</style>
