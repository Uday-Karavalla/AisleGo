-- Every account created before this migration (including the seeded admin, and every real
-- account already registered on the live deployment) was created without any verification
-- step - defaulting the new column to TRUE first grandfathers all of them in as already
-- verified, so this migration can never lock an existing user out. Only accounts registered
-- from this point on start out unverified.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT FALSE;

ALTER TABLE users ADD COLUMN verification_code VARCHAR(10);
ALTER TABLE users ADD COLUMN verification_code_expires_at TIMESTAMPTZ;
