import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getSourceURL } from "../../../../utils/embeddedCodeUtils";

/**
 * This component renders the embedding code modal
 */
const EmbeddingCodeModal = ({ close, eventId }) => {
	const { t } = useTranslation();

	const [textAreaContent, setTextAreaContent] = useState("");
	const [sourceURL, setSourceURL] = useState("");
	const [currentSize, setCurrentSize] = useState("0x0");
	const [showCopySuccess, setCopySuccess] = useState(false);

	useEffect(() => {
		const fetchData = async () => {
			// get source url
			let sourceURL = await getSourceURL();

			setSourceURL(sourceURL);
		};
		fetchData();
	}, []);

	const handleClose = () => {
		close();
	};

	const copy = () => {
		let copyText = document.getElementById("social_embed-textarea");
		copyText.select();
		document.execCommand("copy");

		setCopySuccess(true);
	};

	const updateTextArea = (e) => {
		// chosen frame size
		let frameSize = e.target ? e.target.textContent : e.toElement.textContent;

		// buttons containing possible frame sizes
		let embedSizeButtons = document.getElementsByClassName("embedSizeButton");

		// iterate through embedSizeButtons and mark the chosen size
		if (frameSize) {
			for (let i = 0; i < embedSizeButtons.length; i++) {
				if (frameSize === embedSizeButtons[i].id) {
					embedSizeButtons[i].classList.add("embedSizeButtonSelected");
				} else {
					embedSizeButtons[i].classList.remove("embedSizeButtonSelected");
				}
			}
		}
		// split frameSize to be used in iFrameString
		let size = frameSize.split("x");

		// build whole url
		let url = sourceURL + "/play/" + eventId;
		// code displayed in text area containing the iFrame to copy
		let iFrameString =
			'<iframe allowfullscreen src="' +
			url +
			'" style="border:0px #FFFFFF none;" name="Player" scrolling="no" frameborder="0" marginheight="0px" marginwidth="0px" width="' +
			size[0] +
			'" height="' +
			size[1] +
			'"></iframe>';

		// set state with new inputs
		setTextAreaContent(iFrameString);
		setCurrentSize(frameSize);
	};

	return (
		<>
			<div className="modal-animation modal-overlay" />
			<section className="modal modal-animation" id="embedding-code">
				<header>
					<a
						className="fa fa-times close-modal"
						onClick={() => handleClose()}
					/>
					<h2>{t("CONFIRMATIONS.ACTIONS.SHOW.EMBEDDING_CODE")}</h2>
				</header>

				{/* embed size buttons */}
				<div className="embedded-code-boxes">
					<div
						id="620x349"
						className="embedSizeButton size_620x349"
						onClick={(e) => updateTextArea(e)}
					>
						<span className="span-embedded-code">620x349</span>
					</div>
					<div
						id="540x304"
						className="embedSizeButton size_540x304"
						onClick={(e) => updateTextArea(e)}
					>
						<span className="span-embedded-code">540x304</span>
					</div>
					<div
						id="460x259"
						className="embedSizeButton size_460x259"
						onClick={(e) => updateTextArea(e)}
					>
						<span className="span-embedded-code">460x259</span>
					</div>
					<div
						id="380x214"
						className="embedSizeButton size_380x214"
						onClick={(e) => updateTextArea(e)}
					>
						<span className="span-embedded-code">380x214</span>
					</div>
					<div
						id="300x169"
						className="embedSizeButton size_300x169"
						onClick={(e) => updateTextArea(e)}
					>
						<span className="span-embedded-code">300x169</span>
					</div>
				</div>

				<span id="id_video" className="embedded-code-no-visible">
					{eventId}
				</span>

				{/* text area containing current iFrame code to copy*/}
				<div className="embedded-code-video">
					<textarea
						id="social_embed-textarea"
						className="social_embed-textarea embedded-code-textarea"
						rows="4"
						value={textAreaContent}
						cols="1"
					/>
				</div>

				{/* copy confirmation */}
				{showCopySuccess && (
					<div className="copyConfirm">
						<span id="copy_confirm_pre">
							{t("CONFIRMATIONS.EMBEDDING_CODE")}
						</span>
						<span id="copy_confirm">{currentSize}</span>
					</div>
				)}

				{/* copy button */}
				<div className="embedded-code-copy-to-clipboard">
					<div className="btn-container" style={{ marginButton: "20px" }}>
						<button
							className="cancel-btn"
							style={{ fontSize: "14px" }}
							onClick={() => copy()}
						>
							{t("COPY")}
						</button>
					</div>
				</div>
			</section>
		</>
	);
};

export default EmbeddingCodeModal;
