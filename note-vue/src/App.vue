<script setup lang="ts">
import { defineAsyncComponent, ref, computed, onMounted, onUnmounted, watch, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 布局组件
import NotebookSidebar from './layout/NotebookSidebar.js'
// 通用组件
const NoteEditor = defineAsyncComponent(() => import('./components/note/NoteEditor.vue'))
import Button from './components/ui/Button.js'
import NotebookSelector from './components/ui/NotebookSelector.vue'
const AIChatPage = defineAsyncComponent(() => import('./views/AIChat/index.vue'))
const TemplateCenterPage = defineAsyncComponent(() => import('./views/TemplateCenter/index.vue'))
const SettingsPage = defineAsyncComponent(() => import('./views/Settings/index.vue'))
const SharedKnowledgeBasePage = defineAsyncComponent(() => import('./views/SharedKnowledgeBase/index.vue'))
const PersonalKnowledgeBasePage = defineAsyncComponent(() => import('./views/PersonalKnowledgeBase/index.vue'))
const RecycleBinPage = defineAsyncComponent(() => import('./views/RecycleBin/index.vue'))
const MemberSystemPage = defineAsyncComponent(() => import('./views/MemberSystem/index.vue'))
const LearningPlansPage = defineAsyncComponent(() => import('./views/LearningPlans/index.vue'))
const NoteSquarePage = defineAsyncComponent(() => import('./views/NoteSquare/NoteSquarePage.vue'))
const NoteSquareFavoritesPage = defineAsyncComponent(() => import('./views/NoteSquare/Favorites.vue'))
const NoteSquareLikesPage = defineAsyncComponent(() => import('./views/NoteSquare/Likes.vue'))
const NoteSquareMyNotesPage = defineAsyncComponent(() => import('./views/NoteSquare/MyNotes.vue'))
const TeamPage = defineAsyncComponent(() => import('./views/Team/index.vue'))
const AuthPage = defineAsyncComponent(() => import('./views/Auth/index.vue'))
const PaymentResultPage = defineAsyncComponent(() => import('./views/PaymentResult/index.vue'))
import MemberCenterModal from './components/member/MemberCenterModal.vue'
// 图标
import { Menu } from 'lucide-vue-next'
// 类型
import type { Note, Notebook, FilterType } from './types/note'
import type { Template } from './views/TemplateCenter/index.vue'
// API
import {
  getNotebooks,
  getNotes,
  getNoteDetail,
  createNote as apiCreateNote,
  updateNote as apiUpdateNote,
  deleteNote as apiDeleteNote,
  togglePinNote as apiTogglePinNote,
  createNotebook as apiCreateNotebook,
  deleteNotebook as apiDeleteNotebook,
  exportNote as apiExportNote
} from './api/notes.js'
import { getTemplateById } from './api/template.js'
import { useModal } from './composables/useModal'

const route = useRoute()
const router = useRouter()
const modal = useModal()

// 登录状态
const isLoggedIn = ref(false)

// 数据加载状态
const loading = ref(false)
const notes = ref<Note[]>([])
const notebooks = ref<Notebook[]>([])
const selectedFilter = ref<FilterType>('全部')
const selectedNoteId = ref<string | null>(null)
const selectedNoteDetail = ref<Note | null>(null)
const isSidebarOpen = ref(false)
const currentPage = ref<
  'notes' |
  'note-square' |
  'note-favorites' |
  'note-likes' |
  'note-my-notes' |
  'team' |
  'ai-chat' |
  'template-center' |
  'settings' |
  'shared-knowledge-base' |
  'personal-knowledge-base' |
  'recycle-bin' |
  'member-system'
  | 'learning-plans'
>('notes')
const recycleBinKey = ref(0)
const showMemberCenterModal = ref(false)

/**
 * 将后端返回的笔记数据转换为前端格式
 */
function transformNote(backendNote: any): Note {
  return {
    id: String(backendNote.id),
    title: backendNote.title || '',
    content: backendNote.contentMd || '',
    tags: backendNote.tags || [],
    notebookId: String(backendNote.notebookId || ''),
    isPinned: backendNote.isPinned || false,
    createdAt: backendNote.createTime ? new Date(backendNote.createTime) : new Date(),
    updatedAt: backendNote.updateTime ? new Date(backendNote.updateTime) : new Date()
  }
}

/**
 * 将后端返回的笔记本数据转换为前端格式
 */
function transformNotebook(backendNotebook: any): Notebook {
  return {
    id: String(backendNotebook.id),
    name: backendNotebook.name || '',
    noteCount: backendNotebook.noteCount || 0
  }
}

/**
 * 加载笔记本列表
 */
async function loadNotebooks() {
  try {
    const res = await getNotebooks()
    if (res && res.code === 0 && res.data) {
      notebooks.value = res.data.map(transformNotebook)
      console.log('[App] 笔记本加载成功:', notebooks.value.length)
    }
  } catch (error) {
    console.error('[App] 加载笔记本失败:', error)
  }
}

/**
 * 加载笔记列表
 */
async function loadNotesList(notebookId?: string) {
  loading.value = true
  try {
    const params: any = { page: 1, size: 100 }
    if (notebookId && notebookId !== '全部' && notebookId !== '最近7天') {
      params.notebookId = notebookId
    }
    const res = await getNotes(params)
    if (res && res.code === 0 && res.data) {
      notes.value = (res.data.data || res.data.records || []).map(transformNote)
      console.log('[App] 笔记加载成功:', notes.value.length)
    }
  } catch (error) {
    console.error('[App] 加载笔记失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 加载笔记详情
 */
async function loadNoteDetail(noteId: string) {
  try {
    const res = await getNoteDetail(noteId)
    if (res && res.code === 0 && res.data && selectedNoteId.value === noteId) {
      selectedNoteDetail.value = transformNote(res.data)
    }
  } catch (error) {
    console.error('[App] 加载笔记详情失败:', error)
  }
}

/**
 * 初始化数据
 */
async function initData() {
  if (!isLoggedIn.value) return
  await loadNotebooks()
  await loadNotesList()
}

/**
 * 通知回收站需要刷新（供子组件调用）
 */
function notifyRecycleBinRefresh() {
  recycleBinKey.value++
  console.log('[App] 回收站刷新通知，key:', recycleBinKey.value)
}

/**
 * 刷新笔记和笔记本列表（供子组件调用）
 */
async function refreshNotesAndNotebooks() {
  console.log('[App] 刷新笔记和笔记本列表')
  await loadNotebooks()
  // 重新加载当前视图的笔记列表
  if (selectedFilter.value === '全部' || selectedFilter.value === '最近7天') {
    await loadNotesList()
  } else {
    const notebook = notebooks.value.find(nb => nb.id === selectedFilter.value)
    if (notebook) {
      await loadNotesList(notebook.id)
    }
  }
}

// 提供给子组件使用
provide('notifyRecycleBinRefresh', notifyRecycleBinRefresh)
provide('refreshNotesAndNotebooks', refreshNotesAndNotebooks)

// 登录处理
function handleLogin() {
  isLoggedIn.value = true
}

// 登出处理
function handleLogout() {
  isLoggedIn.value = false
  void router.replace({ name: 'Auth' })
}

async function handleExportNote(note: Note) {
  if (!note?.id) return
  const format = await modal.prompt({
    title: '导出笔记',
    message: '请选择导出格式',
    defaultValue: 'pdf',
    inputType: 'select',
    selectOptions: [
      { label: 'PDF（.pdf）', value: 'pdf' },
      { label: 'Word（.docx）', value: 'docx' },
      { label: 'Excel（.xlsx）', value: 'xlsx' },
      { label: 'Markdown（.md）', value: 'md' }
    ]
  })
  if (!format) return
  const normalized = format.trim().toLowerCase()
  if (!['pdf', 'docx', 'xlsx', 'md'].includes(normalized)) {
    await modal.warning('导出格式不支持')
    return
  }

  try {
    const blob = await apiExportNote(note.id, normalized)
    const filename = `${note.title || 'note'}.${normalized}`
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('[App] 导出失败:', error)
    await modal.error('导出失败，请重试')
  }
}

// 加载笔记 - 支持过滤和排序
const filteredNotes = computed(() => {
  let filtered = [...notes.value]

  // 过滤逻辑 - 前端过滤（后端已按notebookId过滤的情况下做额外处理）
  if (selectedFilter.value === '最近7天') {
    const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
    filtered = filtered.filter(note => note.updatedAt >= sevenDaysAgo)
  } else if (selectedFilter.value !== '全部') {
    filtered = filtered.filter(note => note.notebookId === selectedFilter.value)
  }

  // 排序：置顶的在前，然后按更新时间倒序
  filtered.sort((a, b) => {
    if (a.isPinned && !b.isPinned) return -1
    if (!a.isPinned && b.isPinned) return 1
    return b.updatedAt.getTime() - a.updatedAt.getTime()
  })

  return filtered
})

// 创建新笔记
async function handleCreateNote() {
  let notebookId: string = ''
  let notebookName: string = ''
  
  // 判断当前是否在特定笔记本下
  const isInSpecificNotebook = selectedFilter.value !== '全部' && selectedFilter.value !== '最近7天'
  
  if (isInSpecificNotebook) {
    // 在特定笔记本下，直接创建到该笔记本
    notebookId = selectedFilter.value
    const currentNotebook = notebooks.value.find(nb => nb.id === notebookId)
    notebookName = currentNotebook?.name || ''
  } else {
    // 在全局视图下，弹窗让用户选择笔记本
    // 过滤掉默认笔记本，只显示用户创建的笔记本
    const availableNotebooks = notebooks.value.filter(nb => nb.name !== '默认笔记本')
    
    if (availableNotebooks.length === 0) {
      // 如果没有可用笔记本，提示用户先创建
      const shouldCreate = await modal.confirm({
        title: '创建笔记本',
        message: '您还没有笔记本，是否立即创建一个？',
        type: 'info',
        confirmText: '创建',
        cancelText: '取消'
      })
      
      if (shouldCreate) {
        // 引导用户创建笔记本
        const newName = await modal.prompt({
          title: '新建笔记本',
          message: '请输入笔记本名称',
          placeholder: '请输入笔记本名称...',
          confirmText: '创建',
          cancelText: '取消'
        })
        
        if (!newName || !newName.trim()) return
        
        try {
          const res = await apiCreateNotebook({ name: newName.trim() })
          if (res && res.code === 0 && res.data) {
            const newNotebook = transformNotebook(res.data)
            notebooks.value.push(newNotebook)
            notebookId = newNotebook.id
            notebookName = newNotebook.name
          }
        } catch (error) {
          console.error('[App] 创建笔记本失败:', error)
          await modal.error('创建笔记本失败，请重试')
          return
        }
      } else {
        return
      }
    } else {
      // 有笔记本时，总是让用户手动选择（不自动选择）
      const selectedId = await showNotebookSelector()
      if (!selectedId) {
        // 用户取消了选择
        return
      }
      notebookId = selectedId
      const selectedNotebook = notebooks.value.find(nb => nb.id === selectedId)
      notebookName = selectedNotebook?.name || ''
    }
  }

  // 创建笔记
  try {
    const res = await apiCreateNote({
      notebookId: Number(notebookId),
      title: '无标题笔记',
      contentMd: '',
      status: 0,
      isPinned: false,
      tags: []
    })
    if (res && res.code === 0 && res.data) {
      const newNote = transformNote(res.data)
      notes.value = [newNote, ...notes.value]
      selectedNoteId.value = newNote.id
      selectedNoteDetail.value = newNote
      // 更新笔记本的笔记数量
      await loadNotebooks()
      console.log('[App] 创建笔记成功，所属笔记本:', notebookName || notebookId)
    }
  } catch (error) {
    console.error('[App] 创建笔记失败:', error)
    await modal.error('创建笔记失败，请重试')
  }
  
  isSidebarOpen.value = false
}

// 显示笔记本选择器
const showNotebookSelectorDialog = ref(false)
const pendingNotebookResolve = ref<((value: string | null) => void) | null>(null)

function showNotebookSelector(): Promise<string | null> {
  return new Promise((resolve) => {
    pendingNotebookResolve.value = resolve
    showNotebookSelectorDialog.value = true
  })
}

function handleNotebookSelect(notebookId: string) {
  showNotebookSelectorDialog.value = false
  if (pendingNotebookResolve.value) {
    pendingNotebookResolve.value(notebookId)
    pendingNotebookResolve.value = null
  }
}

function handleNotebookSelectorCancel() {
  showNotebookSelectorDialog.value = false
  if (pendingNotebookResolve.value) {
    pendingNotebookResolve.value(null)
    pendingNotebookResolve.value = null
  }
}

// 更新笔记
async function handleUpdateNote(noteId: string, updates: Partial<Note>) {
  const existing = selectedNoteDetail.value?.id === noteId
    ? selectedNoteDetail.value : notes.value.find(note => note.id === noteId)
  if (!existing) throw new Error('笔记已移除，无法保存')
  const updatedNote = { ...existing, ...updates }
  // 只更新对应的笔记，不能使用当前选择的 ID 保存旧编辑器的草稿。
  if (selectedNoteDetail.value?.id === noteId) selectedNoteDetail.value = updatedNote
  notes.value = notes.value.map(note => note.id === noteId ? updatedNote : note)
  const result = await apiUpdateNote({
    noteId: Number(noteId),
    title: updatedNote.title || '无标题笔记',
    contentMd: updatedNote.content,
    isPinned: updatedNote.isPinned,
    tags: updatedNote.tags
  })
  if (result?.code !== 0) throw new Error(result?.message || '保存笔记失败')
}

async function handleCreateNotebook() {
  const name = await modal.prompt({
    title: '新建笔记本',
    message: '请输入笔记本名称',
    placeholder: '请输入笔记本名称...',
    confirmText: '创建',
    cancelText: '取消'
  })
  
  if (!name || !name.trim()) return
  
  try {
    const res = await apiCreateNotebook({ name: name.trim() })
    if (res && res.code === 0 && res.data) {
      const newNotebook = transformNotebook(res.data)
      notebooks.value = [...notebooks.value, newNotebook]
      selectedFilter.value = newNotebook.id
      await loadNotesList(newNotebook.id)
      await modal.success(`笔记本「${name.trim()}」创建成功！`)
    } else {
      await modal.error(res?.message || '创建笔记本失败')
    }
  } catch (error) {
    console.error('[App] 创建笔记本失败:', error)
    await modal.error('创建笔记本失败，请重试')
  }
}

async function handleDeleteNotebook(notebookId: string) {
  const notebook = notebooks.value.find(nb => nb.id === notebookId)
  const name = notebook?.name || '该笔记本'
  const confirmed = await modal.confirm({
    title: '删除笔记本',
    message: `确定要删除「${name}」吗？此操作不可恢复。`,
    confirmText: '删除',
    cancelText: '取消',
    type: 'warning'
  })
  if (!confirmed) return
  try {
    const res = await apiDeleteNotebook(notebookId)
    if (res && res.code === 0) {
      notebooks.value = notebooks.value.filter(nb => nb.id !== notebookId)
      if (selectedFilter.value === notebookId) {
        selectedFilter.value = '全部'
      }
      await loadNotesList(selectedFilter.value === '全部' ? undefined : selectedFilter.value)
      await modal.success(`笔记本「${name}」已删除`)
    } else {
      await modal.error(res?.message || '删除笔记本失败')
    }
  } catch (error) {
    console.error('[App] 删除笔记本失败:', error)
    await modal.error('删除笔记本失败，请重试')
  }
}

// 切换置顶
async function handleTogglePin(noteId: string) {
  // 先更新本地状态
  notes.value = notes.value.map(note =>
    note.id === noteId
      ? { ...note, isPinned: !note.isPinned }
      : note
  )
  
  // 调用API
  try {
    await apiTogglePinNote(noteId)
    console.log('[App] 切换置顶成功')
  } catch (error) {
    console.error('[App] 切换置顶失败:', error)
    // 失败时回滚
    notes.value = notes.value.map(note =>
      note.id === noteId
        ? { ...note, isPinned: !note.isPinned }
        : note
    )
  }
}

// 拖动排序
function handleReorder(noteIds: string[]) {
  const idMap = new Map(notes.value.map(n => [n.id, n]))
  const reordered = noteIds.map(id => idMap.get(id)).filter(Boolean) as typeof notes.value
  const rest = notes.value.filter(n => !noteIds.includes(n.id))
  notes.value = [...reordered, ...rest]
}

// 删除笔记
async function handleDeleteNote(noteId: string) {
  try {
    await apiDeleteNote(noteId)
    notes.value = notes.value.filter(note => note.id !== noteId)
    if (selectedNoteId.value === noteId) {
      selectedNoteId.value = notes.value[0]?.id || null
      selectedNoteDetail.value = null
      if (selectedNoteId.value) {
        loadNoteDetail(selectedNoteId.value)
      }
    }
    // 更新笔记本的笔记数量
    await loadNotebooks()
    // 增加回收站key，下次切换到回收站时会自动刷新
    recycleBinKey.value++
    console.log('[App] 删除笔记成功')
  } catch (error) {
    console.error('[App] 删除笔记失败:', error)
  }
}

// 修改笔记所属笔记本
async function handleChangeNotebook(noteId: string, notebookId: string) {
  try {
    // 调用更新API
    const note = selectedNoteDetail.value?.id === noteId ? selectedNoteDetail.value : notes.value.find(n => n.id === noteId)
    if (!note) return
    
    const result = await apiUpdateNote({
      noteId: Number(noteId),
      title: note.title || '无标题笔记',
      notebookId: Number(notebookId),
      contentMd: note.content || '',
      isPinned: note.isPinned || false,
      tags: note.tags || []
    })
    if (result?.code !== 0) throw new Error(result?.message || '移动笔记失败')
    
    // 更新本地状态
    notes.value = notes.value.map(n =>
      n.id === noteId ? { ...n, notebookId } : n
    )
    if (selectedNoteDetail.value?.id === noteId) {
      selectedNoteDetail.value = { ...selectedNoteDetail.value, notebookId }
    }
    
    // 更新笔记本的笔记数量
    await loadNotebooks()
    console.log('[App] 修改笔记本成功')
  } catch (error) {
    console.error('[App] 修改笔记本失败:', error)
    await modal.error(error instanceof Error ? error.message : '移动笔记失败，请重试')
  }
}

// 选择笔记
async function handleSelectNote(note: Note) {
  selectedNoteId.value = note.id
  handleNavigate('notes')
  // 加载完整笔记详情
  await loadNoteDetail(note.id)
}

// 处理模板使用
async function handleUseTemplate(template: Template) {
  if (!template?.id) return
  handleNavigate('notes')
  await handleCreateNoteFromTemplate(String(template.id))
}

// 处理过滤变更
async function handleFilterChange(filter: FilterType) {
  selectedFilter.value = filter
  handleNavigate('notes')
  // 根据筛选条件重新加载笔记
  if (filter !== '全部' && filter !== '最近7天') {
    await loadNotesList(filter)
  } else {
    await loadNotesList()
  }
}

// 处理导航
function handleNavigate(page:
  'notes' |
  'note-square' |
  'note-favorites' |
  'note-likes' |
  'note-my-notes' |
  'team' |
  'ai-chat' |
  'template-center' |
  'settings' |
  'shared-knowledge-base' |
  'personal-knowledge-base' |
  'recycle-bin' |
  'member-system' |
  'learning-plans'
) {
  currentPage.value = page
  isSidebarOpen.value = false
  const routeNameByPage = {
    notes: 'Notes',
    'note-square': 'NoteSquare',
    'note-favorites': 'NoteSquareFavorites',
    'note-likes': 'NoteSquareLikes',
    'note-my-notes': 'NoteSquareMine',
    team: 'Team',
    'ai-chat': 'AIChat',
    'template-center': 'TemplateCenter',
    settings: 'Settings',
    'shared-knowledge-base': 'SharedKnowledgeBase',
    'personal-knowledge-base': 'PersonalKnowledgeBase',
    'recycle-bin': 'RecycleBin',
    'member-system': 'MemberSystem',
    'learning-plans': 'LearningPlans'
  } as const
  void router.push({ name: routeNameByPage[page] })
  // 切换到回收站页面时增加key值，强制组件重新加载
  if (page === 'recycle-bin') {
    recycleBinKey.value++
  }
}

// 处理回收站恢复项目
async function handleItemRestored() {
  console.log('[App] 回收站项目已恢复，刷新笔记列表和笔记本列表')
  // 刷新笔记列表和笔记本列表
  await loadNotesList()
  await loadNotebooks()
}

// 关闭遮罩
function handleOverlayClick() {
  isSidebarOpen.value = false
}

// 当前选中的笔记（使用详情数据）
const selectedNote = computed(() => selectedNoteDetail.value)

// 根据本地令牌初始化登录态
try {
  const token = localStorage.getItem('satoken')
  if (token) {
    isLoggedIn.value = true
  }
} catch {}

function handleSessionExpired() {
  isLoggedIn.value = false
  void router.replace({ name: 'Auth' })
}
onMounted(() => window.addEventListener('session-expired', handleSessionExpired))
onUnmounted(() => window.removeEventListener('session-expired', handleSessionExpired))

// 页面加载时初始化数据
onMounted(() => {
  if (isLoggedIn.value) {
    initData()
  }

})

// 监听登录状态变化
watch(isLoggedIn, (newVal) => {
  if (newVal) {
    initData()
  } else {
    notes.value = []
    notebooks.value = []
    selectedNoteId.value = null
    selectedNoteDetail.value = null
  }
})

const pageByRouteName: Record<string, typeof currentPage.value> = {
  Notes: 'notes',
  NoteSquare: 'note-square',
  NoteSquareFavorites: 'note-favorites',
  NoteSquareLikes: 'note-likes',
  NoteSquareMine: 'note-my-notes',
  Team: 'team',
  AIChat: 'ai-chat',
  TemplateCenter: 'template-center',
  Settings: 'settings',
  SharedKnowledgeBase: 'shared-knowledge-base',
  PersonalKnowledgeBase: 'personal-knowledge-base',
  RecycleBin: 'recycle-bin',
  MemberSystem: 'member-system',
  LearningPlans: 'learning-plans'
}

watch(() => route.name, (name) => {
  if (typeof name === 'string' && pageByRouteName[name]) {
    currentPage.value = pageByRouteName[name]
  }
}, { immediate: true })

watch(() => route.query.noteId, async (noteId) => {
  if (!noteId || !isLoggedIn.value) return
  const id = String(Array.isArray(noteId) ? noteId[0] : noteId)
  try {
    const res = await getNoteDetail(id)
    if (res?.code !== 0 || !res.data) throw new Error(res?.message || '来源笔记不存在或无权访问')
    const note = transformNote(res.data)
    selectedFilter.value = '全部'
    selectedNoteId.value = note.id
    selectedNoteDetail.value = note
    if (!notes.value.some(item => item.id === note.id)) notes.value.unshift(note)
    currentPage.value = 'notes'
    await router.replace({ name: 'Notes' })
  } catch (error) {
    await modal.error(error instanceof Error ? error.message : '打开来源笔记失败')
  }
})

// 监听路由参数,处理模板创建笔记
watch(() => route.query.templateId, async (templateId, oldTemplateId) => {
  // 只在templateId真正变化且不为空时才执行,避免初始化时自动创建
  if (templateId && templateId !== oldTemplateId && isLoggedIn.value) {
    await handleCreateNoteFromTemplate(String(templateId))
    // 清除URL中的templateId参数,避免刷新页面时重复创建
    const { templateId: _, ...queryWithoutTemplateId } = route.query
    if (Object.keys(queryWithoutTemplateId).length === 0) {
      await router.replace({ name: 'Notes', query: queryWithoutTemplateId })
    }
  }
})

// 从模板创建笔记
async function handleCreateNoteFromTemplate(templateId: string) {
  try {
    // 获取模板详情
    const res = await getTemplateById(templateId)
    if (res && res.code === 0 && res.data) {
      const template = res.data
      
      // 确定笔记本ID
      let notebookId = selectedFilter.value !== '全部' && selectedFilter.value !== '最近7天' 
        ? selectedFilter.value 
        : (notebooks.value[0]?.id || '')
      
      // 如果没有笔记本，先创建一个默认笔记本
      if (!notebookId && notebooks.value.length === 0) {
        const notebookRes = await apiCreateNotebook({ name: '默认笔记本' })
        if (notebookRes && notebookRes.code === 0 && notebookRes.data) {
          const newNotebook = transformNotebook(notebookRes.data)
          notebooks.value.push(newNotebook)
          notebookId = newNotebook.id
        }
      }
      
      // 创建笔记
      const noteRes = await apiCreateNote({
        notebookId: Number(notebookId),
        title: template.title,
        contentMd: template.content || `# ${template.title}

${template.description}

---

这里开始编辑您的内容...`,
        status: 0,
        isPinned: false,
        tags: template.tags || []
      })
      
      if (noteRes && noteRes.code === 0 && noteRes.data) {
        const newNote = transformNote(noteRes.data)
        notes.value = [newNote, ...notes.value]
        selectedNoteId.value = newNote.id
        selectedNoteDetail.value = newNote
        handleNavigate('notes')
        // 更新笔记本的笔记数量
        await loadNotebooks()
        console.log('[App] 从模板创建笔记成功')
      }
    }
  } catch (error) {
    console.error('[App] 从模板创建笔记失败:', error)
  }
}
</script>

<template>
  <!-- 未登录时显示登录页 -->
  <PaymentResultPage v-if="route.name === 'PaymentResult'" />
  <AuthPage 
    v-else-if="!isLoggedIn"
    @login="handleLogin" 
  />

  <!-- 已登录时显示主应用 -->
  <div v-else class="h-screen flex bg-[var(--wc-bg,#fbf8f2)] overflow-hidden">
    <!-- 移动端遮罩 -->
    <div 
      v-if="isSidebarOpen"
      class="fixed inset-0 bg-black bg-opacity-50 z-30 lg:hidden"
      @click="handleOverlayClick"
    />

    <!-- 侧边栏 - 移动端可隐藏 -->
    <div :class="[
      'fixed lg:static inset-y-0 left-0 z-40',
      'transform transition-transform duration-300 ease-in-out',
      isSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]">
      <NotebookSidebar 
        :notebooks="notebooks"
        :selected-filter="selectedFilter"
        :notes="notes"
        :selected-note-id="selectedNoteId"
        :current-page="currentPage"
        @filter-change="handleFilterChange"
        @create-note="handleCreateNote"
        @create-notebook="handleCreateNotebook"
        @delete-notebook="handleDeleteNotebook"
        @navigate="handleNavigate"
        @open-member-center="showMemberCenterModal = true"
        @logout="handleLogout"
        @select-note="handleSelectNote"
        @toggle-pin="handleTogglePin"
        @delete-note="handleDeleteNote"
        @export-note="handleExportNote"
        @reorder="handleReorder"
      />
    </div>

    <!-- 根据当前页面显示不同内容 -->
    <template v-if="currentPage === 'notes'">
      <!-- 主编辑区域（移除原来的笔记列表） -->
      <div class="flex-1 flex flex-col min-w-0">
        <!-- 移动端顶部工具栏 -->
        <div class="lg:hidden flex items-center gap-2 p-2 border-b border-[var(--wc-line,#e7dcc9)]">
          <Button
            variant="ghost"
            size="sm"
            aria-label="打开导航菜单"
            @click="isSidebarOpen = !isSidebarOpen"
          >
            <Menu class="w-5 h-5" />
          </Button>

        </div>

        <!-- 编辑器 -->
        <div class="flex-1 overflow-hidden">
          <NoteEditor 
            :note="selectedNote"
            :notebooks="notebooks"
            :save-note="handleUpdateNote"
            @change-notebook="handleChangeNotebook"
            @create-note="handleCreateNote"
            @open-template="handleNavigate('template-center')"
          />
        </div>
      </div>
    </template>

    <NoteSquarePage v-else-if="currentPage === 'note-square'" class="flex-1 min-w-0" />
    <NoteSquareFavoritesPage v-else-if="currentPage === 'note-favorites'" class="flex-1 min-w-0" />
    <NoteSquareLikesPage v-else-if="currentPage === 'note-likes'" class="flex-1 min-w-0" />
    <NoteSquareMyNotesPage v-else-if="currentPage === 'note-my-notes'" class="flex-1 min-w-0" />
    <TeamPage v-else-if="currentPage === 'team'" class="flex-1 min-w-0" />
    <AIChatPage v-else-if="currentPage === 'ai-chat'" class="flex-1 min-w-0" />
    <SettingsPage v-else-if="currentPage === 'settings'" class="flex-1 min-w-0" />
    <SharedKnowledgeBasePage v-else-if="currentPage === 'shared-knowledge-base'" class="flex-1 min-w-0" />
    <PersonalKnowledgeBasePage v-else-if="currentPage === 'personal-knowledge-base'" class="flex-1 min-w-0" />
    <RecycleBinPage 
      v-else-if="currentPage === 'recycle-bin'" 
      :key="recycleBinKey" 
      class="flex-1 min-w-0" 
      @item-restored="handleItemRestored"
    />
    <MemberSystemPage v-else-if="currentPage === 'member-system'" class="flex-1 min-w-0" />
    <LearningPlansPage v-else-if="currentPage === 'learning-plans'" class="flex-1 min-w-0" />
    <TemplateCenterPage 
      v-else 
      class="flex-1 min-w-0"
      @use-template="handleUseTemplate" 
    />

    <Button
      v-if="currentPage !== 'notes' && !isSidebarOpen"
      class="fixed bottom-4 left-4 z-20 shadow-lg lg:hidden"
      aria-label="打开导航菜单"
      @click="isSidebarOpen = true"
    ><Menu class="w-5 h-5 mr-2" />菜单</Button>

    <!-- 笔记本选择器弹窗 -->
    <NotebookSelector
      :show="showNotebookSelectorDialog"
      :notebooks="notebooks.filter(nb => nb.name !== '默认笔记本')"
      @select="handleNotebookSelect"
      @cancel="handleNotebookSelectorCancel"
    />
    <MemberCenterModal v-model:visible="showMemberCenterModal" />
  </div>
</template>
