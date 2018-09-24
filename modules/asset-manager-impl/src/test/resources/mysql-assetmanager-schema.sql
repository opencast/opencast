CREATE TABLE `oc_assets_snapshot` (
  `mediapackage_id` varchar(128) NOT NULL,
  `version` bigint(20) NOT NULL,
  `organization_id` varchar(128) NOT NULL,
  `mediapackage_xml` longtext NOT NULL,
  `archival_date` datetime NOT NULL,
  `id` bigint(20) NOT NULL,
  `availability` varchar(32) NOT NULL,
  `series_id` varchar(128) DEFAULT NULL,
  `storage_id` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNQ_oc_assets_snapshot` (`mediapackage_id`,`version`),
  KEY `IX_oc_assets_snapshot_archival_date` (`archival_date`),
  KEY `IX_oc_assets_snapshot_mediapackage_id` (`mediapackage_id`),
  KEY `IX_oc_assets_snapshot_organization_id` (`organization_id`),
  CONSTRAINT `FK_oc_assets_snapshot_organization` FOREIGN KEY (`organization_id`) REFERENCES `oc_organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oc_assets_asset` (
  `id` bigint(20) NOT NULL,
  `mediapackage_element_id` varchar(128) NOT NULL,
  `checksum` varchar(64) NOT NULL,
  `snapshot_id` bigint(20) NOT NULL,
  `mime_type` varchar(64) DEFAULT NULL,
  `size` bigint(20) NOT NULL,
  `storage_id` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IX_oc_assets_asset_checksum` (`checksum`),
  KEY `IX_oc_assets_asset_mediapackage_element_id` (`mediapackage_element_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oc_assets_properties` (
  `id` bigint(20) NOT NULL,
  `val_bool` tinyint(1) DEFAULT '0',
  `val_date` datetime DEFAULT NULL,
  `val_long` bigint(20) DEFAULT NULL,
  `val_string` varchar(255) DEFAULT NULL,
  `mediapackage_id` varchar(128) NOT NULL,
  `namespace` varchar(128) NOT NULL,
  `property_name` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IX_oc_assets_properties_val_date` (`val_date`),
  KEY `IX_oc_assets_properties_val_long` (`val_long`),
  KEY `IX_oc_assets_properties_val_string` (`val_string`),
  KEY `IX_oc_assets_properties_val_bool` (`val_bool`),
  KEY `IX_oc_assets_properties_mediapackage_id` (`mediapackage_id`),
  KEY `IX_oc_assets_properties_namespace` (`namespace`),
  KEY `IX_oc_assets_properties_property_name` (`property_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oc_assets_version_claim` (
  `mediapackage_id` varchar(128) NOT NULL,
  `last_claimed` bigint(20) NOT NULL,
  PRIMARY KEY (`mediapackage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SEQUENCE` (
  `SEQ_NAME` varchar(50) NOT NULL,
  `SEQ_COUNT` decimal(38,0) DEFAULT NULL,
  PRIMARY KEY (`SEQ_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
