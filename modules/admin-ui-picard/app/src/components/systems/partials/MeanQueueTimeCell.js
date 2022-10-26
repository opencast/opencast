import React from "react";
import { useTranslation } from "react-i18next";

/**
 * This component renders the mean queue time cells of systems in the table view
 */
const MeanQueueTimeCell = ({ row }) => {
	const { t } = useTranslation();

	return (
		<span>
			{t("dateFormats.time.medium", { time: new Date(row.meanQueueTime) })}
		</span>
	);
};

export default MeanQueueTimeCell;
