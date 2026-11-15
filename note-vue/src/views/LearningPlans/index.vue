<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { BookOpen, CalendarDays, CheckCircle2, ChevronDown, Circle, ClipboardList, Clock3, Pencil, Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { createReviewPlan, createTask, deleteReviewPlan, deleteTask, getReviewPlans, getTasks, updateReviewPlan, updateTask } from '@/api/learning.js'
import { getNotes } from '@/api/notes.js'
import { useModal } from '@/composables/useModal'
import { formatLocalDate, getUpcomingReviewDates } from '@/utils/reviewSchedule.js'

const modal = useModal()
const router = useRouter()
const tasks = ref([])
const plans = ref([])
const notes = ref([])
const notesLoadError = ref('')
const loading = ref(false)
const tab = ref('tasks')
const newTaskTitle = ref('')
const pendingActions = ref(new Set())
const editingPlanId = ref(null)
const expandedPlanId = ref(null)
const planEditor = ref(null)

const weekdays = [
  { value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' },
  { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' }, { value: 7, label: '周日' }
]

function createPlanForm() {
  const start = new Date()
  const end = new Date(start)
  end.setDate(end.getDate() + 28)
  return {
    title: '', goal: '', noteId: '', frequency: 'weekly',
    repeatDay: start.getDay() || 7, intervalDays: 3,
    startDate: formatLocalDate(start), endDate: formatLocalDate(end)
  }
}

const planForm = ref(createPlanForm())

const pendingTasks = computed(() => tasks.value.filter(t => Number(t.status) !== 2))
const completedTasks = computed(() => tasks.value.filter(t => Number(t.status) === 2))
const noteById = computed(() => new Map(notes.value.map(note => [String(note.id), note])))

function unwrap(res, fallback) {
  if (res?.code !== 0) throw new Error(res?.message || fallback)
  return res.data
}

async function load() {
  if (loading.value) return
  loading.value = true
  try {
    const [taskRes, planRes] = await Promise.all([getTasks(), getReviewPlans()])
    tasks.value = unwrap(taskRes, '任务加载失败') || []
    plans.value = unwrap(planRes, '复习计划加载失败') || []
    try {
      const notePage = unwrap(await getNotes({ page: 1, size: 100 }), '笔记来源加载失败')
      notes.value = notePage?.data || notePage?.records || []
      notesLoadError.value = ''
    } catch (error) {
      notes.value = []
      notesLoadError.value = error instanceof Error ? error.message : '笔记列表暂时不可用'
    }
  } catch (error) {
    await modal.error(error instanceof Error ? error.message : '加载失败，请重试')
  } finally {
    loading.value = false
  }
}

async function perform(key, action) {
  if (pendingActions.value.has(key)) return
  pendingActions.value.add(key)
  try {
    await action()
  } catch (error) {
    await modal.error(error instanceof Error ? error.message : '操作失败，请重试')
  } finally {
    pendingActions.value.delete(key)
  }
}

async function addTask() {
  const title = newTaskTitle.value.trim()
  if (!title) return
  await perform('new-task', async () => {
    const task = unwrap(await createTask({ title, priority: 2, sourceType: 'manual' }), '创建任务失败')
    tasks.value.unshift(task)
    newTaskTitle.value = ''
  })
}

async function toggleTask(task) {
  await perform(`task-${task.id}`, async () => {
    const updated = unwrap(await updateTask(task.id, {
      title: task.title, description: task.description || '',
      status: Number(task.status) === 2 ? 0 : 2,
      priority: task.priority || 2, dueTime: task.dueTime
    }), '更新任务失败')
    Object.assign(task, updated)
  })
}

function sourceLabel(record) {
  if (record.noteId) return record.sourceType === 'note-ai' ? '笔记生成' : '关联笔记'
  if (record.sourceType === 'agent') return 'Agent 创建'
  if (record.sourceType === 'note-ai') return 'AI 生成'
  return '手动创建'
}

function sourceNoteTitle(record) {
  const note = noteById.value.get(String(record.noteId))
  return note?.title || (record.noteId ? `笔记 #${record.noteId}` : '')
}

function openSourceNote(noteId) {
  if (!noteId) return
  void router.push({ name: 'Notes', query: { noteId: String(noteId) } })
}

async function removeTask(task) {
  await perform(`task-${task.id}`, async () => {
    if (!await modal.confirm({ message: '确定删除这个任务吗？' })) return
    unwrap(await deleteTask(task.id), '删除任务失败')
    tasks.value = tasks.value.filter(item => item.id !== task.id)
  })
}

async function savePlan() {
  const title = planForm.value.title.trim()
  if (!title) return
  if (planForm.value.endDate < planForm.value.startDate) {
    await modal.error('结束日期不能早于开始日期')
    return
  }
  const payload = {
    title,
    goal: planForm.value.goal.trim(),
    noteId: planForm.value.noteId ? Number(planForm.value.noteId) : null,
    clearNote: Boolean(editingPlanId.value && !planForm.value.noteId),
    frequency: planForm.value.frequency,
    repeatDay: planForm.value.frequency === 'weekly' ? Number(planForm.value.repeatDay) : null,
    intervalDays: planForm.value.frequency === 'daily' ? 1 : planForm.value.frequency === 'weekly' ? 7 : Number(planForm.value.intervalDays),
    startDate: planForm.value.startDate,
    endDate: planForm.value.endDate
  }
  const key = editingPlanId.value ? `plan-${editingPlanId.value}` : 'new-plan'
  await perform(key, async () => {
    if (editingPlanId.value) {
      const existing = plans.value.find(plan => plan.id === editingPlanId.value)
      const updated = unwrap(await updateReviewPlan(editingPlanId.value, {
        ...payload, status: existing?.status ?? 0
      }), '保存复习计划失败')
      Object.assign(existing, updated)
      expandedPlanId.value = updated.id
    } else {
      const plan = unwrap(await createReviewPlan({ ...payload, sourceType: 'manual' }), '创建复习计划失败')
      plans.value.unshift(plan)
      expandedPlanId.value = plan.id
    }
    editingPlanId.value = null
    planForm.value = createPlanForm()
  })
}

async function editPlan(plan) {
  editingPlanId.value = plan.id
  const startDate = String(plan.startDate).slice(0, 10)
  const startWeekday = new Date(`${startDate}T00:00:00`).getDay() || 7
  planForm.value = {
    title: plan.title,
    goal: plan.goal || '',
    noteId: plan.noteId ? String(plan.noteId) : '',
    frequency: plan.frequency || 'weekly',
    repeatDay: Number(plan.repeatDay) || startWeekday,
    intervalDays: Number(plan.intervalDays) || 7,
    startDate: String(plan.startDate).slice(0, 10),
    endDate: String(plan.endDate).slice(0, 10)
  }
  tab.value = 'plans'
  await nextTick()
  planEditor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function cancelPlanEdit() {
  editingPlanId.value = null
  planForm.value = createPlanForm()
}

function frequencyLabel(plan) {
  if (plan.frequency === 'daily') return '每天'
  if (plan.frequency === 'weekly') return `每周${weekdays.find(day => day.value === Number(plan.repeatDay))?.label || ''}`
  return `每隔 ${Number(plan.intervalDays) || 7} 天`
}

function upcomingDates(plan) {
  return getUpcomingReviewDates(plan, new Date(), 5)
}

function togglePlanDetails(plan) {
  expandedPlanId.value = expandedPlanId.value === plan.id ? null : plan.id
}

async function togglePlan(plan) {
  await perform(`plan-${plan.id}`, async () => {
    const updated = unwrap(await updateReviewPlan(plan.id, {
      title: plan.title, goal: plan.goal || '', frequency: plan.frequency,
      status: Number(plan.status) === 1 ? 0 : 1,
      startDate: plan.startDate, endDate: plan.endDate
    }), '更新复习计划失败')
    Object.assign(plan, updated)
  })
}

async function removePlan(plan) {
  await perform(`plan-${plan.id}`, async () => {
    if (!await modal.confirm({ message: '确定删除这个复习计划吗？' })) return
    unwrap(await deleteReviewPlan(plan.id), '删除复习计划失败')
    plans.value = plans.value.filter(item => item.id !== plan.id)
  })
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
      <div class="lp-add"><input v-model="newTaskTitle" maxlength="255" placeholder="例如：整理数据库索引的三个使用场景" @keyup.enter="addTask" /><button :disabled="pendingActions.has('new-task') || !newTaskTitle.trim()" @click="addTask"><Plus :size="16" />添加任务</button></div>
      <div v-if="pendingTasks.length === 0" class="lp-empty">还没有待处理任务，去笔记页面生成一份学习安排吧。</div>
      <div v-for="task in pendingTasks" :key="task.id" class="lp-row">
        <button class="lp-check" title="完成任务" aria-label="完成任务" :disabled="pendingActions.has(`task-${task.id}`)" @click="toggleTask(task)"><Circle :size="20" /></button>
        <div class="lp-row-main"><strong>{{ task.title }}</strong><span v-if="task.description">{{ task.description }}</span><small v-if="task.dueTime">截止 {{ new Date(task.dueTime).toLocaleString() }}</small><small class="lp-origin"><span>{{ sourceLabel(task) }}</span><button v-if="task.noteId" @click="openSourceNote(task.noteId)">来源笔记：{{ sourceNoteTitle(task) }}</button></small></div>
        <em :class="`priority-${task.priority}`">{{ task.priority === 3 ? '高' : task.priority === 1 ? '低' : '中' }}</em>
        <button class="lp-delete" title="删除任务" aria-label="删除任务" :disabled="pendingActions.has(`task-${task.id}`)" @click="removeTask(task)"><Trash2 :size="15" /></button>
      </div>
      <div v-if="completedTasks.length" class="lp-completed-title">已完成 {{ completedTasks.length }}</div>
      <div v-for="task in completedTasks" :key="`done-${task.id}`" class="lp-row is-done"><button class="lp-check" title="恢复任务" aria-label="恢复任务" :disabled="pendingActions.has(`task-${task.id}`)" @click="toggleTask(task)"><CheckCircle2 :size="20" /></button><div class="lp-row-main"><strong>{{ task.title }}</strong><small class="lp-origin"><span>{{ sourceLabel(task) }}</span><button v-if="task.noteId" @click="openSourceNote(task.noteId)">来源笔记：{{ sourceNoteTitle(task) }}</button></small></div><button class="lp-delete" title="删除任务" aria-label="删除任务" :disabled="pendingActions.has(`task-${task.id}`)" @click="removeTask(task)"><Trash2 :size="15" /></button></div>
    </section>

    <section v-else class="lp-card">
      <div class="lp-card-head"><div><h2>复习计划</h2><p>设定重复周期和来源笔记，查看接下来每一次复习日期。</p></div></div>
      <form ref="planEditor" class="lp-plan-editor" @submit.prevent="savePlan">
        <div class="lp-form-grid">
          <label class="lp-field"><span>计划名称</span><input v-model="planForm.title" maxlength="255" required placeholder="例如：Java 并发基础复习" /></label>
          <label class="lp-field"><span>来源笔记</span><select v-model="planForm.noteId"><option value="">不关联笔记（手动计划）</option><option v-for="note in notes" :key="note.id" :value="String(note.id)">{{ note.title || '无标题笔记' }} · {{ note.notebookName || '未分类' }}</option></select><small v-if="notesLoadError" class="lp-field-error">笔记列表加载失败：{{ notesLoadError }}。仍可创建不关联笔记的计划。</small></label>
          <label class="lp-field lp-field-wide"><span>复习目标</span><input v-model="planForm.goal" maxlength="500" placeholder="例如：掌握线程池和锁的使用边界" /></label>
        </div>
        <div class="lp-schedule-fields">
          <fieldset class="lp-field lp-frequency-field"><legend>重复频率</legend><div class="lp-frequency" role="group" aria-label="重复频率">
            <button v-for="item in [{ value: 'daily', label: '每天' }, { value: 'weekly', label: '每周' }, { value: 'custom', label: '每隔几天' }]" :key="item.value" type="button" :aria-pressed="planForm.frequency === item.value" :class="{ active: planForm.frequency === item.value }" @click="planForm.frequency = item.value">{{ item.label }}</button>
          </div></fieldset>
          <label v-if="planForm.frequency === 'weekly'" class="lp-field"><span>复习日</span><select v-model.number="planForm.repeatDay"><option v-for="day in weekdays" :key="day.value" :value="day.value">{{ day.label }}</option></select></label>
          <label v-if="planForm.frequency === 'custom'" class="lp-field"><span>间隔天数</span><span class="lp-number-input"><input v-model.number="planForm.intervalDays" type="number" min="1" max="365" required /><span>天一次</span></span></label>
          <label class="lp-field"><span>开始日期</span><input v-model="planForm.startDate" type="date" required /></label>
          <label class="lp-field"><span>结束日期</span><input v-model="planForm.endDate" type="date" :min="planForm.startDate" required /></label>
        </div>
        <div class="lp-editor-actions"><p>笔记页的“生成学习安排”会根据笔记内容创建任务和复习计划，并自动关联原笔记；这里创建的计划可以手动选择来源，复习日期按所选频率和日期范围计算。</p><div><button v-if="editingPlanId" class="lp-cancel" type="button" @click="cancelPlanEdit">取消</button><button class="lp-save" type="submit" :disabled="pendingActions.has(editingPlanId ? `plan-${editingPlanId}` : 'new-plan') || !planForm.title.trim()"><Plus v-if="!editingPlanId" :size="16" /><Pencil v-else :size="15" />{{ editingPlanId ? '保存修改' : '创建计划' }}</button></div></div>
      </form>
      <div v-if="plans.length === 0" class="lp-empty">还没有复习计划，可以从任意笔记一键生成。</div>
      <article v-for="plan in plans" :key="plan.id" class="lp-plan-item">
        <div class="lp-plan"><div class="lp-plan-icon"><Clock3 :size="18" /></div><div class="lp-row-main">
          <button class="lp-plan-title" @click="togglePlanDetails(plan)">{{ plan.title }}<ChevronDown :size="15" :class="{ rotated: expandedPlanId === plan.id }" /></button>
          <span>{{ plan.goal || '巩固笔记核心知识' }}</span><small>{{ plan.startDate }} 至 {{ plan.endDate }} · {{ frequencyLabel(plan) }}</small>
          <small class="lp-origin"><span>{{ sourceLabel(plan) }}</span><button v-if="plan.noteId" @click="openSourceNote(plan.noteId)">来源笔记：{{ sourceNoteTitle(plan) }}</button></small>
          <div class="lp-date-preview"><CalendarDays :size="14" /><span v-if="upcomingDates(plan).length">接下来：{{ upcomingDates(plan).slice(0, 3).join('、') }}</span><span v-else>计划期内暂无后续复习日期</span></div>
        </div><div class="lp-plan-actions">
          <button class="lp-status" :disabled="pendingActions.has(`plan-${plan.id}`)" :class="{ done: plan.status === 1 }" @click="togglePlan(plan)">{{ plan.status === 1 ? '已完成' : plan.status === 2 ? '已归档' : '进行中' }}</button>
          <button class="lp-icon-action" title="编辑复习计划" aria-label="编辑复习计划" :disabled="pendingActions.has(`plan-${plan.id}`)" @click="editPlan(plan)"><Pencil :size="15" /></button>
          <button class="lp-delete" title="删除复习计划" aria-label="删除复习计划" :disabled="pendingActions.has(`plan-${plan.id}`)" @click="removePlan(plan)"><Trash2 :size="15" /></button>
        </div></div>
        <div v-if="expandedPlanId === plan.id" class="lp-plan-details">
          <div><strong>复习日期</strong><div v-if="upcomingDates(plan).length" class="lp-date-list"><time v-for="date in upcomingDates(plan)" :key="date">{{ date }}</time></div><p v-else>此计划没有尚未开始的复习日期。</p></div>
          <div><strong>计划来源</strong><p v-if="plan.noteId">关联笔记：{{ sourceNoteTitle(plan) }}<span v-if="noteById.get(String(plan.noteId))?.notebookName"> · {{ noteById.get(String(plan.noteId)).notebookName }}</span></p><p v-else>{{ sourceLabel(plan) }}，未关联具体笔记。</p><button v-if="plan.noteId" class="lp-source-link" @click="openSourceNote(plan.noteId)"><BookOpen :size="15" />打开来源笔记</button></div>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.lp-page{height:100%;overflow:auto;padding:42px clamp(24px,5vw,76px);background:linear-gradient(135deg,#fbf8f2,#f6efe5);color:#3c2b20}.lp-header{display:flex;justify-content:space-between;align-items:flex-start;max-width:920px;margin:0 auto 30px}.lp-kicker{font-size:11px;letter-spacing:0;color:#b46a35;font-weight:700;margin:0 0 8px}.lp-header h1{font-family:Georgia,serif;font-size:32px;margin:0 0 8px;color:#302017}.lp-subtitle{margin:0;color:#846f5e;font-size:14px}.lp-refresh{display:flex;gap:7px;align-items:center;border:1px solid #dfcfbd;background:#fffaf4;color:#75583f;border-radius:8px;padding:8px 12px;cursor:pointer}.lp-tabs,.lp-card{max-width:920px;margin:0 auto}.lp-tabs{display:flex;gap:4px;border-bottom:1px solid #e2d5c7;margin-bottom:18px}.lp-tabs button{display:flex;align-items:center;gap:7px;border:0;background:none;color:#987c65;padding:11px 16px;border-bottom:2px solid transparent;cursor:pointer}.lp-tabs button.active{color:#a65320;border-bottom-color:#c26a26;font-weight:650}.lp-tabs span{font-size:11px;background:#eadbc9;border-radius:10px;padding:1px 6px}.lp-card{background:#fffdf9;border:1px solid #eadfd2;border-radius:8px;box-shadow:0 12px 35px #74502d0d;padding:24px}.lp-card-head h2{margin:0 0 6px;font-size:18px;color:#3c2b20}.lp-card-head p{margin:0;color:#9b8673;font-size:13px}.lp-add{display:flex;gap:10px;margin:22px 0}.lp-add input{flex:1;border:1px solid #e3d5c7;border-radius:8px;padding:10px 12px;background:#fff;color:#4a382b;outline:none}.lp-add input:focus{border-color:#c9834e}.lp-add button{display:flex;align-items:center;gap:5px;border:0;background:#b96028;color:#fff;border-radius:8px;padding:0 14px;cursor:pointer}.lp-row,.lp-plan{display:flex;align-items:center;gap:12px;padding:14px 4px;border-top:1px solid #f0e8df}.lp-check,.lp-delete{border:0;background:none;color:#b87843;cursor:pointer;display:grid;place-items:center}.lp-delete{color:#b8a291}.lp-delete:hover{color:#b24e36}.lp-row-main{flex:1;display:flex;flex-direction:column;gap:4px;min-width:0}.lp-row-main strong{font-size:14px;color:#4b382b}.lp-row-main span,.lp-row-main small{font-size:12px;color:#9a8572}.lp-row-main small{color:#b09580}.lp-row.is-done strong{text-decoration:line-through;color:#a69689}.lp-row.is-done .lp-check{color:#6d9c72}.lp-row em{font-style:normal;font-size:11px;padding:3px 7px;border-radius:5px}.priority-1{background:#e6f1e7;color:#59815e}.priority-2{background:#f7ebd8;color:#aa713c}.priority-3{background:#f8e1dc;color:#ac5546}.lp-empty{text-align:center;padding:42px 10px;color:#aa9786;font-size:13px}.lp-completed-title{font-size:12px;color:#a48e7b;padding:18px 4px 6px}.lp-plan-icon{width:36px;height:36px;border-radius:8px;background:#f4e5d5;color:#b6652c;display:grid;place-items:center}.lp-status{border:1px solid #e0c6aa;border-radius:7px;background:#fff7ed;color:#af692f;padding:6px 10px;font-size:12px;cursor:pointer}.lp-status.done{background:#e7f1e8;color:#5f8663;border-color:#cce0cf}.lp-spin{animation:spin 1s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
.lp-page button:disabled{cursor:default;opacity:.55}.lp-row-main strong,.lp-row-main span{overflow-wrap:anywhere}
.lp-plan-editor{display:grid;gap:18px;margin:22px 0 26px;padding:18px;border:1px solid #eadfd2;border-radius:8px;background:#fcf9f4}
.lp-form-grid,.lp-schedule-fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}
.lp-field{display:flex;flex-direction:column;gap:7px;min-width:0;color:#715b48;font-size:12px}
.lp-field>span:first-child,.lp-field legend{font-weight:650;color:#614832}
.lp-field input,.lp-field select{width:100%;min-width:0;box-sizing:border-box;border:1px solid #dfd1c2;border-radius:7px;background:#fff;padding:9px 10px;color:#46372a;font:inherit;outline:none}
.lp-field input:focus,.lp-field select:focus{border-color:#bd7847;box-shadow:0 0 0 2px #bd784722}
.lp-field-wide{grid-column:1/-1}
.lp-field-error{color:#a94f3e;line-height:1.5}
.lp-frequency-field{grid-column:span 2;min-width:0;margin:0;padding:0;border:0}
.lp-frequency-field legend{margin-bottom:7px;font-size:12px}
.lp-frequency{display:flex;gap:4px;padding:3px;border-radius:8px;background:#eee4d8;width:max-content;max-width:100%}
.lp-frequency button{border:0;border-radius:6px;background:transparent;color:#7e6a56;padding:7px 12px;font:inherit;cursor:pointer}
.lp-frequency button.active{background:#fff;color:#9b511f;box-shadow:0 1px 3px #5236181c;font-weight:650}
.lp-number-input{display:flex;align-items:center;gap:8px}.lp-number-input input{max-width:120px}.lp-number-input span{color:#927b66}
.lp-editor-actions{display:flex;align-items:center;justify-content:space-between;gap:18px}.lp-editor-actions p{margin:0;max-width:620px;color:#95816e;font-size:12px;line-height:1.6}.lp-editor-actions>div{display:flex;gap:8px;flex-shrink:0}
.lp-save,.lp-cancel,.lp-source-link{display:flex;align-items:center;justify-content:center;gap:6px;border-radius:7px;padding:8px 12px;font:inherit;cursor:pointer}
.lp-save{border:0;background:#b96028;color:white}.lp-cancel{border:1px solid #ded0c1;background:#fff;color:#755f4a}
.lp-plan-item{border-top:1px solid #f0e8df}.lp-plan-item .lp-plan{align-items:flex-start;border:0}.lp-plan-title{display:flex;align-items:center;gap:7px;width:max-content;max-width:100%;padding:0;border:0;background:none;color:#4b382b;font-size:14px;font-weight:650;text-align:left;cursor:pointer}.lp-plan-title svg{flex:none;color:#9c8068;transition:transform .16s}.lp-plan-title svg.rotated{transform:rotate(180deg)}
.lp-origin{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.lp-origin button{padding:0;border:0;background:none;color:#a55a2b;font:inherit;text-decoration:underline;text-underline-offset:2px;cursor:pointer}.lp-date-preview{display:flex;align-items:flex-start;gap:6px;margin-top:5px;color:#607c62;font-size:12px;line-height:1.5}.lp-date-preview svg{flex:none;margin-top:1px}
.lp-plan-actions{display:flex;align-items:center;gap:8px;flex:none}.lp-icon-action{width:32px;height:32px;display:grid;place-items:center;border:1px solid #e6dacc;border-radius:7px;background:#fff;color:#85674d;cursor:pointer}.lp-plan-details{display:grid;grid-template-columns:1fr 1fr;gap:20px;padding:14px 16px 18px 52px;background:#faf6f0;border-radius:0 0 8px 8px;color:#725d49;font-size:12px;line-height:1.6}.lp-plan-details strong{display:block;margin-bottom:6px;color:#574331}.lp-plan-details p{margin:0 0 8px}.lp-date-list{display:flex;flex-wrap:wrap;gap:6px}.lp-date-list time{padding:4px 8px;border-radius:5px;background:#eaf0e7;color:#527357;font-variant-numeric:tabular-nums}.lp-source-link{padding:0;border:0;background:none;color:#a55a2b}
@media(max-width:640px){.lp-page{padding:24px 16px}.lp-header{gap:12px;margin-bottom:22px}.lp-header h1{font-size:27px}.lp-subtitle{font-size:12px;line-height:1.6}.lp-refresh{flex-shrink:0}.lp-card{padding:16px}.lp-add{flex-direction:column}.lp-add input{min-width:0}.lp-add button{justify-content:center;padding:10px 14px}.lp-plan{flex-wrap:wrap}.lp-plan .lp-row-main{flex-basis:calc(100% - 60px)}.lp-tabs button{padding:10px}.lp-row-main small{overflow-wrap:anywhere}.lp-plan-editor{padding:14px}.lp-form-grid,.lp-schedule-fields{grid-template-columns:minmax(0,1fr)}.lp-field-wide,.lp-frequency-field{grid-column:auto}.lp-editor-actions{align-items:flex-start;flex-direction:column}.lp-editor-actions>div{width:100%;justify-content:flex-end}.lp-plan-actions{margin-left:48px}.lp-plan-details{grid-template-columns:1fr;padding:14px}.lp-frequency{width:100%}.lp-frequency button{flex:1;padding-inline:6px}}
</style>
