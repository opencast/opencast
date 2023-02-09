import React, { useState } from "react";
import { useTranslation } from "react-i18next";

/**
 * This component renders the effective role tab of the user details modal
 */
const UserEffectiveRolesTab = ({ formik }) => {
	const { t } = useTranslation();

	const [searchField, setSearchField] = useState("");
	const [defaultItems] = useState(formik.values.roles);
	const [items, setItems] = useState(formik.values.roles);

	const clearSearchField = () => {
		setSearchField("");
		setItems(defaultItems);
	};

	const handleChangeSearch = async (input) => {
		const filtered = defaultItems.filter((item) => {
			return item.name.toLowerCase().includes(input.toLowerCase());
		});
		setSearchField(input);
		setItems(filtered);
	};

	return (
		<div className="modal-content">
			<div className="modal-body">
				<div className="form-container multi-select-container">
					<label>{t("USERS.USERS.DETAILS.TABS.EFFECTIVEROLES")}</label>
					<p>{t("USERS.USERS.DETAILS.DESCRIPTION.EFFECTIVEROLES")}</p>

					{/* list  all roles a user got */}
					<a className="clear" onClick={() => clearSearchField()} />
					<input
						type="text"
						id="search_effective"
						className="search"
						value={searchField}
						onChange={(e) => handleChangeSearch(e.target.value)}
						placeholder={t("TABLE_FILTERS.PLACEHOLDER")}
					/>

					<select multiple style={{ height: "26em" }}>
						{items.map((item, key) => (
							<option key={key} value={item.name}>
								{item.name}
							</option>
						))}
					</select>
				</div>
			</div>
		</div>
	);
};

export default UserEffectiveRolesTab;
