import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import cn from "classnames";
import { getFilterProfiles } from "../../selectors/tableFilterProfilesSelectors";
import {
	cancelEditFilterProfile,
	createFilterProfile,
	editFilterProfile,
	removeFilterProfile,
} from "../../actions/tableFilterProfilesActions";
import { getFilters } from "../../selectors/tableFilterSelectors";
import { loadFilterProfile } from "../../actions/tableFilterActions";

/**
 * This component renders the table filter profiles in the upper right corner when clicked on settings icon of the
 * table filters.
 */
const TableFiltersProfiles = ({
	showFilterSettings,
	setFilterSettings,
	createFilterProfile,
	filterMap,
	cancelEditFilterProfile,
	profiles,
	removeFilterProfile,
	loadFilterProfile,
	loadResource,
	loadResourceIntoTable,
	resource,
}) => {
	// State for switching between list of profiles and saving/editing dialog
	const [settingsMode, setSettingsMode] = useState(true);

	// State for helping saving and editing profiles
	const [profileName, setProfileName] = useState("");
	const [profileDescription, setProfileDescription] = useState("");
	const [currentlyEditing, setCurrentlyEditing] = useState("");
	const [validName, setValidName] = useState(false);

	const { t } = useTranslation();

	const currentProfiles = profiles.filter(
		(profile) => profile.resource === resource
	);

	// todo: Maybe saving to storage is needed
	const saveProfile = () => {
		if (validName) {
			const filterProfile = {
				name: profileName,
				description: profileDescription,
				filterMap: filterMap,
				resource: resource,
			};
			createFilterProfile(filterProfile);
		}
		setSettingsMode(!settingsMode);
		resetStateValues();
	};

	const editFilterProfile = (profile) => {
		setSettingsMode(false);
		setCurrentlyEditing(profile);
		setProfileName(profile.name);
		setProfileDescription(profile.description);
		removeFilterProfile(profile);
		setValidName(true);
	};

	const cancelEditProfile = () => {
		if (currentlyEditing !== "") {
			createFilterProfile(currentlyEditing);
		}
		cancelEditFilterProfile();
		setSettingsMode(!settingsMode);
		setFilterSettings(!showFilterSettings);
		resetStateValues();
	};

	const resetStateValues = () => {
		setProfileName("");
		setProfileDescription("");
		setCurrentlyEditing("");
		setValidName(false);
	};

	const handleChange = (e) => {
		const itemName = e.target.name;
		const itemValue = e.target.value;

		if (itemName === "name") {
			const isDuplicated = profiles.some(
				(profile) => profile.name === itemValue
			);
			if (!isDuplicated) {
				setValidName(true);
			} else {
				setValidName(false);
			}
			setProfileName(itemValue);
		}
		if (itemName === "description") {
			setProfileDescription(itemValue);
		}
	};

	const chooseFilterProfile = (filterMap) => {
		loadFilterProfile(filterMap);

		// Reload resources when filters are removed
		loadResource();
		loadResourceIntoTable();
	};

	return (
		<>
			{/*Show filter profiles dialog if settings icon in TableFilters is clicked*/}
			{showFilterSettings && (
				<div className="btn-dd filter-settings-dd df-profile-filters">
					{/* depending on settingsMode show list of all saved profiles or the chosen profile to edit*/}
					{settingsMode ? (
						// if settingsMode is true the list with all saved profiles is shown
						<div className="filters-list">
							<header>
								<a
									className="icon close"
									onClick={() => setFilterSettings(!showFilterSettings)}
								/>
								<h4>{t("TABLE_FILTERS.PROFILES.FILTERS_HEADER")}</h4>
							</header>
							<ul>
								{currentProfiles.length === 0 ? (
									//if no profiles saved yet
									<li>{t("TABLE_FILTERS.PROFILES.EMPTY")}</li>
								) : (
									// repeat for each profile in profiles filtered for currently shown resource (else-case)
									currentProfiles.map((profile, key) => (
										<li key={key}>
											<a
												title="profile.description"
												onClick={() => chooseFilterProfile(profile.filterMap)}
											>
												{profile.name.substr(0, 70)}
											</a>
											{/* Settings icon to edit profile */}
											<a
												onClick={() => editFilterProfile(profile)}
												title={t("TABLE_FILTERS.PROFILES.EDIT")}
												className="icon edit"
											/>
											{/* Remove icon to remove profile */}
											<a
												onClick={() => removeFilterProfile(profile)}
												title={t("TABLE_FILTERS.PROFILES.REMOVE")}
												className="icon remove"
											/>
										</li>
									))
								)}
							</ul>

							{/* Save the currently selected filter options as new profile */}
							{/* settingsMode is switched and save dialog is opened*/}
							<div className="input-container">
								<div className="btn-container">
									<a
										className="save"
										onClick={() => setSettingsMode(!settingsMode)}
									>
										{t("TABLE_FILTERS.PROFILES.SAVE_FILTERS").substr(0, 70)}
									</a>
								</div>
							</div>
						</div>
					) : (
						// if settingsMode is false then show editing dialog of selected filter profile
						<div className="filter-details">
							<header>
								<a
									className="icon close"
									onClick={() => {
										setFilterSettings(!showFilterSettings);
										setSettingsMode(true);
									}}
								/>
								<h4>{t("TABLE_FILTERS.PROFILES.FILTER_HEADER")}</h4>
							</header>
							{/* Input form for save/editing profile*/}
							<div>
								<label>
									{t("TABLE_FILTERS.PROFILES.NAME")}{" "}
									<i className="required">*</i>
								</label>
								{/*Input for name of the filter profile*/}
								<input
									required
									name="name"
									type="text"
									value={profileName}
									onChange={(e) => handleChange(e)}
									placeholder={t("TABLE_FILTERS.PROFILES.NAME_PLACEHOLDER")}
								/>

								<label>{t("TABLE_FILTERS.PROFILES.DESCRIPTION")}</label>
								{/*Input for a description of the filter profile*/}
								<textarea
									value={profileDescription}
									name="description"
									onChange={(e) => handleChange(e)}
									placeholder={t(
										"TABLE_FILTERS.PROFILES.DESCRIPTION_PLACEHOLDER"
									)}
								/>
							</div>
							<div className="input-container">
								{/* Buttons for saving and canceling editing */}
								<div className="btn-container">
									<a onClick={cancelEditProfile} className="cancel">
										{t("CANCEL")}
									</a>
									<a
										onClick={saveProfile}
										className={cn("save", { disabled: !validName })}
									>
										{t("SAVE")}
									</a>
								</div>
							</div>
						</div>
					)}
				</div>
			)}
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	filterMap: getFilters(state),
	profiles: getFilterProfiles(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadFilterProfile: (filterMap) => dispatch(loadFilterProfile(filterMap)),
	createFilterProfile: (filterProfile) =>
		dispatch(createFilterProfile(filterProfile)),
	editFilterProfile: (filterProfile) =>
		dispatch(editFilterProfile(filterProfile)),
	removeFilterProfile: (filterProfile) =>
		dispatch(removeFilterProfile(filterProfile)),
	cancelEditFilterProfile: () => dispatch(cancelEditFilterProfile()),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(TableFiltersProfiles);
