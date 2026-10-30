import request from '@/utils/http'

export function fetchSearchTemplates(params: Api.Template.TemplateSearchParams) {
  return request.get<Api.Common.PageResult<Api.Template.TemplateVO>>({
    url: '/api/templates',
    params
  })
}

export function fetchGetTemplateDetail(templateId: number) {
  return request.get<Api.Template.TemplateVO>({
    url: `/api/templates/${templateId}`
  })
}

export function fetchGetRelatedTemplates(templateId: number, limit: number = 3) {
  return request.get<Api.Template.TemplateVO[]>({
    url: `/api/templates/${templateId}/related`,
    params: { limit }
  })
}

export function fetchGetTemplateComments(templateId: number) {
  return request.get<Api.Template.TemplateCommentVO[]>({
    url: `/api/templates/${templateId}/comments`
  })
}

export function fetchAdminCommentList(params: Api.Template.AdminCommentListParams) {
  return request.get<Api.Common.PageResult<Api.Template.TemplateCommentVO>>({
    url: '/api/templates/admin/comments',
    params
  })
}

export function fetchAdminDeleteComment(commentId: number, sourceType: 'TEMPLATE' | 'NOTE') {
  return request.del<void>({
    url: `/api/templates/admin/comments/${commentId}`,
    params: { sourceType },
    showSuccessMessage: true
  })
}

export function fetchUseTemplate(templateId: number) {
  return request.post<void>({
    url: `/api/templates/${templateId}/use`,
    showSuccessMessage: true
  })
}

export function fetchLikeTemplateComment(commentId: number) {
  return request.post<void>({
    url: `/api/templates/comments/${commentId}/like`,
    showSuccessMessage: true
  })
}

export function fetchDeleteTemplateComment(commentId: number) {
  return request.del<void>({
    url: `/api/templates/comments/${commentId}`,
    showSuccessMessage: true
  })
}

export function fetchCreateTemplate(params: Api.Template.TemplateCreateDTO) {
  return request.post<Api.Template.TemplateVO>({
    url: '/api/templates',
    params,
    showSuccessMessage: true
  })
}

export function fetchUpdateTemplate(templateId: number, params: Api.Template.TemplateCreateDTO) {
  return request.put<Api.Template.TemplateVO>({
    url: `/api/templates/${templateId}`,
    params,
    showSuccessMessage: true
  })
}

export function fetchDeleteTemplate(templateId: number) {
  return request.del<void>({
    url: `/api/templates/${templateId}`,
    showSuccessMessage: true
  })
}
