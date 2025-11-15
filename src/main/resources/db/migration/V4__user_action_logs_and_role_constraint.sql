-- sql d:\study\空间壁纸\image_server\src\main\resources\db\migration\V4__user_action_logs_and_role_constraint.sql
-- 1) 操作日志表（若不存在则创建）
CREATE TABLE IF NOT EXISTS user_action_logs (
  id BIGSERIAL PRIMARY KEY,
  user_uuid UUID NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
  action TEXT NOT NULL,
  target_uuid UUID,
  meta JSONB DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_action_logs_user_created
  ON user_action_logs (user_uuid, created_at DESC);

-- 2) users.role 约束（若未添加则添加）
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_users_role'
      AND conrelid = 'users'::regclass
  ) THEN
    ALTER TABLE users
      ADD CONSTRAINT chk_users_role
      CHECK (role IN ('admin','user','blacklist'));
  END IF;
END
$$;

-- 3) purchases 表补充 wallpaper_uuid 字段（若不存在则添加）
ALTER TABLE purchases
  ADD COLUMN IF NOT EXISTS wallpaper_uuid UUID REFERENCES wallpapers(uuid);

-- 可选索引，提升查询性能
CREATE INDEX IF NOT EXISTS idx_purchases_user_wallpaper
  ON purchases (user_uuid, wallpaper_uuid);

ALTER TABLE user_action_logs
  ADD COLUMN IF NOT EXISTS username TEXT;

UPDATE user_action_logs l
SET username = u.username
FROM users u
WHERE l.user_uuid = u.uuid
  AND (l.username IS NULL OR l.username = '');

ALTER TABLE user_action_logs
  ALTER COLUMN username SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_user_action_logs_username
  ON user_action_logs (username);