import { useTranslation } from "react-i18next";
import React from "react";

/**
 * This component renders the start cells of events in the table view
 */
const EventsStartCell = ({ row }) => {
	const { t } = useTranslation();

	return (
		// Link template for start date of event
		<span>
			{t("dateFormats.time.short", { time: new Date(row.start_date) })}
		</span>
	);
};

export default EventsStartCell;
