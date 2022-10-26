import React from "react";
import { useTranslation } from "react-i18next";

/**
 * This component renders the access table containing access rules provided by user before in wizard summary pages
 */
const AccessSummaryTable = ({ policies, header }) => {
	const { t } = useTranslation();

	return (
		<div className="obj tbl-list">
			<header className="no-expand">{t(header)}</header>
			<table className="main-tbl">
				<thead>
					<tr>
						<th className="fit">
							{t("EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.ROLE")}
						</th>
						<th className="fit">
							{t("EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.READ")}
						</th>
						<th className="fit">
							{t("EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.WRITE")}
						</th>
						<th className="fit">
							{t("EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.ADDITIONAL_ACTIONS")}
						</th>
					</tr>
				</thead>
				<tbody>
					{/*Insert row for each policy user has provided*/}
					{policies.map((policy, key) => (
						<tr key={key}>
							<td>{policy.role}</td>
							<td className="fit">
								<input type="checkbox" disabled checked={policy.read} />
							</td>
							<td className="fit">
								<input type="checkbox" disabled checked={policy.write} />
							</td>
							<td className="fit">
								{/*repeat for each additional action*/}
								{policy.actions.map((action, key) => (
									<div key={key}>{action}</div>
								))}
							</td>
						</tr>
					))}
				</tbody>
			</table>
		</div>
	);
};

export default AccessSummaryTable;
