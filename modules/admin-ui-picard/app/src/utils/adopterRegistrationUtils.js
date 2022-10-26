import axios from "axios";

// get information about adopter
export const fetchAdopterRegistration = async () => {
	// fetch current information about adopter
	const response = await axios.get("/admin-ng/adopter/registration");

	return await response.data;
};

// post request for adopter information
export const postRegistration = async (values) => {
	// build body
	let body = new URLSearchParams();
	body.append("contactMe", values.contactMe);
	body.append("allowsStatistics", values.allowsStatistics);
	body.append("allowsErrorReports", values.allowsErrorReports);
	body.append("organisationName", values.organisationName);
	body.append("departmentName", values.departmentName);
	body.append("country", values.country);
	body.append("postalCode", values.postalCode);
	body.append("city", values.city);
	body.append("firstName", values.firstName);
	body.append("lastName", values.lastName);
	body.append("street", values.street);
	body.append("streetNo", values.streetNo);
	body.append("email", values.email);
	body.append("registered", "true");

	// save adopter information and return next state
	await axios.post("/admin-ng/adopter/registration", body, {
		headers: {
			"Content-Type": "application/x-www-form-urlencoded",
		},
	});
};

// delete adopter information
export const deleteAdopterRegistration = async () => {
	// delete adopter information
	await axios.delete("/admin-ng/adopter/registration", {
		headers: {
			"Content-Type": "application/x-www-form-urlencoded",
		},
	});
};
