<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Avatar from '@/components/ui/Avatar.vue'
import Badge from '@/components/ui/Badge.vue'
import Switch from '@/components/ui/Switch.vue'
import Textarea from '@/components/ui/Textarea.vue'
import SimpleMarkdownEditor from '@/components/note/SimpleMarkdownEditor.vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useModal } from '@/composables/useModal'
import {
  Search,
  Plus,
  Upload,
  FileText,
  Settings,
  X,
  Users,
  MessageSquare,
  Lock,
  Trash2,
  ChevronRight,
  Bot,
  Layout,
  ArrowLeft,
  Save,
  PenTool,
  GitBranch,
  Send,
  ChevronDown,
  Check,
  Copy,
  PanelLeftClose,
  PanelLeftOpen
} from 'lucide-vue-next'
import {
  getTeamDetail,
  updateTeam,
  getTeamDocs,
  createDoc,
  getDocDetail,
  updateDoc,
  deleteDoc,
  acquireLock,
  releaseLock,
  getDocVersions,
  getDocActivities,
  restoreDocVersion,
  getDocComments,
  addDocComment,
  resolveComment,
  getTeamMembers,
  updateMemberRole,
  removeMember,
  resetInviteCode,
  leaveTeam,
  dissolveTeam,
  getAISessions,
  createAISession,
  deleteAISession,
  getAIMessages,
  sendAIMessage,
  sendAIMessageStream
} from '@/api/team'
import { uploadFile } from '@/api/apiClient'

type ViewType = 'notes' | 'members' | 'ai'

const props = defineProps<{ teamId: string; openSettings?: boolean }>()

const emit = defineEmits<{ back: [] }>()

const modal = useModal()

interface Note {
  id: string
  title: string
  contentMd: string
  excerpt: string
  updateTime: string
  updatedByUser?: { id: number; username: string; avatarUrl: string }
  tags: string[]
  status: number
  lockedBy?: number | null
  lockedByUser?: { id: number; username: string; avatarUrl: string } | null
  lockToken?: string
  lockExpireTime?: string
  version: number
}

interface CommentItem {
  id: string
  content: string
  user: { id: number; username: string; avatarUrl: string }
  createTime: string
  type: number
  quote?: string
  resolved?: boolean
  parentId?: string | number
}

interface Member {
  id: number
  userId: number
  username: string
  avatarUrl: string
  email: string
  role: number
  status: number
  joinTime: string
}

interface Session {
  id: string
  title: string
  updateTime: string
  isPinned: boolean
  lastMessage?: string
}

const teamInfo = ref<any>(null)
const currentView = ref<ViewType>('notes')
const notes = ref<Note[]>([])
const selectedNoteId = ref<string | null>(null)
const isEditing = ref(false)
const searchQuery = ref('')
const rightPanelTab = ref<'comments' | 'history' | 'activity'>('comments')
const showRightPanel = ref(true)
const showDocListPanel = ref(true)
const editorContent = ref('')
const currentLockToken = ref('')
const deletingDocId = ref<string | null>(null)
let pollTimer: number | null = null

const comments = ref<CommentItem[]>([])
const versions = ref<any[]>([])
const activities = ref<any[]>([])
const members = ref<Member[]>([])
const newComment = ref('')
const replyTo = ref<CommentItem | null>(null)
const commentPending = ref(false)
const resolvingComments = ref(new Set<string>())
const onlyUnresolved = ref(false)
const docLoading = ref(false)
const docError = ref('')
const docSort = ref('recent')
const articleRef = ref<HTMLElement | null>(null)
let docRequest = 0
const canEditDocs = computed(() => teamInfo.value?.myRole != null && teamInfo.value.myRole <= 2)
const openCommentCount = computed(() => comments.value.filter(c => !c.resolved).length)
const visibleComments = computed(() => comments.value.filter(c => !onlyUnresolved.value || !c.resolved))
const selectedNote = computed(() => notes.value.find(n => n.id === selectedNoteId.value) || null)
const filteredNotes = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  const list = notes.value.filter(n => !q || `${n.title} ${n.excerpt || ''}`.toLowerCase().includes(q))
  return [...list].sort((a, b) => docSort.value === 'title'
    ? a.title.localeCompare(b.title, 'zh-CN')
    : new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime())
})

function normalizeDoc(doc: any): Note {
  return { ...doc, id: String(doc.id) }
}

function excerptText(value: string) {
  return (value || '').replace(/[#>*_`\[\]]/g, '').replace(/\s+/g, ' ').trim()
}

const renderedDocument = computed(() => {
  const html = renderMarkdown(selectedNote.value?.contentMd || '')
  const parsed = new DOMParser().parseFromString(html, 'text/html')
  const headings = [...parsed.querySelectorAll('h1,h2,h3')].map((heading, index) => {
    const id = `team-section-${index}`
    heading.id = id
    return { id, text: heading.textContent || '' }
  })
  return { html: parsed.body.innerHTML, headings }
})

function jumpToSection(id: string) {
  articleRef.value?.querySelector(`#${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function unwrapTeamResponse(res: any, fallback: string) {
  if (res?.code !== 0) throw new Error(res?.message || fallback)
  return res.data
}

// 加载团队信息
async function loadTeamInfo() {
  try {
    const res = await getTeamDetail(props.teamId)
    if (res.code === 0) teamInfo.value = res.data
  } catch (e) {
    console.error('加载团队信息失败', e)
  }
}

const showSettingsModal = ref(false)
const showInviteModal = ref(false)

watch(
  () => props.openSettings,
  (v) => {
    if (v) showSettingsModal.value = true
  },
  { immediate: true }
)

function openTeamSettings() {
  showSettingsModal.value = true
}

function closeTeamSettings() {
  showSettingsModal.value = false
}

function openInviteModal() {
  showInviteModal.value = true
}

function closeInviteModal() {
  inviteCopied.value = false
  inviteLinkCopied.value = false
  showInviteModal.value = false
}

const canManageTeam = computed(() => {
  return teamInfo.value && (teamInfo.value.myRole === 0 || teamInfo.value.myRole === 1)
})

const isCreator = computed(() => {
  return teamInfo.value && teamInfo.value.myRole === 0
})

const inviteRoleText = computed(() => {
  if (teamInfo.value?.inviteRole === 2) return '编辑者'
  return '查看者'
})

const inviteRemainingUses = computed(() => {
  const maxUses = teamInfo.value?.inviteMaxUses
  const usedCount = teamInfo.value?.inviteUsedCount
  if (typeof maxUses !== 'number') return '不限'
  return String(Math.max(maxUses - (typeof usedCount === 'number' ? usedCount : 0), 0))
})

const inviteShareText = computed(() => {
  const code = teamInfo.value?.inviteCode
  if (!code) return ''
  const teamName = teamInfo.value?.name || '团队空间'
  return `邀请你加入团队「${teamName}」，请在团队页面点击“输入邀请码加入”，邀请码：${code}`
})

const settingsName = ref('')
const settingsDescription = ref('')
const settingsAvatarUrl = ref('')
const settingsIsPublic = ref(false)
const settingsInviteRole = ref<number>(3)
const settingsInviteExpireTime = ref<string>('')
const settingsInviteMaxUses = ref<number | null>(null)
const inviteCopied = ref(false)
const inviteLinkCopied = ref(false)
let inviteCopiedTimer: number | null = null
let inviteLinkCopiedTimer: number | null = null

watch(teamInfo, (v) => {
  if (!v) return
  settingsName.value = v.name || ''
  settingsDescription.value = v.description || ''
  settingsAvatarUrl.value = v.avatarUrl || ''
  settingsIsPublic.value = !!v.isPublic
  settingsInviteRole.value = typeof v.inviteRole === 'number' ? v.inviteRole : 3
  settingsInviteExpireTime.value = v.inviteExpireTime ? String(v.inviteExpireTime).slice(0, 16) : ''
  settingsInviteMaxUses.value = typeof v.inviteMaxUses === 'number' ? v.inviteMaxUses : null
}, { immediate: true })

const avatarUploading = ref(false)
const avatarInputRef = ref<HTMLInputElement | null>(null)

function openTeamAvatarPicker() {
  avatarInputRef.value?.click()
}

async function handleTeamAvatarUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    await modal.error('请上传图片文件')
    input.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    await modal.error('图片大小不能超过5MB')
    input.value = ''
    return
  }

  avatarUploading.value = true
  try {
    const r = await uploadFile('/api/common/file/upload', file, 'file')
    if (r && r.code === 0 && r.data?.fileUrl) {
      settingsAvatarUrl.value = r.data.fileUrl
      await modal.success('上传成功')
    } else {
      await modal.error(r?.message || '上传失败，请重试')
    }
  } catch (error) {
    await modal.error('上传失败，请重试')
  } finally {
    avatarUploading.value = false
    input.value = ''
  }
}

async function handleSaveTeamSettings() {
  if (!canManageTeam.value) {
    await modal.error('权限不足')
    return
  }

  try {
    const payload: any = {
      name: settingsName.value,
      description: settingsDescription.value,
      avatarUrl: settingsAvatarUrl.value,
      isPublic: settingsIsPublic.value,
      inviteRole: settingsInviteRole.value,
      inviteMaxUses: settingsInviteMaxUses.value
    }
    if (settingsInviteExpireTime.value) {
      payload.inviteExpireTime = settingsInviteExpireTime.value
    }
    const res = await updateTeam(props.teamId, payload)
    if (res.code === 0) {
      teamInfo.value = res.data
      await modal.success('保存成功')
    } else {
      await modal.error(res.message || '保存失败')
    }
  } catch (e) {
    await modal.error('保存失败，请重试')
  }
}

async function handleResetInviteCode() {
  if (!canManageTeam.value) {
    await modal.error('权限不足')
    return
  }

  const confirmed = await modal.confirm({
    title: '重置邀请码',
    message: '重置后旧邀请码将失效，确定继续吗？',
    type: 'warning'
  })
  if (!confirmed) return

  try {
    const res = await resetInviteCode(props.teamId)
    if (res.code === 0) {
      inviteCopied.value = false
      inviteLinkCopied.value = false
      teamInfo.value = res.data
      await modal.success('邀请码已重置')
    } else {
      await modal.error(res.message || '重置失败')
    }
  } catch (e) {
    await modal.error('重置失败，请重试')
  }
}

async function handleLeaveTeam() {
  const confirmed = await modal.confirm({
    title: '退出团队',
    message: '确定要退出该团队吗？',
    type: 'warning'
  })
  if (!confirmed) return

  try {
    const res = await leaveTeam(props.teamId)
    if (res.code === 0) {
      await modal.success('已退出团队')
      emit('back')
    } else {
      await modal.error(res.message || '退出失败')
    }
  } catch (e) {
    await modal.error('退出失败，请重试')
  }
}

async function handleDissolveTeam() {
  if (!isCreator.value) {
    await modal.error('仅创建者可解散团队')
    return
  }

  const confirmed = await modal.confirm({
    title: '解散团队',
    message: '确定要解散此团队吗？此操作不可恢复。',
    type: 'warning'
  })
  if (!confirmed) return

  try {
    const res = await dissolveTeam(props.teamId)
    if (res.code === 0) {
      await modal.success('团队已解散')
      emit('back')
    } else {
      await modal.error(res.message || '解散失败')
    }
  } catch (e) {
    await modal.error('解散失败，请重试')
  }
}

// 加载文档列表
async function loadDocs() {
  try {
    const res = await getTeamDocs(props.teamId, { page: 1, size: 50 })
    if (res.code === 0 && res.data) {
      const oldMap = new Map((notes.value || []).map(n => [n.id, n]))
      const list = (res.data.data || []).map(normalizeDoc)
      notes.value = list.map((n: any) => {
        const old = oldMap.get(n.id) || {}
        const merged: any = {
          ...old,
          ...n
        }
        if (merged.contentMd == null) merged.contentMd = (old as any).contentMd
        return merged
      })
      if (notes.value.length > 0 && !selectedNoteId.value) {
        await selectNote(notes.value[0].id)
      }
    }
  } catch (e) {
    console.error('加载文档列表失败', e)
  }
}

// 加载评论
async function loadComments() {
  if (!selectedNoteId.value) return
  const docId = selectedNoteId.value
  try {
    const res = await getDocComments(props.teamId, docId)
    if (selectedNoteId.value === docId && res.code === 0) comments.value = res.data || []
  } catch (e) {
    console.error('加载评论失败', e)
  }
}

// 加载版本历史
async function loadVersions() {
  if (!selectedNoteId.value) return
  const docId = selectedNoteId.value
  try {
    const res = await getDocVersions(props.teamId, docId)
    if (selectedNoteId.value === docId && res.code === 0) versions.value = res.data || []
  } catch (e) {
    console.error('加载版本失败', e)
  }
}

// 加载动态
async function loadActivities() {
  if (!selectedNoteId.value) return
  const docId = selectedNoteId.value
  try {
    const res = await getDocActivities(props.teamId, docId)
    if (selectedNoteId.value === docId && res.code === 0) activities.value = res.data || []
  } catch (e) {
    console.error('加载动态失败', e)
  }
}

// 加载成员
async function loadMembers() {
  try {
    const res = await getTeamMembers(props.teamId)
    if (res.code === 0) members.value = res.data || []
  } catch (e) {
    console.error('加载成员失败', e)
  }
}

// 添加评论
async function handleAddComment() {
  if (!newComment.value.trim() || !selectedNoteId.value || commentPending.value) return
  const docId = selectedNoteId.value
  commentPending.value = true
  try {
    const res = await addDocComment(props.teamId, docId, {
      content: newComment.value.trim(),
      parentId: replyTo.value?.id,
      type: 1
    })
    if (res.code === 0) {
      newComment.value = ''
      replyTo.value = null
      await loadComments()
    } else {
      await modal.error(res.message || '评论失败')
    }
  } catch (e) {
    await modal.error('评论失败，请重试')
  } finally {
    commentPending.value = false
  }
}

async function handleResolveComment(comment: CommentItem) {
  const id = String(comment.id)
  if (!selectedNoteId.value || !canEditDocs.value || resolvingComments.value.has(id)) return
  resolvingComments.value.add(id)
  try {
    unwrapTeamResponse(await resolveComment(props.teamId, selectedNoteId.value, id), '标记处理结果失败')
    await loadComments()
  } catch (error) {
    await modal.error(error instanceof Error ? error.message : '标记处理结果失败')
  } finally {
    resolvingComments.value.delete(id)
  }
}

// 恢复版本
async function handleRestoreVersion(eventId: string) {
  const confirmed = await modal.confirm({
    title: '恢复版本',
    message: '确定要恢复到此版本吗？当前内容将被覆盖。',
    type: 'warning'
  })
  if (!confirmed || !selectedNoteId.value) return
  
  try {
    const res = await restoreDocVersion(props.teamId, selectedNoteId.value, eventId)
    if (res.code === 0) {
      await selectNote(selectedNoteId.value)
      await loadVersions()
      await modal.success('版本已恢复')
    } else {
      await modal.error(res.message || '恢复失败')
    }
  } catch (e) {
    await modal.error('恢复失败，请重试')
  }
}

onMounted(async () => {
  await loadTeamInfo()
  await loadDocs()
  
  pollTimer = window.setInterval(() => {
    if (currentView.value === 'notes' && selectedNoteId.value && !isEditing.value) {
      loadDocs()
    }
  }, 10000)

  // 监听页面卸载事件，释放编辑锁
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
  if (inviteCopiedTimer) {
    window.clearTimeout(inviteCopiedTimer)
    inviteCopiedTimer = null
  }
  if (inviteLinkCopiedTimer) {
    window.clearTimeout(inviteLinkCopiedTimer)
    inviteLinkCopiedTimer = null
  }
  window.removeEventListener('beforeunload', handleBeforeUnload)
  // 组件卸载时释放锁
  if (isEditing.value && currentLockToken.value && selectedNote.value) {
    releaseLockSync()
  }
})

// 页面刷新或关闭前释放锁
function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (isEditing.value && currentLockToken.value && selectedNote.value) {
    releaseLockSync()
    event.preventDefault()
    event.returnValue = ''
  }
}

// 同步释放锁（使用 sendBeacon 确保请求能够发送）
function releaseLockSync() {
  if (!selectedNote.value || !currentLockToken.value) return
  
  try {
    fetch(
      `/api/teams/${props.teamId}/docs/${selectedNote.value.id}/lock?lockToken=${encodeURIComponent(currentLockToken.value)}`,
      { method: 'DELETE', keepalive: true, headers: { Authorization: `Bearer ${localStorage.getItem('satoken') || ''}` } }
    )
  } catch (e) {
    console.error('释放锁失败', e)
  }
}

watch(currentView, (v) => {
  if (v === 'members') loadMembers()
  if (v === 'ai') loadSessions()
})

watch(rightPanelTab, (t) => {
  if (t === 'activity') loadActivities()
})

async function selectNote(noteId: string) {
  if (isEditing.value) {
    await modal.warning('请先保存或取消当前文档的修改')
    return
  }
  const id = String(noteId)
  const request = ++docRequest
  selectedNoteId.value = id
  docLoading.value = true
  docError.value = ''
  comments.value = []
  versions.value = []
  activities.value = []
  replyTo.value = null
  newComment.value = ''
  try {
    const doc = normalizeDoc(unwrapTeamResponse(await getDocDetail(props.teamId, id), '文档加载失败'))
    if (request !== docRequest) return
    editorContent.value = doc.contentMd || ''
    const idx = notes.value.findIndex(n => n.id === id)
    if (idx >= 0) notes.value[idx] = { ...notes.value[idx], ...doc }
    else notes.value.push(doc)
    await Promise.allSettled([loadComments(), loadVersions(), loadActivities()])
  } catch (e) {
    if (request === docRequest) docError.value = e instanceof Error ? e.message : '文档加载失败'
  } finally {
    if (request === docRequest) docLoading.value = false
  }
}

async function handleCreateDoc() {
  const title = await modal.prompt({
    title: '新建文档',
    message: '请输入文档标题',
    placeholder: '文档标题'
  })
  if (!title || !title.trim()) return

  try {
    const res = await createDoc(props.teamId, { title: title.trim(), contentMd: '# ' + title.trim() + '\n\n' })
    if (res.code === 0 && res.data) {
      notes.value = [normalizeDoc(res.data), ...notes.value]
      await selectNote(String(res.data.id))
      await modal.success('文档创建成功')
    } else {
      await modal.error(res.message || '创建失败')
    }
  } catch (e) {
    await modal.error('创建失败，请重试')
  }
}

async function handleDeleteDoc(note: Note) {
  if (deletingDocId.value) return
  if (note.lockedBy) {
    await modal.error('文档正在被编辑，无法删除')
    return
  }
  if (isEditing.value && selectedNoteId.value === note.id) {
    await modal.error('请先保存并退出编辑后再删除')
    return
  }

  const confirmed = await modal.confirm({
    title: '删除文档',
    message: `确定要删除《${note.title}》吗？此操作不可恢复。`,
    type: 'warning',
    confirmText: '删除'
  })
  if (!confirmed) return

  deletingDocId.value = note.id
  try {
    const res = await deleteDoc(props.teamId, note.id)
    if (res.code === 0) {
      const idx = notes.value.findIndex(n => n.id === note.id)
      const nextNotes = notes.value.filter(n => n.id !== note.id)
      notes.value = nextNotes

      if (selectedNoteId.value === note.id) {
        const next = nextNotes[idx] || nextNotes[idx - 1] || null
        selectedNoteId.value = null
        editorContent.value = ''
        if (next) {
          await selectNote(next.id)
        }
      }

      await modal.success('删除成功')
    } else {
      await modal.error(res.message || '删除失败')
    }
  } catch (e) {
    await modal.error('删除失败，请重试')
  } finally {
    deletingDocId.value = null
  }
}

async function handleTakeLock() {
  if (!selectedNote.value) return

  try {
    const res = await acquireLock(props.teamId, selectedNote.value.id)
    if (res.code === 0 && res.data) {
      if (res.data.success) {
        currentLockToken.value = res.data.lockToken || ''
        isEditing.value = true
        // 更新本地文档的锁状态
        const idx = notes.value.findIndex(n => n.id === selectedNote.value?.id)
        if (idx >= 0) {
          notes.value[idx].lockedBy = selectedNote.value.updatedByUser?.id
          notes.value[idx].lockToken = res.data.lockToken
          notes.value[idx].lockExpireTime = res.data.lockExpireTime
        }
        await modal.success('已获取编辑锁，开始编辑')
      } else {
        const lockedBy = res.data.lockedByUser?.username || '其他用户'
        await modal.error(res.data.message || `文档正在被 ${lockedBy} 编辑`)
      }
    } else {
      await modal.error(res.message || '获取锁失败')
    }
  } catch (e) {
    await modal.error('获取锁失败，请重试')
  }
}

async function handleReleaseLock() {
  if (!selectedNote.value) return

  try {
    // 先保存文档
    const updateRes = await updateDoc(props.teamId, selectedNote.value.id, {
      title: selectedNote.value.title,
      contentMd: editorContent.value
    })
    
    if (updateRes.code !== 0) {
      await modal.error(updateRes.message || '保存失败')
      return
    }

    const idx = notes.value.findIndex(n => n.id === selectedNote.value?.id)
    if (idx >= 0) {
      notes.value[idx] = { ...notes.value[idx], contentMd: editorContent.value }
    }

    // 再释放锁
    await releaseLock(props.teamId, selectedNote.value.id, currentLockToken.value)
    currentLockToken.value = ''
    isEditing.value = false
    
    // 刷新文档列表
    await loadDocs()
    if (selectedNoteId.value) {
      await selectNote(selectedNoteId.value)
    }
    await modal.success('已保存并释放锁')
  } catch (e) {
    await modal.error('保存失败，请重试')
  }
}

function handleCancelEdit() {
  if (!selectedNote.value) return
  editorContent.value = selectedNote.value.contentMd || ''
  isEditing.value = false
  // 释放锁但不保存
  releaseLock(props.teamId, selectedNote.value.id, currentLockToken.value)
  currentLockToken.value = ''
}

function tabBtnClass(tab: typeof rightPanelTab.value) {
  return [
    'h-8 text-xs rounded-md px-3 flex items-center justify-center',
    rightPanelTab.value === tab ? 'bg-white shadow-sm ring-1 ring-gray-200 text-gray-900' : 'text-gray-500 hover:bg-gray-100'
  ]
}

const sessions = ref<Session[]>([])
const currentSessionId = ref('')
const messages = ref<{ role: 'user' | 'ai'; content: string; citations?: any[] }[]>([])
const aiInput = ref('')
const isSearching = ref(false)
const abortController = ref<AbortController | null>(null)

const TEAM_AI_WELCOME_MESSAGE = '你好！我是团队知识库助手，可以帮你查询团队文档中的内容。请问有什么需要帮助的？'

marked.setOptions({
  breaks: true,
  gfm: true
})

function renderMarkdown(content: string) {
  const raw = marked.parse(content || '') as unknown as string
  return DOMPurify.sanitize(raw)
}

async function handleOpenCitation(cite: any) {
  const docId = cite?.docId ?? cite?.id
  if (!docId) return

  if (isEditing.value) {
    await modal.warning('请先保存或取消当前文档的修改')
    return
  }
  currentView.value = 'notes'
  await selectNote(String(docId))
  await loadDocs()
}

function handleStopAI() {
  try {
    abortController.value?.abort()
  } catch {
  }
}

async function loadSessions() {
  try {
    const res = await getAISessions(props.teamId)
    if (res.code === 0) {
      sessions.value = res.data || []
      if (sessions.value.length > 0 && !currentSessionId.value) {
        handleSelectSession(sessions.value[0].id)
      }
    }
  } catch (e) {
    console.error('加载会话失败', e)
  }
}

async function handleDeleteSession(sessionId: string) {
  const confirmed = await modal.confirm({
    title: '删除会话',
    message: '确定要删除该历史对话吗？',
    type: 'warning'
  })
  if (!confirmed) return

  try {
    const res = await deleteAISession(props.teamId, sessionId)
    if (res.code === 0) {
      await loadSessions()
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = ''
        messages.value = []
        if (sessions.value.length > 0) {
          handleSelectSession(sessions.value[0].id)
        }
      }
    } else {
      await modal.error(res.message || '删除失败')
    }
  } catch (e) {
    await modal.error('删除失败，请重试')
  }
}

async function handleCreateSession() {
  try {
    const res = await createAISession(props.teamId, '新会话')
    if (res.code === 0 && res.data) {
      sessions.value = [res.data, ...sessions.value]
      handleSelectSession(res.data.id)
    }
  } catch (e) {
    console.error('创建会话失败', e)
  }
}

async function handleSelectSession(id: string) {
  if (id === currentSessionId.value) return
  currentSessionId.value = id
  try {
    const res = await getAIMessages(props.teamId, id)
    if (res.code === 0) {
      messages.value = (res.data || [])
        .filter((m: any) => !(m.role === 2 && m.content === TEAM_AI_WELCOME_MESSAGE))
        .map((m: any) => ({
          role: m.role === 1 ? 'user' : 'ai',
          content: m.content,
          citations: m.citations
        }))
    }
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

async function handleSendAI() {
  if (!aiInput.value.trim() || isSearching.value) return

  if (!currentSessionId.value) {
    await handleCreateSession()
    if (!currentSessionId.value) return
  }

  const q = aiInput.value
  messages.value = [...messages.value, { role: 'user', content: q }]
  aiInput.value = ''
  isSearching.value = true

  messages.value = [...messages.value, { role: 'ai', content: '' }]
  let currentContent = ''
  abortController.value = new AbortController()

  try {
    await sendAIMessageStream(props.teamId, currentSessionId.value, q, (chunk: string) => {
      currentContent += chunk.replace(/\[NL\]/g, '\n')
      messages.value[messages.value.length - 1].content = currentContent
    }, abortController.value.signal)

    isSearching.value = false
    abortController.value = null

    const refreshed = await getAIMessages(props.teamId, currentSessionId.value)
    if (refreshed.code === 0) {
      messages.value = (refreshed.data || [])
        .filter((m: any) => !(m.role === 2 && m.content === TEAM_AI_WELCOME_MESSAGE))
        .map((m: any) => ({
          role: m.role === 1 ? 'user' : 'ai',
          content: m.content,
          citations: m.citations
        }))
    }

    await loadSessions()
  } catch (e) {
    if ((e as any)?.name === 'AbortError' || abortController.value?.signal?.aborted) {
      isSearching.value = false
      abortController.value = null
      return
    }
    if ((e as any)?.message && String((e as any).message).includes('404')) {
      try {
        const res = await sendAIMessage(props.teamId, currentSessionId.value, q)
        isSearching.value = false
        abortController.value = null

        if (messages.value.length > 0 && messages.value[messages.value.length - 1].role === 'ai') {
          if (res.code === 0 && res.data) {
            messages.value[messages.value.length - 1].content = res.data.content || ''
            messages.value[messages.value.length - 1].citations = res.data.citations
          } else {
            messages.value[messages.value.length - 1].content = res.message || '抱歉，处理请求时出错了。'
          }
        }

        await loadSessions()
        return
      } catch (e2) {
      }
    }
    isSearching.value = false
    abortController.value = null
    if (messages.value.length > 0 && messages.value[messages.value.length - 1].role === 'ai') {
      messages.value[messages.value.length - 1].content = '抱歉，网络错误，请重试。'
    } else {
      messages.value = [...messages.value, {
        role: 'ai',
        content: '抱歉，网络错误，请重试。'
      }]
    }
  }
}

// 格式化时间
function formatTime(time: string) {
  if (!time) return ''
  try {
    const d = new Date(time)
    const now = new Date()
    const diff = now.getTime() - d.getTime()
    const minutes = Math.floor(diff / 60000)
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    const hours = Math.floor(minutes / 60)
    if (hours < 24) return `${hours}小时前`
    const days = Math.floor(hours / 24)
    return `${days}天前`
  } catch {
    return ''
  }
}

function roleText(role: number) {
  if (role === 0) return '创建者'
  if (role === 1) return '管理员'
  if (role === 2) return '编辑者'
  return '查看者'
}

async function handleRemoveMember(userId: number) {
  const confirmed = await modal.confirm({
    title: '移除成员',
    message: '确定要移除此成员吗？',
    type: 'warning'
  })
  if (!confirmed) return
  
  try {
    const res = await removeMember(props.teamId, userId)
    if (res.code === 0) {
      members.value = members.value.filter(m => m.userId !== userId)
      await modal.success('成员已移除')
    } else {
      await modal.error(res.message || '移除失败')
    }
  } catch (e) {
    await modal.error('移除失败，请重试')
  }
}

async function handleRoleChange(userId: number, newRole: number) {
  try {
    const res = await updateMemberRole(props.teamId, userId, newRole)
    if (res.code === 0) {
      const idx = members.value.findIndex(m => m.userId === userId)
      if (idx >= 0) members.value[idx].role = newRole
    } else {
      await modal.error(res.message || '修改角色失败')
    }
  } catch (e) {
    await modal.error('修改角色失败')
  }
}

async function handleCopyInviteCode() {
  if (!teamInfo.value?.inviteCode) return
  try {
    await navigator.clipboard.writeText(teamInfo.value.inviteCode)
    inviteCopied.value = true
    if (inviteCopiedTimer) window.clearTimeout(inviteCopiedTimer)
    inviteCopiedTimer = window.setTimeout(() => {
      inviteCopied.value = false
      inviteCopiedTimer = null
    }, 2000)
  } catch (e) {
    await modal.error('复制失败，请重试')
  }
}
 
async function handleCopyInviteLink() {
  if (!inviteShareText.value) return
  try {
    await navigator.clipboard.writeText(inviteShareText.value)
    inviteLinkCopied.value = true
    if (inviteLinkCopiedTimer) window.clearTimeout(inviteLinkCopiedTimer)
    inviteLinkCopiedTimer = window.setTimeout(() => {
      inviteLinkCopied.value = false
      inviteLinkCopiedTimer = null
    }, 2000)
  } catch (e) {
    await modal.error('复制失败，请重试')
  }
}
</script>

<template>
  <div class="h-screen flex flex-col text-gray-900">
    <div class="ws-top z-10">
      <div class="flex items-center gap-3 min-w-0">
        <Button variant="ghost" size="icon" class="text-gray-500 shrink-0" @click="emit('back')">
          <ArrowLeft class="w-5 h-5" />
        </Button>

        <div class="flex items-center gap-3 min-w-0">
          <Avatar class="w-8 h-8 rounded-lg border border-gray-200 shrink-0">
            <img
              v-if="teamInfo?.avatarUrl"
              :src="teamInfo.avatarUrl"
              class="w-full h-full object-cover rounded-lg"
            />
            <div v-else class="w-full h-full bg-blue-100 text-blue-700 flex items-center justify-center rounded-lg text-xs font-medium">
              {{ (teamInfo?.name || '团队').substring(0, 2) }}
            </div>
          </Avatar>
          <div class="min-w-0">
            <div class="font-semibold text-sm text-gray-900 leading-tight truncate">{{ teamInfo?.name || '加载中...' }}</div>
            <div class="text-[11px] text-gray-500 mt-0.5">{{ teamInfo?.memberCount || 0 }} 位成员</div>
          </div>
        </div>

        <div class="h-5 w-px bg-gray-200 mx-1 shrink-0" />

        <nav class="flex items-center gap-1">
          <button class="ws-tab" :class="{ 'is-on': currentView === 'notes' }" @click="currentView = 'notes'">
            <Layout class="w-4 h-4" />
            文档
          </button>
          <button class="ws-tab" :class="{ 'is-on': currentView === 'ai' }" @click="currentView = 'ai'">
            <Bot class="w-4 h-4" />
            AI 知识库
          </button>
          <button class="ws-tab" :class="{ 'is-on': currentView === 'members' }" @click="currentView = 'members'">
            <Users class="w-4 h-4" />
            成员
          </button>
        </nav>
      </div>

      <div class="flex items-center gap-2 shrink-0">
        <Button size="sm" class="gap-2" @click="handleCreateDoc">
          <Plus class="w-4 h-4" />
          新建文档
        </Button>
        <Button
          v-if="canManageTeam"
          variant="ghost"
          size="icon"
          class="text-gray-500"
          @click="openTeamSettings"
        >
          <Settings class="w-4 h-4" />
        </Button>
      </div>
    </div>

    <div class="flex-1 overflow-hidden relative">
      <div v-if="currentView === 'members'" class="h-full overflow-y-auto">
        <div class="max-w-5xl mx-auto px-10 py-8">
          <div class="flex items-start justify-between gap-6 mb-6">
            <div>
              <h2 class="text-[22px] font-semibold text-gray-900 tracking-[-0.018em]">成员管理</h2>
              <p class="text-[13px] text-gray-500 mt-1.5">管理团队成员及其权限角色</p>
            </div>
            <div class="flex gap-3 shrink-0">
              <Button variant="outline" :disabled="!canManageTeam" @click="handleResetInviteCode">重置邀请码</Button>
              <Button class="gap-2" :disabled="!canManageTeam" @click="openInviteModal">
                <Users class="w-4 h-4" />
                邀请成员
              </Button>
            </div>
          </div>

          <div class="wc-panel overflow-hidden">
            <table class="wc-table">
              <thead>
                <tr>
                  <th>成员</th>
                  <th class="w-[140px]">角色</th>
                  <th class="w-[130px]">加入时间</th>
                  <th class="w-[90px]">状态</th>
                  <th class="w-[90px] text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in members" :key="member.id">
                  <td>
                    <div class="flex items-center gap-3 min-w-0">
                      <Avatar class="w-8 h-8 shrink-0">
                        <img v-if="member.avatarUrl" :src="member.avatarUrl" class="w-full h-full object-cover rounded-full" />
                        <div v-else class="w-full h-full bg-blue-100 text-blue-700 flex items-center justify-center rounded-full text-xs">
                          {{ (member.username || '').substring(0, 2).toUpperCase() }}
                        </div>
                      </Avatar>
                      <div class="min-w-0">
                        <div class="font-medium text-gray-900 truncate">{{ member.username }}</div>
                        <div class="text-xs text-gray-500 truncate">{{ member.email }}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <select
                      class="bg-transparent border border-gray-200 rounded-md text-gray-700 text-xs py-1 px-2 focus:outline-none focus:border-gray-300"
                      :value="member.role"
                      @change="handleRoleChange(member.userId, Number(($event.target as HTMLSelectElement).value))"
                      :disabled="member.role === 0"
                    >
                      <option :value="0" v-if="member.role === 0">创建者</option>
                      <option :value="1">管理员</option>
                      <option :value="2">编辑者</option>
                      <option :value="3">查看者</option>
                    </select>
                  </td>
                  <td class="text-gray-500">{{ formatTime(member.joinTime) }}</td>
                  <td>
                    <Badge variant="secondary" class="bg-green-50 text-green-700 hover:bg-green-100 font-normal">正常</Badge>
                  </td>
                  <td class="text-right">
                    <Button
                      v-if="member.role !== 0"
                      variant="ghost"
                      size="sm"
                      class="text-red-600 hover:text-red-700 hover:bg-red-50"
                      @click="handleRemoveMember(member.userId)"
                    >移除</Button>
                  </td>
                </tr>
                <tr v-if="members.length === 0">
                  <td colspan="5" class="!py-0">
                    <div class="wc-empty">
                      <div class="wc-empty-icon"><Users /></div>
                      <div class="wc-empty-title">还没有其他成员</div>
                      <div class="wc-empty-desc">用邀请码把同事拉进来，就可以一起编辑团队文档了。</div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div v-else-if="currentView === 'ai'" class="h-full flex w-full">
        <div class="w-64 ws-side ws-side-left">
          <div class="p-3">
            <Button
              class="w-full justify-start gap-2 bg-white border border-gray-200 text-gray-700 hover:border-gray-300 hover:text-gray-900 shadow-sm transition-all"
              variant="outline"
              @click="handleCreateSession"
            >
              <Plus class="w-4 h-4" />
              新建会话
            </Button>
          </div>

          <div class="flex-1 overflow-y-auto px-2 pb-4">
            <div class="ws-eyebrow px-2 mb-2">历史对话</div>
            <div class="space-y-1">
              <div
                v-for="s in sessions"
                :key="s.id"
                class="ws-row group flex items-center gap-3 text-sm"
                :class="currentSessionId === s.id ? 'is-on text-gray-900' : 'text-gray-600'"
                @click="handleSelectSession(s.id)"
              >
                <MessageSquare
                  class="w-4 h-4 flex-shrink-0"
                  :class="
                    currentSessionId === s.id ? 'text-blue-600' : 'text-gray-400 group-hover:text-gray-500'
                  "
                />
                <div class="flex-1 min-w-0">
                  <div class="truncate font-medium">{{ s.title }}</div>
                  <div class="text-[10px] text-gray-400 mt-0.5">{{ formatTime(s.updateTime) }}</div>
                </div>

                <Button
                  variant="ghost"
                  size="icon"
                  class="h-7 w-7 opacity-0 group-hover:opacity-100 transition-opacity shrink-0"
                  @click.stop="handleDeleteSession(s.id)"
                >
                  <Trash2 class="w-4 h-4 text-gray-400 hover:text-red-600" />
                </Button>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 flex flex-col min-w-0">
          <div class="h-[52px] px-5 border-b border-gray-200 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-2">
              <Bot class="w-4 h-4 text-blue-600" />
              <span class="font-semibold text-sm text-gray-900">AI 知识库问答</span>
              <span class="text-[11px] text-gray-400 ml-1">基于团队文档回答</span>
            </div>
          </div>

          <div class="flex-1 overflow-y-auto px-6 py-6 space-y-6">
            <div
              v-for="(msg, i) in messages"
              :key="i"
              class="flex gap-3"
              :class="msg.role === 'user' ? 'flex-row-reverse' : ''"
            >
              <Avatar class="w-8 h-8 flex-shrink-0">
                <div v-if="msg.role === 'ai'" class="w-full h-full bg-blue-100 flex items-center justify-center text-blue-700">
                  <Bot class="w-4 h-4" />
                </div>
                <div v-else class="w-full h-full bg-blue-600 text-white flex items-center justify-center rounded-full text-[11px] font-medium">Me</div>
              </Avatar>

              <div class="space-y-2 max-w-[78%]" :class="msg.role === 'user' ? 'items-end flex flex-col' : ''">
                <div class="ws-bub" :class="msg.role === 'user' ? 'ws-bub-user' : 'ws-bub-ai'">
                  <div class="markdown-preview" v-html="renderMarkdown(msg.content)" />
                </div>

                <div v-if="msg.citations && msg.citations.length" class="flex flex-wrap gap-2 w-full">
                  <button
                    v-for="(cite, idx) in msg.citations"
                    :key="idx"
                    type="button"
                    class="inline-flex items-center gap-1.5 max-w-full px-2 py-1 rounded-full border border-gray-200 bg-white text-gray-700 hover:border-blue-300 hover:text-blue-700 transition-colors"
                    :title="cite.title"
                    @click="handleOpenCitation(cite)"
                  >
                    <FileText class="w-3 h-3 text-gray-400" />
                    <span class="text-xs font-medium truncate">{{ cite.title }}</span>
                  </button>
                </div>
              </div>
            </div>

          </div>

          <div class="px-6 py-4 border-t border-gray-200">
            <div class="relative max-w-4xl mx-auto w-full">
              <Input
                placeholder="向 AI 提问 (e.g. Q3的核心目标是什么？)..."
                class="pr-12 py-5 bg-gray-50 border-gray-200 focus:bg-white transition-all focus:ring-2 focus:ring-blue-100 focus:border-blue-300"
                v-model="aiInput"
                @keydown.enter.prevent="handleSendAI"
                :disabled="isSearching"
              />
              <Button
                size="icon"
                class="absolute right-2 top-1/2 -translate-y-1/2 h-8 w-8 bg-blue-600 hover:bg-blue-700"
                @click="isSearching ? handleStopAI() : handleSendAI()"
              >
                <X v-if="isSearching" class="w-4 h-4" />
                <Send v-else class="w-4 h-4" />
              </Button>
            </div>
            <p class="text-[11px] text-center text-gray-400 mt-2">答案由 AI 生成，请核对引用来源。</p>
          </div>
        </div>
      </div>

      <div v-else class="h-full flex">
        <div v-if="showDocListPanel" class="w-72 ws-side ws-side-left">
          <div class="p-3 space-y-3">
            <div class="flex items-center justify-between px-1">
              <span class="text-sm font-semibold text-gray-900">团队文档</span>
              <Button variant="ghost" size="icon" class="h-7 w-7" @click="showDocListPanel = false">
                <PanelLeftClose class="w-4 h-4 text-gray-500" />
              </Button>
            </div>

            <div class="relative">
              <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <Input
                placeholder="搜索文档..."
                class="pl-9 bg-white border-gray-200"
                v-model="searchQuery"
              />
            </div>

            <div class="flex items-center justify-between gap-2 px-1 pb-1 text-xs text-gray-500">
              <span>{{ filteredNotes.length }} 份资料</span>
              <label class="flex items-center gap-1.5">
                <span>排序</span>
                <select v-model="docSort" aria-label="团队文档排序" class="rounded border border-gray-200 bg-white px-2 py-1 text-xs text-gray-700">
                  <option value="recent">最近更新</option>
                  <option value="title">按标题</option>
                </select>
              </label>
            </div>
          </div>

          <div class="flex-1 overflow-y-auto px-2 pb-4 space-y-1">
            <div
              v-for="note in filteredNotes"
              :key="note.id"
              class="ws-row group"
              :class="selectedNoteId === note.id ? 'is-on' : ''"
              @click="selectNote(note.id)"
            >
              <div class="flex items-start justify-between mb-1">
                <div class="flex items-center gap-2 overflow-hidden">
                  <FileText
                    class="w-4 h-4 flex-shrink-0"
                    :class="selectedNoteId === note.id ? 'text-blue-600' : 'text-gray-400'"
                  />
                  <span
                    class="text-sm font-medium truncate"
                    :class="selectedNoteId === note.id ? 'text-gray-900' : 'text-gray-700'"
                  >
                    {{ note.title }}
                  </span>
                </div>
                <div class="flex items-center gap-1">
                  <Lock v-if="note.lockedBy" class="w-3 h-3 text-orange-500" />
                  <Button
                    variant="ghost"
                    size="icon"
                    class="h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity"
                    :disabled="!!note.lockedBy || (isEditing && selectedNoteId === note.id) || deletingDocId === note.id"
                    @click.stop="handleDeleteDoc(note)"
                  >
                    <Trash2 class="w-4 h-4 text-gray-400 hover:text-red-600" />
                  </Button>
                </div>
              </div>
              <p class="text-xs text-gray-500 line-clamp-2 ml-6 mb-2">{{ excerptText(note.excerpt || '') }}</p>
              <div class="ml-6 flex items-center gap-2 text-[10px] text-gray-400">
                <span>更新者：{{ note.updatedByUser?.username || '团队成员' }}</span>
                <span>{{ formatTime(note.updateTime) }}</span>
                <Badge
                  v-if="note.status === 0"
                  variant="outline"
                  class="text-[10px] h-4 px-1 py-0 text-gray-500"
                >
                  草稿
                </Badge>
              </div>
            </div>

            <div v-if="filteredNotes.length === 0" class="wc-empty !py-9">
              <div class="wc-empty-icon"><FileText /></div>
              <div class="wc-empty-title !text-[13px]">{{ searchQuery ? '没有匹配的文档' : '还没有文档' }}</div>
              <div class="wc-empty-desc !text-[12px]">
                {{ searchQuery ? '标题和摘要均未匹配，换个关键词试试' : '从项目目标、接口约定或会议纪要开始整理，团队成员可以评论并追踪修改历史。' }}
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1 flex flex-col min-w-0">
          <div class="h-[52px] flex items-center justify-between px-5 shrink-0">
            <div class="flex items-center gap-2 text-sm text-gray-500 overflow-hidden">
              <Button
                v-if="!showDocListPanel"
                variant="ghost"
                size="icon"
                class="h-7 w-7 flex-shrink-0"
                @click="showDocListPanel = true"
              >
                <PanelLeftOpen class="w-4 h-4 text-gray-500" />
              </Button>
              <span class="truncate">{{ teamInfo?.name || '项目空间' }}</span>
              <ChevronRight class="w-4 h-4 flex-shrink-0" />
              <span class="text-gray-900 font-medium truncate">{{ selectedNote?.title || '' }}</span>
            </div>

            <div class="flex items-center gap-3">
              <template v-if="selectedNote && selectedNote.lockedBy && !isEditing">
                <div class="flex items-center gap-2 bg-orange-50 text-orange-700 px-3 py-1.5 rounded-full text-xs">
                  <Lock class="w-3 h-3" />
                  <Avatar class="w-4 h-4">
                    <img v-if="selectedNote.lockedByUser?.avatarUrl" :src="selectedNote.lockedByUser.avatarUrl" class="w-full h-full object-cover rounded-full" />
                    <div v-else class="w-full h-full bg-orange-200 text-orange-800 flex items-center justify-center rounded-full text-[10px]">{{ (selectedNote.lockedByUser?.username || '?')[0] }}</div>
                  </Avatar>
                  <span class="font-medium">{{ selectedNote.lockedByUser?.username || '其他用户' }} 正在编辑</span>
                </div>
              </template>

              <template v-else-if="isEditing">
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-400">自动保存中...</span>
                  <Button size="sm" variant="outline" @click="handleCancelEdit">取消</Button>
                  <Button size="sm" class="gap-2" @click="handleReleaseLock">
                    <Save class="w-4 h-4" />
                    保存并退出
                  </Button>
                </div>
              </template>

              <template v-else>
                <div class="flex items-center gap-2">
                  <span v-if="selectedNote" class="text-xs text-gray-400">上次更新: {{ selectedNote.updateTime ? formatTime(selectedNote.updateTime) : '-' }}</span>
                  <Button size="sm" class="gap-2" :disabled="!selectedNote || docLoading || !!docError || !canEditDocs || !!selectedNote.lockedBy" @click="handleTakeLock">
                    <PenTool class="w-4 h-4" />
                    编辑
                  </Button>
                </div>
              </template>

              <div class="w-px h-4 bg-gray-200 mx-1" />
              <Button variant="ghost" size="icon" @click="showRightPanel = !showRightPanel">
                <Layout class="w-4 h-4" :class="showRightPanel ? 'text-blue-600' : 'text-gray-400'" />
              </Button>
            </div>
          </div>

          <div class="flex-1 overflow-hidden flex min-h-0">
            <div class="ws-paper">
              <div v-if="docLoading" class="wc-empty h-full">
                <div class="wc-empty-icon"><FileText /></div>
                <div class="wc-empty-title">正在加载团队文档</div>
                <div class="wc-empty-desc">正文、评论和版本记录会根据当前文档一起更新。</div>
              </div>

              <div v-else-if="docError" class="wc-empty h-full">
                <div class="wc-empty-icon"><FileText /></div>
                <div class="wc-empty-title">文档加载失败</div>
                <div class="wc-empty-desc">{{ docError }}</div>
                <Button variant="outline" class="mt-3" @click="selectedNoteId && selectNote(selectedNoteId)">重新加载</Button>
              </div>

              <div v-else-if="!selectedNote" class="wc-empty h-full">
                <div class="wc-empty-icon"><FileText /></div>
                <div class="wc-empty-title">团队资料工作区</div>
                <div class="wc-empty-desc">选择一份资料查看负责人和正文，再用右侧面板跟进评论、版本和协作动态。</div>
                <div class="mt-4 flex flex-wrap justify-center gap-2">
                  <Button v-if="filteredNotes[0]" variant="outline" @click="selectNote(filteredNotes[0].id)">打开最近资料</Button>
                  <Button v-if="canEditDocs" @click="handleCreateDoc"><Plus class="w-4 h-4" />新建团队文档</Button>
                  <Button variant="outline" class="gap-2" @click="currentView = 'ai'; loadSessions()"><Bot class="w-4 h-4" />向团队资料提问</Button>
                </div>
              </div>

              <template v-else>
                <div v-if="isEditing" class="flex-1 min-h-0">
                  <SimpleMarkdownEditor v-model="editorContent" placeholder="点击开始编辑，支持 Markdown 格式" />
                </div>

                <div v-else class="flex-1 min-h-0 overflow-y-auto">
                  <div class="max-w-5xl mx-auto py-8 px-8">
                    <div v-if="renderedDocument.headings.length" class="mb-6 rounded-lg border border-gray-100 bg-white px-4 py-3">
                      <div class="mb-2 text-xs font-semibold text-gray-500">文档目录</div>
                      <div class="flex flex-wrap gap-2">
                        <button v-for="heading in renderedDocument.headings" :key="heading.id" class="rounded border border-gray-100 px-2 py-1 text-xs text-gray-600 hover:border-blue-200 hover:text-blue-700" @click="jumpToSection(heading.id)">{{ heading.text }}</button>
                      </div>
                    </div>
                    <!-- 项目未安装 @tailwindcss/typography，prose-* 全是无效类；
                         正文改用站内已有的 .markdown-preview 排版 -->
                    <article ref="articleRef" class="markdown-preview text-[15px] text-gray-800">
                      <div v-html="renderedDocument.html" />
                    </article>
                  </div>
                </div>
              </template>
            </div>

            <div v-if="showRightPanel" class="w-80 ws-side ws-side-right">
              <div class="px-3 pt-3">
                <div class="flex items-center gap-2">
                  <div class="flex-1 grid grid-cols-3 bg-[var(--wc-bg-warm)] rounded-lg p-1">
                    <button :class="tabBtnClass('comments')" @click="rightPanelTab = 'comments'">评论</button>
                    <button :class="tabBtnClass('history')" @click="rightPanelTab = 'history'">历史</button>
                    <button :class="tabBtnClass('activity')" @click="rightPanelTab = 'activity'">动态</button>
                  </div>
                  <Button variant="ghost" size="icon" class="h-8 w-8 flex-shrink-0" @click="showRightPanel = false">
                    <PanelLeftOpen class="w-4 h-4 text-gray-500 rotate-180" />
                  </Button>
                </div>
              </div>

              <div v-if="rightPanelTab === 'comments'" class="flex-1 flex flex-col min-h-0">
                <div class="p-4 flex items-center justify-between border-b border-gray-200">
                  <span class="text-xs font-medium text-gray-500">{{ comments.length }} 条讨论 · {{ openCommentCount }} 项待跟进</span>
                  <Button variant="outline" size="sm" class="h-7 px-2 text-xs" :aria-pressed="onlyUnresolved" @click="onlyUnresolved = !onlyUnresolved">
                    {{ onlyUnresolved ? '显示全部' : '仅看待跟进' }}
                  </Button>
                </div>

                <div class="flex-1 overflow-y-auto p-4">
                  <div class="space-y-5">
                    <div v-for="c in visibleComments" :key="c.id" class="group" :class="c.parentId ? 'ml-5 border-l-2 border-gray-100 pl-3' : ''">
                      <div class="flex items-center gap-2 mb-2">
                        <Avatar class="w-6 h-6">
                          <img v-if="c.user?.avatarUrl" :src="c.user.avatarUrl" class="w-full h-full object-cover rounded-full" />
                          <div v-else class="w-full h-full bg-blue-100 text-blue-600 flex items-center justify-center rounded-full text-[10px]">
                            {{ (c.user?.username || '?').substring(0, 2).toUpperCase() }}
                          </div>
                        </Avatar>
                        <span class="text-xs font-medium text-gray-900">{{ c.user?.username || '未知' }}</span>
                        <span class="text-[10px] text-gray-400 ml-auto">{{ formatTime(c.createTime) }}</span>
                      </div>

                      <div
                        v-if="c.type === 2 && c.quote"
                        class="mb-2 pl-2 border-l-2 border-blue-200 bg-blue-50 p-1.5 rounded text-xs text-gray-600"
                      >
                        "{{ c.quote }}"
                      </div>

                      <p class="text-sm text-gray-800 leading-relaxed bg-white p-2 rounded-lg border border-gray-100 shadow-sm">
                        {{ c.content }}
                      </p>

                      <div class="flex items-center gap-3 mt-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button v-if="c.type === 1 && !c.parentId" class="text-[10px] text-gray-400 hover:text-blue-600" @click="replyTo = c">回复</button>
                        <button v-if="!c.resolved && canEditDocs" class="text-[10px] text-gray-400 hover:text-green-600 disabled:opacity-50" :disabled="resolvingComments.has(String(c.id))" @click="handleResolveComment(c)">标记已处理</button>
                        <span v-if="c.resolved" class="text-[10px] text-green-600">已处理</span>
                      </div>
                    </div>
                    <div v-if="visibleComments.length === 0" class="wc-empty !py-8">
                      <div class="wc-empty-title !text-[13px]">{{ comments.length ? '没有待跟进评论' : '还没有讨论' }}</div>
                      <div class="wc-empty-desc !text-[12px]">{{ comments.length ? '切换筛选即可查看已处理记录。' : '针对文档内容提问或留下验收结论，团队可以在此回复并跟进。' }}</div>
                    </div>
                  </div>
                </div>

                <div class="p-4 bg-white border-t border-gray-200">
                  <div v-if="replyTo" class="mb-2 flex items-center justify-between rounded bg-blue-50 px-2 py-1.5 text-xs text-blue-800">
                    <span class="truncate">回复 {{ replyTo.user?.username || '团队成员' }}：{{ replyTo.content }}</span>
                    <button class="ml-2 shrink-0" aria-label="取消回复" @click="replyTo = null"><X class="h-3.5 w-3.5" /></button>
                  </div>
                  <Input 
                    :placeholder="replyTo ? '回复这条讨论 (Enter 发送)...' : '补充问题、验收结果或决策 (Enter 发送)...'"
                    class="text-sm" 
                    v-model="newComment"
                    @keydown.enter.prevent="handleAddComment"
                    :disabled="commentPending || !selectedNote"
                  />
                </div>
              </div>

              <div v-else-if="rightPanelTab === 'history'" class="flex-1 overflow-y-auto p-4">
                <div class="relative pl-4 border-l border-gray-200 space-y-8">
                  <div v-for="(v, idx) in versions" :key="v.id || idx" class="relative">
                    <div
                      class="absolute -left-[21px] top-0 w-2.5 h-2.5 rounded-full border-2 border-white"
                      :class="idx === 0 ? 'bg-blue-600' : 'bg-gray-300'"
                    />
                    <div class="mb-1 flex items-center justify-between">
                      <span class="text-sm font-medium text-gray-900">V{{ v.version || (versions.length - idx) }}</span>
                      <Badge variant="outline" class="text-[10px] font-normal">{{ idx === 0 ? '当前版本' : formatTime(v.createTime) }}</Badge>
                    </div>
                    <div class="text-xs text-gray-500 mb-2">由 <span class="font-medium text-gray-700">{{ v.user?.username || '未知' }}</span> 更新</div>
                    <div class="text-xs text-gray-400">{{ v.message || '更新了文档内容' }}</div>
                    <Button v-if="idx !== 0" variant="outline" size="sm" class="mt-2 h-7 text-xs w-full" @click="handleRestoreVersion(v.id)">
                      <GitBranch class="w-3 h-3 mr-1" />
                      恢复此版本
                    </Button>
                  </div>
                  <div v-if="versions.length === 0" class="wc-empty !py-8"><div class="wc-empty-title !text-[13px]">暂无历史版本</div><div class="wc-empty-desc !text-[12px]">编辑并保存文档后，每次变更都会保留快照，可从此处恢复。</div></div>
                </div>
              </div>

              <div v-else class="flex-1 overflow-y-auto p-4">
                <div class="space-y-4">
                  <div v-for="a in activities" :key="a.id" class="flex gap-3 text-xs">
                    <Avatar class="w-6 h-6 flex-shrink-0">
                      <img v-if="a.user?.avatarUrl" :src="a.user.avatarUrl" class="w-full h-full object-cover rounded-full" />
                      <div v-else class="w-full h-full bg-blue-100 text-blue-600 flex items-center justify-center rounded-full text-[10px]">
                        {{ (a.user?.username || '?').substring(0, 2).toUpperCase() }}
                      </div>
                    </Avatar>
                    <div class="min-w-0">
                      <div class="text-gray-800 leading-relaxed">
                        <span class="font-medium text-gray-900">{{ a.user?.username || '未知' }}</span>
                        <span class="text-gray-500 mx-1">{{ a.message || '更新了文档' }}</span>
                      </div>
                      <div class="text-gray-400 mt-0.5">{{ formatTime(a.createTime) }}</div>
                    </div>
                  </div>
                  <div v-if="activities.length === 0" class="wc-empty !py-8"><div class="wc-empty-title !text-[13px]">暂无协作动态</div><div class="wc-empty-desc !text-[12px]">创建文档、发表讨论和更新版本后，相关操作会按时间记录。</div></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition
        enter-active-class="transition-opacity duration-200"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition-opacity duration-200"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showSettingsModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/40" @click="closeTeamSettings" />
          <div
            class="relative w-full max-w-3xl overflow-hidden rounded-2xl bg-white shadow-2xl"
            @click.stop
          >
            <button
              class="absolute right-4 top-4 rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
              @click="closeTeamSettings"
            >
              <X class="h-5 w-5" />
            </button>

            <div class="max-h-[80vh] overflow-auto p-7 space-y-4 bg-[var(--wc-bg)]">
              <div>
                <h2 class="text-2xl font-bold text-gray-900">团队设置</h2>
                <p class="text-gray-500 mt-1">管理团队信息、邀请设置与权限操作</p>
              </div>

              <div class="wc-panel p-6 space-y-5">
                <div class="flex items-center justify-between">
                  <div>
                    <div class="font-semibold text-gray-900">基础信息</div>
                    <div class="text-xs text-gray-500 mt-1">团队名称、简介、头像、公开性</div>
                  </div>
                  <Button size="sm" class="gap-2" :disabled="!canManageTeam" @click="handleSaveTeamSettings">
                    <Save class="w-4 h-4" />
                    保存
                  </Button>
                </div>

                <div class="space-y-2">
                  <div class="text-sm text-gray-700">团队名称</div>
                  <Input v-model="settingsName" placeholder="请输入团队名称" :disabled="!canManageTeam" />
                </div>

                <div class="space-y-2">
                  <div class="text-sm text-gray-700">团队简介</div>
                  <Textarea v-model="settingsDescription" placeholder="请输入团队简介" :disabled="!canManageTeam" :rows="4" />
                </div>

                <div class="space-y-2">
                  <div class="text-sm text-gray-700">团队头像</div>
                  <div class="flex items-center gap-4">
                    <Avatar class="w-14 h-14 rounded-lg border border-gray-200">
                      <img v-if="settingsAvatarUrl" :src="settingsAvatarUrl" class="w-full h-full object-cover rounded-lg" />
                      <div v-else class="w-full h-full bg-blue-100 text-blue-600 flex items-center justify-center rounded-lg text-sm font-medium">
                        {{ (settingsName || '团队').substring(0, 2) }}
                      </div>
                    </Avatar>

                    <div class="flex items-center gap-3">
                      <input
                        ref="avatarInputRef"
                        type="file"
                        accept="image/*"
                        class="hidden"
                        @change="handleTeamAvatarUpload"
                      />
                      <Button
                        variant="outline"
                        size="sm"
                        class="gap-2"
                        :disabled="!canManageTeam || avatarUploading"
                        @click="openTeamAvatarPicker"
                      >
                        <Upload class="w-4 h-4" />
                        {{ avatarUploading ? '上传中...' : '上传头像' }}
                      </Button>
                      <div class="text-xs text-gray-500">支持图片，最大 5MB</div>
                    </div>
                  </div>
                </div>

                <div class="flex items-center justify-between rounded-lg border border-gray-200 p-3">
                  <div>
                    <div class="text-sm font-medium text-gray-900">公开团队</div>
                    <div class="text-xs text-gray-500 mt-0.5">开启后其他人可发现该团队（不影响加入权限规则）</div>
                  </div>
                  <Switch v-model="settingsIsPublic" :disabled="!canManageTeam" />
                </div>
              </div>

              <div class="wc-panel p-6 space-y-5">
                <div class="flex items-center justify-between">
                  <div>
                    <div class="font-semibold text-gray-900">邀请设置</div>
                    <div class="text-xs text-gray-500 mt-1">邀请码与默认加入权限</div>
                  </div>
                  <Button variant="outline" size="sm" :disabled="!canManageTeam" @click="handleResetInviteCode">重置邀请码</Button>
                </div>

                <div class="space-y-2">
                  <div class="text-sm text-gray-700">邀请码</div>
                  <div class="text-sm text-gray-900 bg-gray-50 border border-gray-200 rounded-md px-3 py-2">
                    {{ teamInfo?.inviteCode || '无权限查看或未生成' }}
                  </div>
                  <div class="text-[11px] text-gray-400">非管理员会被隐藏邀请码</div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div class="space-y-2">
                    <div class="text-sm text-gray-700">默认邀请角色</div>
                    <select
                      class="w-full bg-white border border-gray-300 rounded-md px-3 py-2 text-sm"
                      v-model.number="settingsInviteRole"
                      :disabled="!canManageTeam"
                    >
                      <option :value="2">编辑者</option>
                      <option :value="3">查看者</option>
                    </select>
                  </div>
                  <div class="space-y-2">
                    <div class="text-sm text-gray-700">最大使用次数</div>
                    <Input v-model.number="settingsInviteMaxUses" type="number" :disabled="!canManageTeam" placeholder="例如 100" />
                  </div>
                </div>

                <div class="space-y-2">
                  <div class="text-sm text-gray-700">过期时间（可选）</div>
                  <Input v-model="settingsInviteExpireTime" type="datetime-local" :disabled="!canManageTeam" />
                </div>

                <div class="flex justify-end">
                  <Button size="sm" class="gap-2" :disabled="!canManageTeam" @click="handleSaveTeamSettings">
                    <Save class="w-4 h-4" />
                    保存邀请设置
                  </Button>
                </div>
              </div>

              <div class="wc-panel p-6 space-y-4">
                <div>
                  <div class="font-semibold text-gray-900">成员与权限</div>
                  <div class="text-xs text-gray-500 mt-1">管理成员、角色与权限</div>
                </div>
                <div class="flex gap-3">
                  <Button
                    variant="outline"
                    @click="currentView = 'members'; closeTeamSettings()"
                  >
                    进入成员管理
                  </Button>
                </div>
              </div>

              <div class="wc-panel p-6 space-y-4 !border-red-200">
                <div>
                  <div class="font-semibold text-red-700">危险操作</div>
                  <div class="text-xs text-red-500 mt-1">以下操作不可恢复，请谨慎</div>
                </div>
                <div class="flex gap-3">
                  <Button variant="outline" class="border-red-200 text-red-700 hover:bg-red-50" @click="handleLeaveTeam">退出团队</Button>
                  <Button v-if="isCreator" class="bg-red-600 hover:bg-red-700" @click="handleDissolveTeam">解散团队</Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
      <Transition
        enter-active-class="transition-opacity duration-200"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition-opacity duration-200"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showInviteModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/40" @click="closeInviteModal" />
          <div
            class="relative w-full max-w-xl overflow-hidden rounded-2xl bg-white shadow-2xl"
            @click.stop
          >
            <button
              class="absolute right-4 top-4 rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
              @click="closeInviteModal"
            >
              <X class="h-5 w-5" />
            </button>

            <div class="p-8 space-y-6">
              <div>
                <h2 class="text-2xl font-bold text-gray-900">邀请成员</h2>
                <p class="text-gray-500 mt-1">通过邀请码或邀请链接，将成员加入当前团队空间。</p>
              </div>

              <div class="rounded-xl border border-gray-200 bg-gray-50 p-5 space-y-4">
                <div>
                  <div class="text-sm text-gray-600 mb-2">邀请码</div>
                  <div class="flex gap-3">
                    <div class="flex-1 rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm font-medium text-gray-900 break-all">
                      {{ teamInfo?.inviteCode || '无权限查看或未生成' }}
                    </div>
                    <Button variant="outline" class="gap-2" :disabled="!teamInfo?.inviteCode" @click="handleCopyInviteCode">
                      <Copy class="w-4 h-4" />
                      {{ inviteCopied ? '已复制' : '复制邀请码' }}
                    </Button>
                  </div>
                </div>

                <div>
                  <div class="text-sm text-gray-600 mb-2">邀请文案</div>
                  <div class="flex gap-3">
                    <div class="flex-1 rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm text-gray-700 break-all">
                      {{ inviteShareText || '请先生成邀请码' }}
                    </div>
                    <Button variant="outline" class="gap-2" :disabled="!inviteShareText" @click="handleCopyInviteLink">
                      <Copy class="w-4 h-4" />
                      {{ inviteLinkCopied ? '已复制' : '复制文案' }}
                    </Button>
                  </div>
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div class="rounded-xl border border-gray-200 p-4 bg-white">
                  <div class="text-xs text-gray-500">默认加入角色</div>
                  <div class="mt-2 text-sm font-medium text-gray-900">{{ inviteRoleText }}</div>
                </div>
                <div class="rounded-xl border border-gray-200 p-4 bg-white">
                  <div class="text-xs text-gray-500">剩余可用次数</div>
                  <div class="mt-2 text-sm font-medium text-gray-900">{{ inviteRemainingUses }}</div>
                </div>
                <div class="rounded-xl border border-gray-200 p-4 bg-white">
                  <div class="text-xs text-gray-500">过期时间</div>
                  <div class="mt-2 text-sm font-medium text-gray-900">{{ teamInfo?.inviteExpireTime ? String(teamInfo.inviteExpireTime).replace('T', ' ').slice(0, 16) : '长期有效' }}</div>
                </div>
              </div>

              <div class="rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-800">
                被邀请成员可在团队列表页通过“输入邀请码加入”进入团队。若需变更默认角色、有效期或最大使用次数，可前往团队设置中的邀请设置调整。
              </div>

              <div class="flex items-center justify-between gap-3">
                <Button variant="outline" @click="handleResetInviteCode">重置邀请码</Button>
                <div class="flex gap-3">
                  <Button variant="outline" @click="closeInviteModal">关闭</Button>
                  <Button class="gap-2" :disabled="!teamInfo?.inviteCode" @click="handleCopyInviteCode">
                    <Copy class="w-4 h-4" />
                    复制邀请码
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
