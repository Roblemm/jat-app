import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { App } from './App';

const areas = [
  { id: 'career-id', name: 'Career', displayOrder: 1 },
  { id: 'personal-id', name: 'Personal', displayOrder: 2 },
];

const emptyDashboard = {
  date: '2026-06-26',
  tasksDueToday: [],
  overdueTasks: [],
  scheduledBlocks: [],
  activeGoals: [],
  counts: { dueToday: 0, overdue: 0, scheduled: 0, activeGoals: 0 },
};

const task = {
  id: 'task-id',
  areaId: 'career-id',
  areaName: 'Career',
  projectId: null,
  projectName: null,
  goalId: null,
  goalTitle: null,
  title: 'Send application',
  description: null,
  taskType: 'ACTION',
  status: 'TODO',
  priority: 'HIGH',
  recurrence: 'NONE',
  dueAt: null,
  remindAt: null,
  scheduledStart: null,
  scheduledEnd: null,
  completedAt: null,
  createdAt: null,
  updatedAt: null,
};

const dashboardWithDueTask = {
  ...emptyDashboard,
  tasksDueToday: [task],
  counts: { ...emptyDashboard.counts, dueToday: 1 },
};

describe('App', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.startsWith('/api/areas')) {
        return jsonResponse(areas);
      }

      if (url.startsWith('/api/dashboard/today')) {
        return jsonResponse(dashboardWithDueTask);
      }

      if (url === '/api/tasks/task-id/status' && init?.method === 'PATCH') {
        return jsonResponse({ ...task, status: 'COMPLETED', completedAt: '2026-06-26T12:00:00Z' });
      }

      if (url.startsWith('/api/tasks')) {
        return jsonResponse([task]);
      }

      if (url.startsWith('/api/projects') || url.startsWith('/api/goals') || url.startsWith('/api/job-opportunities')) {
        return jsonResponse([]);
      }

      return jsonResponse({ message: 'Not found' }, 404);
    }));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders the usable app shell first', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: /today command/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /jobs/i })).toBeInTheDocument();
  });

  it('lets the context page change the active area', async () => {
    render(<App />);

    fireEvent.click(screen.getByRole('button', { name: /context/i }));

    const contextAreaSelect = await screen.findByLabelText(/current area/i);
    fireEvent.change(contextAreaSelect, { target: { value: 'personal-id' } });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /context/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/current area/i)).toHaveValue('personal-id');
    });
  });

  it('updates task status from the task list', async () => {
    const fetchMock = vi.mocked(fetch);
    render(<App />);

    fireEvent.click(screen.getByRole('button', { name: /tasks/i }));

    const statusSelect = await screen.findByLabelText(/status for send application/i);
    fireEvent.change(statusSelect, { target: { value: 'COMPLETED' } });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/tasks/task-id/status',
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify({ status: 'COMPLETED' }),
        }),
      );
    });
  });

  it('completes a due task from the today dashboard', async () => {
    const fetchMock = vi.mocked(fetch);
    render(<App />);

    const completeButton = await screen.findByRole('button', { name: /complete send application/i });
    fireEvent.click(completeButton);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/tasks/task-id/status',
        expect.objectContaining({
          method: 'PATCH',
          body: JSON.stringify({ status: 'COMPLETED' }),
        }),
      );
    });
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
