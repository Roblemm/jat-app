import type {
  Area,
  Goal,
  GoalType,
  JobImportPreview,
  JobOpportunity,
  JobStatus,
  Project,
  Recurrence,
  Task,
  TaskPriority,
  TaskType,
  TodayDashboard,
} from './types';

type QueryValue = string | null | undefined;

const jsonHeaders = { 'Content-Type': 'application/json' };

function queryString(params: Record<string, QueryValue>) {
  const query = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      query.set(key, value);
    }
  });

  const serialized = query.toString();
  return serialized ? `?${serialized}` : '';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init);

  if (!response.ok) {
    const fallback = `Request failed with status ${response.status}`;
    const error = await response.json().catch(() => ({ message: fallback }));
    throw new Error(error.message ?? fallback);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const api = {
  areas: () => request<Area[]>('/api/areas'),

  projects: (areaId: string) => request<Project[]>(`/api/projects${queryString({ areaId })}`),

  createProject: (body: { areaId: string; name: string }) =>
    request<Project>('/api/projects', {
      method: 'POST',
      headers: jsonHeaders,
      body: JSON.stringify(body),
    }),

  goals: (params: { areaId: string; projectId?: string | null }) =>
    request<Goal[]>(`/api/goals${queryString(params)}`),

  createGoal: (body: {
    areaId: string;
    projectId?: string | null;
    title: string;
    description?: string | null;
    goalType: GoalType;
    recurrence: Recurrence;
    targetValue?: number | null;
    unit?: string | null;
    targetDate?: string | null;
  }) =>
    request<Goal>('/api/goals', {
      method: 'POST',
      headers: jsonHeaders,
      body: JSON.stringify(body),
    }),

  tasks: (params: { areaId: string; projectId?: string | null; goalId?: string | null }) =>
    request<Task[]>(`/api/tasks${queryString(params)}`),

  createTask: (body: {
    areaId: string;
    projectId?: string | null;
    goalId?: string | null;
    title: string;
    description?: string | null;
    taskType: TaskType;
    priority: TaskPriority;
    recurrence: Recurrence;
    dueAt?: string | null;
    remindAt?: string | null;
    scheduledStart?: string | null;
    scheduledEnd?: string | null;
  }) =>
    request<Task>('/api/tasks', {
      method: 'POST',
      headers: jsonHeaders,
      body: JSON.stringify(body),
    }),

  today: (params: { date: string; areaId?: string | null }) =>
    request<TodayDashboard>(`/api/dashboard/today${queryString(params)}`),

  jobs: (status?: JobStatus | 'ALL') =>
    request<JobOpportunity[]>(
      `/api/job-opportunities${queryString({ status: status === 'ALL' ? null : status })}`,
    ),

  createJob: (body: {
    title: string;
    company: string;
    location?: string | null;
    sourceUrl?: string | null;
    sourceName?: string | null;
    notes?: string | null;
  }) =>
    request<JobOpportunity>('/api/job-opportunities', {
      method: 'POST',
      headers: jsonHeaders,
      body: JSON.stringify(body),
    }),

  updateJob: (id: string, body: {
    title: string;
    company: string;
    location?: string | null;
    sourceUrl?: string | null;
    sourceName?: string | null;
    status: JobStatus;
    notes?: string | null;
  }) =>
    request<JobOpportunity>(`/api/job-opportunities/${id}`, {
      method: 'PUT',
      headers: jsonHeaders,
      body: JSON.stringify(body),
    }),

  deleteJob: (id: string) =>
    request<void>(`/api/job-opportunities/${id}`, {
      method: 'DELETE',
    }),

  previewJobImport: (body: { sourceUrl?: string | null; pastedText?: string | null }) =>
    request<JobImportPreview>('/api/job-imports/preview', {
      method: 'POST',
      headers: jsonHeaders,
      body: JSON.stringify(body),
    }),
};
