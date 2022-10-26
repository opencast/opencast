import { makeStyles } from "@material-ui/core";

// Base style for Stepper component
export const useStepperStyle = makeStyles((theme) => ({
	root: {
		background: "#eeeff0",
		height: "100px",
	},
}));

// Style of icons used in Stepper
export const useStepIconStyles = makeStyles({
	root: {
		height: 22,
		alignItems: "center",
	},
	circle: {
		color: "#92a0ab",
		width: "20px",
		height: "20px",
	},
});

/* This method checks if the summary page is reachable.
 * If the clicked page is some other page than summary then no check is needed.
 * If the clicked page is summary then it only should be clickable/reachable if all other
 * visible pages of the wizard are valid.
 */
export const isSummaryReachable = (key, steps, completed) => {
	if (steps[key].name === "summary") {
		const visibleSteps = steps.filter((step) => !step.hidden);

		return Object.keys(completed).length >= visibleSteps.length - 2;
	}

	return true;
};
