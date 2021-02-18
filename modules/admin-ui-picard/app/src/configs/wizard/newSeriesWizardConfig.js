// All fields for new series form that are fix and not depending on response of backend
// InitialValues of Formik form (others computed dynamically depending on responses from backend)
export const initialFormValuesNewSeries = {
    policies: [{
        role: 'ROLE_USER_ADMIN',
        read: true,
        write: true,
        actions: []
    }],
    theme: ''
};
