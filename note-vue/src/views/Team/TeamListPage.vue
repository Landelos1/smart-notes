<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Avatar from '@/components/ui/Avatar.vue'
import Badge from '@/components/ui/Badge.vue'
import { useModal } from '@/composables/useModal'
import { Search, Plus, Users, ArrowRight, MoreHorizontal, Settings, LogOut, Copy } from 'lucide-vue-next'
import { getMyTeams, getDiscoverableTeams, createTeam, joinTeamByCode, resetInviteCode, dissolveTeam } from '@/api/team'

interface Team {
  id: string
  name: string
  avatarUrl?: string
  memberCount: number
  myRole?: number // 0=创建者 1=管理员 2=编辑者 3=查看者
  lastActiveTime: string
  description: string
  inviteCode?: string
}

const emit = defineEmits<{
  'navigate-to-team': [teamId: string]
  'navigate-to-team-settings': [teamId: string]
}>()

const modal = useModal()

const teams = ref<Team[]>([])
const discoverableTeams = ref<Team[]>([])
const loading = ref(false)
const searchQuery = ref('')

const openMenuTeamId = ref<string | null>(null)

const filteredTeams = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return teams.value
  return teams.value.filter(t => t.name.toLowerCase().includes(q))
})

const filteredDiscoverableTeams = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return discoverableTeams.value
  return discoverableTeams.value.filter(t => t.name.toLowerCase().includes(q))
})

// 加载团队列表
async function loadTeams() {
  loading.value = true
  try {
    const res = await getMyTeams()
    if (res.code === 0 && res.data) {
      teams.value = res.data
    }
    const discoverRes = await getDiscoverableTeams()
    if (discoverRes.code === 0 && discoverRes.data) {
      discoverableTeams.value = discoverRes.data
    }
  } catch (e) {
    console.error('加载团队列表失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTeams()
})

async function handleCreateTeam() {
  const name = await modal.prompt({
    title: '创建新团队',
    message: '创建一个新的协作空间，您可以稍后邀请成员。',
    placeholder: '团队名称 (e.g. 研发中心)'
  })

  if (!name || !name.trim()) return

  loading.value = true
  try {
    const res = await createTeam({ name: name.trim() })
    if (res.code === 0 && res.data) {
      teams.value = [res.data, ...teams.value]
      await modal.success('团队创建成功！')
    } else {
      await modal.error(res.message || '创建失败')
    }
  } catch (e) {
    await modal.error('创建失败，请重试')
  } finally {
    loading.value = false
  }
}

async function handleJoinTeam() {
  const code = await modal.prompt({
    title: '加入团队',
    message: '输入管理员分享的邀请码加入现有团队',
    placeholder: '请输入邀请码...'
  })

  if (!code || !code.trim()) return

  loading.value = true
  try {
    const res = await joinTeamByCode(code.trim())
    if (res.code === 0 && res.data) {
      teams.value = [res.data, ...teams.value]
      discoverableTeams.value = discoverableTeams.value.filter(t => t.id !== res.data.id)
      await modal.success('加入团队成功！')
    } else {
      await modal.error(res.message || '邀请码无效或已过期')
    }
  } catch (e) {
    await modal.error('加入失败，请重试')
  } finally {
    loading.value = false
  }
}

async function copyInviteCode(teamId: string) {
  const team = teams.value.find(t => t.id === teamId)
  const code = team?.inviteCode || ''
  if (!code) {
    await modal.error('邀请码不存在')
    return
  }
  try {
    await navigator.clipboard.writeText(code)
    await modal.success('邀请码已复制到剪贴板')
  } catch {
    await modal.info(`邀请码：${code}`)
  }
}

async function handleDissolveTeam(teamId: string) {
  const confirmed = await modal.confirm({
    title: '解散团队',
    message: '确定要解散此团队吗？此操作不可恢复。',
    type: 'warning'
  })
  if (!confirmed) return

  try {
    const res = await dissolveTeam(teamId)
    if (res.code === 0) {
      teams.value = teams.value.filter(t => t.id !== teamId)
      await modal.success('团队已解散')
    } else {
      await modal.error(res.message || '解散失败')
    }
  } catch (e) {
    await modal.error('解散失败，请重试')
  }
}

function roleText(myRole?: number) {
  if (myRole === null || myRole === undefined) return '公开'
  if (myRole === 0 || myRole === 1) return '管理员'
  if (myRole === 2) return '编辑者'
  return '成员'
}

function isAdmin(myRole?: number) {
  return myRole === 0 || myRole === 1
}

function formatLastActive(time: string) {
  if (!time) return '未知'
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
    return '未知'
  }
}

function toggleMenu(teamId: string) {
  openMenuTeamId.value = openMenuTeamId.value === teamId ? null : teamId
}

function closeMenu() {
  openMenuTeamId.value = null
}
</script>

<template>
  <div class="h-full overflow-y-auto wc-page px-6 md:px-8 pb-10" @click="closeMenu">
    <div class="max-w-6xl mx-auto space-y-7">
      <div class="pt-7 pb-1 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 class="wc-page-title">团队空间</h1>
          <p class="wc-page-sub">把知识沉淀到团队里，和成员一起维护同一份资料</p>
        </div>
        <div class="flex gap-2.5 shrink-0">
          <Button variant="outline" class="h-9" :disabled="loading" @click.stop="handleJoinTeam">输入邀请码加入</Button>
          <Button class="h-9 gap-2" :disabled="loading" @click.stop="handleCreateTeam">
            <Plus class="w-4 h-4" />
            创建团队
          </Button>
        </div>
      </div>

      <div class="relative max-w-md">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
        <Input
          placeholder="搜索团队名称…"
          class="pl-9 h-9 bg-gray-50 border-gray-200"
          v-model="searchQuery"
        />
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        <template v-if="loading && teams.length === 0">
          <div v-for="i in 3" :key="i" class="space-y-3">
            <div class="h-[200px] w-full rounded-xl bg-gray-200 animate-pulse" />
          </div>
        </template>

        <template v-else-if="filteredTeams.length > 0">
          <div
            v-for="team in filteredTeams"
            :key="team.id"
            class="group wc-panel cursor-pointer overflow-hidden transition-shadow duration-300 hover:shadow-md"
            @click="emit('navigate-to-team', team.id)"
          >
            <div class="p-6 pb-4">
              <div class="flex justify-between items-start">
                <Avatar class="w-12 h-12 rounded-lg border border-gray-100 shadow-sm">
                  <img v-if="team.avatarUrl" :src="team.avatarUrl" class="w-full h-full object-cover rounded-lg" />
                  <div v-else class="w-full h-full rounded-lg bg-blue-50 text-blue-600 font-bold text-lg flex items-center justify-center">
                    {{ team.name.substring(0, 2).toUpperCase() }}
                  </div>
                </Avatar>

                <div class="flex items-center gap-1" @click.stop>
                  <Badge variant="secondary" class="bg-gray-100 text-gray-500 font-normal">
                    {{ roleText(team.myRole) }}
                  </Badge>

                  <div v-if="isAdmin(team.myRole)" class="relative">
                    <Button
                      variant="ghost"
                      size="icon"
                      class="h-8 w-8 text-gray-400 hover:text-gray-600"
                      @click.stop="toggleMenu(team.id)"
                    >
                      <MoreHorizontal class="w-4 h-4" />
                    </Button>

                    <div
                      v-if="openMenuTeamId === team.id"
                      class="absolute right-0 mt-2 w-40 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden z-20"
                      @click.stop
                    >
                      <button
                        class="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                        @click="copyInviteCode(team.id); closeMenu()"
                      >
                        <Copy class="w-4 h-4" />
                        复制邀请码
                      </button>
                      <button
                        class="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                        @click="emit('navigate-to-team-settings', team.id); closeMenu()"
                      >
                        <Settings class="w-4 h-4" />
                        团队设置
                      </button>
                      <button 
                        v-if="team.myRole === 0"
                        class="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                        @click="handleDissolveTeam(team.id); closeMenu()"
                      >
                        <LogOut class="w-4 h-4" />
                        解散团队
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mt-4 text-[15px] font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
                {{ team.name }}
              </div>
              <p class="text-sm text-gray-500 mt-1 line-clamp-2 min-h-[40px]">{{ team.description || '暂无描述' }}</p>
            </div>

            <div class="px-6 pb-4 pt-0">
              <div class="flex items-center gap-4 text-sm text-gray-500">
                <div class="flex items-center gap-1.5">
                  <Users class="w-4 h-4" />
                  <span>{{ team.memberCount }} 成员</span>
                </div>
                <div class="w-1 h-1 rounded-full bg-gray-300" />
                <span>活跃于 {{ formatLastActive(team.lastActiveTime) }}</span>
              </div>
            </div>

            <div class="px-6 pt-0 pb-6">
              <Button
                variant="outline"
                class="w-full h-9 text-gray-600 group-hover:bg-blue-600 group-hover:text-white group-hover:border-blue-600 transition-colors"
              >
                进入空间
                <ArrowRight class="w-4 h-4 ml-2" />
              </Button>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="col-span-full rounded-xl border border-dashed border-gray-300 bg-white/60">
            <div class="wc-empty">
              <div class="wc-empty-icon"><Users /></div>
              <p class="wc-empty-title">还没有团队</p>
              <p class="wc-empty-desc">创建一个新团队开始协作，或者向管理员索要邀请码加入现有团队。</p>
              <div class="wc-empty-action">
                <Button :disabled="loading" @click="handleCreateTeam">立即创建团队</Button>
              </div>
            </div>
          </div>
        </template>
      </div>

      <div v-if="filteredDiscoverableTeams.length > 0" class="space-y-3">
        <div>
          <h2 class="text-[15px] font-semibold text-gray-900">发现公开团队</h2>
          <p class="text-[12.5px] text-gray-500 mt-1">这些团队已开启公开可发现，加入仍需符合团队的邀请码规则。</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          <div
            v-for="team in filteredDiscoverableTeams"
            :key="team.id"
            class="group wc-panel cursor-pointer overflow-hidden transition-shadow duration-300 hover:shadow-md"
          >
            <div class="p-6 pb-4">
              <div class="flex justify-between items-start">
                <Avatar class="w-12 h-12 rounded-lg border border-gray-100 shadow-sm">
                  <img v-if="team.avatarUrl" :src="team.avatarUrl" class="w-full h-full object-cover rounded-lg" />
                  <div v-else class="w-full h-full rounded-lg bg-blue-50 text-blue-600 font-bold text-lg flex items-center justify-center">
                    {{ team.name.substring(0, 2).toUpperCase() }}
                  </div>
                </Avatar>

                <Badge variant="secondary" class="bg-blue-50 text-blue-600 font-normal">
                  {{ roleText(team.myRole) }}
                </Badge>
              </div>

              <div class="mt-4 text-[15px] font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
                {{ team.name }}
              </div>
              <p class="text-sm text-gray-500 mt-1 line-clamp-2 min-h-[40px]">{{ team.description || '暂无描述' }}</p>
            </div>

            <div class="px-6 pb-4 pt-0">
              <div class="flex items-center gap-4 text-sm text-gray-500">
                <div class="flex items-center gap-1.5">
                  <Users class="w-4 h-4" />
                  <span>{{ team.memberCount }} 成员</span>
                </div>
                <div class="w-1 h-1 rounded-full bg-gray-300" />
                <span>活跃于 {{ formatLastActive(team.lastActiveTime) }}</span>
              </div>
            </div>

            <div class="px-6 pt-0 pb-6">
              <Button
                variant="outline"
                class="w-full"
                :disabled="loading"
                @click.stop="handleJoinTeam"
              >
                输入邀请码加入
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
