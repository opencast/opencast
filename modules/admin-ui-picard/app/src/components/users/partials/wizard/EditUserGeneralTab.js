import React from "react";
import { useTranslation } from "react-i18next";
import cn from "classnames";
import { Field } from "formik";

/**
 * This component renders the general user information tab in the users details modal.
 */
const EditUserGeneralTab = ({ formik }) => {
	const { t } = useTranslation();

	// style used in user details modal
	const editStyle = {
		color: "#666666",
		fontSize: "14px",
	};

	// style for input fields when disabled
	const disabledStyle = {
		backgroundColor: "#eeeff0",
	};

	return (
		<div className="modal-content">
			<div className="modal-body">
				<div className="form-container">
					{!formik.values.manageable && (
						<div className="modal-alert warning">
							<p>{t("NOTIFICATIONS.USER_NOT_MANAGEABLE")}</p>
						</div>
					)}
					<div className="row" style={editStyle}>
						<label>
							{t("USERS.USERS.DETAILS.FORM.USERNAME")}
							<i className="required">*</i>
						</label>
						<input
							type="text"
							name="username"
							style={disabledStyle}
							disabled
							value={formik.values.username}
						/>
					</div>
					<div className="row" style={editStyle}>
						<label>{t("USERS.USERS.DETAILS.FORM.NAME")}</label>
						<Field
							type="text"
							name="name"
							style={formik.values.manageable ? {} : disabledStyle}
							disabled={!formik.values.manageable}
							className={cn({
								error: formik.touched.name && formik.errors.name,
							})}
							value={formik.values.name}
						/>
					</div>
					<div className="row" style={editStyle}>
						<label>{t("USERS.USERS.DETAILS.FORM.EMAIL")}</label>
						<Field
							type="text"
							name="email"
							style={formik.values.manageable ? {} : disabledStyle}
							disabled={!formik.values.manageable}
							className={cn({
								error: formik.touched.email && formik.errors.email,
							})}
							value={formik.values.email}
						/>
					</div>
					<div className="row" style={editStyle}>
						<label>{t("USERS.USERS.DETAILS.FORM.PASSWORD")}</label>
						<Field
							type="password"
							name="password"
							style={formik.values.manageable ? {} : disabledStyle}
							disabled={!formik.values.manageable}
							className={cn({
								error: formik.touched.password && formik.errors.password,
							})}
							placeholder={t("USERS.USERS.DETAILS.FORM.PASSWORD") + "..."}
						/>
					</div>
					<div className="row" style={editStyle}>
						<label>{t("USERS.USERS.DETAILS.FORM.REPEAT_PASSWORD")}</label>
						<Field
							type="password"
							name="passwordConfirmation"
							style={formik.values.manageable ? {} : disabledStyle}
							disabled={!formik.values.manageable}
							className={cn({
								error:
									formik.touched.passwordConfirmation &&
									formik.errors.passwordConfirmation,
							})}
							placeholder={
								t("USERS.USERS.DETAILS.FORM.REPEAT_PASSWORD") + "..."
							}
						/>
					</div>
				</div>
			</div>
		</div>
	);
};

export default EditUserGeneralTab;
