<script setup lang="ts">
import { ref, watch, onBeforeUnmount, onMounted } from 'vue'
import { createDraftSaver } from '@/utils/draftSaver'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import SimpleMarkdownEditor from './SimpleMarkdownEditor.vue'
import SIcon from '@/components/ui/SIcon.vue'
import { Plus, X, Database, BookOpen, Sparkles, PenLine, Wand2, FileText, ListChecks, Languages, Upload } from 'lucide-vue-next'
import type { Note, Notebook } from '@/types/note'
import { addNoteToKnowledgeBase } from '@/api/notes.js'
import { publishNoteSquare } from '@/api/noteSquare.js'
import { processNoteWithAI } from '@/api/noteAI.js'
import { useModal } from '@/composables/useModal'

interface Props {
  note: Note | null
  notebooks?: Notebook[]
  saveNote: (noteId: string, updates: Partial<Note>) => Promise<void>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'change-notebook': [noteId: string, notebookId: string]
  'create-note': []
  'open-template': []
}>()

const title = ref('')
const content = ref('')
const tags = ref<string[]>([])
const newTag = ref('')
const showTagInput = ref(false)
const showAIAssistant = ref(false)
const selectedText = ref('')
const selectedOriginalText = ref('')
const aiAction = ref('continue')
const customPrompt = ref('')
const generatedContent = ref('')
const isGenerating = ref(false)
let isExternalUpdate = false
const saveState = ref<'pending' | 'saving' | 'saved' | 'error'>('saved')
const accountScope = [...(localStorage.getItem('satoken') || '')].reduce((hash, char) => (hash * 31 + char.charCodeAt(0)) >>> 0, 0)
const draftKey = (noteId: string) => `note-draft:${accountScope}:${noteId}`
const recoveredDraft = ref(false)
const saver = createDraftSaver<Partial<Note>>(
  async (noteId, draft) => {
    await props.saveNote(noteId, draft)
    try {
      if (sessionStorage.getItem(draftKey(noteId)) === JSON.stringify(draft)) sessionStorage.removeItem(draftKey(noteId))
    } catch { /* Saving to the server must not depend on browser storage. */ }
  },
  (noteId, state) => { if (noteId === props.note?.id) saveState.value = state }
)

// 更新本地状态
watch(() => props.note, (newNote, oldNote) => {
  if (newNote && oldNote?.id === newNote.id) return
  saveState.value = 'saved'
  recoveredDraft.value = false
  if (oldNote && oldNote.id !== newNote?.id) void saver.flush(oldNote.id).catch(() => {})
  isExternalUpdate = true
  if (newNote) {
    // 标记为外部更新，避免触发自动保存
    isExternalUpdate = true
    title.value = newNote.title
    content.value = newNote.content
    tags.value = [...newNote.tags]
  } else {
    title.value = ''
    content.value = ''
    tags.value = []
  }
  isExternalUpdate = false
  if (newNote) {
    try {
      const stored = sessionStorage.getItem(draftKey(newNote.id))
      if (stored) {
        const draft = JSON.parse(stored)
        isExternalUpdate = true
        title.value = draft.title ?? newNote.title
        content.value = draft.content ?? newNote.content
        tags.value = draft.tags ?? newNote.tags
        isExternalUpdate = false
        recoveredDraft.value = true
        saver.schedule(newNote.id, draft)
      }
    } catch { isExternalUpdate = false }
  }
}, { immediate: true })

// 防抖自动保存
watch([title, content, tags], () => {
  // 如果是外部更新引起的变化，不触发保存
  if (!props.note || isExternalUpdate) return
  
  const draft = {
      title: title.value,
      content: content.value,
      tags: [...tags.value],
      updatedAt: new Date()
  }
  try { sessionStorage.setItem(draftKey(props.note.id), JSON.stringify(draft)) } catch { /* Storage may be unavailable. */ }
  saver.schedule(props.note.id, draft)
}, { deep: true, flush: 'sync' })

function warnUnsaved(event: BeforeUnloadEvent) {
  if (saver.hasUnsaved()) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', warnUnsaved))
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', warnUnsaved)
  void saver.flushAll().catch(() => { void modal.error('笔记保存失败，请检查网络后重试') })
})

async function retrySave() {
  try { await saver.flushAll() } catch { /* 错误状态保留，允许重试 */ }
}

function handleAddTag() {
  if (newTag.value.trim() && !tags.value.includes(newTag.value.trim())) {
    tags.value = [...tags.value, newTag.value.trim()]
    newTag.value = ''
    showTagInput.value = false
  }
}

function handleRemoveTag(tagToRemove: string) {
  tags.value = tags.value.filter(tag => tag !== tagToRemove)
}

function openAIAssistant() {
  const selection = window.getSelection()?.toString() || ''
  selectedOriginalText.value = selection
  selectedText.value = selection || content.value || ''
  aiAction.value = 'continue'
  customPrompt.value = ''
  generatedContent.value = ''
  showAIAssistant.value = true
}

function closeAIAssistant() {
  showAIAssistant.value = false
  isGenerating.value = false
}

async function generateContent() {
  if (!selectedText.value) {
    selectedText.value = content.value || ''
  }
  
  isGenerating.value = true
  generatedContent.value = ''
  
  try {
    // 调用真实API
    await processNoteWithAI(
      {
        action: aiAction.value,
        selectedText: selectedText.value,
        fullContent: content.value,
        customPrompt: customPrompt.value || undefined,
        targetLanguage: aiAction.value === 'translate' ? '英语' : undefined
      },
      // 流式接收数据
      (chunk: string) => {
        generatedContent.value += chunk
      }
    )
  } catch (error) {
    console.error('[NoteEditor] AI生成失败:', error)
    generatedContent.value = '生成失败，请重试：' + (error instanceof Error ? error.message : String(error))
  } finally {
    isGenerating.value = false
  }
}

function applyInsert() {
  if (!generatedContent.value) return
  content.value = content.value ? `${content.value}\n\n${generatedContent.value}` : generatedContent.value
  closeAIAssistant()
}

function applyReplace() {
  if (!generatedContent.value) return
  if (selectedOriginalText.value && content.value.includes(selectedOriginalText.value)) {
    content.value = content.value.replace(selectedOriginalText.value, generatedContent.value)
  } else {
    content.value = generatedContent.value
  }
  closeAIAssistant()
}

// 添加知识库加载状态
const addingToKb = ref(false)
// 笔记本选择下拉
const showNotebookDropdown = ref(false)
const modal = useModal()
const publishingNote = ref(false)
const aiActions = [
  { id: 'continue', title: '续写内容', desc: '根据当前内容继续写作', icon: PenLine },
  { id: 'optimize', title: '优化文字', desc: '改善表达和文字质量', icon: Wand2 },
  { id: 'summarize', title: '总结内容', desc: '提炼核心要点', icon: FileText },
  { id: 'expand', title: '扩展内容', desc: '丰富和详细化内容', icon: Sparkles },
  { id: 'translate', title: '翻译', desc: '翻译为其他语言', icon: Languages },
  { id: 'list', title: '转为列表', desc: '整理为要点列表', icon: ListChecks }
]

async function handleAddToKnowledgeBase() {
  if (!props.note?.id) {
    await modal.warning('请先保存笔记')
    return
  }
  
  addingToKb.value = true
  try {
    await saver.flushAll()
    const res = await addNoteToKnowledgeBase(props.note.id)
    if (res && res.code === 0) {
      await modal.success('已成功添加到知识库')
    } else {
      await modal.error(res?.message || '添加失败，请重试')
    }
  } catch (error) {
    console.error('[NoteEditor] 添加到知识库失败:', error)
    await modal.error('添加失败，请重试')
  } finally {
    addingToKb.value = false
  }
}

async function handlePublishNote() {
  if (!props.note?.id) {
    await modal.warning('请先保存笔记')
    return
  }

  publishingNote.value = true
  try {
    await saver.flushAll()
    const res = await publishNoteSquare({ noteId: props.note.id })
    if (res && res.code === 0) {
      await modal.success('发布成功')
    } else {
      await modal.error(res?.message || '发布失败，请重试')
    }
  } catch (error) {
    console.error('[NoteEditor] 发布笔记失败:', error)
    await modal.error('发布失败，请重试')
  } finally {
    publishingNote.value = false
  }
}

function handleTagKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    handleAddTag()
  } else if (event.key === 'Escape') {
    showTagInput.value = false
    newTag.value = ''
  }
}

// 修改笔记本
async function handleChangeNotebook(notebookId: string) {
  if (props.note?.id) {
    try {
      await saver.flushAll()
      emit('change-notebook', props.note.id, notebookId)
      showNotebookDropdown.value = false
    } catch { await modal.error('请先重试保存笔记，再移动笔记本') }
  }
}

// 获取当前笔记本名称
function getCurrentNotebookName(): string {
  if (!props.note?.notebookId || !props.notebooks) return '未分类'
  const notebook = props.notebooks.find(nb => nb.id === props.note?.notebookId)
  return notebook?.name || '未分类'
}
</script>

<template>
  <div v-if="!note" class="h-full flex items-center justify-center">
    <div class="wce">
      <!-- 叠纸装饰：三层错位的白纸，中间一层放图标 -->
      <div class="wce-art">
        <span class="wce-leaf wce-leaf-3"></span>
        <span class="wce-leaf wce-leaf-2"></span>
        <span class="wce-leaf wce-leaf-1">
          <SIcon name="note" :size="24" />
        </span>
      </div>

      <h2 class="wce-title">还没有打开笔记</h2>
      <p class="wce-desc">从左侧列表选一条继续阅读，或者直接开一张新的白纸。</p>

      <div class="wce-acts">
        <button class="wce-btn is-main" @click="emit('create-note')">
          <SIcon name="plus" :size="14" :thickness="2.2" />
          新建笔记
        </button>
        <button class="wce-btn" @click="emit('open-template')">
          <SIcon name="layout" :size="14" />
          从模板开始
        </button>
      </div>

      <div class="wce-keys">
        <span class="wce-key"><kbd>⌘</kbd><kbd>N</kbd> 新建</span>
        <i class="wce-key-sep"></i>
        <span class="wce-key"><kbd>⌘</kbd><kbd>K</kbd> 搜索</span>
        <i class="wce-key-sep"></i>
        <span class="wce-key">在左栏右键笔记可导出</span>
      </div>
    </div>
  </div>

  <div v-else class="h-full flex flex-col">
    <!-- 编辑器头部：标题独占一行，元信息压成一行 -->
    <div class="wc-ed-head">
      <input
        v-model="title"
        class="wc-ed-title"
        placeholder="无标题笔记"
        maxlength="200"
      />

      <!-- 元信息：笔记本 / 标签 在左，动作在右 -->
      <div class="wc-ed-meta">
        <button v-if="saveState === 'error'" class="text-xs text-red-600" @click="retrySave">保存失败，点击重试</button>
        <span v-else class="text-xs text-gray-500" role="status">{{ saveState === 'saved' ? '已保存' : saveState === 'saving' ? '保存中…' : '等待保存…' }}</span>
        <span v-if="recoveredDraft" class="text-xs text-amber-700">已恢复本次会话中的草稿</span>
        <!-- 笔记本选择器 -->
        <div class="relative" v-if="notebooks && notebooks.length > 0">
          <Button
            variant="ghost"
            size="sm"
            class="h-7 gap-1.5 px-2 text-xs text-gray-600"
            @click="showNotebookDropdown = !showNotebookDropdown"
          >
            <BookOpen class="w-3.5 h-3.5" />
            {{ getCurrentNotebookName() }}
          </Button>
          <!-- 笔记本下拉菜单 -->
          <div
            v-if="showNotebookDropdown"
            class="absolute left-0 top-full mt-1.5 bg-white border border-gray-200 rounded-xl shadow-lg z-50 min-w-[168px] overflow-hidden py-1"
          >
            <button
              v-for="notebook in notebooks"
              :key="notebook.id"
              class="w-full px-3 py-2 text-left text-[13px] hover:bg-gray-100 flex items-center gap-2"
              :class="{ 'bg-blue-50 text-blue-700': notebook.id === note?.notebookId }"
              @click="handleChangeNotebook(notebook.id)"
            >
              <BookOpen class="w-3.5 h-3.5" />
              {{ notebook.name }}
            </button>
          </div>
        </div>

        <span v-if="notebooks && notebooks.length > 0" class="h-3.5 w-px bg-gray-200"></span>

        <Badge
          v-for="tag in tags"
          :key="tag"
          variant="secondary"
          class="gap-1 pl-2 pr-1 text-xs"
        >
          {{ tag }}
          <button
            @click="handleRemoveTag(tag)"
            class="hover:bg-gray-300 rounded-full p-0.5"
          >
            <X class="w-3 h-3" />
          </button>
        </Badge>

        <div v-if="showTagInput" class="flex items-center gap-1">
          <Input
            v-model="newTag"
            @keydown="handleTagKeydown"
            @blur="handleAddTag"
            placeholder="标签名"
            class="h-7 w-24 text-xs"
            autofocus
          />
        </div>
        <Button
          v-else
          variant="ghost"
          size="sm"
          class="h-7 gap-1 px-2 text-xs text-gray-500"
          @click="showTagInput = true"
        >
          <Plus class="w-3.5 h-3.5" />
          标签
        </Button>

        <div class="wc-ed-meta-r">
          <Button
            variant="ghost"
            size="sm"
            class="h-7 gap-1.5 px-2 text-xs"
            :disabled="addingToKb"
            @click="handleAddToKnowledgeBase"
          >
            <Database class="w-3.5 h-3.5" />
            {{ addingToKb ? '添加中' : '知识库' }}
          </Button>

          <Button
            variant="ghost"
            size="sm"
            class="h-7 gap-1.5 px-2 text-xs"
            :disabled="publishingNote"
            @click="handlePublishNote"
          >
            <Upload class="w-3.5 h-3.5" />
            {{ publishingNote ? '发布中' : '发布' }}
          </Button>

          <!-- 整屏唯一实心主按钮 -->
          <Button
            size="sm"
            class="h-7 gap-1.5 px-3 text-xs"
            @click="openAIAssistant"
          >
            <Sparkles class="w-3.5 h-3.5" />
            AI 助手
          </Button>
        </div>
      </div>
    </div>

    <!-- 稿纸 -->
    <div class="wc-ed-paper">
      <SimpleMarkdownEditor
        v-model="content"
        placeholder="点击开始编辑，支持 Markdown 格式"
      />
    </div>

    <!-- AI 助手弹窗 -->
    <div v-if="showAIAssistant" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="closeAIAssistant"></div>
      <div class="relative w-full max-w-3xl bg-white rounded-xl shadow-xl border border-gray-200 overflow-hidden flex flex-col max-h-[90vh]">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 flex-shrink-0">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-lg bg-blue-600 text-white flex items-center justify-center">
              <Sparkles class="w-5 h-5" />
            </div>
            <div>
              <h3 class="text-base font-semibold text-gray-900">AI 写作助手</h3>
              <p class="text-xs text-gray-500">选择一个操作来优化您的内容</p>
            </div>
          </div>
          <button class="text-gray-400 hover:text-gray-600" @click="closeAIAssistant">
            <X class="w-4 h-4" />
          </button>
        </div>

        <div class="px-6 py-4 space-y-5 overflow-y-auto flex-1">
          <div>
            <label class="text-sm text-gray-700 mb-2 block">选中的内容</label>
            <div class="bg-gray-100 border border-gray-200 rounded-lg p-3 text-sm text-gray-700 whitespace-pre-wrap min-h-[64px] max-h-[120px] overflow-y-auto">
              {{ selectedText || '暂无选中文本，将基于全文处理' }}
            </div>
          </div>

          <div>
            <label class="text-sm text-gray-700 mb-2 block">选择操作</label>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
              <button
                v-for="item in aiActions"
                :key="item.id"
                class="border rounded-lg px-3 py-2 text-left transition-all"
                :class="aiAction === item.id ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300 hover:bg-blue-50/50'"
                @click="aiAction = item.id"
              >
                <div class="flex items-center gap-2 text-sm font-medium text-gray-900">
                  <component :is="item.icon" class="w-4 h-4 text-blue-600" />
                  {{ item.title }}
                </div>
                <div class="text-xs text-gray-500 mt-1">{{ item.desc }}</div>
              </button>
            </div>
          </div>

          <div>
            <label class="text-sm text-gray-700 mb-2 block">或者输入自定义指令</label>
            <Input
              v-model="customPrompt"
              placeholder="例如：将这段文字改写成诗歌形式..."
              class="text-sm"
            />
          </div>

          <div v-if="generatedContent" class="space-y-2">
            <label class="text-sm text-gray-700 block">生成结果</label>
            <div class="bg-gray-100 border border-gray-200 rounded-lg p-3 text-sm text-gray-700 whitespace-pre-wrap min-h-[80px]">
              {{ generatedContent }}
            </div>
          </div>
        </div>

        <div class="flex items-center justify-between px-6 py-3 border-t border-gray-200 bg-gray-50">
          <div class="text-xs text-gray-500">
            已选中 {{ selectedText.length }} 个字符
          </div>
          <div class="flex items-center gap-2">
            <Button variant="ghost" size="sm" @click="closeAIAssistant">取消</Button>
            <Button
              v-if="generatedContent"
              variant="outline"
              size="sm"
              @click="applyReplace"
            >
              替换
            </Button>
            <Button
              v-if="generatedContent"
              variant="outline"
              size="sm"
              @click="applyInsert"
            >
              插入
            </Button>
            <Button
              v-if="generatedContent"
              variant="outline"
              size="sm"
              @click="generateContent"
              :disabled="isGenerating"
            >
              重新生成
            </Button>
            <Button
              v-else
              size="sm"
              class="bg-blue-600 hover:bg-blue-700 text-white"
              :disabled="isGenerating"
              @click="generateContent"
            >
              {{ isGenerating ? '生成中...' : '生成内容' }}
            </Button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
