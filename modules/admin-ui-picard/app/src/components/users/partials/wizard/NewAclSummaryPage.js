import React from "react";
import Notifications from "../../../shared/Notifications";
import { useTranslation } from "react-i18next";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";

const NewAclSummaryPage = ({ previousPage, formik }) => {
	const { t } = useTranslation();

	return (
		<>
			<div className="modal-content">
				<div className="modal-body">
					<div className="full-col">
						<Notifications />
						<div className="obj tbl-list">
							<header className="no-expand">{t("")}</header>
							<div className="obj-container">
								<table className="main-tbl">
									<tr>
										<td>{t("USERS.ACLS.NEW.METADATA.NAME.CAPTION")}</td>
										<td>{formik.values.name}</td>
									</tr>
								</table>
							</div>
						</div>

						<div className="obj tbl-list">
							<header className="no-expand">
								{t("USERS.ACLS.NEW.ACCESS.CAPTION")}
							</header>
							<table className="main-tbl">
								<thead>
									<tr>
										<th className="fit">
											{t("USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.ROLE")}
										</th>
										<th className="fit">
											{t("USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.READ")}
										</th>
										<th className="fit">
											{t("USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.WRITE")}
										</th>
										<th className="fit">
											{t(
												"USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.ADDITIONAL_ACTIONS"
											)}
										</th>
									</tr>
									{formik.values.acls.length > 0 &&
										formik.values.acls.map((acl, key) => (
											<tr key={key}>
												<td>{acl.role}</td>
												<td className="fit">
													<input type="checkbox" disabled checked={acl.read} />
												</td>
												<td className="fit">
													<input type="checkbox" disabled checked={acl.write} />
												</td>
												<td>
													{acl.actions.map((action, key) => (
														<div key={key}>{action}</div>
													))}
												</td>
											</tr>
										))}
								</thead>
							</table>
						</div>
					</div>
				</div>
			</div>

			{/* Button for navigation to next page */}
			<WizardNavigationButtons
				isLast
				formik={formik}
				previousPage={previousPage}
			/>
		</>
	);
};

export default NewAclSummaryPage;
