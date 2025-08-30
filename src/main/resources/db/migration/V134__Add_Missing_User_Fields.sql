-- Migration to add missing fields to the users table that are defined in the User entity
-- This resolves the mismatch between the JPA entity and the actual database schema

-- Add hire_date column for tracking when users were hired
ALTER TABLE users 
ADD COLUMN hire_date DATE;

-- Add comment to document the field
COMMENT ON COLUMN users.hire_date IS 'Date when the user was hired by the company';

-- Add index for performance when filtering by hire date
CREATE INDEX idx_users_hire_date ON users(hire_date) WHERE hire_date IS NOT NULL AND deleted_at IS NULL;

-- Update existing users to have a default hire date based on their created_at timestamp
-- This is optional - you may want to leave it null for existing users
-- UPDATE users 
-- SET hire_date = created_at::date 
-- WHERE hire_date IS NULL AND deleted_at IS NULL;