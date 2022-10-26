import React from "react";
import { useTranslation } from "react-i18next";
import cn from "classnames";
import { Step, StepButton, StepLabel, Stepper } from "@material-ui/core";
import {
	isSummaryReachable,
	useStepperStyle,
} from "../../../utils/wizardUtils";
import CustomStepIcon from "./CustomStepIcon";
import { checkAcls } from "../../../thunks/aclThunks";
import { connect } from "react-redux";

/**
 * This components renders the stepper navigation of new resource wizards
 */
const WizardStepper = ({
	steps,
	page,
	setPage,
	formik,
	completed,
	setCompleted,
	hasAccessPage = false,
	checkAcls,
}) => {
	const { t } = useTranslation();

	const classes = useStepperStyle();

	const handleOnClick = async (key) => {
		if (isSummaryReachable(key, steps, completed)) {
			if (hasAccessPage) {
				let check = await checkAcls(formik.values.acls);
				if (!check) {
					return;
				}
			}

			if (formik.isValid) {
				let updatedCompleted = completed;
				updatedCompleted[page] = true;
				setCompleted(updatedCompleted);
				setPage(key);
			}
		}
	};

	const disabled = !(formik.dirty && formik.isValid);

	return (
		<Stepper
			activeStep={page}
			nonLinear
			alternativeLabel
			connector={false}
			className={cn("step-by-step", classes.root)}
		>
			{steps.map((label, key) =>
				!label.hidden ? (
					<Step key={label.translation} completed={completed[key]}>
						<StepButton onClick={() => handleOnClick(key)} disabled={disabled}>
							<StepLabel StepIconComponent={CustomStepIcon}>
								{t(label.translation)}
							</StepLabel>
						</StepButton>
					</Step>
				) : null
			)}
		</Stepper>
	);
};

const mapDispatchToProps = (dispatch) => ({
	checkAcls: (acls) => dispatch(checkAcls(acls)),
});

export default connect(null, mapDispatchToProps)(WizardStepper);
