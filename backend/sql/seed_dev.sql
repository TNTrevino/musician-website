-- Seed data for QA and local dev. NOT for prod.
-- Run AFTER `sqlx migrate run` (see migrations/README.md):
--   psql "$DATABASE_URL" -f backend/sql/seed_dev.sql
--
-- Each piece's file_name must exist in PIECE_FILES_DIR (drop in any small PDF
-- named sample.pdf). time_length is a java.time.Duration stored as nanoseconds.

INSERT INTO sebas.piece
  (name, composer, price, description, date_composed, has_electronics,
   completed, num_of_players, difficulty_grade, time_length, file_name)
VALUES
  ('Test Sonata', 'Sebastian Havner', 1.00,
   'A seeded piece for QA and dev checkout testing.',
   2024, false, true, 1, 3, 300000000000, 'sample.pdf'),       -- 5 min
  ('Test Duet', 'Sebastian Havner', 2.50,
   'A second seeded piece so multi-item carts can be tested.',
   2025, false, true, 2, 4, 480000000000, 'sample.pdf'),       -- 8 min
  ('Test Electronics Etude', 'Sebastian Havner', 0.50,
   'Seeded piece with electronics flag for filter testing.',
   2023, true, true, 1, 6, 600000000000, 'sample.pdf')         -- 10 min
ON CONFLICT (name) DO NOTHING;
