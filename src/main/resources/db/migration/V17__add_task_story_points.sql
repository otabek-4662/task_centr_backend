-- Add story_points to tasks table
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS story_points INTEGER;
