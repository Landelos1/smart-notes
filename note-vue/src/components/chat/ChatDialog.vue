<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import Button from '@/components/ui/Button.vue'
import { Send, Bot, User, Sparkles, Paperclip, Square, Upload as UploadIcon, BookmarkPlus, X, Check, FileText, ExternalLink } from 'lucide-vue-next'
import { formatRelativeTime } from '@/utils/timeFormat'
import type { Message } from '@/types/chat'
import { marked } from 'marked'
import { uploadDocument } from '@/api/personalKnowledge'
import { getNotebooks, createNote } from '@/api/notes'
import FilePreviewModal from '@/components/common/FilePreviewModal.vue'

interface Props {
  messages: Message[]
  isLoading?: boolean
  conversationTitle?: string  // 对话标题，用于保存笔记时的默认标题
  agentId?: string           // 当前选中的智能体ID
  useKnowledgeBase?: boolean // 是否使用知识库增强
}

const props = withDefaults(defineProps<Props>(), {
  isLoading: false,
  conversationTitle: '',
  agentId: '',
  useKnowledgeBase: false
})

const emit = defineEmits<{
  'send-message': [content: string]
  'stop': []
  'files-uploaded': []
  'note-saved': [] // 笔记保存成功事件
  'update:agent-id': [value: string]
  'update:use-knowledge-base': [value: boolean]
}>()

const input = ref('')
const selectedAgentId = ref(props.agentId)
const useKnowledge = ref(props.useKnowledgeBase)

const agentOptions = [
  { label: '不使用智能体', value: '' },
  { label: '笔记整理智能体', value: 'note_organizer' },
  { label: '学习辅导智能体', value: 'study_coach' },
  { label: '工作效率智能体', value: 'productivity' },
  { label: '创意写作智能体', value: 'creative_writer' },
  { label: '知识关联智能体', value: 'knowledge_graph' },
  { label: '个人成长智能体', value: 'growth_mentor' }
]

// 监听 props 变化同步到内部状态
watch(() => props.agentId, (newVal) => {
  selectedAgentId.value = newVal
})

watch(() => props.useKnowledgeBase, (newVal) => {
  useKnowledge.value = newVal
})

// 内部状态变化时通知父组件
watch(selectedAgentId, (newVal) => {
  emit('update:agent-id', newVal)
})

watch(useKnowledge, (newVal) => {
  emit('update:use-knowledge-base', newVal)
})
const messagesEndRef = ref<HTMLDivElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<File[]>([])
const isUploading = ref(false)
const uploadProgress = ref(0)
const sessionFileContents = ref<Map<string, string>>(new Map()) // 存储当前会话临时文件内容

// 保存到笔记相关状态
const showSaveDialog = ref(false)
const savingMessage = ref<Message | null>(null)
const notebooks = ref<any[]>([])
const selectedNotebookId = ref<number | null>(null)
const noteTitle = ref('')
const isSaving = ref(false)
const saveError = ref('')
const saveSuccess = ref(false)
const showCitationPreview = ref(false)
const citationPreviewFile = ref<any | null>(null)

function openCitation(citation: any) {
  if (!citation?.fileId) return
  const title = citation.title || '来源文件'
  const extension = title.includes('.') ? title.split('.').pop()?.toLowerCase() : 'txt'
  citationPreviewFile.value = {
    id: citation.fileId,
    originalFilename: title,
    fileName: title,
    fileExtension: extension,
    quote: citation.quote,
    page: citation.page
  }
  showCitationPreview.value = true
}

// 自动滚动到底部
watch(() => props.messages, async () => {
  await nextTick()
  messagesEndRef.value?.scrollIntoView({ behavior: 'smooth' })
}, { deep: true })

function handleSend() {
  if (input.value.trim() && !props.isLoading) {
    emit('send-message', input.value.trim())
    input.value = ''
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
    }
  }
}

function handleStop() {
  emit('stop')
}

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleInputChange(e: Event) {
  const textarea = e.target as HTMLTextAreaElement
  input.value = textarea.value
  textarea.style.height = 'auto'
  textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
}

function handleFileUpload() {
  fileInputRef.value?.click()
}

async function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    selectedFiles.value = [...selectedFiles.value, ...Array.from(files)]
    // 自动读取文件内容
    await readSelectedFilesContent()
  }
  target.value = ''
}

function removeSelectedFile(index: number) {
  selectedFiles.value.splice(index, 1)
}

// 读取文件内容（仅支持文本文件）
async function readFileContent(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      resolve(e.target?.result as string || '')
    }
    reader.onerror = () => {
      reject(new Error(`读取文件 ${file.name} 失败`))
    }
    reader.readAsText(file)
  })
}

// 读取选中文件的内容（临时使用，不上传到知识库）
async function readSelectedFilesContent() {
  if (selectedFiles.value.length === 0) {
    return { success: true, fileContents: new Map<string, string>() }
  }

  isUploading.value = true
  uploadProgress.value = 0
  const fileContents = new Map<string, string>()
  const failedFiles: string[] = []

  try {
    for (let i = 0; i < selectedFiles.value.length; i++) {
      const file = selectedFiles.value[i]
      try {
        // 只读取文本文件
        if (file.type.startsWith('text/') || 
            file.name.endsWith('.txt') || 
            file.name.endsWith('.md') ||
            file.name.endsWith('.json') ||
            file.name.endsWith('.xml') ||
            file.name.endsWith('.csv')) {
          const content = await readFileContent(file)
          fileContents.set(file.name, content)
        } else {
          failedFiles.push(file.name + '(仅支持文本文件)')
        }
      } catch (error) {
        console.error(`读取文件 ${file.name} 失败:`, error)
        failedFiles.push(file.name)
      }
      uploadProgress.value = Math.round(((i + 1) / selectedFiles.value.length) * 100)
    }

    // 保存到会话临时存储
    sessionFileContents.value = fileContents

    return {
      success: failedFiles.length === 0,
      fileContents,
      readCount: fileContents.size,
      failedCount: failedFiles.length,
      failedFiles
    }
  } finally {
    isUploading.value = false
    uploadProgress.value = 0
  }
}

function setQuickInput(text: string) {
  input.value = text
}

// 暴露方法和属性供父组件使用
defineExpose({
  readSelectedFilesContent,
  selectedFiles,
  sessionFileContents
})

// 配置 marked 选项
marked.setOptions({
  breaks: true, // 支持 GitHub 风格的换行
  gfm: true,    // 启用 GitHub 风格的 Markdown
})

// 渲染 Markdown 内容
// 打开保存到笔记对话框
async function openSaveDialog(message: Message) {
  savingMessage.value = message
  showSaveDialog.value = true
  saveError.value = ''
  saveSuccess.value = false
  // 优先使用对话标题，否则从内容生成
  noteTitle.value = props.conversationTitle || generateNoteTitle(message.content)
  
  // 获取笔记本列表
  try {
    const res = await getNotebooks()
    if (res.code === 0 && res.data) {
      notebooks.value = res.data
      if (notebooks.value.length > 0 && !selectedNotebookId.value) {
        selectedNotebookId.value = notebooks.value[0].id
      }
    }
  } catch (error) {
    console.error('获取笔记本失败:', error)
    saveError.value = '获取笔记本列表失败'
  }
}

// 关闭保存对话框
function closeSaveDialog() {
  showSaveDialog.value = false
  savingMessage.value = null
  saveError.value = ''
  saveSuccess.value = false
}

// 生成笔记标题（取内容前30个字符）
function generateNoteTitle(content: string): string {
  // 移除 Markdown 标记和换行符，提取纯文本
  const plainText = content
    .replace(/\[NL\]/g, ' ')
    .replace(/[#*`>\-_\[\]()]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return plainText.slice(0, 30) + (plainText.length > 30 ? '...' : '')
}

// 保存到笔记
async function saveToNote() {
  if (!savingMessage.value || !selectedNotebookId.value || !noteTitle.value.trim()) {
    saveError.value = '请选择笔记本并输入标题'
    return
  }
  
  isSaving.value = true
  saveError.value = ''
  
  try {
    // 将 [NL] 标记还原为换行符
    const contentMd = savingMessage.value.content.replace(/\[NL\]/g, '\n')
    
    const res = await createNote({
      notebookId: selectedNotebookId.value,
      title: noteTitle.value.trim(),
      contentMd: contentMd,
      status: 1 // 已发布
    })
    
    if (res.code === 0) {
      saveSuccess.value = true
      // 发出笔记保存成功事件，通知父组件刷新
      emit('note-saved')
      setTimeout(() => {
        closeSaveDialog()
      }, 1500)
    } else {
      saveError.value = res.message || '保存失败'
    }
  } catch (error: any) {
    console.error('保存笔记失败:', error)
    saveError.value = error.message || '保存失败，请重试'
  } finally {
    isSaving.value = false
  }
}

function renderMarkdown(content: string): string {
  // 1. 将后端编码的换行符标记还原为实际换行符
  let normalizedContent = content.replace(/\[NL\]/g, '\n')
  
  // 2. 规范化换行符：将 Windows 风格的 \r\n 转换为 \n
  normalizedContent = normalizedContent.replace(/\r\n/g, '\n')
  
  // 3. 确保段落分隔：将两个及以上连续换行符规范化
  normalizedContent = normalizedContent.replace(/\n{2,}/g, '\n\n')
  
  return marked(normalizedContent) as string
}
</script>

<template>
  <div class="h-full flex flex-col bg-white">
    <!-- Header -->
    <div class="border-b border-gray-200 p-4 bg-gradient-to-r from-blue-50 to-purple-50">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
          <Sparkles class="w-5 h-5 text-white" />
        </div>
        <div>
          <h2 class="text-lg">智能助手</h2>
          <p class="text-xs text-gray-500">我是你的AI助手，有什么可以帮你？</p>
        </div>
      </div>
    </div>

    <!-- Messages -->
    <div class="flex-1 overflow-y-auto p-6 space-y-4">
      <div v-if="messages.length === 0" class="h-full flex flex-col items-center justify-center text-gray-400">
        <Sparkles class="w-16 h-16 mb-4 text-gray-300" />
        <p class="text-lg mb-2">开始新对话</p>
        <p class="text-sm mb-6">向AI助手提问，获取智能回答</p>
        
        <!-- 建议问题 -->
        <div class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3 w-full max-w-2xl">
          <button
            @click="setQuickInput('帮我总结一下今天的笔记')"
            class="p-4 border-2 border-gray-200 rounded-lg hover:border-blue-400 hover:bg-blue-50 text-left transition-all hover:shadow-sm"
          >
            <p class="text-sm text-gray-700">帮我总结一下今天的笔记</p>
          </button>
          <button
            @click="setQuickInput('如何提高工作效率？')"
            class="p-4 border-2 border-gray-200 rounded-lg hover:border-blue-400 hover:bg-blue-50 text-left transition-all hover:shadow-sm"
          >
            <p class="text-sm text-gray-700">如何提高工作效率？</p>
          </button>
          <button
            @click="setQuickInput('推荐一些学习资源')"
            class="p-4 border-2 border-gray-200 rounded-lg hover:border-blue-400 hover:bg-blue-50 text-left transition-all hover:shadow-sm"
          >
            <p class="text-sm text-gray-700">推荐一些学习资源</p>
          </button>
          <button
            @click="setQuickInput('帮我制定一个学习计划')"
            class="p-4 border-2 border-gray-200 rounded-lg hover:border-blue-400 hover:bg-blue-50 text-left transition-all hover:shadow-sm"
          >
            <p class="text-sm text-gray-700">帮我制定一个学习计划</p>
          </button>
        </div>
      </div>
      
      <template v-else>
        <div
          v-for="message in messages"
          :key="message.id"
          :class="[
            'flex gap-3',
            message.role === 'user' ? 'justify-end pr-4' : 'justify-start pl-4'
          ]"
        >
          <!-- AI头像 -->
          <div 
            v-if="message.role === 'assistant'" 
            class="flex-shrink-0 w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center"
          >
            <Bot class="w-4 h-4 text-white" />
          </div>

          <!-- 消息内容 -->
          <div :class="[
            'max-w-[70%] flex flex-col',
            message.role === 'user' ? 'items-end' : 'items-start'
          ]">
            <div :class="[
              'inline-block p-3 rounded-xl',
              message.role === 'user'
                ? 'bg-blue-500 text-white rounded-tr-sm'
                : 'bg-gray-100 text-gray-900 rounded-tl-sm'
            ]">
              <!-- 用户消息：纯文本显示 -->
              <p v-if="message.role === 'user'" class="whitespace-pre-wrap break-words break-all">{{ message.content }}</p>
              <!-- AI消息：Markdown渲染 -->
              <div 
                v-else 
                class="markdown-content break-words break-all overflow-wrap-anywhere"
                v-html="renderMarkdown(message.content)"
              ></div>
            </div>
            <div class="flex items-center gap-2 mt-1 px-1">
              <p class="text-xs text-gray-400">
                {{ formatRelativeTime(message.timestamp) }}
              </p>
              <!-- AI消息显示保存按钮 -->
              <button
                v-if="message.role === 'assistant' && message.content"
                @click="openSaveDialog(message)"
                class="flex items-center gap-1 text-xs text-gray-400 hover:text-blue-500 transition-colors"
                title="保存到笔记"
              >
                <BookmarkPlus class="w-3.5 h-3.5" />
                <span>保存</span>
              </button>
            </div>
            <div v-if="message.role === 'assistant' && message.citations && message.citations.length" class="flex flex-wrap gap-2 mt-2 px-1">
                <button
                v-for="(cite, idx) in message.citations"
                :key="`${message.id || 'msg'}-${idx}`"
                  type="button"
                  @click="openCitation(cite)"
                  class="inline-flex items-center gap-1.5 max-w-full px-2 py-1 rounded-full border border-gray-200 bg-white text-gray-700 hover:border-blue-300 hover:bg-blue-50 transition-colors text-left"
                :title="cite.title"
              >
                <FileText class="w-3 h-3 text-gray-400" />
                <span class="flex max-w-[260px] min-w-0 flex-col">
                  <span class="truncate text-xs font-medium">{{ cite.title || '来源文件' }}</span>
                  <span class="text-[10px] text-gray-400">
                    <span v-if="cite.page">第 {{ cite.page }} 页</span>
                    <span v-if="cite.page && cite.score != null"> · </span>
                    <span v-if="cite.score != null">相似度 {{ Math.round(cite.score * 100) }}%</span>
                  </span>
                </span>
                  <ExternalLink class="w-3 h-3 text-gray-400 shrink-0" />
                </button>
            </div>
          </div>

          <!-- 用户头像 -->
          <div 
            v-if="message.role === 'user'" 
            class="flex-shrink-0 w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center"
          >
            <User class="w-4 h-4 text-white" />
          </div>
        </div>

        <div ref="messagesEndRef" />
      </template>
    </div>

    <!-- Input area -->
    <div class="border-t border-gray-200 p-4 bg-gray-50">
      <!-- 智能体选择器和知识库开关 -->
      <div class="flex items-center justify-end gap-4 mb-3">
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-600">智能体</span>
          <select
            :value="selectedAgentId"
            @change="selectedAgentId = ($event.target as HTMLSelectElement).value"
            class="text-sm border border-gray-300 rounded-md px-2 py-1 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option v-for="agent in agentOptions" :key="agent.value" :value="agent.value">
              {{ agent.label }}
            </option>
          </select>
        </div>
        <span class="text-sm text-gray-600">使用知识库增强</span>
        <button
          @click="useKnowledge = !useKnowledge"
          :class="[
            'relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
            useKnowledge ? 'bg-blue-600' : 'bg-gray-200'
          ]"
        >
          <span
            :class="[
              'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
              useKnowledge ? 'translate-x-6' : 'translate-x-1'
            ]"
          />
        </button>
        <span :class="['text-xs', useKnowledge ? 'text-blue-600' : 'text-gray-400']">
          {{ useKnowledge ? '已开启' : '已关闭' }}
        </span>
      </div>
      <div class="flex gap-2 items-end">
        <textarea
          ref="textareaRef"
          :value="input"
          @input="handleInputChange"
          @keydown="handleKeyDown"
          placeholder="输入消息...（Shift + Enter 换行）"
          class="flex-1 min-h-[44px] max-h-[200px] resize-none px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
          :disabled="isLoading"
        />
        <Button
          v-if="isLoading"
          @click="handleStop"
          class="bg-red-500 hover:bg-red-600 text-white px-4 h-11 rounded-lg shadow-sm"
          aria-label="停止回答"
        >
          <Square class="w-4 h-4" />
        </Button>
        <Button
          v-else
          @click="handleSend"
          :disabled="!input.trim()"
          class="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white px-4 h-11 rounded-lg shadow-sm"
          aria-label="发送"
        >
          <Send class="w-4 h-4" />
        </Button>
        <Button
          @click="handleFileUpload"
          class="bg-white border border-gray-300 text-gray-700 hover:border-blue-300 hover:text-blue-600 hover:bg-blue-50 px-4 h-11 rounded-lg"
          aria-label="上传文件"
        >
          <Paperclip class="w-4 h-4" />
        </Button>
        <input
          ref="fileInputRef"
          type="file"
          multiple
          @change="handleFileChange"
          class="hidden"
        />
      </div>
      <div v-if="selectedFiles.length > 0" class="mt-2 flex flex-wrap gap-2">
        <div v-for="(file, index) in selectedFiles" :key="`${file.name}-${index}`" class="flex items-center gap-2 text-xs px-2.5 py-1 rounded-full border border-gray-200 bg-white text-gray-700">
          <span class="max-w-[160px] truncate">{{ file.name }}</span>
          <button type="button" class="text-gray-400 hover:text-red-500" @click="removeSelectedFile(index)" aria-label="移除文件">×</button>
        </div>
      </div>
      <!-- 上传进度提示 -->
      <div v-if="isUploading" class="mt-2 text-sm text-blue-600 flex items-center gap-2">
        <UploadIcon class="w-4 h-4 animate-pulse" />
        <span>正在读取文件... {{ uploadProgress }}%</span>
      </div>
      <p class="text-xs text-gray-400 mt-2">
        AI助手可能会产生错误信息，请仔细核实重要内容。未开启知识库增强时，上传的文件仅临时使用；开启后会基于整个知识库回答。
      </p>
    </div>
    
    <!-- 保存到笔记对话框 -->
    <div v-if="showSaveDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeSaveDialog">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 overflow-hidden">
        <!-- 对话框标题 -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h3 class="text-lg font-medium text-gray-900">保存到笔记</h3>
          <button @click="closeSaveDialog" class="text-gray-400 hover:text-gray-600">
            <X class="w-5 h-5" />
          </button>
        </div>
        
        <!-- 对话框内容 -->
        <div class="p-5 space-y-4">
          <!-- 成功提示 -->
          <div v-if="saveSuccess" class="flex items-center gap-2 text-green-600 bg-green-50 p-3 rounded-lg">
            <Check class="w-5 h-5" />
            <span>保存成功！</span>
          </div>
          
          <template v-else>
            <!-- 选择笔记本 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5">选择笔记本</label>
              <select 
                v-model="selectedNotebookId"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
              >
                <option v-for="nb in notebooks" :key="nb.id" :value="nb.id">{{ nb.name }}</option>
              </select>
              <p v-if="notebooks.length === 0" class="text-xs text-gray-500 mt-1">暂无笔记本，请先创建</p>
            </div>
            
            <!-- 笔记标题 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5">笔记标题</label>
              <input 
                v-model="noteTitle"
                type="text"
                placeholder="请输入笔记标题"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
              />
            </div>
            
            <!-- 内容预览 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5">内容预览</label>
              <div class="max-h-32 overflow-y-auto p-3 bg-gray-50 rounded-lg text-sm text-gray-600">
                <div class="markdown-preview" v-html="renderMarkdown(savingMessage?.content || '')"></div>
              </div>
            </div>
            
            <!-- 错误提示 -->
            <p v-if="saveError" class="text-sm text-red-500">{{ saveError }}</p>
          </template>
        </div>
        
        <!-- 对话框按钮 -->
        <div v-if="!saveSuccess" class="flex justify-end gap-3 px-5 py-4 border-t border-gray-100 bg-gray-50">
          <button 
            @click="closeSaveDialog"
            class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors"
          >
            取消
          </button>
          <button 
            @click="saveToNote"
            :disabled="isSaving || notebooks.length === 0"
            class="px-4 py-2 text-sm text-white bg-blue-500 hover:bg-blue-600 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <span v-if="isSaving">保存中...</span>
            <span v-else>保存</span>
          </button>
        </div>
      </div>
    </div>
  </div>

  <FilePreviewModal
    v-model:visible="showCitationPreview"
    :file="citationPreviewFile"
    @download="() => undefined"
  />
</template>

<style scoped>
/* Markdown 内容样式 */
.markdown-content {
  line-height: 1.6;
  word-wrap: break-word;
  overflow-wrap: anywhere;
  word-break: break-word;
}

/* 通用换行规则，防止长单词或连续字符溢出 */
.markdown-content :deep(*) {
  word-wrap: break-word;
  overflow-wrap: anywhere;
}

/* 标题样式 */
.markdown-content :deep(h1) {
  font-size: 1.5em;
  font-weight: bold;
  margin-top: 0.5em;
  margin-bottom: 0.5em;
  border-bottom: 2px solid #e7dcc9;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h2) {
  font-size: 1.3em;
  font-weight: bold;
  margin-top: 0.5em;
  margin-bottom: 0.4em;
  border-bottom: 1px solid #e7dcc9;
  padding-bottom: 0.2em;
}

.markdown-content :deep(h3) {
  font-size: 1.1em;
  font-weight: bold;
  margin-top: 0.4em;
  margin-bottom: 0.3em;
}

/* 列表样式 */
.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin: 0.25em 0;
}

/* 代码块样式 */
.markdown-content :deep(code) {
  background-color: #f6f0e5;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  word-break: break-all;
  overflow-wrap: anywhere;
}

.markdown-content :deep(pre) {
  background-color: #f6f0e5;
  padding: 1em;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0.5em 0;
  max-width: 100%;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 引用样式 */
.markdown-content :deep(blockquote) {
  border-left: 4px solid #d9cab1;
  padding-left: 1em;
  margin: 0.5em 0;
  color: #8b745c;
  font-style: italic;
}

/* 加粗和斜体 */
.markdown-content :deep(strong) {
  font-weight: bold;
}

.markdown-content :deep(em) {
  font-style: italic;
}

/* 链接样式 */
.markdown-content :deep(a) {
  color: #b25f22;
  text-decoration: underline;
}

.markdown-content :deep(a:hover) {
  color: #8e4a1b;
}

/* 段落间距 - 增强段落分隔 */
.markdown-content :deep(p) {
  margin: 0.75em 0;
  line-height: 1.8;
}

/* 移除第一个段落的上边距和最后一个段落的下边距 */
.markdown-content :deep(p:first-child) {
  margin-top: 0;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

/* 水平线 */
.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #e7dcc9;
  margin: 1em 0;
}

/* 表格样式 */
.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.5em 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #d9cab1;
  padding: 0.5em;
  text-align: left;
}

.markdown-content :deep(th) {
  background-color: #f6f0e5;
  font-weight: bold;
}

/* 预览区域的Markdown样式 */
.markdown-preview {
  font-size: 0.875rem;
  line-height: 1.5;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3) {
  font-weight: 600;
  margin: 0.5em 0;
}

.markdown-preview :deep(ul),
.markdown-preview :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

.markdown-preview :deep(p) {
  margin: 0.5em 0;
}

.markdown-preview :deep(code) {
  background-color: #efe5d4;
  padding: 0.1em 0.3em;
  border-radius: 3px;
  font-size: 0.85em;
}
</style>
