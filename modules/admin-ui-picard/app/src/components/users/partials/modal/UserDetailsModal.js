import React from "react";
import { useTranslation } from "react-i18next";
import UserDetails from "./UserDetails";

/**
 * This component renders the modal for displaying user details
 */
const UserDetailsModal = ({ close, username }) => {
	const { t } = useTranslation();

	const handleClose = () => {
		close();
	};

	const modalStyle = {
		fontSize: "14px",
	};

	return (
		// todo: add hotkeys
		<>
			<div className="modal-animation modal-overlay" />
			<section
				id="user-details-modal"
				className="modal wizard modal-animation"
				style={modalStyle}
			>
				<header>
					<a
						className="fa fa-times close-modal"
						onClick={() => handleClose()}
					/>
					<h2>
						{t("USERS.USERS.DETAILS.EDITCAPTION", { username: username })}
					</h2>
				</header>

				{/* component that manages tabs of user details modal*/}
				<UserDetails close={close} />
			</section>
		</>
	);
};

export default UserDetailsModal;
