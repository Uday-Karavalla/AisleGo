-- V3 seeded a demo admin account (admin@aislego.com) with a password that was published in
-- plaintext in README.md for local-demo convenience. V3 has no environment/profile guard, so
-- it runs unconditionally against whatever database Flyway connects to, including production -
-- meaning any fresh or rebuilt production database (disaster recovery, moving hosts, etc.)
-- would silently recreate a publicly-documented admin credential from a public GitHub repo.
--
-- Disables the account rather than deleting it outright: a hard DELETE could violate the
-- supermarkets.reviewed_by foreign key if this account was ever used to approve/reject a store
-- (see V3), whereas `enabled = false` is checked first in AuthService#login, before the
-- password is even compared - it fully blocks login regardless of what the password is set to.
-- The password hash is also scrambled to an unusable value as defense in depth. This runs
-- every time (idempotent no-op if the row doesn't exist or is already disabled).
UPDATE users
SET enabled = false,
    password_hash = '$2b$10$0000000000000000000000eu1ZzX9qk7yT3vN5rB8mC2dS6fH4jL0'
WHERE email = 'admin@aislego.com';
