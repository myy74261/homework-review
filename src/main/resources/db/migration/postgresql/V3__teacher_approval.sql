ALTER TABLE accounts
    ADD COLUMN approval_status varchar(20),
    ADD COLUMN review_note varchar(300),
    ADD COLUMN reviewed_at timestamp(6) with time zone,
    ADD COLUMN reviewed_by_account_id bigint;

UPDATE accounts
SET approval_status = 'APPROVED'
WHERE approval_status IS NULL;

ALTER TABLE accounts
    ALTER COLUMN approval_status SET NOT NULL;

CREATE INDEX idx_accounts_role_approval
    ON accounts (role, approval_status, created_at);
