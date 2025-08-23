# CAFM Database Complete Visual Diagrams

## 1. Main Entity Relationship Diagram (Core Business Tables)

```mermaid
erDiagram
    users {
        uuid id PK
        string email UK
        string username UK
        string password_hash
        string first_name
        string last_name
        string phone
        string employee_id UK
        enum user_type
        enum status
        string full_name
        boolean is_active
        timestamp deleted_at
        numeric performance_rating
        integer productivity_score
    }
    
    schools {
        uuid id PK
        string code UK
        string name
        string type
        string gender
        string address
        string city
        decimal latitude
        decimal longitude
        boolean is_active
        numeric maintenance_score
        string activity_level
        timestamp deleted_at
    }
    
    reports {
        uuid id PK
        string report_number UK
        uuid school_id FK
        uuid supervisor_id FK
        string title
        text description
        enum status
        enum priority
        date reported_date
        date scheduled_date
        date completed_date
        decimal estimated_cost
        decimal actual_cost
        integer age_days
        boolean is_overdue
        numeric efficiency_score
        timestamp deleted_at
    }
    
    maintenance_counts {
        uuid id PK
        uuid school_id FK
        uuid supervisor_id FK
        date count_date
        string category
        string item_name
        integer total_count
        integer working_count
        integer damaged_count
        integer missing_count
        timestamp deleted_at
    }
    
    damage_counts {
        uuid id PK
        uuid school_id FK
        uuid supervisor_id FK
        date assessment_date
        string area
        string damage_type
        string severity
        text description
        decimal estimated_repair_cost
        timestamp deleted_at
    }
    
    supervisor_schools {
        uuid id PK
        uuid supervisor_id FK
        uuid school_id FK
        timestamp assigned_at
        boolean is_active
        timestamp deleted_at
    }
    
    notifications {
        uuid id PK
        uuid user_id FK
        string title
        text body
        jsonb data
        boolean read
        timestamp created_at
        timestamp deleted_at
    }
    
    attachments {
        uuid id PK
        string entity_type
        uuid entity_id
        string attachment_type
        text file_url
        string file_name
        bigint file_size
        uuid uploaded_by FK
        timestamp deleted_at
    }
    
    users ||--o{ reports : "creates"
    users ||--o{ reports : "assigned_to"
    users ||--o{ supervisor_schools : "assigned"
    users ||--o{ maintenance_counts : "performs"
    users ||--o{ damage_counts : "assesses"
    users ||--o{ notifications : "receives"
    users ||--o{ attachments : "uploads"
    
    schools ||--o{ reports : "has"
    schools ||--o{ supervisor_schools : "supervised_by"
    schools ||--o{ maintenance_counts : "tracked"
    schools ||--o{ damage_counts : "assessed"
    
    reports ||--o{ attachments : "has_files"
    maintenance_counts ||--o{ attachments : "has_photos"
    damage_counts ||--o{ attachments : "documented"
```

## 2. User Management Domain Diagram

```mermaid
graph TB
    subgraph "User Core"
        users[fa:fa-users Users]
        roles[fa:fa-shield-alt Roles]
        user_roles[fa:fa-link User-Roles]
    end
    
    subgraph "Authentication"
        refresh_tokens[fa:fa-key Refresh Tokens]
        password_reset[fa:fa-unlock Password Reset]
        fcm_tokens[fa:fa-mobile FCM Tokens]
    end
    
    subgraph "User Extensions"
        profiles[fa:fa-id-card Profiles]
        technicians[fa:fa-wrench Technicians]
        admins[fa:fa-crown Admins View]
        supervisors[fa:fa-hard-hat Supervisors View]
    end
    
    users --> user_roles
    roles --> user_roles
    users --> refresh_tokens
    users --> password_reset
    users --> fcm_tokens
    users --> profiles
    users --> technicians
    users -.-> admins
    users -.-> supervisors
    
    style users fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style roles fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style refresh_tokens fill:#FF9800,stroke:#E65100,stroke-width:2px,color:#fff
```

## 3. School Management Hierarchy

```mermaid
graph TD
    schools[Schools] --> |has many| supervisors[Supervisor Assignments]
    schools --> |generates| reports[Reports/Work Orders]
    schools --> |tracks| maintenance[Maintenance Counts]
    schools --> |assesses| damages[Damage Assessments]
    schools --> |records| attendance[Supervisor Attendance]
    schools --> |documents| achievements[School Achievements]
    
    supervisors --> |managed by| users[Users/Supervisors]
    reports --> |assigned to| users
    maintenance --> |performed by| users
    damages --> |assessed by| users
    attendance --> |tracked for| users
    
    reports --> |has| attachments[File Attachments]
    reports --> |has| comments[Comments]
    maintenance --> |contains| items[Maintenance Items]
    damages --> |itemizes| damage_items[Damage Items]
    
    style schools fill:#673AB7,stroke:#4527A0,stroke-width:3px,color:#fff
    style reports fill:#F44336,stroke:#C62828,stroke-width:2px,color:#fff
    style users fill:#4CAF50,stroke:#2E7D32,stroke-width:2px,color:#fff
```

## 4. Report Lifecycle Flow Diagram

```mermaid
flowchart LR
    subgraph "Creation Phase"
        A[fa:fa-plus Create Report] --> B[DRAFT Status]
        B --> C{Validate}
        C -->|Valid| D[SUBMITTED]
        C -->|Invalid| B
    end
    
    subgraph "Review Phase"
        D --> E[IN_REVIEW]
        E --> F{Decision}
        F -->|Approve| G[APPROVED]
        F -->|Reject| H[REJECTED]
        F -->|Need Info| D
        H --> B
    end
    
    subgraph "Execution Phase"
        G --> I[IN_PROGRESS]
        I --> J{Work Status}
        J -->|Complete| K[COMPLETED]
        J -->|Hold| L[PENDING]
        J -->|Cancel| M[CANCELLED]
        L --> I
    end
    
    subgraph "Archive Phase"
        K --> N[Calculate Metrics]
        N --> O[Update Scores]
        O --> P[Archive]
        M --> Q[Soft Delete]
        Q --> R[Purge After 90 Days]
    end
    
    style A fill:#4CAF50
    style K fill:#8BC34A
    style M fill:#F44336
    style P fill:#9E9E9E
```

## 5. Data Partitioning Strategy

```mermaid
graph TB
    subgraph "Reports Partitioning - Monthly"
        reports_master[Reports Master Table]
        reports_master --> jan24[January 2024<br/>10,000 records]
        reports_master --> feb24[February 2024<br/>12,000 records]
        reports_master --> mar24[March 2024<br/>11,500 records]
        reports_master --> apr24[April 2024<br/>13,000 records]
        reports_master --> may24[May 2024<br/>Current Month]
        reports_master --> future[Future Partition<br/>Default]
    end
    
    subgraph "Notifications - Weekly"
        notif_master[Notifications Master]
        notif_master --> week1[Week 1<br/>5,000 records]
        notif_master --> week2[Week 2<br/>4,500 records]
        notif_master --> week3[Week 3<br/>5,200 records]
        notif_master --> week4[Week 4<br/>Current Week]
        notif_master --> weekdef[Default<br/>Future]
    end
    
    style reports_master fill:#2196F3,stroke:#0D47A1,stroke-width:3px
    style notif_master fill:#FF9800,stroke:#E65100,stroke-width:3px
    style may24 fill:#4CAF50,stroke:#1B5E20,stroke-width:2px
    style week4 fill:#4CAF50,stroke:#1B5E20,stroke-width:2px
```

## 6. Audit System Architecture

```mermaid
flowchart TB
    subgraph "Database Operations"
        INSERT[INSERT Operation]
        UPDATE[UPDATE Operation]
        DELETE[DELETE Operation]
        SOFT_DEL[SOFT DELETE]
    end
    
    subgraph "Audit Triggers"
        BEFORE_TRIG[Before Trigger<br/>Validation]
        AFTER_TRIG[After Trigger<br/>Logging]
    end
    
    subgraph "Audit Storage"
        AUDIT_LOG[(Audit Log<br/>All Operations)]
        HISTORY[(History Tables<br/>Version Control)]
        COMPLIANCE[(Compliance Reports)]
    end
    
    subgraph "Audit Views"
        DAILY[Daily Summary]
        USER_ACT[User Activity]
        SENSITIVE[Sensitive Ops]
    end
    
    INSERT --> BEFORE_TRIG
    UPDATE --> BEFORE_TRIG
    DELETE --> BEFORE_TRIG
    SOFT_DEL --> BEFORE_TRIG
    
    BEFORE_TRIG --> AFTER_TRIG
    
    AFTER_TRIG --> AUDIT_LOG
    AFTER_TRIG --> HISTORY
    
    AUDIT_LOG --> DAILY
    AUDIT_LOG --> USER_ACT
    AUDIT_LOG --> SENSITIVE
    
    AUDIT_LOG --> COMPLIANCE
    
    style AUDIT_LOG fill:#9C27B0,stroke:#4A148C,stroke-width:3px,color:#fff
    style HISTORY fill:#673AB7,stroke:#311B92,stroke-width:2px,color:#fff
```

## 7. Performance Optimization Layers

```mermaid
graph TD
    subgraph "Level 1: Application Cache"
        APP_CACHE[Application Cache<br/>5 min TTL]
    end
    
    subgraph "Level 2: Redis Cache"
        REDIS[Redis Cache<br/>15 min TTL]
    end
    
    subgraph "Level 3: Smart Cache"
        SMART[Smart Cache Table<br/>60 min TTL<br/>Hit/Miss Tracking]
    end
    
    subgraph "Level 4: Materialized Views"
        MV_DASH[Dashboard Stats<br/>15 min refresh]
        MV_SCHOOL[School Performance<br/>30 min refresh]
        MV_WORK[Supervisor Workload<br/>15 min refresh]
    end
    
    subgraph "Level 5: Indexes"
        BTREE[B-Tree Indexes<br/>Primary/Foreign Keys]
        GIN[GIN Indexes<br/>JSONB/Full-Text]
        PARTIAL[Partial Indexes<br/>Exclude Deleted]
        TRGM[Trigram Indexes<br/>Fuzzy Search]
    end
    
    subgraph "Level 6: Partitioned Storage"
        PARTS[Partitioned Tables<br/>Monthly/Weekly]
    end
    
    REQUEST[User Request] --> APP_CACHE
    APP_CACHE -->|miss| REDIS
    REDIS -->|miss| SMART
    SMART -->|miss| MV_DASH
    MV_DASH --> BTREE
    MV_SCHOOL --> GIN
    MV_WORK --> PARTIAL
    BTREE --> PARTS
    GIN --> PARTS
    PARTIAL --> PARTS
    TRGM --> PARTS
    
    style REQUEST fill:#FFC107,stroke:#F57C00
    style APP_CACHE fill:#8BC34A,stroke:#33691E
    style REDIS fill:#4CAF50,stroke:#1B5E20
    style SMART fill:#00BCD4,stroke:#006064
    style PARTS fill:#3F51B5,stroke:#1A237E,color:#fff
```

## 8. Soft Delete and Recovery Flow

```mermaid
stateDiagram-v2
    [*] --> Active: Record Created
    
    Active --> SoftDeleted: Soft Delete
    note right of SoftDeleted
        - deleted_at = timestamp
        - deleted_by = user_id
        - Excluded from queries
    end note
    
    SoftDeleted --> RecycleBin: Within 30 days
    note right of RecycleBin
        - Visible in recycle bin
        - Can be restored
        - Shows deletion reason
    end note
    
    RecycleBin --> Active: Restore
    RecycleBin --> Archived: After 30 days
    
    Archived --> Purged: After 90 days
    note right of Purged
        - Permanently deleted
        - Cannot be recovered
        - Logged in audit
    end note
    
    Purged --> [*]: Destroyed
    
    SoftDeleted --> CascadeDelete: Related Records
    note left of CascadeDelete
        - Attachments deleted
        - Comments deleted
        - History preserved
    end note
```

## 9. Search System Architecture

```mermaid
flowchart TB
    subgraph "Search Input Layer"
        SEARCH[Search Query]
        FILTERS[Filters & Criteria]
        LANG[Language Detection]
    end
    
    subgraph "Processing Layer"
        FTS[Full-Text Search<br/>English/Arabic]
        TRGM[Trigram Matching<br/>Fuzzy Search]
        WEIGHT[Weighted Scoring]
    end
    
    subgraph "Index Layer"
        TSV[TSVector Indexes]
        GIN_IDX[GIN Indexes]
        TRGM_IDX[Trigram Indexes]
    end
    
    subgraph "Data Sources"
        REPORTS_TBL[(Reports)]
        SCHOOLS_TBL[(Schools)]
        USERS_TBL[(Users)]
        MAINT_TBL[(Maintenance)]
    end
    
    subgraph "Results"
        RANK[Relevance Ranking]
        HIGHLIGHT[Term Highlighting]
        SUGGEST[Suggestions]
    end
    
    SEARCH --> LANG
    FILTERS --> FTS
    LANG --> FTS
    LANG --> TRGM
    
    FTS --> TSV
    TRGM --> TRGM_IDX
    
    TSV --> REPORTS_TBL
    TSV --> SCHOOLS_TBL
    GIN_IDX --> USERS_TBL
    TRGM_IDX --> MAINT_TBL
    
    REPORTS_TBL --> WEIGHT
    SCHOOLS_TBL --> WEIGHT
    USERS_TBL --> WEIGHT
    MAINT_TBL --> WEIGHT
    
    WEIGHT --> RANK
    RANK --> HIGHLIGHT
    RANK --> SUGGEST
    
    style SEARCH fill:#2196F3,stroke:#0D47A1,stroke-width:2px,color:#fff
    style RANK fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
```

## 10. Analytics and Reporting Pipeline

```mermaid
graph LR
    subgraph "Data Collection"
        TRANS[Transactional Data]
        EVENTS[Event Stream]
        LOGS[System Logs]
    end
    
    subgraph "Processing"
        AGG[Aggregation<br/>Functions]
        CALC[Calculations<br/>& Scoring]
        ML[Predictive<br/>Analytics]
    end
    
    subgraph "Storage"
        SUMMARY[(Analytics<br/>Summary)]
        TIME[(Time Series<br/>Data)]
        CACHE[(Smart<br/>Cache)]
    end
    
    subgraph "Visualization"
        DASH[Dashboards]
        REPORTS[Reports]
        ALERTS[Alerts]
        API[API Export]
    end
    
    TRANS --> AGG
    EVENTS --> AGG
    LOGS --> CALC
    
    AGG --> SUMMARY
    CALC --> TIME
    ML --> CACHE
    
    SUMMARY --> DASH
    TIME --> REPORTS
    CACHE --> ALERTS
    SUMMARY --> API
    
    style TRANS fill:#FF5722,stroke:#BF360C
    style AGG fill:#9C27B0,stroke:#4A148C
    style SUMMARY fill:#3F51B5,stroke:#1A237E
    style DASH fill:#4CAF50,stroke:#1B5E20
```

## 11. Database Security Layers

```mermaid
graph TB
    subgraph "Access Control"
        AUTH[Authentication<br/>JWT Tokens]
        AUTHZ[Authorization<br/>Role-Based]
        RLS[Row Level Security<br/>Policies]
    end
    
    subgraph "Data Protection"
        ENCRYPT[Encryption<br/>BCrypt Passwords]
        HASH[Hashing<br/>Token Storage]
        MASK[Data Masking<br/>Sensitive Fields]
    end
    
    subgraph "Audit & Compliance"
        AUDIT[Audit Logging<br/>All Operations]
        HISTORY[Version History<br/>Change Tracking]
        COMPLY[Compliance<br/>Reports]
    end
    
    subgraph "Threat Prevention"
        VALIDATE[Input Validation<br/>Domain Types]
        SANITIZE[SQL Injection<br/>Prevention]
        RATE[Rate Limiting<br/>API Throttling]
    end
    
    USER[User Request] --> AUTH
    AUTH --> AUTHZ
    AUTHZ --> RLS
    RLS --> ENCRYPT
    ENCRYPT --> VALIDATE
    VALIDATE --> SANITIZE
    SANITIZE --> DATABASE[(Database)]
    DATABASE --> AUDIT
    AUDIT --> HISTORY
    HISTORY --> COMPLY
    
    style AUTH fill:#F44336,stroke:#B71C1C,stroke-width:3px,color:#fff
    style DATABASE fill:#4CAF50,stroke:#1B5E20,stroke-width:3px,color:#fff
    style AUDIT fill:#2196F3,stroke:#0D47A1,stroke-width:2px,color:#fff
```

## 12. Complete System Integration View

```mermaid
graph TB
    subgraph "Client Applications"
        MOBILE[Mobile App<br/>Supervisors]
        WEB[Web Panel<br/>Admins]
        API_CLIENT[API Clients]
    end
    
    subgraph "API Gateway"
        REST[REST API]
        WS[WebSocket]
        GRAPHQL[GraphQL]
    end
    
    subgraph "Application Layer"
        AUTH_SVC[Auth Service]
        REPORT_SVC[Report Service]
        SCHOOL_SVC[School Service]
        NOTIF_SVC[Notification Service]
    end
    
    subgraph "Data Access Layer"
        REPOS[Repositories]
        CACHE_MGR[Cache Manager]
        SEARCH_MGR[Search Engine]
    end
    
    subgraph "Database Core"
        PG[(PostgreSQL<br/>53 Tables)]
        REDIS[(Redis Cache)]
        MINIO[(MinIO Storage)]
    end
    
    subgraph "Background Jobs"
        SCHEDULER[Scheduler]
        WORKERS[Workers]
        QUEUE[Job Queue]
    end
    
    MOBILE --> REST
    WEB --> REST
    API_CLIENT --> GRAPHQL
    
    REST --> AUTH_SVC
    WS --> NOTIF_SVC
    GRAPHQL --> SCHOOL_SVC
    
    AUTH_SVC --> REPOS
    REPORT_SVC --> CACHE_MGR
    SCHOOL_SVC --> SEARCH_MGR
    NOTIF_SVC --> REPOS
    
    REPOS --> PG
    CACHE_MGR --> REDIS
    SEARCH_MGR --> PG
    
    PG --> SCHEDULER
    SCHEDULER --> WORKERS
    WORKERS --> QUEUE
    QUEUE --> PG
    
    style PG fill:#336791,stroke:#1e4668,stroke-width:4px,color:#fff
    style REDIS fill:#DC382D,stroke:#A41E11,stroke-width:3px,color:#fff
    style REST fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
```

## Database Statistics Summary

| Category | Count | Description |
|----------|-------|-------------|
| **Core Tables** | 38 | Business entity tables |
| **System Tables** | 15 | Audit, cache, analytics |
| **Views** | 15+ | Active records, summaries |
| **Materialized Views** | 6 | Performance optimization |
| **Functions** | 50+ | Business logic, utilities |
| **Triggers** | 30+ | Automation, validation |
| **Indexes** | 100+ | Performance optimization |
| **Constraints** | 150+ | Data integrity |
| **Partitions** | 24+ | Monthly/weekly partitions |

## Color Legend

- 🟢 **Green**: Active/Operational Components
- 🔵 **Blue**: Data Storage/Processing
- 🟣 **Purple**: Audit/Monitoring Systems
- 🟠 **Orange**: Cache/Performance Layer
- 🔴 **Red**: Security/Critical Components
- ⚫ **Gray**: Archived/Deleted Data