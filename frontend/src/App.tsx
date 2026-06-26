import {
  BriefcaseBusiness,
  CalendarDays,
  CheckCircle2,
  FolderKanban,
  Gauge,
  Layers3,
  LinkIcon,
  ListChecks,
  Plus,
  RefreshCw,
  Save,
  Sparkles,
  Target,
  Trash2,
} from 'lucide-react';
import { FormEvent, useEffect, useMemo, useState } from 'react';

import { api } from './api';
import type {
  Area,
  Goal,
  GoalStatus,
  GoalType,
  JobImportPreview,
  JobOpportunity,
  JobStatus,
  Project,
  Recurrence,
  Task,
  TaskPriority,
  TaskStatus,
  TaskType,
  TodayDashboard,
} from './types';

type View = 'today' | 'goals' | 'tasks' | 'jobs' | 'context';

const goalTypes: GoalType[] = ['CHECKLIST', 'TARGET', 'HABIT'];
const goalStatuses: Array<GoalStatus | 'ALL'> = ['ALL', 'ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED'];
const recurrences: Recurrence[] = ['NONE', 'DAILY', 'WEEKLY', 'MONTHLY'];
const taskTypes: TaskType[] = ['ACTION', 'IDEA', 'ROUTINE'];
const taskStatuses: Array<TaskStatus | 'ALL'> = ['ALL', 'TODO', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED'];
const priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];
const jobStatuses: Array<JobStatus | 'ALL'> = ['ALL', 'SAVED', 'APPLIED', 'INTERVIEWING', 'OFFER', 'REJECTED', 'ARCHIVED'];

const todayLocalDate = () => {
  const now = new Date();
  const offset = now.getTimezoneOffset();
  return new Date(now.getTime() - offset * 60_000).toISOString().slice(0, 10);
};

const toIsoOrNull = (value: string) => (value ? new Date(value).toISOString() : null);
const emptyToNull = (value: string) => (value.trim() ? value.trim() : null);

export function App() {
  const [view, setView] = useState<View>('today');
  const [areas, setAreas] = useState<Area[]>([]);
  const [selectedAreaId, setSelectedAreaId] = useState('');
  const [dashboardAreaId, setDashboardAreaId] = useState('ALL');
  const [dashboardDate, setDashboardDate] = useState(todayLocalDate());
  const [projects, setProjects] = useState<Project[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [goalStatusFilter, setGoalStatusFilter] = useState<GoalStatus | 'ALL'>('ACTIVE');
  const [taskStatusFilter, setTaskStatusFilter] = useState<TaskStatus | 'ALL'>('ALL');
  const [taskPriorityFilter, setTaskPriorityFilter] = useState<TaskPriority | 'ALL'>('ALL');
  const [taskProjectFilter, setTaskProjectFilter] = useState('ALL');
  const [taskGoalFilter, setTaskGoalFilter] = useState('ALL');
  const [dashboard, setDashboard] = useState<TodayDashboard | null>(null);
  const [jobs, setJobs] = useState<JobOpportunity[]>([]);
  const [jobStatusFilter, setJobStatusFilter] = useState<JobStatus | 'ALL'>('ALL');
  const [jobPreview, setJobPreview] = useState<JobImportPreview | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [projectName, setProjectName] = useState('');
  const [goalForm, setGoalForm] = useState({
    projectId: '',
    title: '',
    description: '',
    goalType: 'TARGET' as GoalType,
    recurrence: 'WEEKLY' as Recurrence,
    targetValue: '',
    unit: '',
    targetDate: '',
  });
  const [taskForm, setTaskForm] = useState({
    projectId: '',
    goalId: '',
    title: '',
    description: '',
    taskType: 'ACTION' as TaskType,
    priority: 'MEDIUM' as TaskPriority,
    recurrence: 'NONE' as Recurrence,
    dueAt: '',
    remindAt: '',
    scheduledStart: '',
    scheduledEnd: '',
  });
  const [importForm, setImportForm] = useState({ sourceUrl: '', pastedText: '' });
  const [jobDraft, setJobDraft] = useState({
    title: '',
    company: '',
    location: '',
    sourceUrl: '',
    sourceName: '',
    notes: '',
  });

  const selectedArea = useMemo(
    () => areas.find((area) => area.id === selectedAreaId),
    [areas, selectedAreaId],
  );

  const visibleGoals = useMemo(
    () => goals.filter((goal) => goalStatusFilter === 'ALL' || goal.status === goalStatusFilter),
    [goals, goalStatusFilter],
  );

  const visibleTasks = useMemo(
    () => tasks.filter((task) => {
      const matchesStatus = taskStatusFilter === 'ALL' || task.status === taskStatusFilter;
      const matchesPriority = taskPriorityFilter === 'ALL' || task.priority === taskPriorityFilter;
      const matchesProject = taskProjectFilter === 'ALL' || (task.projectId ?? 'NONE') === taskProjectFilter;
      const matchesGoal = taskGoalFilter === 'ALL' || (task.goalId ?? 'NONE') === taskGoalFilter;
      return matchesStatus && matchesPriority && matchesProject && matchesGoal;
    }),
    [tasks, taskStatusFilter, taskPriorityFilter, taskProjectFilter, taskGoalFilter],
  );

  const showError = (message: string) => {
    setError(message);
    setNotice('');
  };

  const showNotice = (message: string) => {
    setNotice(message);
    setError('');
  };

  const runAction = async (action: () => Promise<void>, successMessage?: string) => {
    setBusy(true);
    try {
      await action();
      if (successMessage) {
        showNotice(successMessage);
      }
    } catch (caught) {
      showError(caught instanceof Error ? caught.message : 'Something went wrong.');
    } finally {
      setBusy(false);
    }
  };

  const loadAreas = async () => {
    const data = await api.areas();
    setAreas(data);
    setSelectedAreaId((current) => current || data[0]?.id || '');
  };

  const loadAreaData = async (areaId: string) => {
    if (!areaId) {
      return;
    }

    const [projectData, goalData, taskData] = await Promise.all([
      api.projects(areaId),
      api.goals({ areaId }),
      api.tasks({ areaId }),
    ]);
    setProjects(projectData);
    setGoals(goalData);
    setTasks(taskData);
  };

  const loadDashboard = async () => {
    const areaId = dashboardAreaId === 'ALL' ? null : dashboardAreaId;
    setDashboard(await api.today({ date: dashboardDate, areaId }));
  };

  const loadJobs = async () => {
    setJobs(await api.jobs(jobStatusFilter));
  };

  useEffect(() => {
    void runAction(loadAreas);
  }, []);

  useEffect(() => {
    if (selectedAreaId) {
      void runAction(() => loadAreaData(selectedAreaId));
    }
  }, [selectedAreaId]);

  useEffect(() => {
    void runAction(loadDashboard);
  }, [dashboardAreaId, dashboardDate]);

  useEffect(() => {
    void runAction(loadJobs);
  }, [jobStatusFilter]);

  const createProject = (event: FormEvent) => {
    event.preventDefault();
    void runAction(async () => {
      await api.createProject({ areaId: selectedAreaId, name: projectName });
      setProjectName('');
      await loadAreaData(selectedAreaId);
    }, 'Project created.');
  };

  const createGoal = (event: FormEvent) => {
    event.preventDefault();
    void runAction(async () => {
      await api.createGoal({
        areaId: selectedAreaId,
        projectId: emptyToNull(goalForm.projectId),
        title: goalForm.title,
        description: emptyToNull(goalForm.description),
        goalType: goalForm.goalType,
        recurrence: goalForm.recurrence,
        targetValue: goalForm.targetValue ? Number(goalForm.targetValue) : null,
        unit: emptyToNull(goalForm.unit),
        targetDate: emptyToNull(goalForm.targetDate),
      });
      setGoalForm((current) => ({ ...current, title: '', description: '', targetValue: '', unit: '', targetDate: '' }));
      await loadAreaData(selectedAreaId);
      await loadDashboard();
    }, 'Goal created.');
  };

  const createTask = (event: FormEvent) => {
    event.preventDefault();
    void runAction(async () => {
      await api.createTask({
        areaId: selectedAreaId,
        projectId: emptyToNull(taskForm.projectId),
        goalId: emptyToNull(taskForm.goalId),
        title: taskForm.title,
        description: emptyToNull(taskForm.description),
        taskType: taskForm.taskType,
        priority: taskForm.priority,
        recurrence: taskForm.recurrence,
        dueAt: toIsoOrNull(taskForm.dueAt),
        remindAt: toIsoOrNull(taskForm.remindAt),
        scheduledStart: toIsoOrNull(taskForm.scheduledStart),
        scheduledEnd: toIsoOrNull(taskForm.scheduledEnd),
      });
      setTaskForm((current) => ({ ...current, title: '', description: '', dueAt: '', remindAt: '', scheduledStart: '', scheduledEnd: '' }));
      await loadAreaData(selectedAreaId);
      await loadDashboard();
    }, 'Task created.');
  };

  const updateGoalStatus = (goal: Goal, status: GoalStatus) => {
    void runAction(async () => {
      await api.updateGoalStatus(goal.id, status);
      await loadAreaData(selectedAreaId);
      await loadDashboard();
    }, 'Goal updated.');
  };

  const deleteGoal = (goalId: string) => {
    void runAction(async () => {
      await api.deleteGoal(goalId);
      await loadAreaData(selectedAreaId);
      await loadDashboard();
    }, 'Goal deleted.');
  };

  const updateTaskStatus = (task: Task, status: TaskStatus) => {
    void runAction(async () => {
      await api.updateTaskStatus(task.id, status);
      await loadAreaData(selectedAreaId);
      await loadDashboard();
    }, 'Task updated.');
  };

  const deleteTask = (taskId: string) => {
    void runAction(async () => {
      await api.deleteTask(taskId);
      await loadAreaData(selectedAreaId);
      await loadDashboard();
    }, 'Task deleted.');
  };

  const previewImport = (event: FormEvent) => {
    event.preventDefault();
    void runAction(async () => {
      const preview = await api.previewJobImport({
        sourceUrl: emptyToNull(importForm.sourceUrl),
        pastedText: emptyToNull(importForm.pastedText),
      });
      setJobPreview(preview);
      setJobDraft({
        title: preview.extracted.title ?? '',
        company: preview.extracted.company ?? '',
        location: preview.extracted.location ?? '',
        sourceUrl: preview.sourceUrl ?? importForm.sourceUrl,
        sourceName: preview.extracted.sourceName ?? '',
        notes: preview.extracted.description ?? '',
      });
    }, 'Preview generated.');
  };

  const saveJob = (event: FormEvent) => {
    event.preventDefault();
    void runAction(async () => {
      await api.createJob({
        title: jobDraft.title,
        company: jobDraft.company,
        location: emptyToNull(jobDraft.location),
        sourceUrl: emptyToNull(jobDraft.sourceUrl),
        sourceName: emptyToNull(jobDraft.sourceName),
        notes: emptyToNull(jobDraft.notes),
      });
      setJobPreview(null);
      setImportForm({ sourceUrl: '', pastedText: '' });
      setJobDraft({ title: '', company: '', location: '', sourceUrl: '', sourceName: '', notes: '' });
      await loadJobs();
    }, 'Job saved.');
  };

  const updateJobStatus = (job: JobOpportunity, status: JobStatus) => {
    void runAction(async () => {
      await api.updateJob(job.id, {
        title: job.title,
        company: job.company,
        location: job.location,
        sourceUrl: job.sourceUrl,
        sourceName: job.sourceName,
        status,
        notes: job.notes,
      });
      await loadJobs();
    }, 'Job updated.');
  };

  const deleteJob = (id: string) => {
    void runAction(async () => {
      await api.deleteJob(id);
      await loadJobs();
    }, 'Job deleted.');
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-mark">
          <div className="brand-icon">J</div>
          <div>
            <strong>Jat</strong>
            <span>Command Center</span>
          </div>
        </div>

        <nav className="nav-stack" aria-label="Primary">
          <NavButton icon={<Gauge />} label="Today" active={view === 'today'} onClick={() => setView('today')} />
          <NavButton icon={<Target />} label="Goals" active={view === 'goals'} onClick={() => setView('goals')} />
          <NavButton icon={<ListChecks />} label="Tasks" active={view === 'tasks'} onClick={() => setView('tasks')} />
          <NavButton icon={<BriefcaseBusiness />} label="Jobs" active={view === 'jobs'} onClick={() => setView('jobs')} />
          <NavButton icon={<Layers3 />} label="Context" active={view === 'context'} onClick={() => setView('context')} />
        </nav>

        <div className="sidebar-control">
          <label htmlFor="area-select">Area</label>
          <select id="area-select" value={selectedAreaId} onChange={(event) => setSelectedAreaId(event.target.value)}>
            {areas.map((area) => (
              <option value={area.id} key={area.id}>{area.name}</option>
            ))}
          </select>
        </div>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">{selectedArea?.name ?? 'Local workspace'}</p>
            <h1>{view === 'today' ? 'Today Command' : titleForView(view)}</h1>
          </div>
          <button className="icon-button" type="button" onClick={() => void runAction(async () => {
            await Promise.all([selectedAreaId ? loadAreaData(selectedAreaId) : Promise.resolve(), loadDashboard(), loadJobs()]);
          }, 'Refreshed.')}>
            <RefreshCw size={18} />
            <span>Refresh</span>
          </button>
        </header>

        {(error || notice) && (
          <div className={error ? 'message error' : 'message success'}>
            {error || notice}
          </div>
        )}

        {view === 'today' && (
          <TodayView
            areas={areas}
            dashboard={dashboard}
            dashboardAreaId={dashboardAreaId}
            dashboardDate={dashboardDate}
            setDashboardAreaId={setDashboardAreaId}
            setDashboardDate={setDashboardDate}
            updateTaskStatus={updateTaskStatus}
          />
        )}

        {view === 'goals' && (
          <GoalsView
            projects={projects}
            goals={visibleGoals}
            goalStatusFilter={goalStatusFilter}
            setGoalStatusFilter={setGoalStatusFilter}
            goalForm={goalForm}
            setGoalForm={setGoalForm}
            createGoal={createGoal}
            updateGoalStatus={updateGoalStatus}
            deleteGoal={deleteGoal}
            busy={busy}
          />
        )}

        {view === 'tasks' && (
          <TasksView
            projects={projects}
            goals={goals}
            tasks={visibleTasks}
            taskStatusFilter={taskStatusFilter}
            setTaskStatusFilter={setTaskStatusFilter}
            taskPriorityFilter={taskPriorityFilter}
            setTaskPriorityFilter={setTaskPriorityFilter}
            taskProjectFilter={taskProjectFilter}
            setTaskProjectFilter={setTaskProjectFilter}
            taskGoalFilter={taskGoalFilter}
            setTaskGoalFilter={setTaskGoalFilter}
            taskForm={taskForm}
            setTaskForm={setTaskForm}
            createTask={createTask}
            updateTaskStatus={updateTaskStatus}
            deleteTask={deleteTask}
            busy={busy}
          />
        )}

        {view === 'jobs' && (
          <JobsView
            jobs={jobs}
            jobStatusFilter={jobStatusFilter}
            setJobStatusFilter={setJobStatusFilter}
            importForm={importForm}
            setImportForm={setImportForm}
            previewImport={previewImport}
            jobPreview={jobPreview}
            jobDraft={jobDraft}
            setJobDraft={setJobDraft}
            saveJob={saveJob}
            updateJobStatus={updateJobStatus}
            deleteJob={deleteJob}
            busy={busy}
          />
        )}

        {view === 'context' && (
          <ContextView
            areas={areas}
            selectedAreaId={selectedAreaId}
            setSelectedAreaId={setSelectedAreaId}
            projects={projects}
            projectName={projectName}
            setProjectName={setProjectName}
            createProject={createProject}
            busy={busy}
          />
        )}
      </main>
    </div>
  );
}

function NavButton({ icon, label, active, onClick }: { icon: React.ReactNode; label: string; active: boolean; onClick: () => void }) {
  return (
    <button className={active ? 'nav-button active' : 'nav-button'} type="button" onClick={onClick}>
      {icon}
      <span>{label}</span>
    </button>
  );
}

function TodayView({
  areas,
  dashboard,
  dashboardAreaId,
  dashboardDate,
  setDashboardAreaId,
  setDashboardDate,
  updateTaskStatus,
}: {
  areas: Area[];
  dashboard: TodayDashboard | null;
  dashboardAreaId: string;
  dashboardDate: string;
  setDashboardAreaId: (value: string) => void;
  setDashboardDate: (value: string) => void;
  updateTaskStatus: (task: Task, status: TaskStatus) => void;
}) {
  return (
    <section className="view-grid">
      <div className="toolbar">
        <label>
          Date
          <input type="date" value={dashboardDate} onChange={(event) => setDashboardDate(event.target.value)} />
        </label>
        <label>
          Scope
          <select value={dashboardAreaId} onChange={(event) => setDashboardAreaId(event.target.value)}>
            <option value="ALL">All areas</option>
            {areas.map((area) => (
              <option key={area.id} value={area.id}>{area.name}</option>
            ))}
          </select>
        </label>
      </div>

      <div className="metric-row">
        <Metric label="Due" value={dashboard?.counts.dueToday ?? 0} />
        <Metric label="Overdue" value={dashboard?.counts.overdue ?? 0} tone="warn" />
        <Metric label="Blocks" value={dashboard?.counts.scheduled ?? 0} />
        <Metric label="Goals" value={dashboard?.counts.activeGoals ?? 0} />
      </div>

      <div className="split-grid">
        <TaskDashboardPanel
          icon={<CalendarDays />}
          title="Due today"
          tasks={dashboard?.tasksDueToday ?? []}
          updateTaskStatus={updateTaskStatus}
        />
        <TaskDashboardPanel
          icon={<CheckCircle2 />}
          title="Scheduled"
          tasks={dashboard?.scheduledBlocks ?? []}
          updateTaskStatus={updateTaskStatus}
          summary={dashboardScheduleSummary}
        />
        <ListPanel icon={<Target />} title="Active goals" items={dashboard?.activeGoals.map(goalSummary) ?? []} />
        <TaskDashboardPanel
          icon={<ListChecks />}
          title="Overdue"
          tasks={dashboard?.overdueTasks ?? []}
          updateTaskStatus={updateTaskStatus}
          danger
        />
      </div>
    </section>
  );
}

function GoalsView({
  projects,
  goals,
  goalStatusFilter,
  setGoalStatusFilter,
  goalForm,
  setGoalForm,
  createGoal,
  updateGoalStatus,
  deleteGoal,
  busy,
}: {
  projects: Project[];
  goals: Goal[];
  goalStatusFilter: GoalStatus | 'ALL';
  setGoalStatusFilter: (value: GoalStatus | 'ALL') => void;
  goalForm: {
    projectId: string;
    title: string;
    description: string;
    goalType: GoalType;
    recurrence: Recurrence;
    targetValue: string;
    unit: string;
    targetDate: string;
  };
  setGoalForm: React.Dispatch<React.SetStateAction<typeof goalForm>>;
  createGoal: (event: FormEvent) => void;
  updateGoalStatus: (goal: Goal, status: GoalStatus) => void;
  deleteGoal: (goalId: string) => void;
  busy: boolean;
}) {
  return (
    <section className="two-column">
      <form className="panel form-panel" onSubmit={createGoal}>
        <PanelTitle icon={<Plus />} title="Create goal" />
        <label>Title<input required value={goalForm.title} onChange={(event) => setGoalForm((form) => ({ ...form, title: event.target.value }))} /></label>
        <label>Project<select value={goalForm.projectId} onChange={(event) => setGoalForm((form) => ({ ...form, projectId: event.target.value }))}>
          <option value="">No project</option>
          {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
        </select></label>
        <div className="form-row">
          <label>Type<select value={goalForm.goalType} onChange={(event) => setGoalForm((form) => ({ ...form, goalType: event.target.value as GoalType }))}>
            {goalTypes.map((type) => <option key={type}>{type}</option>)}
          </select></label>
          <label>Repeats<select value={goalForm.recurrence} onChange={(event) => setGoalForm((form) => ({ ...form, recurrence: event.target.value as Recurrence }))}>
            {recurrences.map((recurrence) => <option key={recurrence}>{recurrence}</option>)}
          </select></label>
        </div>
        <div className="form-row">
          <label>Target<input type="number" min="0" step="0.01" value={goalForm.targetValue} onChange={(event) => setGoalForm((form) => ({ ...form, targetValue: event.target.value }))} /></label>
          <label>Unit<input value={goalForm.unit} onChange={(event) => setGoalForm((form) => ({ ...form, unit: event.target.value }))} /></label>
        </div>
        <label>Target date<input type="date" value={goalForm.targetDate} onChange={(event) => setGoalForm((form) => ({ ...form, targetDate: event.target.value }))} /></label>
        <label>Description<textarea value={goalForm.description} onChange={(event) => setGoalForm((form) => ({ ...form, description: event.target.value }))} /></label>
        <SubmitButton busy={busy} label="Save goal" />
      </form>

      <div className="panel list-panel-large">
        <div className="panel-heading with-filter">
          <PanelTitle icon={<Target />} title="Goals" />
          <select value={goalStatusFilter} onChange={(event) => setGoalStatusFilter(event.target.value as GoalStatus | 'ALL')}>
            {goalStatuses.map((status) => <option key={status}>{status}</option>)}
          </select>
        </div>
        <div className="stack-list">
          {goals.map((goal) => (
            <article className="row-item" key={goal.id}>
              <div>
                <strong>{goal.title}</strong>
                <span>{goal.goalType} · {goal.recurrence} · {goal.projectName ?? 'No project'}</span>
              </div>
              <div className="row-actions">
                <select
                  aria-label={`Status for ${goal.title}`}
                  value={goal.status}
                  onChange={(event) => updateGoalStatus(goal, event.target.value as GoalStatus)}
                >
                  {goalStatuses.filter((status) => status !== 'ALL').map((status) => <option key={status}>{status}</option>)}
                </select>
                <button className="icon-only danger-button" type="button" onClick={() => deleteGoal(goal.id)} aria-label={`Delete ${goal.title}`}>
                  <Trash2 size={16} />
                </button>
              </div>
            </article>
          ))}
          {goals.length === 0 && <EmptyState label="No goals match this filter." />}
        </div>
      </div>
    </section>
  );
}

function TasksView({
  projects,
  goals,
  tasks,
  taskStatusFilter,
  setTaskStatusFilter,
  taskPriorityFilter,
  setTaskPriorityFilter,
  taskProjectFilter,
  setTaskProjectFilter,
  taskGoalFilter,
  setTaskGoalFilter,
  taskForm,
  setTaskForm,
  createTask,
  updateTaskStatus,
  deleteTask,
  busy,
}: {
  projects: Project[];
  goals: Goal[];
  tasks: Task[];
  taskStatusFilter: TaskStatus | 'ALL';
  setTaskStatusFilter: (value: TaskStatus | 'ALL') => void;
  taskPriorityFilter: TaskPriority | 'ALL';
  setTaskPriorityFilter: (value: TaskPriority | 'ALL') => void;
  taskProjectFilter: string;
  setTaskProjectFilter: (value: string) => void;
  taskGoalFilter: string;
  setTaskGoalFilter: (value: string) => void;
  taskForm: {
    projectId: string;
    goalId: string;
    title: string;
    description: string;
    taskType: TaskType;
    priority: TaskPriority;
    recurrence: Recurrence;
    dueAt: string;
    remindAt: string;
    scheduledStart: string;
    scheduledEnd: string;
  };
  setTaskForm: React.Dispatch<React.SetStateAction<typeof taskForm>>;
  createTask: (event: FormEvent) => void;
  updateTaskStatus: (task: Task, status: TaskStatus) => void;
  deleteTask: (taskId: string) => void;
  busy: boolean;
}) {
  return (
    <section className="two-column">
      <form className="panel form-panel" onSubmit={createTask}>
        <PanelTitle icon={<Plus />} title="Create task" />
        <label>Title<input required value={taskForm.title} onChange={(event) => setTaskForm((form) => ({ ...form, title: event.target.value }))} /></label>
        <div className="form-row">
          <label>Type<select value={taskForm.taskType} onChange={(event) => setTaskForm((form) => ({ ...form, taskType: event.target.value as TaskType }))}>
            {taskTypes.map((type) => <option key={type}>{type}</option>)}
          </select></label>
          <label>Priority<select value={taskForm.priority} onChange={(event) => setTaskForm((form) => ({ ...form, priority: event.target.value as TaskPriority }))}>
            {priorities.map((priority) => <option key={priority}>{priority}</option>)}
          </select></label>
        </div>
        <div className="form-row">
          <label>Project<select value={taskForm.projectId} onChange={(event) => setTaskForm((form) => ({ ...form, projectId: event.target.value }))}>
            <option value="">No project</option>
            {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
          </select></label>
          <label>Goal<select value={taskForm.goalId} onChange={(event) => setTaskForm((form) => ({ ...form, goalId: event.target.value }))}>
            <option value="">No goal</option>
            {goals.map((goal) => <option key={goal.id} value={goal.id}>{goal.title}</option>)}
          </select></label>
        </div>
        <label>Repeats<select value={taskForm.recurrence} onChange={(event) => setTaskForm((form) => ({ ...form, recurrence: event.target.value as Recurrence }))}>
          {recurrences.map((recurrence) => <option key={recurrence}>{recurrence}</option>)}
        </select></label>
        <div className="form-row">
          <label>Due<input type="datetime-local" value={taskForm.dueAt} onChange={(event) => setTaskForm((form) => ({ ...form, dueAt: event.target.value }))} /></label>
          <label>Remind<input type="datetime-local" value={taskForm.remindAt} onChange={(event) => setTaskForm((form) => ({ ...form, remindAt: event.target.value }))} /></label>
        </div>
        <div className="form-row">
          <label>Start<input type="datetime-local" value={taskForm.scheduledStart} onChange={(event) => setTaskForm((form) => ({ ...form, scheduledStart: event.target.value }))} /></label>
          <label>End<input type="datetime-local" value={taskForm.scheduledEnd} onChange={(event) => setTaskForm((form) => ({ ...form, scheduledEnd: event.target.value }))} /></label>
        </div>
        <label>Description<textarea value={taskForm.description} onChange={(event) => setTaskForm((form) => ({ ...form, description: event.target.value }))} /></label>
        <SubmitButton busy={busy} label="Save task" />
      </form>

      <div className="panel list-panel-large">
        <div className="panel-heading">
          <PanelTitle icon={<ListChecks />} title="Tasks" />
        </div>
        <div className="toolbar compact-toolbar">
          <label>Status<select value={taskStatusFilter} onChange={(event) => setTaskStatusFilter(event.target.value as TaskStatus | 'ALL')}>
            {taskStatuses.map((status) => <option key={status}>{status}</option>)}
          </select></label>
          <label>Priority<select value={taskPriorityFilter} onChange={(event) => setTaskPriorityFilter(event.target.value as TaskPriority | 'ALL')}>
            <option value="ALL">ALL</option>
            {priorities.map((priority) => <option key={priority}>{priority}</option>)}
          </select></label>
          <label>Project<select value={taskProjectFilter} onChange={(event) => setTaskProjectFilter(event.target.value)}>
            <option value="ALL">All projects</option>
            <option value="NONE">No project</option>
            {projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}
          </select></label>
          <label>Goal<select value={taskGoalFilter} onChange={(event) => setTaskGoalFilter(event.target.value)}>
            <option value="ALL">All goals</option>
            <option value="NONE">No goal</option>
            {goals.map((goal) => <option key={goal.id} value={goal.id}>{goal.title}</option>)}
          </select></label>
        </div>
        <div className="stack-list">
          {tasks.map((task) => (
            <article className="row-item" key={task.id}>
              <div>
                <strong>{task.title}</strong>
                <span>{task.taskType} · {task.priority} · {task.goalTitle ?? task.projectName ?? 'Unlinked'}</span>
              </div>
              <div className="row-actions">
                {task.status !== 'COMPLETED' && (
                  <button className="icon-only" type="button" onClick={() => updateTaskStatus(task, 'COMPLETED')} aria-label={`Complete ${task.title}`}>
                    <CheckCircle2 size={16} />
                  </button>
                )}
                <select
                  aria-label={`Status for ${task.title}`}
                  value={task.status}
                  onChange={(event) => updateTaskStatus(task, event.target.value as TaskStatus)}
                >
                  {taskStatuses.filter((status) => status !== 'ALL').map((status) => <option key={status}>{status}</option>)}
                </select>
                <button className="icon-only danger-button" type="button" onClick={() => deleteTask(task.id)} aria-label={`Delete ${task.title}`}>
                  <Trash2 size={16} />
                </button>
              </div>
            </article>
          ))}
          {tasks.length === 0 && <EmptyState label="No tasks match these filters." />}
        </div>
      </div>
    </section>
  );
}

function JobsView({
  jobs,
  jobStatusFilter,
  setJobStatusFilter,
  importForm,
  setImportForm,
  previewImport,
  jobPreview,
  jobDraft,
  setJobDraft,
  saveJob,
  updateJobStatus,
  deleteJob,
  busy,
}: {
  jobs: JobOpportunity[];
  jobStatusFilter: JobStatus | 'ALL';
  setJobStatusFilter: (value: JobStatus | 'ALL') => void;
  importForm: { sourceUrl: string; pastedText: string };
  setImportForm: React.Dispatch<React.SetStateAction<{ sourceUrl: string; pastedText: string }>>;
  previewImport: (event: FormEvent) => void;
  jobPreview: JobImportPreview | null;
  jobDraft: { title: string; company: string; location: string; sourceUrl: string; sourceName: string; notes: string };
  setJobDraft: React.Dispatch<React.SetStateAction<typeof jobDraft>>;
  saveJob: (event: FormEvent) => void;
  updateJobStatus: (job: JobOpportunity, status: JobStatus) => void;
  deleteJob: (id: string) => void;
  busy: boolean;
}) {
  return (
    <section className="jobs-layout">
      <div className="panel form-panel">
        <PanelTitle icon={<Sparkles />} title="Autofill" />
        <form onSubmit={previewImport}>
          <label>Link<input value={importForm.sourceUrl} onChange={(event) => setImportForm((form) => ({ ...form, sourceUrl: event.target.value }))} /></label>
          <label>Page text<textarea className="tall-textarea" value={importForm.pastedText} onChange={(event) => setImportForm((form) => ({ ...form, pastedText: event.target.value }))} /></label>
          <SubmitButton busy={busy} label="Preview" icon={<Sparkles size={16} />} />
        </form>

        {jobPreview && (
          <form className="review-form" onSubmit={saveJob}>
            <div className="confidence-row">
              <StatusPill label={`Title ${jobPreview.confidence.title}`} />
              <StatusPill label={`Company ${jobPreview.confidence.company}`} />
              <StatusPill label={`Location ${jobPreview.confidence.location}`} />
            </div>
            <label>Title<input required value={jobDraft.title} onChange={(event) => setJobDraft((draft) => ({ ...draft, title: event.target.value }))} /></label>
            <label>Company<input required value={jobDraft.company} onChange={(event) => setJobDraft((draft) => ({ ...draft, company: event.target.value }))} /></label>
            <label>Location<input value={jobDraft.location} onChange={(event) => setJobDraft((draft) => ({ ...draft, location: event.target.value }))} /></label>
            <label>Source<input value={jobDraft.sourceName} onChange={(event) => setJobDraft((draft) => ({ ...draft, sourceName: event.target.value }))} /></label>
            <label>Notes<textarea value={jobDraft.notes} onChange={(event) => setJobDraft((draft) => ({ ...draft, notes: event.target.value }))} /></label>
            <SubmitButton busy={busy} label="Save job" icon={<Save size={16} />} />
          </form>
        )}
      </div>

      <div className="panel list-panel-large">
        <div className="panel-heading with-filter">
          <PanelTitle icon={<BriefcaseBusiness />} title="Job Inbox" />
          <select value={jobStatusFilter} onChange={(event) => setJobStatusFilter(event.target.value as JobStatus | 'ALL')}>
            {jobStatuses.map((status) => <option key={status}>{status}</option>)}
          </select>
        </div>
        <div className="stack-list">
          {jobs.map((job) => (
            <article className="job-item" key={job.id}>
              <div>
                <strong>{job.title}</strong>
                <span>{job.company} · {job.location ?? 'Location open'} · {job.sourceName ?? 'Manual'}</span>
                {job.sourceUrl && <a href={job.sourceUrl} target="_blank" rel="noreferrer"><LinkIcon size={14} /> Open posting</a>}
              </div>
              <div className="job-actions">
                <select value={job.status} onChange={(event) => updateJobStatus(job, event.target.value as JobStatus)}>
                  {jobStatuses.filter((status) => status !== 'ALL').map((status) => <option key={status}>{status}</option>)}
                </select>
                <button className="icon-only danger-button" type="button" onClick={() => deleteJob(job.id)} aria-label={`Delete ${job.title}`}>
                  <Trash2 size={16} />
                </button>
              </div>
            </article>
          ))}
          {jobs.length === 0 && <EmptyState label="No jobs in this filter yet." />}
        </div>
      </div>
    </section>
  );
}

function ContextView({
  areas,
  selectedAreaId,
  setSelectedAreaId,
  projects,
  projectName,
  setProjectName,
  createProject,
  busy,
}: {
  areas: Area[];
  selectedAreaId: string;
  setSelectedAreaId: (value: string) => void;
  projects: Project[];
  projectName: string;
  setProjectName: (value: string) => void;
  createProject: (event: FormEvent) => void;
  busy: boolean;
}) {
  return (
    <section className="two-column">
      <form className="panel form-panel" onSubmit={createProject}>
        <PanelTitle icon={<FolderKanban />} title="Projects" />
        <label>Current area<select value={selectedAreaId} onChange={(event) => setSelectedAreaId(event.target.value)}>
          {areas.map((area) => <option key={area.id} value={area.id}>{area.name}</option>)}
        </select></label>
        <label>New project<input required value={projectName} onChange={(event) => setProjectName(event.target.value)} /></label>
        <SubmitButton busy={busy} label="Create project" />
      </form>
      <div className="panel list-panel-large">
        <PanelTitle icon={<Layers3 />} title="Areas and projects" />
        <div className="area-strip">
          {areas.map((area) => <StatusPill key={area.id} label={area.name} />)}
        </div>
        <div className="stack-list">
          {projects.map((project) => (
            <article className="row-item" key={project.id}>
              <div>
                <strong>{project.name}</strong>
                <span>{project.areaName}</span>
              </div>
            </article>
          ))}
          {projects.length === 0 && <EmptyState label="No projects in this area yet." />}
        </div>
      </div>
    </section>
  );
}

function Metric({ label, value, tone }: { label: string; value: number; tone?: 'warn' }) {
  return (
    <div className={tone === 'warn' ? 'metric warn' : 'metric'}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ListPanel({ icon, title, items, danger }: { icon: React.ReactNode; title: string; items: string[]; danger?: boolean }) {
  return (
    <div className={danger ? 'panel list-panel danger-panel' : 'panel list-panel'}>
      <PanelTitle icon={icon} title={title} />
      <div className="stack-list compact">
        {items.map((item) => <span className="list-line" key={item}>{item}</span>)}
        {items.length === 0 && <EmptyState label="Nothing here." />}
      </div>
    </div>
  );
}

function TaskDashboardPanel({
  icon,
  title,
  tasks,
  updateTaskStatus,
  summary = dashboardTaskSummary,
  danger,
}: {
  icon: React.ReactNode;
  title: string;
  tasks: Task[];
  updateTaskStatus: (task: Task, status: TaskStatus) => void;
  summary?: (task: Task) => string;
  danger?: boolean;
}) {
  return (
    <div className={danger ? 'panel list-panel danger-panel' : 'panel list-panel'}>
      <PanelTitle icon={icon} title={title} />
      <div className="stack-list compact">
        {tasks.map((task) => (
          <article className="dashboard-task-row" key={task.id}>
            <div>
              <strong>{task.title}</strong>
              <span>{summary(task)}</span>
            </div>
            {/* Dashboard task actions stay narrow: finish visible work here, edit details from Tasks. */}
            {task.status !== 'COMPLETED' && (
              <button className="icon-only" type="button" onClick={() => updateTaskStatus(task, 'COMPLETED')} aria-label={`Complete ${task.title}`}>
                <CheckCircle2 size={16} />
              </button>
            )}
          </article>
        ))}
        {tasks.length === 0 && <EmptyState label="Nothing here." />}
      </div>
    </div>
  );
}

function PanelTitle({ icon, title }: { icon: React.ReactNode; title: string }) {
  return (
    <div className="panel-heading">
      {icon}
      <h2>{title}</h2>
    </div>
  );
}

function SubmitButton({ busy, label, icon }: { busy: boolean; label: string; icon?: React.ReactNode }) {
  return (
    <button className="primary-button" type="submit" disabled={busy}>
      {icon ?? <Save size={16} />}
      <span>{busy ? 'Working' : label}</span>
    </button>
  );
}

function StatusPill({ label }: { label: string }) {
  return <span className="status-pill">{label}</span>;
}

function EmptyState({ label }: { label: string }) {
  return <p className="empty-state">{label}</p>;
}

function titleForView(view: View) {
  return {
    today: 'Today Command',
    goals: 'Goals',
    tasks: 'Tasks',
    jobs: 'Job Inbox',
    context: 'Context',
  }[view];
}

function dashboardTaskSummary(task: Task) {
  const context = task.goalTitle ?? task.projectName ?? task.areaName;
  return `${task.priority} - ${context}${task.dueAt ? ` - ${new Date(task.dueAt).toLocaleString()}` : ''}`;
}

function dashboardScheduleSummary(task: Task) {
  const context = task.goalTitle ?? task.projectName ?? task.areaName;
  return `${task.scheduledStart ? new Date(task.scheduledStart).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }) : 'No time'} - ${context}`;
}

function taskSummary(task: Task) {
  return `${task.title} · ${task.priority}${task.dueAt ? ` · ${new Date(task.dueAt).toLocaleString()}` : ''}`;
}

function scheduleSummary(task: Task) {
  return `${task.title} · ${task.scheduledStart ? new Date(task.scheduledStart).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }) : 'No time'}`;
}

function goalSummary(goal: Goal) {
  return `${goal.title} · ${goal.goalType}${goal.targetValue ? ` · ${goal.targetValue} ${goal.unit ?? ''}` : ''}`;
}
