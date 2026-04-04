-- Migration: support half-star ratings (0.5, 1.0, ..., 5.0)
-- Run this in Supabase SQL editor

-- Books
ALTER TABLE books
  ALTER COLUMN rating TYPE NUMERIC(3,1);

ALTER TABLE books
  DROP CONSTRAINT IF EXISTS rating_only_for_read;

ALTER TABLE books
  DROP CONSTRAINT IF EXISTS books_rating_check;

ALTER TABLE books
  ADD CONSTRAINT books_rating_check
    CHECK (rating IS NULL OR (rating >= 0.5 AND rating <= 5 AND MOD(rating * 2, 1) = 0));

ALTER TABLE books
  ADD CONSTRAINT rating_only_for_read
    CHECK ((status = 'read') OR (rating IS NULL));

-- Movies
ALTER TABLE movies
  ALTER COLUMN rating TYPE NUMERIC(3,1);

ALTER TABLE movies
  DROP CONSTRAINT IF EXISTS movies_rating_check;

ALTER TABLE movies
  ADD CONSTRAINT movies_rating_check
    CHECK (rating IS NULL OR (rating >= 0.5 AND rating <= 5 AND MOD(rating * 2, 1) = 0));

-- Series
ALTER TABLE series
  ALTER COLUMN rating TYPE NUMERIC(3,1);

ALTER TABLE series
  DROP CONSTRAINT IF EXISTS series_rating_check;

ALTER TABLE series
  ADD CONSTRAINT series_rating_check
    CHECK (rating IS NULL OR (rating >= 0.5 AND rating <= 5 AND MOD(rating * 2, 1) = 0));
