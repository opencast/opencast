-- Increase mime_type field size
ALTER TABLE oc_assets_asset MODIFY COLUMN mime_type VARCHAR (255);

-- Add modified and deletion date fields to series.
--
-- The deletion date is straight forward, as all existing records can correctly use 'null' as value.
-- Modification date is more tricky. We don't want to make the field nullable, so we have to find a
-- somewhat useful value. But we also don't want to set all values to the unix epoch as code
-- could reasonably assume that almost all modification dates of real world series are unique. So
-- we instead derive the modification dates from the series' events. Unix epoch only for series
-- without events.
ALTER TABLE oc_series ADD deletion_date TIMESTAMP NULL;
ALTER TABLE oc_series ADD modified_date TIMESTAMP NULL;
UPDATE oc_series
    SET modified_date = (
        SELECT MAX(oc_search.modification_date)
            FROM oc_search
            WHERE oc_search.series_id = oc_series.id
    );
UPDATE oc_series
    SET modified_date = TIMESTAMP '1970-01-01 00:00:01'
    WHERE modified_date IS NULL;
ALTER TABLE oc_series MODIFY modified_date TIMESTAMP NOT NULL;
