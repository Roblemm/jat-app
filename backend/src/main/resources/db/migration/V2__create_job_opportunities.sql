-- Job opportunities are stored separately from tasks because they represent external leads,
-- while tasks represent work the user plans to do inside Jat.
CREATE TABLE job_opportunities (
    id UUID PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    company VARCHAR(160) NOT NULL,
    location VARCHAR(160),
    source_url VARCHAR(600),
    source_name VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- Status and created_at are the main inbox filters for reviewing newly saved opportunities.
CREATE INDEX idx_job_opportunities_status ON job_opportunities(status);
CREATE INDEX idx_job_opportunities_created_at ON job_opportunities(created_at);
