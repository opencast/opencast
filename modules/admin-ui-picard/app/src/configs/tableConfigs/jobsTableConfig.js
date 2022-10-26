import JobsStartedCell from "../../components/systems/partials/JobsStartedCell";
import JobsSubmittedCell from "../../components/systems/partials/JobsSubmittedCell";

/**
 * Config that contains the columns and further information regarding jobs. These are the information that never or hardly changes.
 * That's why it is hard coded here and not fetched from server.
 * Information configured in this file:
 * - columns: names, labels, sortable, (template)
 * - caption for showing in table view
 * - resource type (here: jobs)
 * - category type (here: systems)
 * - is multi select possible?
 */
export const jobsTableConfig = {
	columns: [
		{
			name: "id",
			label: "SYSTEMS.JOBS.TABLE.ID",
			sortable: true,
		},
		{
			name: "status",
			label: "SYSTEMS.JOBS.TABLE.STATUS",
			translate: true,
			sortable: true,
		},
		{
			name: "operation",
			label: "SYSTEMS.JOBS.TABLE.OPERATION",
			sortable: true,
		},
		{
			name: "type",
			label: "SYSTEMS.JOBS.TABLE.TYPE",
			sortable: true,
		},
		{
			name: "processingHost",
			label: "SYSTEMS.JOBS.TABLE.HOST_NAME",
			sortable: true,
		},
		{
			name: "processingNode",
			label: "SYSTEMS.JOBS.TABLE.NODE_NAME",
			sortable: true,
		},
		{
			template: "JobsSubmittedCell",
			name: "submitted",
			label: "SYSTEMS.JOBS.TABLE.SUBMITTED",
			sortable: true,
		},
		{
			template: "JobsStartedCell",
			name: "started",
			label: "SYSTEMS.JOBS.TABLE.STARTED",
			sortable: true,
		},
		{
			name: "creator",
			label: "SYSTEMS.JOBS.TABLE.CREATOR",
			sortable: true,
		},
	],
	caption: "SYSTEMS.JOBS.TABLE.CAPTION",
	resource: "jobs",
	category: "systems",
	multiSelect: false,
};

/**
 * This map contains the mapping between the template strings above and the corresponding react component.
 * This helps to render different templates of cells more dynamically. Even empty needed, because Table component
 * uses template map.
 */
export const jobsTemplateMap = {
	JobsStartedCell: JobsStartedCell,
	JobsSubmittedCell: JobsSubmittedCell,
};
