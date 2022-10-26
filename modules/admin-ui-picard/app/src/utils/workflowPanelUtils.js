// fill values with default configuration of chosen workflow
export const setDefaultConfig = (workflowDefinitions, workflowId) => {
	let defaultConfiguration = {};

	// find configuration panel information about chosen workflow
	let configPanel = workflowDefinitions.find(
		(workflow) => workflow.id === workflowId
	).configuration_panel_json;

	// only set default values if there is an configuration panel
	if (configPanel.length > 0) {
		// iterate through all config options and set their defaults
		configPanel.forEach((configOption) => {
			if (configOption.fieldset) {
				defaultConfiguration = fillDefaultConfig(
					configOption.fieldset,
					defaultConfiguration
				);
			}
		});
	}

	return defaultConfiguration;
};

// fills default configuration with values
const fillDefaultConfig = (fieldset, defaultConfiguration) => {
	// iteration through each input field
	fieldset.forEach((field) => {
		// set value in default configuration
		if (field.type !== "radio") {
			defaultConfiguration[field.name] = field.value;
		} else {
			// set only the checked input of radio button as default value
			if (field.type === "radio" && field.checked) {
				defaultConfiguration[field.name] = field.value;
			}
		}

		// if an input has further configuration then go through fillDefaultConfig again
		if (field.fieldset) {
			defaultConfiguration = fillDefaultConfig(
				field.fieldset,
				defaultConfiguration
			);
		}
	});

	return defaultConfiguration;
};
