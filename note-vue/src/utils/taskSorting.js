function getPriority(task) {
  const priority = Number(task?.priority)
  return Number.isFinite(priority) && priority >= 1 && priority <= 3 ? priority : 2
}

export function getTaskNotebookLabel(task, notesById = new Map()) {
  if (!task?.noteId) return '未关联笔记'
  return notesById.get(String(task.noteId))?.notebookName || '未分类笔记本'
}

function getNotebookSortGroup(task, notesById) {
  if (!task?.noteId) return 2
  return notesById.get(String(task.noteId))?.notebookName ? 0 : 1
}

export function sortTasks(tasks, sortBy, notesById = new Map()) {
  const sorted = [...tasks]
  if (sortBy === 'default') return sorted

  return sorted.sort((left, right) => {
    if (sortBy === 'notebook') {
      const groupOrder = getNotebookSortGroup(left, notesById) - getNotebookSortGroup(right, notesById)
      if (groupOrder) return groupOrder
      const categoryOrder = getTaskNotebookLabel(left, notesById)
        .localeCompare(getTaskNotebookLabel(right, notesById), 'zh-CN')
      if (categoryOrder) return categoryOrder
      return getPriority(right) - getPriority(left)
    }

    const priorityOrder = getPriority(left) - getPriority(right)
    return sortBy === 'priority-asc' ? priorityOrder : -priorityOrder
  })
}
