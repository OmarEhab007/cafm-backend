# CAFM Database Visual Diagrams

## Complete Entity Relationship Diagram

```mermaid
erDiagram
    %% ===== USER MANAGEMENT DOMAIN =====
    users ||--o{ user_roles : "has roles"
    roles ||--o{ user_roles : "assigned to"
    users ||--o{ profiles : "has profile"
    users ||--o{ refresh_tokens : "authenticates with"
    users ||--o{ password_reset_tokens : "requests reset"
    users ||--o{ user_fcm_tokens : "receives notifications"
    users ||--o{ technicians : "supervises"
    users ||--o{ notifications : "receives"
    users ||--o{ notification_queue : "queued for"
    
    %% ===== SCHOOL DOMAIN =====
    schools ||--o{ supervisor_schools : "has supervisors"
    users ||--o{ supervisor_schools : "assigned to schools"
    schools ||--o{ supervisor_attendance : "tracks attendance"
    schools ||--o{ school_achievements : "has achievements"
    
    %% ===== REPORTS/WORK ORDERS DOMAIN =====
    schools ||--o{ reports : "generates"
    users ||--o{ reports : "creates/assigned"
    reports ||--o{ report_attachments : "has attachments"
    reports ||--o{ report_comments : "has comments"
    reports ||--o{ achievement_photos : "documented in"
    reports ||--o{ report_data_items : "contains data"
    
    %% ===== MAINTENANCE DOMAIN =====
    schools ||--o{ maintenance_counts : "has counts"
    users ||--o{ maintenance_counts : "performs"
    maintenance_counts ||--o{ maintenance_items : "contains items"
    maintenance_counts ||--o{ survey_responses : "has responses"
    maintenance_categories ||--o{ maintenance_item_templates : "categorizes"
    maintenance_item_templates ||--o{ maintenance_items : "based on"
    
    schools ||--o{ maintenance_reports : "has reports"
    users ||--o{ maintenance_reports : "creates"
    maintenance_reports ||--o{ report_data_items : "contains data"
    
    %% ===== DAMAGE ASSESSMENT DOMAIN =====
    schools ||--o{ damage_counts : "assessed for"
    users ||--o{ damage_counts : "assesses"
    damage_counts ||--o{ damage_items : "itemizes"
    damage_counts ||--o{ damage_count_photos : "documents"
    
    %% ===== FILE MANAGEMENT DOMAIN =====
    attachments ||--o{ reports : "attached to"
    attachments ||--o{ maintenance_reports : "attached to"
    attachments ||--o{ school_achievements : "attached to"
    users ||--o{ attachments : "uploads"
    users ||--o{ file_uploads : "uploads"
    
    %% ===== ADMIN LEGACY DOMAIN =====
    admins ||--o{ supervisors : "manages"
    supervisors ||--o{ users : "legacy link"
```

## System Architecture Layers

```mermaid
graph TB
    subgraph "Presentation Layer"
        API[REST API]
        WS[WebSocket]
        SWAGGER[Swagger UI]
    end
    
    subgraph "Application Layer"
        AUTH[Authentication]
        AUTHZ[Authorization]
        VALIDATION[Validation]
        EVENTS[Event System]
    end
    
    subgraph "Business Logic Layer"
        REPORTS_SVC[Reports Service]
        SCHOOLS_SVC[Schools Service]
        MAINT_SVC[Maintenance Service]
        NOTIF_SVC[Notification Service]
        ANALYTICS[Analytics Engine]
    end
    
    subgraph "Data Access Layer"
        REPOS[Repositories]
        CACHE[Cache Layer]
        SEARCH[Search Engine]
    end
    
    subgraph "Database Layer"
        subgraph "Core Tables"
            USERS_DB[(Users)]
            SCHOOLS_DB[(Schools)]
            REPORTS_DB[(Reports)]
        end
        
        subgraph "System Tables"
            AUDIT[(Audit Log)]
            HISTORY[(History Tables)]
            CACHE_DB[(Smart Cache)]
        end
        
        subgraph "Optimizations"
            PARTITIONS[Table Partitions]
            MATVIEWS[Materialized Views]
            INDEXES[Indexes]
        end
    end
    
    API --> AUTH
    WS --> EVENTS
    AUTH --> AUTHZ
    AUTHZ --> VALIDATION
    VALIDATION --> REPORTS_SVC
    VALIDATION --> SCHOOLS_SVC
    VALIDATION --> MAINT_SVC
    EVENTS --> NOTIF_SVC
    
    REPORTS_SVC --> REPOS
    SCHOOLS_SVC --> REPOS
    MAINT_SVC --> REPOS
    NOTIF_SVC --> CACHE
    ANALYTICS --> SEARCH
    
    REPOS --> USERS_DB
    REPOS --> SCHOOLS_DB
    REPOS --> REPORTS_DB
    CACHE --> CACHE_DB
    SEARCH --> MATVIEWS
    
    USERS_DB --> AUDIT
    SCHOOLS_DB --> AUDIT
    REPORTS_DB --> AUDIT
    AUDIT --> HISTORY
```

## Data Flow Diagram

```mermaid
flowchart TD
    subgraph "User Actions"
        CREATE[Create Report]
        UPDATE[Update Status]
        DELETE[Soft Delete]
        SEARCH[Search]
    end
    
    subgraph "Triggers & Functions"
        VALIDATE[Validation Trigger]
        AUDIT_TRIG[Audit Trigger]
        HISTORY_TRIG[History Trigger]
        COMPUTE[Computed Fields]
        CASCADE[Cascade Delete]
    end
    
    subgraph "Data Storage"
        MAIN[(Main Table)]
        AUDIT_LOG[(Audit Log)]
        HISTORY[(History Table)]
        CACHE[(Cache)]
        SEARCH_IDX[(Search Index)]
    end
    
    subgraph "Background Jobs"
        PARTITION[Create Partitions]
        REFRESH[Refresh Views]
        CLEANUP[Cleanup Old Data]
        NOTIFY[Send Notifications]
    end
    
    CREATE --> VALIDATE
    UPDATE --> VALIDATE
    DELETE --> CASCADE
    SEARCH --> SEARCH_IDX
    
    VALIDATE --> COMPUTE
    COMPUTE --> MAIN
    MAIN --> AUDIT_TRIG
    MAIN --> HISTORY_TRIG
    CASCADE --> AUDIT_TRIG
    
    AUDIT_TRIG --> AUDIT_LOG
    HISTORY_TRIG --> HISTORY
    
    MAIN --> CACHE
    CACHE --> REFRESH
    
    PARTITION --> MAIN
    CLEANUP --> AUDIT_LOG
    CLEANUP --> HISTORY
    
    MAIN --> NOTIFY
```

## Report Lifecycle State Diagram

```mermaid
stateDiagram-v2
    [*] --> DRAFT: Create
    DRAFT --> SUBMITTED: Submit
    DRAFT --> CANCELLED: Cancel
    
    SUBMITTED --> IN_REVIEW: Review Start
    SUBMITTED --> REJECTED: Direct Reject
    SUBMITTED --> CANCELLED: Cancel
    
    IN_REVIEW --> APPROVED: Approve
    IN_REVIEW --> REJECTED: Reject
    IN_REVIEW --> SUBMITTED: Send Back
    
    APPROVED --> IN_PROGRESS: Start Work
    APPROVED --> CANCELLED: Cancel
    
    IN_PROGRESS --> COMPLETED: Complete
    IN_PROGRESS --> CANCELLED: Cancel
    IN_PROGRESS --> pending: Hold
    
    pending --> IN_PROGRESS: Resume
    
    REJECTED --> DRAFT: Revise
    REJECTED --> CANCELLED: Cancel
    
    COMPLETED --> [*]: Archive
    CANCELLED --> [*]: Archive
    
    note right of COMPLETED
        Triggers:
        - Update efficiency_score
        - Calculate resolution_time
        - Update user performance
        - Refresh analytics
    end note
    
    note left of CANCELLED
        Soft Delete after 30 days
        Purge after 90 days
    end note
```

## Partitioning Strategy Visualization

```mermaid
graph TB
    subgraph "Reports Table Partitioning"
        REPORTS[Reports Master]
        REPORTS --> R202401[2024-01]
        REPORTS --> R202402[2024-02]
        REPORTS --> R202403[2024-03]
        REPORTS --> R202404[2024-04]
        REPORTS --> R202405[2024-05]
        REPORTS --> R202406[2024-06]
        REPORTS --> RDEFAULT[Default Future]
        
        style R202401 fill:#e1f5fe
        style R202402 fill:#e1f5fe
        style R202403 fill:#e1f5fe
        style R202404 fill:#b3e5fc
        style R202405 fill:#b3e5fc
        style R202406 fill:#81d4fa
        style RDEFAULT fill:#4fc3f7
    end
    
    subgraph "Notifications Partitioning"
        NOTIF[Notifications Master]
        NOTIF --> N202501[Week 1]
        NOTIF --> N202502[Week 2]
        NOTIF --> N202503[Week 3]
        NOTIF --> N202504[Week 4]
        NOTIF --> NDEFAULT[Default]
        
        style N202501 fill:#fff3e0
        style N202502 fill:#ffe0b2
        style N202503 fill:#ffcc80
        style N202504 fill:#ffb74d
        style NDEFAULT fill:#ffa726
    end
```

## Audit System Flow

```mermaid
sequenceDiagram
    participant User
    participant Application
    participant Database
    participant AuditTrigger
    participant AuditLog
    participant History
    
    User->>Application: Update Report
    Application->>Database: UPDATE reports SET ...
    Database->>AuditTrigger: Fire BEFORE UPDATE
    AuditTrigger->>AuditTrigger: Validate Changes
    Database->>Database: Apply Update
    Database->>AuditTrigger: Fire AFTER UPDATE
    AuditTrigger->>AuditLog: Log Operation
    AuditTrigger->>History: Create Version
    Database-->>Application: Success
    Application-->>User: Update Complete
    
    Note over AuditLog: Stores: user, timestamp,<br/>old values, new values,<br/>changed fields
    Note over History: Stores: full record<br/>snapshot with<br/>version number
```

## Search System Architecture

```mermaid
graph LR
    subgraph "Search Input"
        QUERY[Search Query]
        FILTERS[Filters]
    end
    
    subgraph "Search Processing"
        FTS[Full-Text Search]
        TRGM[Trigram Matching]
        FUZZY[Fuzzy Search]
    end
    
    subgraph "Search Indexes"
        GIN[GIN Index]
        GIST[GiST Index]
        TSVECTOR[TS Vector]
    end
    
    subgraph "Results"
        REPORTS_R[Reports]
        SCHOOLS_R[Schools]
        USERS_R[Users]
        RANK[Relevance Ranking]
    end
    
    QUERY --> FTS
    QUERY --> TRGM
    FILTERS --> FUZZY
    
    FTS --> TSVECTOR
    TRGM --> GIN
    FUZZY --> GIST
    
    TSVECTOR --> REPORTS_R
    GIN --> SCHOOLS_R
    GIST --> USERS_R
    
    REPORTS_R --> RANK
    SCHOOLS_R --> RANK
    USERS_R --> RANK
```

## Performance Optimization Layers

```mermaid
graph TD
    subgraph "Query Layer"
        QUERY[SQL Query]
    end
    
    subgraph "Cache Layer"
        SMART_CACHE[Smart Cache<br/>TTL: 60min]
        REDIS[Redis Cache<br/>TTL: 15min]
        SESSION[Session Cache<br/>TTL: 24hr]
    end
    
    subgraph "View Layer"
        MATVIEW[Materialized Views<br/>Refresh: 15min]
        ACTIVE_VIEW[Active Record Views<br/>Real-time]
    end
    
    subgraph "Index Layer"
        BTREE[B-Tree Indexes]
        GIN_IDX[GIN Indexes]
        PARTIAL[Partial Indexes]
    end
    
    subgraph "Storage Layer"
        PARTITIONED[Partitioned Tables]
        COMPRESSED[Compressed JSONB]
        ARCHIVED[Archived Data]
    end
    
    QUERY --> SMART_CACHE
    SMART_CACHE -->|miss| REDIS
    REDIS -->|miss| SESSION
    SESSION -->|miss| MATVIEW
    MATVIEW -->|stale| ACTIVE_VIEW
    ACTIVE_VIEW --> BTREE
    ACTIVE_VIEW --> GIN_IDX
    ACTIVE_VIEW --> PARTIAL
    PARTIAL --> PARTITIONED
    PARTITIONED --> COMPRESSED
    COMPRESSED --> ARCHIVED
    
    style SMART_CACHE fill:#4caf50
    style REDIS fill:#66bb6a
    style SESSION fill:#81c784
    style MATVIEW fill:#2196f3
    style ACTIVE_VIEW fill:#42a5f5
```

## Data Integrity Enforcement

```mermaid
flowchart TB
    subgraph "Input Validation"
        DOMAIN[Domain Types]
        CHECK[Check Constraints]
        REGEX[Regex Patterns]
    end
    
    subgraph "Referential Integrity"
        FK[Foreign Keys]
        CASCADE_UPD[Cascade Update]
        CASCADE_DEL[Cascade Delete]
        RESTRICT[Restrict Delete]
    end
    
    subgraph "Business Rules"
        TRIGGER_VAL[Validation Triggers]
        EXCLUSION[Exclusion Constraints]
        UNIQUE[Unique Constraints]
    end
    
    subgraph "Data Quality"
        RULES[Quality Rules]
        VIOLATIONS[Violation Log]
        MONITORING[Quality Monitoring]
    end
    
    INPUT[User Input] --> DOMAIN
    DOMAIN --> CHECK
    CHECK --> REGEX
    REGEX --> FK
    FK --> CASCADE_UPD
    FK --> CASCADE_DEL
    FK --> RESTRICT
    CASCADE_UPD --> TRIGGER_VAL
    CASCADE_DEL --> TRIGGER_VAL
    RESTRICT --> TRIGGER_VAL
    TRIGGER_VAL --> EXCLUSION
    EXCLUSION --> UNIQUE
    UNIQUE --> RULES
    RULES --> VIOLATIONS
    VIOLATIONS --> MONITORING
    MONITORING --> ALERT[Alert System]
```

## Analytics Pipeline

```mermaid
graph LR
    subgraph "Data Sources"
        REPORTS_SRC[(Reports)]
        SCHOOLS_SRC[(Schools)]
        USERS_SRC[(Users)]
        MAINT_SRC[(Maintenance)]
    end
    
    subgraph "Processing"
        AGG[Aggregation]
        CALC[Calculations]
        PREDICT[Predictions]
        SCORE[Scoring]
    end
    
    subgraph "Storage"
        ANALYTICS[(Analytics Summary)]
        TIMESERIES[(Time Series)]
        METRICS[(Metrics)]
    end
    
    subgraph "Presentation"
        DASHBOARD[Dashboards]
        REPORTS_OUT[Reports]
        API_OUT[API]
    end
    
    REPORTS_SRC --> AGG
    SCHOOLS_SRC --> AGG
    USERS_SRC --> CALC
    MAINT_SRC --> PREDICT
    
    AGG --> ANALYTICS
    CALC --> SCORE
    PREDICT --> TIMESERIES
    SCORE --> METRICS
    
    ANALYTICS --> DASHBOARD
    TIMESERIES --> REPORTS_OUT
    METRICS --> API_OUT
```

## Summary Statistics

### Total Database Objects
- **Core Tables**: 38
- **System Tables**: 15
- **Materialized Views**: 6
- **Regular Views**: 15+
- **Functions**: 50+
- **Triggers**: 30+
- **Indexes**: 100+
- **Constraints**: 150+

### Key Metrics
- **Partitions**: Monthly (Reports), Weekly (Notifications)
- **Audit Retention**: 90 days (then archived)
- **Soft Delete Retention**: 30 days (recycle bin), 90 days (purge)
- **Cache TTL**: 15-60 minutes depending on data type
- **View Refresh**: 15 minutes for materialized views
- **Search Languages**: English, Arabic
- **Performance Target**: <100ms for indexed queries