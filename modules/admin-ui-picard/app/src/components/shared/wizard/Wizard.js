import React, {useState} from 'react';

const Wizard = ({ children, initialValues, onSubmit }) => {
    const [stepNumber, setStepNumber] = useState(0);
    const steps = React.Children.toArray(children);
    const [snapshot, setSnapshot] = useState(initialValues);

    const step = steps[stepNumber];
    const totalSteps = step.length;
    const isLastStep = stepNumber === totalSteps - 1;

    const next = values => {
        setSnapshot(values);
        setStepNumber(Math.min(stepNumber + 1, totalSteps - 1));
    }

    const previous = values => {
        setSnapshot(values);
        setStepNumber((Math.max(stepNumber - 1, 0)))
    }
}
