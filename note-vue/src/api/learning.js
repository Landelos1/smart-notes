import { apiFetch } from './apiClient.js'

export function getTasks() {
  return apiFetch('/api/tasks', { method: 'GET' })
}

export function createTask(data) {
  return apiFetch('/api/tasks', { method: 'POST', body: JSON.stringify(data) })
}

export function updateTask(id, data) {
  return apiFetch(`/api/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export function deleteTask(id) {
  return apiFetch(`/api/tasks/${id}`, { method: 'DELETE' })
}

export function getReviewPlans() {
  return apiFetch('/api/review-plans', { method: 'GET' })
}

export function createReviewPlan(data) {
  return apiFetch('/api/review-plans', { method: 'POST', body: JSON.stringify(data) })
}

export function updateReviewPlan(id, data) {
  return apiFetch(`/api/review-plans/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export function deleteReviewPlan(id) {
  return apiFetch(`/api/review-plans/${id}`, { method: 'DELETE' })
}

export function generateLearningPlan(noteId, replaceExisting = true) {
  return apiFetch(`/api/learning-plans/generate/${noteId}`, {
    method: 'POST',
    body: JSON.stringify({ replaceExisting })
  })
}
