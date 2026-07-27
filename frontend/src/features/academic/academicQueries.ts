import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  bulkUpdateScores,
  cancelAssessment,
  cancelAssignment,
  closeAssignment,
  closeEvaluationPeriod,
  completeAssessment,
  completeLesson,
  createAssessment,
  createAssignment,
  createEvaluationPeriod,
  createLesson,
  deleteMaterial,
  finalizeStudentEvaluation,
  getAssessment,
  getAssessmentScores,
  getAssessments,
  getAssignment,
  getAssignmentSubmissions,
  getAssignments,
  getEvaluationPeriod,
  getEvaluationPeriods,
  getLesson,
  getLessons,
  getMaterials,
  getMyAssessments,
  getMyAssignments,
  getMyEvaluations,
  getMyLessons,
  getMyMaterials,
  getMyProgressReportDocument,
  getMyProgressReports,
  getMyScores,
  getProgressReportDocument,
  getProgressReports,
  getStudentEvaluation,
  getStudentEvaluations,
  gradeSubmission,
  openAssessment,
  publishAssessmentScores,
  publishAssignment,
  publishLesson,
  publishStudentEvaluation,
  reopenEvaluationPeriod,
  reopenStudentEvaluation,
  submitMyAssignment,
  updateAssessment,
  updateAssignment,
  updateEvaluationPeriod,
  updateLesson,
  uploadMaterial,
  upsertStudentEvaluation,
} from './academicApi'
import type {
  AssessmentSearchParams,
  AssignmentSearchParams,
  BulkScoreUpdatePayload,
  CreateAssessmentPayload,
  CreateEvaluationPeriodPayload,
  EvaluationPeriodSearchParams,
  GradeSubmissionPayload,
  LessonSearchParams,
  ProgressReportSearchParams,
  ReopenEvaluationPayload,
  UpdateAssessmentPayload,
  UpdateAssignmentPayload,
  UpdateEvaluationPeriodPayload,
  UpdateLessonPayload,
  UploadMaterialParams,
  UpsertStudentEvaluationPayload,
} from './academicTypes'

export const academicKeys = {
  all: ['academic'] as const,
  lessons: (params: LessonSearchParams) => ['academic', 'lessons', params] as const,
  lesson: (id: number) => ['academic', 'lesson', id] as const,
  materials: (classroomId: number, visibility?: string) =>
    ['academic', 'materials', classroomId, visibility] as const,
  assignments: (params: AssignmentSearchParams) => ['academic', 'assignments', params] as const,
  assignment: (id: number) => ['academic', 'assignment', id] as const,
  submissions: (assignmentId: number) => ['academic', 'submissions', assignmentId] as const,
  assessments: (params: AssessmentSearchParams) => ['academic', 'assessments', params] as const,
  assessment: (id: number) => ['academic', 'assessment', id] as const,
  scores: (assessmentId: number) => ['academic', 'scores', assessmentId] as const,
  evaluationPeriods: (params: EvaluationPeriodSearchParams) =>
    ['academic', 'evaluation-periods', params] as const,
  evaluationPeriod: (id: number) => ['academic', 'evaluation-period', id] as const,
  evaluations: (periodId: number, classroomId: number) =>
    ['academic', 'evaluations', periodId, classroomId] as const,
  evaluation: (id: number) => ['academic', 'evaluation', id] as const,
  progressReports: (params: ProgressReportSearchParams) =>
    ['academic', 'progress-reports', params] as const,
  progressReportDocument: (id: number) => ['academic', 'progress-report-document', id] as const,
  myLessons: ['academic', 'me', 'lessons'] as const,
  myMaterials: ['academic', 'me', 'materials'] as const,
  myAssignments: ['academic', 'me', 'assignments'] as const,
  myAssessments: ['academic', 'me', 'assessments'] as const,
  myScores: ['academic', 'me', 'scores'] as const,
  myEvaluations: ['academic', 'me', 'evaluations'] as const,
  myProgressReports: ['academic', 'me', 'progress-reports'] as const,
  myProgressReportDocument: (id: number) =>
    ['academic', 'me', 'progress-report-document', id] as const,
}

export function useLessons(params: LessonSearchParams, enabled = true) {
  return useQuery({
    queryKey: academicKeys.lessons(params),
    queryFn: () => getLessons(params),
    enabled,
  })
}

export function useLesson(id?: number) {
  return useQuery({
    queryKey: academicKeys.lesson(id!),
    queryFn: () => getLesson(id!),
    enabled: id != null && Number.isFinite(id),
  })
}

export function useCreateLesson() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createLesson,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useUpdateLesson() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateLessonPayload }) =>
      updateLesson(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function usePublishLesson() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: publishLesson,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useCompleteLesson() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: completeLesson,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useMaterials(classroomId?: number, visibility?: string) {
  return useQuery({
    queryKey: academicKeys.materials(classroomId!, visibility),
    queryFn: () => getMaterials(classroomId!, visibility),
    enabled: classroomId != null && Number.isFinite(classroomId),
  })
}

export function useUploadMaterial() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (params: UploadMaterialParams) => uploadMaterial(params),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useDeleteMaterial() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteMaterial,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useAssignments(params: AssignmentSearchParams, enabled = true) {
  return useQuery({
    queryKey: academicKeys.assignments(params),
    queryFn: () => getAssignments(params),
    enabled,
  })
}

export function useAssignment(id?: number) {
  return useQuery({
    queryKey: academicKeys.assignment(id!),
    queryFn: () => getAssignment(id!),
    enabled: id != null && Number.isFinite(id),
  })
}

export function useCreateAssignment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createAssignment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useUpdateAssignment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateAssignmentPayload }) =>
      updateAssignment(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function usePublishAssignment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: publishAssignment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useCloseAssignment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: closeAssignment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useCancelAssignment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: cancelAssignment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useAssignmentSubmissions(assignmentId?: number) {
  return useQuery({
    queryKey: academicKeys.submissions(assignmentId!),
    queryFn: () => getAssignmentSubmissions(assignmentId!),
    enabled: assignmentId != null && Number.isFinite(assignmentId),
  })
}

export function useGradeSubmission() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: GradeSubmissionPayload }) =>
      gradeSubmission(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useAssessments(params: AssessmentSearchParams, enabled = true) {
  return useQuery({
    queryKey: academicKeys.assessments(params),
    queryFn: () => getAssessments(params),
    enabled,
  })
}

export function useAssessment(id?: number) {
  return useQuery({
    queryKey: academicKeys.assessment(id!),
    queryFn: () => getAssessment(id!),
    enabled: id != null && Number.isFinite(id),
  })
}

export function useCreateAssessment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateAssessmentPayload) => createAssessment(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useUpdateAssessment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateAssessmentPayload }) =>
      updateAssessment(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useOpenAssessment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: openAssessment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useCompleteAssessment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: completeAssessment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useCancelAssessment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: cancelAssessment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useAssessmentScores(assessmentId?: number) {
  return useQuery({
    queryKey: academicKeys.scores(assessmentId!),
    queryFn: () => getAssessmentScores(assessmentId!),
    enabled: assessmentId != null && Number.isFinite(assessmentId),
  })
}

export function useBulkUpdateScores() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      assessmentId,
      payload,
    }: {
      assessmentId: number
      payload: BulkScoreUpdatePayload
    }) => bulkUpdateScores(assessmentId, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function usePublishAssessmentScores() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: publishAssessmentScores,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useEvaluationPeriods(params: EvaluationPeriodSearchParams, enabled = true) {
  return useQuery({
    queryKey: academicKeys.evaluationPeriods(params),
    queryFn: () => getEvaluationPeriods(params),
    enabled,
  })
}

export function useEvaluationPeriod(id?: number) {
  return useQuery({
    queryKey: academicKeys.evaluationPeriod(id!),
    queryFn: () => getEvaluationPeriod(id!),
    enabled: id != null && Number.isFinite(id),
  })
}

export function useCreateEvaluationPeriod() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateEvaluationPeriodPayload) => createEvaluationPeriod(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useUpdateEvaluationPeriod() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateEvaluationPeriodPayload }) =>
      updateEvaluationPeriod(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useCloseEvaluationPeriod() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: closeEvaluationPeriod,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useReopenEvaluationPeriod() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ReopenEvaluationPayload }) =>
      reopenEvaluationPeriod(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useStudentEvaluations(periodId?: number, classroomId?: number) {
  return useQuery({
    queryKey: academicKeys.evaluations(periodId!, classroomId!),
    queryFn: () => getStudentEvaluations(periodId!, classroomId!),
    enabled:
      periodId != null &&
      classroomId != null &&
      Number.isFinite(periodId) &&
      Number.isFinite(classroomId),
  })
}

export function useStudentEvaluation(id?: number) {
  return useQuery({
    queryKey: academicKeys.evaluation(id!),
    queryFn: () => getStudentEvaluation(id!),
    enabled: id != null && Number.isFinite(id),
  })
}

export function useUpsertStudentEvaluation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: UpsertStudentEvaluationPayload) => upsertStudentEvaluation(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function usePublishStudentEvaluation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: publishStudentEvaluation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useFinalizeStudentEvaluation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: finalizeStudentEvaluation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useReopenStudentEvaluation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ReopenEvaluationPayload }) =>
      reopenStudentEvaluation(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: academicKeys.all }),
  })
}

export function useProgressReports(params: ProgressReportSearchParams, enabled = true) {
  return useQuery({
    queryKey: academicKeys.progressReports(params),
    queryFn: () => getProgressReports(params),
    enabled,
  })
}

export function useProgressReportDocument(id?: number) {
  return useQuery({
    queryKey: academicKeys.progressReportDocument(id!),
    queryFn: () => getProgressReportDocument(id!),
    enabled: id != null && Number.isFinite(id),
  })
}

export function useMyLessons(enabled = true) {
  return useQuery({
    queryKey: academicKeys.myLessons,
    queryFn: getMyLessons,
    enabled,
  })
}

export function useMyMaterials(enabled = true) {
  return useQuery({
    queryKey: academicKeys.myMaterials,
    queryFn: getMyMaterials,
    enabled,
  })
}

export function useMyAssignments(enabled = true) {
  return useQuery({
    queryKey: academicKeys.myAssignments,
    queryFn: getMyAssignments,
    enabled,
  })
}

export function useSubmitMyAssignment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      assignmentId,
      textAnswer,
      files,
    }: {
      assignmentId: number
      textAnswer?: string
      files?: File[]
    }) => submitMyAssignment(assignmentId, { textAnswer, files }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: academicKeys.myAssignments })
      queryClient.invalidateQueries({ queryKey: academicKeys.all })
    },
  })
}

export function useMyAssessments(enabled = true) {
  return useQuery({
    queryKey: academicKeys.myAssessments,
    queryFn: getMyAssessments,
    enabled,
  })
}

export function useMyScores(enabled = true) {
  return useQuery({
    queryKey: academicKeys.myScores,
    queryFn: getMyScores,
    enabled,
  })
}

export function useMyEvaluations(enabled = true) {
  return useQuery({
    queryKey: academicKeys.myEvaluations,
    queryFn: getMyEvaluations,
    enabled,
  })
}

export function useMyProgressReports(enabled = true) {
  return useQuery({
    queryKey: academicKeys.myProgressReports,
    queryFn: getMyProgressReports,
    enabled,
  })
}

export function useMyProgressReportDocument(id?: number) {
  return useQuery({
    queryKey: academicKeys.myProgressReportDocument(id!),
    queryFn: () => getMyProgressReportDocument(id!),
    enabled: id != null && Number.isFinite(id),
  })
}
