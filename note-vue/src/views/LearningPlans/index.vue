<script setup>
import { computed, onMounted, ref } from 'vue'
import { CheckCircle2, Circle, ClipboardList, Clock3, Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import { createReviewPlan, createTask, deleteReviewPlan, deleteTask, getReviewPlans, getTasks, updateReviewPlan, updateTask } from '@/api/learning.js'
import { useModal } from '@/composables/useModal'

const modal = useModal()
const tasks = ref([])
const plans = ref([])
const loading = ref(false)
const tab = ref('tasks')
const newTaskTitle = ref('')
const newPlanTitle = ref('')

const pendingTasks = computed(() => tasks.value.filter(t => Number(t.status) !== 2))
const completedTasks = computed(() => tasks.value.filter(t => Number(t.status) === 2))

async function load() {
  loading.value = true
  try {
    const [taskRes, planRes] = await Promise.all([getTasks(), getReviewPlans()])
    if (taskRes?.code === 0) tasks.value = taskRes.data || []
    if (planRes?.code === 0) plans.value = planRes.data || []
  } catch (error) {
    await modal.error('任务与复习计划加载失败，请检查后端服务')
  } finally { loading.value = false }
}

async function addTask() {
  const title = newTaskTitle.value.trim()
  if (!title) return
  const res = await createTask({ title, priority: 2, sourceType: 'manual' })
  if (res?.code === 0) { tasks.value.unshift(res.data); newTaskTitle.value = '' }
  else await modal.error(res?.message || '创建任务失败')
}

async function toggleTask(task) {
  const res = await updateTask(task.id, { title: task.title, description: task.description || '', status: Number(task.status) === 2 ? 0 : 2, priority: task.priority || 2, dueTime: task.dueTime })
  if (res?.code === 0) Object.assign(task, res.data)
  else await modal.error(res?.message || '更新任务失败')
}

async function removeTask(task) {
  if (!await modal.confirm('确定删除这个任务吗？')) return
  const res = await deleteTask(task.id)
  if (res?.code === 0) tasks.value = tasks.value.filter(item => item.id !== task.id)
}

async function addPlan() {
  const title = newPlanTitle.value.trim()
  if (!title) return
  const startDate = new Date().toISOString().slice(0, 10)
  const end = new Date(); end.setDate(end.getDate() + 28)
  const res = await createReviewPlan({ title, goal: '巩固核心知识', frequency: 'weekly', startDate, endDate: end.toISOString().slice(0, 10), sourceType: 'manual' })
  if (res?.code === 0) { plans.value.unshift(res.data); newPlanTitle.value = '' }
  else await modal.error(res?.message || '创建复习计划失败')
}

async function togglePlan(plan) {
  const res = await updateReviewPlan(plan.id, { title: plan.title, goal: plan.goal || '', frequency: plan.frequency, status: Number(plan.status) === 1 ? 0 : 1, startDate: plan.startDate, endDate: plan.endDate })
  if (res?.code === 0) Object.assign(plan, res.data)
}

async function removePlan(plan) {
  if (!await modal.confirm('确定删除这个复习计划吗？')) return
  const res = await deleteReviewPlan(plan.id)
  if (res?.code === 0) plans.value = plans.value.filter(item => item.id !== plan.id)
}

onMounted(load)
</script>

<template>
  <main class="lp-page">
    <header class="lp-header">
      <div>
        <p class="lp-kicker">LEARNING LOOP</p>
        <h1>任务与复习</h1>
        <p class="lp-subtitle">把笔记变成可执行的行动，再用规律复习真正掌握知识。</p>
      </div>
      <button class="lp-refresh" :disabled="loading" @click="load"><RefreshCw :size="15" :class="{ 'lp-spin': loading }" />刷新</button>
    </header>

    <section class="lp-tabs">
      <button :class="{ active: tab === 'tasks' }" @click="tab = 'tasks'"><ClipboardList :size="16" />学习任务 <span>{{ pendingTasks.length }}</span></button>
      <button :class="{ active: tab === 'plans' }" @click="tab = 'plans'"><Clock3 :size="16" />复习计划 <span>{{ plans.length }}</span></button>
    </section>

    <section v-if="tab === 'tasks'" class="lp-card">
      <div class="lp-card-head"><div><h2>待办任务</h2><p>来自笔记拆解、AI Agent 或手动创建的学习行动。</p></div></div>
      <div class="lp-add"><input v-model="newTaskTitle" placeholder="例如：整理数据库索引的三个使用场景" @keyup.enter="addTask" /><button @click="addTask"><Plus :size="16" />添加任务</button></div>
      <div v-if="pendingTasks.length === 0" class="lp-empty">还没有待处理任务，去笔记页面生成一份学习安排吧。</div>
      <div v-for="task in pendingTasks" :key="task.id" class="lp-row">
        <button class="lp-check" title="完成任务" @click="toggleTask(task)"><Circle :size="20" /></button>
        <div class="lp-row-main"><strong>{{ task.title }}</strong><span v-if="task.description">{{ task.description }}</span><small v-if="task.dueTime">截止 {{ new Date(task.dueTime).toLocaleString() }}</small></div>
        <em :class="`priority-${task.priority}`">{{ task.priority === 3 ? '高' : task.priority === 1 ? '低' : '中' }}</em>
        <button class="lp-delete" @click="removeTask(task)"><Trash2 :size="15" /></button>
      </div>
      <div v-if="completedTasks.length" class="lp-completed-title">已完成 {{ completedTasks.length }}</div>
      <div v-for="task in completedTasks" :key="`done-${task.id}`" class="lp-row is-done"><button class="lp-check" @click="toggleTask(task)"><CheckCircle2 :size="20" /></button><div class="lp-row-main"><strong>{{ task.title }}</strong></div><button class="lp-delete" @click="removeTask(task)"><Trash2 :size="15" /></button></div>
    </section>

    <section v-else class="lp-card">
      <div class="lp-card-head"><div><h2>复习计划</h2><p>为重要笔记安排节奏，形成可持续的记忆周期。</p></div></div>
      <div class="lp-add"><input v-model="newPlanTitle" placeholder="例如：Java 并发基础复习计划" @keyup.enter="addPlan" /><button @click="addPlan"><Plus :size="16" />创建计划</button></div>
      <div v-if="plans.length === 0" class="lp-empty">还没有复习计划，可以从任意笔记一键生成。</div>
      <div v-for="plan in plans" :key="plan.id" class="lp-plan"><div class="lp-plan-icon"><Clock3 :size="18" /></div><div class="lp-row-main"><strong>{{ plan.title }}</strong><span>{{ plan.goal || '巩固笔记核心知识' }}</span><small>{{ plan.startDate }} 至 {{ plan.endDate }} · {{ plan.frequency === 'daily' ? '每天' : plan.frequency === 'weekly' ? '每周' : '自定义' }}</small></div><button class="lp-status" :class="{ done: plan.status === 1 }" @click="togglePlan(plan)">{{ plan.status === 1 ? '已完成' : '进行中' }}</button><button class="lp-delete" @click="removePlan(plan)"><Trash2 :size="15" /></button></div>
    </section>
  </main>
</template>

<style scoped>
.lp-page{height:100%;overflow:auto;padding:42px clamp(24px,5vw,76px);background:linear-gradient(135deg,#fbf8f2,#f6efe5);color:#3c2b20}.lp-header{display:flex;justify-content:space-between;align-items:flex-start;max-width:920px;margin:0 auto 30px}.lp-kicker{font-size:11px;letter-spacing:2px;color:#b46a35;font-weight:700;margin:0 0 8px}.lp-header h1{font-family:Georgia,serif;font-size:32px;margin:0 0 8px;color:#302017}.lp-subtitle{margin:0;color:#846f5e;font-size:14px}.lp-refresh{display:flex;gap:7px;align-items:center;border:1px solid #dfcfbd;background:#fffaf4;color:#75583f;border-radius:8px;padding:8px 12px;cursor:pointer}.lp-tabs,.lp-card{max-width:920px;margin:0 auto}.lp-tabs{display:flex;gap:4px;border-bottom:1px solid #e2d5c7;margin-bottom:18px}.lp-tabs button{display:flex;align-items:center;gap:7px;border:0;background:none;color:#987c65;padding:11px 16px;border-bottom:2px solid transparent;cursor:pointer}.lp-tabs button.active{color:#a65320;border-bottom-color:#c26a26;font-weight:650}.lp-tabs span{font-size:11px;background:#eadbc9;border-radius:10px;padding:1px 6px}.lp-card{background:#fffdf9;border:1px solid #eadfd2;border-radius:16px;box-shadow:0 12px 35px #74502d0d;padding:24px}.lp-card-head h2{margin:0 0 6px;font-size:18px;color:#3c2b20}.lp-card-head p{margin:0;color:#9b8673;font-size:13px}.lp-add{display:flex;gap:10px;margin:22px 0}.lp-add input{flex:1;border:1px solid #e3d5c7;border-radius:9px;padding:10px 12px;background:#fff;color:#4a382b;outline:none}.lp-add input:focus{border-color:#c9834e}.lp-add button{display:flex;align-items:center;gap:5px;border:0;background:#b96028;color:#fff;border-radius:9px;padding:0 14px;cursor:pointer}.lp-row,.lp-plan{display:flex;align-items:center;gap:12px;padding:14px 4px;border-top:1px solid #f0e8df}.lp-check,.lp-delete{border:0;background:none;color:#b87843;cursor:pointer;display:grid;place-items:center}.lp-delete{color:#b8a291}.lp-delete:hover{color:#b24e36}.lp-row-main{flex:1;display:flex;flex-direction:column;gap:4px;min-width:0}.lp-row-main strong{font-size:14px;color:#4b382b}.lp-row-main span,.lp-row-main small{font-size:12px;color:#9a8572}.lp-row-main small{color:#b09580}.lp-row.is-done strong{text-decoration:line-through;color:#a69689}.lp-row.is-done .lp-check{color:#6d9c72}.lp-row em{font-style:normal;font-size:11px;padding:3px 7px;border-radius:5px}.priority-1{background:#e6f1e7;color:#59815e}.priority-2{background:#f7ebd8;color:#aa713c}.priority-3{background:#f8e1dc;color:#ac5546}.lp-empty{text-align:center;padding:42px 10px;color:#aa9786;font-size:13px}.lp-completed-title{font-size:12px;color:#a48e7b;padding:18px 4px 6px}.lp-plan-icon{width:36px;height:36px;border-radius:10px;background:#f4e5d5;color:#b6652c;display:grid;place-items:center}.lp-status{border:1px solid #e0c6aa;border-radius:7px;background:#fff7ed;color:#af692f;padding:6px 10px;font-size:12px;cursor:pointer}.lp-status.done{background:#e7f1e8;color:#5f8663;border-color:#cce0cf}.lp-spin{animation:spin 1s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
</style>
