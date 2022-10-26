import SeriesTitleCell from "../../components/events/partials/SeriesTitleCell";
import SeriesCreatorsCell from "../../components/events/partials/SeriesCreatorsCell";
import SeriesContributorsCell from "../../components/events/partials/SeriesContributorsCell";
import SeriesDateTimeCell from "../../components/events/partials/SeriesDateTimeCell";
import SeriesActionsCell from "../../components/events/partials/SeriesActionsCell";

/**
 * Config that contains the columns and further information regarding series. These are the information that never or hardly changes.
 * That's why it is hard coded here and not fetched from server.
 * Information configured in this file:
 * - columns: names, labels, sortable, (template)
 * - caption for showing in table view
 * - resource type (here: series)
 * - category type (here: events)
 * - is multi select possible?
 */
export const seriesTableConfig = {
	columns: [
		{
			template: "SeriesTitleCell",
			name: "title",
			label: "EVENTS.SERIES.TABLE.TITLE",
			sortable: true,
		},
		{
			template: "SeriesCreatorsCell",
			name: "organizers",
			label: "EVENTS.SERIES.TABLE.ORGANIZERS",
			sortable: true,
		},
		{
			template: "SeriesContributorsCell",
			name: "contributors",
			label: "EVENTS.SERIES.TABLE.CONTRIBUTORS",
			sortable: true,
		},
		{
			template: "SeriesDateTimeCell",
			name: "creation_date",
			label: "EVENTS.SERIES.TABLE.CREATED",
			sortable: true,
		},
		{
			template: "SeriesActionsCell",
			name: "actions",
			label: "EVENTS.SERIES.TABLE.ACTION",
		},
	],
	caption: "EVENTS.SERIES.TABLE.CAPTION",
	resource: "series",
	category: "events",
	multiSelect: true,
};

/**
 * This map contains the mapping between the template strings above and the corresponding react component.
 * This helps to render different templates of cells more dynamically
 */
export const seriesTemplateMap = {
	SeriesTitleCell: SeriesTitleCell,
	SeriesCreatorsCell: SeriesCreatorsCell,
	SeriesContributorsCell: SeriesContributorsCell,
	SeriesDateTimeCell: SeriesDateTimeCell,
	SeriesActionsCell: SeriesActionsCell,
};
