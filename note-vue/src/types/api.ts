export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserInfo {
  id: number
  username: string
  phone: string
  avatarUrl: string
  gender: 0 | 1 | 2
  birthDate: string | null
  createTime: string
  isMember: boolean
  memberExpireTime: string | null
  memberLevelName: string | null
}

export interface TemplateData {
  id: number | string
  title: string
  description: string
  category: string
  thumbnail: string
  content?: string
  downloads: number
  rating: number
  views: number
  author: string
  updatedAt: string
  tags: string[]
}

export interface PreviewFile {
  id: number
  originalFilename: string
  fileExtension: string
  fileSize: number
  mimeType?: string
}
