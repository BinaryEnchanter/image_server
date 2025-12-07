-- V1 初始化 schema
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
  uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username TEXT NOT NULL UNIQUE,
  email TEXT UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'user',
  coins BIGINT DEFAULT 0,
  -- extra JSONB DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS wallpapers (
  uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_uuid UUID REFERENCES users(uuid) ON DELETE SET NULL,
  name TEXT NOT NULL,
  description TEXT,
  tags TEXT,              -- comma separated tags
  width INT,
  height INT,
  size_bytes BIGINT,
  download_count BIGINT DEFAULT 0,
  favorite_count BIGINT DEFAULT 0,
  paid BOOLEAN DEFAULT FALSE,
  price_cents INT DEFAULT 0,
  storage_path TEXT NOT NULL,
  thumb_path TEXT,
  phash TEXT,
  visibility TEXT DEFAULT 'public',
  local_only BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS favorites (
  id BIGSERIAL PRIMARY KEY,
  user_uuid UUID REFERENCES users(uuid) ON DELETE CASCADE,
  wallpaper_uuid UUID REFERENCES wallpapers(uuid) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS purchases (
  id BIGSERIAL PRIMARY KEY,
  user_uuid UUID REFERENCES users(uuid),
  wallpaper_uuid UUID REFERENCES wallpapers(uuid),
  price_cents INT,
  currency TEXT DEFAULT 'coins',
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS uploads (
  id BIGSERIAL PRIMARY KEY,
  user_uuid UUID REFERENCES users(uuid),
  wallpaper_uuid UUID,
  original_filename TEXT,
  status TEXT DEFAULT 'processing',
  error_msg TEXT,
  ip TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

-- index for name search
CREATE INDEX IF NOT EXISTS idx_wallpapers_name ON wallpapers USING gin (to_tsvector('english', name));

