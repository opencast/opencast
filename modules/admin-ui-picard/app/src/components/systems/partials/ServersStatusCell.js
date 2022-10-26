import React from "react";

/**
 * This component renders the status cells of servers in the table view
 */
const ServersStatusCell = ({ row }) => {
	return row.online && !row.maintenance ? (
		<div className="circle green" />
	) : row.online && row.maintenance ? (
		<div className="circle yellow" />
	) : !row.online ? (
		<div className="circle red" />
	) : null;
};

export default ServersStatusCell;
