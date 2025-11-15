-- sql d:\study\空间壁纸\image_server\src\main\resources\db\migration\V5_comments.sql
CREATE TABLE IF NOT EXISTS comments (
  id BIGSERIAL PRIMARY KEY,
  user_uuid UUID NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
  wallpaper_uuid UUID NOT NULL REFERENCES wallpapers(uuid) ON DELETE CASCADE,
  parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  like_count BIGINT DEFAULT 0,
  dislike_count BIGINT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_comments_wallpaper_created ON comments (wallpaper_uuid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_parent ON comments (parent_id);

CREATE TABLE IF NOT EXISTS comment_votes (
  id BIGSERIAL PRIMARY KEY,
  comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
  user_uuid UUID NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
  vote SMALLINT NOT NULL CHECK (vote IN (1,-1)),
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_comment_votes ON comment_votes (comment_id, user_uuid);