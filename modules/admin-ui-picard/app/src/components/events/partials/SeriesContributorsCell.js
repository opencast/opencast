import React from "react";

/**
 * This component renders the contributors cells of series in the table view
 */
const SeriesContributorsCell = ({ row }) => {
	return row.contributors.map((contributor, key) => (
		<span key={key} className="metadata-entry">
			{contributor}
		</span>
	));
};

export default SeriesContributorsCell;
