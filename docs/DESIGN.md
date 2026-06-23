# Jat Design

Jat is a local-first command center for goals, tasks, career execution, job tracking, and project work. It is designed for fast capture first, then structured planning after.

## Product Principles

- Capture should be quick and forgiving.
- The app should prompt for structure, but not block saving.
- Areas are required because they define context.
- Projects and goals are optional because not every item needs that much structure.
- Task types should only exist when they change behavior.
- Goals measure outcomes; tasks represent actions.

## Core Hierarchy

```text
Area
  -> Project
      -> Goal
          -> Task
```

The hierarchy is flexible:

```text
Area: required
Project: optional
Goal: optional
Task: required
```

Examples:

```text
Career -> Portfolio -> Finish project milestone -> Complete API endpoint
Career -> Applications -> Reach weekly application target -> Submit application
Business -> Product Research -> Evaluate new opportunity -> Review market example
School -> Course Work -> Prepare for exam -> Review lecture notes
Personal -> no project -> no goal -> Schedule appointment
```

## Default Areas

The first version starts with exactly these areas:

- Career
- Personal
- School
- Business

Users can add more later, but these four should exist automatically so the app is useful immediately.

## Projects

Projects belong to an area and are optional on tasks and goals.

In the UI, projects should feel like free text with autocomplete. If a user types a new project name, Jat can create the project record behind the scenes.

Examples:

- Portfolio
- Applications
- Client Work
- Product Research
- Course Work
- Personal Admin

## Task Types

Task types should stay small and meaningful.

```text
ACTION
IDEA
ROUTINE
```

**ACTION** is a concrete thing to complete.

Examples:

- Submit application to Company X
- Finish local launcher
- Record product walkthrough

**IDEA** is captured thinking that is not committed work yet.

Examples:

- New product concept
- Automation workflow idea
- Market research note

**ROUTINE** is repeated work or behavior.

Examples:

- Practice Java daily
- Review goals every morning
- Complete focused work block every weekday

Reminder is not a task type. Any task can have reminder fields.

Task scheduling fields:

```text
due_at
remind_at
scheduled_start
scheduled_end
recurrence
```

## Goal Types

Goal types describe how progress is measured.

```text
CHECKLIST
TARGET
HABIT
```

**CHECKLIST** progress comes from linked tasks or milestones.

Example:

```text
Ship project MVP
Progress: 7 / 20 linked tasks complete
```

**TARGET** progress is numeric.

Examples:

```text
Complete 20 applications/week
Send 10 outreach messages/day
Finish 15 practice problems this month
```

**HABIT** progress is consistency-based.

Examples:

```text
Practice Java daily
Review goals every morning
Write project notes twice a week
```

Goal properties are separate from goal type:

```text
area_id
project_id
target_date
recurrence
target_value
unit
```

## Recurrence

Tasks and goals can both recur, but they mean different things.

```text
Recurring task = repeated action
Recurring goal = repeated progress period
```

Example:

```text
Recurring goal: Practice technical skills 5 days/week
Recurring task: Study for 30 minutes daily
```

Completing the recurring task can contribute to the recurring goal.

For V1, recurrence should start simple:

```text
NONE
DAILY
WEEKLY
MONTHLY
```

## Goal Periods

Recurring goals use a hybrid model.

In the UI, a recurring goal appears as one goal:

```text
Complete 20 applications/week
```

Behind the scenes, progress is stored per period:

```text
Week 1: 8 / 20
Week 2: 0 / 20
```

This keeps the daily interface simple while preserving history for reviews and charts.

Recurring goals should not automatically copy every linked task into the next period by default. Repeated actions should be represented by recurring tasks. Later, Jat can support optional task templates for goals if needed.

## Progress Sources

Goal progress can come from automatic and manual sources.

Automatic progress:

- Completed linked tasks
- Later: submitted job applications
- Later: outreach records

Manual progress:

- Check-ins entered directly by the user

Example:

```text
Goal: Complete 20 applications/week
Automatic: completed "Submit application" tasks add +1
Manual: "Submitted 4 applications today" adds +4
```

Design rule:

```text
Tasks are actions.
Check-ins are measurements.
Goals are outcomes.
```

## Initial Data Model

The first planning schema should include:

```text
areas
projects
goals
goal_periods
goal_check_ins
tasks
```

Later modules can add:

```text
job_opportunities
applications
resume_documents
notes
tags
automation_sources
ai_runs
```

## Capture Flow

Jat should support messy input without losing structure.

```text
Quick capture
  -> suggest area, project, goal, and type
  -> user accepts, edits, or skips suggestions
  -> item is saved either way
```

Example:

```text
Input: Review competitor launch
Suggested area: Business
Suggested project: Product Research
Suggested type: IDEA
Suggested goal: Evaluate new opportunities
```

The app should prompt, not force.
