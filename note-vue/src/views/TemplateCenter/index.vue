<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Grid3x3, List, Star, Download, Eye, Clock, TrendingUp, Plus, X } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import TemplateDetailPage from './TemplateDetail.vue'
import { getTemplates, getTemplateById, useTemplate, createTemplate, getMyTemplates, getFavoriteTemplates } from '@/api/template'
import { uploadFile } from '@/api/apiClient'
import { useModal } from '@/composables/useModal'

const modal = useModal()
const route = useRoute()
const router = useRouter()

// 禁用继承属性，避免多根节点警告
defineOptions({
  inheritAttrs: false
})

export interface Template {
  id: string
  title: string
  description: string
  category: string
  thumbnail: string
  downloads: number
  rating: number
  views: number
  author: string
  updatedAt: Date
  tags: string[]
}

const categories = [
  { id: 'all', name: '全部模板', icon: Grid3x3 },
  { id: 'favorites', name: '我的收藏', icon: Star },
  { id: 'work', name: '工作汇报', icon: TrendingUp },
  { id: 'study', name: '学习笔记', icon: List },
  { id: 'meeting', name: '会议记录', icon: Clock },
  { id: 'project', name: '项目管理', icon: Star },
]

const emit = defineEmits<{
  'select-template': [template: Template]
  'use-template': [template: Template]
}>()

const selectedCategory = ref('all')
const searchQuery = ref('')
const viewMode = ref<'grid' | 'list'>('grid')
const selectedTemplate = ref<Template | null>(null)
const templates = ref<Template[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(15)

const totalPages = computed(() => {
  const totalCount = total.value > 0 ? total.value : templates.value.length
  if (totalCount <= 0) return 0
  return Math.ceil(totalCount / pageSize.value)
})

// 上传模板相关
const showUploadModal = ref(false)
const uploading = ref(false)
const thumbnailUploading = ref(false)
const thumbnailInputRef = ref<HTMLInputElement | null>(null)
const newTemplate = ref({
  title: '',
  description: '',
  category: 'work',
  thumbnail: '',
  content: '',
  tags: [] as string[],
  isPublic: true
})
const tagInput = ref('')

// 加载模板列表
async function loadTemplates() {
  loading.value = true
  try {
    let res: any
    
    // 根据分类决定调用哪个接口
    if (selectedCategory.value === 'favorites') {
      // 获取收藏的模板
      const params = {
        page: currentPage.value,
        size: pageSize.value
      }
      res = await getFavoriteTemplates(params)
    } else {
      // 获取普通模板列表
      const params: Record<string, any> = {
        category: selectedCategory.value,
        page: currentPage.value,
        pageSize: pageSize.value
      }
      if (searchQuery.value) {
        params.keyword = searchQuery.value
      }
      res = await getTemplates(params)
    }
    
    // apiClient直接返回JSON对象，不是{data: ...}包装
    if (res && res.code === 0) {
      const pageResult = res.data
      const templateList = pageResult?.data || []
      
      templates.value = templateList.map((item: any) => ({
        ...item,
        updatedAt: item.updatedAt ? new Date(item.updatedAt) : new Date(),
        tags: item.tags || []
      }))
      total.value = pageResult?.total || 0
    }
  } catch (error) {
    console.error('加载模板失败:', error)
  } finally {
    loading.value = false
  }
}

// 过滤后的模板列表（前端过滤作为补充）
const filteredTemplates = computed(() => {
  // 如果是收藏列表，直接返回全部（已在后端过滤）
  if (selectedCategory.value === 'favorites') {
    return templates.value
  }
  
  return templates.value.filter(template => {
    const matchesCategory = selectedCategory.value === 'all' || template.category === selectedCategory.value
    const matchesSearch = !searchQuery.value || 
      template.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      template.description.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      template.tags.some(tag => tag.toLowerCase().includes(searchQuery.value.toLowerCase()))
    return matchesCategory && matchesSearch
  })
})

async function handleUseTemplate(template: Template) {
  try {
    // 调用后端接口增加下载次数
    await useTemplate(template.id)
  } catch (error) {
    console.error('使用模板失败:', error)
  }
  emit('use-template', template)
  selectedTemplate.value = null
}

function handleSelectTemplate(template: Template) {
  selectedTemplate.value = template
}

function handleBack() {
  selectedTemplate.value = null
  if (route.name === 'TemplateCenter' && route.params.id) {
    router.replace({ name: 'TemplateCenter' })
  }
}

// 分享链接直接打开模板详情；过期请求不能覆盖之后打开的模板。
watch(() => route.params.id, async (id, _, onCleanup) => {
  if (route.name !== 'TemplateCenter') return
  if (!id) {
    selectedTemplate.value = null
    return
  }
  let cancelled = false
  onCleanup(() => { cancelled = true })
  selectedTemplate.value = null
  try {
    const res = await getTemplateById(String(id)) as any
    if (cancelled) return
    if (res?.code === 0 && res.data) {
      selectedTemplate.value = {
        ...res.data,
        updatedAt: res.data.updatedAt ? new Date(res.data.updatedAt) : new Date(),
        tags: res.data.tags || []
      }
    } else {
      await modal.error(res?.message || '模板不存在或无法访问')
    }
  } catch (error) {
    if (!cancelled) await modal.error('加载分享模板失败，请稍后重试')
  }
}, { immediate: true })

// 监听分类变化，重新加载
watch(selectedCategory, () => {
  currentPage.value = 1
  loadTemplates()
})

// 防抖搜索
let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadTemplates()
  }, 300)
})

// 打开上传模板弹窗
function openUploadModal() {
  newTemplate.value = {
    title: '',
    description: '',
    category: 'work',
    thumbnail: '',
    content: '',
    tags: [],
    isPublic: true
  }
  tagInput.value = ''
  showUploadModal.value = true
}

// 关闭上传模板弹窗
function closeUploadModal() {
  showUploadModal.value = false
}

function triggerThumbnailSelect() {
  thumbnailInputRef.value?.click()
}

async function handleThumbnailSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  thumbnailUploading.value = true
  try {
    const res = await uploadFile('/api/common/file/upload', file, 'file') as any
    if (res && res.code === 0 && res.data?.fileUrl) {
      newTemplate.value.thumbnail = res.data.fileUrl
    } else {
      await modal.error(res?.message || '缩略图上传失败')
    }
  } catch (error: any) {
    console.error('上传缩略图失败:', error)
    await modal.error(error?.message || '缩略图上传失败，请重试')
  } finally {
    thumbnailUploading.value = false
    target.value = ''
  }
}

// 添加标签
function addTag() {
  const tag = tagInput.value.trim()
  if (tag && !newTemplate.value.tags.includes(tag)) {
    newTemplate.value.tags.push(tag)
  }
  tagInput.value = ''
}

// 删除标签
function removeTag(index: number) {
  newTemplate.value.tags.splice(index, 1)
}

// 提交模板
async function submitTemplate() {
  if (!newTemplate.value.title.trim()) {
    await modal.warning('请输入模板标题')
    return
  }
  if (!newTemplate.value.category) {
    await modal.warning('请选择分类')
    return
  }

  uploading.value = true
  try {
    const res = await createTemplate({
      title: newTemplate.value.title,
      description: newTemplate.value.description,
      category: newTemplate.value.category,
      thumbnail: newTemplate.value.thumbnail || '/placeholder-template.png',
      content: newTemplate.value.content,
      tags: newTemplate.value.tags,
      isPublic: newTemplate.value.isPublic
    }) as any
    // apiClient直接返回JSON对象
    if (res && res.code === 0) {
      await modal.success('模板上传成功！')
      closeUploadModal()
      loadTemplates()
    } else {
      await modal.error(res?.message || '上传失败')
    }
  } catch (error: any) {
    console.error('上传模板失败:', error)
    await modal.error(error?.message || '上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

// 组件加载时获取数据
onMounted(() => {
  loadTemplates()
  
  // 监听自定义事件：从相关模板点击切换
  window.addEventListener('select-template', (e: Event) => {
    const customEvent = e as CustomEvent
    if (customEvent.detail) {
      handleSelectTemplate(customEvent.detail)
    }
  })

  // __TEMP_PREVIEW__ 仅供本地视觉走查，上线前删除
  if (import.meta.env.DEV) {
    const tid = new URLSearchParams(location.search).get('__template')
    if (tid) {
      setTimeout(async () => {
        const res = await getTemplateById(tid) as any
        if (res?.code === 0 && res.data) {
          handleSelectTemplate({
            ...res.data,
            updatedAt: res.data.updatedAt ? new Date(res.data.updatedAt) : new Date(),
            tags: res.data.tags || []
          } as any)
        }
      }, 800)
    }
  }
})
</script>

<template>
  <div v-bind="$attrs" class="h-full">
    <!-- 模板详情页 -->
    <TemplateDetailPage
      v-if="selectedTemplate"
      :template="selectedTemplate"
      @back="handleBack"
      @use-template="handleUseTemplate"
    />

    <!-- 模板列表页 -->
    <div v-else class="h-full flex flex-col bg-gray-50">
      <!-- Header -->
      <div class="bg-white border-b border-gray-200 p-6">
        <div class="w-full">
          <h1 class="mb-4">模板中心</h1>
          <p class="text-gray-600 mb-6">
            精选优质模板，快速开始您的工作
          </p>

        <!-- Search bar -->
        <div class="flex gap-4 items-center">
          <div class="flex-1 relative">
            <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="搜索模板..."
              v-model="searchQuery"
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <div class="flex gap-2">
            <Button
              :variant="viewMode === 'grid' ? 'default' : 'ghost'"
              size="sm"
              @click="viewMode = 'grid'"
              class="px-3"
            >
              <Grid3x3 class="w-4 h-4" />
            </Button>
            <Button
              :variant="viewMode === 'list' ? 'default' : 'ghost'"
              size="sm"
              @click="viewMode = 'list'"
              class="px-3"
            >
              <List class="w-4 h-4" />
            </Button>
          </div>
          <!-- 上传模板按钮 -->
          <Button
            @click="openUploadModal"
            class="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2"
          >
            <Plus class="w-4 h-4" />
            上传模板
          </Button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-hidden">
      <div class="w-full h-full flex gap-6 p-6">
        <!-- Categories sidebar -->
        <div class="w-56 flex-shrink-0">
          <div class="bg-white rounded-lg border border-gray-200 p-4">
            <h3 class="mb-3 text-gray-700">分类</h3>
            <div class="space-y-1">
              <button
                v-for="category in categories"
                :key="category.id"
                @click="selectedCategory = category.id"
                :class="[
                  'w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors',
                  selectedCategory === category.id
                    ? 'bg-blue-50 text-blue-600'
                    : 'hover:bg-gray-50 text-gray-700'
                ]"
              >
                <component :is="category.icon" class="w-4 h-4" />
                <span class="text-sm">{{ category.name }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Templates grid/list -->
        <div class="flex-1 flex flex-col overflow-hidden">
          <div class="flex-1 overflow-y-auto">
            <div v-if="filteredTemplates.length === 0" class="h-full flex items-center justify-center text-gray-400">
              <div class="text-center">
                <Search class="w-16 h-16 mx-auto mb-4 text-gray-300" />
                <p>未找到相关模板</p>
              </div>
            </div>
            <div 
              v-else
              :class="[
                viewMode === 'grid'
                  ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6'
                  : 'space-y-4'
              ]"
            >
              <div
                v-for="template in filteredTemplates"
                :key="template.id"
                @click="handleSelectTemplate(template)"
                :class="[
                  'bg-white rounded-lg border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow cursor-pointer',
                  viewMode === 'list' ? 'flex' : ''
                ]"
              >
                <!-- Thumbnail -->
                <div :class="['relative', viewMode === 'list' ? 'w-48 flex-shrink-0' : 'w-full h-48']">
                  <img
                    :src="template.thumbnail"
                    :alt="template.title"
                    class="w-full h-full object-cover"
                  />
                  <div class="absolute top-2 right-2 flex gap-1">
                    <span class="px-2 py-1 bg-white/90 rounded text-xs flex items-center gap-1">
                      <Star class="w-3 h-3 text-yellow-500 fill-yellow-500" />
                      {{ template.rating }}
                    </span>
                  </div>
                </div>

                <!-- Content -->
                <div class="p-4 flex-1 flex flex-col">
                  <h3 class="mb-2 line-clamp-1">{{ template.title }}</h3>
                  <p class="text-sm text-gray-600 mb-3 line-clamp-2 flex-1">
                    {{ template.description }}
                  </p>

                  <!-- Tags -->
                  <div class="flex flex-wrap gap-1 mb-3">
                    <span
                      v-for="(tag, index) in template.tags"
                      :key="index"
                      class="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs"
                    >
                      {{ tag }}
                    </span>
                  </div>

                  <!-- Stats -->
                  <div class="flex items-center gap-4 text-xs text-gray-500 mb-3">
                    <span class="flex items-center gap-1">
                      <Download class="w-3 h-3" />
                      {{ template.downloads }}
                    </span>
                    <span class="flex items-center gap-1">
                      <Eye class="w-3 h-3" />
                      {{ template.views }}
                    </span>
                  </div>

                  <!-- Footer -->
                  <div class="flex items-center justify-between pt-3 border-t border-gray-100">
                    <span class="text-xs text-gray-500">
                      by {{ template.author }}
                    </span>
                    <Button
                      size="sm"
                      @click.stop="handleUseTemplate(template)"
                      class="bg-blue-600 hover:bg-blue-700 text-white"
                    >
                      使用模板
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="totalPages > 0" class="flex justify-center py-4">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              size="small"
              layout="prev, pager, next"
              @current-change="loadTemplates"
            />
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 上传模板弹窗 -->
  <Teleport to="body">
    <div v-if="showUploadModal" class="fixed inset-0 z-50 flex items-center justify-center">
      <!-- 背景遮罩 -->
      <div class="absolute inset-0 bg-black/50" @click="closeUploadModal"></div>
      
      <!-- 弹窗内容 -->
      <div class="relative bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto m-4">
        <!-- 标题栏 -->
        <div class="flex items-center justify-between p-4 border-b">
          <h2 class="text-lg font-semibold">上传模板</h2>
          <button @click="closeUploadModal" class="p-1 hover:bg-gray-100 rounded">
            <X class="w-5 h-5 text-gray-500" />
          </button>
        </div>
        
        <!-- 表单内容 -->
        <div class="p-6 space-y-4">
          <!-- 标题 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">模板标题 *</label>
            <input
              v-model="newTemplate.title"
              type="text"
              placeholder="请输入模板标题"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          
          <!-- 分类 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">分类 *</label>
            <select
              v-model="newTemplate.category"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="work">工作汇报</option>
              <option value="study">学习笔记</option>
              <option value="meeting">会议记录</option>
              <option value="project">项目管理</option>
            </select>
          </div>
          
          <!-- 描述 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
            <textarea
              v-model="newTemplate.description"
              placeholder="请输入模板描述"
              rows="3"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            ></textarea>
          </div>
          
          <!-- 缩略图上传 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">缩略图</label>
            <div class="flex items-center gap-3">
              <input
                ref="thumbnailInputRef"
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleThumbnailSelect"
              />
              <Button
                variant="outline"
                size="sm"
                :disabled="thumbnailUploading"
                @click="triggerThumbnailSelect"
              >
                {{ thumbnailUploading ? '上传中...' : '选择图片' }}
              </Button>
              <span class="text-xs text-gray-500">支持 jpg/png/gif</span>
            </div>
            <div v-if="newTemplate.thumbnail" class="mt-3">
              <img
                :src="newTemplate.thumbnail"
                alt="缩略图预览"
                class="w-40 h-24 object-cover rounded border border-gray-200"
              />
            </div>
          </div>
          
          <!-- 模板内容 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">模板内容</label>
            <textarea
              v-model="newTemplate.content"
              placeholder="请输入模板内容（支持Markdown格式）"
              rows="6"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
            ></textarea>
          </div>
          
          <!-- 标签 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">标签</label>
            <div class="flex gap-2 mb-2">
              <input
                v-model="tagInput"
                type="text"
                placeholder="输入标签后回车"
                @keyup.enter="addTag"
                class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <Button @click="addTag" variant="outline" size="sm">添加</Button>
            </div>
            <div class="flex flex-wrap gap-2">
              <span
                v-for="(tag, index) in newTemplate.tags"
                :key="index"
                class="px-2 py-1 bg-blue-100 text-blue-700 rounded flex items-center gap-1"
              >
                {{ tag }}
                <button @click="removeTag(index)" class="hover:text-blue-900">
                  <X class="w-3 h-3" />
                </button>
              </span>
            </div>
          </div>
          
          <!-- 是否公开 -->
          <div class="flex items-center gap-2">
            <input
              v-model="newTemplate.isPublic"
              type="checkbox"
              id="isPublic"
              class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
            />
            <label for="isPublic" class="text-sm text-gray-700">公开模板（其他用户可见）</label>
          </div>
        </div>
        
        <!-- 底部按钮 -->
        <div class="flex justify-end gap-3 p-4 border-t">
          <Button variant="outline" @click="closeUploadModal">取消</Button>
          <Button
            @click="submitTemplate"
            :disabled="uploading"
            class="bg-blue-600 hover:bg-blue-700 text-white"
          >
            {{ uploading ? '上传中...' : '确认上传' }}
          </Button>
        </div>
      </div>
    </div>
  </Teleport>
  </div>
</template>
