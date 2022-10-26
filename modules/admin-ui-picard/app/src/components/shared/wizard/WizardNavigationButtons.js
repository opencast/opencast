import React from "react";
import { useTranslation } from "react-i18next";
import cn from "classnames";

const WizardNavigationButtons = ({
	isFirst,
	isLast,
	noValidation,
	formik,
	nextPage,
	previousPage,
}) => {
	const { t } = useTranslation();

	const validation = noValidation
		? {}
		: {
				active: formik.dirty && formik.isValid,
				inactive: !(formik.dirty && formik.isValid),
		  };

	const disabled = !(formik.dirty && formik.isValid);

	return (
		<>
			<footer>
				{isLast ? (
					<button
						type="submit"
						className={cn("submit", validation)}
						disabled={noValidation ? false : disabled}
						onClick={() => {
							formik.handleSubmit();
						}}
						tabIndex="100"
					>
						{t("WIZARD.CREATE")}
					</button>
				) : (
					<button
						type="submit"
						className={cn("submit", validation)}
						disabled={noValidation ? false : disabled}
						onClick={() => {
							nextPage(formik.values);
						}}
						tabIndex="100"
					>
						{t("WIZARD.NEXT_STEP")}
					</button>
				)}
				{!isFirst && (
					<button
						className="cancel"
						onClick={() => previousPage(formik.values, false)}
						tabIndex="101"
					>
						{t("WIZARD.BACK")}
					</button>
				)}
			</footer>

			<div className="btm-spacer" />
		</>
	);
};

export default WizardNavigationButtons;
