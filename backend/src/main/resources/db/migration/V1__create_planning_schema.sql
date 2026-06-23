-- Areas are the required top-level context for every planning item.
-- They keep different parts of life separated without forcing every item into a project.
CREATE TABLE areas (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL
);

-- Projects are optional sub-contexts within an area. The unique constraint keeps
-- autocomplete/free-create behavior from producing duplicate project names per area.
CREATE TABLE projects (
    id UUID PRIMARY KEY,
    area_id UUID NOT NULL REFERENCES areas(id),
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_projects_area_name UNIQUE (area_id, name)
);

-- Goals measure outcomes. Goal type controls how progress is calculated; recurrence
-- and target dates are properties that can apply to any goal type.
CREATE TABLE goals (
    id UUID PRIMARY KEY,
    area_id UUID NOT NULL REFERENCES areas(id),
    project_id UUID REFERENCES projects(id),
    title VARCHAR(160) NOT NULL,
    description TEXT,
    goal_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    recurrence VARCHAR(32) NOT NULL,
    target_value NUMERIC(12, 2),
    unit VARCHAR(40),
    target_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- Recurring goals stay visible as one goal in the UI, while periods preserve
-- weekly/monthly history for reviews, charts, and trend tracking.
CREATE TABLE goal_periods (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    target_value NUMERIC(12, 2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_goal_periods_goal_range UNIQUE (goal_id, period_start, period_end)
);

-- Check-ins record manual progress without requiring users to create artificial tasks.
CREATE TABLE goal_check_ins (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    goal_period_id UUID REFERENCES goal_periods(id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL,
    note TEXT,
    checked_in_on DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- Tasks represent captured work, ideas, or routines. Reminders and scheduling are
-- fields on the task because any task type can be brought back later or calendar-blocked.
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    area_id UUID NOT NULL REFERENCES areas(id),
    project_id UUID REFERENCES projects(id),
    goal_id UUID REFERENCES goals(id) ON DELETE SET NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    recurrence VARCHAR(32) NOT NULL,
    due_at TIMESTAMPTZ,
    remind_at TIMESTAMPTZ,
    scheduled_start TIMESTAMPTZ,
    scheduled_end TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- Foreign-key and date indexes keep common dashboard/filter queries responsive.
CREATE INDEX idx_projects_area_id ON projects(area_id);
CREATE INDEX idx_goals_area_id ON goals(area_id);
CREATE INDEX idx_goals_project_id ON goals(project_id);
CREATE INDEX idx_goal_periods_goal_id ON goal_periods(goal_id);
CREATE INDEX idx_goal_check_ins_goal_id ON goal_check_ins(goal_id);
CREATE INDEX idx_tasks_area_id ON tasks(area_id);
CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_goal_id ON tasks(goal_id);
CREATE INDEX idx_tasks_due_at ON tasks(due_at);

-- Stable IDs make seeded areas easy to reference in tests, seed data, and demos.
INSERT INTO areas (id, name, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Career', 10),
    ('00000000-0000-0000-0000-000000000002', 'Personal', 20),
    ('00000000-0000-0000-0000-000000000003', 'School', 30),
    ('00000000-0000-0000-0000-000000000004', 'Business', 40);
