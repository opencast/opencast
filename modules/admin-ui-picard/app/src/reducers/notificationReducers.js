import {
	CREATE_NOTIFICATION,
	REMOVE_NOTIFICATION,
	REMOVE_NOTIFICATION_WIZARD_ACCESS,
	REMOVE_NOTIFICATION_WIZARD_FORM,
	SET_HIDDEN,
} from "../actions/notificationActions";
import {
	NOTIFICATION_CONTEXT,
	NOTIFICATION_CONTEXT_ACCESS,
} from "../configs/modalConfig";

/*
State is looking something like this
notifications: {
    notificationPositionGlobal: 'bottom-right'
    notifications: [{
        message: "",
        id: "",
        hidden: false,
        duration: 0,
        type: "error",
        parameter: "",
        key: "",
        context: ""
    }, ...],

}
*/

/**
 * This file contains redux reducer for actions affecting the state of table
 */

// Initial state of notifications in store
// If you want to change the position of notification, here it can be done
const initialState = {
	notificationPositionGlobal: "bottom-right",
	notifications: [],
};

// Reducer for notifications
export const notifications = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case CREATE_NOTIFICATION: {
			const { notification, id } = payload;
			return {
				...state,
				notifications: [
					...state.notifications,
					{
						id: id,
						key: notification.key,
						message: notification.message,
						type: notification.type,
						hidden: notification.hidden,
						duration: notification.duration,
						parameter: notification.parameter,
						context: notification.context,
					},
				],
			};
		}
		case REMOVE_NOTIFICATION: {
			const { id: idToRemove } = payload;
			return {
				...state,
				notifications: state.notifications.filter(
					(notification) => notification.id !== idToRemove
				),
			};
		}
		case REMOVE_NOTIFICATION_WIZARD_FORM: {
			return {
				...state,
				notifications: state.notifications.filter(
					(notification) => notification.context !== NOTIFICATION_CONTEXT
				),
			};
		}
		case REMOVE_NOTIFICATION_WIZARD_ACCESS: {
			return {
				...state,
				notifications: state.notifications.filter(
					(notification) => notification.context !== NOTIFICATION_CONTEXT_ACCESS
				),
			};
		}
		case SET_HIDDEN: {
			const { id: idToUpdate, isHidden } = payload;
			return {
				...state,
				notifications: state.notifications.map((notification) => {
					if (notification.id === idToUpdate) {
						return {
							...notification,
							hidden: isHidden,
						};
					}
					return notification;
				}),
			};
		}
		default:
			return state;
	}
};
