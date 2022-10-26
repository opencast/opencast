import ThemesActionsCell from "../../components/configuration/partials/ThemesActionsCell";

/**
 * Config that contains the columns and further information regarding themes. These are the information that never or hardly changes.
 * That's why it is hard coded here and not fetched from server.
 * Information configured in this file:
 * - columns: names, labels, sortable, (template)
 * - caption for showing in table view
 * - resource type (here: themes)
 * - category type (here: configuration)
 * - is multi select possible?
 */
export const themesTableConfig = {
	columns: [
		{
			name: "name",
			label: "CONFIGURATION.THEMES.TABLE.NAME",
			sortable: true,
		},
		{
			name: "description",
			label: "CONFIGURATION.THEMES.TABLE.DESCRIPTION",
			sortable: true,
		},
		{
			name: "creator",
			label: "CONFIGURATION.THEMES.TABLE.CREATOR",
			sortable: true,
		},
		{
			name: "creation_date",
			label: "CONFIGURATION.THEMES.TABLE.CREATED",
			sortable: true,
		},
		{
			template: "ThemesActionsCell",
			name: "actions",
			label: "CONFIGURATION.THEMES.TABLE.ACTION",
		},
	],
	caption: "CONFIGURATION.THEMES.TABLE.CAPTION",
	resource: "themes",
	category: "configuration",
	multiSelect: false,
};

/**
 * This map contains the mapping between the template strings above and the corresponding react component.
 * This helps to render different templates of cells more dynamically. Even empty needed, because Table component
 * uses template map.
 */
export const themesTemplateMap = {
	ThemesActionsCell: ThemesActionsCell,
};
