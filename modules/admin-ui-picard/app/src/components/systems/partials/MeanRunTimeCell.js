import React from "react";
import { useTranslation } from "react-i18next";

/**
 * This component renders the mean run time cells of systems in the table view
 */
const MeanRunTimeCell = ({ row }) => {
	const { t } = useTranslation();

	return (
		<span>
			{t("dateFormats.time.medium", { time: new Date(row.meanRunTime) })}
		</span>
	);
};

export default MeanRunTimeCell;
