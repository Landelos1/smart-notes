<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import Button from '@/components/ui/Button.vue'
import Avatar from '@/components/ui/Avatar.vue'
import Input from '@/components/ui/Input.vue'
import Badge from '@/components/ui/Badge.vue'
import Select from '@/components/ui/Select.vue'

import { useModal } from '@/composables/useModal'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { deleteNote } from '@/api/notes.js'
import { processNoteWithAI } from '@/api/noteAI.js'
import {
  createNoteSquareComment,
  deleteNoteSquareComment,
  favoriteNoteSquare,
  getNoteSquareCategories,
  getNoteSquare,
  getNoteSquareComments,
  getNoteSquareDetail,
  getNoteSquareFavorites,
  getNoteSquareLikes,
  getNoteSquareMyNotes,
  likeNoteSquare,
  streamNoteSquareSearchSummary,
  unfavoriteNoteSquare,
  unlikeNoteSquare,
  unpublishNoteSquare
} from '@/api/noteSquare.js'
import {
  Search,
  Heart,
  Share2,
  Bookmark,
  ArrowLeft,
  Send,
  Trash2,
  EyeOff,
  Sparkles,
  Copy,
  Check,
  X,
  Bot,
  ThumbsUp
} from 'lucide-vue-next'

interface User {
  id: string
  name: string
  avatar: string
}

interface Comment {
  id: string
  user: User
  content: string
  createdAt: string
  likes: number
  isOwner?: boolean
  replyToUserName?: string
  replies?: Comment[]
}

interface PublicNote {
  id: string
  title: string
  category?: string
  content: string
  images: string[]
  user: User
  createdAt: string
  likes: number
  comments: number
  favorites?: number
  views?: number
  isLiked?: boolean
  isBookmarked?: boolean
  tags: string[]
  status?: 'published' | 'draft'
  updatedAt?: string
}

type NoteSquareTab = 'recommend' | 'latest' | 'favorites' | 'likes' | 'my-notes'

interface Props {
  initialTab?: NoteSquareTab
}

const props = withDefaults(defineProps<Props>(), {
  initialTab: 'recommend'
})

marked.setOptions({
  breaks: true,
  gfm: true
})

const notes = ref<PublicNote[]>([])
const selectedNote = ref<PublicNote | null>(null)
const searchQuery = ref('')
const activeTab = ref<NoteSquareTab>(props.initialTab)
const selectedCategory = ref('')
const categories = ref<string[]>([])
const isLoadingCategories = ref(true)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showAISummary = ref(false)
const aiSummaryContent = ref('')
const isAISummarizing = ref(false)
const aiSummaryError = ref('')
const summaryAbortController = ref<AbortController | null>(null)
const lastSummaryKey = ref('')

const totalPages = computed(() => {
  if (!total.value) return 0
  return Math.ceil(total.value / pageSize.value)
})
const categoryOptions = computed(() => [
  { value: '', label: isLoadingCategories.value ? '正在提炼...' : '全部分类' },
  ...categories.value.map(category => ({ value: category, label: category }))
])

const commentTotal = computed(() => countComments(comments.value))

const isLiked = ref(false)
const likeCount = ref(0)
const isBookmarked = ref(false)
const favoriteCount = ref(0)
const viewCount = ref(0)
const commentText = ref('')
const comments = ref<Comment[]>([])
const commentsLoading = ref(false)
const commentSubmitting = ref(false)
const commentError = ref('')
const replyTarget = ref<{ commentId: string; userId: string; userName: string } | null>(null)

const showAIChat = ref(false)
const aiMessages = ref<{ role: 'user' | 'ai'; content: string }[]>([])
const aiInput = ref('')
const isAiTyping = ref(false)
const chatEndRef = ref<HTMLDivElement | null>(null)
const aiAbortController = ref<AbortController | null>(null)

const isForked = ref(false)
const openAiOnSelect = ref(false)
const modal = useModal()

// AI速读侧边栏状态
const showQuickAI = ref(false)
const quickAINote = ref<PublicNote | null>(null)
const quickAIMessages = ref<{ role: 'user' | 'ai'; content: string }[]>([])
const quickAIInput = ref('')
const isQuickAITyping = ref(false)
const quickAIChatEndRef = ref<HTMLDivElement | null>(null)
const quickAIAbortController = ref<AbortController | null>(null)
let searchTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.initialTab,
  (value) => {
    activeTab.value = value
  }
)

watch(activeTab, () => {
  if (currentPage.value !== 1) {
    currentPage.value = 1
    return
  }
  fetchNotes()
})

watch(currentPage, () => {
  fetchNotes()
})

watch(selectedCategory, () => {
  if (currentPage.value !== 1) {
    currentPage.value = 1
    return
  }
  fetchNotes()
})

watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!searchQuery.value.trim()) {
    resetAISummary()
  }
  if (currentPage.value !== 1) {
    currentPage.value = 1
    return
  }
  searchTimer = setTimeout(() => {
    fetchNotes()
  }, 300)
})

onMounted(() => {
  fetchNotes()
  void fetchCategories().then(() => {
    if (categories.value.length > 0) void fetchNotes()
  })
})

async function fetchCategories() {
  try {
    const res = await getNoteSquareCategories()
    categories.value = res?.code === 0 && Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.error('[NoteSquare] 加载分类失败:', error)
    categories.value = []
  } finally {
    isLoadingCategories.value = false
  }
}

function formatTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

function extractFirstImageUrl(content?: string) {
  if (!content) return ''
  const htmlMatch = content.match(/<img[^>]+src=['"]([^'"]+)['"]/i)
  if (htmlMatch?.[1]) return htmlMatch[1]
  const mdMatch = content.match(/!\[[^\]]*]\(([^)\s]+)(?:\s+['"][^'"]*['"])?\)/)
  if (mdMatch?.[1]) return mdMatch[1]
  return ''
}

function mapSquareNote(raw: any, fullContent = false): PublicNote {
  const publishTime = raw.publishTime || raw.createTime
  const updateTime = raw.updateTime || raw.createTime || raw.publishTime
  const preview = raw.summary || raw.content || ''
  const content = fullContent ? (raw.content || raw.summary || '') : preview
  const status = raw.squareStatus === 1 ? 'published' : 'draft'
  const firstImage = raw.coverUrl || extractFirstImageUrl(raw.content || raw.summary || '')
  return {
    id: String(raw.id ?? ''),
    title: raw.title || '',
    category: raw.category || '',
    content,
    images: firstImage ? [firstImage] : [],
    user: {
      id: String(raw.authorId ?? ''),
      name: raw.authorName || '匿名用户',
      avatar: raw.authorAvatar || ''
    },
    createdAt: formatTime(publishTime),
    updatedAt: updateTime ? formatTime(updateTime) : undefined,
    likes: raw.likeCount || 0,
    comments: raw.commentCount || 0,
    favorites: raw.favoriteCount || 0,
    views: raw.viewCount || 0,
    isLiked: Boolean(raw.isLiked),
    isBookmarked: Boolean(raw.isFavorited),
    tags: raw.tags || [],
    status
  }
}

const renderedNoteHtml = computed(() => {
  if (!selectedNote.value?.content) {
    return '<p class="text-gray-500">暂无内容</p>'
  }
  const normalizedContent = selectedNote.value.content.replace(/\r\n?/g, '\n')
  const rawHtml = marked.parse(normalizedContent) as string
  return DOMPurify.sanitize(rawHtml)
})

// 列表卡片的摘要是「纯文本预览」，不需要完整 Markdown 渲染。
// 原来直接 marked.parse 再靠 line-clamp 截断，结果是多个块级节点堆叠、
// 行数不受控，还会把 --- / ** / ### 这些标记原样留在卡片上。
// 这里先剥掉标记再转义输出，行数就能稳定由 line-clamp 控制。
function stripMarkdown(src: string) {
  return src
    .replace(/\r\n?/g, '\n')
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]*)`/g, '$1')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/(^|\s)#{1,6}\s+/g, ' ')
    .replace(/(^|\s)>\s?/g, ' ')
    .replace(/(^|\s)[-*+]\s+/g, '$1· ')
    .replace(/[-_]{3,}/g, ' ')
    .replace(/(\*\*|__|~~)/g, '')
    .replace(/(^|\s)[*_](?=\s|$)/g, ' ')
    .replace(/[ \t\u00a0]+/g, ' ')
    .replace(/\s*\n\s*/g, ' ')
    .trim()
}

function escapeHtml(src: string) {
  return src.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function renderPreview(note: PublicNote) {
  if (!note.content) return ''
  return escapeHtml(stripMarkdown(note.content))
}

async function fetchNotes() {
  try {
    const keyword = searchQuery.value.trim()
    let res
    if (activeTab.value === 'favorites') {
      res = await getNoteSquareFavorites({
        page: currentPage.value,
        size: pageSize.value,
        keyword
      })
    } else if (activeTab.value === 'likes') {
      res = await getNoteSquareLikes({
        page: currentPage.value,
        size: pageSize.value,
        keyword
      })
    } else if (activeTab.value === 'my-notes') {
      res = await getNoteSquareMyNotes({
        page: currentPage.value,
        size: pageSize.value,
        keyword
      })
    } else {
      res = await getNoteSquare({
        page: currentPage.value,
        size: pageSize.value,
        keyword,
        category: selectedCategory.value,
        sortBy: activeTab.value === 'latest' ? 'latest' : 'recommend',
        sortOrder: 'desc'
      })
    }

    if (res && res.code === 0) {
      const records = res.data?.data || []
      notes.value = records.map((item: any) => mapSquareNote(item))
      total.value = res.data?.total || 0
    } else {
      notes.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('[NoteSquare] 加载笔记广场失败:', error)
    notes.value = []
    total.value = 0
  }

  await maybeFetchAISummary()
}

function resetAISummary() {
  summaryAbortController.value?.abort()
  summaryAbortController.value = null
  showAISummary.value = false
  aiSummaryContent.value = ''
  aiSummaryError.value = ''
  isAISummarizing.value = false
  lastSummaryKey.value = ''
}

function getSummaryKey(keyword: string) {
  return `${activeTab.value}:${selectedCategory.value}:${keyword}`
}

function sanitizeSummaryChunk(chunk: string) {
  return chunk
    .replace(/\*\*/g, '')
    .replace(/__/g, '')
    .replace(/`+/g, '')
    .replace(/^#+\s*/gm, '')
}

async function maybeFetchAISummary() {
  const keyword = searchQuery.value.trim()
  if (currentPage.value !== 1) return
  if (!keyword || notes.value.length === 0 || !['recommend', 'latest'].includes(activeTab.value)) {
    resetAISummary()
    return
  }

  const summaryKey = getSummaryKey(keyword)
  if (summaryKey === lastSummaryKey.value && aiSummaryContent.value && !aiSummaryError.value) {
    showAISummary.value = true
    return
  }
  lastSummaryKey.value = summaryKey

  summaryAbortController.value?.abort()
  const controller = new AbortController()
  summaryAbortController.value = controller
  showAISummary.value = true
  isAISummarizing.value = true
  aiSummaryContent.value = ''
  aiSummaryError.value = ''

  try {
    await streamNoteSquareSearchSummary(
      {
        keyword,
        maxNotes: 10,
        sortBy: activeTab.value,
        category: selectedCategory.value || undefined
      },
      (chunk: string) => {
        aiSummaryContent.value += sanitizeSummaryChunk(chunk)
      },
      controller.signal
    )
  } catch (error: any) {
    if (error?.name === 'AbortError') return
    console.error('[NoteSquare] AI总结失败:', error)
    aiSummaryError.value = 'AI总结生成失败，请稍后重试'
  } finally {
    isAISummarizing.value = false
    if (summaryAbortController.value === controller) {
      summaryAbortController.value = null
    }
  }
}

function handleStopAISummary() {
  summaryAbortController.value?.abort()
  summaryAbortController.value = null
  isAISummarizing.value = false
}

async function fetchComments(noteId: string) {
  if (selectedNote.value?.id === noteId && selectedNote.value.status === 'draft') {
    comments.value = []
    commentError.value = ''
    commentsLoading.value = false
    return
  }
  commentsLoading.value = true
  commentError.value = ''
  try {
    const res = await getNoteSquareComments(noteId)
    if (res && res.code === 0) {
      const items = Array.isArray(res.data) ? res.data : []
      comments.value = items.map(mapComment)
    } else {
      comments.value = []
      commentError.value = res?.message || '加载评论失败'
    }
  } catch (error) {
    console.error('[NoteSquare] 加载评论失败:', error)
    comments.value = []
    commentError.value = '加载评论失败'
  } finally {
    commentsLoading.value = false
  }
}

function mapComment(raw: any): Comment {
  const replies = Array.isArray(raw.replies) ? raw.replies.map(mapComment) : []
  return {
    id: String(raw.id ?? ''),
    user: {
      id: String(raw.userId ?? ''),
      name: raw.authorName || '匿名用户',
      avatar: raw.authorAvatar || ''
    },
    content: raw.content || '',
    createdAt: formatTime(raw.createTime),
    likes: raw.likes || 0,
    isOwner: Boolean(raw.isOwner),
    replyToUserName: raw.replyToUserName || '',
    replies: replies.length ? replies : undefined
  }
}

function countComments(items: Comment[]): number {
  let totalCount = 0
  items.forEach(item => {
    totalCount += 1
    if (item.replies && item.replies.length) {
      totalCount += countComments(item.replies)
    }
  })
  return totalCount
}

function handleBack() {
  selectedNote.value = null
  showAIChat.value = false
  openAiOnSelect.value = false
  comments.value = []
  commentText.value = ''
  replyTarget.value = null
}

async function handleDelete(noteId: string) {
  const confirmed = await modal.confirm({
    title: '删除笔记',
    message: '确定要删除这条笔记吗？此操作无法撤销。',
    confirmText: '删除',
    cancelText: '取消'
  })
  if (!confirmed) return
  try {
    const res = await deleteNote(noteId)
    if (res && res.code === 0) {
      if (selectedNote.value?.id === noteId) {
        handleBack()
      }
      await fetchNotes()
    } else {
      await modal.error(res?.message || '删除失败，请重试')
    }
  } catch (error) {
    console.error('[NoteSquare] 删除笔记失败:', error)
    await modal.error('删除失败，请重试')
  }
}

async function handleUnpublish(noteId: string) {
  const target = notes.value.find(note => note.id === noteId) || selectedNote.value
  if (target?.status !== 'published') {
    await modal.info('该笔记尚未发布')
    return
  }
  const confirmed = await modal.confirm({
    title: '取消发布',
    message: '确定要取消发布吗？笔记将回到草稿状态。',
    confirmText: '确定',
    cancelText: '取消'
  })
  if (!confirmed) return
  try {
    const res = await unpublishNoteSquare(noteId)
    if (res && res.code === 0) {
      if (selectedNote.value?.id === noteId) {
        selectedNote.value = {
          ...selectedNote.value,
          status: 'draft'
        }
      }
      await fetchNotes()
    } else {
      await modal.error(res?.message || '操作失败，请重试')
    }
  } catch (error) {
    console.error('[NoteSquare] 取消发布失败:', error)
    await modal.error('操作失败，请重试')
  }
}

async function toggleNoteLike(noteId: string) {
  const target = notes.value.find(note => note.id === noteId) || selectedNote.value
  if (!target) return
  if (target.status === 'draft') {
    await modal.info('草稿笔记无法点赞')
    return
  }
  const nextLiked = !target.isLiked
  try {
    const res = nextLiked ? await likeNoteSquare(noteId) : await unlikeNoteSquare(noteId)
    if (!res || res.code !== 0) {
      await modal.error(res?.message || '操作失败，请重试')
      return
    }
    const delta = nextLiked ? 1 : -1
    notes.value = notes.value.map(note => {
      if (note.id !== noteId) return note
      return {
        ...note,
        isLiked: nextLiked,
        likes: Math.max(0, note.likes + delta)
      }
    })
    if (selectedNote.value?.id === noteId) {
      const updatedLikes = Math.max(0, likeCount.value + delta)
      selectedNote.value = {
        ...selectedNote.value,
        isLiked: nextLiked,
        likes: updatedLikes
      }
      isLiked.value = nextLiked
      likeCount.value = updatedLikes
    }
    if (activeTab.value === 'likes' && !nextLiked) {
      await fetchNotes()
    }
  } catch (error) {
    console.error('[NoteSquare] 点赞失败:', error)
    await modal.error('操作失败，请重试')
  }
}

async function toggleNoteBookmark(noteId: string) {
  const target = notes.value.find(note => note.id === noteId) || selectedNote.value
  if (!target) return
  if (target.status === 'draft') {
    await modal.info('草稿笔记无法收藏')
    return
  }
  const nextBookmarked = !target.isBookmarked
  try {
    const res = nextBookmarked ? await favoriteNoteSquare(noteId) : await unfavoriteNoteSquare(noteId)
    if (!res || res.code !== 0) {
      await modal.error(res?.message || '操作失败，请重试')
      return
    }
    const delta = nextBookmarked ? 1 : -1
    notes.value = notes.value.map(note =>
      note.id === noteId
        ? {
            ...note,
            isBookmarked: nextBookmarked,
            favorites: Math.max(0, (note.favorites || 0) + delta)
          }
        : note
    )
    if (selectedNote.value?.id === noteId) {
      const updatedFavorites = Math.max(0, favoriteCount.value + delta)
      selectedNote.value = {
        ...selectedNote.value,
        isBookmarked: nextBookmarked,
        favorites: updatedFavorites
      }
      isBookmarked.value = nextBookmarked
      favoriteCount.value = updatedFavorites
    }
    if (activeTab.value === 'favorites' && !nextBookmarked) {
      await fetchNotes()
    }
  } catch (error) {
    console.error('[NoteSquare] 收藏失败:', error)
    await modal.error('操作失败，请重试')
  }
}

async function handleSelectNote(note: PublicNote, openAi = false) {
  openAiOnSelect.value = openAi
  selectedNote.value = note
  try {
    const res = await getNoteSquareDetail(note.id)
    if (res && res.code === 0 && res.data) {
      selectedNote.value = { ...mapSquareNote(res.data, true), category: note.category }
    }
  } catch (error) {
    console.error('[NoteSquare] 获取笔记详情失败:', error)
  }
  await fetchComments(note.id)
}

// AI速读功能 - 不进入详情页，在右侧显示AI助手
async function handleQuickAI(note: PublicNote) {
  quickAINote.value = note
  showQuickAI.value = true
  isQuickAITyping.value = false
  quickAIMessages.value = []
  quickAIInput.value = ''
  quickAIAbortController.value?.abort()
  
  // 获取笔记详情
  try {
    const res = await getNoteSquareDetail(note.id)
    if (res && res.code === 0 && res.data) {
      quickAINote.value = { ...mapSquareNote(res.data, true), category: note.category }
    }
  } catch (error) {
    console.error('[NoteSquare] 获取笔记详情失败:', error)
  }

  await sendQuickAISummary(quickAINote.value || note)
}

function closeQuickAI() {
  showQuickAI.value = false
  quickAINote.value = null
  quickAIMessages.value = []
  quickAIInput.value = ''
  quickAIAbortController.value?.abort()
}

async function handleSendQuickAI() {
  if (!quickAIInput.value.trim() || !quickAINote.value) return

  const userMsg = quickAIInput.value.trim()
  quickAIMessages.value = [...quickAIMessages.value, { role: 'user', content: userMsg }]
  quickAIInput.value = ''

  await sendQuickAIQuestion(userMsg, quickAINote.value)
}

watch([quickAIMessages, showQuickAI], async () => {
  if (!showQuickAI.value) return
  await nextTick()
  quickAIChatEndRef.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
})

async function sendQuickAISummary(note: PublicNote) {
  const noteTitle = note.title || '未命名'
  const noteContent = note.content || ''
  const userPrompt = `速读文章：《${noteTitle}》`
  quickAIMessages.value = [...quickAIMessages.value, { role: 'user', content: userPrompt }]

  if (!noteContent.trim()) {
    quickAIMessages.value = [...quickAIMessages.value, {
      role: 'ai',
      content: '未获取到文章内容，无法生成速读摘要。'
    }]
    return
  }

  quickAIAbortController.value?.abort()
  const controller = new AbortController()
  quickAIAbortController.value = controller
  isQuickAITyping.value = true

  const aiIndex = quickAIMessages.value.length
  quickAIMessages.value = [...quickAIMessages.value, { role: 'ai', content: '' }]
  let accumulatedContent = ''
  const customPrompt = `你是一位阅读助手，请对文章生成速读摘要。
要求：
1. 输出格式必须包含标题、核心要点和结尾提示。
2. 核心要点用列表（-）展示，3-6条。
3. 内容客观、简洁，不添加未提及的信息。

输出格式：
**速读文章：《${noteTitle}》**

**核心要点：**
- 要点1
- 要点2

---

你可以继续向我提问关于这篇文章的问题。`

  try {
    await processNoteWithAI(
      {
        action: 'custom',
        fullContent: noteContent,
        customPrompt
      },
      (chunk: string) => {
        accumulatedContent += chunk
        quickAIMessages.value = quickAIMessages.value.map((msg, idx) =>
          idx === aiIndex ? { ...msg, content: accumulatedContent } : msg
        )
      },
      controller.signal
    )
  } catch (error: any) {
    if (error?.name === 'AbortError') return
    console.error('[NoteSquare] 速读摘要失败:', error)
    quickAIMessages.value = quickAIMessages.value.map((msg, idx) =>
      idx === aiIndex
        ? { ...msg, content: '抱歉，速读摘要生成失败，请稍后再试。' }
        : msg
    )
  } finally {
    isQuickAITyping.value = false
    quickAIAbortController.value = null
  }
}

async function sendQuickAIQuestion(question: string, note: PublicNote) {
  const noteTitle = note.title || '未命名'
  const noteContent = note.content || ''

  if (!noteContent.trim()) {
    quickAIMessages.value = [...quickAIMessages.value, {
      role: 'ai',
      content: '未获取到文章内容，无法回答该问题。'
    }]
    return
  }

  quickAIAbortController.value?.abort()
  const controller = new AbortController()
  quickAIAbortController.value = controller
  isQuickAITyping.value = true

  const aiIndex = quickAIMessages.value.length
  quickAIMessages.value = [...quickAIMessages.value, { role: 'ai', content: '' }]
  let accumulatedContent = ''
  const customPrompt = `你是阅读助手，请基于文章内容回答用户问题。
如果文章中没有提到相关信息，请明确说明“文章未提及”。
回答要简洁清晰。

文章标题：《${noteTitle}》
用户问题：${question}`

  try {
    await processNoteWithAI(
      {
        action: 'custom',
        fullContent: noteContent,
        customPrompt
      },
      (chunk: string) => {
        accumulatedContent += chunk
        quickAIMessages.value = quickAIMessages.value.map((msg, idx) =>
          idx === aiIndex ? { ...msg, content: accumulatedContent } : msg
        )
      },
      controller.signal
    )
  } catch (error: any) {
    if (error?.name === 'AbortError') return
    console.error('[NoteSquare] 速读问答失败:', error)
    quickAIMessages.value = quickAIMessages.value.map((msg, idx) =>
      idx === aiIndex
        ? { ...msg, content: '抱歉，回答生成失败，请稍后再试。' }
        : msg
    )
  } finally {
    isQuickAITyping.value = false
    quickAIAbortController.value = null
  }
}

async function handleLike() {
  if (!selectedNote.value) return
  await toggleNoteLike(selectedNote.value.id)
}

async function handleBookmark() {
  if (!selectedNote.value) return
  await toggleNoteBookmark(selectedNote.value.id)
}

function handleFork() {
  if (isForked.value) return
  if (window.confirm('确定要将这篇笔记转存到您的笔记本吗？\n\n转存后，您可以自由编辑副本，原文的更新不会影响您的副本。')) {
    setTimeout(() => {
      isForked.value = true
      window.alert('转存成功！已保存到“默认笔记本”。')
    }, 500)
  }
}

async function handleSendComment() {
  if (!selectedNote.value) return
  if (selectedNote.value.status === 'draft') {
    await modal.info('草稿笔记无法评论')
    return
  }
  const content = commentText.value.trim()
  if (!content || commentSubmitting.value) return

  commentSubmitting.value = true
  try {
    const payload = {
      content,
      parentId: replyTarget.value ? Number(replyTarget.value.commentId) : undefined,
      replyToUserId: replyTarget.value ? Number(replyTarget.value.userId) : undefined
    }
    const res = await createNoteSquareComment(selectedNote.value.id, payload)
    if (res && res.code === 0) {
      commentText.value = ''
      replyTarget.value = null
      await fetchComments(selectedNote.value.id)
    } else {
      await modal.error(res?.message || '发表评论失败，请重试')
    }
  } catch (error) {
    console.error('[NoteSquare] 发表评论失败:', error)
    await modal.error('发表评论失败，请重试')
  } finally {
    commentSubmitting.value = false
  }
}

function handleReply(parentComment: Comment, targetComment?: Comment) {
  const target = targetComment || parentComment
  replyTarget.value = {
    commentId: parentComment.id,
    userId: target.user.id,
    userName: target.user.name
  }
}

function handleCancelReply() {
  replyTarget.value = null
}

async function handleDeleteComment(commentId: string) {
  if (!selectedNote.value) return
  if (!window.confirm('确定要删除这条评论吗？')) return
  try {
    const res = await deleteNoteSquareComment(commentId)
    if (res && res.code === 0) {
      await fetchComments(selectedNote.value.id)
    } else {
      await modal.error(res?.message || '删除评论失败，请重试')
    }
  } catch (error) {
    console.error('[NoteSquare] 删除评论失败:', error)
    await modal.error('删除评论失败，请重试')
  }
}

async function handleSendAI() {
  if (!aiInput.value.trim()) return
  if (!selectedNote.value) return

  const userMsg = aiInput.value.trim()
  aiMessages.value = [...aiMessages.value, { role: 'user', content: userMsg }]
  aiInput.value = ''

  const noteTitle = selectedNote.value.title || '未命名'
  const noteContent = selectedNote.value.content || ''
  if (!noteContent.trim()) {
    aiMessages.value = [...aiMessages.value, {
      role: 'ai',
      content: '未获取到笔记内容，无法回答该问题。'
    }]
    return
  }

  aiAbortController.value?.abort()
  const controller = new AbortController()
  aiAbortController.value = controller
  isAiTyping.value = true

  const aiIndex = aiMessages.value.length
  aiMessages.value = [...aiMessages.value, { role: 'ai', content: '' }]
  let accumulatedContent = ''
  const customPrompt = `你是笔记助手，请基于笔记内容回答用户问题。
如果笔记中没有提到相关信息，请明确说明“笔记未提及”。
回答要简洁清晰。

笔记标题：《${noteTitle}》
用户问题：${userMsg}`

  try {
    await processNoteWithAI(
      {
        action: 'custom',
        fullContent: noteContent,
        customPrompt
      },
      (chunk: string) => {
        accumulatedContent += chunk
        aiMessages.value = aiMessages.value.map((msg, idx) =>
          idx === aiIndex ? { ...msg, content: accumulatedContent } : msg
        )
      },
      controller.signal
    )
  } catch (error: any) {
    if (error?.name === 'AbortError') return
    console.error('[NoteSquare] AI问答失败:', error)
    aiMessages.value = aiMessages.value.map((msg, idx) =>
      idx === aiIndex
        ? { ...msg, content: '抱歉，回答生成失败，请稍后再试。' }
        : msg
    )
  } finally {
    isAiTyping.value = false
    aiAbortController.value = null
  }
}

function getInitial(name: string) {
  return name ? name.charAt(0) : '用'
}

function getPageTitle() {
  if (activeTab.value === 'favorites') return '我的收藏'
  if (activeTab.value === 'likes') return '我的点赞'
  if (activeTab.value === 'my-notes') return '我的笔记'
  return '笔记广场'
}

watch(selectedNote, (note) => {
  if (!note) return
  isLiked.value = Boolean(note.isLiked)
  likeCount.value = note.likes
  isBookmarked.value = Boolean(note.isBookmarked)
  favoriteCount.value = note.favorites || 0
  viewCount.value = note.views || 0
  commentText.value = ''
  comments.value = []
  commentError.value = ''
  commentsLoading.value = false
  replyTarget.value = null
  isForked.value = false
  showAIChat.value = openAiOnSelect.value
  aiMessages.value = [
    {
      role: 'ai',
      content: `你好！我是 AI 笔记助手。我已经阅读了《${note.title}》，你可以问我任何关于这篇笔记的问题，比如：\n\n1. 这篇笔记的核心观点是什么？\n2. 作者提到的“${note.tags[0] || '关键点'}”是指什么？`
    }
  ]
  aiInput.value = ''
})

watch([aiMessages, showAIChat], async () => {
  if (!showAIChat.value) return
  await nextTick()
  chatEndRef.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
})
</script>

<template>
  <div v-if="selectedNote" class="h-full flex flex-col md:flex-row overflow-hidden bg-white">
    <div class="hidden md:flex md:w-1/2 lg:w-3/5 bg-white relative">
      <Button
        variant="ghost"
        size="icon"
        class="absolute left-4 top-4 text-gray-700 hover:bg-gray-100 z-10"
        @click="handleBack"
      >
        <ArrowLeft class="w-6 h-6" />
      </Button>
      <div class="w-full h-full overflow-y-auto px-8 py-10 markdown-preview note-reader" v-html="renderedNoteHtml"></div>
    </div>

    <div class="w-full md:w-1/2 lg:w-2/5 flex flex-col h-full bg-white relative">
      <div class="md:hidden flex items-center justify-between p-4 border-b">
        <Button variant="ghost" size="icon" @click="handleBack">
          <ArrowLeft class="w-5 h-5" />
        </Button>
        <div class="flex items-center gap-2">
          <Avatar class="w-8 h-8">
            <img
              v-if="selectedNote.user.avatar"
              :src="selectedNote.user.avatar"
              :alt="selectedNote.user.name"
              class="w-full h-full object-cover rounded-full"
            />
            <div v-else class="w-full h-full bg-gray-200 rounded-full flex items-center justify-center text-xs text-gray-600">
              {{ getInitial(selectedNote.user.name) }}
            </div>
          </Avatar>
          <span class="font-medium text-sm">{{ selectedNote.user.name }}</span>
        </div>
        <Button variant="ghost" size="icon">
          <Share2 class="w-5 h-5" />
        </Button>
      </div>

      <div class="flex-1 overflow-y-auto flex flex-col">
        <div class="p-6 flex-1 flex flex-col">
          <div class="hidden md:flex items-center justify-between mb-6">
            <div class="flex items-center gap-3">
              <Avatar class="w-10 h-10 border border-gray-100">
                <img
                  v-if="selectedNote.user.avatar"
                  :src="selectedNote.user.avatar"
                  :alt="selectedNote.user.name"
                  class="w-full h-full object-cover rounded-full"
                />
                <div v-else class="w-full h-full bg-gray-200 rounded-full flex items-center justify-center text-xs text-gray-600">
                  {{ getInitial(selectedNote.user.name) }}
                </div>
              </Avatar>
              <div>
                <div class="font-medium text-sm">{{ selectedNote.user.name }}</div>
                <div class="text-xs text-gray-500">发布于 {{ selectedNote.createdAt }}</div>
              </div>
            </div>

            <Button
              :variant="showAIChat ? 'default' : 'outline'"
              size="sm"
              :class="['gap-2 transition-all', showAIChat ? 'bg-blue-600 hover:bg-blue-700 text-white' : 'text-blue-600 border-blue-200 hover:bg-blue-50']"
              @click="showAIChat = !showAIChat"
            >
              <component :is="showAIChat ? X : Sparkles" class="w-4 h-4" />
              {{ showAIChat ? '关闭助手' : 'AI 解读' }}
            </Button>
          </div>

          <template v-if="!showAIChat">
            <h1 class="text-xl font-bold text-gray-900 mb-3">{{ selectedNote.title }}</h1>
            <div class="md:hidden text-gray-800 text-sm leading-relaxed mb-4 markdown-preview" v-html="renderedNoteHtml"></div>

            <div class="flex flex-wrap gap-2 mb-6">
              <Badge v-for="tag in selectedNote.tags" :key="tag" variant="secondary" class="text-blue-700 bg-blue-50">
                #{{ tag }}
              </Badge>
            </div>

            <div class="flex flex-wrap items-center gap-4 text-xs text-gray-500 mb-4">
              <span>阅读 {{ viewCount }}</span>
              <span>点赞 {{ likeCount }}</span>
              <span>收藏 {{ favoriteCount }}</span>
              <span>评论 {{ commentTotal }}</span>
            </div>

            <div class="flex items-center justify-between text-xs text-gray-400 mb-6">
              <span>{{ selectedNote.createdAt }}</span>
              <span v-if="selectedNote.status" class="bg-gray-100 px-2 py-0.5 rounded text-gray-600">
                {{ selectedNote.status === 'published' ? '已发布' : '草稿' }}
              </span>
            </div>

            <div class="border-t border-gray-100 mb-6" />
          </template>

          <div class="flex-1 min-h-[300px] relative">
            <div
              v-if="showAIChat"
              class="absolute inset-0 flex flex-col bg-blue-50/50 rounded-xl border border-blue-100 overflow-hidden"
            >
              <div class="p-3 bg-white border-b border-blue-100 flex items-center justify-between">
                <div class="flex items-center gap-2 text-blue-700 font-medium">
                  <Bot class="w-5 h-5" />
                  AI 笔记助手
                </div>
              </div>

              <div class="flex-1 overflow-y-auto p-4 space-y-4">
                <div v-for="(msg, idx) in aiMessages" :key="idx" :class="['flex', msg.role === 'user' ? 'justify-end' : 'justify-start']">
                  <div
                    :class="[
                      'max-w-[85%] rounded-lg p-3 text-sm whitespace-pre-wrap',
                      msg.role === 'user'
                        ? 'bg-blue-600 text-white'
                        : 'bg-white border border-gray-100 text-gray-800 shadow-sm'
                    ]"
                  >
                    {{ msg.content }}
                  </div>
                </div>
                <div ref="chatEndRef" />
              </div>
            </div>

            <div v-else>
              <div class="text-sm font-medium mb-4 text-gray-500">共 {{ commentTotal }} 条评论</div>
              <div v-if="commentsLoading" class="text-sm text-gray-400 py-6">评论加载中...</div>
              <div v-else-if="commentError" class="text-sm text-red-500 py-6">{{ commentError }}</div>
              <div v-else-if="comments.length === 0" class="text-sm text-gray-400 py-6">暂无评论</div>
              <div v-else class="space-y-6 pb-4">
                <div v-for="comment in comments" :key="comment.id" class="flex gap-3">
                  <Avatar class="w-8 h-8 mt-1">
                    <img
                      v-if="comment.user.avatar"
                      :src="comment.user.avatar"
                      :alt="comment.user.name"
                      class="w-full h-full object-cover rounded-full"
                    />
                    <div v-else class="w-full h-full bg-gray-200 rounded-full flex items-center justify-center text-xs text-gray-600">
                      {{ getInitial(comment.user.name) }}
                    </div>
                  </Avatar>
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-1">
                      <span class="text-sm text-gray-600 font-medium">{{ comment.user.name }}</span>
                    </div>
                    <p class="text-sm text-gray-800 mb-2">
                      <span v-if="comment.replyToUserName" class="text-gray-500">回复 {{ comment.replyToUserName }}：</span>
                      {{ comment.content }}
                    </p>
                    <div class="flex items-center gap-4 text-xs text-gray-400">
                      <span>{{ comment.createdAt }}</span>
                      <button class="hover:text-gray-600" @click="handleReply(comment)">回复</button>
                      <button class="flex items-center gap-1 hover:text-gray-600">
                        <Heart class="w-3 h-3" /> {{ comment.likes }}
                      </button>
                      <button v-if="comment.isOwner" class="hover:text-red-500" @click="handleDeleteComment(comment.id)">删除</button>
                    </div>
                    <div
                      v-if="comment.replies && comment.replies.length > 0"
                      class="mt-3 ml-2 pl-3 border-l-2 border-gray-100 space-y-3"
                    >
                      <div v-for="reply in comment.replies" :key="reply.id" class="flex gap-2">
                        <Avatar class="w-6 h-6">
                          <img
                            v-if="reply.user.avatar"
                            :src="reply.user.avatar"
                            :alt="reply.user.name"
                            class="w-full h-full object-cover rounded-full"
                          />
                          <div v-else class="w-full h-full bg-gray-200 rounded-full flex items-center justify-center text-xs text-gray-600">
                            {{ getInitial(reply.user.name) }}
                          </div>
                        </Avatar>
                        <div class="flex-1">
                          <span class="text-xs text-gray-600 font-medium mr-2">{{ reply.user.name }}</span>
                          <span v-if="reply.replyToUserName" class="text-xs text-gray-500 mr-1">
                            回复 {{ reply.replyToUserName }}：
                          </span>
                          <span class="text-xs text-gray-800">{{ reply.content }}</span>
                          <div class="mt-1 flex items-center gap-3 text-[10px] text-gray-400">
                            <span>{{ reply.createdAt }}</span>
                            <button class="hover:text-gray-600" @click="handleReply(comment, reply)">回复</button>
                            <button v-if="reply.isOwner" class="hover:text-red-500" @click="handleDeleteComment(reply.id)">删除</button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="absolute bottom-0 left-0 right-0 bg-white border-t border-gray-100 p-4 flex items-center justify-between gap-4 z-20">
        <div class="flex-1 relative">
          <div v-if="showAIChat" class="relative">
            <Input
              v-model="aiInput"
              placeholder="问问 AI 关于这篇笔记的问题..."
              class="bg-blue-50 border-blue-100 focus:border-blue-300 rounded-full pl-4 pr-10"
              @keydown.enter="handleSendAI"
            />
            <Button
              size="icon"
              variant="ghost"
              class="absolute right-1 top-1/2 -translate-y-1/2 w-8 h-8 text-blue-600 hover:bg-blue-100 rounded-full"
              @click="handleSendAI"
            >
              <Send class="w-4 h-4" />
            </Button>
          </div>
          <div v-else class="relative">
            <div v-if="replyTarget" class="absolute -top-5 left-2 text-xs text-gray-500">
              回复 {{ replyTarget.userName }}
              <button class="ml-2 text-blue-500 hover:text-blue-600" @click="handleCancelReply">取消</button>
            </div>
            <Input
              v-model="commentText"
              :placeholder="replyTarget ? `回复 ${replyTarget.userName}...` : '说点什么...'"
              :disabled="commentSubmitting"
              class="bg-gray-100 border-none rounded-full pl-4 pr-10"
              @keydown.enter="handleSendComment"
            />
            <Button
              v-if="commentText"
              size="icon"
              variant="ghost"
              class="absolute right-1 top-1/2 -translate-y-1/2 w-8 h-8 text-blue-600"
              :disabled="commentSubmitting"
              @click="handleSendComment"
            >
              <Send class="w-4 h-4" />
            </Button>
          </div>
        </div>

        <div class="flex items-center gap-1 md:gap-3">
          <Button
            variant="ghost"
            size="icon"
            :class="['hover:bg-transparent', isLiked ? 'text-red-500' : 'text-gray-600']"
            @click="handleLike"
          >
            <Heart :class="['w-6 h-6', isLiked ? 'fill-current' : '']" />
            <span class="sr-only">Like</span>
          </Button>
          <span class="text-sm text-gray-500 min-w-[20px] hidden md:block">{{ likeCount }}</span>

          <Button
            variant="ghost"
            size="icon"
            :class="['hover:bg-transparent', isBookmarked ? 'text-yellow-500' : 'text-gray-600']"
            @click="handleBookmark"
          >
            <Bookmark :class="['w-6 h-6', isBookmarked ? 'fill-current' : '']" />
          </Button>

          <Button
            variant="ghost"
            size="icon"
            :class="['hover:bg-transparent', isForked ? 'text-green-600' : 'text-gray-600']"
            title="转存到我的笔记"
            @click="handleFork"
          >
            <component :is="isForked ? Check : Copy" class="w-6 h-6" />
          </Button>

          <Button variant="ghost" size="icon" class="text-gray-600 hover:bg-transparent">
            <Share2 class="w-6 h-6" />
          </Button>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="h-full flex bg-gray-50">
    <!-- 主内容区域 -->
    <div :class="['flex-1 flex flex-col h-full transition-all duration-300', showQuickAI ? 'mr-0' : '']">
    <div class="bg-white/85 backdrop-blur border-b border-gray-200 sticky top-0 z-10">
      <div class="max-w-6xl mx-auto px-6 py-3 flex items-center justify-between gap-4">
        <h1 class="text-[17px] font-semibold tracking-[-0.01em] text-gray-900 flex-shrink-0">{{ getPageTitle() }}</h1>
        <div class="flex-1 max-w-md relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
          <Input
            v-model="searchQuery"
            class="pl-9 h-9 bg-gray-50 border-gray-200 focus:bg-white transition-all"
            placeholder="搜索笔记标题或正文…"
          />
        </div>
      </div>

      <div
        v-if="['recommend', 'latest'].includes(activeTab)"
        class="max-w-6xl mx-auto px-6 pb-2.5 flex items-center gap-1"
      >
        <button
          :class="['px-3.5 py-1.5 rounded-lg text-[13px] transition-colors', activeTab === 'recommend' ? 'bg-blue-50 text-blue-700 font-medium' : 'text-gray-500 hover:bg-gray-50']"
          @click="activeTab = 'recommend'"
        >
          推荐
        </button>
        <button
          :class="['px-3.5 py-1.5 rounded-lg text-[13px] transition-colors', activeTab === 'latest' ? 'bg-blue-50 text-blue-700 font-medium' : 'text-gray-500 hover:bg-gray-50']"
          @click="activeTab = 'latest'"
        >
          最新
        </button>
        <span class="ml-4 text-xs text-gray-500">分类</span>
        <Select
          v-model="selectedCategory"
          :options="categoryOptions"
          :disabled="isLoadingCategories"
          class="w-36"
        />
      </div>

      <div v-else class="max-w-6xl mx-auto px-6 pb-2.5 text-[12.5px] text-gray-500">
        共 {{ total || notes.length }} 条内容
      </div>
    </div>

    <div class="flex-1 overflow-hidden flex flex-col">
      <div class="flex-1 overflow-y-auto px-6 py-5">
        <div class="max-w-6xl mx-auto">
          <div class="grid grid-cols-1 gap-3">
          <div
            v-if="showAISummary"
            class="rounded-xl border border-blue-100 bg-blue-50/60 p-4 shadow-sm"
          >
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2 text-blue-700 font-medium">
                <Sparkles class="w-4 h-4" />
                AI 智能总结
              </div>
              <div class="flex items-center gap-3 text-xs text-blue-500">
                <span v-if="isAISummarizing">生成中...</span>
                <button
                  v-if="isAISummarizing"
                  class="text-blue-600 hover:text-blue-700"
                  @click="handleStopAISummary"
                >
                  停止
                </button>
              </div>
            </div>
            <div v-if="aiSummaryError" class="text-sm text-red-500 flex items-center gap-2">
              <span>{{ aiSummaryError }}</span>
              <button class="text-blue-600 hover:text-blue-700 text-xs" @click="maybeFetchAISummary">
                重试
              </button>
            </div>
            <div v-else class="text-sm text-gray-700 whitespace-pre-wrap">
              <span v-if="!aiSummaryContent && isAISummarizing">AI正在生成总结...</span>
              <span v-else>{{ aiSummaryContent || '暂无总结内容' }}</span>
            </div>
          </div>
          <div
            v-for="note in notes"
            :key="note.id"
            class="wc-panel p-5 hover:shadow-md transition-shadow cursor-pointer group flex gap-5"
            @click="handleSelectNote(note)"
          >
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-2.5">
                <Avatar class="w-6 h-6">
                  <img
                    v-if="note.user.avatar"
                    :src="note.user.avatar"
                    :alt="note.user.name"
                    class="w-full h-full object-cover rounded-full"
                  />
                  <div v-else class="w-full h-full bg-gray-200 rounded-full flex items-center justify-center text-[10px] text-gray-600">
                    {{ getInitial(note.user.name) }}
                  </div>
                </Avatar>
                <span class="text-[12.5px] font-medium text-gray-600">{{ note.user.name }}</span>
                <Badge v-if="note.category" variant="secondary" class="text-gray-600 bg-gray-100">
                  {{ note.category }}
                </Badge>
              </div>

              <h3 class="text-[15.5px] font-semibold text-gray-900 mb-1.5 group-hover:text-blue-600 transition-colors line-clamp-1">{{ note.title }}</h3>
              <div
                class="text-gray-500 text-[13px] leading-[1.7] line-clamp-2 mb-3 note-preview"
                v-html="renderPreview(note)"
              />

              <div class="flex items-center gap-4 text-xs flex-wrap">
                <Button
                  variant="ghost"
                  size="sm"
                  :class="[
                    'h-8 px-3 rounded-full hover:bg-gray-100 gap-1.5',
                    note.isLiked ? 'text-blue-600 bg-blue-50 hover:bg-blue-100' : 'text-gray-500'
                  ]"
                  @click.stop="toggleNoteLike(note.id)"
                >
                  <ThumbsUp :class="['w-3.5 h-3.5', note.isLiked ? 'fill-current' : '']" />
                  <span>{{ note.likes || '点赞' }}</span>
                </Button>

                <Button
                  variant="ghost"
                  size="sm"
                  :class="[
                    'h-8 px-3 rounded-full hover:bg-gray-100 gap-1.5',
                    note.isBookmarked ? 'text-yellow-600 bg-yellow-50 hover:bg-yellow-100' : 'text-gray-500'
                  ]"
                  @click.stop="toggleNoteBookmark(note.id)"
                >
                  <Bookmark :class="['w-3.5 h-3.5', note.isBookmarked ? 'fill-current' : '']" />
                  <span>{{ note.isBookmarked ? '已收藏' : '收藏' }}</span>
                </Button>

                <Button
                  variant="ghost"
                  size="sm"
                  class="h-8 px-3 rounded-full hover:bg-blue-100 hover:text-blue-700 text-gray-500 gap-1.5"
                  @click.stop="handleQuickAI(note)"
                >
                  <Sparkles class="w-3.5 h-3.5" />
                  <span>AI速读</span>
                </Button>

                <div v-if="activeTab === 'my-notes'" class="ml-auto flex items-center gap-3">
                  <span :class="['px-2 py-0.5 rounded-full', note.status === 'published' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600']">
                    {{ note.status === 'published' ? '已发布' : '草稿' }}
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    class="h-6 w-6 text-gray-400 hover:text-orange-500"
                    title="取消发布"
                    @click.stop="handleUnpublish(note.id)"
                  >
                    <EyeOff class="w-4 h-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    class="h-6 w-6 text-gray-400 hover:text-red-500"
                    title="删除"
                    @click.stop="handleDelete(note.id)"
                  >
                    <Trash2 class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </div>

            <div v-if="note.images.length > 0" class="w-44 h-28 flex-shrink-0 bg-gray-100 rounded-[10px] overflow-hidden border border-gray-200/60">
              <img
                :src="note.images[0]"
                :alt="note.title"
                class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />
            </div>
          </div>
        </div>

        <div v-if="notes.length === 0" class="wc-empty min-h-[440px]">
          <div class="wc-empty-icon"><Search /></div>
          <p class="wc-empty-title">
            {{ activeTab === 'favorites'
              ? '还没有收藏任何笔记'
              : activeTab === 'likes'
                ? '还没有点赞任何笔记'
                : activeTab === 'my-notes'
                  ? '您还没有发布笔记'
                  : '没有找到相关笔记'
            }}
          </p>
          <p class="wc-empty-desc">
            {{ activeTab === 'favorites'
              ? '在笔记广场读到喜欢的文章，点一下收藏，它就会出现在这里。'
              : activeTab === 'likes'
                ? '给读过的笔记点个赞，方便以后回来找。'
                : activeTab === 'my-notes'
                  ? '写下第一篇笔记，发布之后就会出现在这里。'
                  : '换个关键词试试，或者回到推荐看看大家最近在写什么。'
            }}
          </p>
        </div>
        </div>
      </div>

      <div v-if="total > pageSize" class="flex justify-center py-4 border-t border-gray-100">
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

    <!-- AI速读侧边栏 -->
    <transition name="slide-right">
        <div v-if="showQuickAI" class="w-96 h-full bg-white border-l border-gray-200 flex flex-col shadow-lg">
          <!-- 头部 -->
          <div class="flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-white">
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
                <Bot class="w-5 h-5 text-white" />
              </div>
            <div>
              <h3 class="text-sm font-semibold text-gray-900">AI助手・阅读助手</h3>
            </div>
          </div>
          <Button variant="ghost" size="icon" class="w-8 h-8 text-gray-400 hover:text-gray-600" @click="closeQuickAI">
            <X class="w-4 h-4" />
          </Button>
        </div>

        <!-- 欢迎区域 -->
          <div class="px-4 py-3 bg-blue-50/50 border-b border-blue-100">
            <div class="flex items-start gap-2">
              <Sparkles class="w-5 h-5 text-blue-500 mt-0.5" />
              <div>
                <p class="text-sm font-medium text-blue-700">你好，我是阅读助手</p>
                <p class="text-xs text-blue-500 mt-1">我可以帮你总结全文、你也可以向我提问。</p>
              </div>
            </div>
          </div>

        <!-- 对话区域 -->
        <div class="flex-1 overflow-y-auto p-4 space-y-4">
          <div v-for="(msg, idx) in quickAIMessages" :key="idx" :class="['flex', msg.role === 'user' ? 'justify-end' : 'justify-start']">
            <div
              :class="[
                'max-w-[90%] rounded-lg p-3 text-sm',
                msg.role === 'user'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-800 border border-gray-200'
              ]"
            >
              <div v-if="msg.role === 'ai'" class="whitespace-pre-wrap markdown-content" v-html="DOMPurify.sanitize(marked.parse(msg.content) as string)"></div>
              <span v-else>{{ msg.content }}</span>
            </div>
          </div>
          <div ref="quickAIChatEndRef" />
        </div>

        <!-- 底部操作区 -->
        <div class="border-t border-gray-200 p-3">
          <div class="relative">
            <Input
              v-model="quickAIInput"
              placeholder="输入你的问题..."
              class="pr-10 bg-gray-50 border-gray-200 focus:border-blue-300 rounded-lg"
              @keydown.enter="handleSendQuickAI"
            />
            <Button
              size="icon"
              variant="ghost"
              class="absolute right-1 top-1/2 -translate-y-1/2 w-8 h-8 text-blue-600 hover:bg-blue-100 rounded-lg"
              @click="handleSendQuickAI"
            >
              <Send class="w-4 h-4" />
            </Button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
.slide-right-enter-to,
.slide-right-leave-from {
  transform: translateX(0);
  opacity: 1;
}

.markdown-content strong {
  font-weight: 600;
}

.note-preview :deep(p),
.note-preview :deep(ul),
.note-preview :deep(ol) {
  margin: 0;
}

.note-preview :deep(h1),
.note-preview :deep(h2),
.note-preview :deep(h3),
.note-preview :deep(h4),
.note-preview :deep(h5),
.note-preview :deep(h6) {
  font-size: inherit;
  font-weight: 600;
  margin: 0;
}

.note-preview :deep(li) {
  margin: 0;
}
</style>
