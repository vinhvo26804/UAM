-- Missing legacy status must not enable an account implicitly.
UPDATE users SET status = 'PENDING' WHERE status IS NULL OR BTRIM(status) = '';

-- Keep the existing VARCHAR column; Java maps enum names with EnumType.STRING.
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_status_check
    CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED', 'DISABLED'));
