import request from '@/utils/http'

export function fetchCreateNotebook(params: Api.Notebook.NotebookCreateDTO) {
  return request.post<Api.Notebook.NotebookVO>({
    url: '/api/notebooks',
    params,
    showSuccessMessage: true
  })
}

export function fetchGetUserNotebooks() {
  return request.get<Api.Notebook.NotebookVO[]>({
    url: '/api/notebooks'
  })
}

export function fetchGetNotebookDetail(notebookId: number) {
  return request.get<Api.Notebook.NotebookVO>({
    url: `/api/notebooks/${notebookId}`
  })
}

export function fetchUpdateNotebook(notebookId: number, params: Api.Notebook.NotebookCreateDTO) {
  return request.put<Api.Notebook.NotebookVO>({
    url: `/api/notebooks/${notebookId}`,
    params,
    showSuccessMessage: true
  })
}

export function fetchDeleteNotebook(notebookId: number) {
  return request.del<void>({
    url: `/api/notebooks/${notebookId}`,
    showSuccessMessage: true
  })
}

export function fetchCreateSharedKnowledgeBase(params: Api.SharedKnowledgeBase.SharedKnowledgeBaseCreateDTO) {
  return request.post<Api.SharedKnowledgeBase.SharedKnowledgeBaseVO>({
    url: '/api/shared-knowledge-base',
    params,
    showSuccessMessage: true
  })
}

export function fetchGetMySharedKnowledgeBases() {
  return request.get<Api.SharedKnowledgeBase.SharedKnowledgeBaseVO[]>({
    url: '/api/shared-knowledge-base/my-created'
  })
}

export function fetchGetJoinedSharedKnowledgeBases() {
  return request.get<Api.SharedKnowledgeBase.SharedKnowledgeBaseVO[]>({
    url: '/api/shared-knowledge-base/my-joined'
  })
}

export function fetchGetAllSharedKnowledgeBasesForAdmin() {
  return request.get<Api.SharedKnowledgeBase.SharedKnowledgeBaseVO[]>({
    url: '/api/shared-knowledge-base/admin/list'
  })
}

export function fetchUpdateSharedKnowledgeBase(
  knowledgeBaseId: number,
  params: Api.SharedKnowledgeBase.SharedKnowledgeBaseUpdateDTO
) {
  return request.put<Api.SharedKnowledgeBase.SharedKnowledgeBaseVO>({
    url: `/api/shared-knowledge-base/${knowledgeBaseId}`,
    params,
    showSuccessMessage: true
  })
}

export function fetchDeleteSharedKnowledgeBase(knowledgeBaseId: number) {
  return request.del<void>({
    url: `/api/shared-knowledge-base/${knowledgeBaseId}`,
    showSuccessMessage: true
  })
}

export function fetchGetKnowledgeBaseFiles(
  knowledgeBaseId: number,
  params?: { page?: number; size?: number; keyword?: string }
) {
  return request.get<Api.Common.PageResult<Api.SharedKnowledgeBase.KnowledgeBaseFileVO>>({
    url: `/api/shared-knowledge-base/${knowledgeBaseId}/files`,
    params
  })
}

export function fetchUploadSharedKnowledgeBaseFile(knowledgeBaseId: number, formData: FormData) {
  return request.post<Api.SharedKnowledgeBase.KnowledgeBaseFileVO>({
    url: `/api/shared-knowledge-base/${knowledgeBaseId}/files/upload`,
    params: formData,
    showSuccessMessage: true
  })
}

export function fetchDeleteKnowledgeBaseFile(knowledgeBaseId: number, fileId: number) {
  return request.del<void>({
    url: `/api/shared-knowledge-base/${knowledgeBaseId}/files/${fileId}`,
    showSuccessMessage: true
  })
}

export function fetchRenameKnowledgeBaseFile(knowledgeBaseId: number, fileId: number, newName: string) {
  return request.put<Api.SharedKnowledgeBase.KnowledgeBaseFileVO>({
    url: `/api/shared-knowledge-base/${knowledgeBaseId}/files/${fileId}/rename`,
    params: { newName },
    showSuccessMessage: true
  })
}
