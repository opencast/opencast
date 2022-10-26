import React from "react";

/**
 * This component renders the feeds tab of a certain series
 */
const SeriesDetailsFeedsTab = ({ feeds }) => {
	return (
		<div className="modal-content">
			<div className="modal-body">
				<div className="full-col">
					<div className="obj">
						<div className="obj-container">
							<table className="main-tbl">
								<tbody>
									<tr>
										<th>Type</th>
										<th>Version</th>
										<th>Link</th>
									</tr>
									{/*repeat for each feed link*/}
									{feeds.length > 0 &&
										feeds.map((feed, key) => (
											<tr key={key}>
												<td>{feed.type}</td>
												<td>{feed.version}</td>
												<td>
													<a href={feed.link}>{feed.link}</a>
												</td>
											</tr>
										))}
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
};

export default SeriesDetailsFeedsTab;
