<div align="center">

# ⏰ Task Reminder

### A full‑stack, offline‑capable task & reminder platform with smart notifications, analytics, and recurring workflows.

[![Live App](https://img.shields.io/badge/🚀_Live_App-taskreminder--dun.vercel.app-6366F1?style=for-the-badge)](https://taskreminder-dun.vercel.app/)
[![Frontend Repo](https://img.shields.io/badge/Frontend-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/ashrithBalaji456/Task_Reminder_Frontend)
[![Backend Repo](https://img.shields.io/badge/Backend-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/ashrithBalaji456/Task_Reminder_Backend)

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React_19-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white)
![PWA](https://img.shields.io/badge/PWA-Offline_Ready-5A0FC8?style=flat-square&logo=pwa&logoColor=white)

</div>

> **Note on this document:** This README was reconstructed by inspecting the actual source code in both repositories (entities, controllers, schedulers, and `package.json`/`pom.xml`), so the architecture, endpoints, and entity diagrams reflect what is really in the codebase. I did **not** have access to your deployment dashboards (Render/Railway/Neon/etc.), so exact hosting provider names are shown as placeholders — please swap them for your real providers before publishing.

---

## 📚 Table of Contents

1. [Overview](#-overview)
2. [Live Links](#-live-links)
3. [System Architecture](#-system-architecture)
4. [Tech Stack](#-tech-stack)
5. [Entity Relationship Diagram](#-entity-relationship-diagram)
6. [Core Workflows](#-core-workflows)
   - [Authentication Flow](#1-authentication-flow-jwt--refresh-tokens)
   - [Task Creation & Occurrence Generation](#2-task-creation--occurrence-generation)
   - [Reminder & Notification Pipeline](#3-reminder--notification-pipeline)
   - [Recurring Task Lifecycle](#4-recurring-task-lifecycle)
   - [Weekly / Monthly Report Pipeline](#5-weekly--monthly-report-pipeline)
   - [Offline-First PWA Sync](#6-offline-first-pwa-sync)
7. [Module / Domain Map](#-module--domain-map)
8. [API Surface](#-api-surface)
9. [Deployment Architecture](#-deployment-architecture)
10. [Getting Started Locally](#-getting-started-locally)
11. [Environment Variables](#-environment-variables)
12. [Project Structure](#-project-structure)
13. [Roadmap Ideas](#-roadmap-ideas)

---

## 🧭 Overview

**Task Reminder** is a task management system built around **time-based intent**, not just to‑do lists. Every task compiles down to one or more **occurrences** on the calendar, each occurrence can carry its own reminder policy (single push/email nudge, or a repeating nudge every *N* minutes until acknowledged), and a background scheduler continuously watches the clock to fire reminders, weekly digests, and monthly analytics reports — all while the frontend keeps working offline as an installable PWA.

**Highlights:**
- 🔐 JWT access + refresh token authentication with password reset flow
- 🗓️ One-off & recurring tasks, generating daily/weekly `TaskOccurrence` rows
- 🔔 Dual-channel notifications — **Web Push** (VAPID) and **Email** (Brevo/SMTP)
- 📊 Auto-generated weekly & monthly analytics with server-rendered charts (JFreeChart) embedded in emails
- 📴 Offline-first PWA — IndexedDB cache + background sync queue
- 🕰️ Full task movement history & audit trail (task rescheduled from date → date)
- 📈 In-app analytics dashboard (Recharts) + alerting for overdue/at-risk tasks

---

## 🔗 Live Links

| Resource | Link |
|---|---|
| 🌐 Live Application | **https://taskreminder-dun.vercel.app/** |
| 💻 Frontend Source | **https://github.com/ashrithBalaji456/Task_Reminder_Frontend** |
| ⚙️ Backend Source | **https://github.com/ashrithBalaji456/Task_Reminder_Backend** |

---

## 🏗️ System Architecture

High-level view of how the browser/PWA, API, database, schedulers, and third-party notification providers fit together.

```mermaid
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#6366F1','primaryTextColor':'#fff','primaryBorderColor':'#4338CA','lineColor':'#A5B4FC','secondaryColor':'#22C55E','tertiaryColor':'#0EA5E9'}}}%%
flowchart TB
    subgraph CLIENT["🖥️ Client Layer — Vercel"]
        direction TB
        UI["⚛️ React 19 + TypeScript SPA<br/>Vite build"]
        SW["🔧 Service Worker<br/>(Workbox / vite-plugin-pwa)"]
        IDB[("💾 IndexedDB<br/>offline task cache + sync queue")]
        PUSH_SUB["🔔 Push Subscription<br/>(browser PushManager)"]
        UI <--> SW
        SW <--> IDB
        UI --> PUSH_SUB
    end

    subgraph EDGE["☁️ Edge / CDN"]
        VERCEL["▲ Vercel Edge Network"]
    end

    subgraph API["🧠 Application Layer — Spring Boot 3.3 (Java 17)"]
        direction TB
        GATEWAY["🛡️ Security Filter Chain<br/>JwtAuthenticationFilter"]
        CTRL["📡 REST Controllers<br/>Auth · Task · Recurring · Dashboard<br/>Analytics · History · Alerts · Notifications"]
        SVC["⚙️ Service Layer<br/>Business Logic + Mapping"]
        SCHED["⏱️ Scheduled Jobs<br/>TaskReminderScheduler (60s)<br/>WeeklyReportScheduler (cron)<br/>MonthlyReportScheduler (cron)"]
        CHART["📊 JFreeChart<br/>Server-side chart rendering"]
        GATEWAY --> CTRL --> SVC
        SVC --> CHART
        SCHED --> SVC
    end

    subgraph DATA["🗄️ Data Layer"]
        PG[("🐘 PostgreSQL<br/>Flyway-migrated schema")]
    end

    subgraph EXTERNAL["📡 External Providers"]
        BREVO["✉️ Brevo Email API<br/>transactional + report emails"]
        WEBPUSH["🌐 Web Push Protocol<br/>VAPID-signed"]
        BROWSER_PUSH["🔔 Browser Push Services<br/>(FCM / Mozilla / Apple)"]
    end

    UI -- "HTTPS / REST (Axios)\nJWT Bearer" --> VERCEL --> GATEWAY
    PUSH_SUB -- "subscribe endpoint" --> CTRL
    SVC <--> PG
    SVC -- "send email" --> BREVO
    SVC -- "send push payload" --> WEBPUSH --> BROWSER_PUSH --> PUSH_SUB

    classDef client fill:#EEF2FF,stroke:#6366F1,stroke-width:2px,color:#111
    classDef api fill:#ECFDF5,stroke:#10B981,stroke-width:2px,color:#111
    classDef data fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#111
    classDef ext fill:#FEE2E2,stroke:#EF4444,stroke-width:2px,color:#111
    class UI,SW,IDB,PUSH_SUB client
    class GATEWAY,CTRL,SVC,SCHED,CHART api
    class PG data
    class BREVO,WEBPUSH,BROWSER_PUSH ext
```

> 💡 **Animated tip:** Paste this Markdown into GitHub (native Mermaid support), Obsidian, or [Mermaid Live Editor](https://mermaid.live) — nodes render with smooth transitions and pan/zoom there.

---

## 🧰 Tech Stack

```mermaid
mindmap
  root((Task Reminder))
    Backend
      Spring Boot 3.3.4 · Java 17
      Spring Security + JWT (jjwt 0.12.6)
      Spring Data JPA / Hibernate
      PostgreSQL + Flyway migrations
      H2 (test profile)
      JFreeChart 1.5.4
      Brevo Email API client
      Web Push (VAPID JWT)
      springdoc-openapi (Swagger UI)
    Frontend
      React 19 + TypeScript
      Vite 8 build tool
      React Router 7
      Redux Toolkit + Redux Thunk
      Tailwind CSS 4
      Recharts (analytics charts)
      Framer Motion (animations)
      Axios (HTTP client)
      idb (IndexedDB wrapper)
      vite-plugin-pwa (Workbox SW)
    Infrastructure
      Vercel — Frontend hosting
      Cloud host — Backend (Render/Railway/EC2 style)
      Managed PostgreSQL
      Brevo — Transactional email
      Web Push VAPID keys
```

---

## 🗃️ Entity Relationship Diagram

Reconstructed directly from the JPA `@Entity` classes in `Task_Reminder_Backend`.

```mermaid
erDiagram
    USER ||--o{ TASK_DEFINITION : owns
    USER ||--o{ TASK_OCCURRENCE : owns
    USER ||--o{ TASK_MOVEMENT_HISTORY : "moved by"
    USER ||--o{ REFRESH_TOKEN : has
    USER ||--o{ PUSH_SUBSCRIPTION : registers
    USER ||--o{ EMAIL_NOTIFICATION : "targets"
    USER ||--o{ REMINDER_LOG : "targets"
    USER ||--|| USER_EMAIL_PREFERENCE : configures

    TASK_DEFINITION ||--o{ TASK_OCCURRENCE : generates
    TASK_OCCURRENCE ||--o{ TASK_MOVEMENT_HISTORY : "rescheduled via"
    TASK_OCCURRENCE ||--o{ EMAIL_NOTIFICATION : triggers
    TASK_OCCURRENCE ||--o{ REMINDER_LOG : triggers

    USER {
        Long id PK
        string name
        string email UK
        string passwordHash
        string timezone
        boolean enabled
        string resetToken
        Instant resetTokenExpiry
        Instant createdAt
        Instant updatedAt
    }

    TASK_DEFINITION {
        Long id PK
        Long user_id FK
        string title
        string description
        enum priority
        enum taskType
        enum recurrenceType
        LocalDate startDate
        LocalTime dueTime
        enum reminderOption
        int customReminderMinutes
        int repeatFrequencyMinutes
        string repeatStopCondition
        int maxReminderCount
        boolean notifyByEmail
        boolean notifyByPush
        boolean locked
        Instant createdAt
        Instant updatedAt
    }

    TASK_OCCURRENCE {
        Long id PK
        Long taskDefinition_id FK
        Long user_id FK
        string title
        string description
        enum priority
        LocalDate occurrenceDate
        LocalTime dueTime
        Instant dueDateTime
        enum reminderOption
        int repeatFrequencyMinutes
        int reminderSentCount
        int maxReminderCount
        boolean notifyByEmail
        boolean notifyByPush
        Instant reminderScheduledAt
        enum status
        Instant completedAt
        Instant createdAt
        Instant updatedAt
    }

    TASK_MOVEMENT_HISTORY {
        Long id PK
        Long user_id FK
        Long taskOccurrence_id FK
        LocalDate sourceDate
        LocalDate targetDate
        Instant movedAt
    }

    REFRESH_TOKEN {
        Long id PK
        Long user_id FK
        string token UK
        Instant expiryDate
        Instant createdAt
    }

    PUSH_SUBSCRIPTION {
        Long id PK
        Long user_id FK
        string endpoint
        string p256dhKey
        string authKey
        boolean active
        Instant createdAt
        Instant updatedAt
    }

    EMAIL_NOTIFICATION {
        Long id PK
        Long user_id FK
        Long taskOccurrence_id FK
        enum notificationType
        string periodIdentifier
        Instant scheduledFor
        enum status
        int attemptCount
        int maxAttempts
        Instant nextRetryAt
        string providerMessageId
        Instant sentAt
        string failureReason
        int version
    }

    REMINDER_LOG {
        Long id PK
        Long user_id FK
        Long taskOccurrence_id FK
        string reminderType
        Instant sentAt
        string status
        string brevoMessageId
    }

    USER_EMAIL_PREFERENCE {
        Long id PK
        Long user_id FK
        boolean taskReminderEnabled
        boolean pushNotificationEnabled
        boolean weeklyReportEnabled
        boolean monthlyReportEnabled
        string preferredWeeklyReportDay
        LocalTime preferredWeeklyReportTime
    }
```

**Key design decisions visible in the schema:**
- `TASK_DEFINITION` is the *template* (especially for recurring tasks); `TASK_OCCURRENCE` is the *materialized instance* on a specific date — this is what actually shows up on the calendar and gets reminders.
- `TASK_MOVEMENT_HISTORY` gives a full audit trail every time a task occurrence is dragged/moved to another day (e.g., "move to tomorrow").
- `EMAIL_NOTIFICATION` has its own retry machinery (`attemptCount`, `maxAttempts`, `nextRetryAt`, optimistic-locking `version`) — this is a durable outbox pattern, not fire-and-forget.

---

## 🔄 Core Workflows

### 1. Authentication Flow (JWT + Refresh Tokens)

```mermaid
sequenceDiagram
    autonumber
    actor U as 👤 User
    participant FE as ⚛️ React App
    participant API as 🛡️ AuthController
    participant SVC as ⚙️ AuthService
    participant DB as 🐘 PostgreSQL

    U->>FE: Enter email + password
    FE->>API: POST /api/v1/auth/login
    API->>SVC: authenticate(credentials)
    SVC->>DB: SELECT user WHERE email=?
    DB-->>SVC: user row
    SVC->>SVC: verify password hash (BCrypt)
    SVC->>SVC: generate JWT access token
    SVC->>DB: INSERT refresh_token
    SVC-->>API: AuthResponse{accessToken, refreshToken}
    API-->>FE: 200 OK + tokens
    FE->>FE: store tokens, set AuthContext
    FE->>API: subsequent requests w/ Authorization: Bearer <token>

    rect rgba(99,102,241,0.08)
    note over FE,API: 🔁 When access token expires
    FE->>API: POST /api/v1/auth/refresh {refreshToken}
    API->>SVC: rotate token
    SVC->>DB: validate + rotate refresh_token
    SVC-->>FE: new accessToken
    end
```

### 2. Task Creation & Occurrence Generation

```mermaid
flowchart LR
    A["📝 User fills task form<br/>title, priority, date, reminder rule"] --> B{"🔁 Recurring?"}
    B -- "No — one-off" --> C["POST /api/v1/tasks"]
    B -- "Yes" --> D["POST /api/v1/recurring-tasks"]
    C --> E["TaskService.createTask()"]
    D --> F["RecurringTaskService.create()"]
    E --> G[("TaskDefinition row")]
    F --> G
    G --> H["Generate TaskOccurrence(s)\nfor applicable date(s)"]
    H --> I[("TaskOccurrence rows")]
    I --> J["Compute reminderScheduledAt\nbased on reminderOption /\ncustomReminderMinutes"]
    J --> K["✅ Task appears on\nDashboard / TasksPage"]

    style A fill:#EEF2FF,stroke:#6366F1
    style K fill:#DCFCE7,stroke:#16A34A
    style G fill:#FEF9C3,stroke:#CA8A04
    style I fill:#FEF9C3,stroke:#CA8A04
```

### 3. Reminder & Notification Pipeline

The heart of the system — `TaskReminderScheduler` ticks **every 60 seconds**, scanning for due reminders and routing them through the durable outbox to Email and Push channels.

```mermaid
sequenceDiagram
    autonumber
    participant CRON as ⏱️ TaskReminderScheduler<br/>(fixedRate = 60s)
    participant DB as 🐘 PostgreSQL
    participant NOTI as 📮 EmailNotification Outbox
    participant EMAILSVC as ✉️ EmailSenderService
    participant BREVO as 🟢 Brevo API
    participant PUSHSVC as 🔔 WebPushService
    participant BROWSER as 🌐 Browser Push Service
    actor U as 👤 User's Device

    loop every 60 seconds
        CRON->>DB: enqueueDueReminders(now)
        DB-->>CRON: TaskOccurrences due for reminder
        CRON->>NOTI: create PENDING EmailNotification rows
        CRON->>CRON: processTaskReminders()
        par Email Channel
            CRON->>EMAILSVC: send(notification)
            EMAILSVC->>BREVO: transactional email API call
            alt success
                BREVO-->>EMAILSVC: 202 Accepted + messageId
                EMAILSVC->>DB: status = SENT, providerMessageId
            else failure
                BREVO-->>EMAILSVC: error
                EMAILSVC->>DB: attemptCount++, nextRetryAt = backoff
                note right of DB: retried next tick until maxAttempts
            end
        and Push Channel
            CRON->>PUSHSVC: sendPush(subscription, payload)
            PUSHSVC->>PUSHSVC: sign payload with VAPID JWT
            PUSHSVC->>BROWSER: POST push message
            BROWSER-->>U: 🔔 native OS notification
        end
        CRON->>DB: increment reminderSentCount on TaskOccurrence
        alt reminderOption == REPEAT and under maxReminderCount
            DB-->>CRON: reschedule next reminder tick
        else
            DB-->>CRON: mark reminder cycle complete
        end
    end
```

### 4. Recurring Task Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Created: User defines recurrence rule
    Created --> Active: TaskDefinition saved (locked=false)
    Active --> OccurrenceGenerated: Scheduler/service materializes\nTaskOccurrence for date
    OccurrenceGenerated --> Pending: status = PENDING
    Pending --> RemindersFiring: reminderScheduledAt reached
    RemindersFiring --> Completed: user marks complete\n(PATCH /tasks/{id}/complete)
    RemindersFiring --> Moved: user moves to another day\n(PATCH /tasks/{id}/move)
    Moved --> Pending: new TaskOccurrence date,\nTaskMovementHistory logged
    Pending --> Cancelled: user cancels
    Active --> Locked: PATCH /recurring-tasks/{id}/lock
    Locked --> Active: PATCH /recurring-tasks/{id}/unlock
    Completed --> [*]
    Cancelled --> [*]
```

### 5. Weekly / Monthly Report Pipeline

```mermaid
flowchart TB
    subgraph Weekly["📅 WeeklyReportScheduler — cron: Sun 18:00 (configurable)"]
        W1["Trigger for each user\nwith weeklyReportEnabled=true"] --> W2["AnalyticsService\naggregate week's TaskOccurrences"]
        W2 --> W3["ChartGenerationService\nrender JFreeChart PNG"]
        W3 --> W4["WeeklyReportEmailTemplate\nbuild HTML email"]
        W4 --> W5["EmailSenderService → Brevo"]
    end

    subgraph Monthly["🗓️ MonthlyReportScheduler — cron: 1st @ 08:00 (configurable)"]
        M1["Trigger for each user\nwith monthlyReportEnabled=true"] --> M2["AnalyticsService\naggregate month's stats"]
        M2 --> M3["ChartGenerationService\nrender monthly trend chart"]
        M3 --> M4["MonthlyReportEmailTemplate"]
        M4 --> M5["EmailSenderService → Brevo"]
    end

    W5 --> INBOX["📬 User's Inbox"]
    M5 --> INBOX

    style Weekly fill:#EFF6FF,stroke:#3B82F6
    style Monthly fill:#F5F3FF,stroke:#8B5CF6
    style INBOX fill:#ECFDF5,stroke:#10B981
```

### 6. Offline-First PWA Sync

```mermaid
flowchart TB
    A["📴 User goes offline"] --> B["useNetworkStatus hook\ndetects offline"]
    B --> C["OfflineBanner shown"]
    C --> D["User creates/edits task"]
    D --> E["offlineStorage.ts\nwrite to IndexedDB (idb)"]
    E --> F["syncQueue.ts\nqueue pending mutation"]
    F --> G{"🌐 Back online?"}
    G -- No --> F
    G -- Yes --> H["Service Worker background sync\nflush syncQueue"]
    H --> I["Replay queued requests\nagainst REST API"]
    I --> J["taskStorage.ts\nreconcile local cache\nwith server response"]
    J --> K["✅ UI updates, banner clears"]

    style A fill:#FEE2E2,stroke:#EF4444
    style K fill:#DCFCE7,stroke:#16A34A
```

---

## 🧩 Module / Domain Map

The backend is organized as **feature-sliced packages** (not classic layered MVC) — each domain owns its controller/dto/service/entity/repository.

```mermaid
graph TD
    ROOT["com.application.taskmanager"]
    ROOT --> AUTH["🔐 auth\ncontroller · dto · entity · repository · service"]
    ROOT --> USER["👤 user\nentity · model · repository · service"]
    ROOT --> TASK["✅ task\ncontroller · dto · entity · repository · service"]
    ROOT --> RECUR["🔁 recurring\ncontroller · dto · service"]
    ROOT --> REMINDER["🔔 reminder\nentity · repository · service"]
    ROOT --> NOTIF["📮 notification\nclient · controller · dto · entity\nrepository · scheduler · service · template · util"]
    ROOT --> ANALYTICS["📊 analytics\nchart · controller · dto · service"]
    ROOT --> DASH["🧮 dashboard\ncontroller · dto · service"]
    ROOT --> HIST["🕰️ history\ncontroller · dto · service"]
    ROOT --> ALERT["🚨 alert\ncontroller · dto · service"]
    ROOT --> SEC["🛡️ security\nJWT filter · provider · UserDetailsService"]
    ROOT --> COMMON["🧱 common\nApiResponse · ErrorResponse · HealthController"]
    ROOT --> EXC["⚠️ exception\nGlobalExceptionHandler + custom exceptions"]
    ROOT --> CFG["⚙️ config\nOpenApiConfig"]

    classDef d fill:#EEF2FF,stroke:#6366F1,color:#111
    class AUTH,USER,TASK,RECUR,REMINDER,NOTIF,ANALYTICS,DASH,HIST,ALERT,SEC,COMMON,EXC,CFG d
```

---

## 📡 API Surface

All endpoints are versioned under `/api/v1`. Extracted directly from the controller annotations in the backend repo.

| Domain | Method | Endpoint | Purpose |
|---|---|---|---|
| **Auth** | POST | `/api/v1/auth/register` | Create account |
| | POST | `/api/v1/auth/login` | Login → access + refresh token |
| | POST | `/api/v1/auth/refresh` | Rotate access token |
| | POST | `/api/v1/auth/logout` | Invalidate refresh token |
| | POST | `/api/v1/auth/forgot-password` | Request reset email |
| | POST | `/api/v1/auth/reset-password` | Reset password via token |
| **Tasks** | POST | `/api/v1/tasks` | Create task |
| | GET | `/api/v1/tasks` | List tasks |
| | GET | `/api/v1/tasks/{id}` | Get task |
| | GET | `/api/v1/tasks/today` \| `/tomorrow` \| `/pending` | Filtered views |
| | PUT | `/api/v1/tasks/{id}` | Update task |
| | DELETE | `/api/v1/tasks/{id}` | Delete task |
| | PATCH | `/api/v1/tasks/{id}/complete` | Mark complete |
| | PATCH | `/api/v1/tasks/{id}/move` \| `/move/tomorrow` | Reschedule |
| **Recurring Tasks** | POST/GET/PUT/DELETE | `/api/v1/recurring-tasks` | CRUD |
| | PATCH | `/api/v1/recurring-tasks/{id}/lock` \| `/unlock` | Freeze/unfreeze template |
| **Dashboard** | GET | `/api/v1/dashboard/daily` | Daily summary |
| **Analytics** | GET | `/api/v1/analytics/weekly` \| `/monthly` | Aggregated stats |
| **History** | GET | `/api/v1/history` | Task movement / completion history |
| **Alerts** | GET | `/api/v1/alerts` | At-risk / overdue task alerts |
| **Notifications** | GET | `/api/v1/notifications` | Notification log |
| | GET | `/api/v1/push-subscriptions/vapid-public-key` | Fetch VAPID key |
| | POST/DELETE | `/api/v1/push-subscriptions` | Register/remove push device |
| | POST | `/api/v1/notifications/test/*` | Manual test triggers (reminder/push/reports) |
| **User** | GET/PUT | `/api/v1/users/email-preferences` | Notification preferences |
| **System** | GET | `/health`, `/api/v1/health` | Health check |

> 📘 Interactive API docs are available via **springdoc-openapi** at `/swagger-ui.html` when the backend is running.

---

## 🚀 Deployment Architecture

```mermaid
flowchart LR
    subgraph GH["🐙 GitHub"]
        FE_REPO["Task_Reminder_Frontend"]
        BE_REPO["Task_Reminder_Backend"]
    end

    subgraph CI["🔧 Build & Deploy"]
        FE_BUILD["Vite build\n(tsc -b && vite build)"]
        BE_BUILD["Maven build\n(Spring Boot fat jar)"]
    end

    subgraph HOST["☁️ Hosting"]
        VERCEL["▲ Vercel\n(static SPA + PWA assets)"]
        BACKEND_HOST["🖥️ Backend Host\n(container / PaaS — configure per your provider)"]
        PGHOST[("🐘 Managed PostgreSQL")]
    end

    FE_REPO -- "push to main" --> FE_BUILD --> VERCEL
    BE_REPO -- "push to main" --> BE_BUILD --> BACKEND_HOST
    BACKEND_HOST --> PGHOST
    VERCEL -- "HTTPS API calls" --> BACKEND_HOST

    U["🌍 End Users"] --> VERCEL

    style VERCEL fill:#000,color:#fff,stroke:#333
    style BACKEND_HOST fill:#111827,color:#fff
    style PGHOST fill:#1E3A8A,color:#fff
```

> ⚠️ **Please verify:** the diagram above shows the *shape* of the deployment. I did not have access to your actual infra dashboard, so replace "Backend Host" with your real provider (e.g. Render, Railway, Fly.io, AWS EC2) and confirm the Postgres provider name before publishing.

---

## 🛠️ Getting Started Locally

### Backend

```bash
git clone https://github.com/ashrithBalaji456/Task_Reminder_Backend.git
cd Task_Reminder_Backend

# configure src/main/resources/application.yml or environment variables
# (DB URL, JWT secret, Brevo API key, VAPID keys — see below)

./mvnw clean install
./mvnw spring-boot:run
# Backend runs on http://localhost:8080 by default
```

### Frontend

```bash
git clone https://github.com/ashrithBalaji456/Task_Reminder_Frontend.git
cd Task_Reminder_Frontend

npm install
npm run dev
# Frontend runs on http://localhost:5173 by default (Vite)
```

---

## 🔑 Environment Variables

> These are inferred from the dependencies and service classes present in the backend (JWT, Postgres, Flyway, Brevo, VAPID). Please confirm exact variable **names** against `application.yml` / `application.properties` in the backend repo, since that file wasn't included in the uploaded archive.

| Variable | Used For |
|---|---|
| `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | Signing JWT access/refresh tokens |
| `BREVO_API_KEY` | Transactional email sending |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | Web Push signing |
| `APP_SCHEDULER_WEEKLY_REPORT_CRON` | Overrides default `0 0 18 * * SUN` |
| `APP_SCHEDULER_MONTHLY_REPORT_CRON` | Overrides default `0 0 8 1 * *` |
| `VITE_API_BASE_URL` (frontend) | Points SPA to backend API origin |

---

## 📁 Project Structure

```
Task Reminder/
├── Task_Reminder_Backend/          # Spring Boot 3.3 · Java 17
│   └── src/main/java/com/application/taskmanager/
│       ├── auth/          # login, register, refresh, password reset
│       ├── user/          # user entity + email preferences
│       ├── task/          # TaskDefinition, TaskOccurrence, movement history
│       ├── recurring/     # recurring task templates
│       ├── reminder/      # ReminderLog + Brevo email service
│       ├── notification/  # outbox pattern: email + push + schedulers
│       ├── analytics/     # weekly/monthly aggregation + chart generation
│       ├── dashboard/     # daily summary endpoint
│       ├── history/       # historical task activity
│       ├── alert/         # overdue/at-risk task alerts
│       ├── security/      # JWT filter, provider, UserDetailsService
│       ├── common/        # ApiResponse, ErrorResponse, health check
│       ├── exception/     # global exception handling
│       └── config/        # OpenAPI/Swagger config
│
└── Task_Reminder_Frontend/          # React 19 · TypeScript · Vite
    └── src/
        ├── api/            # Axios client + endpoints
        ├── components/     # shared UI components
        ├── context/        # AuthContext, SidebarContext, ToastContext
        ├── hooks/          # useNetworkStatus, etc.
        ├── pages/          # Landing, Login, Register, Dashboard,
        │                   # Tasks, Recurring, History, Analytics,
        │                   # Alerts, Preferences, Cancelled Tasks
        ├── pwa/             # offline storage, sync queue, push service
        ├── routes/         # AppRoutes, ProtectedRoute
        └── types/          # shared TS types
```

---

## 🧭 Roadmap Ideas

- [ ] Add real-time updates via WebSocket/SSE instead of polling-based reminder checks
- [ ] Multi-device push subscription management UI
- [ ] Timezone-aware recurring task previews before saving
- [ ] Export analytics reports as downloadable PDF (in addition to email)
- [ ] Team/shared task lists

---

<div align="center">

Made with ☕, Spring Boot, and React — by **Ashrith Balaji**

[🌐 Live App](https://taskreminder-dun.vercel.app/) · [Frontend](https://github.com/ashrithBalaji456/Task_Reminder_Frontend) · [Backend](https://github.com/ashrithBalaji456/Task_Reminder_Backend)

</div>
