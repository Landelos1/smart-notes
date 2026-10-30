import request from '@/utils/http'

export function fetchGetNoteSquarePage(params: Api.NoteSquare.NoteSquareListParams) {
  return request.get<Api.Common.PageResult<Api.NoteSquare.NoteSquareVO>>({
    url: '/api/note-square',
    params
  })
}

export function fetchGetNoteSquareDetail(noteId: number) {
  return request.get<Api.NoteSquare.NoteSquareDetailVO>({
    url: `/api/note-square/${noteId}`
  })
}

export function fetchPublishNoteSquare(params: Api.NoteSquare.NoteSquarePublishDTO) {
  return request.post<Api.NoteSquare.NoteSquareDetailVO>({
    url: '/api/note-square/publish',
    params,
    showSuccessMessage: true
  })
}

export function fetchUnpublishNoteSquare(noteId: number) {
  return request.post<void>({
    url: `/api/note-square/${noteId}/unpublish`,
    showSuccessMessage: true
  })
}
