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

describe('App', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.startsWith('/api/areas')) {
        return jsonResponse(areas);
      }

      if (url.startsWith('/api/dashboard/today')) {
        return jsonResponse(emptyDashboard);
      }

      if (url.startsWith('/api/projects') || url.startsWith('/api/goals') || url.startsWith('/api/tasks') || url.startsWith('/api/job-opportunities')) {
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
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
