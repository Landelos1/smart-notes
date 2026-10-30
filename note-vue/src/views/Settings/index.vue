<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import Button from '@/components/ui/Button.vue'
import Avatar from '@/components/ui/Avatar.vue'
import { User, Bot, Upload, Save, ArrowLeft, AlertCircle } from 'lucide-vue-next'
import type { UserProfile, AISettings } from '@/types/note'
import { getUserInfo, updateUserInfo } from '@/api/auth'
import { getAISettings, updateAISettings, validateAISettings, getAvailableModels, validateAIConnection } from '@/api/settings'
import { uploadFile } from '@/api/apiClient'

interface Props {
  onBack?: () => void
}

defineProps<Props>()

const emit = defineEmits<{
  'back': []
}>()

// 加载状态
const loadingProfile = ref(false)
const loadingAI = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 个人信息表单
const profileForm = ref<UserProfile>({
  username: '',
  phone: '',
  avatarUrl: '',
  gender: 0,
  birthDate: '',
})

const avatarUploading = ref(false)
const avatarInputRef = ref<HTMLInputElement | null>(null)
const avatarInitial = computed(() => {
  const name = profileForm.value.username || ''
  return name ? name.charAt(0) : '小'
})

// AI设置表单
const aiForm = ref<AISettings>({
  enablePersonalKb: true,
  personalKbMaxDocs: 5,
  chatStyle: 2,
  replyLanguage: 1,
  assistantName: '小智助手',
  chatApiKey: '',
  chatApiEndpoint: 'https://api.openai.com/v1/chat/completions',
  enableSharedKb: true,
  sharedKbMaxDocs: 5,
  customSystemPrompt: '',
  kbApiKey: '',
  kbApiEndpoint: 'https://api.openai.com/v1/chat/completions',
  useCustomKey: false,
  chatModel: '',
  kbUseCustomKey: false,
  kbChatModel: '',
})

const savingProfile = ref(false)
const savingAI = ref(false)
const testingChat = ref(false)
const testingKb = ref(false)
const loadingModels = ref(false)
const availableModels = ref<string[]>([])
const modelsError = ref('')
const loadingKbModels = ref(false)
const availableKbModels = ref<string[]>([])
const kbModelsError = ref('')

// 显示成功消息
function showSuccess(message: string) {
  successMessage.value = message
  errorMessage.value = ''
  setTimeout(() => {
    successMessage.value = ''
  }, 3000)
}

// 显示错误消息
function showError(message: string) {
  errorMessage.value = message
  successMessage.value = ''
  setTimeout(() => {
    errorMessage.value = ''
  }, 5000)
}

// 加载用户信息
async function loadUserProfile() {
  loadingProfile.value = true
  try {
    const r = await getUserInfo()
    if (r && r.code === 0 && r.data) {
      const user = r.data
      profileForm.value = {
        username: user.username || '',
        phone: user.phone || '',
        avatarUrl: user.avatarUrl || '',
        gender: user.gender || 0,
        birthDate: user.birthDate ? new Date(user.birthDate).toISOString().split('T')[0] : '',
      }
      console.log('[Settings] 用户信息加载成功')
    }
  } catch (error) {
    console.error('[Settings] 加载用户信息失败:', error)
  } finally {
    loadingProfile.value = false
  }
}

function openAvatarPicker() {
  avatarInputRef.value?.click()
}

function validatePhone(phone: string) {
  return /^1\d{10}$/.test(phone)
}

async function handleAvatarUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showError('请上传图片文件')
    input.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    showError('图片大小不能超过5MB')
    input.value = ''
    return
  }
  avatarUploading.value = true
  try {
    const r = await uploadFile('/api/common/file/upload', file, 'file')
    if (r && r.code === 0 && r.data?.fileUrl) {
      profileForm.value.avatarUrl = r.data.fileUrl
      const ur = await updateUserInfo({ avatarUrl: profileForm.value.avatarUrl })
      if (ur && ur.code === 0) {
        showSuccess('头像已更新')
        window.dispatchEvent(new CustomEvent('user-info-updated', { detail: { avatarUrl: profileForm.value.avatarUrl } }))
      } else {
        showError(ur?.message || '更新头像失败')
      }
    } else {
      showError(r?.message || '上传失败，请重试')
    }
  } catch (error) {
    console.error('[Settings] 头像上传失败:', error)
    showError('上传失败，请重试')
  } finally {
    avatarUploading.value = false
    input.value = ''
  }
}

// 加载AI设置
async function loadAISettings() {
  loadingAI.value = true
  try {
    const r = await getAISettings()
    if (r && r.code === 0 && r.data) {
      const settings = r.data
      aiForm.value = {
        enablePersonalKb: settings.enablePersonalKb ?? true,
        personalKbMaxDocs: settings.personalKbMaxDocs ?? 5,
        chatStyle: settings.chatStyle ?? 2,
        replyLanguage: settings.replyLanguage ?? 1,
        assistantName: settings.assistantName || '小智助手',
        chatApiKey: settings.chatApiKey || '',
        chatApiEndpoint: settings.chatApiEndpoint || 'https://api.openai.com/v1/chat/completions',
        enableSharedKb: settings.enableSharedKb ?? true,
        sharedKbMaxDocs: settings.sharedKbMaxDocs ?? 5,
        customSystemPrompt: settings.customSystemPrompt || '',
        kbApiKey: settings.kbApiKey || '',
        kbApiEndpoint: settings.kbApiEndpoint || 'https://api.openai.com/v1/chat/completions',
        useCustomKey: settings.useCustomKey ?? false,
        chatModel: settings.chatModel || '',
        kbUseCustomKey: settings.kbUseCustomKey ?? false,
        kbChatModel: settings.kbChatModel || '',
      }
      console.log('[Settings] AI设置加载成功')
    }
  } catch (error) {
    console.error('[Settings] 加载AI设置失败:', error)
  } finally {
    loadingAI.value = false
  }
}

// 保存个人信息
async function handleSaveProfile() {
  savingProfile.value = true
  try {
    if (profileForm.value.phone && !validatePhone(profileForm.value.phone)) {
      showError('请输入有效的11位手机号')
      savingProfile.value = false
      return
    }
    const userInfo = {
      username: profileForm.value.username,
      phone: profileForm.value.phone,
      gender: profileForm.value.gender,
      birthDate: profileForm.value.birthDate ? new Date(profileForm.value.birthDate) : null,
      avatarUrl: profileForm.value.avatarUrl,
    }
    const r = await updateUserInfo(userInfo)
    if (r && r.code === 0) {
      showSuccess('保存成功！')
      window.dispatchEvent(new CustomEvent('user-info-updated', { detail: userInfo }))
    } else {
      showError(r?.message || '保存失败，请重试')
    }
  } catch (error) {
    console.error('[Settings] 保存个人信息失败:', error)
    showError('保存失败，请重试')
  } finally {
    savingProfile.value = false
  }
}

// 保存AI设置
async function handleSaveAI() {
  // 前端验证
  const validation = validateAISettings(aiForm.value)
  if (!validation.valid) {
    showError(validation.errors.join('\n'))
    return
  }

  savingAI.value = true
  try {
    const r = await updateAISettings(aiForm.value)
    if (r && r.code === 0) {
      showSuccess('保存成功！')
    } else {
      showError(r?.message || '保存失败，请重试')
    }
  } catch (error) {
    console.error('[Settings] 保存AI设置失败:', error)
    showError('保存失败，请重试')
  } finally {
    savingAI.value = false
  }
}

async function testApiConnection(apiKey: string, apiEndpoint: string) {
  try {
    const r = await validateAIConnection(apiKey, apiEndpoint)
    if (r && r.code === 0) {
      return { ok: true, message: r?.data?.message || '连接成功' }
    }
    return { ok: false, message: r?.message || '连接失败' }
  } catch (error: any) {
    return { ok: false, message: error?.message || '网络错误' }
  }
}

async function testChatApiConnection() {
  testingChat.value = true
  try {
    const res = await testApiConnection(aiForm.value.chatApiKey, aiForm.value.chatApiEndpoint)
    if (res.ok) {
      window.alert('AI对话API连接成功：' + res.message)
    } else {
      window.alert('AI对话API连接失败：' + res.message)
    }
  } finally {
    testingChat.value = false
  }
}

async function fetchAvailableModels() {
  if (!aiForm.value.chatApiKey || !aiForm.value.chatApiEndpoint) {
    modelsError.value = '请先填写 API 密钥和 API 端点，并保存设置后再获取模型列表'
    return
  }
  loadingModels.value = true
  modelsError.value = ''
  availableModels.value = []
  try {
    const r = await getAvailableModels()
    if (r && r.code === 0 && Array.isArray(r.data)) {
      availableModels.value = r.data
      if (r.data.length === 0) {
        modelsError.value = '未获取到可用模型，请检查 API 密钥和端点是否正确'
      } else if (!aiForm.value.chatModel || !r.data.includes(aiForm.value.chatModel)) {
        aiForm.value.chatModel = r.data[0]
      }
    } else {
      modelsError.value = r?.message || '获取模型列表失败，请先保存 API 密钥和端点'
    }
  } catch (e) {
    modelsError.value = '获取模型列表异常，请稍后重试'
  } finally {
    loadingModels.value = false
  }
}

async function fetchKbAvailableModels() {
  if (!aiForm.value.kbApiKey || !aiForm.value.kbApiEndpoint) {
    kbModelsError.value = '请先填写知识库 API 密钥和 API 端点'
    return
  }
  loadingKbModels.value = true
  kbModelsError.value = ''
  availableKbModels.value = []
  try {
    const r = await validateAIConnection(aiForm.value.kbApiKey, aiForm.value.kbApiEndpoint)
    if (r && r.code === 0 && Array.isArray(r.data?.models)) {
      availableKbModels.value = r.data.models
      if (r.data.models.length === 0) {
        kbModelsError.value = '未获取到可用模型，请检查知识库 API 密钥和端点是否正确'
      } else if (!aiForm.value.kbChatModel || !r.data.models.includes(aiForm.value.kbChatModel)) {
        aiForm.value.kbChatModel = r.data.models[0]
      }
    } else {
      kbModelsError.value = r?.message || '获取知识库模型列表失败'
    }
  } catch (e) {
    kbModelsError.value = '获取知识库模型列表异常，请稍后重试'
  } finally {
    loadingKbModels.value = false
  }
}

async function testKbApiConnection() {
  testingKb.value = true
  try {
    const res = await testApiConnection(aiForm.value.kbApiKey, aiForm.value.kbApiEndpoint)
    if (res.ok) {
      window.alert('知识库API连接成功：' + res.message)
    } else {
      window.alert('知识库API连接失败：' + res.message)
    }
  } finally {
    testingKb.value = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadUserProfile()
  loadAISettings()
})
</script>

<template>
  <div class="h-full flex flex-col wc-page">
    <!-- Header -->
    <div class="pt-6 pb-1">
      <div class="max-w-3xl mx-auto px-8">
        <div class="flex items-center gap-2 mb-1">
          <Button
            v-if="onBack"
            variant="ghost"
            size="sm"
            @click="emit('back')"
            class="p-1.5 -ml-1.5"
          >
            <ArrowLeft class="w-4 h-4" />
          </Button>
          <h1 class="wc-page-title">个人设置</h1>
        </div>
        <p class="wc-page-sub">
          管理你的账号信息和 AI 助手行为
        </p>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto px-8 pb-10">
      <div class="max-w-3xl mx-auto space-y-4">
        
        <!-- 消息提示 -->
        <div v-if="successMessage" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-center gap-2">
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
          </svg>
          {{ successMessage }}
        </div>
        
        <div v-if="errorMessage" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
          <AlertCircle class="w-5 h-5" />
          {{ errorMessage }}
        </div>
        
        <!-- 个人信息卡片 -->
        <div class="wc-panel p-6">
          <div class="flex items-center gap-2 mb-6">
            <User class="w-5 h-5 text-gray-700" />
            <h2 class="text-lg">个人信息</h2>
          </div>

          <div class="space-y-6">
            <!-- 头像 -->
            <div class="flex items-center gap-4">
              <label class="text-sm text-gray-700 w-24">头像</label>
              <div class="flex items-center gap-4">
                <Avatar class="w-16 h-16">
                  <img
                    v-if="profileForm.avatarUrl"
                    :src="profileForm.avatarUrl"
                    alt="avatar"
                    class="w-full h-full object-cover rounded-full"
                  />
                  <div v-else class="w-full h-full bg-blue-500 rounded-full flex items-center justify-center text-white text-xl">
                    {{ avatarInitial }}
                  </div>
                </Avatar>
                <input
                  ref="avatarInputRef"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  @change="handleAvatarUpload"
                />
                <Button variant="outline" size="sm" :disabled="avatarUploading" @click="openAvatarPicker">
                  <Upload class="w-4 h-4 mr-2" />
                  {{ avatarUploading ? '上传中...' : '更换头像' }}
                </Button>
              </div>
            </div>

            <!-- 昵称 -->
            <div class="flex items-center gap-4">
              <label class="text-sm text-gray-700 w-24">昵称</label>
              <input
                type="text"
                v-model="profileForm.username"
                class="flex-1 max-w-md px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                placeholder="请输入昵称"
              />
            </div>

            <!-- 手机号 -->
            <div class="flex items-center gap-4">
              <label class="text-sm text-gray-700 w-24">手机号</label>
              <input
                type="tel"
                v-model="profileForm.phone"
                placeholder="请输入11位手机号"
                class="flex-1 max-w-md px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
              />
            </div>

            <!-- 性别 -->
            <div class="flex items-center gap-4">
              <label class="text-sm text-gray-700 w-24">性别</label>
              <div class="flex gap-4">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="gender"
                    :checked="profileForm.gender === 1"
                    @change="profileForm.gender = 1"
                    class="w-4 h-4 text-blue-600"
                  />
                  <span class="text-sm">男</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="gender"
                    :checked="profileForm.gender === 2"
                    @change="profileForm.gender = 2"
                    class="w-4 h-4 text-blue-600"
                  />
                  <span class="text-sm">女</span>
                </label>
              </div>
            </div>

            <!-- 生日 -->
            <div class="flex items-center gap-4">
              <label class="text-sm text-gray-700 w-24">生日</label>
              <input
                type="date"
                v-model="profileForm.birthDate"
                class="flex-1 max-w-md px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
              />
            </div>

            <!-- 保存按钮 -->
            <div class="flex justify-end pt-4 border-t border-gray-200">
              <Button
                @click="handleSaveProfile"
                :disabled="savingProfile"
                class="bg-blue-600 hover:bg-blue-700 text-white"
              >
                <Save class="w-4 h-4 mr-2" />
                {{ savingProfile ? '保存中...' : '保存个人信息' }}
              </Button>
            </div>
          </div>
        </div>

        <!-- AI助手设置卡片 -->
        <div class="wc-panel p-6">
          <div class="flex items-center gap-2 mb-6">
            <Bot class="w-5 h-5 text-gray-700" />
            <h2 class="text-lg">AI 助手设置</h2>
          </div>

          <div class="space-y-8">
            <!-- AI对话助手设置 -->
            <div>
              <h3 class="text-sm text-gray-900 mb-4 pb-2 border-b border-gray-200">
                AI 对话助手设置
                <span class="text-xs text-gray-500 ml-2">（作用于 AI对话 页面）</span>
              </h3>
              
              <div class="space-y-4">
                <!-- 启用个人知识库增强 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">启用个人知识库增强</label>
                    <p class="text-xs text-gray-500 mt-1">开启后，AI将结合你的个人知识库内容回答问题</p>
                  </div>
                  <label class="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      v-model="aiForm.enablePersonalKb"
                      class="sr-only peer"
                    />
                    <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </div>

                <!-- 个人知识库参考文档数 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">个人知识库参考文档数</label>
                    <p class="text-xs text-gray-500 mt-1">每次从知识库中检索的文档段落数量</p>
                  </div>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    v-model.number="aiForm.personalKbMaxDocs"
                    class="w-20 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                  />
                </div>

                <!-- 回答风格 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">回答风格</label>
                    <p class="text-xs text-gray-500 mt-1">控制AI回答的详细程度</p>
                  </div>
                  <select
                    v-model.number="aiForm.chatStyle"
                    class="w-40 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                  >
                    <option :value="1">简洁</option>
                    <option :value="2">适中</option>
                    <option :value="3">详细</option>
                  </select>
                </div>

                <!-- 默认回答语言 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">默认回答语言</label>
                    <p class="text-xs text-gray-500 mt-1">设置AI回答使用的语言</p>
                  </div>
                  <select
                    v-model.number="aiForm.replyLanguage"
                    class="w-40 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                  >
                    <option :value="1">跟随问题</option>
                    <option :value="2">只用中文</option>
                    <option :value="3">只用英文</option>
                  </select>
                </div>

                <!-- 助手昵称 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">助手昵称</label>
                    <p class="text-xs text-gray-500 mt-1">为你的AI助手设置一个昵称</p>
                  </div>
                  <input
                    type="text"
                    v-model="aiForm.assistantName"
                    class="w-40 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                    placeholder="例如：小智助手"
                  />
                </div>

                <!-- API密钥 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">API密钥</label>
                    <p class="text-xs text-gray-500 mt-1">设置用于AI对话的API密钥</p>
                  </div>
                  <input
                    type="text"
                    v-model="aiForm.chatApiKey"
                    class="w-60 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                    placeholder="例如：sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                  />
                </div>

                <!-- API端点 -->
                <div class="flex items-start justify-between">
                  <div>
                    <label class="text-sm text-gray-700">API端点</label>
                    <p class="text-xs text-gray-500 mt-1">设置用于AI对话的API端点</p>
                  </div>
                  <div class="w-80">
                    <input
                      type="text"
                      v-model="aiForm.chatApiEndpoint"
                      class="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                      placeholder="例如：https://api.openai.com/v1/chat/completions"
                    />
                    <div class="mt-2">
                      <Button variant="outline" size="sm" :disabled="testingChat" @click="testChatApiConnection">
                        {{ testingChat ? '测试中...' : '测试连接' }}
                      </Button>
                    </div>
                  </div>
                </div>

                <!-- 使用自定义 API Key 开关 -->
                <div class="flex items-center justify-between pt-2 border-t border-gray-100">
                  <div>
                    <label class="text-sm font-medium text-gray-800">使用自定义 API Key 对话</label>
                    <p class="text-xs text-gray-500 mt-1">开启后，AI 对话将使用上方你填写的 API 密钥和端点，而非系统默认模型</p>
                  </div>
                  <label class="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      v-model="aiForm.useCustomKey"
                      class="sr-only peer"
                    />
                    <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </div>

                <!-- 模型选择（useCustomKey 开启后显示） -->
                <div v-if="aiForm.useCustomKey" class="space-y-3 p-4 bg-blue-50 rounded-lg border border-blue-100">
                  <div class="flex items-center justify-between">
                    <div>
                      <label class="text-sm text-gray-700">选择模型</label>
                      <p class="text-xs text-gray-500 mt-1">从你的 API 端点获取可用模型，然后选择使用哪个</p>
                    </div>
                    <Button variant="outline" size="sm" :disabled="loadingModels" @click="fetchAvailableModels">
                      {{ loadingModels ? '获取中...' : '获取模型列表' }}
                    </Button>
                  </div>

                  <!-- 错误提示 -->
                  <p v-if="modelsError" class="text-xs text-red-500">{{ modelsError }}</p>

                  <!-- 模型下拉 -->
                  <div v-if="availableModels.length > 0" class="flex items-center gap-3">
                    <select
                      v-model="aiForm.chatModel"
                      class="flex-1 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                    >
                      <option value="">-- 请选择模型 --</option>
                      <option v-for="model in availableModels" :key="model" :value="model">
                        {{ model }}
                      </option>
                    </select>
                    <span class="text-xs text-gray-500 whitespace-nowrap">共 {{ availableModels.length }} 个</span>
                  </div>

                  <!-- 手动输入（无可用模型时） -->
                  <div v-else-if="!loadingModels" class="flex items-center gap-2">
                    <input
                      type="text"
                      v-model="aiForm.chatModel"
                      class="flex-1 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                      placeholder="或手动输入模型名，例如：gpt-4o"
                    />
                  </div>

                  <p v-if="aiForm.chatModel" class="text-xs text-blue-600">
                    当前将使用模型：<strong>{{ aiForm.chatModel }}</strong>
                  </p>
                </div>
              </div>
            </div>

            <!-- 知识库 AI 助手设置 -->
            <div>
              <h3 class="text-sm text-gray-900 mb-4 pb-2 border-b border-gray-200">
                知识库 AI 助手设置
                <span class="text-xs text-gray-500 ml-2">（作用于共享知识库页面）</span>
              </h3>
              
              <div class="space-y-4">
                <!-- 启用共享知识库增强 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">启用共享知识库增强</label>
                    <p class="text-xs text-gray-500 mt-1">开启后，助手会结合共享知识库内容回答</p>
                  </div>
                  <label class="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      v-model="aiForm.enableSharedKb"
                      class="sr-only peer"
                    />
                    <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </div>

                <!-- 共享知识库参考文档数 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">共享知识库参考文档数</label>
                    <p class="text-xs text-gray-500 mt-1">每次从共享知识库检索的文档段落数量</p>
                  </div>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    v-model.number="aiForm.sharedKbMaxDocs"
                    class="w-20 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                  />
                </div>

                <!-- 自定义系统提示词 -->
                <div>
                  <label class="text-sm text-gray-700 block mb-2">自定义系统提示词</label>
                  <p class="text-xs text-gray-500 mb-2">自定义AI的系统提示词，留空则使用默认配置</p>
                  <textarea
                    v-model="aiForm.customSystemPrompt"
                    rows="4"
                    class="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition resize-none"
                    placeholder="例如：你是一个专业的技术顾问，请用专业且友好的方式回答问题..."
                  />
                </div>

                <!-- 知识库API密钥 -->
                <div class="flex items-center justify-between">
                  <div>
                    <label class="text-sm text-gray-700">知识库API密钥</label>
                    <p class="text-xs text-gray-500 mt-1">设置用于知识库的API密钥</p>
                  </div>
                  <input
                    type="text"
                    v-model="aiForm.kbApiKey"
                    class="w-60 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                    placeholder="例如：sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                  />
                </div>

                <!-- 知识库API端点 -->
                <div class="flex items-start justify-between">
                  <div>
                    <label class="text-sm text-gray-700">知识库API端点</label>
                    <p class="text-xs text-gray-500 mt-1">设置用于知识库的API端点</p>
                  </div>
                  <div class="w-80">
                    <input
                      type="text"
                      v-model="aiForm.kbApiEndpoint"
                      class="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                      placeholder="例如：https://api.openai.com/v1/chat/completions"
                    />
                    <div class="mt-2">
                      <Button variant="outline" size="sm" :disabled="testingKb" @click="testKbApiConnection">
                        {{ testingKb ? '测试中...' : '测试连接' }}
                      </Button>
                    </div>
                  </div>
                </div>

                <!-- 知识库使用自定义 API Key 开关 -->
                <div class="flex items-center justify-between pt-2 border-t border-gray-100">
                  <div>
                    <label class="text-sm font-medium text-gray-800">知识库 AI 使用自定义 API Key</label>
                    <p class="text-xs text-gray-500 mt-1">开启后，共享知识库页面中的 AI 助手将优先使用这里配置的知识库 API 密钥、端点和模型</p>
                  </div>
                  <label class="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      v-model="aiForm.kbUseCustomKey"
                      class="sr-only peer"
                    />
                    <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </div>

                <!-- 知识库模型选择 -->
                <div v-if="aiForm.kbUseCustomKey" class="space-y-3 p-4 bg-indigo-50 rounded-lg border border-indigo-100">
                  <div class="flex items-center justify-between">
                    <div>
                      <label class="text-sm text-gray-700">知识库模型</label>
                      <p class="text-xs text-gray-500 mt-1">从知识库 API 端点获取可用模型，然后选择知识库 AI 助手使用哪个</p>
                    </div>
                    <Button variant="outline" size="sm" :disabled="loadingKbModels" @click="fetchKbAvailableModels">
                      {{ loadingKbModels ? '获取中...' : '获取模型列表' }}
                    </Button>
                  </div>

                  <p v-if="kbModelsError" class="text-xs text-red-500">{{ kbModelsError }}</p>

                  <div v-if="availableKbModels.length > 0" class="flex items-center gap-3">
                    <select
                      v-model="aiForm.kbChatModel"
                      class="flex-1 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                    >
                      <option value="">-- 请选择模型 --</option>
                      <option v-for="model in availableKbModels" :key="model" :value="model">
                        {{ model }}
                      </option>
                    </select>
                    <span class="text-xs text-gray-500 whitespace-nowrap">共 {{ availableKbModels.length }} 个</span>
                  </div>

                  <div v-else-if="!loadingKbModels" class="flex items-center gap-2">
                    <input
                      type="text"
                      v-model="aiForm.kbChatModel"
                      class="flex-1 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:bg-white focus:border-blue-300 focus:ring-2 focus:ring-blue-100 transition"
                      placeholder="或手动输入知识库模型名，例如：qwen-max"
                    />
                  </div>

                  <p v-if="aiForm.kbChatModel" class="text-xs text-indigo-600">
                    当前知识库 AI 将使用模型：<strong>{{ aiForm.kbChatModel }}</strong>
                  </p>
                </div>
              </div>
            </div>

            <!-- 保存按钮 -->
            <div class="flex justify-end pt-4 border-t border-gray-200">
              <Button
                @click="handleSaveAI"
                :disabled="savingAI"
                class="bg-blue-600 hover:bg-blue-700 text-white"
              >
                <Save class="w-4 h-4 mr-2" />
                {{ savingAI ? '保存中...' : '保存 AI 设置' }}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
