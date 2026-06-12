-- Self-serve sheet music delivery: orders, order items, and piece file mapping.
-- Schema is managed manually (ddl-auto: none). Run against prod and qa schemas.

-- Bare filename only (e.g. 'my-piece.pdf'), resolved against PIECE_FILES_DIR.
ALTER TABLE piece ADD COLUMN file_name VARCHAR(255);

-- "order" is a reserved word in SQL, so the table is named "orders".
CREATE TABLE orders (
  id                 BIGSERIAL PRIMARY KEY,
  stripe_session_id  VARCHAR(255) NOT NULL UNIQUE,
  buyer_email        VARCHAR(320),
  status             VARCHAR(32)  NOT NULL,
  amount_total       BIGINT,
  currency           VARCHAR(8),
  download_token     VARCHAR(64)  NOT NULL UNIQUE,
  token_expires_at   TIMESTAMPTZ  NOT NULL,
  download_count     INT          NOT NULL DEFAULT 0,
  max_downloads      INT          NOT NULL DEFAULT 50,
  email_sent_at      TIMESTAMPTZ,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  fulfilled_at       TIMESTAMPTZ
);

CREATE TABLE order_item (
  id          BIGSERIAL PRIMARY KEY,
  order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  piece_id    BIGINT NOT NULL REFERENCES piece(id),
  quantity    INT NOT NULL DEFAULT 1,
  unit_amount BIGINT
);

CREATE INDEX idx_order_item_order ON order_item(order_id);
