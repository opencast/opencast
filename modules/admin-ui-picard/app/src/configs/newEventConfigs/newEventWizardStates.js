import {initArray} from "../../utils/utils";

export const newEventWizardStates = [
    {
        translation: 'EVENTS.EVENTS.NEW.METADATA.CAPTION',
        name: 'metadata'
    },
    {
        translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
        name: 'metadata-extended'
    },
    {
        translation: 'EVENTS.EVENTS.NEW.SOURCE.CAPTION',
        name: 'source'
    },
    {
        translation: 'EVENTS.EVENTS.NEW.UPLOAD_ASSET.CAPTION',
        name: 'upload-asset'
    },
    {
        translation: 'EVENTS.EVENTS.NEW.PROCESSING.CAPTION',
        name: 'processing'
    },
    {
        translation: 'EVENTS.EVENTS.NEW.ACCESS.CAPTION',
        name: 'access'
    },
    {
        translation: 'EVENTS.EVENTS.NEW.SUMMARY.CAPTION',
        name: 'summary'
    }
];

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
    deviceInputs: []
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
