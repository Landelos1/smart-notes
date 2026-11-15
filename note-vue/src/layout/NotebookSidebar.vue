<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import SIcon from '@/components/ui/SIcon.vue'
import type { Notebook, FilterType, Note } from '../types/note'
import { getUserInfo, logout as apiLogout } from '@/api/auth'
import { useModal } from '@/composables/useModal'

type PageKey =
  | 'notes'
  | 'note-square'
  | 'note-favorites'
  | 'note-likes'
  | 'note-my-notes'
  | 'team'
  | 'ai-chat'
  | 'template-center'
  | 'settings'
  | 'shared-knowledge-base'
  | 'personal-knowledge-base'
  | 'recycle-bin'
  | 'member-system'
  | 'learning-plans'

interface Props {
  notebooks: Notebook[]
  selectedFilter: FilterType
  notes: Note[]
  selectedNoteId: string | null
  /** 主区当前页面，用于左侧导航高亮 */
  currentPage?: PageKey
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'filter-change': [filter: FilterType]
  'create-note': []
  'create-notebook': []
  'delete-notebook': [notebookId: string]
  'navigate': [page: PageKey]
  'open-member-center': []
  'logout': []
  'select-note': [note: Note]
  'toggle-pin': [noteId: string]
  'delete-note': [noteId: string]
  'export-note': [note: Note]
  'reorder': [noteIds: string[]]
}>()

/** 主导航：浏览类入口 */
const navBrowse: { key: PageKey; icon: string; label: string }[] = [
  { key: 'note-square', icon: 'compass', label: '笔记广场' },
  { key: 'team', icon: 'users', label: '团队协作空间' },
  { key: 'note-favorites', icon: 'bookmark', label: '我的收藏' },
  { key: 'note-likes', icon: 'heart', label: '我的点赞' },
  { key: 'note-my-notes', icon: 'files', label: '我的笔记' }
]

/** 底部工具区入口 */
const navTools: { key: PageKey; icon: string; label: string; badge?: string }[] = [
  { key: 'ai-chat', icon: 'sparkle', label: 'AI 助手', badge: '新' },
  { key: 'learning-plans', icon: 'check-square', label: '任务与复习' },
  { key: 'personal-knowledge-base', icon: 'archive', label: '个人知识库' },
  { key: 'shared-knowledge-base', icon: 'share', label: '共享知识库' },
  { key: 'template-center', icon: 'layout', label: '模板中心' },
  { key: 'recycle-bin', icon: 'trash', label: '回收站' }
]

const expandedSections = ref<string[]>(['notebooks'])
const expandedNotebooks = ref<string[]>([])
const showUserMenu = ref(false)

// 搜索与标签筛选
const searchKeyword = ref('')
const selectedTag = ref('')
const tagSectionExpanded = ref(false)
const showAllTags = ref(false)

// 拖动排序状态
const draggingId = ref<string | null>(null)
const dragOverId = ref<string | null>(null)

/** 当前是否停留在「我的笔记」主视图（决定笔记本区高亮） */
const isNotesView = computed(() => (props.currentPage || 'notes') === 'notes')

// 聚合所有标签
const allTags = computed(() => {
  const tagCounts = new Map<string, number>()
  props.notes.forEach(note => {
    ;(note.tags || []).forEach((tag: string) => {
      tagCounts.set(tag, (tagCounts.get(tag) || 0) + 1)
    })
  })
  return Array.from(tagCounts, ([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'zh-CN'))
})

const visibleTags = computed(() => {
  if (showAllTags.value) return allTags.value
  const popular = allTags.value.slice(0, 6)
  if (selectedTag.value && !popular.some(tag => tag.name === selectedTag.value)) {
    const selected = allTags.value.find(tag => tag.name === selectedTag.value)
    if (selected) return [...popular, selected]
  }
  return popular
})

const selectedTagNoteCount = computed(() =>
  selectedTag.value
    ? props.notes.filter(note => (note.tags || []).includes(selectedTag.value)).length
    : 0
)

// 搜索+标签筛选后的笔记
function getFilteredNotes(noteList: Note[]): Note[] {
  let list = noteList
  const kw = searchKeyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(
      n =>
        (n.title || '').toLowerCase().includes(kw) ||
        (n.content || '').toLowerCase().includes(kw)
    )
  }
  if (selectedTag.value) {
    list = list.filter(n => (n.tags || []).includes(selectedTag.value))
  }
  return list
}

function selectTag(tag: string) {
  selectedTag.value = selectedTag.value === tag ? '' : tag
  if (selectedTag.value) tagSectionExpanded.value = true
}

// 拖动排序（只允许非置顶笔记间排序）
function onDragStart(e: DragEvent, noteId: string) {
  draggingId.value = noteId
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', noteId)
  }
}

function onDragOver(e: DragEvent, noteId: string) {
  e.preventDefault()
  if (draggingId.value && draggingId.value !== noteId) {
    dragOverId.value = noteId
  }
}

function onDragLeave() {
  dragOverId.value = null
}

function onDrop(e: DragEvent, targetId: string, noteList: Note[]) {
  e.preventDefault()
  dragOverId.value = null
  if (!draggingId.value || draggingId.value === targetId) return
  const sourceNote = noteList.find(n => n.id === draggingId.value)
  const targetNote = noteList.find(n => n.id === targetId)
  if (sourceNote?.isPinned || targetNote?.isPinned) return
  const allIds = props.notes.map(n => n.id)
  const fromIdx = allIds.indexOf(draggingId.value)
  const toIdx = allIds.indexOf(targetId)
  if (fromIdx === -1 || toIdx === -1) return
  const newIds = [...allIds]
  newIds.splice(fromIdx, 1)
  newIds.splice(toIdx, 0, draggingId.value)
  emit('reorder', newIds)
  draggingId.value = null
}

function onDragEnd() {
  draggingId.value = null
  dragOverId.value = null
}

// 用户信息
const userInfo = ref<any>(null)
const loadingUser = ref(false)
const displayNotebooks = computed(() => props.notebooks.filter(nb => nb.name !== '默认笔记本'))

// 计算用户名首字
const userInitial = computed(() => {
  if (userInfo.value?.username) {
    return userInfo.value.username.charAt(0)
  }
  return '用'
})

// 计算显示用户名
const displayName = computed(() => {
  if (userInfo.value?.username) {
    return userInfo.value.username
  }
  return '未登录'
})

function parseToDate(input: unknown): Date | null {
  if (input === null || input === undefined || input === '') return null

  if (input instanceof Date) {
    return isNaN(input.getTime()) ? null : input
  }

  if (typeof input === 'number') {
    const ms = input < 1e12 ? input * 1000 : input
    const d = new Date(ms)
    return isNaN(d.getTime()) ? null : d
  }

  const s = String(input).trim()
  if (!s) return null
  if (/^\d+$/.test(s)) {
    const n = Number(s)
    const ms = n < 1e12 ? n * 1000 : n
    const d = new Date(ms)
    return isNaN(d.getTime()) ? null : d
  }

  const d = new Date(s)
  return isNaN(d.getTime()) ? null : d
}

function formatYmd(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const memberLevelText = computed(() => {
  const u = userInfo.value
  if (!u) return ''
  if (u.memberLevelName) return String(u.memberLevelName)
  if (u.vipLevelName) return String(u.vipLevelName)
  if (u.planName) return String(u.planName)
  if (u.memberStatusName && String(u.memberStatusName) !== '普通用户') return String(u.memberStatusName)
  return '专业会员'
})

const memberExpireText = computed(() => {
  const u = userInfo.value
  if (!u) return ''
  const raw = u.memberExpireTime ?? u.memberExpireAt ?? u.vipExpireTime ?? u.vipExpireAt ?? u.expireTime ?? u.expireAt
  if (!raw) return ''
  return String(raw)
})

const memberExpireDate = computed(() => parseToDate(memberExpireText.value))

const memberExpireYmd = computed(() => {
  const d = memberExpireDate.value
  if (!d) return ''
  return formatYmd(d)
})

const isMemberOpened = computed(() => {
  const u = userInfo.value
  if (!u) return false
  if (u.isVip === true || u.isMember === true) return true
  if (memberExpireDate.value) return true
  const name = currentMemberStatusText.value
  return name !== '普通用户' && name !== '未登录'
})

const isMemberExpired = computed(() => {
  if (!memberExpireDate.value) return false
  const end = new Date(memberExpireDate.value)
  end.setHours(23, 59, 59, 999)
  return Date.now() > end.getTime()
})

const currentMemberStatusText = computed(() => {
  const u = userInfo.value
  if (!u) return '未登录'
  if (memberExpireDate.value) {
    return isMemberExpired.value ? '普通用户' : (memberLevelText.value || '专业会员')
  }
  if (u.isVip === true || u.isMember === true) return memberLevelText.value || '专业会员'
  return '普通用户'
})

const memberTooltipLine1 = computed(() => `会员状态：${currentMemberStatusText.value}`)

const memberTooltipLine2 = computed(() => {
  if (!isMemberOpened.value) return ''
  if (!memberExpireYmd.value) return ''
  const level = memberLevelText.value || '专业会员'
  if (isMemberExpired.value) return `${level}，已于${memberExpireYmd.value}过期`
  return `${level}，有效期至${memberExpireYmd.value}`
})

// 加载用户信息
async function loadUserInfo() {
  loadingUser.value = true
  try {
    const r = await getUserInfo()
    if (r && r.code === 0 && r.data) {
      userInfo.value = r.data
    }
  } catch (error) {
    console.error('[Sidebar] 加载用户信息失败:', error)
  } finally {
    loadingUser.value = false
  }
}

function toggleSection(section: string) {
  if (expandedSections.value.includes(section)) {
    expandedSections.value = expandedSections.value.filter(s => s !== section)
  } else {
    expandedSections.value = [...expandedSections.value, section]
  }
}

function toggleNotebook(notebookId: string) {
  if (expandedNotebooks.value.includes(notebookId)) {
    expandedNotebooks.value = expandedNotebooks.value.filter(id => id !== notebookId)
  } else {
    expandedNotebooks.value = [...expandedNotebooks.value, notebookId]
  }
}

// 获取笔记本下的笔记列表
function getNotebookNotes(notebookId: string) {
  return props.notes.filter(note => note.notebookId === notebookId)
}

// 获取所有笔记
function getAllNotes() {
  return props.notes
}

// 格式化时间
function formatTime(date: Date | undefined): string {
  if (!date) return ''
  const now = new Date()
  const diff = now.getTime() - new Date(date).getTime()
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return new Date(date).toLocaleDateString()
}

const modal = useModal()

async function handleDeleteNote(noteId: string) {
  const confirmed = await modal.confirm({
    title: '删除笔记',
    message: '确定要删除这条笔记吗？此操作不可恢复。',
    type: 'warning',
    confirmText: '删除',
    cancelText: '取消'
  })

  if (confirmed) {
    emit('delete-note', noteId)
  }
}

async function handleLogout() {
  showUserMenu.value = false

  const confirmed = await modal.confirm({
    title: '退出登录',
    message: '确定要退出登录吗？',
    type: 'warning',
    confirmText: '退出',
    cancelText: '取消'
  })

  if (confirmed) {
    try {
      await apiLogout()
      emit('logout')
    } catch (error) {
      console.error('[Sidebar] 退出登录失败:', error)
      emit('logout') // 即使API调用失败也执行登出
    }
  }
}

function openMemberCenter() {
  showUserMenu.value = false
  emit('open-member-center')
}

// 页面加载时获取用户信息
onMounted(() => {
  loadUserInfo()
  const handler = (e: Event) => {
    const detail = (e as CustomEvent).detail || {}
    userInfo.value = { ...(userInfo.value || {}), ...detail }
  }
  window.addEventListener('user-info-updated', handler as EventListener)
  onUnmounted(() => {
    window.removeEventListener('user-info-updated', handler as EventListener)
  })
})
</script>

<template>
  <div class="wcs">
    <!-- 品牌 -->
    <div class="wcs-brand">
      <div class="wcs-logo">N</div>
      <div class="wcs-brand-txt">
        <div class="wcs-brand-name">Note Manager</div>
        <div class="wcs-brand-sub">知识管理工作台</div>
      </div>
    </div>

    <!-- 用户卡 -->
    <div class="wcs-user-wrap">
      <button class="wcs-user" @click="showUserMenu = !showUserMenu">
        <span class="wcs-avatar">
          <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" alt="avatar" />
          <template v-else>{{ userInitial }}</template>
        </span>
        <span class="wcs-user-txt">
          <span class="wcs-user-name">{{ loadingUser ? '加载中…' : displayName }}</span>
          <span class="wcs-user-sub">{{ currentMemberStatusText }}</span>
        </span>
        <span v-if="isMemberOpened" class="wcs-crown" :title="memberTooltipLine1 + (memberTooltipLine2 ? '；' + memberTooltipLine2 : '')">
          <SIcon name="crown" :size="13" />
        </span>
        <SIcon name="chevron-down" :size="13" class="wcs-user-arrow" :class="{ 'is-up': showUserMenu }" />
      </button>

      <!-- 用户菜单 -->
      <transition name="wcs-fade">
        <div v-if="showUserMenu" class="wcs-menu">
          <button class="wcs-menu-item" @click="openMemberCenter">
            <SIcon name="crown" :size="14" class="wcs-menu-ic is-gold" />
            会员中心
          </button>
          <button class="wcs-menu-item" @click="showUserMenu = false; emit('navigate', 'settings')">
            <SIcon name="settings" :size="14" class="wcs-menu-ic" />
            个人设置
          </button>
          <div class="wcs-menu-line" />
          <button class="wcs-menu-item is-danger" @click="handleLogout">
            <SIcon name="logout" :size="14" class="wcs-menu-ic" />
            退出登录
          </button>
        </div>
      </transition>
    </div>

    <!-- 新建笔记 -->
    <div class="wcs-cta">
      <button class="wcs-new" @click="emit('create-note')">
        <SIcon name="plus" :size="14" :thickness="2" />
        <span>新建笔记</span>
        <kbd>⌘N</kbd>
      </button>
    </div>

    <!-- 滚动区 -->
    <div class="wcs-scroll">
      <!-- 浏览 -->
      <div class="wcs-group">
        <div class="wcs-group-title">浏览</div>
        <button
          v-for="item in navBrowse"
          :key="item.key"
          class="wcs-item"
          :class="{ 'is-on': currentPage === item.key }"
          @click="emit('navigate', item.key)"
        >
          <SIcon :name="item.icon" :size="15" class="wcs-ic" />
          <span class="wcs-label">{{ item.label }}</span>
        </button>
      </div>

      <!-- 笔记本 -->
      <div class="wcs-group">
        <div class="wcs-group-title">
          <button class="wcs-group-toggle" @click="toggleSection('notebooks')">
            笔记本
            <SIcon
              name="chevron-down"
              :size="12"
              class="wcs-group-chev"
              :class="{ 'is-fold': !expandedSections.includes('notebooks') }"
            />
          </button>
          <button class="wcs-group-add" title="新建笔记本" @click="emit('create-notebook')">
            <SIcon name="plus" :size="12" :thickness="2" />
          </button>
        </div>

        <div v-if="expandedSections.includes('notebooks')">
          <!-- 搜索 -->
          <div class="wcs-search">
            <SIcon name="search" :size="12" class="wcs-search-ic" />
            <input v-model="searchKeyword" type="text" placeholder="搜索笔记…" />
            <button v-if="searchKeyword" class="wcs-search-x" @click="searchKeyword = ''">
              <SIcon name="close" :size="11" />
            </button>
          </div>

          <!-- 标签筛选 -->
          <div v-if="allTags.length > 0" class="wcs-tag-filter">
            <button
              class="wcs-tag-head"
              :aria-expanded="tagSectionExpanded"
              @click="tagSectionExpanded = !tagSectionExpanded"
            >
              <span>标签筛选</span>
              <span class="wcs-tag-total">{{ allTags.length }}</span>
              <SIcon
                name="chevron-down"
                :size="12"
                class="wcs-tag-chevron"
                :class="{ 'is-fold': !tagSectionExpanded }"
              />
            </button>
            <div v-if="selectedTag" class="wcs-tag-selection">
              <span><strong>{{ selectedTag }}</strong><span> · {{ selectedTagNoteCount }} 篇笔记</span></span>
              <button aria-label="清除标签筛选" @click="selectTag(selectedTag)">清除</button>
            </div>
            <div v-if="tagSectionExpanded" class="wcs-tag-options">
              <button
                v-for="tag in visibleTags"
                :key="tag.name"
                class="wcs-tag-option"
                :class="{ 'is-on': selectedTag === tag.name }"
                @click="selectTag(tag.name)"
              >
                <span>{{ tag.name }}</span>
                <span class="wcs-tag-count">{{ tag.count }}</span>
              </button>
              <button
                v-if="allTags.length > 6"
                class="wcs-tag-more"
                @click="showAllTags = !showAllTags"
              >
                {{ showAllTags ? '收起' : `查看全部 ${allTags.length} 个` }}
              </button>
            </div>
          </div>

          <!-- 全部笔记 -->
          <button
            class="wcs-item wcs-nb"
            :class="{ 'is-on': isNotesView && selectedFilter === '全部' && !expandedNotebooks.includes('全部') }"
            @click="toggleNotebook('全部')"
          >
            <SIcon
              :name="expandedNotebooks.includes('全部') ? 'chevron-down' : 'chevron-right'"
              :size="11"
              class="wcs-caret"
            />
            <SIcon name="layers" :size="15" class="wcs-ic" />
            <span class="wcs-label">全部笔记</span>
            <span class="wcs-count">{{ getFilteredNotes(getAllNotes()).length }}</span>
          </button>

          <!-- 全部笔记展开的列表 -->
          <transition name="wcs-expand">
            <div v-if="expandedNotebooks.includes('全部')" class="wcs-notes">
              <div v-if="getFilteredNotes(getAllNotes()).length === 0" class="wcs-empty-row">
                {{ searchKeyword || selectedTag ? '无匹配笔记' : '暂无笔记' }}
              </div>
              <div
                v-for="note in getFilteredNotes(getAllNotes())"
                :key="'all-' + note.id"
                class="wcs-note"
                :class="{
                  'is-on': selectedNoteId === note.id,
                  'is-over': dragOverId === note.id,
                  'is-drag': draggingId === note.id
                }"
                :draggable="!note.isPinned"
                @dragstart="!note.isPinned && onDragStart($event, note.id)"
                @dragover="!note.isPinned && onDragOver($event, note.id)"
                @dragleave="onDragLeave"
                @drop="!note.isPinned && onDrop($event, note.id, getAllNotes())"
                @dragend="onDragEnd"
                @click="emit('select-note', note)"
              >
                <SIcon v-if="!note.isPinned" name="grip" :size="11" class="wcs-note-grip" @click.stop />
                <span class="wcs-note-main">
                  <span class="wcs-note-title">
                    <SIcon v-if="note.isPinned" name="pin" :size="9" class="wcs-note-pin" />
                    {{ note.title || '无标题' }}
                  </span>
                  <span class="wcs-note-time">{{ formatTime(note.updatedAt) }}</span>
                </span>
                <span class="wcs-note-acts">
                  <button title="置顶" @click.stop="emit('toggle-pin', note.id)">
                    <SIcon name="pin" :size="11" />
                  </button>
                  <button title="导出" @click.stop="emit('export-note', note)">
                    <SIcon name="download" :size="11" />
                  </button>
                  <button title="删除" @click.stop="handleDeleteNote(note.id)">
                    <SIcon name="trash" :size="11" />
                  </button>
                </span>
              </div>
            </div>
          </transition>

          <!-- 各笔记本 -->
          <div v-for="notebook in displayNotebooks" :key="notebook.id">
            <button
              class="wcs-item wcs-nb"
              :class="{
                'is-on': isNotesView && selectedFilter === notebook.id && !expandedNotebooks.includes(notebook.id)
              }"
              @click="toggleNotebook(notebook.id)"
            >
              <SIcon
                :name="expandedNotebooks.includes(notebook.id) ? 'chevron-down' : 'chevron-right'"
                :size="11"
                class="wcs-caret"
              />
              <SIcon name="book" :size="15" class="wcs-ic" />
              <span class="wcs-label">{{ notebook.name }}</span>
              <span class="wcs-count">{{ getFilteredNotes(getNotebookNotes(notebook.id)).length }}</span>
              <span
                class="wcs-nb-del"
                title="删除笔记本"
                role="button"
                tabindex="0"
                @click.stop="emit('delete-notebook', notebook.id)"
              >
                <SIcon name="trash" :size="11" />
              </span>
            </button>

            <transition name="wcs-expand">
              <div v-if="expandedNotebooks.includes(notebook.id)" class="wcs-notes">
                <div v-if="getFilteredNotes(getNotebookNotes(notebook.id)).length === 0" class="wcs-empty-row">
                  {{ searchKeyword || selectedTag ? '无匹配笔记' : '暂无笔记' }}
                </div>
                <div
                  v-for="note in getFilteredNotes(getNotebookNotes(notebook.id))"
                  :key="'notebook-' + notebook.id + '-' + note.id"
                  class="wcs-note"
                  :class="{
                    'is-on': selectedNoteId === note.id,
                    'is-over': dragOverId === note.id,
                    'is-drag': draggingId === note.id
                  }"
                  :draggable="!note.isPinned"
                  @dragstart="!note.isPinned && onDragStart($event, note.id)"
                  @dragover="!note.isPinned && onDragOver($event, note.id)"
                  @dragleave="onDragLeave"
                  @drop="!note.isPinned && onDrop($event, note.id, getNotebookNotes(notebook.id))"
                  @dragend="onDragEnd"
                  @click="emit('select-note', note)"
                >
                  <SIcon v-if="!note.isPinned" name="grip" :size="11" class="wcs-note-grip" @click.stop />
                  <span class="wcs-note-main">
                    <span class="wcs-note-title">
                      <SIcon v-if="note.isPinned" name="pin" :size="9" class="wcs-note-pin" />
                      {{ note.title || '无标题' }}
                    </span>
                    <span class="wcs-note-time">{{ formatTime(note.updatedAt) }}</span>
                  </span>
                  <span class="wcs-note-acts">
                    <button title="置顶" @click.stop="emit('toggle-pin', note.id)">
                      <SIcon name="pin" :size="11" />
                    </button>
                    <button title="导出" @click.stop="emit('export-note', note)">
                      <SIcon name="download" :size="11" />
                    </button>
                    <button title="删除" @click.stop="handleDeleteNote(note.id)">
                      <SIcon name="trash" :size="11" />
                    </button>
                  </span>
                </div>
              </div>
            </transition>
          </div>

          <button class="wcs-add-nb" @click="emit('create-notebook')">
            <SIcon name="plus" :size="12" />
            新建笔记本
          </button>
        </div>
      </div>

      <!-- 工具 -->
      <div class="wcs-group">
        <div class="wcs-group-title">工具</div>
        <button
          v-for="item in navTools"
          :key="item.key"
          class="wcs-item"
          :class="{ 'is-on': currentPage === item.key }"
          @click="emit('navigate', item.key)"
        >
          <SIcon :name="item.icon" :size="15" class="wcs-ic" />
          <span class="wcs-label">{{ item.label }}</span>
          <span v-if="item.badge" class="wcs-badge">{{ item.badge }}</span>
        </button>
      </div>
    </div>

    <!-- 底部 -->
    <div class="wcs-foot">
      <button class="wcs-foot-btn" @click="emit('navigate', 'settings')">
        <SIcon name="settings" :size="14" />
        设置
      </button>
      <button class="wcs-foot-btn" @click="handleLogout">
        <SIcon name="logout" :size="14" />
        退出
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ============ 侧栏外壳：深暖棕「书脊」 ============ */
.wcs {
  width: 236px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #241b14;
  color: #e9dcc7;
  border-right: 1px solid #16100b;
  font-size: 13px;
  position: relative;
  user-select: none;
}

/* 顶部一层暖光，避免整块死黑 */
.wcs::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 180px;
  background: radial-gradient(120% 80% at 20% 0%, rgba(178, 95, 34, 0.16), transparent 70%);
  pointer-events: none;
}

/* ============ 品牌 ============ */
.wcs-brand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 15px 14px 9px;
}

.wcs-logo {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #e08a45, #a8551e);
  color: #fff8ee;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(178, 95, 34, 0.45);
}

.wcs-brand-name {
  font-size: 13.5px;
  font-weight: 650;
  letter-spacing: 0.2px;
  color: #f4ead9;
  line-height: 1.2;
}

.wcs-brand-sub {
  margin-top: 1px;
  font-size: 10.5px;
  color: #8a7761;
  line-height: 1.2;
}

/* ============ 用户卡 ============ */
.wcs-user-wrap {
  position: relative;
  padding: 0 10px 9px;
}

.wcs-user {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.wcs-user:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(224, 138, 69, 0.28);
}

.wcs-avatar {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(145deg, #c26a26, #8e4a1b);
  color: #fff8ee;
  font-size: 11px;
  font-weight: 600;
}

.wcs-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.wcs-user-txt {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
}

.wcs-user-name {
  max-width: 100%;
  font-size: 12.5px;
  font-weight: 550;
  color: #f0e5d3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wcs-user-sub {
  font-size: 10px;
  color: #8a7761;
}

.wcs-crown {
  color: #d99a4e;
  display: grid;
  place-items: center;
}

.wcs-user-arrow {
  color: #6f5d49;
  transition: transform 0.18s;
}

.wcs-user-arrow.is-up {
  transform: rotate(180deg);
}

/* 用户下拉 */
.wcs-menu {
  position: absolute;
  left: 10px;
  right: 10px;
  top: calc(100% - 4px);
  z-index: 50;
  padding: 4px;
  border-radius: 10px;
  background: #30251d;
  border: 1px solid rgba(255, 255, 255, 0.09);
  box-shadow: 0 14px 30px -8px rgba(0, 0, 0, 0.65);
}

.wcs-menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 7px;
  font-size: 12px;
  color: #d9cbb6;
  cursor: pointer;
  background: transparent;
  border: none;
  text-align: left;
}

.wcs-menu-item:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #f4ead9;
}

.wcs-menu-item.is-danger:hover {
  background: rgba(190, 74, 54, 0.18);
  color: #f0a094;
}

.wcs-menu-ic {
  color: #8d7a64;
}

.wcs-menu-ic.is-gold {
  color: #d99a4e;
}

.wcs-menu-line {
  height: 1px;
  margin: 4px 6px;
  background: rgba(255, 255, 255, 0.08);
}

.wcs-fade-enter-active,
.wcs-fade-leave-active {
  transition: opacity 0.14s, transform 0.14s;
}

.wcs-fade-enter-from,
.wcs-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ============ 新建按钮 ============ */
.wcs-cta {
  padding: 0 10px 10px;
}

.wcs-new {
  width: 100%;
  height: 34px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 11px;
  border-radius: 9px;
  border: none;
  background: linear-gradient(180deg, #c26a26, #a8551e);
  color: #fff8ef;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px -3px rgba(178, 95, 34, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.16);
  transition: filter 0.15s, transform 0.15s;
}

.wcs-new:hover {
  filter: brightness(1.08);
  transform: translateY(-0.5px);
}

.wcs-new:active {
  transform: translateY(0);
}

.wcs-new kbd {
  margin-left: auto;
  font-family: inherit;
  font-size: 10px;
  letter-spacing: 0.3px;
  opacity: 0.55;
}

/* ============ 滚动区 ============ */
.wcs-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 2px 8px 10px;
}

.wcs-scroll::-webkit-scrollbar {
  width: 4px;
}

.wcs-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 4px;
}

.wcs-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* ============ 分组 ============ */
.wcs-group {
  margin-top: 4px;
}

.wcs-group + .wcs-group {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.wcs-group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 7px 5px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 1.2px;
  color: #7d6a55;
}

.wcs-group-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  font: inherit;
  color: inherit;
  letter-spacing: inherit;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.wcs-group-toggle:hover {
  color: #a08d76;
}

.wcs-group-chev {
  transition: transform 0.18s;
}

.wcs-group-chev.is-fold {
  transform: rotate(-90deg);
}

.wcs-group-add {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 5px;
  color: #7d6a55;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.14s, color 0.14s;
}

.wcs-group-add:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #e0a35f;
}

/* ============ 导航项 ============ */
.wcs-item {
  position: relative;
  width: 100%;
  height: 32px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 9px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: #bfae97;
  font-size: 12.5px;
  text-align: left;
  cursor: pointer;
  transition: background 0.14s, color 0.14s;
}

.wcs-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #efe3d0;
}

.wcs-item.is-on {
  background: rgba(178, 95, 34, 0.18);
  color: #f2c69b;
}

/* 选中项左侧色条，贴住侧栏外沿 */
.wcs-item.is-on::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 50%;
  width: 3px;
  height: 16px;
  transform: translateY(-50%);
  border-radius: 0 3px 3px 0;
  background: #e08a45;
}

.wcs-ic {
  color: #8d7a64;
  transition: color 0.14s;
}

.wcs-item:hover .wcs-ic {
  color: #c3a37c;
}

.wcs-item.is-on .wcs-ic {
  color: #e5a469;
}

.wcs-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wcs-count {
  font-size: 10.5px;
  color: #6f5d49;
  font-variant-numeric: tabular-nums;
}

.wcs-item.is-on .wcs-count {
  color: #c08f61;
}

.wcs-badge {
  padding: 0 5px;
  height: 15px;
  display: grid;
  place-items: center;
  border-radius: 4px;
  font-size: 9.5px;
  font-weight: 600;
  color: #ffd9b0;
  background: rgba(224, 138, 69, 0.28);
}

/* ============ 搜索 ============ */
.wcs-search {
  position: relative;
  margin: 2px 2px 6px;
}

.wcs-search-ic {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #6f5d49;
  pointer-events: none;
}

.wcs-search input {
  width: 100%;
  height: 28px;
  padding: 0 22px 0 25px;
  border-radius: 7px;
  background: rgba(0, 0, 0, 0.28);
  border: 1px solid rgba(255, 255, 255, 0.07);
  color: #e9dcc7;
  font-size: 12px;
  transition: border-color 0.15s, background 0.15s;
}

.wcs-search input::placeholder {
  color: #6f5d49;
}

.wcs-search input:focus {
  outline: none;
  background: rgba(0, 0, 0, 0.38);
  border-color: rgba(224, 138, 69, 0.5);
}

.wcs-search-x {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  display: grid;
  place-items: center;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: #6f5d49;
  cursor: pointer;
}

.wcs-search-x:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #d9cbb6;
}

/* ============ 标签筛选 ============ */
.wcs-tag-filter {
  margin: 5px 2px 7px;
  padding: 3px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.wcs-tag-head {
  width: 100%;
  min-height: 25px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 2px 4px;
  border: 0;
  background: transparent;
  color: #a08d76;
  font: inherit;
  font-size: 10.5px;
  text-align: left;
  cursor: pointer;
}

.wcs-tag-head:hover,
.wcs-tag-head[aria-expanded='true'] {
  color: #e4d6c2;
}

.wcs-tag-total {
  min-width: 17px;
  padding: 1px 5px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.07);
  color: #8e7c66;
  font-size: 9px;
  text-align: center;
}

.wcs-tag-chevron {
  margin-left: auto;
  transition: transform 0.16s;
}

.wcs-tag-chevron.is-fold {
  transform: rotate(-90deg);
}

.wcs-tag-selection {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin: 2px 2px 4px;
  padding: 5px 7px;
  border-radius: 5px;
  background: rgba(178, 95, 34, 0.16);
  color: #bba58a;
  font-size: 10px;
}

.wcs-tag-selection strong {
  color: #f0c18e;
  font-weight: 600;
}

.wcs-tag-selection button,
.wcs-tag-more {
  flex: none;
  padding: 2px 5px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #d09a67;
  font: inherit;
  font-size: 10px;
  cursor: pointer;
}

.wcs-tag-selection button:hover,
.wcs-tag-more:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #f0c18e;
}

.wcs-tag-options {
  display: grid;
  gap: 2px;
  padding: 2px 1px 3px;
}

.wcs-tag-option {
  width: 100%;
  min-height: 25px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 3px 7px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #b2a18c;
  font: inherit;
  font-size: 10.5px;
  text-align: left;
  cursor: pointer;
  transition: background 0.14s, color 0.14s;
}

.wcs-tag-option:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #eee1cf;
}

.wcs-tag-option.is-on {
  background: rgba(178, 95, 34, 0.22);
  color: #f1bd85;
}

.wcs-tag-count {
  color: #786853;
  font-size: 9px;
}

.wcs-tag-option.is-on .wcs-tag-count {
  color: #d8a979;
}

.wcs-tag-more {
  justify-self: start;
  min-height: 23px;
  margin-top: 1px;
}

/* ============ 笔记本行（多一个折叠箭头） ============ */
.wcs-nb {
  gap: 7px;
}

.wcs-caret {
  color: #5e4d3b;
  margin-left: -2px;
}

.wcs-nb:hover .wcs-caret {
  color: #a08d76;
}

.wcs-nb-del {
  display: none;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  color: #6f5d49;
}

.wcs-nb:hover .wcs-nb-del {
  display: grid;
}

.wcs-nb-del:hover {
  background: rgba(190, 74, 54, 0.2);
  color: #e88b7c;
}

/* ============ 笔记条目 ============ */
.wcs-notes {
  padding: 1px 0 2px 6px;
}

.wcs-note {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  padding: 5px 6px 5px 4px;
  margin-bottom: 1px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.13s;
}

.wcs-note:hover {
  background: rgba(255, 255, 255, 0.06);
}

.wcs-note.is-on {
  background: rgba(178, 95, 34, 0.2);
}

.wcs-note.is-over {
  box-shadow: inset 0 2px 0 #e08a45;
}

.wcs-note.is-drag {
  opacity: 0.4;
}

.wcs-note-grip {
  margin-top: 3px;
  color: #4f4032;
  cursor: grab;
  opacity: 0;
  transition: opacity 0.13s;
}

.wcs-note:hover .wcs-note-grip {
  opacity: 1;
}

.wcs-note-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.wcs-note-title {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 500;
  color: #cbbaa3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wcs-note.is-on .wcs-note-title {
  color: #f2c69b;
}

.wcs-note-pin {
  color: #d99a4e;
  flex-shrink: 0;
}

.wcs-note-time {
  font-size: 10px;
  color: #6b5a47;
}

.wcs-note-acts {
  display: none;
  align-items: center;
  gap: 1px;
  flex-shrink: 0;
}

.wcs-note:hover .wcs-note-acts {
  display: flex;
}

.wcs-note-acts button {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: #6f5d49;
  cursor: pointer;
}

.wcs-note-acts button:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e0a35f;
}

.wcs-note-acts button:last-child:hover {
  background: rgba(190, 74, 54, 0.2);
  color: #e88b7c;
}

.wcs-empty-row {
  padding: 4px 8px;
  font-size: 11px;
  color: #6b5a47;
}

.wcs-add-nb {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 9px;
  margin-top: 2px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: #9c8569;
  font-size: 11.5px;
  cursor: pointer;
  transition: background 0.14s, color 0.14s;
}

.wcs-add-nb:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #e0a35f;
}

/* ============ 底部 ============ */
.wcs-foot {
  display: flex;
  gap: 4px;
  padding: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.wcs-foot-btn {
  flex: 1;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: #9c8569;
  font-size: 11.5px;
  cursor: pointer;
  transition: background 0.14s, color 0.14s;
}

.wcs-foot-btn:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #e9dcc7;
}

/* 展开动画 */
.wcs-expand-enter-active,
.wcs-expand-leave-active {
  transition: opacity 0.15s;
}

.wcs-expand-enter-from,
.wcs-expand-leave-to {
  opacity: 0;
}

/* 矮屏（1366×768 这类笔记本分辨率）：整体压一档，避免工具组被挤出可视区 */
@media (max-height: 840px) {
  .wcs-brand {
    padding: 11px 14px 7px;
  }

  .wcs-user-wrap {
    padding-bottom: 7px;
  }

  .wcs-cta {
    padding-bottom: 8px;
  }

  .wcs-new {
    height: 32px;
  }

  .wcs-item {
    height: 29px;
  }

  .wcs-group-title {
    padding: 6px 7px 4px;
  }

  .wcs-group + .wcs-group {
    margin-top: 8px;
    padding-top: 8px;
  }

  .wcs-note {
    padding: 4px 6px 4px 4px;
  }

  .wcs-add-nb {
    padding: 5px 9px;
  }

  .wcs-foot {
    padding: 6px 8px;
  }
}
</style>
