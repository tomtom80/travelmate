UPDATE account SET date_of_birth = '1900-01-01' WHERE date_of_birth IS NULL;
ALTER TABLE account ALTER COLUMN date_of_birth SET NOT NULL;

UPDATE dependent SET date_of_birth = '1900-01-01' WHERE date_of_birth IS NULL;
ALTER TABLE dependent ALTER COLUMN date_of_birth SET NOT NULL;
