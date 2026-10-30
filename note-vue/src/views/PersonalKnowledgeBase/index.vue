<script setup lang="ts">
import { ref, computed, onMounted, watch, inject } from 'vue'
import Button from '@/components/ui/Button.vue'
import {
  Search, Upload, Filter, Grid, List, Plus, FileText, RefreshCw,
  Trash2, Download, Eye, X, Check, AlertCircle, Loader2, Edit2
} from 'lucide-vue-next'
import FilePreviewModal from '@/components/common/FilePreviewModal.vue'
import {
  getDocumentList,
  uploadDocument,
  deleteDocument,
  renameDocument,
  downloadDocument,
  rebuildDocumentIndex,
  batchDeleteDocuments
} from '@/api/personalKnowledge'
import { useModal } from '@/composables/useModal'

const modal = useModal()

// 注入回收站刷新方法
const notifyRecycleBinRefresh = inject<() => void>('notifyRecycleBinRefresh')

// 类型定义
interface KBDocument {
  id: number
  originalFilename: string
  fileName: string
  fileExtension: string
  fileSize: number
  fileType: string
  mimeType: string
  createTime: string
  updateTime: string
  processingStatus?: string
}

// 状态
const viewMode = ref<'list' | 'grid'>('list')
const searchQuery = ref('')
const documents = ref<KBDocument[]>([])
const selectedDocs = ref<number[]>([])
const selectedTags = ref<string[]>([])
const showDetailDrawer = ref(false)
const selectedDocDetail = ref<KBDocument | null>(null)
const showUploadDialog = ref(false)
const showRenameDialog = ref(false)
const renamingDoc = ref<KBDocument | null>(null)
const newDocTitle = ref('')
const uploadFiles = ref<File[]>([])  
const fileInputRef = ref<HTMLInputElement | null>(null)
const loading = ref(false)
const uploading = ref(false)
const totalCount = ref(0)

// 文件预览相关状态
const showPreviewModal = ref(false)
const previewFile = ref<KBDocument | null>(null)

const currentPage = ref(1)
const pageSize = ref(15)

// 计算属性
const allTags = computed(() => {
  const tags = new Set<string>()
  documents.value.forEach(doc => {
    if (doc.fileType) tags.add(doc.fileType)
    if (doc.fileExtension) tags.add(doc.fileExtension.toUpperCase())
  })
  return Array.from(tags)
})

const filteredDocuments = computed(() => {
  return documents.value.filter(doc => {
    if (selectedTags.value.length > 0) {
      const docTags = [doc.fileType, doc.fileExtension?.toUpperCase()].filter(Boolean)
      if (!selectedTags.value.some(tag => docTags.includes(tag))) return false
    }
    if (searchQuery.value && !doc.originalFilename.toLowerCase().includes(searchQuery.value.toLowerCase())) return false
    return true
  })
})

watch([searchQuery, selectedTags], () => {
  currentPage.value = 1
})

const totalPages = computed(() => {
  if (filteredDocuments.value.length <= 0) return 0
  return Math.ceil(filteredDocuments.value.length / pageSize.value)
})

const pagedDocuments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredDocuments.value.slice(start, start + pageSize.value)
})

function handlePageChange() {
  // 页面数据在前端分页，不需要额外请求；确保滚动区域保持当前位置即可
}

// 加载文档列表
async function loadDocuments() {
  loading.value = true
  try {
    const res = await getDocumentList({
      page: 1,
      size: 100,
      keyword: searchQuery.value,
      sortBy: 'create_time',
      sortOrder: 'desc'
    })
    if (res.code === 0 && res.data) {
      documents.value = res.data.data || []
      totalCount.value = res.data.total || 0
      currentPage.value = 1
    } else {
      console.error('加载文档列表失败:', res.message)
    }
  } catch (error) {
    console.error('加载文档列表出错:', error)
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  loadDocuments()
})

// 方法
function formatFileSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

function formatDateTime(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) {
    uploadFiles.value = Array.from(target.files)
    showUploadDialog.value = true
  }
}

async function handleUpload() {
  if (uploadFiles.value.length === 0) return
  uploading.value = true
  
  try {
    for (const file of uploadFiles.value) {
      const res = await uploadDocument(file)
      if (res.code !== 0) {
        await modal.error(`文件 ${file.name} 上传失败: ${res.message}`)
      }
    }
    await modal.success('文件上传成功！')
    showUploadDialog.value = false
    uploadFiles.value = []
    // 刷新列表
    await loadDocuments()
  } catch (error) {
    console.error('上传失败:', error)
    await modal.error('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

function toggleDocSelection(docId: number) {
  if (selectedDocs.value.includes(docId)) {
    selectedDocs.value = selectedDocs.value.filter(id => id !== docId)
  } else {
    selectedDocs.value = [...selectedDocs.value, docId]
  }
}

function toggleSelectAll() {
  if (selectedDocs.value.length === filteredDocuments.value.length) {
    selectedDocs.value = []
  } else {
    selectedDocs.value = filteredDocuments.value.map(doc => doc.id)
  }
}

async function handleDeleteDocs() {
  if (selectedDocs.value.length === 0) return
  const confirmed = await modal.confirm({
    title: '确认删除',
    message: `确定要删除选中的 ${selectedDocs.value.length} 个文档吗？`,
    confirmText: '删除',
    cancelText: '取消',
    type: 'warning'
  })
  if (!confirmed) return
  
  try {
    const res = await batchDeleteDocuments(selectedDocs.value)
    if (res.code === 0) {
      await modal.success(`删除成功！成功: ${res.data?.successCount || selectedDocs.value.length} 个`)
      selectedDocs.value = []
      await loadDocuments()
      // 通知回收站刷新
      notifyRecycleBinRefresh?.()
    } else {
      await modal.error('删除失败: ' + res.message)
    }
  } catch (error) {
    console.error('批量删除失败:', error)
    await modal.error('删除失败，请重试')
  }
}

async function handleRebuildIndex() {
  if (selectedDocs.value.length === 0) return
  if (!confirm(`确定要重建选中的 ${selectedDocs.value.length} 个文档的索引吗？`)) return
  
  try {
    const res = await rebuildDocumentIndex(selectedDocs.value)
    if (res.code === 0) {
      await modal.success(`索引重建完成！成功: ${res.data?.successCount || 0} 个`)
      selectedDocs.value = []
      await loadDocuments()
    } else {
      await modal.error('重建索引失败: ' + res.message)
    }
  } catch (error) {
    console.error('重建索引失败:', error)
    await modal.error('重建索引失败，请重试')
  }
}

function viewDocDetail(doc: KBDocument) {
  selectedDocDetail.value = doc
  showDetailDrawer.value = true
}

// 打开文件预览
function openFilePreview(doc: KBDocument) {
  previewFile.value = doc
  showPreviewModal.value = true
}

// 从预览弹窗下载文件
function handlePreviewDownload(doc: KBDocument) {
  handleDownloadDoc(doc)
}

function openRenameDialog(doc: KBDocument) {
  renamingDoc.value = doc
  newDocTitle.value = doc.originalFilename
  showRenameDialog.value = true
}

async function handleRename() {
  if (!renamingDoc.value || !newDocTitle.value.trim()) return
  
  try {
    const res = await renameDocument(renamingDoc.value.id, newDocTitle.value.trim())
    if (res.code === 0) {
      await modal.success('重命名成功！')
      showRenameDialog.value = false
      renamingDoc.value = null
      newDocTitle.value = ''
      await loadDocuments()
    } else {
      await modal.error('重命名失败: ' + res.message)
    }
  } catch (error) {
    console.error('重命名失败:', error)
    await modal.error('重命名失败，请重试')
  }
}

async function handleDeleteSingleDoc(doc: KBDocument) {
  const confirmed = await modal.confirm({
    title: '确认删除',
    message: `确定要删除文档“${doc.originalFilename}”吗？`,
    confirmText: '删除',
    cancelText: '取消',
    type: 'warning'
  })
  if (!confirmed) return
  
  try {
    const res = await deleteDocument(doc.id)
    if (res.code === 0) {
      if (selectedDocDetail.value?.id === doc.id) {
        showDetailDrawer.value = false
        selectedDocDetail.value = null
      }
      await modal.success('删除成功！')
      await loadDocuments()
      // 通知回收站刷新
      notifyRecycleBinRefresh?.()
    } else {
      await modal.error('删除失败: ' + res.message)
    }
  } catch (error) {
    console.error('删除失败:', error)
    await modal.error('删除失败，请重试')
  }
}

async function handleDownloadDoc(doc: KBDocument) {
  try {
    await downloadDocument(doc.id, doc.originalFilename)
  } catch (error) {
    console.error('下载失败:', error)
    await modal.error('下载失败，请重试')
  }
}

function toggleTag(tag: string) {
  if (selectedTags.value.includes(tag)) {
    selectedTags.value = selectedTags.value.filter(t => t !== tag)
  } else {
    selectedTags.value = [...selectedTags.value, tag]
  }
}

function getFileTypeColor(fileType: string) {
  const colors: Record<string, string> = {
    'PDF': 'bg-red-500',
    'DOCUMENT': 'bg-blue-500',
    'pdf': 'bg-red-500',
    'doc': 'bg-blue-500',
    'docx': 'bg-blue-500',
    'txt': 'bg-gray-500',
    'md': 'bg-purple-500'
  }
  return colors[fileType] || 'bg-gray-500'
}
</script>

<template>
  <div class="flex-1 flex wc-page overflow-hidden">
    <!-- 左侧栏 -->
    <div class="w-60 wc-side flex flex-col shrink-0">
      <!-- 知识空间 -->
      <div class="px-3 pt-5 pb-4">
        <div class="flex items-center justify-between mb-2 px-1">
          <h3 class="wc-side-title">知识空间</h3>
          <Button variant="ghost" size="sm" class="h-7 w-7 p-0 text-gray-400" @click="loadDocuments" title="刷新"><RefreshCw class="w-3.5 h-3.5" /></Button>
        </div>
        <div class="wc-side-item is-active">
          <span>全部文档</span>
          <span class="text-xs tabular-nums opacity-70">{{ totalCount }}</span>
        </div>
      </div>

      <!-- 标签筛选 -->
      <div class="px-3 pb-4">
        <h3 class="wc-side-title mb-2 px-1">文件类型</h3>
        <div v-if="allTags.length" class="flex flex-wrap gap-1.5 px-1">
          <button v-for="tag in allTags" :key="tag" @click="toggleTag(tag)" :class="['px-2.5 py-1 rounded-full text-xs transition-colors', selectedTags.includes(tag) ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']">{{ tag }}</button>
        </div>
        <p v-else class="text-xs text-gray-400 px-1 leading-relaxed">上传文档后，这里会按文件类型自动分组</p>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- 头部栏 -->
      <div class="wc-page-head">
        <div class="flex items-center gap-3 w-full">
          <div class="flex-1 relative">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input v-model="searchQuery" @keypress.enter="loadDocuments" placeholder="搜索文档名、内容关键词…" class="w-full h-9 pl-9 pr-4 text-sm bg-gray-50 border border-gray-200 rounded-lg placeholder:text-gray-400 focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition" />
          </div>
          <input ref="fileInputRef" type="file" multiple @change="handleFileSelect" class="hidden" accept=".pdf,.md,.doc,.txt" />
          <Button class="h-9 px-4 shrink-0" @click="fileInputRef?.click()"><Upload class="w-4 h-4 mr-2" />上传文档</Button>
          <div class="flex gap-0.5 border border-gray-200 rounded-lg p-0.5 shrink-0">
            <button @click="viewMode = 'list'" :class="['p-1.5 rounded-md transition-colors', viewMode === 'list' ? 'bg-blue-50 text-blue-700' : 'text-gray-400 hover:bg-gray-50']"><List class="w-4 h-4" /></button>
            <button @click="viewMode = 'grid'" :class="['p-1.5 rounded-md transition-colors', viewMode === 'grid' ? 'bg-blue-50 text-blue-700' : 'text-gray-400 hover:bg-gray-50']"><Grid class="w-4 h-4" /></button>
          </div>
        </div>
      </div>

      <!-- 文档列表内容 -->
      <div class="flex-1 overflow-hidden flex flex-col">
        <div class="flex-1 overflow-y-auto px-8 pb-8">
          <!-- 加载状态 -->
          <div v-if="loading" class="flex items-center justify-center gap-3 py-16 text-gray-500">
            <Loader2 class="w-5 h-5 text-blue-600 animate-spin" />
            <span class="text-sm">加载中…</span>
          </div>
          
          <div v-else class="space-y-3">
            <!-- 批量操作栏 -->
            <div v-if="selectedDocs.length > 0" class="bg-blue-50 border border-blue-200 rounded-xl px-4 py-2.5 flex items-center justify-between">
              <span class="text-[13px] text-blue-900">已选择 {{ selectedDocs.length }} 个文档</span>
              <div class="flex gap-2">
                <Button size="sm" variant="outline" class="h-8" @click="handleRebuildIndex"><RefreshCw class="w-3.5 h-3.5 mr-1" />重建索引</Button>
                <Button size="sm" variant="outline" class="h-8 text-red-600 border-red-200 hover:bg-red-50" @click="handleDeleteDocs"><Trash2 class="w-3.5 h-3.5 mr-1" />删除</Button>
              </div>
            </div>
            
            <!-- 空状态 -->
            <div v-if="filteredDocuments.length === 0" class="wc-empty min-h-[70vh]">
              <div class="wc-empty-icon"><FileText /></div>
              <p class="wc-empty-title">还没有文档</p>
              <p class="wc-empty-desc">支持 PDF、Markdown、Word 与纯文本。上传后会自动切分并建立索引，AI 问答时就能引用这些内容。</p>
              <div class="wc-empty-action">
                <Button @click="fileInputRef?.click()"><Upload class="w-4 h-4 mr-2" />上传第一个文档</Button>
              </div>
            </div>

            <!-- 列表视图 -->
            <div v-if="viewMode === 'list' && filteredDocuments.length > 0" class="wc-panel overflow-hidden">
              <table class="wc-table">
                <thead>
                  <tr>
                    <th class="w-10"><input type="checkbox" :checked="selectedDocs.length === filteredDocuments.length && filteredDocuments.length > 0" @change="toggleSelectAll" class="w-4 h-4 text-blue-600 rounded align-middle" /></th>
                    <th>文件名</th>
                    <th class="w-24">类型</th>
                    <th class="w-24">大小</th>
                    <th class="w-40">上传时间</th>
                    <th class="w-36 !text-center">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="doc in pagedDocuments" :key="doc.id">
                    <td><input type="checkbox" :checked="selectedDocs.includes(doc.id)" @change="toggleDocSelection(doc.id)" class="w-4 h-4 text-blue-600 rounded align-middle" /></td>
                    <td>
                      <div class="flex items-center gap-2 min-w-0">
                        <FileText class="w-4 h-4 shrink-0 text-blue-600" />
                        <span class="text-[13.5px] font-medium text-gray-900 truncate">{{ doc.originalFilename }}</span>
                      </div>
                    </td>
                    <td>
                      <span :class="['px-2 py-0.5 rounded-full text-[11px] font-medium', getFileTypeColor(doc.fileExtension)]" class="text-white">{{ doc.fileExtension?.toUpperCase() || 'FILE' }}</span>
                    </td>
                    <td class="text-gray-500">{{ formatFileSize(doc.fileSize) }}</td>
                    <td class="text-gray-500">{{ formatDateTime(doc.createTime) }}</td>
                    <td>
                      <div class="flex items-center justify-center gap-0.5">
                        <Button variant="ghost" size="sm" class="h-7 w-7 p-0 text-gray-500" @click="openFilePreview(doc)" title="预览文件"><Eye class="w-3.5 h-3.5" /></Button>
                        <Button variant="ghost" size="sm" class="h-7 w-7 p-0 text-gray-500" @click="handleDownloadDoc(doc)" title="下载"><Download class="w-3.5 h-3.5" /></Button>
                        <Button variant="ghost" size="sm" class="h-7 w-7 p-0 text-gray-500" @click="openRenameDialog(doc)" title="重命名"><Edit2 class="w-3.5 h-3.5" /></Button>
                        <Button variant="ghost" size="sm" class="h-7 w-7 p-0 text-red-500 hover:bg-red-50" @click="handleDeleteSingleDoc(doc)" title="删除"><Trash2 class="w-3.5 h-3.5" /></Button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- 卡片视图 -->
            <div v-if="viewMode === 'grid' && filteredDocuments.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="doc in pagedDocuments" :key="doc.id" class="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-lg transition-shadow cursor-pointer" @click="openFilePreview(doc)">
                <div class="flex items-start justify-between mb-3">
                  <div class="flex items-center gap-2">
                    <input type="checkbox" :checked="selectedDocs.includes(doc.id)" @click.stop @change="toggleDocSelection(doc.id)" class="w-4 h-4 text-blue-600 rounded" />
                    <FileText class="w-5 h-5 text-blue-600" />
                  </div>
                  <span :class="['px-2 py-1 rounded-full text-xs text-white', getFileTypeColor(doc.fileExtension)]">{{ doc.fileExtension?.toUpperCase() || 'FILE' }}</span>
                </div>
                <h3 class="text-sm font-medium text-gray-900 mb-2 truncate">{{ doc.originalFilename }}</h3>
                <div class="flex items-center justify-between text-xs text-gray-500">
                  <span>{{ formatFileSize(doc.fileSize) }}</span>
                  <span>{{ formatDateTime(doc.createTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="totalPages > 0" class="flex justify-center py-4">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="filteredDocuments.length"
            size="small"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 右侧文档详情抽屉 -->
    <div v-if="showDetailDrawer && selectedDocDetail" class="w-96 bg-white border-l border-gray-200 flex flex-col">
      <div class="p-4 border-b border-gray-200 flex items-center justify-between">
        <h3 class="text-lg font-medium text-gray-900">文档详情</h3>
        <Button variant="ghost" size="sm" @click="showDetailDrawer = false"><X class="w-5 h-5" /></Button>
      </div>
      <div class="flex-1 overflow-y-auto p-4 space-y-4">
        <div>
          <h4 class="text-sm font-medium text-gray-700 mb-2">基本信息</h4>
          <div class="space-y-2 text-sm">
            <div><span class="text-gray-500">文件名：</span><span class="text-gray-900 ml-1">{{ selectedDocDetail.originalFilename }}</span></div>
            <div><span class="text-gray-500">文件类型：</span><span class="text-gray-900 ml-1">{{ selectedDocDetail.fileExtension?.toUpperCase() || '-' }}</span></div>
            <div><span class="text-gray-500">文件大小：</span><span class="text-gray-900 ml-1">{{ formatFileSize(selectedDocDetail.fileSize) }}</span></div>
            <div><span class="text-gray-500">MIME类型：</span><span class="text-gray-900 ml-1">{{ selectedDocDetail.mimeType || '-' }}</span></div>
            <div><span class="text-gray-500">上传时间：</span><span class="text-gray-900 ml-1">{{ formatDateTime(selectedDocDetail.createTime) }}</span></div>
            <div><span class="text-gray-500">更新时间：</span><span class="text-gray-900 ml-1">{{ formatDateTime(selectedDocDetail.updateTime) }}</span></div>
          </div>
        </div>
        <div class="space-y-2 pt-4 border-t border-gray-200">
          <Button class="w-full justify-start" variant="outline" @click="handleDownloadDoc(selectedDocDetail)"><Download class="w-4 h-4 mr-2" />下载文档</Button>
          <Button class="w-full justify-start" variant="outline" @click="openRenameDialog(selectedDocDetail)"><Edit2 class="w-4 h-4 mr-2" />重命名</Button>
          <Button class="w-full justify-start text-red-600" variant="outline" @click="handleDeleteSingleDoc(selectedDocDetail)"><Trash2 class="w-4 h-4 mr-2" />删除</Button>
        </div>
      </div>
    </div>

    <!-- 上传对话框 -->
    <div v-if="showUploadDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-2xl">
        <h2 class="text-lg font-medium text-gray-900 mb-4">上传文档</h2>
        <div class="space-y-4 mb-4">
          <div>
            <label class="block text-sm text-gray-700 mb-2">支持格式：PDF, Markdown, DOC, TXT</label>
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-2">已选择文件</label>
            <div class="space-y-2 max-h-60 overflow-y-auto">
              <div v-for="(file, index) in uploadFiles" :key="index" class="flex items-center justify-between bg-gray-50 px-3 py-2 rounded">
                <span class="text-sm text-gray-900 truncate">{{ file.name }}</span>
                <span class="text-xs text-gray-500 ml-2">{{ formatFileSize(file.size) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-3">
          <Button variant="outline" @click="showUploadDialog = false; uploadFiles = []">取消</Button>
          <Button @click="handleUpload" :disabled="uploading" class="bg-blue-600 hover:bg-blue-700 text-white">
            <Loader2 v-if="uploading" class="w-4 h-4 mr-2 animate-spin" />
            {{ uploading ? '上传中...' : '开始上传' }}
          </Button>
        </div>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <div v-if="showRenameDialog && renamingDoc" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-lg font-medium text-gray-900 mb-4">重命名文档</h2>
        <div class="space-y-4 mb-4">
          <div>
            <label class="block text-sm text-gray-700 mb-2">当前文件名</label>
            <span class="text-gray-900">{{ renamingDoc.originalFilename }}</span>
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-2">新文件名</label>
            <input type="text" v-model="newDocTitle" @keypress.enter="handleRename" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
        </div>
        <div class="flex justify-end gap-3">
          <Button variant="outline" @click="showRenameDialog = false; renamingDoc = null; newDocTitle = ''">取消</Button>
          <Button @click="handleRename" class="bg-blue-600 hover:bg-blue-700 text-white" :disabled="!newDocTitle.trim()">确认重命名</Button>
        </div>
      </div>
    </div>

    <!-- 文件预览弹窗 -->
    <FilePreviewModal
      v-model:visible="showPreviewModal"
      :file="previewFile"
      @download="handlePreviewDownload"
    />
  </div>
</template>
