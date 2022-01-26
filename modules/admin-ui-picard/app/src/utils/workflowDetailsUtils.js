import moment from "moment";

/**
 * This file contains functions and constants that are needed in the event details modal in the workflow details sub-tabs
 */

export const style_nav = {
    borderBottom: "1px solid #d6d6d6",
    lineHeight: "35px",
};

export const style_nav_hierarchy_inactive = {
    marginLeft: "30px",
    color: "#92a0ab"
};

export const style_nav_hierarchy = {
    marginLeft: "30px",
    marginRight: "30px",
    fontWeight: "600",
    color: "#5d7589"
};

export const error_detail_style = {
        overflow: "auto",
        width: "750px"
};

export const formatDuration = (durationInMS) => {
        const duration = moment.duration(durationInMS);
        if (duration.asHours() > 1) {
            return Math.floor(duration.asHours()) + moment.utc(duration.asMilliseconds()).format(":mm:ss");
        } else {
            return moment.utc(duration.asMilliseconds()).format("mm:ss");
        }
};