import * as Yup from 'yup';

export const NewEventSchema = [Yup.object().shape({
    title: Yup.string().required("Required")
}), Yup.object().shape({
    bla: Yup.string().required("Required")
})]
