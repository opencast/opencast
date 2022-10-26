import React from "react";
import { Field } from "formik";
import Notifications from "../../../shared/Notifications";
import { useTranslation } from "react-i18next";
import cn from "classnames";

/**
 * This component renders the general user information tab for new users in the new users wizard.
 */
const NewUserGeneralTab = ({ formik }) => {
	const { t } = useTranslation();

	return (
		<div className="modal-content">
			<div className="modal-body">
				<div className="form-container">
					<Notifications />
					{/* Fields for user information needed */}
					<div className="row">
						<label>
							{t("USERS.USERS.DETAILS.FORM.USERNAME")}
							<i className="required">*</i>
						</label>
						<Field
							type="text"
							name="username"
							autoFocus
							className={cn({
								error: formik.touched.username && formik.errors.username,
							})}
							placeholder={t("USERS.USERS.DETAILS.FORM.USERNAME") + "..."}
						/>
					</div>
					<div className="row">
						<label>
							{t("USERS.USERS.DETAILS.FORM.NAME")}
							<i className="required">*</i>
						</label>
						<Field
							type="text"
							name="name"
							className={cn({
								error: formik.touched.name && formik.errors.name,
							})}
							placeholder={t("USERS.USERS.DETAILS.FORM.NAME") + "..."}
						/>
					</div>
					<div className="row">
						<label>
							{t("USERS.USERS.DETAILS.FORM.EMAIL")}
							<i className="required">*</i>
						</label>
						<Field
							type="text"
							name="email"
							className={cn({
								error: formik.touched.email && formik.errors.email,
							})}
							placeholder={t("USERS.USERS.DETAILS.FORM.EMAIL") + "..."}
						/>
					</div>
					<div className="row">
						<label>
							{t("USERS.USERS.DETAILS.FORM.PASSWORD")}
							<i className="required">*</i>
						</label>
						<Field
							type="password"
							name="password"
							className={cn({
								error: formik.touched.password && formik.errors.password,
							})}
							placeholder={t("USERS.USERS.DETAILS.FORM.PASSWORD") + "..."}
						/>
					</div>
					<div className="row">
						<label>
							{t("USERS.USERS.DETAILS.FORM.REPEAT_PASSWORD")}
							<i className="required">*</i>
						</label>
						<Field
							type="password"
							name="passwordConfirmation"
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

export default NewUserGeneralTab;
