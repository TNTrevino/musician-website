-- Reverses 20260614000001_initial_schema.up.sql.
-- Dropped in reverse dependency order (order_item -> orders/piece -> users).

DROP TABLE IF EXISTS sebas.order_item;
DROP TABLE IF EXISTS sebas.orders;
DROP TABLE IF EXISTS sebas.users;
DROP TABLE IF EXISTS sebas.piece;
