-- Whether the sign-in e-mail is verified, stored so the backend can query by it (admin user lists, and later the
-- community features: comments, player search). Permissions still come from the token of each request.
-- Existing users start as false; the first request after their next sign-in stores the real value (Google
-- accounts are always verified, e-mail accounts once the link was clicked).
ALTER TABLE users ADD COLUMN email_verified boolean NOT NULL DEFAULT false;
