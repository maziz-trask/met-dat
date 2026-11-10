-- Password sign-in for self-registered users. Seeded demo identities keep NULL (demo login only).
ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(100);
