/**
 * This file contains all redux actions that can be executed on the notifications
 */

// Constants regarding creation and deletion of notifications
export const CREATE_NOTIFICATION = "CREATE_NOTIFICATION";
export const REMOVE_NOTIFICATION = "REMOVE_NOTIFICATION";
export const REMOVE_NOTIFICATION_WIZARD_FORM =
	"REMOVE_NOTIFICATION_WIZARD_FORM";
export const REMOVE_NOTIFICATION_WIZARD_ACCESS =
	"REMOVE_NOTIFICATION_WIZARD_ACCESS";

// Constants regarding updates of notification
export const SET_HIDDEN = "SET_HIDDEN";

// Actions affecting creation and deletion of notification

// Counter for id of notifications
let nextNotificationId = 0;
export const createNotification = (notification) => ({
	type: CREATE_NOTIFICATION,
	payload: { notification, id: nextNotificationId++ },
});

export const removeNotification = (id) => ({
	type: REMOVE_NOTIFICATION,
	payload: { id },
});

export const removeNotificationWizardForm = () => ({
	type: REMOVE_NOTIFICATION_WIZARD_FORM,
});

export const removeNotificationWizardAccess = () => ({
	type: REMOVE_NOTIFICATION_WIZARD_ACCESS,
});

// Actions affecting updates of notifications

export const setHidden = (id, isHidden) => ({
	type: SET_HIDDEN,
	payload: { id, isHidden },
});
