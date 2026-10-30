<script setup lang="ts">
import { ref } from 'vue'
import TeamListPage from './TeamListPage.vue'
import TeamWorkspacePage from './TeamWorkspacePage.vue'

const currentTeamId = ref<string | null>(null)
const openSettings = ref(false)

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
