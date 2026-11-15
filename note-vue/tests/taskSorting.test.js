import test from 'node:test'
import assert from 'node:assert/strict'
import { getTaskNotebookLabel, sortTasks } from '../src/utils/taskSorting.js'

const tasks = [
  { id: 1, noteId: 11, priority: 3 },
  { id: 2, noteId: 12, priority: 1 },
  { id: 3, noteId: 11, priority: 1 },
  { id: 4, priority: 3 }
]
const notesById = new Map([
  ['11', { notebookName: 'B' }],
  ['12', { notebookName: 'A' }]
])

test('default order keeps the order returned by the API', () => {
  assert.deepEqual(sortTasks(tasks, 'default', notesById).map(task => task.id), [1, 2, 3, 4])
})

test('notebook sorting groups tasks by source notebook, then higher priority', () => {
  assert.deepEqual(sortTasks(tasks, 'notebook', notesById).map(task => task.id), [2, 1, 3, 4])
})

test('priority sorting supports both directions', () => {
  assert.deepEqual(sortTasks(tasks, 'priority-desc', notesById).map(task => task.id), [1, 4, 2, 3])
  assert.deepEqual(sortTasks(tasks, 'priority-asc', notesById).map(task => task.id), [2, 3, 1, 4])
})

test('tasks without a note or loaded notebook receive explicit category labels', () => {
  assert.equal(getTaskNotebookLabel({ id: 4 }, notesById), '未关联笔记')
  assert.equal(getTaskNotebookLabel({ id: 5, noteId: 99 }, notesById), '未分类笔记本')
})
