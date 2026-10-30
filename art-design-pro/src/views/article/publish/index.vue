<!-- 文章发布页面 -->
<template>
  <div>
    <div>
      <div class="max-w-250 mx-auto my-5">
        <!-- 文章标题、类型 -->
        <ElRow :gutter="10">
          <ElCol :span="18">
            <ElInput
              v-model.trim="articleName"
              placeholder="请输入文章标题（最多100个字符）"
              maxlength="100"
            />
          </ElCol>
          <ElCol :span="6">
            <ElSelect
              v-model="articleType"
              placeholder="请选择文章类型"
              filterable
              :disabled="pageMode === PageModeEnum.Edit"
            >
              <ElOption
                v-for="item in articleTypes"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </ElSelect>
          </ElCol>
        </ElRow>

        <!-- 富文本编辑器 -->
        <ArtWangEditor class="mt-2.5" v-model="editorHtml" />

        <div class="p-5 mt-5 art-card-xs">
          <h2 class="mb-5 text-xl font-medium">发布设置</h2>
          <!-- 图片上传 -->
          <ElForm>
            <ElFormItem label="封面">
              <div class="mt-2.5">
                <ElUpload
                  :action="uploadImageUrl"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="onSuccess"
                  :on-error="onError"
                  :before-upload="beforeUpload"
                >
                  <div
                    v-if="!cover"
                    class="flex-cc flex-col w-65 h-40 border border-dashed border-[#d9d9d9] rounded-md"
                  >
                    <ElIcon class="!text-xl !text-g-600"><Plus /></ElIcon>
                    <div class="mt-2 text-sm text-g-600">点击上传封面</div>
                  </div>
                  <img v-else :src="cover" class="block w-65 h-40 object-cover" />
                </ElUpload>
                <div class="mt-2 text-xs text-g-700">建议尺寸 16:9，jpg/png 格式</div>
              </div>
            </ElFormItem>
            <ElFormItem label="可见">
              <ElSwitch v-model="visible" />
            </ElFormItem>
          </ElForm>

          <div class="flex justify-end">
            <ElButton type="primary" @click="submit" class="w-25">
              {{ pageMode === PageModeEnum.Edit ? '保存' : '发布' }}
            </ElButton>
          </div>
        </div>
      </div>
    </div>

    <!-- <div class="box-border w-70 p-5 border border-[#e3e3e3] rounded-lg">
        <div v-for="(item, index) in outlineList" :key="index">
          <p :class="['h-7.5 text-xs leading-7.5 c-p', item.level === 3 && 'pl-2.5']">{{ item.text }}</p>
        </div>
      </div> -->
  </div>
</template>

<script setup lang="ts">
  import { Plus } from '@element-plus/icons-vue'
  import { useUserStore } from '@/store/modules/user'
  import EmojiText from '@/utils/ui/emojo'
  import { PageModeEnum } from '@/enums/formEnum'
  import { useCommon } from '@/hooks/core/useCommon'
  import { router } from '@/router'
  import { fetchCreateNote, fetchGetNoteDetail, fetchUpdateNote } from '@/api/note'
  import { fetchGetUserNotebooks } from '@/api/notebook'

  defineOptions({ name: 'ArticlePublish' })

  interface ArticleType {
    id: number
    name: string
  }

  interface UploadResponse {
    data: {
      fileUrl?: string
      url?: string
    }
    code?: number
    message?: string
    msg?: string
  }

  const MAX_IMAGE_SIZE = 2 // MB
  const EMPTY_EDITOR_CONTENT = '<p><br></p>'

  const route = useRoute()
  const userStore = useUserStore()
  const { accessToken } = userStore

  const apiBaseUrl = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')
  const uploadImageUrl = `${apiBaseUrl}/api/common/file/upload`
  const uploadHeaders = {
    satoken: accessToken,
    Authorization: accessToken
      ? accessToken.startsWith('Bearer ')
        ? accessToken
        : `Bearer ${accessToken}`
      : ''
  }

  const noteId = computed(() => {
    const q = route.query.id
    const v = Array.isArray(q) ? q[0] : q
    if (!v) return undefined
    const id = Number(v)
    return Number.isNaN(id) ? undefined : id
  })

  const pageMode = ref<PageModeEnum>(PageModeEnum.Add)
  const articleName = ref('')
  const articleType = ref<number>()
  const articleTypes = ref<ArticleType[]>([])
  const editorHtml = ref('')
  const createDate = ref('')
  const cover = ref('')
  const visible = ref(true)

  const currentNoteDetail = ref<Api.Note.NoteDetailVO | null>(null)

  /**
   * 初始化页面模式（新增或编辑）
   */
  const initPageMode = () => {
    pageMode.value = noteId.value ? PageModeEnum.Edit : PageModeEnum.Add

    if (pageMode.value === PageModeEnum.Edit) {
      getArticleDetail()
    } else {
      createDate.value = formatDate(useNow().value)
    }
  }

  /**
   * 获取文章分类列表
   */
  const getArticleTypes = async () => {
    try {
      const notebooks = await fetchGetUserNotebooks()
      articleTypes.value = notebooks.map((item) => ({
        id: item.id,
        name: item.name
      }))

      if (pageMode.value === PageModeEnum.Add && !articleType.value && articleTypes.value.length > 0) {
        articleType.value = articleTypes.value[0].id
      }
    } catch (error) {
      console.error('获取文章分类失败:', error)
      ElMessage.error('获取文章分类失败')
    }

    // TODO: 替换为真实 API 调用
    // const res = await ArticleService.getArticleTypes({})
    // if (res.code === ApiStatus.success) {
    //   articleTypes.value = res.data
    // }
  }

  /**
   * 获取文章详情（编辑模式）
   */
  const getArticleDetail = async () => {
    try {
      if (!noteId.value) return
      const data = await fetchGetNoteDetail(noteId.value)
      currentNoteDetail.value = data
      articleName.value = data.title
      articleType.value = data.notebookId
      editorHtml.value = data.contentMd || data.contentHtml || ''
      visible.value = data.status === 1
    } catch (error) {
      console.error('获取文章详情失败:', error)
      ElMessage.error('获取文章详情失败')
    }
  }

  /**
   * 格式化日期为 YYYY-MM-DD 格式
   */
  const formatDate = (date: string | Date): string => {
    return useDateFormat(date, 'YYYY-MM-DD').value
  }

  /**
   * 验证文章表单数据
   */
  const validateArticle = (): boolean => {
    if (!articleName.value.trim()) {
      ElMessage.error('请输入文章标题')
      return false
    }

    if (!articleType.value) {
      ElMessage.error('请选择文章类型')
      return false
    }

    if (!editorHtml.value || editorHtml.value === EMPTY_EDITOR_CONTENT) {
      ElMessage.error('请输入文章内容')
      return false
    }

    return true
  }

  /**
   * 清理代码块中的多余空格
   */
  const cleanCodeContent = (content: string): string => {
    return content.replace(/(\s*)<\/code>/g, '</code>')
  }

  /**
   * 新增文章
   */
  const addArticle = async () => {
    if (!validateArticle()) return

    try {
      const cleanedContent = cleanCodeContent(editorHtml.value)

      if (!articleType.value) return

      await fetchCreateNote({
        notebookId: articleType.value,
        title: articleName.value,
        contentMd: cleanedContent,
        status: visible.value ? 1 : 0,
        isPinned: false,
        tags: []
      })

      router.push({ name: 'ArticleList', query: { notebookId: articleType.value } })

      // TODO: 替换为真实 API 调用
      // const params = {
      //   title: articleName.value,
      //   type: articleType.value,
      //   content: cleanedContent,
      //   cover: cover.value,
      //   visible: visible.value
      // }
      // const res = await ArticleService.addArticle(params)
      // if (res.code === ApiStatus.success) {
      //   ElMessage.success('文章发布成功')
      //   router.push({ name: 'ArticleList' })
      // }

      console.log('新增文章:', { cleanedContent })
    } catch (error) {
      console.error('发布文章失败:', error)
      ElMessage.error('发布文章失败')
    }
  }

  /**
   * 编辑文章
   */
  const editArticle = async () => {
    if (!validateArticle()) return

    try {
      const cleanedContent = cleanCodeContent(editorHtml.value)

      if (!noteId.value || !articleType.value) return

      await fetchUpdateNote({
        noteId: noteId.value,
        title: articleName.value,
        contentMd: cleanedContent,
        status: visible.value ? 1 : 0,
        isPinned: currentNoteDetail.value?.isPinned ?? false,
        tags: currentNoteDetail.value?.tags ?? []
      })

      router.push({ name: 'ArticleList', query: { notebookId: articleType.value } })

      // TODO: 替换为真实 API 调用
      // const params = {
      //   id: route.query.id,
      //   title: articleName.value,
      //   type: articleType.value,
      //   content: cleanedContent,
      //   cover: cover.value,
      //   visible: visible.value
      // }
      // const res = await ArticleService.editArticle(params)
      // if (res.code === ApiStatus.success) {
      //   ElMessage.success('文章保存成功')
      //   router.push({ name: 'ArticleList' })
      // }

      console.log('编辑文章:', { cleanedContent })
    } catch (error) {
      console.error('保存文章失败:', error)
      ElMessage.error('保存文章失败')
    }
  }

  /**
   * 提交表单（新增或编辑）
   */
  const submit = () => {
    if (pageMode.value === PageModeEnum.Edit) {
      editArticle()
    } else {
      addArticle()
    }
  }

  /**
   * 图片上传成功回调
   */
  const onSuccess = (response: UploadResponse) => {
    if (typeof response?.code === 'number' && response.code !== 0) {
      ElMessage.error(response?.message || response?.msg || `图片上传失败 ${EmojiText[500]}`)
      return
    }

    const url = response?.data?.fileUrl || response?.data?.url
    if (!url) {
      ElMessage.error(`图片上传失败 ${EmojiText[500]}`)
      return
    }

    cover.value = url
    ElMessage.success(`图片上传成功 ${EmojiText[200]}`)
  }

  /**
   * 图片上传失败回调
   */
  const onError = () => {
    ElMessage.error(`图片上传失败 ${EmojiText[500]}`)
  }

  /**
   * 上传前的文件校验
   */
  const beforeUpload = (file: File): boolean => {
    const isImage = file.type.startsWith('image/')
    const isLt2M = file.size / 1024 / 1024 < MAX_IMAGE_SIZE

    if (!isImage) {
      ElMessage.error('只能上传图片文件')
      return false
    }

    if (!isLt2M) {
      ElMessage.error(`图片大小不能超过 ${MAX_IMAGE_SIZE}MB`)
      return false
    }

    return true
  }

  const { scrollToTop } = useCommon()

  onMounted(() => {
    scrollToTop()
    getArticleTypes()
    initPageMode()
  })
</script>
