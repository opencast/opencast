import React, { useEffect, useState } from "react";
import { Formik } from "formik";
import NewEventSummary from "./NewEventSummary";
import { MuiPickersUtilsProvider } from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import { getCurrentLanguageInformation } from "../../../../utils/utils";
import NewAssetUploadPage from "../ModalTabsAndPages/NewAssetUploadPage";
import NewMetadataExtendedPage from "../ModalTabsAndPages/NewMetadataExtendedPage";
import NewMetadataPage from "../ModalTabsAndPages/NewMetadataPage";
import NewAccessPage from "../ModalTabsAndPages/NewAccessPage";
import NewProcessingPage from "../ModalTabsAndPages/NewProcessingPage";
import NewSourcePage from "../ModalTabsAndPages/NewSourcePage";
import { NewEventSchema } from "../../../../utils/validate";
import { logger } from "../../../../utils/logger";
import WizardStepperEvent from "../../../shared/wizard/WizardStepperEvent";
import { getInitialMetadataFieldValues } from "../../../../utils/resourceUtils";
import { sourceMetadata } from "../../../../configs/sourceConfig";
import { initialFormValuesNewEvents } from "../../../../configs/modalConfig";
import {
	getAssetUploadOptions,
	getEventMetadata,
	getExtendedEventMetadata,
} from "../../../../selectors/eventSelectors";
import { postNewEvent } from "../../../../thunks/eventThunks";
import { connect } from "react-redux";

// Get info about the current language and its date locale
const currentLanguage = getCurrentLanguageInformation();

/**
 * This component manages the pages of the new event wizard and the submission of values
 */
const NewEventWizard = ({
	metadataFields,
	extendedMetadata,
	close,
	postNewEvent,
	uploadAssetOptions,
}) => {
	const initialValues = getInitialValues(
		metadataFields,
		extendedMetadata,
		uploadAssetOptions
	);
	let workflowPanelRef = React.useRef();

	const [page, setPage] = useState(0);
	const [snapshot, setSnapshot] = useState(initialValues);
	const [pageCompleted, setPageCompleted] = useState({});

	// Caption of steps used by Stepper
	const steps = [
		{
			translation: "EVENTS.EVENTS.NEW.METADATA.CAPTION",
			name: "metadata",
		},
		{
			translation: "EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA",
			name: "metadata-extended",
			hidden: !(!!extendedMetadata && extendedMetadata.length > 0),
		},
		{
			translation: "EVENTS.EVENTS.NEW.SOURCE.CAPTION",
			name: "source",
		},
		{
			translation: "EVENTS.EVENTS.NEW.UPLOAD_ASSET.CAPTION",
			name: "upload-asset",
			hidden:
				uploadAssetOptions.filter((asset) => asset.type !== "track").length ===
				0,
		},
		{
			translation: "EVENTS.EVENTS.NEW.PROCESSING.CAPTION",
			name: "processing",
		},
		{
			translation: "EVENTS.EVENTS.NEW.ACCESS.CAPTION",
			name: "access",
		},
		{
			translation: "EVENTS.EVENTS.NEW.SUMMARY.CAPTION",
			name: "summary",
		},
	];

	// Validation schema of current page
	const currentValidationSchema = NewEventSchema[page];

	const nextPage = (values) => {
		setSnapshot(values);

		// set page as completely filled out
		let updatedPageCompleted = pageCompleted;
		updatedPageCompleted[page] = true;
		setPageCompleted(updatedPageCompleted);

		if (steps[page + 1].hidden) {
			setPage(page + 2);
		} else {
			setPage(page + 1);
		}
	};

	const previousPage = (values, twoPagesBack) => {
		setSnapshot(values);
		// if previous page is hidden or not always shown, than go back two pages
		if (steps[page - 1].hidden || twoPagesBack) {
			setPage(page - 2);
		} else {
			setPage(page - 1);
		}
	};

	const handleSubmit = (values) => {
		workflowPanelRef.current?.submitForm();
		const response = postNewEvent(values, metadataFields, extendedMetadata);
		logger.info(response);
		close();
	};

	return (
		<>
			{/* Initialize overall form */}
			<MuiPickersUtilsProvider
				utils={DateFnsUtils}
				locale={currentLanguage.dateLocale}
			>
				<Formik
					initialValues={snapshot}
					validationSchema={currentValidationSchema}
					onSubmit={(values) => handleSubmit(values)}
				>
					{/* Render wizard pages depending on current value of page variable */}
					{(formik) => {
						// eslint-disable-next-line react-hooks/rules-of-hooks
						useEffect(() => {
							formik.validateForm();
							// eslint-disable-next-line react-hooks/exhaustive-deps
						}, [page]);

						return (
							<>
								{/* Stepper that shows each step of wizard as header */}
								<WizardStepperEvent
									steps={steps}
									page={page}
									setPage={setPage}
									completed={pageCompleted}
									setCompleted={setPageCompleted}
									formik={formik}
								/>
								<div>
									{page === 0 && (
										<NewMetadataPage
											nextPage={nextPage}
											formik={formik}
											metadataFields={metadataFields}
											header={steps[page].translation}
										/>
									)}
									{page === 1 && (
										<NewMetadataExtendedPage
											previousPage={previousPage}
											nextPage={nextPage}
											formik={formik}
											extendedMetadataFields={extendedMetadata}
										/>
									)}
									{page === 2 && (
										<NewSourcePage
											previousPage={previousPage}
											nextPage={nextPage}
											formik={formik}
										/>
									)}
									{page === 3 && (
										<NewAssetUploadPage
											previousPage={previousPage}
											nextPage={nextPage}
											formik={formik}
										/>
									)}
									{page === 4 && (
										<NewProcessingPage
											previousPage={previousPage}
											nextPage={nextPage}
											workflowPanelRef={workflowPanelRef}
											formik={formik}
										/>
									)}
									{page === 5 && (
										<NewAccessPage
											previousPage={previousPage}
											nextPage={nextPage}
											formik={formik}
											editAccessRole="ROLE_UI_SERIES_DETAILS_ACL_EDIT"
										/>
									)}
									{page === 6 && (
										<NewEventSummary
											previousPage={previousPage}
											formik={formik}
											metaDataExtendedHidden={steps[1].hidden}
											assetUploadHidden={steps[3].hidden}
										/>
									)}
								</div>
							</>
						);
					}}
				</Formik>
			</MuiPickersUtilsProvider>
		</>
	);
};

// Transform all initial values needed from information provided by backend
const getInitialValues = (
	metadataFields,
	extendedMetadata,
	uploadAssetOptions
) => {
	// Transform metadata fields provided by backend (saved in redux)
	let initialValues = getInitialMetadataFieldValues(
		metadataFields,
		extendedMetadata
	);

	// Transform additional metadata for source (provided by constant in newEventConfig)
	if (!!sourceMetadata.UPLOAD) {
		sourceMetadata.UPLOAD.metadata.forEach((field) => {
			initialValues[field.id] = field.value;
		});
	}
	if (!!sourceMetadata.SINGLE_SCHEDULE) {
		sourceMetadata.SINGLE_SCHEDULE.metadata.forEach((field) => {
			initialValues[field.id] = field.value;
		});
	}
	if (!!sourceMetadata.MULTIPLE_SCHEDULE) {
		sourceMetadata.MULTIPLE_SCHEDULE.metadata.forEach((field) => {
			initialValues[field.id] = field.value;
		});
	}

	// Add possible files that can be uploaded in source step
	if (!!uploadAssetOptions) {
		initialValues.uploadAssetsTrack = [];
		// initial value of upload asset needs to be null, because object (file) is saved there
		uploadAssetOptions.forEach((option) => {
			if (option.type === "track") {
				initialValues.uploadAssetsTrack.push({
					...option,
					file: null,
				});
			} else {
				initialValues[option.id] = null;
			}
		});
	}

	// Add all initial form values known upfront listed in newEventsConfig
	for (const [key, value] of Object.entries(initialFormValuesNewEvents)) {
		initialValues[key] = value;
	}

	const defaultDate = new Date();

	// fill times with some default values
	initialValues["scheduleStartHour"] = (defaultDate.getHours() + 1).toString();
	initialValues["scheduleStartMinute"] = "00";
	initialValues["scheduleDurationHours"] = "00";
	initialValues["scheduleDurationMinutes"] = "55";
	initialValues["scheduleEndHour"] = (defaultDate.getHours() + 1).toString();
	initialValues["scheduleEndMinute"] = "55";

	return initialValues;
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	metadataFields: getEventMetadata(state),
	extendedMetadata: getExtendedEventMetadata(state),
	uploadAssetOptions: getAssetUploadOptions(state),
});

const mapDispatchToProps = (dispatch) => ({
	postNewEvent: (values, metadataFields, extendedMetadata) =>
		dispatch(postNewEvent(values, metadataFields, extendedMetadata)),
});

export default connect(mapStateToProps, mapDispatchToProps)(NewEventWizard);
