import {initArray} from "../../utils/utils";

// All fields for new event form that are fix and not depending on response of backend
// InitialValues of Formik form (others computed dynamically depending on responses from backend)
export const initialFormValuesNewEvents = {
    sourceMode: 'UPLOAD',
    scheduleStartDate: new Date().toISOString(),
    scheduleEndDate: new Date().toISOString(),
    scheduleStartTimeHour: '',
    scheduleStartTimeMinutes: '',
    scheduleDurationHour: '',
    scheduleDurationMinutes: '',
    scheduleEndTimeHour: '',
    scheduleEndTimeMinutes: '',
    repeatOn: [],
    location: '',
    deviceInputs: [],
    processingWorkflow: '',
    policies: [{
        role: 'ROLE_USER_ADMIN',
        read: true,
        write: true,
        actions: []
    }]
};

// constants for hours and minutes (used in selection for start/end time and duration)
export const hours = initArray(24);
export const minutes = initArray(60);

// sorted weekdays and their translation key
export const weekdays = [
    {
        name: 'MO',
        label: 'EVENTS.EVENTS.NEW.WEEKDAYS.MO'
    },
    {
        name: 'TU',
        label: 'EVENTS.EVENTS.NEW.WEEKDAYS.TU'
    },
    {
        name: 'WE',
        label: 'EVENTS.EVENTS.NEW.WEEKDAYS.WE'
    },
    {
        name: 'TH',
        label: 'EVENTS.EVENTS.NEW.WEEKDAYS.TH'
    },
    {
        name: 'FR',
        label: 'EVENTS.EVENTS.NEW.WEEKDAYS.FR'
    },
    {
        name: 'SA',
        label: 'EVENTS.EVENTS.NEW.WEEKDAYS.SA'
    },
    {
        name: 'SU',
        label: 'EVENTS.EVENTS.NEW.WEEKDAYS.SU'
    }
];

// Context for notifications shown in events-form
export const NOTIFICATION_CONTEXT = 'events-form';

// Context for notifications shown in access page
export const NOTIFICATION_CONTEXT_ACCESS = 'events-access';
