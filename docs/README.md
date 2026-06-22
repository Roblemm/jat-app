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

## Stack

- Java 21
- Spring Boot 3.3+
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

- Goals CRUD
- Tasks linked to goals
- Calendar planning blocks
- Dashboard / Today view
- Simple Job Inbox
- Notes and tags

Authentication is intentionally out of scope for V1 because the app is local-only.

## Local Development

```powershell
docker compose up --build
```

Frontend: http://localhost:5173
Backend: http://localhost:8080
Backend health: http://localhost:8080/actuator/health

## One-Click Launcher

Run:

```powershell
.\launcher\start-jat.ps1
```

The launcher starts Docker Compose, waits for the backend health endpoint, and opens the frontend in the browser.
