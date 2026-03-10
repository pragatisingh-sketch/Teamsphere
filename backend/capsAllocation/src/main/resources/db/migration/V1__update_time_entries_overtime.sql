-- Update existing records to set is_overtime to false
UPDATE time_entries SET is_overtime = false WHERE is_overtime IS NULL;

-- After all records are updated, add the not null constraint
ALTER TABLE time_entries ALTER COLUMN is_overtime SET NOT NULL;
