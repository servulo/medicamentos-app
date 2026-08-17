-- ADMIN_EMAIL is environment-specific and is inserted by AdminEmailConfig at startup.
CREATE INDEX idx_whitelist_created_at ON whitelist_entries (created_at);
