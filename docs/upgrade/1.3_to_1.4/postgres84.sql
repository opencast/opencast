/*
 * PostgreSQL 8.4 upgrade script for Matterhorn 1.3 to trunk
 */

/**
 * MH-8648 UCB Features
 */
alter table service_registration add column online_from timestamp;
alter table service_registration add column service_state varying(32) NOT NULL default 'NORMAL';
alter table service_registration add column sate_changed timestamp;
alter table service_registration add column warning_state_trigger bigint default 0;
alter table service_registration add column error_state_trigger bigint default 0;