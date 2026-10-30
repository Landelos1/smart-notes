<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import { Trash2, RotateCcw, FileText, StickyNote, Search, X, AlertTriangle } from 'lucide-vue-next'
import { getTrashList, getTrashStats, restoreTrashItem, permanentDeleteTrashItem, emptyTrash } from '@/api/trash'
import { useModal } from '@/composables/useModal'

const modal = useModal()

// 定义 emit
const emit = defineEmits<{
  'item-restored': []
}>()

interface TrashItem {
  id: number
  trashType: number
  typeName: string
  sourceId: number
  title: string
  deleteTime: string
  restoreDeadline: string
  daysLeft: number
  originalCreateTime: string
  notebookId?: number
  notebookName?: string
  summary?: string
  fileExtension?: string
  fileSize?: number
  fileSizeFormatted?: string
  fileType?: string
}

interface TrashStats {
  total: number
  noteCount: number
  fileCount: number
}

// 状态管理
const loading = ref(false)
const trashItems = ref<TrashItem[]>([])
const stats = ref<TrashStats>({ total: 0, noteCount: 0, fileCount: 0 })
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const trashTypeFilter = ref<number | null>(null)

// 确认弹窗状态
const showConfirmDialog = ref(false)
const confirmAction = ref<'restore' | 'delete' | 'empty'>('delete')
const confirmItemId = ref<number | null>(null)
const confirmItemTitle = ref('')

const totalPages = computed(() => {
  if (total.value <= 0) return 0
  return Math.ceil(total.value / pageSize.value)
})

// 加载回收站列表
async function loadTrashList() {
  loading.value = true
  try {
    const response = await getTrashList({
      page: currentPage.value,
      size: pageSize.value,
      trashType: trashTypeFilter.value || undefined,
      keyword: keyword.value || undefined
    })
    if (response.code === 0 && response.data) {
      trashItems.value = response.data.data || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('加载回收站列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载统计信息
async function loadStats() {
  try {
    const response = await getTrashStats()
    if (response.code === 0 && response.data) {
      stats.value = response.data
    }
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 显示确认弹窗
function showConfirm(action: 'restore' | 'delete' | 'empty', id?: number, title?: string) {
  confirmAction.value = action
  confirmItemId.value = id || null
  confirmItemTitle.value = title || ''
  showConfirmDialog.value = true
}

// 取消确认
function cancelConfirm() {
  showConfirmDialog.value = false
  confirmItemId.value = null
  confirmItemTitle.value = ''
}

// 执行确认操作
async function executeConfirm() {
  try {
    if (confirmAction.value === 'restore' && confirmItemId.value) {
      await handleRestore(confirmItemId.value)
    } else if (confirmAction.value === 'delete' && confirmItemId.value) {
      await handlePermanentDelete(confirmItemId.value)
    } else if (confirmAction.value === 'empty') {
      await handleEmptyTrash()
    }
  } finally {
    cancelConfirm()
  }
}

// 恢复项目
async function handleRestore(id: number) {
  try {
    const response = await restoreTrashItem(id)
    if (response.code === 0) {
      await loadTrashList()
      await loadStats()
      // 通知父组件刷新笔记列表
      emit('item-restored')
      await modal.success('恢复成功')
    } else {
      await modal.error(response.message || '恢复失败')
    }
  } catch (error) {
    console.error('恢复失败:', error)
    await modal.error('恢复失败，请稍后重试')
  }
}

// 彻底删除
async function handlePermanentDelete(id: number) {
  try {
    const response = await permanentDeleteTrashItem(id)
    if (response.code === 0) {
      await loadTrashList()
      await loadStats()
      await modal.success('删除成功')
    } else {
      await modal.error(response.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    await modal.error('删除失败，请稍后重试')
  }
}

// 清空回收站
async function handleEmptyTrash() {
  try {
    const response = await emptyTrash()
    if (response.code === 0) {
      await loadTrashList()
      await loadStats()
      currentPage.value = 1
      await modal.success('回收站已清空')
    } else {
      await modal.error(response.message || '清空失败')
    }
  } catch (error) {
    console.error('清空失败:', error)
    await modal.error('清空失败，请稍后重试')
  }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadTrashList()
}

// 类型筛选
function handleTypeFilter(type: number | null) {
  trashTypeFilter.value = type
  currentPage.value = 1
  loadTrashList()
}

// 格式化日期
function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 监听分页变化
watch(currentPage, () => {
  loadTrashList()
})

// 初始化
onMounted(() => {
  loadTrashList()
  loadStats()
})
</script>

<template>
  <div class="flex-1 h-full wc-page flex flex-col overflow-hidden">
    <!-- 顶部标题栏 -->
    <div class="wc-page-head">
      <div>
        <h1 class="wc-page-title">
          <Trash2 /> 内容回收站
        </h1>
        <p class="wc-page-sub">已删除的笔记与文件会在这里保留 30 天，期间随时可以恢复</p>
      </div>
      <Button 
        variant="outline" 
        @click="showConfirm('empty')"
        :disabled="stats.total === 0"
        class="h-9 shrink-0 text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700 disabled:opacity-40"
      >
        <Trash2 class="w-4 h-4 mr-2" />
        清空回收站
      </Button>
    </div>

    <!-- 内容区域 -->
    <div class="wc-page-body">
      <div class="wc-panel flex flex-col overflow-hidden">
        <!-- 卡片头部 -->
        <div class="wc-panel-head">
          <div class="flex items-center justify-between w-full">
            <div>
              <h2 class="wc-panel-title">已删除项目 <span class="text-gray-400 font-normal">{{ stats.total }}</span></h2>
              <p class="wc-panel-sub">笔记 {{ stats.noteCount }} 个 · 文件 {{ stats.fileCount }} 个</p>
            </div>
            <!-- 筛选和搜索 -->
            <div class="flex items-center gap-3">
              <!-- 类型筛选 -->
              <div class="flex items-center gap-1.5">
                <Button 
                  size="sm" 
                  variant="ghost"
                  @click="handleTypeFilter(null)"
                  :class="['h-8 px-3', trashTypeFilter === null ? 'bg-blue-50 text-blue-700 font-medium' : 'text-gray-500']"
                >
                  全部
                </Button>
                <Button 
                  size="sm" 
                  variant="ghost"
                  @click="handleTypeFilter(1)"
                  :class="['h-8 px-3', trashTypeFilter === 1 ? 'bg-blue-50 text-blue-700 font-medium' : 'text-gray-500']"
                >
                  <StickyNote class="w-3 h-3 mr-1" />
                  笔记
                </Button>
                <Button 
                  size="sm" 
                  variant="ghost"
                  @click="handleTypeFilter(2)"
                  :class="['h-8 px-3', trashTypeFilter === 2 ? 'bg-blue-50 text-blue-700 font-medium' : 'text-gray-500']"
                >
                  <FileText class="w-3 h-3 mr-1" />
                  文件
                </Button>
              </div>
              <!-- 搜索框 -->
              <div class="relative">
                <input
                  v-model="keyword"
                  type="text"
                  placeholder="搜索标题 / 文件名"
                  class="h-9 w-56 pl-9 pr-3 text-sm bg-gray-50 border border-gray-200 rounded-lg placeholder:text-gray-400 focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                  @keyup.enter="handleSearch"
                />
                <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
              </div>
            </div>
          </div>
        </div>
        
        <!-- 卡片内容 -->
        <div class="flex flex-col">
          <div class="p-4">
          <!-- 加载状态 -->
          <div v-if="loading" class="flex items-center justify-center gap-3 py-16 text-gray-500">
            <div class="animate-spin rounded-full h-5 w-5 border-2 border-gray-200 border-t-blue-600"></div>
            <span class="text-sm">加载中…</span>
          </div>
          
          <!-- 空状态 -->
          <div v-else-if="trashItems.length === 0" class="wc-empty">
            <div class="wc-empty-icon"><Trash2 /></div>
            <p class="wc-empty-title">回收站是空的</p>
            <p class="wc-empty-desc">删除的笔记和文件会先移到这里，保留 30 天，期间随时可以恢复。</p>
          </div>
          
          <!-- 表格 -->
          <table v-else class="wc-table">
            <thead>
              <tr>
                <th>名称</th>
                <th class="w-24">类型</th>
                <th class="w-32">删除日期</th>
                <th class="w-28">剩余天数</th>
                <th class="w-40 !text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in trashItems" :key="item.id">
                <td>
                  <div class="flex items-center gap-2.5 min-w-0 overflow-hidden">
                    <StickyNote v-if="item.trashType === 1" class="w-4 h-4 shrink-0 text-blue-500" />
                    <FileText v-else class="w-4 h-4 shrink-0 text-orange-500" />
                    <span class="block min-w-0 truncate font-medium text-gray-900">{{ item.title }}</span>
                    <span v-if="item.fileSizeFormatted" class="block shrink-0 text-xs text-gray-400">{{ item.fileSizeFormatted }}</span>
                  </div>
                </td>
                <td>
                  <Badge variant="secondary" class="bg-gray-100 text-gray-700 font-normal">{{ item.typeName }}</Badge>
                </td>
                <td class="text-gray-500">{{ formatDate(item.deleteTime) }}</td>
                <td>
                  <span :class="[
                    'text-[13px] font-medium',
                    item.daysLeft <= 7 ? 'text-red-600' : item.daysLeft <= 14 ? 'text-orange-600' : 'text-gray-500'
                  ]">
                    {{ item.daysLeft }} 天
                  </span>
                </td>
                <td class="!text-right whitespace-nowrap">
                  <Button 
                    size="sm" 
                    variant="ghost" 
                    class="h-7 px-2 text-blue-600 hover:bg-blue-50"
                    @click="showConfirm('restore', item.id, item.title)"
                  >
                    <RotateCcw class="w-3.5 h-3.5 mr-1" />
                    恢复
                  </Button>
                  <Button 
                    size="sm" 
                    variant="ghost" 
                    class="h-7 px-2 text-red-600 hover:bg-red-50"
                    @click="showConfirm('delete', item.id, item.title)"
                  >
                    <X class="w-3.5 h-3.5 mr-1" />
                    删除
                  </Button>
                </td>
              </tr>
            </tbody>
          </table>
          </div>

          <div v-if="totalPages > 1" class="flex justify-center py-4 border-t border-gray-100">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              size="small"
              layout="prev, pager, next"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 确认弹窗 -->
    <div v-if="showConfirmDialog" class="fixed inset-0 flex items-center justify-center z-50" style="background-color: rgba(36, 27, 20, 0.42)">
      <div class="bg-white rounded-xl border border-gray-200 p-6 max-w-sm w-full mx-4" style="box-shadow: 0 24px 60px -16px rgba(36, 27, 20, 0.35)">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-9 h-9 rounded-full bg-yellow-50 grid place-items-center shrink-0">
            <AlertTriangle class="w-[18px] h-[18px] text-yellow-600" />
          </div>
          <h3 class="text-[16px] font-semibold text-gray-900">
            {{ confirmAction === 'restore' ? '确认恢复' : confirmAction === 'delete' ? '确认删除' : '确认清空' }}
          </h3>
        </div>
        <p class="text-[13.5px] leading-relaxed text-gray-500 mb-6 pl-12">
          <template v-if="confirmAction === 'restore'">
            确定要恢复「{{ confirmItemTitle }}」吗？
          </template>
          <template v-else-if="confirmAction === 'delete'">
            确定要彻底删除「{{ confirmItemTitle }}」吗？此操作无法撤销。
          </template>
          <template v-else>
            确定要清空回收站吗？所有项目将被永久删除，此操作无法撤销。
          </template>
        </p>
        <div class="flex justify-end gap-2">
          <Button variant="outline" @click="cancelConfirm">取消</Button>
          <Button 
            :variant="confirmAction === 'restore' ? 'default' : 'destructive'"
            :class="confirmAction === 'restore' ? '' : 'bg-red-600 hover:bg-red-700'"
            @click="executeConfirm"
          >
            确定
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>
