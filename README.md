# TeamSphere

TeamSphere is a full-stack internal workforce operations platform that combines time entry, attendance, leave management, project allocation, approvals, reporting, and release/issue workflows.

## Repository Structure

- `org-chart/` - Angular frontend application
- `backend/capsAllocation/` - Spring Boot backend service
- `install_dependecies.sh` - Environment bootstrap script (Java, Node.js, Angular CLI, prerequisites)
- `postgres_check_install.sh` - PostgreSQL install/check/start script with database setup

## What This Application Does

TeamSphere provides:

- Authentication, password reset, and OTP-based recovery
- Role-based access control for user/admin workflows
- Time entry submission, review, approval, and reminders
- Attendance tracking and compliance monitoring
- Leave and WFH request lifecycle management
- Project and project-assignment management
- Org-chart and employee visibility
- Operational reports and utilization insights
- Issue tracking and release management

## Setup Guide (Recommended Order)

### 1. Install Base Dependencies

Run from the repository root:

```bash
./install_dependecies.sh
```

This script installs/checks:

- `unzip`, `curl`
- Java 21 (via SDKMAN)
- Node.js `v18.19.0` (via NVM)
- Angular CLI 16
- Note: If anything is missing download yourself

### 2. Install and Verify PostgreSQL

Run from the repository root:

```bash
./postgres_check_install.sh
```

This script installs PostgreSQL (if missing), starts/enables the service, sets password for `postgres`, and checks/creates database `vbs_allocation_caps`.

If database creation fails or does not exist, create it manually:

```bash
sudo -u postgres psql
CREATE DATABASE vbs_allocation_caps;
GRANT ALL PRIVILEGES ON DATABASE vbs_allocation_caps TO postgres;
\q
```

### 3. Backend Setup and Run (Maven + Java)

Move to backend:

```bash
cd backend/capsAllocation
```

Start backend using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

Backend runs by default on:

- `http://localhost:8080`

Database defaults used by backend:

- DB name: `vbs_allocation_caps`
- Username: `postgres`
- Password: `voyage`

### 4. Frontend Setup and Run (NVM + Node.js + NPM)

Open a new terminal and move to frontend:

```bash
cd org-chart
```

Install dependencies:

```bash
npm install
```

Run Angular app:

```bash
npm start
```

Frontend runs on:

- `http://localhost:4200`

The frontend is configured to use backend API URL:

- `org-chart/src/environments/environment.ts` -> `apiUrl: 'http://localhost:8080'`

## Common Commands

Frontend (`org-chart`):

- `npm start` - Start dev server
- `npm run build` - Build production bundle
- `npm test` - Run unit tests

Backend (`backend/capsAllocation`):

- `./mvnw spring-boot:run` - Run service
- `./mvnw test` - Run tests

## Notes

- If `nvm` is not loaded in a new shell, source it before using Node-managed versions.
- Some backend integrations (mail, Google Drive/Sheets) may require additional credentials for full functionality.
