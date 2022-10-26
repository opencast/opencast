import ServersStatusCell from "../../components/systems/partials/ServersStatusCell";
import MeanRunTimeCell from "../../components/systems/partials/MeanRunTimeCell";
import MeanQueueTimeCell from "../../components/systems/partials/MeanQueueTimeCell";
import ServersMaintenanceCell from "../../components/systems/partials/ServersMaintenanceCell";

/**
 * Config that contains the columns and further information regarding servers. These are the information that never or hardly changes.
 * That's why it is hard coded here and not fetched from server.
 * Information configured in this file:
 * - columns: names, labels, sortable, (template)
 * - caption for showing in table view
 * - resource type (here: servers)
 * - category type (here: systems)
 * - is multi select possible?
 */
export const serversTableConfig = {
	columns: [
		{
			template: "ServersStatusCell",
			name: "online",
			label: "SYSTEMS.SERVERS.TABLE.STATUS",
			sortable: true,
		},
		{
			name: "hostname",
			label: "SYSTEMS.SERVERS.TABLE.HOST_NAME",
			sortable: true,
		},
		{
			name: "nodeName",
			label: "SYSTEMS.SERVERS.TABLE.NODE_NAME",
			sortable: true,
		},
		{
			name: "cores",
			label: "SYSTEMS.SERVERS.TABLE.CORES",
			sortable: true,
		},
		{
			name: "completed",
			label: "SYSTEMS.SERVERS.TABLE.COMPLETED",
			sortable: true,
		},
		{
			name: "running",
			label: "SYSTEMS.SERVERS.TABLE.RUNNING",
			sortable: true,
		},
		{
			name: "queued",
			label: "SYSTEMS.SERVERS.TABLE.QUEUED",
			sortable: true,
		},
		{
			template: "MeanRunTimeCell",
			name: "meanRunTime",
			label: "SYSTEMS.SERVERS.TABLE.MEAN_RUN_TIME",
			sortable: true,
		},
		{
			template: "MeanQueueTimeCell",
			name: "meanQueueTime",
			label: "SYSTEMS.SERVERS.TABLE.MEAN_QUEUE_TIME",
			sortable: true,
		},
		{
			template: "ServersMaintenanceCell",
			name: "maintenance",
			label: "SYSTEMS.SERVERS.TABLE.MAINTENANCE",
			sortable: true,
		},
	],
	caption: "SYSTEMS.SERVERS.TABLE.CAPTION",
	resource: "servers",
	category: "systems",
	multiSelect: false,
};

/**
 * This map contains the mapping between the template strings above and the corresponding react component.
 * This helps to render different templates of cells more dynamically. Even empty needed, because Table component
 * uses template map.
 */
export const serversTemplateMap = {
	ServersStatusCell: ServersStatusCell,
	MeanRunTimeCell: MeanRunTimeCell,
	MeanQueueTimeCell: MeanQueueTimeCell,
	ServersMaintenanceCell: ServersMaintenanceCell,
};
