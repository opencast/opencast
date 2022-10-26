import React from "react";
import { useTranslation } from "react-i18next";
import ThemeDetails from "./ThemeDetails";

/**
 * This component renders the modal for displaying theme details
 */
const ThemeDetailsModal = ({ handleClose, themeId, themeName }) => {
	const { t } = useTranslation();

	const close = () => {
		handleClose();
	};

	return (
		<>
			<div className="modal-animation modal-overlay" />
			<section
				id="theme-details-modal"
				className="modal wizard modal-animation"
			>
				<header>
					<a className="fa fa-times close-modal" onClick={() => close()} />
					<h2>
						{t("CONFIGURATION.THEMES.DETAILS.EDITCAPTION", { name: themeName })}
					</h2>
				</header>

				{/* component that manages tabs of theme details modal*/}
				<ThemeDetails themeId={themeId} close={close} />
			</section>
		</>
	);
};

export default ThemeDetailsModal;
