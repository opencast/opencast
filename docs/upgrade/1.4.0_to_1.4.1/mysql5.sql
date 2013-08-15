DROP INDEX IX_mh_annotation_user ON mh_annotation;

set @exist := (select count(*) from information_schema.statistics where table_name = 'mh_user_action' and index_name = 'IX_mh_user_action_user');
set @sqlstmt := if( @exist > 0, 'DROP INDEX IX_mh_user_action_user ON mh_user_action', 'SELECT "1.4.x schema detected"' );
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

set @exist := (select count(*) from information_schema.statistics where table_name = 'mh_user_action' and index_name = 'IX_mh_user_action_user_id');
set @sqlstmt := if( @exist > 0, 'DROP INDEX IX_mh_user_action_user_id ON mh_user_action', 'SELECT "1.3.x upgraded schema detected"' );
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

ALTER TABLE mh_annotation CHANGE user user_id VARCHAR(255);
ALTER TABLE mh_user_action CHANGE user user_id VARCHAR(255);

CREATE INDEX IX_mh_annotation_user_id ON mh_annotation (user_id);
CREATE INDEX IX_mh_user_action_user_d ON mh_user_action (user_id);

