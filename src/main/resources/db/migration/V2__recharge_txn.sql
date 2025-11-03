CREATE TABLE IF NOT EXISTS recharge_txn (
  id BIGSERIAL PRIMARY KEY,
  user_uuid UUID NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
  amount_coins BIGINT NOT NULL,        -- 充值的金币数量（整数）
  amount_cents BIGINT DEFAULT 0,       -- 可选：如果想记录金额（测试阶段可留 0）
  provider_txn_id TEXT,                -- 可选：外部支付单号（测试可空）
  status TEXT NOT NULL DEFAULT 'completed', -- completed/failed/pending
  note TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_recharge_user ON recharge_txn(user_uuid);
