import React from "react";
import { useTranslation } from "react-i18next";

/**
 * This component renders the submitted date cells of jobs in the table view
 */
const JobsSubmittedCell = ({ row }) => {
	const { t } = useTranslation();

	return (
		<span>
			{t("dateFormats.dateTime.short", { dateTime: new Date(row.submitted) })}
		</span>
	);
};

export default JobsSubmittedCell;
