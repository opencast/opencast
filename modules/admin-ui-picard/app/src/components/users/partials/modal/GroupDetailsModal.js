import React from "react";
import { useTranslation } from "react-i18next";
import GroupDetails from "./GroupDetails";

/**
 * This component renders the modal for displaying group details
 */
const GroupDetailsModal = ({ close, groupName }) => {
	const { t } = useTranslation();

	const handleClose = () => {
		close();
	};

	const modalStyle = {
		fontSize: "14px",
		color: "#666666",
	};

	return (
		// todo: add hotkeys
		<>
			<div className="modal-animation modal-overlay" />
			<section
				id="group-modal"
				className="modal wizard modal-animation"
				style={modalStyle}
			>
				<header>
					<a
						className="fa fa-times close-modal"
						onClick={() => handleClose()}
					/>
					<h2>{t("USERS.GROUPS.DETAILS.EDITCAPTION", { name: groupName })}</h2>
				</header>

				{/* component that manages tabs of group details modal*/}
				<GroupDetails close={close} />
			</section>
		</>
	);
};

export default GroupDetailsModal;
