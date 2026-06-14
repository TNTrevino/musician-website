-- Production catalog seed, transcribed from the prod DB dump.
-- REVIEW before running — then apply once to a fresh prod DB AFTER migrations:
--   psql "$DATABASE_URL" -f backend/sql/seed_prod.sql
--
-- Notes:
--   * The old Stripe `productid` is intentionally omitted (column removed).
--   * file_name must match the actual PDF uploaded to PIECE_FILES_DIR. The
--     completed pieces have placeholder names below — confirm/adjust them.
--   * WIP pieces (completed=false) have file_name NULL, so they cannot be sold
--     until a PDF is uploaded and file_name is set.
--   * time_length is a java.time.Duration stored as nanoseconds.

INSERT INTO sebas.piece
  (name, composer, price, description, date_composed, has_electronics,
   completed, num_of_players, difficulty_grade, time_length, file_name)
VALUES
  ('Standing on The Shoulders of Giants', 'Sebastian Havner', 50.00,
   'Standing on The Shoulders of Giants is a piece for two marimba soloists with a backing percussion ensemble. The piece is written in a way that while the soloists are in the spotlight, the whole ensemble is very involved andrather than just support the soloists, they have prominent voices that create the piece’s energy.To me, the beauty of music is the emotions it can bring with it. There is a certain almost larger than life feeling Ihave experienced at many points listening to or performing music, when you are completely pulled into a piece itcan be a profound experience. I wrote Standing on the Shoulders of Giants with the intent to invoke thoseemotions in the audience and players while listening or performing.',
   2023, false, true, 8, 6, 480000000000, 'standing-on-the-shoulders-of-giants.pdf'),

  ('Celestial', 'Sebastian Havner', 30.00,
   'Celestial is a percussion sextet written for my alma mater, Berkner Highschool. My intention with this piece was to write a beginner-intermediate level percussion ensemble with the energy and feeling of a professional levelpiece so students can have an enjoyable experience and eventually seamlessly transition to the evolving modernplaystyle. The piece does not follow a strict storyline but one can loosely interpret it as an adventure into space and thereturn back home. It encapsulates the imagery of the cosmos as well as the unknown factor of what dangerscould be out there.',
   2024, true, true, 6, 3, 240000000000, 'celestial.pdf'),

  ('Memento Mori', 'Sebastian Havner', 40.00,
   'Memento Mori is a Latin phrase translating to “Remember that you must die”. I wrote this piece to convey the many different feelings that this idea can cause in a person. At face value, the idea of death can be a very scary and dark thought, although without death, the meaning of life is diminished. Memento Mori aims to show the constant convolution of how death is viewed, while generally intense and “negative” at times the piece diverges to showcase the beauty of death as the finale to someone''s story, rather than an undesirable end. I purposefully switch between different tones within the music to convey my own uncertainty with the idea of death. Memento Mori is written for solo marimba with a percussion quartet accompaniment. It is an advanced piece with individual challenges as well as difficult ensemble moments. The soloist weaves in and out of the spotlight, keeping an interesting blend of soloistic moments and full ensemble ideas. I intended to create a compelling soundscape between the warmer timbre of the marimba with the bright sounds of the metallic instruments backing up the soloist, driven forward by the energy of the hand drumming.',
   2024, false, true, 5, 6, 360000000000, 'memento-mori.pdf'),

  -- WIP: not sellable until a PDF is uploaded and file_name is set
  ('Athanatos', 'Sebastian Havner', 20.00, 'WIP',
   2025, true, false, 1, 6, 360000000000, NULL),

  ('Azure', 'Sebastian Havner', 30.00, 'WIP',
   2025, false, false, 2, 6, 360000000000, NULL)
ON CONFLICT (name) DO NOTHING;
