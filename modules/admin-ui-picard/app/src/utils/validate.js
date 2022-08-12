import * as Yup from 'yup';

/**
 * This File contains all schemas used for validation with yup in the context of events and series
 */

const today = new Date();
today.setHours(0, 0, 0, 0);


// Validation Schema used in new event wizard (each step has its own yup validation object)
export const NewEventSchema = [
    Yup.object().shape({
        title: Yup.string().required('Required')
    }),
    Yup.object().shape({
    }),
    Yup.object().shape({
    uploadAssetsTrack: Yup.array().when('sourceMode', {
        is:  value => value === 'UPLOAD',
        then: Yup.array().test('at-least-one-uploaded', 'at least one uploaded', uploadAssetsTrack => {
            return uploadAssetsTrack.some(asset => !!asset.file)
        })
    }),
    scheduleStartDate: Yup.date().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.date().required('Required')
    }),
    scheduleEndDate: Yup.date().when('sourceMode', {
        is: 'SCHEDULE_MULTIPLE',
        then: Yup.date().required('Required')
    }),
    repeatOn: Yup.array().when('sourceMode', {
        is: 'SCHEDULE_MULTIPLE',
        then: Yup.array().min(1).required('Required')
    }),
    scheduleStartHour: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleStartMinute: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleDurationHours: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleDurationMinutes: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleEndHour: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleEndMinute: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    location: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    })
}), Yup.object().shape({
    processingWorkflow: Yup.string().required('Required')
})];

// Validation Schema used in new series wizard (each step has its own yup validation object)
export const NewSeriesSchema = [Yup.object().shape({
    title: Yup.string().required('Required')
})];

// Validation Schema used in new themes wizard (each step has its own yup validation object)
export const NewThemeSchema = [
    Yup.object().shape({
        name: Yup.string().required('Required')
    }), Yup.object().shape({
        bumperFile: Yup.string().when('bumperActive', {
            is: true,
            then: Yup.string().required('Required')
        })
    }), Yup.object().shape({
        trailerFile: Yup.string().when('trailerActive', {
            is: true,
            then: Yup.string().required('Required')
        })
    }), Yup.object().shape({
        titleSlideBackground: Yup.string().when('titleSlideMode', {
            is: 'upload',
            then: Yup.string().required('Required')
        })
    }), Yup.object().shape({
        watermarkFile: Yup.string().when('watermarkActive', {
            is: true,
            then: Yup.string().required('Required')
        })
    })
];

// Validation Schema used in new ACL wizard (each step has its own yup validation object)
export const NewAclSchema = [
    Yup.object().shape({
        name: Yup.string().required('Required')
    })
];

// Validation Schema used in new groups wizard (each step has its own yup validation object)
export const NewGroupSchema = [
    Yup.object().shape({
        name: Yup.string().required('Required')
    })
];

// Validation Schema used in new user wizard
export const NewUserSchema = usernames => (
    Yup.object().shape({
        username: Yup.string().required('Required').notOneOf(usernames, 'not unique'),
        name: Yup.string().required('Required'),
        email: Yup.string().email().required('Required'),
        password: Yup.string().required('Required'),
        passwordConfirmation: Yup.string()
            .oneOf([Yup.ref('password'), null], 'Passwords must match').required('Required')
}));

// Validation Schema used in user details modal
export const EditUserSchema = Yup.object().shape({
    name: Yup.string().required('Required'),
    email: Yup.string().email().required('Required'),
    passwordConfirmation: Yup.string().when('password', {
        is: value => !!value,
        then: Yup.string()
            .oneOf([Yup.ref('password'), null], 'Passwords must match').required('Required')
    })
});

// Validation Schema used in group details modal
export const EditGroupSchema = Yup.object().shape({
    name: Yup.string().required('Required')
});

// Validation Schema used in adopter registration modal
export const AdopterRegistrationSchema = Yup.object().shape({
    email: Yup.string().email()
});
