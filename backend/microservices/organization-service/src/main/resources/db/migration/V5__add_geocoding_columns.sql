-- V5: Add latitude and longitude columns to organizations table for geocoding support
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS latitude  DOUBLE NULL,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL;
