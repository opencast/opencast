import { createSelector } from "reselect";

export const getNotifications = (state) => state.notifications.notifications;
export const getGlobalPositions = (state) =>
	state.notifications.notificationPositionGlobal;

export const getNotificationById = (id) =>
	createSelector(getNotifications, (notifications) =>
		notifications.filter((notification) => notification.id === id)
	);

export const getLastAddedNotification = createSelector(
	getNotifications,
	(notifications) =>
		notifications.reduce((prev, current) =>
			prev.id > current.id ? prev : current
		)
);
