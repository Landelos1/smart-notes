<script setup lang="ts">
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
// @ts-ignore
import { uploadFile } from '@/api/apiClient.js'

interface Props {
  modelValue: string
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '点击开始编辑，支持 Markdown 格式'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function handleChange(value: string) {
  emit('update:modelValue', value)
}

/**
 * 处理图片上传
 * @param files - 上传的图片文件数组
 * @param callback - 回调函数,返回图片URL数组
 */
async function handleUploadImg(files: File[], callback: (urls: string[]) => void) {
  try {
    // 上传所有图片并获取URL
    const uploadPromises = files.map(async (file) => {
      try {
        // 验证文件类型
        if (!file.type.startsWith('image/')) {
          console.error('只能上传图片文件')
          return ''
        }
        
        // 验证文件大小 (5MB)
        if (file.size > 5 * 1024 * 1024) {
          console.error('图片大小不能超过 5MB')
          return ''
        }

        // 调用通用文件上传 API
        const response = await uploadFile('/api/common/file/upload', file, 'file')
        
        if (response && response.code === 0 && response.data?.fileUrl) {
          return response.data.fileUrl
        } else {
          console.error('图片上传失败:', response?.message || '未知错误')
          return ''
        }
      } catch (error) {
        console.error('图片上传异常:', error)
        return ''
      }
    })

    // 等待所有上传完成
    const urls = await Promise.all(uploadPromises)
    
    // 过滤掉失败的上传
    const validUrls = urls.filter(url => url !== '')
    
    // 调用回调函数，将URL插入编辑器
    callback(validUrls)
  } catch (error) {
    console.error('图片上传处理失败:', error)
    callback([])
  }
}
</script>

<template>
  <!-- wc-md：挂载暖色主题变量（见 assets/css/index.css），不挂则回落库的冷白默认 -->
  <div class="h-full wc-md">
    <MdEditor
      :model-value="modelValue"
      @update:model-value="handleChange"
      @on-upload-img="handleUploadImg"
      :placeholder="placeholder"
      language="zh-CN"
      :toolbars="[
        'bold',
        'underline',
        'italic',
        '-',
        'strikeThrough',
        'title',
        'sub',
        'sup',
        'quote',
        'unorderedList',
        'orderedList',
        'task',
        '-',
        'codeRow',
        'code',
        'link',
        'image',
        'table',
        'mermaid',
        'katex',
        '-',
        'revoke',
        'next',
        'save',
        '=',
        'pageFullscreen',
        'fullscreen',
        'preview',
        'htmlPreview',
        'catalog'
      ]"
      :preview-theme="'default'"
      :code-theme="'atom'"
      style="height: 100%;"
    />
  </div>
</template>

<style>
/* 确保编辑器占满容器 */
.md-editor {
  height: 100% !important;
}
</style>
