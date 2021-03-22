// All fields for new theme form that are fix and not depending on response of backend
// InitialValues of Formik form (others computed dynamically depending on responses from backend)
export const initialFormValuesNewThemes = {
    name: '',
    description: '',
    bumperActive: false,
    bumperFile: {},
    trailerActive: false,
    trailerFile: {},
    titleSlideActive: false,
    titleSlideMode: 'extract',
    titleSlideBackground: {},
    licenseSlideActive: false,
    watermarkActive: false,
    watermarkFile: {},
    watermarkPosition: 'topRight'
};

// Context for notifications shown in themes wizard
export const NOTIFICATION_CONTEXT = 'wizard-form';
