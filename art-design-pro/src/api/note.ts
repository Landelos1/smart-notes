import request from '@/utils/http'

export function fetchCreateNote(params: Api.Note.NoteCreateDTO) {
  return request.post<Api.Note.NoteDetailVO>({
    url: '/api/notes',
    params,
    showSuccessMessage: true
  })
}

export function fetchGetNotePage(params: Api.Note.NoteListParams) {
  return request.get<Api.Common.PageResult<Api.Note.NoteBriefVO>>({
    url: '/api/notes',
    params
  })
}

export function fetchGetNoteDetail(noteId: number) {
  return request.get<Api.Note.NoteDetailVO>({
    url: `/api/notes/${noteId}`
  })
}

export function fetchUpdateNote(params: Api.Note.NoteUpdateDTO) {
  return request.post<Api.Note.NoteDetailVO>({
    url: '/api/notes/update',
    params,
    showSuccessMessage: true
  })
}

export function fetchDeleteNote(noteId: number) {
  return request.del<void>({
    url: `/api/notes/${noteId}`,
    showSuccessMessage: true
  })
}

export function fetchTogglePinNote(noteId: number) {
  return request.post<void>({
    url: `/api/notes/${noteId}/pin`,
    showSuccessMessage: true
  })
}

export function fetchGetUserNoteTags() {
  return request.get<string[]>({
    url: '/api/notes/tags'
  })
}

export function fetchAutoSaveNote(params: Api.Note.NoteAutoSaveDTO) {
  return request.post<void>({
    url: '/api/notes/autosave',
    params
  })
}

export function fetchAddNoteToKnowledgeBase(noteId: number) {
  return request.post<Record<string, any>>({
    url: `/api/notes/${noteId}/add-to-knowledge-base`,
    showSuccessMessage: true
  })
}
