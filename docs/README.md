# Jat - Life Command Center

Jat is a local-first personal productivity and job-search command center built as a full-stack Java project.

## Architecture

```text
Windows Launcher
  -> Docker Compose
      -> React + TypeScript frontend on localhost:5173
      -> Spring Boot REST API on localhost:8080
      -> PostgreSQL database on localhost:5432
```

The frontend never talks directly to the database. It calls Spring Boot REST endpoints under `/api/*`. The backend owns validation, business logic, and persistence.

The planning model centers on required areas, optional projects, goals, tasks, recurring progress periods, and manual check-ins. See [DESIGN.md](./DESIGN.md) for the full product model.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway migrations
- Spring Boot Actuator
- React 19
- TypeScript
- Vite
- Docker Compose

## MVP Scope

- Areas and projects
- Goals with progress tracking
- Tasks linked to areas, projects, and goals
- Calendar planning blocks
- Dashboard / Today view
- Simple Job Inbox
- Notes and tags

Authentication is intentionally out of scope for V1 because the app is local-only.

## Local Development

For normal use on Windows, run the launcher:

```powershell
.\launcher\start-jat.ps1
```

You can also double-click:

```text
launcher/Start Jat.cmd
```

The launcher checks Docker, starts the local services, waits for the backend and frontend to respond, then opens Jat in the browser.

For manual development, run Docker Compose directly:

```powershell
docker compose up --build
```

Frontend: http://localhost:5173
Backend: http://localhost:8080
Backend health: http://localhost:8080/actuator/health

## Launcher Options

Create or refresh a Desktop shortcut:

```powershell
.\launcher\install-desktop-shortcut.ps1
```

You can also double-click:

```text
launcher/Install Desktop Shortcut.cmd
```

This creates `Jat.lnk` on the Windows Desktop and stores the shortcut icon at `%LOCALAPPDATA%\Jat\jat.ico`.

Run without opening the browser:

```powershell
.\launcher\start-jat.ps1 -NoBrowser
```

Use a longer startup timeout:

```powershell
.\launcher\start-jat.ps1 -TimeoutSeconds 300
```
