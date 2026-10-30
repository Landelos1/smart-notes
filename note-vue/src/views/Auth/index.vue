<script setup>
import { ref, reactive, onMounted } from 'vue'
const emit = defineEmits(['login'])
import { useRouter } from 'vue-router'
import Button from '@/components/ui/Button.vue'
import { 
  Phone, Lock, User, Eye, EyeOff, ArrowRight,
  BookOpen, Sparkles, Shield, Zap
} from 'lucide-vue-next'
import { getCaptcha, login as apiLogin, register as apiRegister } from '@/api/auth'

const router = useRouter()

const ROLE_TYPE_USER = 1

const mode = ref('login')
const showPassword = ref(false)
const formData = reactive({
  phone: '',
  password: '',
  captcha: '',
  name: '',
  confirmPassword: '',
  rememberMe: false,
})
const errors = reactive({})
const captchaId = ref('')
const captchaImage = ref('')
const formError = ref('')

const features = [
  { icon: BookOpen, title: '智能知识管理', description: '统一管理您的文档、笔记和知识库' },
  { icon: Sparkles, title: 'AI智能助手', description: '基于知识库的智能问答与内容生成' },
  { icon: Zap, title: '高效协作', description: '团队共享知识，提升协作效率' },
  { icon: Shield, title: '安全可靠', description: '企业级数据加密，保障信息安全' },
]

function validatePhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone)
}

function handleChange(field, value) {
  formData[field] = value
  if (errors[field]) {
    delete errors[field]
  }
}

function validateForm() {
  Object.keys(errors).forEach(key => delete errors[key])
  formError.value = ''
  
  if (!formData.phone) {
    errors.phone = '请输入手机号'
  } else if (!validatePhone(formData.phone)) {
    errors.phone = '请输入有效的手机号'
  }

  if (!formData.password) {
    errors.password = '请输入密码'
  } else if (formData.password.length < 6) {
    errors.password = '密码长度至少为6位'
  }

  if (mode.value === 'register') {
    if (!formData.name) {
      errors.name = '请输入姓名'
    }
    if (!formData.confirmPassword) {
      errors.confirmPassword = '请确认密码'
    } else if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = '两次输入的密码不一致'
    }
  }
  if (!formData.captcha) {
    errors.captcha = '请输入验证码'
  }

  return Object.keys(errors).length === 0
}

function handleSubmit() {
  if (!validateForm()) return
  
  if (mode.value === 'login') {
    doLogin()
  } else {
    doRegister()
  }
}

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  Object.keys(errors).forEach(key => delete errors[key])
  Object.assign(formData, { phone: '', password: '', captcha: '', name: '', confirmPassword: '', rememberMe: false })
  loadCaptcha()
}

async function loadCaptcha() {
  try {
    const r = await getCaptcha()
    if (r && r.code === 0 && r.data) {
      captchaId.value = r.data.captchaId || ''
      captchaImage.value = r.data.imageBase64 ? `data:image/png;base64,${r.data.imageBase64}` : ''
    }
  } catch {}
}

async function doLogin() {
  try {
    const payload = {
      phone: formData.phone,
      password: formData.password,
      captcha: formData.captcha,
      captchaId: captchaId.value,
      roleType: ROLE_TYPE_USER,
    }
    const r = await apiLogin(payload)
    let token = ''
    if (r && typeof r === 'object' && 'code' in r) {
      if (r.code === 0) {
        token = r.data || ''
      }
    } else if (typeof r === 'string') {
      token = r
    }
    if (token) {
      try { localStorage.setItem('satoken', token) } catch {}
      emit('login', formData.phone, formData.password)
      router.push('/')
      return
    }
    formError.value = r && r.message ? r.message : '登录失败'
    if ((r && r.message && r.message.includes('验')) || !formData.captcha) {
      errors.captcha = r.message || '验证码错误'
    } else {
      errors.password = r && r.message ? r.message : '登录失败'
    }
    loadCaptcha()
  } catch {
    formError.value = '登录失败'
    loadCaptcha()
  }
}

async function doRegister() {
  try {
    const payload = {
      phone: formData.phone,
      password: formData.password,
      captcha: formData.captcha,
      captchaId: captchaId.value,
      roleType: ROLE_TYPE_USER,
      username: formData.name,
      avatarUrl: ''
    }
    const r = await apiRegister(payload)
    if (r && r.code === 0) {
      mode.value = 'login'
      Object.assign(formData, { password: '', confirmPassword: '' })
      loadCaptcha()
      return
    }
    errors.confirmPassword = r && r.message ? r.message : '注册失败'
    loadCaptcha()
  } catch {
    formError.value = '注册失败'
    loadCaptcha()
  }
}

onMounted(() => {
  loadCaptcha()
})
</script>

<template>
  <div class="min-h-screen flex bg-gray-50">
    <!-- 左侧 - 品牌展示 -->
    <div class="hidden lg:flex lg:w-1/2 relative overflow-hidden border-r border-gray-200 bg-gray-100 p-12 flex-col justify-between">
      <!-- 暖光装饰 -->
      <div class="pointer-events-none absolute inset-0">
        <div class="absolute -top-32 -left-24 h-[26rem] w-[26rem] rounded-full bg-blue-200/45 blur-3xl"></div>
        <div class="absolute -bottom-40 -right-24 h-[30rem] w-[30rem] rounded-full bg-amber-200/45 blur-3xl"></div>
        <div class="absolute top-[38%] right-10 h-44 w-44 rounded-full border border-gray-300/60"></div>
        <div class="absolute top-[38%] right-[4.5rem] h-44 w-44 rounded-full border border-gray-300/35"></div>
      </div>

      <div class="relative z-10">
        <!-- Logo -->
        <div class="flex items-center gap-3">
          <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-600 shadow-md shadow-blue-600/25">
            <BookOpen class="h-6 w-6 text-white" />
          </div>
          <span class="text-xl font-semibold tracking-tight text-gray-900">知识库管理系统</span>
        </div>

        <!-- 标语 -->
        <div class="mt-16 max-w-md">
          <div class="mb-5 h-px w-12 bg-blue-600"></div>
          <h1 class="font-serif text-4xl font-semibold leading-snug tracking-tight text-gray-900">
            构建您的<br />
            <span class="text-blue-600">智能知识中枢</span>
          </h1>
          <p class="mt-5 text-base leading-relaxed text-gray-600">
            整合文档、笔记与 AI，让零散的内容沉淀成可检索、可对话的知识资产。
          </p>
        </div>
      </div>

      <!-- 特性列表 -->
      <div class="relative z-10 grid grid-cols-2 gap-4">
        <div
          v-for="(feature, index) in features"
          :key="index"
          class="flex gap-3 rounded-2xl border border-gray-200/80 bg-white/70 p-4 backdrop-blur-sm"
        >
          <div class="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg bg-blue-50">
            <component :is="feature.icon" class="h-4 w-4 text-blue-600" />
          </div>
          <div>
            <h3 class="text-sm font-medium text-gray-900">{{ feature.title }}</h3>
            <p class="mt-1 text-xs leading-relaxed text-gray-600">{{ feature.description }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧 - 登录/注册表单 -->
    <div class="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-10 lg:p-12">
      <div class="w-full max-w-[27rem]">
        <!-- 移动端 Logo -->
        <div class="lg:hidden flex items-center justify-center gap-3 mb-8">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 shadow-md shadow-blue-600/25">
            <BookOpen class="h-5 w-5 text-white" />
          </div>
          <span class="text-lg font-semibold tracking-tight text-gray-900">知识库管理系统</span>
        </div>

        <!-- 表单卡片 -->
        <div class="bg-white rounded-3xl border border-gray-200 shadow-lg p-8 sm:p-10">
          <!-- 标题 -->
          <div class="mb-7">
            <h2 class="text-2xl font-semibold tracking-tight text-gray-900">
              {{ mode === 'login' ? '欢迎回来' : '创建账号' }}
            </h2>
            <p class="mt-2 text-sm text-gray-500">
              {{ mode === 'login' ? '登录以继续访问您的知识库' : '开始构建您的知识管理系统' }}
            </p>
          </div>

          <!-- 表单 -->
          <form @submit.prevent="handleSubmit" class="space-y-5">
            <div v-if="formError" class="rounded-xl border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700">
              {{ formError }}
            </div>
            <!-- 姓名 - 仅注册时显示 -->
            <div v-if="mode === 'register'">
              <label class="block text-sm font-medium text-gray-700 mb-2">姓名</label>
              <div class="relative">
                <User class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  :value="formData.name"
                  @input="handleChange('name', $event.target.value)"
                  placeholder="请输入您的姓名"
                  :class="[
                    'w-full pl-10 pr-4 py-3 border rounded-xl focus:outline-none focus:ring-2 transition-all text-base placeholder:text-gray-400 bg-gray-50/60 shadow-sm hover:border-gray-400 focus:bg-white focus:shadow-md',
                    errors.name 
                      ? 'border-red-300 focus:ring-red-500/25 focus:border-red-500' 
                      : 'border-gray-300 focus:ring-blue-500/25 focus:border-blue-500'
                  ]"
                />
              </div>
              <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
            </div>

            <!-- 手机号 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">手机号</label>
              <div class="relative">
                <Phone class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="tel"
                  :value="formData.phone"
                  @input="handleChange('phone', $event.target.value)"
                  placeholder="请输入手机号"
                  :class="[
                    'w-full pl-10 pr-4 py-3 border rounded-xl focus:outline-none focus:ring-2 transition-all text-base placeholder:text-gray-400 bg-gray-50/60 shadow-sm hover:border-gray-400 focus:bg-white focus:shadow-md',
                    errors.phone 
                      ? 'border-red-300 focus:ring-red-500/25 focus:border-red-500' 
                      : 'border-gray-300 focus:ring-blue-500/25 focus:border-blue-500'
                  ]"
                />
              </div>
              <p v-if="errors.phone" class="mt-1 text-sm text-red-600">{{ errors.phone }}</p>
            </div>

            <!-- 密码 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
              <div class="relative">
                <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  :type="showPassword ? 'text' : 'password'"
                  :value="formData.password"
                  @input="handleChange('password', $event.target.value)"
                  placeholder="请输入密码"
                  :class="[
                    'w-full pl-10 pr-12 py-3 border rounded-xl focus:outline-none focus:ring-2 transition-all text-base placeholder:text-gray-400 bg-gray-50/60 shadow-sm hover:border-gray-400 focus:bg-white focus:shadow-md',
                    errors.password 
                      ? 'border-red-300 focus:ring-red-500/25 focus:border-red-500' 
                      : 'border-gray-300 focus:ring-blue-500/25 focus:border-blue-500'
                  ]"
                />
                <button
                  type="button"
                  @click="showPassword = !showPassword"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  <EyeOff v-if="showPassword" class="w-5 h-5" />
                  <Eye v-else class="w-5 h-5" />
                </button>
              </div>
              <p v-if="errors.password" class="mt-1 text-sm text-red-600">{{ errors.password }}</p>
            </div>

            <!-- 确认密码 - 仅注册时显示 -->
            <div v-if="mode === 'register'">
              <label class="block text-sm font-medium text-gray-700 mb-2">确认密码</label>
              <div class="relative">
                <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  :type="showPassword ? 'text' : 'password'"
                  :value="formData.confirmPassword"
                  @input="handleChange('confirmPassword', $event.target.value)"
                  placeholder="请再次输入密码"
                  :class="[
                    'w-full pl-10 pr-4 py-3 border rounded-xl focus:outline-none focus:ring-2 transition-all text-base placeholder:text-gray-400 bg-gray-50/60 shadow-sm hover:border-gray-400 focus:bg-white focus:shadow-md',
                    errors.confirmPassword 
                      ? 'border-red-300 focus:ring-red-500/25 focus:border-red-500' 
                      : 'border-gray-300 focus:ring-blue-500/25 focus:border-blue-500'
                  ]"
                />
              </div>
              <p v-if="errors.confirmPassword" class="mt-1 text-sm text-red-600">{{ errors.confirmPassword }}</p>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">图形验证码</label>
              <div class="relative">
                  <input
                    type="text"
                    :value="formData.captcha"
                    @input="handleChange('captcha', $event.target.value)"
                    placeholder="请输入验证码"
                    :class="[
                      'w-full px-3 py-3 border rounded-xl focus:outline-none focus:ring-2 transition-all text-base placeholder:text-gray-400 bg-gray-50/60 shadow-sm hover:border-gray-400 focus:bg-white focus:shadow-md',
                      errors.captcha 
                        ? 'border-red-300 focus:ring-red-500/25 focus:border-red-500' 
                        : 'border-gray-300 focus:ring-blue-500/25 focus:border-blue-500'
                    ]"
                  />
                </div>
                <p v-if="errors.captcha" class="mt-1 text-sm text-red-600">{{ errors.captcha }}</p>
              </div>
              <div class="flex items-end">
                <img :src="captchaImage" alt="" class="h-11 rounded-lg border border-gray-300 mr-3 cursor-pointer select-none" @click="loadCaptcha" />
              </div>
            </div>

            <!-- 记住我 & 忘记密码 - 仅登录时显示 -->
            <div v-if="mode === 'login'" class="flex items-center justify-between">
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  :checked="formData.rememberMe"
                  @change="handleChange('rememberMe', $event.target.checked)"
                  class="w-4 h-4 rounded border-gray-300 text-blue-600 accent-blue-600 focus:ring-blue-500/30"
                />
                <span class="text-sm text-gray-600">记住我</span>
              </label>
              <button type="button" class="text-sm text-blue-600 hover:text-blue-700 font-medium">
                忘记密码？
              </button>
            </div>

            <!-- 服务条款 - 仅注册时显示 -->
            <div v-if="mode === 'register'" class="flex items-start gap-2">
              <input
                type="checkbox"
                id="terms"
                class="w-4 h-4 mt-0.5 rounded border-gray-300 text-blue-600 accent-blue-600 focus:ring-blue-500/30"
              />
              <label for="terms" class="text-sm text-gray-600">
                我已阅读并同意
                <button type="button" class="text-blue-600 hover:text-blue-700">服务条款</button>
                和
                <button type="button" class="text-blue-600 hover:text-blue-700">隐私政策</button>
              </label>
            </div>

            <!-- 提交按钮 -->
            <Button
              type="submit"
              class="w-full rounded-xl bg-blue-600 hover:bg-blue-700 text-white py-3 flex items-center justify-center gap-2 shadow-md shadow-blue-600/20"
            >
              {{ mode === 'login' ? '登录' : '注册' }}
              <ArrowRight class="w-5 h-5" />
            </Button>
          </form>

          <!-- 切换登录/注册 -->
          <div class="mt-6 text-center">
            <span class="text-gray-600">
              {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
            </span>
            <button
              type="button"
              @click="toggleMode"
              class="text-blue-600 hover:text-blue-700 font-medium"
            >
              {{ mode === 'login' ? '立即注册' : '立即登录' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
