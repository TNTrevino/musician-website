-- Initial schema for the Sebastian musician website.
--
-- Every environment uses its own database with the same fixed schema, `sebas`.
-- Objects are explicitly schema-qualified (sebas.*) rather than relying on the
-- connection's search_path, so the result is identical no matter how sqlx/psql
-- connect. The migration self-creates the schema, so a fresh database needs
-- nothing more than `sqlx migrate run` (see migrations/README.md).
--
-- Column types mirror what the JPA entities expect (ddl-auto: none, so the
-- app never creates or validates the schema itself).

CREATE SCHEMA IF NOT EXISTS sebas;

CREATE TABLE sebas.piece (
  id                BIGSERIAL PRIMARY KEY,
  name              VARCHAR(255) NOT NULL UNIQUE,
  composer          VARCHAR(255) NOT NULL,
  price             DOUBLE PRECISION NOT NULL,
  description       VARCHAR(2000) NOT NULL,
  date_composed     INTEGER NOT NULL,
  has_electronics   BOOLEAN NOT NULL,
  completed         BOOLEAN NOT NULL,
  num_of_players    INTEGER NOT NULL,
  difficulty_grade  INTEGER,
  time_length       NUMERIC(21) NOT NULL,   -- java.time.Duration stored as nanoseconds
  file_name         VARCHAR(255)            -- bare filename in PIECE_FILES_DIR; NULL = not sellable
);

CREATE TABLE sebas.users (
  id          BIGSERIAL PRIMARY KEY,
  username    VARCHAR(255),
  first_name  VARCHAR(255) NOT NULL,
  last_name   VARCHAR(255) NOT NULL,
  password    VARCHAR(255) NOT NULL,
  email       VARCHAR(255) NOT NULL,
  -- serialized Spring Security SimpleGrantedAuthority (see Users.java).
  -- A smell worth a future migration (store the role as text), but reproduced
  -- here to match the entity's current mapping.
  authority   BYTEA NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sebas.orders (                            -- "order" is reserved in SQL
  id                 BIGSERIAL PRIMARY KEY,
  stripe_session_id  VARCHAR(255) NOT NULL UNIQUE,   -- idempotency anchor
  buyer_email        VARCHAR(320),
  status             VARCHAR(32)  NOT NULL,          -- PENDING | FULFILLED
  amount_total       BIGINT,                         -- cents
  currency           VARCHAR(8),
  download_token     VARCHAR(64)  NOT NULL UNIQUE,
  token_expires_at   TIMESTAMPTZ  NOT NULL,
  download_count     INT          NOT NULL DEFAULT 0,
  max_downloads      INT          NOT NULL DEFAULT 50,
  email_sent_at      TIMESTAMPTZ,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  fulfilled_at       TIMESTAMPTZ
);

CREATE TABLE sebas.order_item (
  id          BIGSERIAL PRIMARY KEY,
  order_id    BIGINT NOT NULL REFERENCES sebas.orders(id) ON DELETE CASCADE,
  piece_id    BIGINT NOT NULL REFERENCES sebas.piece(id),
  quantity    INT NOT NULL DEFAULT 1,
  unit_amount BIGINT
);

CREATE INDEX idx_order_item_order ON sebas.order_item(order_id);
