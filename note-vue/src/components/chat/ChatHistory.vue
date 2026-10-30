<script setup lang="ts">
import { MessageSquare, Plus, Trash2 } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { formatRelativeTime } from '@/utils/timeFormat'
import type { ChatSession } from '@/types/chat'

interface Props {
  sessions: ChatSession[]
  selectedSessionId: string | null
}

defineProps<Props>()

const emit = defineEmits<{
  'select-session': [session: ChatSession]
  'new-chat': []
  'delete-session': [sessionId: string]
}>()
</script>

<template>
  <div class="w-80 h-full bg-white border-r border-gray-200 flex flex-col">
    <!-- Header -->
    <div class="p-4 border-b border-gray-200">
      <div class="flex items-center justify-between mb-3">
        <h2 class="flex items-center gap-2">
          <MessageSquare class="w-5 h-5" />
          历史对话
        </h2>
      </div>
      <Button 
        class="w-full justify-start bg-blue-600 hover:bg-blue-700 text-white"
        @click="emit('new-chat')"
      >
        <Plus class="w-4 h-4 mr-2" />
        新建对话
      </Button>
    </div>

    <!-- Sessions list -->
    <div class="flex-1 overflow-y-auto">
      <div v-if="sessions.length === 0" class="p-4 text-center text-gray-400">
        暂无历史对话
      </div>
      <div v-else class="divide-y divide-gray-100">
        <div
          v-for="session in sessions"
          :key="session.id"
          @click="emit('select-session', session)"
          :class="[
            'p-4 cursor-pointer hover:bg-gray-50 transition-colors group',
            selectedSessionId === session.id ? 'bg-blue-50 border-l-2 border-l-blue-500' : ''
          ]"
        >
          <div class="flex items-start justify-between gap-2 mb-1">
            <h3 class="flex-1 truncate">
              {{ session.title }}
            </h3>
            <Button
              variant="ghost"
              size="sm"
              class="p-1 h-auto opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500"
              @click.stop="emit('delete-session', session.id)"
            >
              <Trash2 class="w-3 h-3" />
            </Button>
          </div>
          
          <p class="text-xs text-gray-600 mb-2 line-clamp-2">
            {{ session.lastMessage || '暂无对话内容' }}
          </p>

          <div class="text-xs text-gray-400">
            {{ formatRelativeTime(session.timestamp) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
