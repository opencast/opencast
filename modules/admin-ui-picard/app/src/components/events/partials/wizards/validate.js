import * as Yup from 'yup';

const today = new Date();
today.setHours(0, 0, 0, 0);

export const NewEventSchema = [Yup.object().shape({
    title: Yup.string().required('Required')
}), Yup.object().shape({
    sourceMode: Yup.string(),
    scheduleStartDate: Yup.date().when('sourceMode', {
        is: 'SCHEDULE_SINGLE' || 'SCHEDULE_MULTIPLE',
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
    scheduleStartTimeHour: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleStartTimeMinutes: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleDurationHour: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleDurationMinutes: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleEndTimeHour: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    scheduleEndTimeMinutes: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    }),
    location: Yup.string().when('sourceMode', {
        is: value => value === 'SCHEDULE_SINGLE' || value === 'SCHEDULE_MULTIPLE',
        then: Yup.string().required('Required')
    })
})]
