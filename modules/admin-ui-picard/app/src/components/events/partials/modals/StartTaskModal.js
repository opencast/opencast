import React, { useEffect } from "react";
import { Formik } from "formik";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import { initialFormValuesStartTask } from "../../../../configs/modalConfig";
import WizardStepper from "../../../shared/wizard/WizardStepper";
import StartTaskGeneralPage from "../ModalTabsAndPages/StartTaskGeneralPage";
import StartTaskWorkflowPage from "../ModalTabsAndPages/StartTaskWorkflowPage";
import StartTaskSummaryPage from "../ModalTabsAndPages/StartTaskSummaryPage";
import { postTasks } from "../../../../thunks/taskThunks";
import { usePageFunctions } from "../../../../hooks/wizardHooks";
import { checkValidityStartTaskEventSelection } from "../../../../utils/bulkActionUtils";

/**
 * This component manages the pages of the task start bulk action
 */
const StartTaskModal = ({ close, postTasks }) => {
	const { t } = useTranslation();

	const initialValues = initialFormValuesStartTask;

	const [
		snapshot,
		page,
		nextPage,
		previousPage,
		setPage,
		pageCompleted,
		setPageCompleted,
	] = usePageFunctions(0, initialValues);

	const steps = [
		{
			translation: "BULK_ACTIONS.SCHEDULE_TASK.GENERAL.CAPTION",
			name: "general",
		},
		{
			translation: "BULK_ACTIONS.SCHEDULE_TASK.TASKS.CAPTION",
			name: "tasks",
		},
		{
			translation: "BULK_ACTIONS.SCHEDULE_TASK.SUMMARY.CAPTION",
			name: "summary",
		},
	];

	const validateFormik = (values) => {
		const errors = {};
		if (!checkValidityStartTaskEventSelection(values)) {
			errors.events = "Not on all events task startable!";
		}
		if (
			steps[page].name !== "general" &&
			!(
				!!values.workflow &&
				values.workflow !== "" &&
				values.configuration !== {}
			)
		) {
			errors.worflow = "Workflow not selected!";
		}
		return errors;
	};

	const handleSubmit = (values) => {
		postTasks(values);
		close();
	};

	return (
		<>
			<div className="modal-animation modal-overlay" />
			<section className="modal wizard modal-animation">
				<header>
					<a className="fa fa-times close-modal" onClick={() => close()} />
					<h2>{t("BULK_ACTIONS.SCHEDULE_TASK.CAPTION")}</h2>
				</header>

				{/* Initialize overall form */}
				<Formik
					initialValues={snapshot}
					validate={(values) => validateFormik(values)}
					onSubmit={(values) => handleSubmit(values)}
				>
					{/* Render wizard pages depending on current value of page variable */}
					{(formik) => {
						// eslint-disable-next-line react-hooks/rules-of-hooks
						useEffect(() => {
							formik.validateForm().then();
						}, [page]);

						return (
							<>
								{/* Stepper that shows each step of wizard as header */}
								<WizardStepper
									steps={steps}
									page={page}
									setPage={setPage}
									completed={pageCompleted}
									setCompleted={setPageCompleted}
									formik={formik}
								/>
								<div>
									{page === 0 && (
										<StartTaskGeneralPage formik={formik} nextPage={nextPage} />
									)}
									{page === 1 && (
										<StartTaskWorkflowPage
											formik={formik}
											nextPage={nextPage}
											previousPage={previousPage}
											setPageCompleted={setPageCompleted}
										/>
									)}
									{page === 2 && (
										<StartTaskSummaryPage
											formik={formik}
											previousPage={previousPage}
										/>
									)}
								</div>
							</>
						);
					}}
				</Formik>
			</section>
		</>
	);
};

const mapDispatchToState = (dispatch) => ({
	postTasks: (values) => dispatch(postTasks(values)),
});

export default connect(null, mapDispatchToState)(StartTaskModal);
