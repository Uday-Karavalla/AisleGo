-- One-time promotion of the production owner's already-registered account.
--
-- The selector is the MD5 digest of the normalized email rather than the email itself, so
-- personally identifying account data is not published in this repository. The digest is
-- only a row selector, not an authentication secret. On fresh/local databases where that
-- account does not exist, both statements are safe no-ops.
UPDATE users
SET email_verified = true,
    updated_at = now(),
    version = version + 1
WHERE md5(lower(email)) = 'e8dc41b74dc344135e5ae3f177a6a59f';

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN'
FROM users
WHERE md5(lower(email)) = 'e8dc41b74dc344135e5ae3f177a6a59f'
ON CONFLICT DO NOTHING;
