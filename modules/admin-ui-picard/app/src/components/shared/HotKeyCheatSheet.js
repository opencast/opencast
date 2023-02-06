import React from "react";
import { useTranslation } from "react-i18next";
import { availableHotkeys } from "../../configs/hotkeysConfig";

/**
 * This component renders the hotkey cheat sheet showing all available hotkeys
 */
const HotKeyCheatSheet = ({ close }) => {
	const { t } = useTranslation();

	const handleClose = () => {
		close();
	};

	return (
		<>
			<div className="modal-animation modal-overlay" />
			<div className="modal modal-animation">
				<header>
					<a
						className="fa fa-times close-modal"
						onClick={() => handleClose()}
					/>
					<h2>{t("HOTKEYS.CHEAT_SHEET.TITLE")}</h2>
				</header>
				<div className="modal-content active">
					<div className="modal-body">
						<div className="full-col">
							<p className="hint">{t("HOTKEYS.CHEAT_SHEET.HINT")}</p>
							{/* Repeat table for each key */}
							{Object.keys(availableHotkeys).map((hotkeyGroup, key) => (
								<div className="obj tbl-list" key={key}>
									<header>
										{t("HOTKEYS.GROUPS." + hotkeyGroup.toUpperCase())}
									</header>
									<table className="main-tbl">
										<tbody>
											{/* Repeat row for each hotkey in group*/}
											{Object.keys(availableHotkeys[hotkeyGroup]).map(
												(hotkey, key) => (
													<tr key={key}>
														<td className="hotkey">
															<p className="combo">
																<span className="chord">
																	{/* repeat for each key in hotkey */}
																	{availableHotkeys[hotkeyGroup][
																		hotkey
																	].combo.map((comboKey, key) => (
																		<>
																			<span key={key}>
																				<span className="key">
																					{t(
																						"HOTKEYS.KEYS." +
																							comboKey.toUpperCase(),
																						comboKey
																					)}
																				</span>
																			</span>
																			{comboKey ===
																			availableHotkeys[hotkeyGroup][hotkey]
																				.combo[
																				availableHotkeys[hotkeyGroup][hotkey]
																					.combo.length - 1
																			]
																				? ""
																				: " + "}
																		</>
																	))}
																</span>
															</p>
														</td>
														<td>
															{t(
																availableHotkeys[hotkeyGroup][hotkey]
																	.description
															)}
														</td>
													</tr>
												)
											)}
										</tbody>
									</table>
								</div>
							))}
						</div>
					</div>
				</div>
			</div>
		</>
	);
};

export default HotKeyCheatSheet;
