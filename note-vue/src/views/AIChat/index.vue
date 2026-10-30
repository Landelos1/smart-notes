<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue'
import ChatHistory from '@/components/chat/ChatHistory.vue'
import ChatDialog from '@/components/chat/ChatDialog.vue'
import type { ChatSession, Message } from '@/types/chat'
import { sendChatMessageStream, getChatSessions, createChatSession, deleteChatSession, getChatHistory } from '@/api/chat'
import { uploadDocument } from '@/api/personalKnowledge'
import { getUserInfo } from '@/api/auth'
import { useModal } from '@/composables/useModal'
import { useMembershipQuota } from '@/composables/useMembershipQuota'

const modal = useModal()

// 注入刷新笔记列表的方法
const refreshNotesAndNotebooks = inject<() => Promise<void>>('refreshNotesAndNotebooks')

// 会话数据
const sessions = ref<ChatSession[]>([])
const selectedSessionId = ref<string | null>(null)
const isLoading = ref(false)
const isLoadingSessions = ref(false)
const abortController = ref<AbortController | null>(null)
const chatDialogRef = ref<InstanceType<typeof ChatDialog> | null>(null)

const { decrementAIQuota, refreshQuota } = useMembershipQuota()
const currentUserId = ref<number | null>(null)

// 知识库开关：默认不使用知识库（新建对话时不基于之前的文件回答）
const useKnowledge = ref(false)
const selectedAgentId = ref('')
const agentOptions = [
  { label: '笔记整理智能体', value: 'note_organizer' },
  { label: '学习辅导智能体', value: 'study_coach' },
  { label: '工作效率智能体', value: 'productivity' },
  { label: '创意写作智能体', value: 'creative_writer' },
  { label: '知识关联智能体', value: 'knowledge_graph' },
  { label: '个人成长智能体', value: 'growth_mentor' }
]

function mapSessionFromApi(session: any): ChatSession {
  return {
    id: String(session.id),
    title: session.title || '新对话',
    lastMessage: session.lastMessage || '',
    timestamp: session.timestamp ? new Date(session.timestamp) : new Date(),
    messages: []
  }
}

function mapMessageFromApi(message: any): Message {
  return {
    id: String(message.id),
    role: message.role,
    content: message.content,
    citations: message.citations || [],
    timestamp: message.timestamp ? new Date(message.timestamp) : new Date()
  }
}

async function fetchSessions() {
  isLoadingSessions.value = true
  try {
    const res = await getChatSessions()
    if (res.code === 0) {
      sessions.value = (res.data || []).map(mapSessionFromApi)
    } else {
      await modal.error(res.message || '获取会话失败')
    }
  } catch (error) {
    console.error('加载会话数据失败:', error)
    await modal.error('获取会话失败，请重试')
  } finally {
    isLoadingSessions.value = false
  }
}

async function loadSessionMessages(sessionId: string) {
  try {
    const res = await getChatHistory(sessionId)
    if (res.code === 0) {
      const messages = (res.data || []).map(mapMessageFromApi)
      sessions.value = sessions.value.map(session =>
        session.id === sessionId ? { ...session, messages } : session
      )
    } else {
      await modal.error(res.message || '获取历史消息失败')
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
    await modal.error('获取历史消息失败，请重试')
  }
}

async function initSessions() {
  await fetchSessions()

  if (sessions.value.length === 0) {
    await handleNewChat()
  } else {
    selectedSessionId.value = sessions.value[0]?.id || null
    if (selectedSessionId.value) {
      await loadSessionMessages(selectedSessionId.value)
    }
  }
}

// 初始化时加载数据
onMounted(() => {
  void initSessions()
  void (async () => {
    try {
      const r = await getUserInfo()
      if (r && r.code === 0 && r.data?.id) {
        currentUserId.value = Number(r.data.id)
      }
    } catch (e) {
      console.error('[AIChat] 获取用户信息失败:', e)
    }
  })()
})

const selectedSession = computed(() => 
  sessions.value.find(s => s.id === selectedSessionId.value) || null
)

// 创建新对话
async function handleNewChat() {
  try {
    const res = await createChatSession('新对话')
    if (res.code === 0 && res.data) {
      const newSession = mapSessionFromApi(res.data)
      sessions.value = [{ ...newSession }, ...sessions.value]
      selectedSessionId.value = newSession.id
    } else {
      await modal.error(res.message || '创建会话失败')
    }
  } catch (error) {
    console.error('创建会话失败:', error)
    await modal.error('创建会话失败，请重试')
  }
}

// 发送消息
async function handleSendMessage(content: string) {
  let enhancedMessage = content
  
  // 如果有文件待读取，先读取文件内容
  if (chatDialogRef.value?.selectedFiles?.length > 0) {
    try {
      const result = await chatDialogRef.value.readSelectedFilesContent()
      
      if (result.readCount > 0) {
        // 提示用户文件已读取
        const message = `已读取 ${result.readCount} 个文件。${result.failedCount > 0 ? `失败 ${result.failedCount} 个。` : ''}`
        await modal.info(message)
        
        if (!useKnowledge.value) {
          // 未开启知识库增强：将文件内容拼接到消息中
          const fileContexts: string[] = []
          result.fileContents.forEach((content: string, fileName: string) => {
            fileContexts.push(`

=== 文件: ${fileName} ===
${content}
=== 文件结束 ===`)
          })
          enhancedMessage = `请基于以下文件内容回答问题：${fileContexts.join('\n')}\n\n用户问题：${content}`
          
          // 清空选中的文件
          chatDialogRef.value.selectedFiles = []
        } else {
          // 开启知诈6库增强：将文件上传到知识库
          let uploadSuccess = 0
          let uploadFailed = 0
          for (const file of chatDialogRef.value.selectedFiles) {
            try {
              await uploadDocument(file)
              uploadSuccess++
            } catch (error) {
              console.error(`上传文件 ${file.name} 失败:`, error)
              uploadFailed++
            }
          }
          if (uploadSuccess > 0) {
            await modal.success(`已上传 ${uploadSuccess} 个文件到知识库。${uploadFailed > 0 ? `失败 ${uploadFailed} 个。` : ''}`)
          }
          // 清空选中的文件
          chatDialogRef.value.selectedFiles = []
        }
      }
      
      if (result.failedCount > 0) {
        console.error('部分文件读取失败:', result.failedFiles)
      }
    } catch (error) {
      console.error('读取文件失败:', error)
      await modal.error('文件读取失败，请重试')
      return
    }
  }

  if (!selectedSessionId.value) {
    await handleNewChat()
  }

  if (!selectedSessionId.value) {
    return
  }

  const userMessage: Message = {
    id: Date.now().toString(),
    role: 'user',
    content,
    timestamp: new Date()
  }

  // 更新会话：添加用户消息
  sessions.value = sessions.value.map(session => {
    if (session.id === selectedSessionId.value) {
      const updatedMessages = [...session.messages, userMessage]
      const newTitle = session.messages.length === 0 
        ? content.substring(0, 20) + (content.length > 20 ? '...' : '')
        : session.title
      
      return {
        ...session,
        messages: updatedMessages,
        lastMessage: content,
        timestamp: new Date(),
        title: newTitle
      }
    }
    return session
  })

  // 调用后端流式API
  isLoading.value = true
  abortController.value?.abort()
  abortController.value = new AbortController()
  
  try {
    // 创建AI消息占位符
    const aiMessageId = (Date.now() + 1).toString()
    const aiMessage: Message = {
      id: aiMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date()
    }
    
    // 添加AI消息占位符到会话
    sessions.value = sessions.value.map(session => {
      if (session.id === selectedSessionId.value) {
        return {
          ...session,
          messages: [...session.messages, aiMessage]
        }
      }
      return session
    })

    // 累积的内容
    let accumulatedContent = ''

    // 调用流式API，传入回调函数处理每个数据块
    await sendChatMessageStream(
      {
        memoryId: selectedSessionId.value!,
        message: enhancedMessage,  // 使用增强后的消息
        displayMessage: content,
        agentId: selectedAgentId.value,
        useKnowledge: useKnowledge.value
      },
      (chunk: string) => {
        // 接收到新的数据块
        accumulatedContent += chunk

        // 实时更新AI消息内容
        sessions.value = sessions.value.map(session => {
          if (session.id === selectedSessionId.value) {
            return {
              ...session,
              messages: session.messages.map(msg => 
                msg.id === aiMessageId 
                  ? { ...msg, content: accumulatedContent }
                  : msg
              ),
              lastMessage: accumulatedContent.substring(0, 50) + (accumulatedContent.length > 50 ? '...' : ''),
              timestamp: new Date()
            }
          }
          return session
        })
      },
      abortController.value.signal
    )

    decrementAIQuota()
    if (currentUserId.value) {
      void refreshQuota(currentUserId.value)
    }

    await loadSessionMessages(selectedSessionId.value)

  } catch (error) {
    if (error?.name === 'AbortError') {
      return
    }
    console.error('发送消息失败:', error)
    
    // 添加错误消息
    const errorMessage: Message = {
      id: (Date.now() + 2).toString(),
      role: 'assistant',
      content: '抱歉，发送消息失败。请检查网络连接或稍后重试。',
      timestamp: new Date()
    }

    sessions.value = sessions.value.map(session => {
      if (session.id === selectedSessionId.value) {
        // 移除最后一条空消息（如果有的话）
        const messages = session.messages.filter(msg => msg.content !== '')
        return {
          ...session,
          messages: [...messages, errorMessage],
          lastMessage: errorMessage.content,
          timestamp: new Date()
        }
      }
      return session
    })
  } finally {
    isLoading.value = false
    abortController.value = null
  }
}

function handleStopResponse() {
  abortController.value?.abort()
}

// 处理笔记保存成功事件
async function handleNoteSaved() {
  console.log('[AIChat] 笔记保存成功，刷新笔记列表')
  // 调用父组件提供的刷新方法
  if (refreshNotesAndNotebooks) {
    await refreshNotesAndNotebooks()
  }
}

// 删除会话
async function handleDeleteSession(sessionId: string) {
  try {
    const res = await deleteChatSession(sessionId)
    if (res.code !== 0) {
      await modal.error(res.message || '删除会话失败')
      return
    }
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    if (selectedSessionId.value === sessionId) {
      selectedSessionId.value = sessions.value[0]?.id || null
      if (selectedSessionId.value) {
        await loadSessionMessages(selectedSessionId.value)
      } else {
        await handleNewChat()
      }
    }
  } catch (error) {
    console.error('删除会话失败:', error)
    await modal.error('删除会话失败，请重试')
  }
}

// 选择会话
async function handleSelectSession(session: ChatSession) {
  if (selectedSessionId.value === session.id) {
    return
  }
  selectedSessionId.value = session.id
  await loadSessionMessages(session.id)
}
</script>

<template>
  <div class="h-full flex bg-gray-50 overflow-hidden">
    <!-- 历史对话侧栏 -->
    <ChatHistory
      :sessions="sessions"
      :selected-session-id="selectedSessionId"
      @select-session="handleSelectSession"
      @new-chat="handleNewChat"
      @delete-session="handleDeleteSession"
    />

    <!-- 聊天对话框 -->
    <div class="flex-1 flex flex-col min-w-0">
      <ChatDialog
        ref="chatDialogRef"
        :messages="selectedSession?.messages || []"
        :is-loading="isLoading"
        :conversation-title="selectedSession?.title || ''"
        :agent-id="selectedAgentId"
        :use-knowledge-base="useKnowledge"
        @send-message="handleSendMessage"
        @stop="handleStopResponse"
        @note-saved="handleNoteSaved"
        @update:agent-id="selectedAgentId = $event"
        @update:use-knowledge-base="useKnowledge = $event"
      />
    </div>
  </div>
</template>
