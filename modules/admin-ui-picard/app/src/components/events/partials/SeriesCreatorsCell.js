import React from "react";

/**
 * This component renders the creators cells of series in the table view
 */
const SeriesCreatorsCell = ({ row }) => {
	return row.organizers.map((organizer, key) => (
		<span key={key} className="metadata-entry">
			{organizer}
		</span>
	));
};

export default SeriesCreatorsCell;
