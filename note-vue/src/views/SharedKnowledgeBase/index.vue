<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import Button from '@/components/ui/Button.vue'
import Avatar from '@/components/ui/Avatar.vue'
import Badge from '@/components/ui/Badge.vue'
import Select from '@/components/ui/Select.vue'
import Switch from '@/components/ui/Switch.vue'
import {
  Search, Plus, Users, FileText, Upload, Trash2, Eye, Download, X,
  Lock, Globe, ArrowLeft, Send, Square, Bot, Sparkles, LogOut, UserPlus, Share2, Copy, Check, Edit2, Pen
} from 'lucide-vue-next'
import { marked } from 'marked'
import FilePreviewModal from '@/components/common/FilePreviewModal.vue'
import type { SharedKnowledgeBase, KnowledgeBaseFile, KnowledgeBaseMember, ChatMessage, KbInvitation } from '@/types/note'
import { getUserInfo } from '@/api/auth'
import { uploadFile } from '@/api/apiClient'
import {
  getKnowledgeBaseSquare,
  getMyCreatedKnowledgeBases,
  getMyJoinedKnowledgeBases,
  getKnowledgeBaseDetail,
  createKnowledgeBase as apiCreateKnowledgeBase,
  updateKnowledgeBase as apiUpdateKnowledgeBase,
  deleteKnowledgeBase as apiDeleteKnowledgeBase,
  joinKnowledgeBase as apiJoinKnowledgeBase,
  leaveKnowledgeBase as apiLeaveKnowledgeBase,
  getKnowledgeBaseMembers,
  removeMember as apiRemoveMember,
  inviteMember as apiInviteMember,
  updateMemberRole as apiUpdateMemberRole,
  updateKnowledgeBasePublicStatus as apiUpdatePublicStatus,
  getKnowledgeBaseFiles,
  uploadKnowledgeBaseFile,
  uploadKnowledgeBaseFileWithProgress,
  deleteKnowledgeBaseFile,
  chatWithKnowledgeBaseStream,
  renameKnowledgeBaseFile,
  downloadKnowledgeBaseFile,
  getMyInvitations,
  getMyInvitationCount,
  respondInvitation
} from '@/api/sharedKnowledge'
import { useModal } from '@/composables/useModal'
import { useMembershipQuota } from '@/composables/useMembershipQuota'

const modal = useModal()
const { incrementKbUsed: incrementKbUsedLocal, decrementKbUsed: decrementKbUsedLocal, refreshQuota } = useMembershipQuota()

marked.setOptions({
  breaks: true,
  gfm: true
})

// 角色类型
type KbRole = 'admin' | 'editor' | 'viewer'

// 角色选项
const roleOptions = [
  { value: 'admin', label: '管理员' },
  { value: 'viewer', label: '成员' }
]

// 状态
const activeTab = ref<'square' | 'created' | 'joined' | 'detail'>('square')
const sourceTab = ref<'square' | 'created' | 'joined'>('square')
const searchKeyword = ref('')
const squareKnowledgeBases = ref<SharedKnowledgeBase[]>([])
const createdKnowledgeBases = ref<SharedKnowledgeBase[]>([])
const joinedKnowledgeBases = ref<SharedKnowledgeBase[]>([])
const currentKnowledgeBase = ref<SharedKnowledgeBase | null>(null)
const knowledgeBaseFiles = ref<KnowledgeBaseFile[]>([])
const fileSearchKeyword = ref('')
const members = ref<KnowledgeBaseMember[]>([])
const loading = ref(false)
const currentUserId = ref<number | null>(null)

// 邀请相关状态
const pendingInvitations = ref<KbInvitation[]>([])
const invitationCount = ref(0)
const invitationLoading = ref(false)
const invitationTimer = ref<number | null>(null)

// 分页（广场：后端分页；created/joined：前端分页）
const squareCurrentPage = ref(1)
const squarePageSize = ref(10)
const squareTotal = ref(0)

const squareTotalPages = computed(() => {
  const totalCount = squareTotal.value > 0 ? squareTotal.value : squareKnowledgeBases.value.length
  if (totalCount <= 0) return 0
  return Math.ceil(totalCount / squarePageSize.value)
})

const createdCurrentPage = ref(1)
const createdPageSize = ref(10)
const createdTotalPages = computed(() => {
  if (createdKnowledgeBases.value.length <= 0) return 0
  return Math.ceil(createdKnowledgeBases.value.length / createdPageSize.value)
})
const pagedCreatedKnowledgeBases = computed(() => {
  const start = (createdCurrentPage.value - 1) * createdPageSize.value
  return createdKnowledgeBases.value.slice(start, start + createdPageSize.value)
})

const joinedCurrentPage = ref(1)
const joinedPageSize = ref(10)
const joinedTotalPages = computed(() => {
  if (joinedKnowledgeBases.value.length <= 0) return 0
  return Math.ceil(joinedKnowledgeBases.value.length / joinedPageSize.value)
})
const pagedJoinedKnowledgeBases = computed(() => {
  const start = (joinedCurrentPage.value - 1) * joinedPageSize.value
  return joinedKnowledgeBases.value.slice(start, start + joinedPageSize.value)
})

// 对话框状态
const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const showUploadDialog = ref(false)
const showMembersDialog = ref(false)
const showPreviewModal = ref(false)
const showInviteDialog = ref(false)
const showShareDialog = ref(false)
const showRenameDialog = ref(false)
const previewFile = ref<import('@/types/api').PreviewFile | null>(null)
const renamingFile = ref<KnowledgeBaseFile | null>(null)
const newFileName = ref('')
const siteOrigin = ref('')
const uploadInputRef = ref<HTMLInputElement | null>(null)
const coverInputRef = ref<HTMLInputElement | null>(null)
const coverUploading = ref(false)

// 表单数据
const formData = ref({ id: '', name: '', description: '', coverUrl: '', isPublic: true, password: '' })

// 邀请表单
const inviteEmail = ref('')
const inviteRole = ref<KbRole>('viewer')
const joinPassword = ref('')
const joiningKb = ref<SharedKnowledgeBase | null>(null)
const uploadFileList = ref<File[]>([])
const uploadProgresses = ref<number[]>([])
const uploadStatuses = ref<('pending' | 'uploading' | 'processing' | 'done' | 'error')[]>([])
const isUploading = ref(false)
const MAX_FILE_SIZE = 200 * 1024 * 1024 // 200MB

// AI 助手
const showAiChat = ref(false)
const kbChatMessages = ref<ChatMessage[]>([])
const kbChatInput = ref('')
const isKbChatting = ref(false)
const kbChatAbortController = ref<AbortController | null>(null)
const chatContainerRef = ref<HTMLDivElement | null>(null)
const aiChatPosition = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

const quickQuestions = ['这个知识库主要包含什么内容？', '帮我总结一下最近上传的文档', '有关于React的资料吗？']

function normalizeKnowledgeBase(raw: Record<string, any>): SharedKnowledgeBase {
  const kb = { ...raw }
  if (kb.fileCount == null && raw.file_count != null) kb.fileCount = raw.file_count
  if (kb.memberCount == null && raw.member_count != null) kb.memberCount = raw.member_count
  if (kb.creatorId == null && raw.creator_id != null) kb.creatorId = raw.creator_id
  if (kb.creatorName == null && raw.creator_name != null) kb.creatorName = raw.creator_name
  if (kb.coverUrl == null && raw.cover_url != null) kb.coverUrl = raw.cover_url
  if (kb.isPublic == null && raw.is_public != null) kb.isPublic = raw.is_public
  if (kb.hasPassword == null && raw.has_password != null) kb.hasPassword = raw.has_password
  if (kb.createTime == null && raw.create_time != null) kb.createTime = raw.create_time
  return kb as SharedKnowledgeBase
}

function normalizeKnowledgeBaseList(list: any[]) {
  return (list || []).map(item => normalizeKnowledgeBase(item))
}

function resolveMemberRole(member?: KnowledgeBaseMember): KbRole | 'none' {
  if (!member) return 'none'
  
  // 如果role字段已经是字符串类型，直接返回
  if (typeof member.role === 'string') {
    return member.role as KbRole
  }
  
  // 从roleName或status解析角色（注意匹配顺序，先匹配更具体的）
  const roleText = `${member.roleName || ''} ${member.status || ''}`.trim()
  if (roleText) {
    if (roleText.includes('创建者')) return 'admin'
    if (roleText.includes('管理员')) return 'admin'
    // 注意：必须在匹配“管理员”之后再匹配“成员”，因为“管理员”也包含“成员”
    if (roleText.includes('成员') || roleText.includes('查看')) return 'viewer'
  }
  
  // 如果role是整数，映射为字符串（后端数据格式：1=创建者，2=管理员，3=成员）
  if (typeof member.role === 'number') {
    if (member.role === 1) return 'admin' // 创建者
    if (member.role === 2) return 'admin' // 管理员
    if (member.role === 3) return 'viewer' // 成员
    return 'viewer' // 默认为成员
  }
  
  return member.role || 'viewer'
}

// 计算当前用户在当前知识库中的角色
const currentUserRole = computed((): KbRole | 'none' => {
  if (!currentKnowledgeBase.value) return 'none'
  if (Number(currentKnowledgeBase.value.creatorId) === currentUserId.value) return 'admin'
  const member = members.value.find(m => Number(m.userId) === currentUserId.value)
  return resolveMemberRole(member)
})

const isCreator = computed(() => Number(currentKnowledgeBase.value?.creatorId) === currentUserId.value)
const isAdmin = computed(() => currentUserRole.value === 'admin')
const canSetMemberRole = computed(() => {
  if (!currentKnowledgeBase.value || currentUserId.value === null) return false
  if (Number(currentKnowledgeBase.value.creatorId) === currentUserId.value) return true
  const member = members.value.find(m => Number(m.userId) === currentUserId.value)
  return resolveMemberRole(member) === 'admin'
})

// 权限判断
const permissions = computed(() => ({
  canView: currentUserRole.value !== 'none' || currentKnowledgeBase.value?.isPublic,
  canEdit: currentUserRole.value === 'admin',
  canUpload: currentUserRole.value === 'admin',
  canDelete: currentUserRole.value === 'admin',
  canInvite: currentUserRole.value !== 'none',
  canManageMembers: isCreator.value || isAdmin.value,
  canShare: currentUserRole.value !== 'none', // 所有成员都可以分享
}))

const canManageMembers = computed(() => permissions.value.canManageMembers)
const canEditKnowledgeBase = computed(() => permissions.value.canEdit)

const shareLink = computed(() => {
  if (!siteOrigin.value || !currentKnowledgeBase.value) return ''
  return `${siteOrigin.value}/share/kb/${currentKnowledgeBase.value.id}`
})

// 加载广场知识库列表
async function loadSquareKnowledgeBases() {
  try {
    loading.value = true
    const res = await getKnowledgeBaseSquare({
      keyword: searchKeyword.value,
      page: squareCurrentPage.value,
      size: squarePageSize.value
    })
    if (res.code === 0 && res.data) {
      squareKnowledgeBases.value = normalizeKnowledgeBaseList(res.data.data || [])
      squareTotal.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载知识库广场失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载我创建的知识库
async function loadCreatedKnowledgeBases() {
  try {
    loading.value = true
    const res = await getMyCreatedKnowledgeBases()
    if (res.code === 0) {
      createdKnowledgeBases.value = normalizeKnowledgeBaseList(res.data || [])
      createdCurrentPage.value = 1
    }
  } catch (e) {
    console.error('加载我创建的知识库失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载我加入的知识库
async function loadJoinedKnowledgeBases() {
  try {
    loading.value = true
    const res = await getMyJoinedKnowledgeBases()
    if (res.code === 0) {
      joinedKnowledgeBases.value = normalizeKnowledgeBaseList(res.data || [])
      joinedCurrentPage.value = 1
    }
  } catch (e) {
    console.error('加载我加入的知识库失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载知识库详情
async function loadKnowledgeBaseDetail(kbId: number) {
  try {
    loading.value = true
    const res = await getKnowledgeBaseDetail(kbId)
    if (res.code === 0) {
      currentKnowledgeBase.value = normalizeKnowledgeBase(res.data || {})
      // 加载文件列表
      await loadKnowledgeBaseFiles(kbId)
      // 加载成员列表
      await loadKnowledgeBaseMembers(kbId)
    }
  } catch (e) {
    console.error('加载知识库详情失败:', e)
  } finally {
    loading.value = false
  }
}

// 加载知识库文件
async function loadKnowledgeBaseFiles(kbId: number) {
  try {
    const res = await getKnowledgeBaseFiles(kbId, {
      page: 1,
      size: 50,
      keyword: fileSearchKeyword.value
    })
    if (res.code === 0 && res.data) {
      knowledgeBaseFiles.value = res.data.data || []
      if (currentKnowledgeBase.value) {
        currentKnowledgeBase.value = {
          ...currentKnowledgeBase.value,
          fileCount: res.data.total ?? currentKnowledgeBase.value.fileCount
        }
      }
    }
  } catch (e) {
    console.error('加载文件列表失败:', e)
  }
}

// 加载知识库成员
async function loadKnowledgeBaseMembers(kbId: number) {
  try {
    const res = await getKnowledgeBaseMembers(kbId)
    if (res.code === 0) {
      members.value = res.data || []

      if (currentKnowledgeBase.value) {
        currentKnowledgeBase.value = {
          ...currentKnowledgeBase.value,
          memberCount: members.value.length
        }
      }
    }
  } catch (e) {
    console.error('加载成员列表失败:', e)
  }
}

// 搜索知识库
async function handleSearch() {
  squareCurrentPage.value = 1
  await loadSquareKnowledgeBases()
}

watch(squareCurrentPage, () => {
  if (activeTab.value === 'square') {
    loadSquareKnowledgeBases()
  }
})

// 方法
// 处理知识库卡片点击
function handleKnowledgeBaseClick(kb: SharedKnowledgeBase) {
  // 所有知识库都可以点击查看详情(浏览基本信息和文件列表)
  // 但文件操作(预览、下载等)需要加入后才能进行
  viewKnowledgeBase(kb)
}

async function viewKnowledgeBase(kb: SharedKnowledgeBase) {
  sourceTab.value = activeTab.value as 'square' | 'created' | 'joined'
  activeTab.value = 'detail'
  await loadKnowledgeBaseDetail(Number(kb.id))
}

function joinKnowledgeBase(kb: SharedKnowledgeBase) {
  joiningKb.value = kb
  if (kb.hasPassword) {
    // 有密码,弹出密码输入框
    showJoinDialog.value = true
  } else {
    // 无密码,弹出确认对话框
    modal.confirm({
      title: '加入知识库',
      message: `确定要加入知识库"${kb.name}"吗？`,
      confirmText: '加入',
      cancelText: '取消'
    }).then(confirmed => {
      if (confirmed) {
        confirmJoin()
      } else {
        joiningKb.value = null
      }
    })
  }
}

async function confirmJoin() {
  if (!joiningKb.value) return
  try {
    const res = await apiJoinKnowledgeBase({
      knowledgeBaseId: Number(joiningKb.value.id),
      password: joinPassword.value || undefined
    })
    if (res.code === 0) {
      showJoinDialog.value = false
      joinPassword.value = ''
      const joinedKb = joiningKb.value
      joiningKb.value = null
      await modal.success('加入成功！')
      
      // 自动进入知识库详情页
      await viewKnowledgeBase(joinedKb)
      
      // 刷新列表
      await loadSquareKnowledgeBases()
      await loadJoinedKnowledgeBases()
    } else {
      alert(res.message || '加入失败')
    }
  } catch (e) {
    console.error('加入知识库失败:', e)
    alert('加入失败')
  }
}

async function leaveKnowledgeBase(kb: SharedKnowledgeBase) {
  const confirmed = await modal.confirm({
    title: '退出知识库',
    message: '确定要退出该知识库吗？',
    confirmText: '退出',
    cancelText: '取消'
  })
  if (!confirmed) return
  try {
    const res = await apiLeaveKnowledgeBase(Number(kb.id))
    if (res.code === 0) {
      await modal.success('已退出')
      await loadJoinedKnowledgeBases()
      await loadSquareKnowledgeBases()
    } else {
      await modal.error(res.message || '退出失败')
    }
  } catch (e) {
    console.error('退出知识库失败:', e)
    await modal.error('退出失败')
  }
}

async function handleCreateKnowledgeBase() {
  try {
    if (formData.value.id) {
      // 编辑
      const res = await apiUpdateKnowledgeBase(Number(formData.value.id), {
        name: formData.value.name,
        description: formData.value.description,
        coverUrl: formData.value.coverUrl,
        isPublic: formData.value.isPublic,
        password: formData.value.password || undefined
      })
      if (res.code === 0) {
        showCreateDialog.value = false
        resetForm()
        await modal.success('保存成功！')
        await loadCreatedKnowledgeBases()
      } else {
        await modal.error(res.message || '保存失败')
      }
    } else {
      // 创建
      const res = await apiCreateKnowledgeBase({
        name: formData.value.name,
        description: formData.value.description,
        coverUrl: formData.value.coverUrl,
        isPublic: formData.value.isPublic,
        password: formData.value.password || undefined
      })
      if (res.code === 0) {
        showCreateDialog.value = false
        resetForm()
        await modal.success('创建成功！')
        incrementKbUsedLocal()
        if (currentUserId.value) {
          await refreshQuota(currentUserId.value)
        }
        await Promise.all([loadCreatedKnowledgeBases(), loadSquareKnowledgeBases()])
      } else {
        await modal.error(res.message || '创建失败')
      }
    }
  } catch (e) {
    console.error('操作失败:', e)
    await modal.error('操作失败')
  }
}

function editKnowledgeBase(kb: SharedKnowledgeBase) {
  formData.value = {
    id: String(kb.id),
    name: kb.name,
    description: kb.description,
    coverUrl: kb.coverUrl || '',
    isPublic: kb.isPublic,
    password: ''
  }
  showCreateDialog.value = true
}

async function deleteKnowledgeBase(kb: SharedKnowledgeBase) {
  const confirmed = await modal.confirm({
    title: '删除知识库',
    message: `确定要删除知识库“${kb.name}”吗？此操作不可恢复！`,
    confirmText: '删除',
    cancelText: '取消'
  })
  if (!confirmed) return
  try {
    const res = await apiDeleteKnowledgeBase(Number(kb.id))
    if (res.code === 0) {
      await modal.success('删除成功！')
      decrementKbUsedLocal()
      if (currentUserId.value) {
        await refreshQuota(currentUserId.value)
      }
      await loadCreatedKnowledgeBases()
    } else {
      await modal.error(res.message || '删除失败')
    }
  } catch (e) {
    console.error('删除知识库失败:', e)
    await modal.error('删除失败')
  }
}

function resetForm() {
  formData.value = { id: '', name: '', description: '', coverUrl: '', isPublic: true, password: '' }
}

async function handleCoverSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    alert('请上传图片文件')
    input.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过5MB')
    input.value = ''
    return
  }
  coverUploading.value = true
  try {
    const r = await uploadFile('/api/common/file/upload', file, 'file')
    if (r && r.code === 0 && r.data?.fileUrl) {
      formData.value.coverUrl = r.data.fileUrl
    } else {
        await modal.error(r?.message || '封面上传失败')
    }
  } catch (e) {
    console.error('上传封面失败:', e)
    await modal.error('封面上传失败')
  } finally {
    coverUploading.value = false
    input.value = ''
  }
}

function triggerCoverSelect() {
  coverInputRef.value?.click()
}

function clearCover() {
  formData.value.coverUrl = ''
}

async function goBackToList() {
  activeTab.value = sourceTab.value
  currentKnowledgeBase.value = null
  showAiChat.value = false
  kbChatMessages.value = []

  if (activeTab.value === 'square') {
    await loadSquareKnowledgeBases()
  }
  if (activeTab.value === 'created') {
    await loadCreatedKnowledgeBases()
  }
  if (activeTab.value === 'joined') {
    await loadJoinedKnowledgeBases()
  }
}

function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) {
    const files = Array.from(target.files)
    const oversized = files.filter(f => f.size > MAX_FILE_SIZE)
    if (oversized.length > 0) {
      const names = oversized.map(f => `${f.name}（${formatFileSize(f.size)}）`).join('、')
      modal.error(`以下文件超出 200MB 限制，已自动移除：\n${names}`)
      uploadFileList.value = files.filter(f => f.size <= MAX_FILE_SIZE)
    } else {
      uploadFileList.value = files
    }
    uploadProgresses.value = uploadFileList.value.map(() => 0)
    uploadStatuses.value = uploadFileList.value.map(() => 'pending')
  }
  target.value = ''
}

function formatDateTime(value: string) {
  if (!value) return ''
  const trimmed = value.replace(/\.([0-9]{3})[0-9]+$/, '.$1')
  const date = new Date(trimmed)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

 function triggerUploadFileSelect() {
  uploadInputRef.value?.click()
 }

async function handleUploadFiles() {
  if (uploadFileList.value.length === 0 || !currentKnowledgeBase.value) return
  isUploading.value = true
  uploadProgresses.value = uploadFileList.value.map(() => 0)
  uploadStatuses.value = uploadFileList.value.map(() => 'pending')
  let hasError = false
  let errorMsg = ''
  try {
    for (let i = 0; i < uploadFileList.value.length; i++) {
      const file = uploadFileList.value[i]
      uploadStatuses.value[i] = 'uploading'
      try {
        const res = await uploadKnowledgeBaseFileWithProgress(
          Number(currentKnowledgeBase.value.id),
          file,
          (percent) => { uploadProgresses.value[i] = percent },
          () => {
            uploadProgresses.value[i] = 100
            uploadStatuses.value[i] = 'processing'
          }
        )
        if (!res || res.code !== 0) {
          uploadStatuses.value[i] = 'error'
          hasError = true
          errorMsg = res?.message || '上传失败'
        } else {
          uploadProgresses.value[i] = 100
          uploadStatuses.value[i] = 'done'
        }
      } catch (e) {
        uploadStatuses.value[i] = 'error'
        hasError = true
        errorMsg = (e as Error).message || '上传失败'
      }
    }
    await loadKnowledgeBaseFiles(Number(currentKnowledgeBase.value.id))
    if (hasError) {
      await modal.error(errorMsg || '部分文件上传失败')
    } else {
      showUploadDialog.value = false
      uploadFileList.value = []
      uploadProgresses.value = []
      uploadStatuses.value = []
      await modal.success('上传成功！')
    }
  } finally {
    isUploading.value = false
  }
}

async function deleteFile(file: KnowledgeBaseFile) {
  if (!currentKnowledgeBase.value) return
  if (confirm(`确定要删除文件"${file.originalFilename}"吗？`)) {
    try {
      const fileId = Number((file as any).fileId ?? (file as any).id)
      const res = await deleteKnowledgeBaseFile(Number(currentKnowledgeBase.value.id), fileId)
      if (res.code === 0) {
        await modal.success('删除成功！')
        await loadKnowledgeBaseFiles(Number(currentKnowledgeBase.value.id))
      } else {
        await modal.error(res.message || '删除失败')
      }
    } catch (e) {
      console.error('删除文件失败:', e)
      await modal.error('删除失败')
    }
  }
}

// 打开文件预览
function openFilePreview(file: KnowledgeBaseFile) {
  // 判断是否为成员
  if (currentUserRole.value === 'none') {
    // 未加入知识库,弹出提示
    modal.error('加入知识库才能浏览完整内容')
    return
  }
  
  // 已加入,正常预览
  const ext = file.originalFilename?.split('.').pop()?.toLowerCase() || ''
  const fileId = Number((file as any).fileId ?? (file as any).id)
  previewFile.value = { id: fileId, originalFilename: file.originalFilename, fileSize: file.fileSize, fileExtension: ext }
  showPreviewModal.value = true
}

// 从预览弹窗下载文件
function handlePreviewDownload() {
  const file = knowledgeBaseFiles.value.find(item => Number((item as any).fileId ?? item.id) === previewFile.value?.id)
  if (file) void handleDownloadFile(file)
}

// 下载文件
async function handleDownloadFile(file: KnowledgeBaseFile) {
  // 判断是否为成员
  if (currentUserRole.value === 'none') {
    // 未加入知识库,弹出提示
    modal.error('加入知识库才能下载文件')
    return
  }
  
  try {
    const fileId = Number((file as any).fileId ?? (file as any).id)
    await downloadKnowledgeBaseFile(fileId, file.originalFilename)
  } catch (error) {
    console.error('下载失败:', error)
    alert('下载失败，请重试')
  }
}

// 打开重命名对话框
function openRenameDialog(file: KnowledgeBaseFile) {
  renamingFile.value = file
  newFileName.value = file.originalFilename
  showRenameDialog.value = true
}

// 重命名文件
async function handleRename() {
  if (!renamingFile.value || !newFileName.value.trim() || !currentKnowledgeBase.value) return
  
  try {
    const fileId = Number((renamingFile.value as any).fileId ?? (renamingFile.value as any).id)
    const res = await renameKnowledgeBaseFile(fileId, newFileName.value.trim())
    if (res.code === 0) {
      alert('重命名成功！')
      showRenameDialog.value = false
      renamingFile.value = null
      newFileName.value = ''
      await loadKnowledgeBaseFiles(Number(currentKnowledgeBase.value.id))
    } else {
      alert('重命名失败: ' + res.message)
    }
  } catch (error) {
    console.error('重命名失败:', error)
    alert('重命名失败，请重试')
  }
}

async function removeMember(member: KnowledgeBaseMember) {
  if (!currentKnowledgeBase.value) return
  if (confirm(`确定要移除成员“${member.username}”吗？`)) {
    try {
      const res = await apiRemoveMember(Number(currentKnowledgeBase.value.id), Number(member.userId))
      if (res.code === 0) {
        alert('移除成功！')
        await loadKnowledgeBaseMembers(Number(currentKnowledgeBase.value.id))
      } else {
        alert(res.message || '移除失败')
      }
    } catch (e) {
      console.error('移除成员失败:', e)
      alert('移除失败')
    }
  }
}

// 邀请成员
async function handleInvite() {
  if (!inviteEmail.value.trim() || !currentKnowledgeBase.value) return
  try {
    const res = await apiInviteMember(Number(currentKnowledgeBase.value.id), {
      email: inviteEmail.value.trim(),
      role: canSetMemberRole.value ? inviteRole.value : 'viewer'
    })
    if (res.code === 0) {
      const roleLabel = roleOptions.find(r => r.value === inviteRole.value)?.label || inviteRole.value
      alert(`已发送邀请给 ${inviteEmail.value} (${roleLabel})`)
      inviteEmail.value = ''
      inviteRole.value = 'viewer'
      showInviteDialog.value = false
      await loadKnowledgeBaseMembers(Number(currentKnowledgeBase.value.id))
    } else {
      alert(res.message || '邀请失败')
    }
  } catch (e) {
    console.error('邀请成员失败:', e)
    alert('邀请失败')
  }
}

// 更改成员角色
async function handleChangeRole(member: KnowledgeBaseMember, newRole: KbRole) {
  if (!currentKnowledgeBase.value) return
  if (!canSetMemberRole.value) {
    alert('无权限')
    return
  }
  try {
    const res = await apiUpdateMemberRole(
      Number(currentKnowledgeBase.value.id),
      Number(member.userId),
      newRole
    )
    if (res.code === 0) {
      alert('权限已更新')
      await loadKnowledgeBaseMembers(Number(currentKnowledgeBase.value.id))
    } else {
      alert(res.message || '更新失败')
    }
  } catch (e) {
    console.error('更新成员角色失败:', e)
    alert('更新失败')
  }
}

// 复制分享链接
function handleCopyLink() {
  if (!currentKnowledgeBase.value) return
  if (!shareLink.value) {
    alert('链接生成失败')
    return
  }
  navigator.clipboard.writeText(shareLink.value)
  alert('链接已复制到剪贴板')
}

// 切换公开状态
async function handleTogglePublic(isPublic: boolean) {
  if (!currentKnowledgeBase.value) return
  try {
    const res = await apiUpdatePublicStatus(Number(currentKnowledgeBase.value.id), isPublic)
    if (res.code === 0) {
      currentKnowledgeBase.value = { ...currentKnowledgeBase.value, isPublic }
      alert(isPublic ? '知识库已设为公开' : '知识库已设为私密')
    } else {
      alert(res.message || '更新失败')
    }
  } catch (e) {
    console.error('更新公开状态失败:', e)
    alert('更新失败')
  }
}

// 获取角色显示名称
function getRoleLabel(role: KbRole | 'none'): string {
  if (role === 'none') return '未知'
  return roleOptions.find(r => r.value === role)?.label || role
}

// 获取角色徽章样式
function getRoleBadgeClass(role: KbRole | 'none'): string {
  switch (role) {
    case 'admin': return 'bg-red-100 text-red-700'
    case 'viewer': return 'bg-gray-100 text-gray-700'
    default: return 'bg-gray-100 text-gray-700'
  }
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

function renderMarkdown(content: string) {
  return marked.parse(content || '') as string
}

// AI 聊天
async function sendKbMessage() {
  if (!kbChatInput.value.trim() || isKbChatting.value || !currentKnowledgeBase.value) return
  const userMessage = kbChatInput.value.trim()
  kbChatMessages.value.push({ type: 'user', content: userMessage, time: new Date().toISOString() })
  kbChatInput.value = ''
  isKbChatting.value = true
  kbChatMessages.value.push({ type: 'ai', content: '', time: new Date().toISOString() })
  kbChatAbortController.value = new AbortController()

  try {
    const reader = await chatWithKnowledgeBaseStream(
      Number(currentKnowledgeBase.value.id),
      userMessage,
      undefined,
      { signal: kbChatAbortController.value.signal }
    )
    const decoder = new TextDecoder()
    let currentContent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const text = decoder.decode(value, { stream: true })
      // 解析SSE格式
      const lines = text.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          // 只移除 "data:" 前缀，不使用trim()以保留空格
          const data = line.slice(5)
          if (data && data !== '[DONE]') {
            // 将 [NL] 标记还原为实际换行符
            currentContent += data.replace(/\[NL\]/g, '\n')
            kbChatMessages.value[kbChatMessages.value.length - 1].content = currentContent
            await nextTick()
            chatContainerRef.value?.scrollTo({ top: chatContainerRef.value.scrollHeight })
          }
        }
      }
    }
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') {
      return
    }
    console.error('AI对话失败:', e)
    kbChatMessages.value[kbChatMessages.value.length - 1].content = '抱歉，AI对话出现错误，请稍后重试'
  } finally {
    isKbChatting.value = false
    kbChatAbortController.value = null
  }
}

function stopKbChat() {
  if (!isKbChatting.value) return
  kbChatAbortController.value?.abort()
}

async function clearKbChat() {
  const confirmed = await modal.confirm({
    title: '清空对话',
    message: '确定要清空对话历史吗？',
    confirmText: '清空',
    cancelText: '取消'
  })
  if (!confirmed) return
  kbChatMessages.value = []
}

// 拖拽
function handleMouseDown(e: MouseEvent) {
  isDragging.value = true
  dragOffset.value = { x: e.clientX - aiChatPosition.value.x, y: e.clientY - aiChatPosition.value.y }
}

function handleMouseMove(e: MouseEvent) {
  if (isDragging.value) {
    aiChatPosition.value = {
      x: Math.max(0, Math.min(e.clientX - dragOffset.value.x, window.innerWidth - 400)),
      y: Math.max(0, Math.min(e.clientY - dragOffset.value.y, window.innerHeight - 600)),
    }
  }
}

function handleMouseUp() {
  isDragging.value = false
}

onMounted(async () => {
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  try {
    siteOrigin.value = window.location.origin
  } catch {}
  
  // 优先从 API 获取最新的用户信息，确保数据是最新的
  try {
    const r = await getUserInfo()
    const user = r?.data
    const uid = user?.id
    if (uid !== undefined && uid !== null && String(uid).trim() !== '') {
      currentUserId.value = Number(uid)
      try {
        // 更新 localStorage 中的用户信息
        localStorage.setItem('userInfo', JSON.stringify(user))
      } catch {}
    }
  } catch (e) {
    console.warn('从 API 获取用户信息失败，尝试使用缓存')
    // 如果 API 调用失败，再尝试使用 localStorage
    try {
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        const user = JSON.parse(userInfo)
        const uid = user?.id ?? user?.userId ?? user?.user_id
        if (uid !== undefined && uid !== null && String(uid).trim() !== '') {
          currentUserId.value = Number(uid)
        }
      }
    } catch (e2) {
      console.error('获取用户信息失败')
    }
  }
  
  // 加载初始数据
  await loadSquareKnowledgeBases()
  await loadCreatedKnowledgeBases()
  await loadJoinedKnowledgeBases()
  
  // 启动邀请轮询
  startInvitationPolling()
  
  // 初始加载邀请列表（如果当前Tab是'我加入的'）
  if (activeTab.value === 'joined') {
    await loadPendingInvitations()
  }
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  // 停止邀请轮询
  stopInvitationPolling()
})

watch(showAiChat, (val) => {
  if (val && aiChatPosition.value.x === 0) {
    aiChatPosition.value = { x: window.innerWidth - 450, y: 100 }
  }
})

function openUploadDialog() {
  if (!permissions.value.canUpload) {
    alert('无权限')
    return
  }
  showUploadDialog.value = true
}

function openInviteDialog() {
  if (!permissions.value.canInvite) {
    alert('无权限')
    return
  }
  showInviteDialog.value = true
}

// 加载待处理邀请列表
async function loadPendingInvitations() {
  try {
    invitationLoading.value = true
    const res = await getMyInvitations({ status: 0, page: 1, size: 10 })
    console.log('邀请列表响应:', res)
    if (res.code === 0 && res.data) {
      // 检查数据结构，可能是 res.data.data 或直接 res.data
      if (Array.isArray(res.data)) {
        pendingInvitations.value = res.data
      } else if (res.data.data && Array.isArray(res.data.data)) {
        pendingInvitations.value = res.data.data
      } else {
        pendingInvitations.value = []
      }
      console.log('待处理邀请数量:', pendingInvitations.value.length)
    } else {
      console.error('邀请列表返回异常:', res)
    }
  } catch (e) {
    console.error('加载邀请列表失败:', e)
  } finally {
    invitationLoading.value = false
  }
}

// 加载待处理邀请数量
async function loadInvitationCount() {
  try {
    const res = await getMyInvitationCount()
    if (res.code === 0) {
      invitationCount.value = res.data || 0
    }
  } catch (e) {
    console.error('加载邀请数量失败:', e)
  }
}

// 接受邀请
async function handleAcceptInvitation(invitation: KbInvitation) {
  try {
    const res = await respondInvitation(invitation.id, true)
    if (res.code === 0) {
      await modal.success('已接受邀请！')
      // 刷新邀请列表和已加入的知识库列表
      await loadPendingInvitations()
      await loadInvitationCount()
      await loadJoinedKnowledgeBases()
    } else {
      await modal.error(res.message || '接受邀请失败')
    }
  } catch (e) {
    console.error('接受邀请失败:', e)
    await modal.error('接受邀请失败')
  }
}

// 拒绝邀请
async function handleRejectInvitation(invitation: KbInvitation) {
  if (!confirm(`确定要拒绝来自 ${invitation.inviterName} 的邀请吗？`)) return
  try {
    const res = await respondInvitation(invitation.id, false)
    if (res.code === 0) {
      await modal.success('已拒绝邀请')
      // 刷新邀请列表
      await loadPendingInvitations()
      await loadInvitationCount()
    } else {
      await modal.error(res.message || '拒绝邀请失败')
    }
  } catch (e) {
    console.error('拒绝邀请失败:', e)
    await modal.error('拒绝邀请失败')
  }
}

// 启动邀请轮询
function startInvitationPolling() {
  // 清除现有定时器
  if (invitationTimer.value !== null) {
    clearInterval(invitationTimer.value)
  }
  // 立即加载一次
  loadInvitationCount()
  // 每30秒轮询一次
  invitationTimer.value = window.setInterval(() => {
    if (activeTab.value === 'joined') {
      loadInvitationCount()
    }
  }, 30000)
}

// 停止邀请轮询
function stopInvitationPolling() {
  if (invitationTimer.value !== null) {
    clearInterval(invitationTimer.value)
    invitationTimer.value = null
  }
}

// 监听Tab切换，当切换到"我加入的"Tab时加载邀请
watch(activeTab, async (newTab) => {
  if (newTab === 'joined') {
    await loadPendingInvitations()
    await loadInvitationCount()
  }
})
</script>

<template>
  <div class="h-full flex flex-col bg-gray-50 overflow-hidden">
    <!-- 顶部标签栏 -->
    <div v-if="activeTab !== 'detail'" class="bg-white border-b border-gray-200 px-6 py-3">
      <div class="flex gap-6">
        <button @click="activeTab = 'square'" :class="['pb-2 border-b-2 transition-colors', activeTab === 'square' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-600 hover:text-gray-900']">知识库广场</button>
        <button @click="activeTab = 'created'" :class="['pb-2 border-b-2 transition-colors', activeTab === 'created' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-600 hover:text-gray-900']">我创建的</button>
        <button @click="activeTab = 'joined'" :class="['pb-2 border-b-2 transition-colors relative', activeTab === 'joined' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-600 hover:text-gray-900']">
          我加入的
          <span v-if="invitationCount > 0" class="absolute -top-1 -right-4 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">{{ invitationCount }}</span>
        </button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="flex-1 overflow-hidden px-6 pt-6">
      <div class="h-full flex flex-col overflow-hidden">
      <!-- 知识库广场 -->
      <div v-if="activeTab === 'square'" class="flex-1 overflow-hidden flex flex-col">
        <div class="flex-1 overflow-y-auto space-y-6">
        <div class="flex gap-3">
          <div class="flex-1 relative">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input v-model="searchKeyword" placeholder="搜索知识库..." class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <Button @click="handleSearch" class="bg-blue-600 hover:bg-blue-700 text-white">搜索</Button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6">
          <div
            v-for="kb in squareKnowledgeBases"
            :key="kb.id"
            class="bg-white rounded-lg border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow cursor-pointer flex flex-col h-[360px]"
            @click="handleKnowledgeBaseClick(kb)"
          >
            <div class="relative w-full h-48">
              <div
                class="w-full h-full"
                :class="kb.coverUrl ? 'bg-gray-100 bg-cover bg-center' : 'bg-gradient-to-br from-blue-500 to-purple-600'"
                :style="kb.coverUrl ? { backgroundImage: `url(${kb.coverUrl})` } : undefined"
              />
              <div class="absolute top-2 right-2 flex gap-1">
                <span class="px-2 py-1 bg-white/90 rounded text-xs flex items-center gap-1">
                  <Lock v-if="kb.hasPassword" class="w-3 h-3" />
                  <Globe v-else class="w-3 h-3" />
                  {{ kb.hasPassword ? '私密' : '公开' }}
                </span>
              </div>
            </div>
            <div class="p-4 flex-1 flex flex-col">
              <h3 class="mb-2 line-clamp-1 text-gray-900">{{ kb.name }}</h3>
              <p class="text-sm text-gray-600 mb-3 line-clamp-2 flex-1">{{ kb.description }}</p>
              <div class="flex items-center gap-4 text-xs text-gray-500 mb-3">
                <span class="flex items-center gap-1"><Users class="w-3 h-3" />{{ kb.memberCount }}</span>
                <span class="flex items-center gap-1"><FileText class="w-3 h-3" />{{ kb.fileCount }}</span>
              </div>
              <div class="flex items-center justify-between pt-3 border-t border-gray-100">
                <span class="text-xs text-gray-500">by {{ kb.creatorName }}</span>
                <Button
                  v-if="kb.isJoined"
                  size="sm"
                  disabled
                  class="bg-gray-500 text-white cursor-not-allowed h-8 px-3 text-xs"
                  @click.stop
                >
                  已加入
                </Button>
                <Button
                  v-else
                  size="sm"
                  class="bg-blue-600 hover:bg-blue-700 text-white h-8 px-3 text-xs"
                  @click.stop="joinKnowledgeBase(kb)"
                >
                  <Lock v-if="kb.hasPassword" class="w-3 h-3 mr-1" />
                  {{ kb.hasPassword ? '输入密码加入' : '立即加入' }}
                </Button>
              </div>
            </div>
          </div>
        </div>
        </div>

        <div v-if="squareTotalPages > 0" class="flex justify-center py-4">
          <el-pagination
            v-model:current-page="squareCurrentPage"
            :page-size="squarePageSize"
            :total="squareTotal"
            size="small"
            layout="prev, pager, next"
          />
        </div>
      </div>
  
      <!-- 我创建的 -->
      <div v-if="activeTab === 'created'" class="flex-1 overflow-hidden flex flex-col">
        <div class="flex-1 overflow-y-auto space-y-6">
        <div class="flex justify-between items-center">
          <h2 class="text-xl text-gray-900">我创建的知识库</h2>
          <Button @click="resetForm(); showCreateDialog = true" class="bg-blue-600 hover:bg-blue-700 text-white"><Plus class="w-4 h-4 mr-2" />创建知识库</Button>
        </div>
        <div v-if="createdKnowledgeBases.length === 0" class="text-center py-20">
          <FileText class="w-16 h-16 mx-auto text-gray-300 mb-4" />
          <p class="text-gray-500 mb-4">还没有创建任何知识库</p>
          <Button @click="resetForm(); showCreateDialog = true" class="bg-blue-600 hover:bg-blue-700 text-white">创建第一个知识库</Button>
        </div>
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6">
          <div
            v-for="kb in pagedCreatedKnowledgeBases"
            :key="kb.id"
            class="bg-white rounded-lg border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow flex flex-col h-[360px]"
          >
            <div
              class="relative w-full h-48 cursor-pointer"
              :class="kb.coverUrl ? 'bg-gray-100 bg-cover bg-center' : 'bg-gradient-to-br from-green-500 to-green-600'"
              :style="kb.coverUrl ? { backgroundImage: `url(${kb.coverUrl})` } : undefined"
              @click="viewKnowledgeBase(kb)"
            />
            <div class="p-4 flex-1 flex flex-col cursor-pointer" @click="viewKnowledgeBase(kb)">
              <h3 class="mb-2 line-clamp-1 text-gray-900">{{ kb.name }}</h3>
              <p class="text-sm text-gray-600 mb-3 line-clamp-2 flex-1">{{ kb.description }}</p>
              <div class="flex items-center gap-4 text-xs text-gray-500 mb-3">
                <span class="flex items-center gap-1"><Users class="w-3 h-3" />{{ kb.memberCount }}</span>
                <span class="flex items-center gap-1"><FileText class="w-3 h-3" />{{ kb.fileCount }}</span>
              </div>
              <div class="flex items-center justify-end pt-3 border-t border-gray-100 gap-2">
                <Button size="sm" variant="ghost" class="bg-gray-100 hover:bg-gray-200 h-8 w-8 p-0 justify-center" @click.stop="editKnowledgeBase(kb)">
                  <Edit2 class="w-4 h-4" />
                </Button>
                <Button size="sm" variant="ghost" class="bg-red-50 hover:bg-red-100 text-red-600 h-8 w-8 p-0 justify-center" @click.stop="deleteKnowledgeBase(kb)">
                  <Trash2 class="w-4 h-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>
        </div>

        <div v-if="createdTotalPages > 0" class="flex justify-center py-4">
          <el-pagination
            v-model:current-page="createdCurrentPage"
            :page-size="createdPageSize"
            :total="createdKnowledgeBases.length"
            size="small"
            layout="prev, pager, next"
          />
        </div>
      </div>

      <!-- 我加入的 -->
      <div v-if="activeTab === 'joined'" class="flex-1 overflow-hidden flex flex-col">
        <div class="flex-1 overflow-y-auto space-y-6">
        <h2 class="text-xl text-gray-900">我加入的知识库</h2>
        
        <!-- 待处理邀请区域 -->
        <div v-if="pendingInvitations.length > 0" class="space-y-4">
          <div class="flex items-center gap-2">
            <h3 class="text-lg font-medium text-gray-900">待处理的邀请</h3>
            <span class="bg-red-100 text-red-600 text-xs px-2 py-0.5 rounded-full">{{ invitationCount }}</span>
          </div>
          
          <div class="grid grid-cols-1 gap-4">
            <div
              v-for="invitation in pendingInvitations"
              :key="invitation.id"
              class="bg-gradient-to-r from-blue-50 to-purple-50 border-2 border-blue-200 rounded-lg p-4 hover:shadow-md transition-shadow"
            >
              <div class="flex items-start gap-4">
                <!-- 左侧信息 -->
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-2">
                    <h4 class="text-lg font-medium text-gray-900">{{ invitation.knowledgeBaseName }}</h4>
                    <span class="px-2 py-0.5 bg-blue-100 text-blue-700 text-xs rounded">
                      {{ invitation.roleLabel }}
                    </span>
                  </div>
                  <p class="text-sm text-gray-600 mb-2">
                    <span class="font-medium">{{ invitation.inviterName }}</span> 邀请你加入此知识库
                  </p>
                  <div class="flex items-center gap-4 text-xs text-gray-500">
                    <span>邀请时间：{{ formatDateTime(invitation.inviteTime) }}</span>
                    <span v-if="invitation.daysLeft !== undefined" :class="invitation.daysLeft <= 2 ? 'text-red-500' : ''">
                      剩余 {{ invitation.daysLeft }} 天
                    </span>
                  </div>
                  <p v-if="invitation.inviterMessage" class="text-sm text-gray-700 mt-2 italic">
                    “{{ invitation.inviterMessage }}”
                  </p>
                </div>
                
                <!-- 右侧按钮 -->
                <div class="flex gap-2">
                  <Button
                    size="sm"
                    class="bg-green-600 hover:bg-green-700 text-white"
                    @click="handleAcceptInvitation(invitation)"
                  >
                    <Check class="w-4 h-4 mr-1" />接受
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    class="border-gray-300 hover:bg-gray-100"
                    @click="handleRejectInvitation(invitation)"
                  >
                    <X class="w-4 h-4 mr-1" />拒绝
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 加载中状态 -->
        <div v-if="invitationLoading && pendingInvitations.length === 0" class="text-center py-4">
          <p class="text-gray-500 text-sm">加载邀请中...</p>
        </div>
        
        <!-- 已加入的知识库列表 -->
        <div v-if="joinedKnowledgeBases.length === 0 && pendingInvitations.length === 0" class="text-center py-20">
          <Users class="w-16 h-16 mx-auto text-gray-300 mb-4" />
          <p class="text-gray-500">还没有加入任何知识库</p>
        </div>
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6">
          <div
            v-for="kb in pagedJoinedKnowledgeBases"
            :key="kb.id"
            class="bg-white rounded-lg border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow flex flex-col h-[360px]"
          >
            <div
              class="relative w-full h-48 cursor-pointer"
              :class="kb.coverUrl ? 'bg-gray-100 bg-cover bg-center' : 'bg-gradient-to-br from-purple-500 to-pink-600'"
              :style="kb.coverUrl ? { backgroundImage: `url(${kb.coverUrl})` } : undefined"
              @click="viewKnowledgeBase(kb)"
            />
            <div class="p-4 flex-1 flex flex-col cursor-pointer" @click="viewKnowledgeBase(kb)">
              <h3 class="mb-2 line-clamp-1 text-gray-900">{{ kb.name }}</h3>
              <p class="text-sm text-gray-600 mb-3 line-clamp-2 flex-1">{{ kb.description }}</p>
              <div class="flex items-center gap-4 text-xs text-gray-500 mb-3">
                <span class="flex items-center gap-1"><Users class="w-3 h-3" />{{ kb.memberCount }}</span>
                <span class="flex items-center gap-1"><FileText class="w-3 h-3" />{{ kb.fileCount }}</span>
              </div>
              <div class="flex items-center justify-between pt-3 border-t border-gray-100">
                <span class="text-xs text-gray-500">by {{ kb.creatorName }}</span>
                <Button size="sm" variant="ghost" class="bg-red-50 hover:bg-red-100 text-red-600 h-8 px-3 text-xs" @click.stop="leaveKnowledgeBase(kb)">
                  <LogOut class="w-3 h-3 mr-1" />退出
                </Button>
              </div>
            </div>
          </div>
        </div>
        </div>

        <div v-if="joinedTotalPages > 0" class="flex justify-center py-4">
          <el-pagination
            v-model:current-page="joinedCurrentPage"
            :page-size="joinedPageSize"
            :total="joinedKnowledgeBases.length"
            size="small"
            layout="prev, pager, next"
          />
        </div>
      </div>

      <!-- 知识库详情 -->
      <div v-if="activeTab === 'detail' && currentKnowledgeBase" class="space-y-6">
        <div class="bg-white rounded-lg border border-gray-200 p-6">
          <Button variant="ghost" size="sm" @click="goBackToList" class="mb-4"><ArrowLeft class="w-4 h-4 mr-2" />返回</Button>
          <div class="flex gap-6">
            <div
              class="w-48 h-32 rounded-lg flex-shrink-0 overflow-hidden"
              :class="currentKnowledgeBase.coverUrl ? 'bg-gray-100' : 'bg-gradient-to-br from-blue-500 to-purple-600'"
            >
              <img
                v-if="currentKnowledgeBase.coverUrl"
                :src="currentKnowledgeBase.coverUrl"
                :alt="currentKnowledgeBase.name"
                class="w-full h-full object-cover"
              />
            </div>
            <div class="flex-1 space-y-3">
              <h1 class="text-2xl text-gray-900">{{ currentKnowledgeBase.name }}</h1>
              <p class="text-gray-600">{{ currentKnowledgeBase.description }}</p>
              <div class="flex items-center gap-4 text-sm">
                <span class="flex items-center gap-1" :class="currentKnowledgeBase.hasPassword ? 'text-orange-600' : 'text-green-600'">
                  <Lock v-if="currentKnowledgeBase.hasPassword" class="w-4 h-4" /><Globe v-else class="w-4 h-4" />
                  {{ currentKnowledgeBase.hasPassword ? '私密' : '公开' }}
                </span>
                <span class="flex items-center gap-1 text-gray-600"><Users class="w-4 h-4" />{{ currentKnowledgeBase.memberCount }} 成员</span>
                <span class="flex items-center gap-1 text-gray-600"><FileText class="w-4 h-4" />{{ currentKnowledgeBase.fileCount }} 文件</span>
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <Button @click="openUploadDialog" class="bg-blue-600 hover:bg-blue-700 text-white"><Upload class="w-4 h-4 mr-2" />上传文件</Button>
              <Button variant="outline" @click="openInviteDialog"><UserPlus class="w-4 h-4 mr-2" />邀请成员</Button>
              <Button v-if="permissions.canShare" variant="outline" @click="showShareDialog = true"><Share2 class="w-4 h-4 mr-2" />分享</Button>
              <Button variant="outline" @click="showMembersDialog = true"><Users class="w-4 h-4 mr-2" />成员列表</Button>
              <Button variant="outline" @click="showAiChat = !showAiChat"><Bot class="w-4 h-4 mr-2" />{{ showAiChat ? '关闭' : '打开' }} AI 助手</Button>
            </div>
          </div>
        </div>

        <!-- 文件列表 -->
        <div class="bg-white rounded-lg border border-gray-200 p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg text-gray-900">文件列表</h2>
            <div class="relative">
              <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input v-model="fileSearchKeyword" placeholder="搜索文件..." class="pl-9 pr-4 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>
          <table class="w-full">
            <thead class="bg-gray-50 border-b border-gray-200">
              <tr>
                <th class="px-4 py-3 text-left text-xs text-gray-600">文件名</th>
                <th class="px-4 py-3 text-left text-xs text-gray-600">大小</th>
                <th class="px-4 py-3 text-left text-xs text-gray-600">上传者</th>
                <th class="px-4 py-3 text-left text-xs text-gray-600">上传时间</th>
                <th class="px-4 py-3 text-left text-xs text-gray-600">来源</th>
                <th class="px-4 py-3 text-center text-xs text-gray-600">操作</th>
              </tr>
            </thead>
            <tbody v-if="knowledgeBaseFiles.length > 0" class="divide-y divide-gray-200">
              <tr v-for="file in knowledgeBaseFiles" :key="file.id" class="hover:bg-gray-50">
                <td class="px-4 py-3"><div class="flex items-center gap-2"><FileText class="w-4 h-4 text-blue-600" /><span class="text-sm text-gray-900">{{ file.originalFilename }}</span></div></td>
                <td class="px-4 py-3 text-sm text-gray-600">{{ formatFileSize(file.fileSize) }}</td>
                <td class="px-4 py-3 text-sm text-gray-600">{{ file.uploaderName }}</td>
                <td class="px-4 py-3 text-sm text-gray-600">{{ formatDateTime(file.uploadTime) }}</td>
                <td class="px-4 py-3"><span :class="['inline-flex items-center px-2 py-1 rounded-full text-xs', file.sourceType === 'upload' ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700']">{{ file.sourceType === 'upload' ? '直接上传' : '笔记转换' }}</span></td>
                <td class="px-4 py-3">
                  <div class="flex items-center justify-center gap-1">
                    <Button variant="ghost" size="sm" @click="openFilePreview(file)" title="预览文件"><Eye class="w-4 h-4" /></Button>
                    <Button variant="ghost" size="sm" @click="handleDownloadFile(file)" title="下载"><Download class="w-4 h-4" /></Button>
                    <Button v-if="permissions.canEdit" variant="ghost" size="sm" @click="openRenameDialog(file)" title="重命名"><Pen class="w-4 h-4" /></Button>
                    <Button v-if="permissions.canDelete" variant="ghost" size="sm" @click="deleteFile(file)" title="删除"><Trash2 class="w-4 h-4 text-red-600" /></Button>
                  </div>
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr>
                <td colspan="6" class="px-4 py-12 text-center text-gray-500">
                  <FileText class="w-12 h-12 mx-auto text-gray-300 mb-2" />
                  <p>该知识库还没有上传任何文件</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    </div>

    <!-- 创建/编辑对话框 -->
    <div v-if="showCreateDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-lg">
        <h2 class="text-lg text-gray-900 mb-4">{{ formData.id ? '编辑知识库' : '创建知识库' }}</h2>
        <div class="space-y-4">
          <div><label class="block text-sm text-gray-700 mb-2">知识库名称</label><input v-model="formData.name" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="请输入知识库名称" /></div>
          <div><label class="block text-sm text-gray-700 mb-2">描述</label><textarea v-model="formData.description" rows="3" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none" placeholder="请输入知识库描述"></textarea></div>
          <div>
            <label class="block text-sm text-gray-700 mb-2">封面图片</label>
            <div class="flex items-center gap-4">
              <div class="w-24 h-16 rounded-lg border border-gray-200 overflow-hidden bg-gray-50 flex items-center justify-center">
                <img v-if="formData.coverUrl" :src="formData.coverUrl" alt="封面" class="w-full h-full object-cover" />
                <span v-else class="text-xs text-gray-400">暂无封面</span>
              </div>
              <div class="flex items-center gap-2">
                <input ref="coverInputRef" type="file" accept="image/*" class="hidden" @change="handleCoverSelect" />
                <Button variant="outline" size="sm" @click="triggerCoverSelect" :disabled="coverUploading">
                  {{ coverUploading ? '上传中...' : '上传封面' }}
                </Button>
                <Button v-if="formData.coverUrl" variant="ghost" size="sm" @click="clearCover">移除</Button>
              </div>
            </div>
          </div>
          <label class="flex items-center gap-2"><input type="checkbox" v-model="formData.isPublic" class="w-4 h-4 text-blue-600 rounded" /><span class="text-sm text-gray-700">公开知识库</span></label>
          <div v-if="!formData.isPublic"><label class="block text-sm text-gray-700 mb-2">访问密码（可选）</label><input type="password" v-model="formData.password" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="留空则无需密码" /></div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <Button variant="outline" @click="showCreateDialog = false; resetForm()">取消</Button>
          <Button @click="handleCreateKnowledgeBase" class="bg-blue-600 hover:bg-blue-700 text-white" :disabled="!formData.name.trim()">{{ formData.id ? '保存' : '创建' }}</Button>
        </div>
      </div>
    </div>

    <!-- 加入对话框 -->
    <div v-if="showJoinDialog && joiningKb" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-lg text-gray-900 mb-4">加入知识库</h2>
        <p class="text-sm text-gray-600 mb-4">知识库 "{{ joiningKb.name }}" 需要密码才能加入</p>
        <div class="mb-4"><label class="block text-sm text-gray-700 mb-2">访问密码</label><input type="password" v-model="joinPassword" @keypress.enter="confirmJoin" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="请输入密码" /></div>
        <div class="flex justify-end gap-3">
          <Button variant="outline" @click="showJoinDialog = false; joinPassword = ''; joiningKb = null">取消</Button>
          <Button @click="confirmJoin" class="bg-blue-600 hover:bg-blue-700 text-white" :disabled="!joinPassword.trim()">加入</Button>
        </div>
      </div>
    </div>

    <!-- 上传文件对话框 -->
    <div v-if="showUploadDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-lg">
        <h2 class="text-lg text-gray-900 mb-4">上传文件</h2>
        <div v-if="!isUploading" class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center mb-4">
          <Upload class="w-12 h-12 mx-auto text-gray-400 mb-3" />
          <p class="text-sm text-gray-600 mb-2">拖拽文件到此处，或点击选择文件</p>
          <p class="text-xs text-gray-400 mb-3">单个文件不超过 200MB</p>
          <input ref="uploadInputRef" type="file" multiple @change="handleFileSelect" class="hidden" />
          <Button variant="outline" size="sm" class="cursor-pointer" @click="triggerUploadFileSelect">选择文件</Button>
        </div>
        <div v-if="uploadFileList.length > 0" class="mb-4">
          <p class="text-sm text-gray-700 mb-2">{{ isUploading ? '正在上传文件并处理文档...' : `已选择 ${uploadFileList.length} 个文件：` }}</p>
          <div class="space-y-2 max-h-52 overflow-y-auto">
            <div v-for="(file, index) in uploadFileList" :key="index" class="text-sm bg-gray-50 px-3 py-2 rounded">
              <div class="flex items-center justify-between mb-1">
                <span class="truncate text-gray-700 flex-1 mr-2">{{ file.name }}</span>
                <span class="text-xs text-gray-400 shrink-0">{{ formatFileSize(file.size) }}</span>
                <span v-if="uploadStatuses[index] === 'done'" class="ml-2 text-green-500 text-xs shrink-0">✓ 完成</span>
                <span v-else-if="uploadStatuses[index] === 'error'" class="ml-2 text-red-500 text-xs shrink-0">✗ 失败</span>
                <span v-else-if="uploadStatuses[index] === 'processing'" class="ml-2 text-amber-500 text-xs shrink-0">处理中...</span>
                <span v-else-if="uploadStatuses[index] === 'uploading'" class="ml-2 text-blue-500 text-xs shrink-0">{{ uploadProgresses[index] }}%</span>
              </div>
              <div v-if="isUploading || uploadStatuses[index] !== 'pending'" class="w-full bg-gray-200 rounded-full h-1.5">
                <div
                  class="h-1.5 rounded-full transition-all duration-200"
                  :class="{
                    'bg-blue-500': uploadStatuses[index] === 'uploading',
                    'bg-amber-500': uploadStatuses[index] === 'processing',
                    'bg-green-500': uploadStatuses[index] === 'done',
                    'bg-red-400': uploadStatuses[index] === 'error',
                    'bg-gray-300': uploadStatuses[index] === 'pending'
                  }"
                  :style="{ width: `${uploadProgresses[index]}%` }"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-3">
          <Button variant="outline" :disabled="isUploading" @click="showUploadDialog = false; uploadFileList = []; uploadProgresses = []; uploadStatuses = []">取消</Button>
          <Button @click="handleUploadFiles" class="bg-blue-600 hover:bg-blue-700 text-white" :disabled="uploadFileList.length === 0 || isUploading">
            {{ isUploading ? '上传中...' : '上传' }}
          </Button>
        </div>
      </div>
    </div>

    <!-- 邀请成员对话框 -->
    <div v-if="showInviteDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-lg text-gray-900 mb-2">邀请成员</h2>
        <p class="text-sm text-gray-500 mb-4">邀请新成员加入 "{{ currentKnowledgeBase?.name }}"</p>
        <div class="space-y-4">
          <div>
            <label class="block text-sm text-gray-700 mb-2">邮箱地址 / 用户名</label>
            <input v-model="inviteEmail" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="输入用户邮箱或用户名..." />
          </div>
          <div v-if="canSetMemberRole">
            <label class="block text-sm text-gray-700 mb-2">设置角色</label>
            <Select v-model="inviteRole" :options="roleOptions" placeholder="选择角色" />
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <Button variant="outline" @click="showInviteDialog = false; inviteEmail = ''">取消</Button>
          <Button @click="handleInvite" class="bg-blue-600 hover:bg-blue-700 text-white" :disabled="!inviteEmail.trim()">发送邀请</Button>
        </div>
      </div>
    </div>

    <!-- 分享对话框 -->
    <div v-if="showShareDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-lg text-gray-900 mb-2">分享知识库</h2>
        <p class="text-sm text-gray-500 mb-4">设置访问权限和分享链接</p>
        <div class="space-y-6">
          <!-- 公开访问开关 -->
          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div class="space-y-1">
              <div class="font-medium flex items-center gap-2 text-sm">
                <Globe class="w-4 h-4" /> 公开访问
              </div>
              <div class="text-xs text-gray-500">
                {{ currentKnowledgeBase?.isPublic ? '任何人均可查看此知识库' : '仅成员可访问此知识库' }}
              </div>
            </div>
            <Switch 
              :model-value="currentKnowledgeBase?.isPublic"
              @update:model-value="handleTogglePublic"
            />
          </div>
          <!-- 分享链接 -->
          <div>
            <label class="block text-sm text-gray-700 mb-2">分享链接</label>
            <div class="flex gap-2">
              <input readonly :value="shareLink" class="flex-1 px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-sm text-gray-600" />
              <Button variant="outline" @click="handleCopyLink"><Copy class="w-4 h-4 mr-1" />复制</Button>
            </div>
          </div>
        </div>
        <div class="flex justify-end mt-6">
          <Button variant="outline" @click="showShareDialog = false">关闭</Button>
        </div>
      </div>
    </div>

    <!-- 成员管理对话框 -->
    <div v-if="showMembersDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[80vh] flex flex-col">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg text-gray-900">成员管理 ({{ members.length }})</h2>
          <p class="text-sm text-gray-500">查看和管理知识库成员权限</p>
        </div>
        <div class="flex-1 overflow-y-auto">
          <div class="space-y-3">
            <div v-for="member in members" :key="member.id" class="flex items-center justify-between p-3 hover:bg-gray-50 rounded-lg border border-gray-100">
              <div class="flex items-center gap-3">
                <Avatar class="w-10 h-10">
                  <img
                    v-if="member.avatarUrl"
                    :src="member.avatarUrl"
                    :alt="member.username"
                    class="w-full h-full object-cover rounded-full"
                  />
                  <div v-else class="w-full h-full bg-blue-500 rounded-full flex items-center justify-center text-white text-sm font-medium">{{ member.username[0] }}</div>
                </Avatar>
                <div>
                  <div class="font-medium text-sm text-gray-900 flex items-center gap-2">
                    {{ member.username }}
                    <span class="text-xs text-gray-500">
                      {{ member.status || member.roleName || getRoleLabel(member.role || 'viewer') }}
                    </span>
                  </div>
                  <div class="text-xs text-gray-500">加入时间: {{ formatDateTime(String(member.joinTime || '')) }}</div>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <!-- 角色显示/选择 -->
                <template v-if="canSetMemberRole && Number(member.userId) !== currentUserId && Number(member.userId) !== Number(currentKnowledgeBase?.creatorId)">
                  <Select 
                    :model-value="resolveMemberRole(member)" 
                    @update:model-value="(val: string) => handleChangeRole(member, val as KbRole)"
                    :options="roleOptions"
                    class="w-28"
                  />
                </template>
                <template v-else>
                  <span :class="['inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium', getRoleBadgeClass(resolveMemberRole(member))]">
                    {{ getRoleLabel(resolveMemberRole(member)) }}
                  </span>
                </template>
                <!-- 移除按钮 -->
                <Button 
                  v-if="canManageMembers && Number(member.userId) !== currentUserId && Number(member.userId) !== Number(currentKnowledgeBase?.creatorId)"
                  variant="ghost" 
                  size="sm" 
                  @click="removeMember(member)" 
                  class="text-red-500 hover:bg-red-50"
                >
                  <X class="w-4 h-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>
        <div class="flex justify-end mt-6 pt-4 border-t border-gray-200">
          <Button variant="outline" @click="showMembersDialog = false">关闭</Button>
        </div>
      </div>
    </div>

    <!-- AI 助手浮窗 -->
    <div v-if="showAiChat && activeTab === 'detail'" class="fixed bg-white rounded-lg shadow-2xl border border-gray-200 flex flex-col z-50" :style="{ left: aiChatPosition.x + 'px', top: aiChatPosition.y + 'px', width: '400px', height: '600px' }">
      <div class="bg-gradient-to-r from-blue-600 to-purple-600 text-white px-4 py-3 rounded-t-lg cursor-move" @mousedown="handleMouseDown">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2"><Bot class="w-5 h-5" /><div><h3 class="text-sm">AI 智能助手</h3><p class="text-xs opacity-90">基于知识库内容的智能问答</p></div></div>
          <Button variant="ghost" size="sm" @click="showAiChat = false" class="text-white hover:bg-white/20 p-1"><X class="w-4 h-4" /></Button>
        </div>
      </div>
      <div ref="chatContainerRef" class="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
        <div v-if="kbChatMessages.length === 0" class="text-center py-8">
          <Sparkles class="w-12 h-12 mx-auto text-blue-500 mb-3" />
          <p class="text-gray-600 mb-4">你好！我是 AI 助手</p>
          <p class="text-sm text-gray-500 mb-4">你可以问我关于这个知识库的任何问题</p>
          <div class="space-y-2">
            <p class="text-xs text-gray-500">试试这些问题：</p>
            <button v-for="(q, i) in quickQuestions" :key="i" @click="kbChatInput = q" class="block w-full text-left text-sm text-blue-600 hover:bg-blue-50 px-3 py-2 rounded-lg transition-colors">{{ q }}</button>
          </div>
        </div>
        <template v-else>
          <div v-for="(msg, index) in kbChatMessages" :key="index" :class="['flex', msg.type === 'user' ? 'justify-end' : 'justify-start']">
            <div :class="['max-w-[80%] rounded-lg px-4 py-2', msg.type === 'user' ? 'bg-blue-600 text-white' : 'bg-white border border-gray-200 text-gray-900']">
              <template v-if="msg.type === 'ai'">
                <div class="text-sm markdown-preview break-words" v-html="renderMarkdown(msg.content)"></div>
              </template>
              <p v-else class="text-sm whitespace-pre-wrap break-words">{{ msg.content }}</p>
            </div>
          </div>
        </template>
      </div>
      <div class="border-t border-gray-200 p-3 bg-white rounded-b-lg">
        <div v-if="kbChatMessages.length > 0" class="mb-2"><Button variant="ghost" size="sm" @click="clearKbChat" class="text-xs text-gray-600 hover:text-red-600"><Trash2 class="w-3 h-3 mr-1" />清空对话</Button></div>
        <div class="flex gap-2">
          <textarea v-model="kbChatInput" @keydown.enter.prevent="sendKbMessage" placeholder="输入你的问题..." rows="2" class="flex-1 px-3 py-2 border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm" :disabled="isKbChatting"></textarea>
          <Button
            @click="isKbChatting ? stopKbChat() : sendKbMessage()"
            :disabled="!isKbChatting && !kbChatInput.trim()"
            class="bg-blue-600 hover:bg-blue-700 text-white self-end"
            size="sm"
            :title="isKbChatting ? '停止回复' : '发送'"
          >
            <Square v-if="isKbChatting" class="w-4 h-4" />
            <Send v-else class="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <div v-if="showRenameDialog && renamingFile" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-lg font-medium text-gray-900 mb-4">重命名文件</h2>
        <div class="space-y-4 mb-4">
          <div>
            <label class="block text-sm text-gray-700 mb-2">当前文件名</label>
            <span class="text-gray-900">{{ renamingFile.originalFilename }}</span>
          </div>
          <div>
            <label class="block text-sm text-gray-700 mb-2">新文件名</label>
            <input type="text" v-model="newFileName" @keypress.enter="handleRename" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
        </div>
        <div class="flex justify-end gap-3">
          <Button variant="outline" @click="showRenameDialog = false; renamingFile = null; newFileName = ''">取消</Button>
          <Button @click="handleRename" class="bg-blue-600 hover:bg-blue-700 text-white" :disabled="!newFileName.trim()">确认重命名</Button>
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
