export type Role = "TEACHER" | "STUDENT";

export interface User {
  id: number;
  username: string;
  displayName: string;
  role: Role;
  studentNumber: string | null;
}

export interface Session {
  user: User;
  csrfToken: string;
  csrfHeader: string;
  homePath: string;
}

export interface Classroom {
  id: number;
  name: string;
  courseName: string;
  joinCode: string;
  memberCount: number;
  startAt: string | null;
  endAt: string | null;
  status: "UNSCHEDULED" | "UPCOMING" | "ACTIVE" | "ENDED" | "DISSOLVED";
}

export interface Assignment {
  id: number;
  classroomId: number;
  title: string;
  courseName: string;
  instructions: string;
  dueAt: string;
  submissionFormat: "DOCUMENT" | "CODE" | "SCREENSHOT" | "MIXED";
  aiPolicy: "NOT_ALLOWED" | "ASSISTED_ALLOWED" | "GENERATION_ALLOWED";
  aiDeclarationRequired: boolean;
  sourceDeclarationRequired: boolean;
  appealAllowed: boolean;
  status: "PUBLISHED" | "WITHDRAWN";
}

export interface Rubric {
  id: number;
  name: string;
  points: number;
  description: string;
}

export interface Submission {
  id: number;
  assignmentId: number;
  assignmentTitle: string;
  studentName: string;
  studentNumber: string;
  content: string;
  wordCount: number;
  attemptNumber: number;
  dueAt: string;
  submittedAt: string;
  reviewedAt: string | null;
  reviewStatus: "PENDING" | "REVIEWED";
  aiUsage: string;
  aiTools: string | null;
  aiScope: string | null;
  sourceDeclaration: string | null;
  collaborationDeclaration: string | null;
  integrityConfirmed: boolean;
}

export interface Attachment {
  id: number;
  originalName: string;
  contentType: string;
  fileSize: number;
  attachmentKind: string;
  url: string;
}

export interface ReviewDraft {
  title: string;
  content: string;
  revision: number;
  updatedAt: string;
  versionCount: number;
}

export interface Score {
  id: number;
  rubricId: number;
  rubricName: string;
  maxPoints: number;
  suggestedPoints: number;
  finalPoints: number | null;
  suggestion: string;
}

export interface IntegrityFlag {
  id: number;
  category: string;
  severity: string;
  evidence: string;
  reason: string;
  status: "SUSPECTED" | "CONFIRMED" | "CLEARED";
  createdAt: string;
  resolvedAt: string | null;
}

export interface Appeal {
  id: number;
  submissionId: number;
  reason: string;
  requestedResolution: string | null;
  status: "SUBMITTED" | "UNDER_REVIEW" | "ACCEPTED" | "REJECTED";
  teacherResponse: string | null;
  createdAt: string;
  respondedAt: string | null;
}

export interface Issue {
  id: number;
  rubricName: string;
  evidenceQuote: string;
  teacherNote: string;
  createdAt: string;
}

export interface AiReviewSuggestion {
  id: number;
  rubricName: string;
  evidenceQuote: string;
  suggestion: string;
  suggestedPoints: number | null;
  status: "PENDING" | "ADOPTED" | "DISMISSED" | "SUPERSEDED";
  createdAt: string;
  decidedAt: string | null;
}

export interface AiReviewRun {
  id: number;
  model: string;
  summary: string;
  createdAt: string;
  suggestions: AiReviewSuggestion[];
}

export interface AiProviderOption {
  code: string;
  name: string;
  baseUrl: string;
  defaultModel: string;
}

export interface AiSettings {
  configured: boolean;
  source: "TEACHER" | "SERVER";
  provider: string;
  providerName: string;
  baseUrl: string;
  model: string;
  maskedKey: string;
  updatedAt: string | null;
  providers: AiProviderOption[];
}

export interface AiConnectionResult {
  success: boolean;
  message: string;
}

export interface AssignmentOverview {
  assignment: Assignment;
  submissionCount: number;
}

export interface EditableAssignment {
  assignment: Assignment;
  rubrics: Rubric[];
  submissionCount: number;
}

export interface SubmissionOverview {
  id: number;
  studentName: string;
  studentNumber: string;
  assignmentTitle: string;
  attemptNumber: number;
  submittedAt: string;
  reviewStatus: string;
  statusText: string;
}

export interface CommonIssue {
  rubricName: string;
  count: number;
}

export interface AiBatchProgress {
  assignmentId: number;
  total: number;
  completed: number;
  running: number;
  pending: number;
  failed: number;
  skipped: number;
}

export interface AppealOverview {
  appeal: Appeal;
  studentName: string;
  assignmentTitle: string;
}

export interface TeacherDashboard {
  user: User;
  classrooms: Classroom[];
  assignments: AssignmentOverview[];
  submissions: SubmissionOverview[];
  commonIssues: CommonIssue[];
  appeals: AppealOverview[];
  reviewedCount: number;
  aiConfigured: boolean;
  aiBatchProgress: AiBatchProgress[];
}

export interface TeacherReviewWorkspace {
  assignment: Assignment;
  submission: Submission;
  review: ReviewDraft;
  attachments: Attachment[];
  rubrics: Rubric[];
  scores: Score[];
  finalScoreTotal: number | null;
  integrityFlags: IntegrityFlag[];
  issues: Issue[];
  reviewChecklist: string[];
  aiConfigured: boolean;
  aiReviews: AiReviewRun[];
}

export interface StudentAssignmentOverview {
  assignment: Assignment;
  latestSubmission: Submission | null;
  statusText: string;
  activeAppeal: boolean;
}

export interface StudentDashboard {
  user: User;
  classrooms: Classroom[];
  assignments: StudentAssignmentOverview[];
}

export interface StudentAssignmentDetail {
  assignment: Assignment;
  rubrics: Rubric[];
  latestSubmission: Submission | null;
  attachments: Attachment[];
  review: ReviewDraft | null;
  scores: Score[];
  finalScoreTotal: number | null;
  confirmedIntegrityFlags: IntegrityFlag[];
  appeals: Appeal[];
  activeAppeal: boolean;
}

export interface LessonIssueGroup {
  rubricName: string;
  count: number;
  teacherNotes: string[];
}

export interface LessonWorkspace {
  assignment: Assignment;
  rubrics: Rubric[];
  title: string;
  content: string;
  revision: number;
  updatedAt: string;
  evidenceCount: number;
  issueGroups: LessonIssueGroup[];
  aiConfigured: boolean;
}

export interface ApiProblem {
  code?: string;
  message?: string;
  currentRevision?: number;
}
