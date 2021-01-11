import {createNotification, removeNotification} from "../actions/notificationActions";
import {getLastAddedNotification, getNotificationById} from "../selectors/notificationSelector";

//todo: Put in config file and get values from there
const ADMIN_NOTIFICATION_DURATION_ERROR = 5;
const ADMIN_NOTIFICATION_DURATION_SUCCESS = 5;
const ADMIN_NOTIFICATION_DURATION_WARNING = 5;
const ADMIN_NOTIFICATION_DURATION_GLOBAL = 5;

export const addNotification = (type, key, duration, parameter, context) => (dispatch, getState) => {
    if (!duration) {
        // todo: exchange with values read from config (see old UI)
        // fall back to defaults
        // eslint-disable-next-line default-case
        switch (type) {
            case 'error':
                duration = ADMIN_NOTIFICATION_DURATION_ERROR;
                break;
            case 'success':
                duration = ADMIN_NOTIFICATION_DURATION_SUCCESS;
                break;
            case 'warning':
                duration = ADMIN_NOTIFICATION_DURATION_WARNING;
                break;
        }
    }
    // default durations are in seconds. duration needs to be in milliseconds
    if (duration > 0) duration *= 1000;

    if (!context) {
        context = 'global';
    }

    if (!parameter) {
        parameter = {};
    }

    // Create new notification, id is set in action
    const notification = {
        type: type,
        key: key,
        message: 'NOTIFICATIONS.' + key,
        parameter: parameter,
        duration: duration,
        hidden: false,
        context: context
    }
    dispatch(createNotification(notification));

    // Get newly created notification and its id
    let latestNotification = getLastAddedNotification(getState());

    // Fade out notification if it is not -1 -> -1 means 'stay forever'
    // Start timeout for fading out after time in duration is over
    if (parseInt(latestNotification.duration) && parseInt(latestNotification.duration) !== -1) {
        setTimeout(() => dispatch(removeNotification(latestNotification.id)), latestNotification.duration);
    }
}
