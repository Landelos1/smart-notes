<template>
  <ElDialog
    v-model="dialogVisible"
    :title="dialogType === 'add' ? '发布模板' : '编辑模板'"
    width="55%"
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
  >
    <div class="max-h-150 overflow-y-auto">
      <div class="max-w-250 mx-auto">
        <ElRow :gutter="10">
          <ElCol :span="16">
            <ElInput v-model.trim="formData.title" placeholder="请输入模板标题（最多100个字符）" maxlength="100" />
          </ElCol>
          <ElCol :span="8">
            <ElSelect v-model="formData.category" placeholder="请选择模板分类" filterable>
              <ElOption label="工作汇报" value="work" />
              <ElOption label="学习笔记" value="study" />
              <ElOption label="会议记录" value="meeting" />
              <ElOption label="项目管理" value="project" />
            </ElSelect>
          </ElCol>
        </ElRow>

        <ElInput class="mt-2.5" v-model="formData.content" type="textarea" :rows="16" placeholder="输入模板内容，支持 Markdown 格式" />

        <div class="p-5 mt-5 art-card-xs">
          <h2 class="mb-5 text-xl font-medium">发布设置</h2>
          <ElForm>
            <ElFormItem label="缩略图">
              <div class="mt-2.5">
                <ElUpload
                  :action="uploadImageUrl"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="onUploadSuccess"
                  :on-error="onUploadError"
                  :before-upload="beforeUpload"
                >
                  <div
                    v-if="!formData.thumbnail"
                    class="flex-cc flex-col w-65 h-40 border border-dashed border-[#d9d9d9] rounded-md cursor-pointer"
                  >
                    <ElIcon class="!text-xl !text-g-600"><Plus /></ElIcon>
                    <div class="mt-2 text-sm text-g-600">点击上传缩略图</div>
                  </div>
                  <img v-else :src="formData.thumbnail" class="block w-65 h-40 object-cover cursor-pointer" />
                </ElUpload>
                <div class="mt-2 text-xs text-g-700">建议尺寸 16:9，jpg/png 格式</div>
              </div>
            </ElFormItem>

            <ElFormItem label="描述">
              <ElInput
                v-model="formData.description"
                type="textarea"
                :rows="3"
                placeholder="请输入模板描述"
                maxlength="500"
                show-word-limit
              />
            </ElFormItem>

            <ElFormItem label="标签">
              <ElInput
                v-model="tagsInput"
                placeholder="输入标签后回车添加，多个标签用逗号分隔"
                @keyup.enter="addTag"
              />
              <div class="mt-2">
                <ElTag
                  v-for="(tag, index) in (formData.tags || [])"
                  :key="index"
                  closable
                  @close="removeTag(index)"
                  class="mr-2 mb-2"
                >
                  {{ tag }}
                </ElTag>
              </div>
            </ElFormItem>

            <ElFormItem label="公开">
              <ElSwitch v-model="formData.isPublic" />
            </ElFormItem>
          </ElForm>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <ElButton @click="dialogVisible = false" :disabled="submitting">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit" :loading="submitting">
          {{ dialogType === 'add' ? '发布' : '保存' }}
        </ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
  import { Plus } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'
  import { useUserStore } from '@/store/modules/user'

  interface Props {
    visible: boolean
    type: 'add' | 'edit'
    templateData?: Partial<Api.Template.TemplateVO>
    submitting?: boolean
  }

  interface Emits {
    (e: 'update:visible', value: boolean): void
    (e: 'submit', payload: Api.Template.TemplateCreateDTO): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const userStore = useUserStore()

  const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
  })

  const dialogType = computed(() => props.type)

  const tagsInput = ref('')

  const apiBaseUrl = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')
  const uploadImageUrl = `${apiBaseUrl}/api/common/file/upload`

  const uploadHeaders = computed(() => {
    const token = userStore.accessToken
    const authorization = token && !token.startsWith('Bearer ') ? `Bearer ${token}` : token
    return {
      satoken: token,
      Authorization: authorization || ''
    }
  })

  const formData = reactive<Omit<Required<Api.Template.TemplateCreateDTO>, 'id'>>({
    title: '',
    description: '',
    category: 'work',
    thumbnail: '',
    content: '',
    tags: [],
    isPublic: true
  })

  const initFormData = () => {
    const isEdit = props.type === 'edit' && props.templateData
    const data = props.templateData

    Object.assign(formData, {
      title: isEdit && data ? data.title || '' : '',
      description: isEdit && data ? data.description || '' : '',
      category: isEdit && data ? data.category || 'work' : 'work',
      thumbnail: isEdit && data ? data.thumbnail || '' : '',
      content: isEdit && data ? data.content || '' : '',
      tags: isEdit && data ? [...(data.tags || [])] : [],
      isPublic: isEdit && data ? data.isPublic !== false : true
    })
  }

  watch(
    () => [props.visible, props.type, props.templateData],
    ([visible]) => {
      if (visible) {
        initFormData()
        tagsInput.value = ''
      }
    },
    { immediate: true }
  )

  const beforeUpload = (file: File) => {
    const isImage = file.type.startsWith('image/')
    const isLt5M = file.size / 1024 / 1024 < 5

    if (!isImage) {
      ElMessage.error('只能上传图片文件')
      return false
    }
    if (!isLt5M) {
      ElMessage.error('图片大小不能超过 5MB')
      return false
    }
    return true
  }

  const onUploadSuccess = (response: any) => {
    if (response?.code === 0 && response.data) {
      formData.thumbnail = response.data.fileUrl || response.data.filePath || ''
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(response?.message || response?.msg || '上传失败')
    }
  }

  const onUploadError = () => {
    ElMessage.error('上传失败，请重试')
  }

  const addTag = () => {
    const input = tagsInput.value.trim()
    if (!input) return

    const next = input
      .split(/[,，]/)
      .map((t) => t.trim())
      .filter((t) => t)

    formData.tags = Array.from(new Set([...(formData.tags || []), ...next]))
    tagsInput.value = ''
  }

  const removeTag = (index: number) => {
    if (!formData.tags) return
    formData.tags.splice(index, 1)
  }

  const handleSubmit = async () => {
    if (!formData.title) {
      ElMessage.warning('请输入模板标题')
      return
    }
    if (!formData.category) {
      ElMessage.warning('请选择模板分类')
      return
    }

    if (props.submitting) return
    addTag()
    emit('submit', {
      ...formData,
      tags: formData.tags || []
    })
  }
</script>
