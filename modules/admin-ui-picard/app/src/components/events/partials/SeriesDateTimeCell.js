import React from "react";
import { useTranslation } from "react-i18next";

/**
 * This component renders the creation date cells of series in the table view
 */
const SeriesDateTimeCell = ({ row }) => {
	const { t } = useTranslation();

	return (
		// Link template for creation date of series
		<span>
			{t("dateFormats.dateTime.short", {
				dateTime: new Date(row.creation_date),
			})}
		</span>
	);
};

export default SeriesDateTimeCell;
