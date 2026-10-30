<script setup lang="ts">
import { onMounted, ref } from 'vue'
import TeamListPage from './TeamListPage.vue'
import TeamWorkspacePage from './TeamWorkspacePage.vue'

const currentTeamId = ref<string | null>(null)
const openSettings = ref(false)

// __TEMP_PREVIEW__ 仅供本地视觉走查，上线前删除
onMounted(() => {
  if (import.meta.env.DEV) {
    const q = new URLSearchParams(location.search)
    const t = q.get('__team')
    if (t) currentTeamId.value = t
    if (q.get('__settings')) openSettings.value = true
  }
})

function handleNavigateToTeam(teamId: string) {
  currentTeamId.value = teamId
  openSettings.value = false
}

function handleNavigateToTeamSettings(teamId: string) {
  currentTeamId.value = teamId
  openSettings.value = true
}

function handleBack() {
  currentTeamId.value = null
  openSettings.value = false
}
</script>

<template>
  <TeamListPage
    v-if="!currentTeamId"
    @navigate-to-team="handleNavigateToTeam"
    @navigate-to-team-settings="handleNavigateToTeamSettings"
  />
  <TeamWorkspacePage v-else :team-id="currentTeamId" :open-settings="openSettings" @back="handleBack" />
</template>
