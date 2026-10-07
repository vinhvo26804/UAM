-- Migration V7: Create spaces, sprints, issues, idempotency and audit tables

-- 1. SPACES
CREATE TABLE spaces (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    next_issue_number BIGINT NOT NULL DEFAULT 1 CHECK (next_issue_number > 0),
    work_revision BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_spaces_created_by ON spaces(created_by);

-- 2. SPACE_MEMBERS
CREATE TABLE space_members (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_space_member UNIQUE (space_id, user_id),
    CONSTRAINT chk_space_member_role CHECK (role IN ('ADMIN', 'MANAGER', 'MEMBER', 'VIEWER'))
);

CREATE INDEX idx_space_members_user_space ON space_members(user_id, space_id);
CREATE INDEX idx_space_members_space_id ON space_members(space_id);

-- 3. SPACE_ISSUE_TYPES
CREATE TABLE space_issue_types (
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    issue_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (space_id, issue_type),
    CONSTRAINT chk_space_issue_type CHECK (issue_type IN ('EPIC', 'STORY', 'TASK', 'BUG', 'SUBTASK'))
);

-- 4. SPRINTS
CREATE TABLE sprints (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    goal VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'FUTURE',
    position BIGINT NOT NULL CHECK (position > 0),
    planned_start_at TIMESTAMP,
    planned_end_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sprint_space_position UNIQUE (space_id, position),
    CONSTRAINT chk_sprint_status CHECK (status IN ('FUTURE', 'ACTIVE', 'CLOSED')),
    CONSTRAINT chk_sprint_planned_dates CHECK (
        (planned_start_at IS NULL AND planned_end_at IS NULL) OR
        (planned_start_at IS NOT NULL AND planned_end_at IS NOT NULL AND planned_start_at < planned_end_at)
    ),
    CONSTRAINT chk_sprint_lifecycle_dates CHECK (
        (status = 'FUTURE' AND started_at IS NULL AND completed_at IS NULL) OR
        (status = 'ACTIVE' AND started_at IS NOT NULL AND completed_at IS NULL) OR
        (status = 'CLOSED' AND started_at IS NOT NULL AND completed_at IS NOT NULL AND completed_at >= started_at)
    )
);

CREATE INDEX idx_sprints_space_status_pos ON sprints(space_id, status, position);
CREATE UNIQUE INDEX uk_sprints_single_active ON sprints(space_id) WHERE status = 'ACTIVE';

-- 5. ISSUES
CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    issue_number BIGINT NOT NULL CHECK (issue_number > 0),
    issue_key VARCHAR(32) NOT NULL UNIQUE,
    issue_type VARCHAR(20) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assignee_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    epic_id BIGINT REFERENCES issues(id) ON DELETE RESTRICT,
    parent_issue_id BIGINT REFERENCES issues(id) ON DELETE RESTRICT,
    sprint_id BIGINT REFERENCES sprints(id) ON DELETE RESTRICT,
    estimate_hours NUMERIC(8,2) CHECK (estimate_hours IS NULL OR (estimate_hours >= 0 AND estimate_hours <= 999999.99)),
    due_date DATE,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uk_issues_space_number UNIQUE (space_id, issue_number),
    CONSTRAINT chk_issues_type CHECK (issue_type IN ('EPIC', 'STORY', 'TASK', 'BUG', 'SUBTASK')),
    CONSTRAINT chk_issues_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT chk_issues_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_issues_summary CHECK (LENGTH(TRIM(summary)) > 0),
    CONSTRAINT chk_issues_deletion CHECK (
        (deleted_at IS NULL AND deleted_by IS NULL) OR
        (deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    ),
    CONSTRAINT chk_issues_hierarchy_rules CHECK (
        -- EPIC cannot have parent, epic_id, sprint_id, or estimate_hours
        (issue_type = 'EPIC' AND parent_issue_id IS NULL AND epic_id IS NULL AND sprint_id IS NULL AND estimate_hours IS NULL)
        OR
        -- STORY, TASK, BUG cannot have parent_issue_id (only Subtask has parent_issue_id)
        (issue_type IN ('STORY', 'TASK', 'BUG') AND parent_issue_id IS NULL)
        OR
        -- SUBTASK must have parent_issue_id and cannot have direct epic_id
        (issue_type = 'SUBTASK' AND parent_issue_id IS NOT NULL AND epic_id IS NULL)
    )
);

CREATE INDEX idx_issues_space_list ON issues(space_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_space_status ON issues(space_id, status, id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_space_assignee ON issues(space_id, assignee_id, id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_space_epic ON issues(space_id, epic_id, id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_space_parent ON issues(space_id, parent_issue_id, id) WHERE deleted_at IS NULL;
CREATE INDEX idx_issues_space_sprint ON issues(space_id, sprint_id, id) WHERE deleted_at IS NULL;

-- 6. ISSUE_LABELS
CREATE TABLE issue_labels (
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    label VARCHAR(50) NOT NULL,
    PRIMARY KEY (issue_id, label)
);

-- 7. ISSUE_SPRINT_HISTORY
CREATE TABLE issue_sprint_history (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    sprint_id BIGINT NOT NULL REFERENCES sprints(id) ON DELETE RESTRICT,
    entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    entered_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    left_by BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    enter_reason VARCHAR(40) NOT NULL,
    leave_reason VARCHAR(40),
    CONSTRAINT chk_sprint_history_dates CHECK (left_at IS NULL OR left_at >= entered_at)
);

CREATE INDEX idx_sprint_history_issue ON issue_sprint_history(issue_id, entered_at DESC, id DESC);
CREATE UNIQUE INDEX uk_sprint_history_open_interval ON issue_sprint_history(issue_id) WHERE left_at IS NULL;

-- 8. ISSUE_AUDIT_EVENTS
CREATE TABLE issue_audit_events (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    actor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(40) NOT NULL,
    changes TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issue_audit_events ON issue_audit_events(issue_id, created_at DESC, id DESC);

-- 9. IDEMPOTENCY_RECORDS
CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    operation VARCHAR(32) NOT NULL,
    request_key UUID NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    resource_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT uk_idempotency_actor_op_key UNIQUE (actor_id, operation, request_key)
);

CREATE INDEX idx_idempotency_lookup ON idempotency_records(actor_id, operation, request_key);
