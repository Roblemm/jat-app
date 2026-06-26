import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { App } from './App';

describe('App', () => {
  it('renders the usable app shell first', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: /today command/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /jobs/i })).toBeInTheDocument();
  });
});
