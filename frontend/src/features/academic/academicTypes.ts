export type LessonStatus = 'DRAFT' | 'PUBLISHED' | 'COMPLETED'

export type MaterialVisibility = 'TEACHER_ONLY' | 'CLASS_STUDENTS'

export type AssignmentStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'CANCELED'

export type AssignmentTargetMode = 'ENTIRE_CLASS' | 'SELECTED_STUDENTS'

export type SubmissionStatus = 'SUBMITTED' | 'LATE' | 'GRADED' | 'RETURNED'

export type AssessmentType =
  | 'QUIZ'
  | 'TEST'
  | 'MIDTERM'
  | 'FINAL'
  | 'SPEAKING'
  | 'LISTENING'
  | 'READING'
  | 'WRITING'
  | 'OTHER'

export type AssessmentStatus = 'DRAFT' | 'OPEN' | 'COMPLETED' | 'CANCELED'

export type AssessmentScoreStatus = 'NOT_GRADED' | 'GRADED' | 'ABSENT' | 'EXEMPT'

export type EvaluationPeriodStatus = 'OPEN' | 'CLOSED'

export type StudentEvaluationStatus = 'DRAFT' | 'PUBLISHED' | 'FINALIZED'

export type OverallRating = 'EXCELLENT' | 'GOOD' | 'SATISFACTORY' | 'NEEDS_IMPROVEMENT'

export type ProgressReportStatus = 'DRAFT' | 'PUBLISHED' | 'FINALIZED'

export const LESSON_STATUS_LABELS: Record<LessonStatus, string> = {
  DRAFT: 'Nháp',
  PUBLISHED: 'Đã xuất bản',
  COMPLETED: 'Hoàn thành',
}

export const MATERIAL_VISIBILITY_LABELS: Record<MaterialVisibility, string> = {
  TEACHER_ONLY: 'Chỉ giáo viên',
  CLASS_STUDENTS: 'Học viên trong lớp',
}

export const ASSIGNMENT_STATUS_LABELS: Record<AssignmentStatus, string> = {
  DRAFT: 'Nháp',
  PUBLISHED: 'Đã giao',
  CLOSED: 'Đã đóng',
  CANCELED: 'Đã hủy',
}

export const ASSIGNMENT_TARGET_MODE_LABELS: Record<AssignmentTargetMode, string> = {
  ENTIRE_CLASS: 'Cả lớp',
  SELECTED_STUDENTS: 'Học viên được chọn',
}

export const SUBMISSION_STATUS_LABELS: Record<SubmissionStatus, string> = {
  SUBMITTED: 'Đã nộp',
  LATE: 'Nộp muộn',
  GRADED: 'Đã chấm',
  RETURNED: 'Đã trả bài',
}

export const ASSESSMENT_TYPE_LABELS: Record<AssessmentType, string> = {
  QUIZ: 'Quiz',
  TEST: 'Kiểm tra',
  MIDTERM: 'Giữa kỳ',
  FINAL: 'Cuối kỳ',
  SPEAKING: 'Nói',
  LISTENING: 'Nghe',
  READING: 'Đọc',
  WRITING: 'Viết',
  OTHER: 'Khác',
}

export const ASSESSMENT_STATUS_LABELS: Record<AssessmentStatus, string> = {
  DRAFT: 'Nháp',
  OPEN: 'Đang mở',
  COMPLETED: 'Hoàn thành',
  CANCELED: 'Đã hủy',
}

export const ASSESSMENT_SCORE_STATUS_LABELS: Record<AssessmentScoreStatus, string> = {
  NOT_GRADED: 'Chưa chấm',
  GRADED: 'Đã chấm',
  ABSENT: 'Vắng',
  EXEMPT: 'Miễn',
}

export const EVALUATION_PERIOD_STATUS_LABELS: Record<EvaluationPeriodStatus, string> = {
  OPEN: 'Đang mở',
  CLOSED: 'Đã đóng',
}

export const STUDENT_EVALUATION_STATUS_LABELS: Record<StudentEvaluationStatus, string> = {
  DRAFT: 'Nháp',
  PUBLISHED: 'Đã công bố',
  FINALIZED: 'Đã chốt',
}

export const OVERALL_RATING_LABELS: Record<OverallRating, string> = {
  EXCELLENT: 'Xuất sắc',
  GOOD: 'Tốt',
  SATISFACTORY: 'Đạt',
  NEEDS_IMPROVEMENT: 'Cần cải thiện',
}

export const PROGRESS_REPORT_STATUS_LABELS: Record<ProgressReportStatus, string> = {
  DRAFT: 'Nháp',
  PUBLISHED: 'Đã công bố',
  FINALIZED: 'Đã chốt',
}

export interface LessonRecord {
  id: number
  classSessionId: number
  classroomId: number
  title: string
  objectives?: string | null
  plannedContent?: string | null
  actualContent?: string | null
  vocabulary?: string | null
  grammarTopics?: string | null
  skills?: string | null
  homeworkInstruction?: string | null
  teacherNote?: string | null
  status: LessonStatus
  createdBy?: string | null
  updatedBy?: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateLessonPayload {
  classSessionId: number
  title: string
  objectives?: string | null
  plannedContent?: string | null
  vocabulary?: string | null
  grammarTopics?: string | null
  skills?: string | null
  homeworkInstruction?: string | null
  teacherNote?: string | null
}

export interface UpdateLessonPayload {
  title: string
  objectives?: string | null
  plannedContent?: string | null
  actualContent?: string | null
  vocabulary?: string | null
  grammarTopics?: string | null
  skills?: string | null
  homeworkInstruction?: string | null
  teacherNote?: string | null
}

export interface LessonSearchParams {
  classroomId?: number
  status?: LessonStatus
  keyword?: string
  page?: number
  size?: number
}

export interface LearningMaterial {
  id: number
  title: string
  description?: string | null
  fileName?: string | null
  contentType?: string | null
  fileSize?: number | null
  classroomId: number
  lessonRecordId?: number | null
  assignmentId?: number | null
  assessmentId?: number | null
  visibility: MaterialVisibility
  active?: boolean | null
  uploadedBy?: string | null
  uploadedAt: string
}

export interface UploadMaterialParams {
  title: string
  description?: string
  classroomId: number
  lessonRecordId?: number
  assignmentId?: number
  assessmentId?: number
  visibility?: MaterialVisibility
  file: File
}

export interface Assignment {
  id: number
  title: string
  description?: string | null
  instructions?: string | null
  classroomId: number
  lessonRecordId?: number | null
  assignedDate: string
  dueDate?: string | null
  maxScore?: number | null
  allowSubmission: boolean
  allowLateSubmission: boolean
  targetMode: AssignmentTargetMode
  targetStudentIds: number[]
  status: AssignmentStatus
  createdByTeacherId?: number | null
  createdBy?: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateAssignmentPayload {
  title: string
  description?: string | null
  instructions?: string | null
  classroomId: number
  lessonRecordId?: number | null
  assignedDate: string
  dueDate?: string | null
  maxScore?: number | null
  allowSubmission?: boolean | null
  allowLateSubmission?: boolean | null
  targetMode: AssignmentTargetMode
  targetStudentIds?: number[] | null
}

export interface UpdateAssignmentPayload {
  title: string
  description?: string | null
  instructions?: string | null
  lessonRecordId?: number | null
  assignedDate: string
  dueDate?: string | null
  maxScore?: number | null
  allowSubmission?: boolean | null
  allowLateSubmission?: boolean | null
  targetMode: AssignmentTargetMode
  targetStudentIds?: number[] | null
}

export interface AssignmentSearchParams {
  classroomId?: number
  status?: AssignmentStatus
  keyword?: string
  page?: number
  size?: number
}

export interface AssignmentSubmissionAttachment {
  id: number
  fileName: string
  contentType?: string | null
  fileSize?: number | null
  uploadedAt: string
}

export interface AssignmentSubmission {
  id: number
  assignmentId: number
  studentId: number
  submittedAt?: string | null
  textAnswer?: string | null
  status: SubmissionStatus
  teacherScore?: number | null
  teacherFeedback?: string | null
  gradedAt?: string | null
  gradedBy?: string | null
  attachments: AssignmentSubmissionAttachment[]
  createdAt: string
  updatedAt: string
}

export interface GradeSubmissionPayload {
  teacherScore?: number | null
  teacherFeedback?: string | null
  status: SubmissionStatus
}

export interface Assessment {
  id: number
  classroomId: number
  classSessionId?: number | null
  evaluationPeriodId?: number | null
  title: string
  description?: string | null
  type: AssessmentType
  assessmentDate: string
  maxScore: number
  weight?: number | null
  status: AssessmentStatus
  publishedToStudents?: boolean | null
  createdByTeacherId?: number | null
  createdBy?: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateAssessmentPayload {
  classroomId: number
  classSessionId?: number | null
  evaluationPeriodId?: number | null
  title: string
  description?: string | null
  type: AssessmentType
  assessmentDate: string
  maxScore: number
  weight?: number | null
}

export interface UpdateAssessmentPayload {
  classSessionId?: number | null
  evaluationPeriodId?: number | null
  title: string
  description?: string | null
  type: AssessmentType
  assessmentDate: string
  maxScore: number
  weight?: number | null
}

export interface AssessmentSearchParams {
  classroomId?: number
  status?: AssessmentStatus
  periodId?: number
  page?: number
  size?: number
}

export interface AssessmentScore {
  id: number
  assessmentId: number
  studentId: number
  score?: number | null
  status: AssessmentScoreStatus
  teacherComment?: string | null
  gradedBy?: string | null
  gradedAt?: string | null
  publishedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface ScoreRowPayload {
  studentId: number
  score?: number | null
  status: AssessmentScoreStatus
  teacherComment?: string | null
}

export interface BulkScoreUpdatePayload {
  rows: ScoreRowPayload[]
}

export interface EvaluationPeriod {
  id: number
  name: string
  classroomId?: number | null
  startDate: string
  endDate: string
  status: EvaluationPeriodStatus
  reopenReason?: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateEvaluationPeriodPayload {
  name: string
  classroomId?: number | null
  startDate: string
  endDate: string
}

export interface UpdateEvaluationPeriodPayload {
  name: string
  classroomId?: number | null
  startDate: string
  endDate: string
}

export interface EvaluationPeriodSearchParams {
  classroomId?: number
  status?: EvaluationPeriodStatus
  keyword?: string
  page?: number
  size?: number
}

export interface ReopenEvaluationPayload {
  reason: string
}

export interface StudentEvaluation {
  id: number
  evaluationPeriodId: number
  classroomId: number
  studentId: number
  teacherId?: number | null
  strengths?: string | null
  areasForImprovement?: string | null
  learningAttitude?: string | null
  participation?: string | null
  homeworkPerformance?: string | null
  teacherComment?: string | null
  recommendation?: string | null
  internalNote?: string | null
  overallRating?: OverallRating | null
  status: StudentEvaluationStatus
  publishedAt?: string | null
  finalizedAt?: string | null
  reopenReason?: string | null
  createdAt: string
  updatedAt: string
}

export interface UpsertStudentEvaluationPayload {
  evaluationPeriodId: number
  classroomId: number
  studentId: number
  strengths?: string | null
  areasForImprovement?: string | null
  learningAttitude?: string | null
  participation?: string | null
  homeworkPerformance?: string | null
  teacherComment?: string | null
  recommendation?: string | null
  internalNote?: string | null
  overallRating?: OverallRating | null
}

export interface ProgressReport {
  id: number
  evaluationId?: number | null
  evaluationPeriodId?: number | null
  classroomId: number
  studentId: number
  teacherId?: number | null
  status: ProgressReportStatus
  publishedAt?: string | null
  generatedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface ProgressReportSearchParams {
  classroomId?: number
  studentId?: number
  periodId?: number
  status?: ProgressReportStatus
  page?: number
  size?: number
}

export interface CenterProfileSnapshot {
  id: number
  centerName: string
  centerSubtitle?: string | null
  logoUrl?: string | null
  address?: string | null
  phone?: string | null
  email?: string | null
  website?: string | null
  taxCode?: string | null
  bankName?: string | null
  bankAccountNumber?: string | null
  bankAccountName?: string | null
  paymentInstruction?: string | null
  invoiceFooterNote?: string | null
  receiptFooterNote?: string | null
}

export interface AcademicProgressSummary {
  studentId: number
  classroomId: number
  evaluationPeriodId?: number | null
  sessionsHeld: number
  sessionsPresent: number
  sessionsAbsent: number
  sessionsExcused: number
  attendanceRate: number
  assignmentsAssigned: number
  assignmentsSubmitted: number
  assignmentsLate: number
  assignmentsGraded: number
  assignmentCompletionRate: number
  assessmentCount: number
  averageAssessmentPercentage?: number | null
  weightedAssessmentAverage?: number | null
  currentEnrollmentTotalSessions?: number | null
  currentEnrollmentUsedSessions?: number | null
  currentEnrollmentRemainingSessions?: number | null
}

export interface AssessmentScoreBreakdownItem {
  assessmentId: number
  title: string
  assessmentDate: string
  type: AssessmentType
  maxScore: number
  score?: number | null
  status: AssessmentScoreStatus
  normalizedPercentage?: number | null
  weight?: number | null
}

export interface ProgressReportDocument {
  reportId: number
  reportStatus: ProgressReportStatus
  generatedAt?: string | null
  publishedAt?: string | null
  centerProfile: CenterProfileSnapshot
  studentId: number
  studentCode?: string | null
  studentName?: string | null
  classroomId: number
  classCode?: string | null
  className?: string | null
  teacherId?: number | null
  teacherName?: string | null
  evaluationPeriodId?: number | null
  evaluationPeriodName?: string | null
  periodStartDate?: string | null
  periodEndDate?: string | null
  progressSummary: AcademicProgressSummary
  assessmentBreakdown: AssessmentScoreBreakdownItem[]
  evaluationId?: number | null
  evaluationStatus?: StudentEvaluationStatus | null
  strengths?: string | null
  areasForImprovement?: string | null
  learningAttitude?: string | null
  participation?: string | null
  homeworkPerformance?: string | null
  teacherComment?: string | null
  recommendation?: string | null
  overallRating?: OverallRating | null
}

export interface ClassroomStudentOption {
  studentId: number
  studentCode: string
  studentName: string
}
