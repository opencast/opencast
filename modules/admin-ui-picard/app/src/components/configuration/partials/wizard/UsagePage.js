import React from "react";
import { useTranslation } from "react-i18next";

/**
 * This component renders the usage of a theme in the theme details modal
 */
const UsagePage = ({ themeUsage }) => {
	const { t } = useTranslation();

	return (
		<div className="modal-content">
			<div className="modal-body">
				<div className="full-col">
					<div className="obj">
						<div className="obj-container summary-list">
							<table className="main-tbl">
								<thead>
									<tr>
										<th>{t("CONFIGURATION.THEMES.DETAILS.USAGE.SERIE")}</th>
									</tr>
								</thead>
								<tbody>
									{!!themeUsage.series && themeUsage.series.length > 0 ? (
										themeUsage.series.map((usage, key) => (
											<tr key={key}>
												<td>{usage.title}</td>
											</tr>
										))
									) : (
										<tr>
											<td>{t("CONFIGURATION.THEMES.DETAILS.USAGE.EMPTY")}</td>
										</tr>
									)}
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
};

export default UsagePage;
