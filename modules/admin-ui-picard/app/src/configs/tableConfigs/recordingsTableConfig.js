import RecordingsActionCell from "../../components/recordings/partials/RecordingsActionCell";
import RecordingsNameCell from "../../components/recordings/partials/RecordingsNameCell";
import RecordingsStatusCell from "../../components/recordings/partials/RecordingsStatusCell";
import RecordingsUpdateCell from "../../components/recordings/partials/RecordingsUpdateCell";

/**
 * Config that contains the columns and further information regarding recordings. These are the information that never or hardly changes.
 * That's why it is hard coded here and not fetched from server.
 * Information configured in this file:
 * - columns: names, labels, sortable, (template)
 * - caption for showing in table view
 * - resource type (here: recordings)
 * - category type (here: recordings)
 * - is multi select possible?
 */
export const recordingsTableConfig = {
	columns: [
		{
			name: "status",
			template: "RecordingsStatusCell",
			label: "RECORDINGS.RECORDINGS.TABLE.STATUS",
			translate: true,
			sortable: true,
		},
		{
			template: "RecordingsNameCell",
			name: "name",
			label: "RECORDINGS.RECORDINGS.TABLE.NAME",
			sortable: true,
		},
		{
			template: "RecordingsUpdateCell",
			name: "update",
			label: "RECORDINGS.RECORDINGS.TABLE.UPDATED",
			sortable: true,
		},
		{
			template: "RecordingsActionCell",
			name: "actions",
			label: "RECORDINGS.RECORDINGS.TABLE.ACTION",
		},
	],
	caption: "RECORDINGS.RECORDINGS.TABLE.CAPTION",
	resource: "recordings",
	category: "recordings",
	multiSelect: false,
};

/**
 * This map contains the mapping between the template strings above and the corresponding react component.
 * This helps to render different templates of cells more dynamically
 */
export const recordingsTemplateMap = {
	RecordingsActionCell: RecordingsActionCell,
	RecordingsNameCell: RecordingsNameCell,
	RecordingsStatusCell: RecordingsStatusCell,
	RecordingsUpdateCell: RecordingsUpdateCell,
};
