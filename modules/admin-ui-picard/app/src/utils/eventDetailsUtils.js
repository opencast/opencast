import moment from "moment";

/**
 * This file contains functions and constants that are needed in the event details modal
 */

export const style_nav = {
	borderBottom: "1px solid #d6d6d6",
	lineHeight: "35px",
};

export const style_nav_hierarchy_inactive = {
	marginLeft: "30px",
	color: "#92a0ab",
};

export const style_nav_hierarchy = {
	marginLeft: "30px",
	marginRight: "30px",
	fontWeight: "600",
	color: "#5d7589",
};

export const style_button_spacing = {
	marginTop: "13px",
	marginLeft: "15px",
	marginRight: "15px",
};

export const error_detail_style = {
	overflow: "auto",
	width: "750px",
};

export const formatDuration = (durationInMS) => {
	const duration = moment.duration(durationInMS);
	if (duration.asHours() > 1) {
		return moment.utc(duration.asMilliseconds()).format("HH:mm:ss");
	} else {
		return moment.utc(duration.asMilliseconds()).format("mm:ss");
	}
};

export const humanReadableBytesFilter = (bytesValue) => {
	// best effort, independent on type
	let bytes = parseInt(bytesValue);

	if (isNaN(bytes)) {
		return bytesValue;
	}

	// from http://stackoverflow.com/a/14919494
	const thresh = 1000;
	if (Math.abs(bytes) < thresh) {
		return bytes + " B";
	}
	const units = ["kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"];
	let u = -1;
	do {
		bytes /= thresh;
		++u;
	} while (Math.abs(bytes) >= thresh && u < units.length - 1);

	return bytes.toFixed(1) + " " + units[u];
};
