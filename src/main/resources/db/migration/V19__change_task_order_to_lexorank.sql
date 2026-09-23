-- Change task_order from INTEGER to VARCHAR(255) for Lexorank support
-- We use LPAD to zero-pad existing integers to 10 characters so that lexicographical sorting is preserved.
-- E.g. 1 -> "0000000001", 10 -> "0000000010"

ALTER TABLE tasks 
ALTER COLUMN task_order TYPE VARCHAR(255) 
USING LPAD(task_order::text, 10, '0');
