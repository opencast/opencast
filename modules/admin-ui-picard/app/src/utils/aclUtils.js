/*This File contains functions that are needed in several access right views*/

export const getAclTemplateText = (aclTemplates, formikTemplate) => {
	if (!!aclTemplates && aclTemplates.length > 0) {
		const template = aclTemplates.find(
			(template) => formikTemplate === template.id
		);
		return !!template ? template.value : "";
	} else {
		return "";
	}
};

export const filterRoles = (roles, policies) => {
	return roles.filter(
		(role) => !policies.find((policy) => policy.role === role.name)
	);
};
