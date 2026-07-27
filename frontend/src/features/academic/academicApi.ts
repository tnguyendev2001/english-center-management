import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  Assessment,
  AssessmentScore,
  AssessmentSearchParams,
  Assignment,
  AssignmentSearchParams,
  AssignmentSubmission,
  BulkScoreUpdatePayload,
  CreateAssessmentPayload,
  CreateAssignmentPayload,
  CreateEvaluationPeriodPayload,
  CreateLessonPayload,
  EvaluationPeriod,
  EvaluationPeriodSearchParams,
  GradeSubmissionPayload,
  LearningMaterial,
  LessonRecord,
  LessonSearchParams,
  ProgressReport,
  ProgressReportDocument,
  ProgressReportSearchParams,
  ReopenEvaluationPayload,
  StudentEvaluation,
  UpdateAssessmentPayload,
  UpdateAssignmentPayload,
  UpdateEvaluationPeriodPayload,
  UpdateLessonPayload,
  UploadMaterialParams,
  UpsertStudentEvaluationPayload,
} from './academicTypes'

function toFormData(params: UploadMaterialParams) {
  const formData = new FormData()
  formData.append('file', params.file)
  formData.append('title', params.title)
  formData.append('classroomId', String(params.classroomId))
  if (params.description) formData.append('description', params.description)
  if (params.lessonRecordId != null) formData.append('lessonRecordId', String(params.lessonRecordId))
  if (params.assignmentId != null) formData.append('assignmentId', String(params.assignmentId))
  if (params.assessmentId != null) formData.append('assessmentId', String(params.assessmentId))
  if (params.visibility) formData.append('visibility', params.visibility)
  return formData
}

export function triggerBlobDownload(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(url)
}

// --- Lessons ---

export async function getLessons(params: LessonSearchParams = {}) {
  const response = await httpClient.get<ApiResponse<LessonRecord[]>>('/academic/lessons', { params })
  return response.data
}

export async function getLesson(id: number) {
  const response = await httpClient.get<ApiResponse<LessonRecord>>(`/academic/lessons/${id}`)
  return response.data.data
}

export async function createLesson(payload: CreateLessonPayload) {
  const response = await httpClient.post<ApiResponse<LessonRecord>>('/academic/lessons', payload)
  return response.data.data
}

export async function updateLesson(id: number, payload: UpdateLessonPayload) {
  const response = await httpClient.put<ApiResponse<LessonRecord>>(`/academic/lessons/${id}`, payload)
  return response.data.data
}

export async function publishLesson(id: number) {
  const response = await httpClient.post<ApiResponse<LessonRecord>>(`/academic/lessons/${id}/publish`)
  return response.data.data
}

export async function completeLesson(id: number) {
  const response = await httpClient.post<ApiResponse<LessonRecord>>(`/academic/lessons/${id}/complete`)
  return response.data.data
}

// --- Materials ---

export async function getMaterials(classroomId: number, visibility?: string) {
  const response = await httpClient.get<ApiResponse<LearningMaterial[]>>('/academic/materials', {
    params: { classroomId, visibility },
  })
  return response.data.data
}

export async function uploadMaterial(params: UploadMaterialParams) {
  const response = await httpClient.post<ApiResponse<LearningMaterial>>(
    '/academic/materials',
    toFormData(params),
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data.data
}

export async function downloadMaterial(id: number, fileName = 'material') {
  const response = await httpClient.get<Blob>(`/academic/materials/${id}/download`, {
    responseType: 'blob',
  })
  triggerBlobDownload(response.data, fileName)
}

export async function deleteMaterial(id: number) {
  const response = await httpClient.delete<ApiResponse<null>>(`/academic/materials/${id}`)
  return response.data.data
}

// --- Assignments ---

export async function getAssignments(params: AssignmentSearchParams = {}) {
  const response = await httpClient.get<ApiResponse<Assignment[]>>('/academic/assignments', { params })
  return response.data
}

export async function getAssignment(id: number) {
  const response = await httpClient.get<ApiResponse<Assignment>>(`/academic/assignments/${id}`)
  return response.data.data
}

export async function createAssignment(payload: CreateAssignmentPayload) {
  const response = await httpClient.post<ApiResponse<Assignment>>('/academic/assignments', payload)
  return response.data.data
}

export async function updateAssignment(id: number, payload: UpdateAssignmentPayload) {
  const response = await httpClient.put<ApiResponse<Assignment>>(
    `/academic/assignments/${id}`,
    payload,
  )
  return response.data.data
}

export async function publishAssignment(id: number) {
  const response = await httpClient.post<ApiResponse<Assignment>>(
    `/academic/assignments/${id}/publish`,
  )
  return response.data.data
}

export async function closeAssignment(id: number) {
  const response = await httpClient.post<ApiResponse<Assignment>>(`/academic/assignments/${id}/close`)
  return response.data.data
}

export async function cancelAssignment(id: number) {
  const response = await httpClient.post<ApiResponse<Assignment>>(
    `/academic/assignments/${id}/cancel`,
  )
  return response.data.data
}

export async function getAssignmentSubmissions(assignmentId: number) {
  const response = await httpClient.get<ApiResponse<AssignmentSubmission[]>>(
    `/academic/assignments/${assignmentId}/submissions`,
  )
  return response.data.data
}

export async function gradeSubmission(id: number, payload: GradeSubmissionPayload) {
  const response = await httpClient.put<ApiResponse<AssignmentSubmission>>(
    `/academic/submissions/${id}/grade`,
    payload,
  )
  return response.data.data
}

// --- Assessments & scores ---

export async function getAssessments(params: AssessmentSearchParams = {}) {
  const response = await httpClient.get<ApiResponse<Assessment[]>>('/academic/assessments', {
    params,
  })
  return response.data
}

export async function getAssessment(id: number) {
  const response = await httpClient.get<ApiResponse<Assessment>>(`/academic/assessments/${id}`)
  return response.data.data
}

export async function createAssessment(payload: CreateAssessmentPayload) {
  const response = await httpClient.post<ApiResponse<Assessment>>('/academic/assessments', payload)
  return response.data.data
}

export async function updateAssessment(id: number, payload: UpdateAssessmentPayload) {
  const response = await httpClient.put<ApiResponse<Assessment>>(
    `/academic/assessments/${id}`,
    payload,
  )
  return response.data.data
}

export async function openAssessment(id: number) {
  const response = await httpClient.post<ApiResponse<Assessment>>(`/academic/assessments/${id}/open`)
  return response.data.data
}

export async function completeAssessment(id: number) {
  const response = await httpClient.post<ApiResponse<Assessment>>(
    `/academic/assessments/${id}/complete`,
  )
  return response.data.data
}

export async function cancelAssessment(id: number) {
  const response = await httpClient.post<ApiResponse<Assessment>>(
    `/academic/assessments/${id}/cancel`,
  )
  return response.data.data
}

export async function getAssessmentScores(assessmentId: number) {
  const response = await httpClient.get<ApiResponse<AssessmentScore[]>>(
    `/academic/assessments/${assessmentId}/scores`,
  )
  return response.data.data
}

export async function bulkUpdateScores(assessmentId: number, payload: BulkScoreUpdatePayload) {
  const response = await httpClient.put<ApiResponse<AssessmentScore[]>>(
    `/academic/assessments/${assessmentId}/scores`,
    payload,
  )
  return response.data.data
}

export async function publishAssessmentScores(assessmentId: number) {
  const response = await httpClient.post<ApiResponse<AssessmentScore[]>>(
    `/academic/assessments/${assessmentId}/publish-scores`,
  )
  return response.data.data
}

// --- Evaluation periods & evaluations ---

export async function getEvaluationPeriods(params: EvaluationPeriodSearchParams = {}) {
  const response = await httpClient.get<ApiResponse<EvaluationPeriod[]>>(
    '/academic/evaluation-periods',
    { params },
  )
  return response.data
}

export async function getEvaluationPeriod(id: number) {
  const response = await httpClient.get<ApiResponse<EvaluationPeriod>>(
    `/academic/evaluation-periods/${id}`,
  )
  return response.data.data
}

export async function createEvaluationPeriod(payload: CreateEvaluationPeriodPayload) {
  const response = await httpClient.post<ApiResponse<EvaluationPeriod>>(
    '/academic/evaluation-periods',
    payload,
  )
  return response.data.data
}

export async function updateEvaluationPeriod(id: number, payload: UpdateEvaluationPeriodPayload) {
  const response = await httpClient.put<ApiResponse<EvaluationPeriod>>(
    `/academic/evaluation-periods/${id}`,
    payload,
  )
  return response.data.data
}

export async function closeEvaluationPeriod(id: number) {
  const response = await httpClient.post<ApiResponse<EvaluationPeriod>>(
    `/academic/evaluation-periods/${id}/close`,
  )
  return response.data.data
}

export async function reopenEvaluationPeriod(id: number, payload: ReopenEvaluationPayload) {
  const response = await httpClient.post<ApiResponse<EvaluationPeriod>>(
    `/academic/evaluation-periods/${id}/reopen`,
    payload,
  )
  return response.data.data
}

export async function getStudentEvaluations(periodId: number, classroomId: number) {
  const response = await httpClient.get<ApiResponse<StudentEvaluation[]>>('/academic/evaluations', {
    params: { periodId, classroomId },
  })
  return response.data.data
}

export async function getStudentEvaluation(id: number) {
  const response = await httpClient.get<ApiResponse<StudentEvaluation>>(`/academic/evaluations/${id}`)
  return response.data.data
}

export async function upsertStudentEvaluation(payload: UpsertStudentEvaluationPayload) {
  const response = await httpClient.put<ApiResponse<StudentEvaluation>>(
    '/academic/evaluations',
    payload,
  )
  return response.data.data
}

export async function publishStudentEvaluation(id: number) {
  const response = await httpClient.post<ApiResponse<StudentEvaluation>>(
    `/academic/evaluations/${id}/publish`,
  )
  return response.data.data
}

export async function finalizeStudentEvaluation(id: number) {
  const response = await httpClient.post<ApiResponse<StudentEvaluation>>(
    `/academic/evaluations/${id}/finalize`,
  )
  return response.data.data
}

export async function reopenStudentEvaluation(id: number, payload: ReopenEvaluationPayload) {
  const response = await httpClient.post<ApiResponse<StudentEvaluation>>(
    `/academic/evaluations/${id}/reopen`,
    payload,
  )
  return response.data.data
}

// --- Progress reports ---

export async function getProgressReports(params: ProgressReportSearchParams = {}) {
  const response = await httpClient.get<ApiResponse<ProgressReport[]>>(
    '/academic/progress-reports',
    { params },
  )
  return response.data
}

export async function getProgressReportDocument(id: number) {
  const response = await httpClient.get<ApiResponse<ProgressReportDocument>>(
    `/academic/progress-reports/${id}/document`,
  )
  return response.data.data
}

// --- Student me endpoints ---

export async function getMyLessons() {
  const response = await httpClient.get<ApiResponse<LessonRecord[]>>('/me/lessons')
  return response.data.data
}

export async function getMyMaterials() {
  const response = await httpClient.get<ApiResponse<LearningMaterial[]>>('/me/materials')
  return response.data.data
}

export async function getMyAssignments() {
  const response = await httpClient.get<ApiResponse<Assignment[]>>('/me/assignments')
  return response.data.data
}

export async function getMyAssignment(id: number) {
  const response = await httpClient.get<ApiResponse<Assignment>>(`/me/assignments/${id}`)
  return response.data.data
}

export async function submitMyAssignment(
  assignmentId: number,
  payload: { textAnswer?: string; files?: File[] },
) {
  const formData = new FormData()
  if (payload.textAnswer) formData.append('textAnswer', payload.textAnswer)
  payload.files?.forEach((file) => formData.append('files', file))
  const response = await httpClient.post<ApiResponse<AssignmentSubmission>>(
    `/me/assignments/${assignmentId}/submissions`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data.data
}

export async function getMyAssessments() {
  const response = await httpClient.get<ApiResponse<Assessment[]>>('/me/assessments')
  return response.data.data
}

export async function getMyScores() {
  const response = await httpClient.get<ApiResponse<AssessmentScore[]>>('/me/scores')
  return response.data.data
}

export async function getMyEvaluations() {
  const response = await httpClient.get<ApiResponse<StudentEvaluation[]>>('/me/evaluations')
  return response.data.data
}

export async function getMyProgressReports() {
  const response = await httpClient.get<ApiResponse<ProgressReport[]>>('/me/progress-reports')
  return response.data.data
}

export async function getMyProgressReportDocument(id: number) {
  const response = await httpClient.get<ApiResponse<ProgressReportDocument>>(
    `/me/progress-reports/${id}`,
  )
  return response.data.data
}
