import { apiFetch } from './apiClient.js'

/**
 * Note square APIs
 */

export async function getNoteSquare(params = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page || 1))
  query.set('size', String(params.size || 10))
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.category) query.set('category', params.category)
  if (params.sortBy) query.set('sortBy', params.sortBy)
  if (params.sortOrder) query.set('sortOrder', params.sortOrder)
  return apiFetch(`/api/note-square?${query.toString()}`, { method: 'GET' })
}

export async function getNoteSquareCategories() {
  return apiFetch('/api/note-square/categories', { method: 'GET' })
}

export async function publishNoteSquare(data) {
  return apiFetch('/api/note-square/publish', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export async function unpublishNoteSquare(noteId) {
  return apiFetch(`/api/note-square/${noteId}/unpublish`, { method: 'POST' })
}

export async function getNoteSquareDetail(noteId) {
  return apiFetch(`/api/note-square/${noteId}`, { method: 'GET' })
}

export async function getNoteSquareComments(noteId) {
  return apiFetch(`/api/note-square/${noteId}/comments`, { method: 'GET' })
}

export async function createNoteSquareComment(noteId, data) {
  return apiFetch(`/api/note-square/${noteId}/comments`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export async function deleteNoteSquareComment(commentId) {
  return apiFetch(`/api/note-square/comments/${commentId}`, { method: 'DELETE' })
}

export async function likeNoteSquare(noteId) {
  return apiFetch(`/api/note-square/${noteId}/like`, { method: 'POST' })
}

export async function unlikeNoteSquare(noteId) {
  return apiFetch(`/api/note-square/${noteId}/like`, { method: 'DELETE' })
}

export async function favoriteNoteSquare(noteId) {
  return apiFetch(`/api/note-square/${noteId}/favorite`, { method: 'POST' })
}

export async function unfavoriteNoteSquare(noteId) {
  return apiFetch(`/api/note-square/${noteId}/favorite`, { method: 'DELETE' })
}

export async function getNoteSquareFavorites(params = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page || 1))
  query.set('size', String(params.size || 10))
  if (params.keyword) query.set('keyword', params.keyword)
  return apiFetch(`/api/note-square/favorites?${query.toString()}`, { method: 'GET' })
}

export async function getNoteSquareLikes(params = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page || 1))
  query.set('size', String(params.size || 10))
  if (params.keyword) query.set('keyword', params.keyword)
  return apiFetch(`/api/note-square/likes?${query.toString()}`, { method: 'GET' })
}

export async function getNoteSquareMyNotes(params = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page || 1))
  query.set('size', String(params.size || 10))
  if (params.keyword) query.set('keyword', params.keyword)
  return apiFetch(`/api/note-square/my?${query.toString()}`, { method: 'GET' })
}

export async function streamNoteSquareSearchSummary(params, onChunk, signal) {
  const token = localStorage.getItem('satoken') || ''

  const response = await fetch('/api/note-square/search-summary', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'satoken': token
    },
    body: JSON.stringify(params),
    signal
  })

  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem('satoken')
      throw new Error('未授权，请重新登录')
    }
    throw new Error(`HTTP错误: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const content = line.substring(5)
          if (content.length > 0) {
            const decodedContent = content.replace(/\[NL\]/g, '\n')
            onChunk(decodedContent)
          }
        }
      }
    }

    if (buffer.trim()) {
      if (buffer.startsWith('data:')) {
        const content = buffer.substring(5)
        if (content.length > 0) {
          const decodedContent = content.replace(/\[NL\]/g, '\n')
          onChunk(decodedContent)
        }
      }
    }
  } finally {
    reader.releaseLock()
  }
}
