<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import Button from '@/components/ui/Button.vue'
import {
  ArrowLeft, Star, Download, Eye, Clock, User, Share2, Heart,
  BookmarkPlus, MoreVertical, ThumbsUp, MessageSquare, ChevronRight, AlertCircle
} from 'lucide-vue-next'
import { getRelatedTemplates, getTemplateComments, likeTemplateComment, 
  shareTemplate, favoriteTemplate, unfavoriteTemplate, getFavoriteStatus,
  useTemplate, reportTemplate, createTemplateComment, unlikeTemplateComment, getTemplateById } from '@/api/template'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 类型定义
interface Template {
  id: string
  title: string
  description: string
  category: string
  thumbnail: string
  content?: string
  downloads: number
  rating: number
  views: number
  author: string
  updatedAt: Date
  tags: string[]
}

interface Comment {
  id: string
  author: string
  avatar: string
  content: string
  rating: number
  createdAt: Date
  likes: number
}

const props = defineProps<{
  template: Template
}>()

const emit = defineEmits<{
  'back': []
  'use-template': [template: Template]
}>()

// 状态
const router = useRouter()
const relatedTemplates = ref<Template[]>([])
const comments = ref<Comment[]>([])
const isFavorited = ref(false)
const activeTab = ref<'overview' | 'comments'>('overview')
const likedComments = ref<Set<string>>(new Set())
const loadingRelated = ref(false)
const loadingComments = ref(false)
const showShareDialog = ref(false)
const showReportDialog = ref(false)
const showCommentForm = ref(false)
const shareInfo = ref<any>(null)
const reportForm = ref({ reason: '', description: '' })
const commentForm = ref({ content: '', rating: 5 })

// 加载模板详情(触发浏览量增加)
async function loadTemplateDetail() {
  if (!props.template?.id) return
  try {
    const res = await getTemplateById(props.template.id)
    if (res && res.code === 0 && res.data) {
      // 更新模板的浏览次数和评分
      if (props.template.views !== undefined && res.data.views !== undefined) {
        props.template.views = res.data.views
      }
      if (props.template.rating !== undefined && res.data.rating !== undefined) {
        props.template.rating = res.data.rating
      }
    }
  } catch {
    // 忽略错误，不影响页面显示
  }
}

// 加载相关模板
async function loadRelatedTemplates() {
  if (!props.template?.id) return
  loadingRelated.value = true
  try {
    const res = await getRelatedTemplates(props.template.id, 3)
    if (res && res.code === 0) {
      relatedTemplates.value = (res.data || []).map((item: any) => ({
        ...item,
        updatedAt: item.updatedAt ? new Date(item.updatedAt) : new Date(),
        tags: item.tags || []
      }))
    }
  } finally {
    loadingRelated.value = false
  }
}

// 加载评论
async function loadComments() {
  if (!props.template?.id) return
  loadingComments.value = true
  try {
    const res = await getTemplateComments(props.template.id)
    if (res && res.code === 0) {
      comments.value = (res.data || []).map((item: any) => ({
        ...item,
        createdAt: item.createdAt ? new Date(item.createdAt) : new Date()
      }))
    }
  } finally {
    loadingComments.value = false
  }
}

// 点赞评论
async function handleLikeComment(commentId: string) {
  const newSet = new Set(likedComments.value)
  if (newSet.has(commentId)) {
    // 取消点赞
    try {
      await unlikeTemplateComment(commentId)
      newSet.delete(commentId)
      // 更新评论点赞数
      const comment = comments.value.find(c => c.id === commentId)
      if (comment) comment.likes--
      ElMessage.success('已取消点赞')
    } catch {
      ElMessage.error('取消点赞失败')
    }
  } else {
    // 点赞
    try {
      await likeTemplateComment(commentId)
      newSet.add(commentId)
      // 更新评论点赞数
      const comment = comments.value.find(c => c.id === commentId)
      if (comment) comment.likes++
      ElMessage.success('点赞成功')
    } catch {
      ElMessage.error('点赞失败')
    }
  }
  likedComments.value = newSet
}

// 立即使用模板
async function handleUseTemplate() {
  try {
    // 调用使用模板接口,增加下载计数
    await useTemplate(props.template.id)
    // 本地更新下载次数
    if (props.template.downloads !== undefined) {
      props.template.downloads++
    }
    ElMessage.success('开始使用模板')
    // 跳转到笔记页面,将模板ID作为参数传递
    router.push({
      path: '/',
      query: { templateId: props.template.id }
    })
  } catch {
    ElMessage.error('使用模板失败')
  }
}

// 分享模板
async function handleShare() {
  try {
    const res = await shareTemplate(props.template.id)
    if (res && res.code === 0) {
      shareInfo.value = res.data
      showShareDialog.value = true
    }
  } catch {
    ElMessage.error('获取分享信息失败')
  }
}

// 复制分享链接
function copyShareLink() {
  if (shareInfo.value && shareInfo.value.shareUrl) {
    // 使用Clipboard API复制链接
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(shareInfo.value.shareUrl)
        .then(() => {
          ElMessage.success('链接已复制到剪贴板')
        })
        .catch(() => {
          // 降级方案:手动选择
          ElMessage.warning('复制失败,请手动选择复制')
        })
    } else {
      // 老版本浏览器降级方案
      ElMessage.warning('请手动选择复制链接')
    }
  }
}

// 收藏/取消收藏
async function handleFavorite() {
  try {
    if (isFavorited.value) {
      await unfavoriteTemplate(props.template.id)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteTemplate(props.template.id)
      isFavorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

// 加载收藏状态
async function loadFavoriteStatus() {
  if (!props.template?.id) return
  try {
    const res = await getFavoriteStatus(props.template.id)
    if (res && res.code === 0) {
      isFavorited.value = res.data
    }
  } catch {
    // 静默失败
  }
}

// 显示举报对话框
function handleReport() {
  showReportDialog.value = true
  reportForm.value = { reason: '', description: '' }
}

// 提交举报
async function submitReport() {
  if (!reportForm.value.reason) {
    ElMessage.warning('请填写举报原因')
    return
  }
  try {
    await reportTemplate(props.template.id, reportForm.value)
    ElMessage.success('举报已提交')
    showReportDialog.value = false
  } catch {
    ElMessage.error('提交举报失败')
  }
}

// 显示评论表单
function handleShowCommentForm() {
  showCommentForm.value = true
  commentForm.value = { content: '', rating: 5 }
}

// 提交评论
async function submitComment() {
  if (!commentForm.value.content) {
    ElMessage.warning('请填写评论内容')
    return
  }
  try {
    await createTemplateComment(props.template.id, commentForm.value)
    ElMessage.success('评论发表成功')
    showCommentForm.value = false
    // 刷新评论列表
    await loadComments()
    // 重新加载模板详情以获取最新评分
    await loadTemplateDetail()
    // 自动切换到用户评价标签页
    activeTab.value = 'comments'
  } catch {
    ElMessage.error('发表评论失败')
  }
}

function formatDate(date: Date) {
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function getCommentLikes(comment: Comment) {
  return comment.likes
}

// 计算评分分布
const ratingDistribution = computed(() => {
  return [5, 4, 3, 2, 1].map(stars => {
    const count = comments.value.filter(c => c.rating === stars).length
    const percentage = comments.value.length > 0 ? (count / comments.value.length) * 100 : 0
    return { stars, count, percentage }
  })
})

// 渲染Markdown内容
const renderedContent = computed(() => {
  if (!props.template?.content) {
    return '<p class="text-gray-500">暂无详细介绍</p>'
  }
  const normalizedContent = props.template.content.replace(/\\r\\n|\\n/g, '\n')
  const rawHtml = marked(normalizedContent) as string
  return DOMPurify.sanitize(rawHtml)
})

// 处理相关模板点击
function handleRelatedTemplateClick(template: Template) {
  emit('back')  // 先返回列表页
  // 使用 nextTick 确保返回后再选择新模板
  setTimeout(() => {
    // 通知父组件选择新模板
    const event = new CustomEvent('select-template', { detail: template })
    window.dispatchEvent(event)
  }, 100)
}

// 监听模板变化，重新加载数据
watch(() => props.template, () => {
  if (props.template) {
    loadTemplateDetail()  // 触发浏览量增加
    loadRelatedTemplates()
    loadComments()
    loadFavoriteStatus()
  }
}, { immediate: true })

onMounted(() => {
  if (props.template) {
    loadRelatedTemplates()
    loadComments()
    loadFavoriteStatus()
  }
})
</script>

<template>
  <div class="h-full flex flex-col bg-gray-50 overflow-y-auto">
    <!-- Header -->
    <div class="bg-white border-b border-gray-200">
      <div class="w-full p-6">
        <div class="flex items-center gap-4 mb-6">
          <Button variant="ghost" size="sm" @click="emit('back')" class="flex items-center gap-2">
            <ArrowLeft class="w-4 h-4" />返回
          </Button>
          <div class="flex-1" />
          <Button variant="outline" size="sm" class="flex items-center gap-2" @click="handleShare">
            <Share2 class="w-4 h-4" />分享
          </Button>
        </div>

        <!-- Template info -->
        <div class="flex gap-8">
          <!-- Thumbnail -->
          <div class="w-80 flex-shrink-0">
            <div class="relative rounded-lg overflow-hidden border border-gray-200">
              <img :src="template.thumbnail" :alt="template.title" class="w-full h-64 object-cover" />
              <div class="absolute top-3 right-3">
                <span class="px-3 py-1.5 bg-white/90 backdrop-blur rounded-lg text-sm flex items-center gap-1.5 shadow-sm">
                  <Star class="w-4 h-4 text-yellow-500 fill-yellow-500" />
                  <span class="font-medium">{{ template.rating }}</span>
                </span>
              </div>
            </div>
          </div>

          <!-- Details -->
          <div class="flex-1 flex flex-col">
            <h1 class="text-2xl font-bold mb-3">{{ template.title }}</h1>
            <p class="text-gray-600 mb-4 leading-relaxed">{{ template.description }}</p>

            <!-- Stats -->
            <div class="flex items-center gap-6 mb-6">
              <div class="flex items-center gap-2 text-gray-600">
                <Download class="w-5 h-5" />
                <span class="text-sm"><span class="font-medium text-gray-900">{{ template.downloads }}</span> 次使用</span>
              </div>
              <div class="flex items-center gap-2 text-gray-600">
                <Eye class="w-5 h-5" />
                <span class="text-sm"><span class="font-medium text-gray-900">{{ template.views }}</span> 次浏览</span>
              </div>
              <div class="flex items-center gap-2 text-gray-600">
                <MessageSquare class="w-5 h-5" />
                <span class="text-sm"><span class="font-medium text-gray-900">{{ comments.length }}</span> 条评价</span>
              </div>
            </div>

            <!-- Tags -->
            <div class="flex flex-wrap gap-2 mb-6">
              <span v-for="(tag, index) in template.tags" :key="index" class="px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-sm">{{ tag }}</span>
            </div>

            <!-- Meta info -->
            <div class="flex items-center gap-6 text-sm text-gray-600 mb-6">
              <div class="flex items-center gap-2"><User class="w-4 h-4" /><span>作者：{{ template.author }}</span></div>
              <div class="flex items-center gap-2"><Clock class="w-4 h-4" /><span>更新于 {{ formatDate(template.updatedAt) }}</span></div>
            </div>

            <!-- Action buttons -->
            <div class="flex gap-3 mt-auto">
              <Button @click="handleUseTemplate" class="flex-1 bg-blue-600 hover:bg-blue-700 text-white">立即使用</Button>
              <Button variant="outline" class="flex-1" @click="handleFavorite">
                <BookmarkPlus class="w-4 h-4 mr-2" />
                {{ isFavorited ? '已收藏' : '添加到收藏夹' }}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="bg-white border-b border-gray-200">
      <div class="w-full px-6">
        <div class="flex gap-8">
          <button @click="activeTab = 'overview'" :class="['py-4 border-b-2 transition-colors', activeTab === 'overview' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-600 hover:text-gray-900']">模板介绍</button>
          <button @click="activeTab = 'comments'" :class="['py-4 border-b-2 transition-colors', activeTab === 'comments' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-600 hover:text-gray-900']">用户评价 ({{ comments.length }})</button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1">
      <div class="w-full p-6">
        <div class="flex gap-6">
          <!-- Main content -->
          <div class="flex-1">
            <!-- Overview Tab -->
            <div v-if="activeTab === 'overview'" class="bg-white rounded-lg border border-gray-200 p-6">
              <h2 class="text-lg font-semibold mb-4">模板详情</h2>
              <div class="prose prose-sm max-w-none" v-html="renderedContent"></div>
            </div>

            <!-- Comments Tab -->
            <div v-if="activeTab === 'comments'" class="space-y-4">
              <!-- Comment form -->
              <div class="bg-white rounded-lg border border-gray-200 p-6">
                <h3 class="text-sm font-medium mb-4">发表评价</h3>
                <div class="space-y-4">
                  <div>
                    <label class="text-sm text-gray-600 mb-2 block">评分</label>
                    <div class="flex items-center gap-2">
                      <Star v-for="i in 5" :key="i" 
                        @click="commentForm.rating = i"
                        :class="['w-6 h-6 cursor-pointer transition-colors', 
                          i <= commentForm.rating ? 'text-yellow-500 fill-yellow-500' : 'text-gray-300 hover:text-yellow-400']" />
                    </div>
                  </div>
                  <div>
                    <label class="text-sm text-gray-600 mb-2 block">评论内容</label>
                    <textarea v-model="commentForm.content" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      rows="4" placeholder="请分享您的使用体验..."></textarea>
                  </div>
                  <Button @click="submitComment" class="bg-blue-600 hover:bg-blue-700 text-white">提交评论</Button>
                </div>
              </div>

              <!-- Overall rating -->
              <div class="bg-white rounded-lg border border-gray-200 p-6">
                <div class="flex items-center gap-8">
                  <div class="text-center">
                    <div class="text-4xl font-bold text-gray-900 mb-2">{{ template.rating }}</div>
                    <div class="flex items-center gap-1 mb-1">
                      <Star v-for="i in 5" :key="i" :class="['w-4 h-4', i <= Math.floor(template.rating) ? 'text-yellow-500 fill-yellow-500' : 'text-gray-300']" />
                    </div>
                    <p class="text-sm text-gray-500">{{ comments.length }} 条评价</p>
                  </div>
                  <div class="flex-1">
                    <div class="space-y-2">
                      <div v-for="item in ratingDistribution" :key="item.stars" class="flex items-center gap-3">
                        <span class="text-sm text-gray-600 w-12">{{ item.stars }} 星</span>
                        <div class="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                          <div class="h-full bg-yellow-400" :style="{ width: `${item.percentage}%` }" />
                        </div>
                        <span class="text-sm text-gray-600 w-8">{{ item.count }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

                <div class="space-y-4">
                <div v-for="comment in comments" :key="comment.id" class="bg-white rounded-lg border border-gray-200 p-6">
                  <div class="flex items-start gap-4">
                    <img :src="comment.avatar" :alt="comment.author" class="w-10 h-10 rounded-full" />
                    <div class="flex-1">
                      <div class="flex items-center gap-3 mb-2">
                        <span class="font-medium text-gray-900">{{ comment.author }}</span>
                        <div class="flex items-center gap-1">
                          <Star v-for="i in 5" :key="i" :class="['w-4 h-4', i <= comment.rating ? 'text-yellow-500 fill-yellow-500' : 'text-gray-300']" />
                        </div>
                        <span class="text-xs text-gray-500">{{ formatDate(comment.createdAt) }}</span>
                      </div>
                      <p class="text-gray-700 leading-relaxed mb-3">{{ comment.content }}</p>
                      <div class="flex items-center gap-4">
                        <button @click="handleLikeComment(comment.id)"
                          :class="['flex items-center gap-1.5 text-sm transition-colors', likedComments.has(comment.id) ? 'text-blue-600' : 'text-gray-500 hover:text-blue-600']">
                          <ThumbsUp :class="['w-4 h-4', likedComments.has(comment.id) ? 'fill-blue-600' : '']" />
                          <span>{{ getCommentLikes(comment) }}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Sidebar -->
          <div class="w-80 flex-shrink-0">
            <div class="bg-white rounded-lg border border-gray-200 p-4 sticky top-6">
              <h3 class="text-sm font-medium mb-4">相关模板</h3>
              <div class="space-y-3">
                <div v-for="related in relatedTemplates" :key="related.id"
                  @click="handleRelatedTemplateClick(related)"
                  class="flex gap-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors border border-transparent hover:border-gray-200">
                  <img :src="related.thumbnail" :alt="related.title" class="w-16 h-16 rounded object-cover flex-shrink-0" />
                  <div class="flex-1 min-w-0">
                    <h4 class="text-sm text-gray-900 mb-1 truncate">{{ related.title }}</h4>
                    <p class="text-xs text-gray-500 line-clamp-2 mb-1">{{ related.description }}</p>
                    <div class="flex items-center gap-2 text-xs text-gray-500">
                      <span class="flex items-center gap-1"><Star class="w-3 h-3 text-yellow-500 fill-yellow-500" />{{ related.rating }}</span>
                      <span class="flex items-center gap-1"><Download class="w-3 h-3" />{{ related.downloads }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Share Dialog -->
    <el-dialog v-model="showShareDialog" title="分享模板" width="500px">
      <div v-if="shareInfo" class="space-y-4">
        <div>
          <label class="text-sm text-gray-600 mb-2 block">分享链接</label>
          <div class="flex gap-2">
            <input :value="shareInfo.shareUrl" readonly
              class="flex-1 px-3 py-2 border border-gray-300 rounded-lg bg-gray-50" />
            <Button @click="copyShareLink" variant="outline">复制</Button>
          </div>
        </div>
        <div class="text-center">
          <label class="text-sm text-gray-600 mb-2 block">扫描二维码</label>
          <img :src="shareInfo.qrCodeUrl" alt="二维码" class="mx-auto w-48 h-48" />
        </div>
      </div>
    </el-dialog>

    <!-- Report Dialog -->
    <el-dialog v-model="showReportDialog" title="举报模板" width="500px">
      <div class="space-y-4">
        <div>
          <label class="text-sm text-gray-600 mb-2 block">举报原因 <span class="text-red-500">*</span></label>
          <select v-model="reportForm.reason"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">请选择举报原因</option>
            <option value="内容违规">内容违规</option>
            <option value="侵权盗版">侵权盗版</option>
            <option value="虚假宣传">虚假宣传</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div>
          <label class="text-sm text-gray-600 mb-2 block">详细描述</label>
          <textarea v-model="reportForm.description"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            rows="4" placeholder="请描述具体问题..."></textarea>
        </div>
      </div>
      <template #footer>
        <Button @click="showReportDialog = false" variant="outline">取消</Button>
        <Button @click="submitReport" class="bg-blue-600 hover:bg-blue-700 text-white">提交</Button>
      </template>
    </el-dialog>
  </div>
</template>
