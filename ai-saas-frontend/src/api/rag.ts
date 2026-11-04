import { request } from './request'

// 知识库列表
export function getKnowledgeBases(params?: Record<string, any>) {
  return request({ url: '/v1/knowledge-bases', method: 'get', params })
}

// 创建知识库
export function createKnowledgeBase(data: Record<string, any>) {
  return request({ url: '/v1/knowledge-bases', method: 'post', data })
}

// 删除知识库
export function deleteKnowledgeBase(id: string) {
  return request({ url: `/v1/knowledge-bases/${id}`, method: 'delete' })
}

// 文档列表
export function getDocuments(kbId: string, params?: Record<string, any>) {
  return request({ url: `/v1/knowledge-bases/${kbId}/documents`, method: 'get', params })
}

// 上传文档（后端为 POST /documents/upload）
export function uploadDocument(kbId: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `/v1/knowledge-bases/${kbId}/documents/upload`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 删除文档
export function deleteDocument(kbId: string, docId: string) {
  return request({ url: `/v1/knowledge-bases/${kbId}/documents/${docId}`, method: 'delete' })
}

// RAG 检索（后端为 POST /api/v1/rag/query）
export function ragSearch(data: Record<string, any>) {
  return request({ url: '/v1/rag/query', method: 'post', data })
}
