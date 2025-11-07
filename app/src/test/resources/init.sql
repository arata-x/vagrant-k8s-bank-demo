CREATE ROLE appuser LOGIN PASSWORD 'strong-password';

CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION appuser;

CREATE TABLE IF NOT EXISTS app.accounts (
      id UUID PRIMARY KEY DEFAULT uuidv7(),
      owner_name TEXT NOT NULL,
      currency CHAR(3) NOT NULL,
      balance NUMERIC(18, 2) NOT NULL DEFAULT 0,
      version BIGINT NOT NULL DEFAULT 0,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_accounts_owner ON app.accounts(owner_name);

CREATE TABLE IF NOT EXISTS app.ledger_entries (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  account_id UUID NOT NULL CONSTRAINT fk_ledger_account REFERENCES app.accounts(ID),
  direction TEXT NOT NULL,
  amount NUMERIC(18, 2) NOT NULL CHECK (AMOUNT > 0),
  reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);