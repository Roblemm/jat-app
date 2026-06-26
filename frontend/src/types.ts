export type Area = {
  id: string;
  name: string;
  displayOrder: number;
};

export type Project = {
  id: string;
  areaId: string;
  areaName: string;
  name: string;
  normalizedName: string;
  createdAt: string | null;
  updatedAt: string | null;
};

export type GoalType = 'CHECKLIST' | 'TARGET' | 'HABIT';
export type GoalStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ARCHIVED';
export type Recurrence = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';

export type Goal = {
  id: string;
  areaId: string;
  areaName: string;
  projectId: string | null;
  projectName: string | null;
  title: string;
  description: string | null;
  goalType: GoalType;
  status: GoalStatus;
  recurrence: Recurrence;
  targetValue: number | null;
  unit: string | null;
  targetDate: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type TaskType = 'ACTION' | 'IDEA' | 'ROUTINE';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'ARCHIVED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export type Task = {
  id: string;
  areaId: string;
  areaName: string;
  projectId: string | null;
  projectName: string | null;
  goalId: string | null;
  goalTitle: string | null;
  title: string;
  description: string | null;
  taskType: TaskType;
  status: TaskStatus;
  priority: TaskPriority;
  recurrence: Recurrence;
  dueAt: string | null;
  remindAt: string | null;
  scheduledStart: string | null;
  scheduledEnd: string | null;
  completedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type TodayDashboard = {
  date: string;
  tasksDueToday: Task[];
  overdueTasks: Task[];
  scheduledBlocks: Task[];
  activeGoals: Goal[];
  counts: {
    dueToday: number;
    overdue: number;
    scheduled: number;
    activeGoals: number;
  };
};

export type JobStatus = 'SAVED' | 'APPLIED' | 'INTERVIEWING' | 'OFFER' | 'REJECTED' | 'ARCHIVED';

export type JobOpportunity = {
  id: string;
  title: string;
  company: string;
  location: string | null;
  sourceUrl: string | null;
  sourceName: string | null;
  status: JobStatus;
  notes: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ExtractionConfidence = 'LOW' | 'MEDIUM' | 'HIGH';

export type JobImportPreview = {
  sourceUrl: string | null;
  extracted: {
    title: string | null;
    company: string | null;
    location: string | null;
    description: string | null;
    sourceName: string | null;
  };
  confidence: {
    title: ExtractionConfidence;
    company: ExtractionConfidence;
    location: ExtractionConfidence;
  };
  needsReview: boolean;
};
