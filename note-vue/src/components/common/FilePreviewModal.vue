<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed, shallowRef, markRaw, nextTick } from 'vue'
import { X, Loader2, FileText, Download, ZoomIn, ZoomOut, ChevronLeft, ChevronRight, AlertCircle } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { getFilePreviewData } from '@/api/personalKnowledge'
import * as pdfjsLib from 'pdfjs-dist'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import VueOfficeDocx from '@vue-office/docx'
import VueOfficeExcel from '@vue-office/excel'
import VueOfficePptx from '@vue-office/pptx'

import '@vue-office/docx/lib/index.css'
import '@vue-office/excel/lib/index.css'

// 设置 PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.mjs',
  import.meta.url
).href

import type { PreviewFile as FileDoc } from '@/types/api'

const props = defineProps<{
  visible: boolean
  file: FileDoc | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'download': [file: FileDoc]
}>()

// 状态
const loading = ref(false)
const error = ref('')
const pdfCanvasRef = ref<HTMLCanvasElement | null>(null)
const wordContentRef = ref<HTMLDivElement | null>(null)
const textContentRef = ref<HTMLDivElement | null>(null)

// PDF 相关状态
const pdfDoc = shallowRef<any>(null)
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1.2)

const officeSrc = shallowRef<ArrayBuffer | string | null>(null)
const imageObjectUrl = ref<string>('')
const markdownHtmlContent = ref('')

// 内容
const wordHtmlContent = ref('')
const textContent = ref('')

// 计算文件类型
const fileType = computed(() => {
  if (!props.file) return 'unknown'
  const ext = props.file.fileExtension?.toLowerCase() || ''
  if (ext === 'pdf') return 'pdf'
  if (ext === 'docx') return 'docx'
  if (ext === 'xlsx' || ext === 'xls') return 'excel'
  if (ext === 'pptx' || ext === 'ppt') return 'pptx'
  if (ext === 'png' || ext === 'jpg' || ext === 'jpeg' || ext === 'gif' || ext === 'bmp' || ext === 'webp') return 'image'
  if (ext === 'md') return 'markdown'
  if (ext === 'txt' || ext === 'md') return 'text'
  return 'unknown'
})

const isZoomable = computed(() => {
  return fileType.value === 'pdf' || fileType.value === 'text' || fileType.value === 'markdown' || fileType.value === 'image'
})

// 关闭弹窗
function closeModal() {
  emit('update:visible', false)
  resetState()
}

// 重置状态
function resetState() {
  loading.value = false
  error.value = ''
  pdfDoc.value = null
  currentPage.value = 1
  totalPages.value = 0
  scale.value = 1.2
  officeSrc.value = null
  if (imageObjectUrl.value) {
    URL.revokeObjectURL(imageObjectUrl.value)
    imageObjectUrl.value = ''
  }
  markdownHtmlContent.value = ''
  wordHtmlContent.value = ''
  textContent.value = ''
}

// 加载预览
async function loadPreview() {
  if (!props.file) return
  
  loading.value = true
  error.value = ''
  
  try {
    const { data, contentType } = await getFilePreviewData(props.file.id)
    
    switch (fileType.value) {
      case 'pdf':
        await loadPdf(data)
        break
      case 'docx':
        await loadWord(data)
        break
      case 'excel':
        officeSrc.value = data
        break
      case 'pptx':
        officeSrc.value = data
        break
      case 'text':
        loadText(data)
        break
      case 'markdown':
        loadMarkdown(data)
        break
      case 'image':
        loadImage(data, contentType)
        break
      default:
        error.value = '不支持预览此文件类型'
    }
  } catch (err: any) {
    console.error('加载预览失败:', err)
    error.value = err.message || '加载预览失败'
  } finally {
    loading.value = false
  }
}

// 加载 PDF
async function loadPdf(arrayBuffer: ArrayBuffer) {
  try {
    const loadingTask = pdfjsLib.getDocument({ data: arrayBuffer })
    pdfDoc.value = markRaw(await loadingTask.promise)
    totalPages.value = pdfDoc.value.numPages
    currentPage.value = Math.min(Math.max(props.file?.page || 1, 1), totalPages.value)
    await nextTick()
    await renderPdfPage()
  } catch (err) {
    console.error('PDF 加载失败:', err)
    throw new Error('PDF 文件加载失败')
  }
}

// 渲染 PDF 页面
async function renderPdfPage() {
  if (!pdfDoc.value || !pdfCanvasRef.value) return
  
  try {
    const page = await pdfDoc.value.getPage(currentPage.value)
    const viewport = page.getViewport({ scale: scale.value })
    
    const canvas = pdfCanvasRef.value
    const context = canvas.getContext('2d')
    
    canvas.height = viewport.height
    canvas.width = viewport.width
    
    await page.render({
      canvasContext: context,
      viewport: viewport
    }).promise
  } catch (err) {
    console.error('PDF 页面渲染失败:', err)
  }
}

// PDF 翻页
function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    renderPdfPage()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    renderPdfPage()
  }
}

// 缩放
function zoomIn() {
  scale.value = Math.min(scale.value + 0.2, 3)
  if (fileType.value === 'pdf') {
    renderPdfPage()
  }
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.2, 0.5)
  if (fileType.value === 'pdf') {
    renderPdfPage()
  }
}

// 加载 Word 文档
async function loadWord(arrayBuffer: ArrayBuffer) {
  try {
    officeSrc.value = arrayBuffer
  } catch (err) {
    console.error('Word 文档加载失败:', err)
    throw new Error('Word 文档加载失败')
  }
}

function loadMarkdown(arrayBuffer: ArrayBuffer) {
  try {
    const decoder = new TextDecoder('utf-8')
    const md = decoder.decode(arrayBuffer)
    markdownHtmlContent.value = DOMPurify.sanitize(marked.parse(md) as string)
  } catch (err) {
    console.error('Markdown 文件加载失败:', err)
    throw new Error('Markdown 文件加载失败')
  }
}

function loadImage(arrayBuffer: ArrayBuffer, contentType: string) {
  try {
    if (imageObjectUrl.value) {
      URL.revokeObjectURL(imageObjectUrl.value)
      imageObjectUrl.value = ''
    }
    const blob = new Blob([arrayBuffer], { type: contentType || 'application/octet-stream' })
    imageObjectUrl.value = URL.createObjectURL(blob)
  } catch (err) {
    console.error('图片文件加载失败:', err)
    throw new Error('图片文件加载失败')
  }
}

function officeRenderedHandler() {
  // noop
}

function officeErrorHandler(e: any) {
  console.error('Office 文件渲染失败:', e)
  error.value = '文件预览失败'
}

// 加载文本文件
function loadText(arrayBuffer: ArrayBuffer) {
  try {
    const decoder = new TextDecoder('utf-8')
    textContent.value = decoder.decode(arrayBuffer)
  } catch (err) {
    console.error('文本文件加载失败:', err)
    throw new Error('文本文件加载失败')
  }
}

// 下载文件
function handleDownload() {
  if (props.file) {
    emit('download', props.file)
  }
}

// 监听 visible 变化
watch(() => props.visible, (newVal) => {
  if (newVal && props.file) {
    loadPreview()
  } else {
    resetState()
  }
})

// 监听文件变化
watch(() => props.file, (newFile) => {
  if (props.visible && newFile) {
    loadPreview()
  }
})

// ESC 键关闭
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible) {
    closeModal()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
      <!-- 遮罩 -->
      <div class="absolute inset-0 bg-black/60" @click="closeModal"></div>
      
      <!-- 弹窗内容 -->
      <div class="relative bg-white rounded-lg shadow-2xl w-[90vw] max-w-5xl h-[85vh] flex flex-col overflow-hidden">
        <!-- 头部 -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 bg-gray-50">
          <div class="flex items-center gap-3">
            <FileText class="w-5 h-5 text-blue-600" />
            <h3 class="text-lg font-medium text-gray-900 truncate max-w-md">{{ file?.originalFilename || '文件预览' }}</h3>
          </div>
          <div class="flex items-center gap-2">
            <!-- 缩放控制 -->
            <div v-if="isZoomable" class="flex items-center gap-1 mr-4">
              <Button variant="ghost" size="sm" @click="zoomOut" title="缩小">
                <ZoomOut class="w-4 h-4" />
              </Button>
              <span class="text-sm text-gray-600 min-w-[60px] text-center">{{ Math.round(scale * 100) }}%</span>
              <Button variant="ghost" size="sm" @click="zoomIn" title="放大">
                <ZoomIn class="w-4 h-4" />
              </Button>
            </div>
            <Button variant="outline" size="sm" @click="handleDownload">
              <Download class="w-4 h-4 mr-2" />下载
            </Button>
            <Button variant="ghost" size="sm" @click="closeModal">
              <X class="w-5 h-5" />
            </Button>
          </div>
        </div>
        
        <!-- 内容区域 -->
        <div class="flex-1 overflow-auto bg-gray-100 p-4">
          <div v-if="file?.quote" class="mx-auto mb-4 max-w-4xl rounded-xl border border-blue-100 bg-blue-50 px-4 py-3">
            <div class="mb-1 flex items-center justify-between gap-3">
              <span class="text-xs font-semibold text-blue-800">检索到的原文</span>
              <span v-if="file.page" class="shrink-0 text-xs text-blue-700">第 {{ file.page }} 页</span>
            </div>
            <p class="whitespace-pre-wrap break-words text-sm leading-6 text-gray-700">{{ file.quote }}</p>
          </div>
          <!-- 加载状态 -->
          <div v-if="loading" class="flex flex-col items-center justify-center h-full">
            <Loader2 class="w-12 h-12 text-blue-600 animate-spin mb-4" />
            <p class="text-gray-600">正在加载预览...</p>
          </div>
          
          <!-- 错误状态 -->
          <div v-else-if="error" class="flex flex-col items-center justify-center h-full">
            <AlertCircle class="w-16 h-16 text-red-400 mb-4" />
            <p class="text-red-600 mb-4">{{ error }}</p>
            <Button variant="outline" @click="loadPreview">重试</Button>
          </div>
          
          <!-- PDF 预览 -->
          <div v-else-if="fileType === 'pdf'" class="flex flex-col items-center">
            <div class="bg-white shadow-lg rounded-lg overflow-hidden">
              <canvas ref="pdfCanvasRef"></canvas>
            </div>
          </div>
          
          <!-- docx 预览 -->
          <div v-else-if="fileType === 'docx'" class="bg-white shadow-lg rounded-lg overflow-hidden mx-auto h-full">
            <VueOfficeDocx
              :src="officeSrc"
              style="height: 100%;"
              @rendered="officeRenderedHandler"
              @error="officeErrorHandler"
            />
          </div>

          <!-- excel 预览 -->
          <div v-else-if="fileType === 'excel'" class="bg-white shadow-lg rounded-lg overflow-hidden mx-auto h-full">
            <VueOfficeExcel
              :src="officeSrc"
              style="height: 100%;"
              @rendered="officeRenderedHandler"
              @error="officeErrorHandler"
            />
          </div>

          <!-- pptx 预览 -->
          <div v-else-if="fileType === 'pptx'" class="bg-white shadow-lg rounded-lg overflow-hidden mx-auto h-full">
            <VueOfficePptx
              :src="officeSrc"
              style="height: 100%;"
              @rendered="officeRenderedHandler"
              @error="officeErrorHandler"
            />
          </div>

          <!-- 图片预览 -->
          <div v-else-if="fileType === 'image'" class="flex items-center justify-center">
            <img
              v-if="imageObjectUrl"
              :src="imageObjectUrl"
              class="max-w-full h-auto bg-white shadow-lg rounded-lg"
              :style="{ transform: `scale(${scale})`, transformOrigin: 'center center' }"
              alt="preview"
            />
          </div>

          <!-- Markdown 预览 -->
          <div v-else-if="fileType === 'markdown'" class="bg-white shadow-lg rounded-lg p-8 max-w-4xl mx-auto">
            <div
              class="prose prose-sm max-w-none"
              :style="{ fontSize: `${scale}em` }"
              v-html="markdownHtmlContent"
            ></div>
          </div>
          
          <!-- 文本预览 -->
          <div v-else-if="fileType === 'text'" class="bg-white shadow-lg rounded-lg p-6 max-w-4xl mx-auto">
            <pre 
              ref="textContentRef" 
              class="whitespace-pre-wrap font-mono text-sm text-gray-800"
              :style="{ fontSize: `${scale}em` }"
            >{{ textContent }}</pre>
          </div>
          
          <!-- 不支持的类型 -->
          <div v-else class="flex flex-col items-center justify-center h-full">
            <FileText class="w-16 h-16 text-gray-400 mb-4" />
            <p class="text-gray-600 mb-2">不支持预览此文件类型</p>
            <p class="text-sm text-gray-500 mb-4">请下载后使用其他软件打开</p>
            <Button variant="outline" @click="handleDownload">
              <Download class="w-4 h-4 mr-2" />下载文件
            </Button>
          </div>
        </div>
        
        <!-- PDF 底部分页控制 -->
        <div v-if="fileType === 'pdf' && totalPages > 0 && !loading && !error" 
             class="flex items-center justify-center gap-4 px-6 py-3 border-t border-gray-200 bg-gray-50">
          <Button variant="outline" size="sm" @click="prevPage" :disabled="currentPage <= 1">
            <ChevronLeft class="w-4 h-4" />
          </Button>
          <span class="text-sm text-gray-600">
            第 {{ currentPage }} 页 / 共 {{ totalPages }} 页
          </span>
          <Button variant="outline" size="sm" @click="nextPage" :disabled="currentPage >= totalPages">
            <ChevronRight class="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.word-content :deep(table) {
  @apply border-collapse w-full my-4;
}
.word-content :deep(td),
.word-content :deep(th) {
  @apply border border-gray-300 px-3 py-2;
}
.word-content :deep(img) {
  @apply max-w-full h-auto;
}
</style>
