// Check if an event is scheduled and therefore editable
import { hasDeviceAccess } from "./resourceUtils";
import { NOTIFICATION_CONTEXT } from "../configs/modalConfig";

// Check if event is scheduled and therefore the schedule is editable
export const isScheduleEditable = (event) => {
	return (
		event.event_status.toUpperCase().indexOf("SCHEDULED") > -1 ||
		!event.selected
	);
};

// Check if multiple events are scheduled and therefore the schedule is editable
export const isAllScheduleEditable = (events) => {
	for (let i = 0; i < events.length; i++) {
		if (!isScheduleEditable(events[i])) {
			return false;
		}
	}
	return true;
};

// Check if user has access rights for capture agent of event
export const isAgentAccess = (event, user) => {
	return !event.selected || hasDeviceAccess(user, event.agent_id);
};

// Check if user has access rights for capture agent of several events
export const isAllAgentAccess = (events, user) => {
	for (let i = 0; i < events.length; i++) {
		if (!events[i].selected || !isScheduleEditable(events[i])) {
			continue;
		}
		if (!isAgentAccess(events[i], user)) {
			return false;
		}
	}
	return true;
};

// Check validity of selected event list for bulk schedule update for activating next button
export const checkValidityUpdateScheduleEventSelection = (
	formikValues,
	user
) => {
	if (formikValues.events.length > 0) {
		if (
			isAllScheduleEditable(formikValues.events) &&
			isAllAgentAccess(formikValues.events, user)
		) {
			return formikValues.events.some((event) => event.selected === true);
		} else {
			return false;
		}
	} else {
		return false;
	}
};

// check changed events in formik for scheduling conflicts
export const checkSchedulingConflicts = async (
	formikValues,
	setConflicts,
	checkConflicts,
	addNotification
) => {
	// Check if each start is before end
	for (let i = 0; i < formikValues.editedEvents.length; i++) {
		let event = formikValues.editedEvents[i];
		let startTime = new Date();
		startTime.setHours(
			event.changedStartTimeHour,
			event.changedStartTimeMinutes,
			0,
			0
		);
		let endTime = new Date();
		endTime.setHours(
			event.changedEndTimeHour,
			event.changedEndTimeMinutes,
			0,
			0
		);

		if (startTime > endTime) {
			addNotification(
				"error",
				"CONFLICT_END_BEFORE_START",
				-1,
				null,
				NOTIFICATION_CONTEXT
			);
			return false;
		}
	}

	// Use backend for check for conflicts with other events
	const response = await checkConflicts(formikValues.editedEvents);

	if (response.length > 0) {
		setConflicts(response);
		return false;
	} else {
		setConflicts([]);
		return true;
	}
};

// Check if an event is in a state that a task on it can be started
export const isStartable = (event) => {
	return (
		event.event_status.toUpperCase().indexOf("PROCESSED") > -1 ||
		event.event_status.toUpperCase().indexOf("PROCESSING_FAILURE") > -1 ||
		event.event_status.toUpperCase().indexOf("PROCESSING_CANCELED") > -1 ||
		!event.selected
	);
};

// Check if multiple events are in a state that a task on them can be started
export const isTaskStartable = (events) => {
	for (let i = 0; i < events.length; i++) {
		if (!isStartable(events[i])) {
			return false;
		}
	}
	return true;
};

// Check validity of selected event list for bulk start task for activating next button
export const checkValidityStartTaskEventSelection = (formikValues) => {
	if (formikValues.events.length > 0) {
		if (isTaskStartable(formikValues.events)) {
			return formikValues.events.some((event) => event.selected === true);
		} else {
			return false;
		}
	} else {
		return false;
	}
};
