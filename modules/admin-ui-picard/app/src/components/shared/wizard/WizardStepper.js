import React from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import {makeStyles, Step, StepLabel, Stepper} from "@material-ui/core";
import {FaCircle, FaDotCircle} from "react-icons/all";

// Base style for Stepper component
const useStepperStyle = makeStyles(theme => ({
    root: {
        background: '#eeeff0',
        height: '100px'
    },
}));

// Style of icons used in Stepper
const useStepIconStyles = makeStyles({
    root: {
        height: 22,
        alignItems: 'center',
    },
    circle: {
        color: '#92a0ab',
        width: '20px',
        height: '20px'
    },
});

const WizardStepper = ({ steps, page }) => {
    const { t } = useTranslation();

    const classes = useStepperStyle();

    return (
        <Stepper activeStep={page}
                 alternativeLabel
                 connector={false}
                 className={cn("step-by-step", classes.root)}>
            {steps.map(label => (
                !label.hidden ? (
                    <Step key={label.translation}>
                        <StepLabel StepIconComponent={CustomStepIcon}>{t(label.translation)}</StepLabel>
                    </Step>
                ) : null
            ))}
        </Stepper>
    );
};

// Component that renders icons of Stepper depending on completeness of steps
const CustomStepIcon = (props) => {
    const classes = useStepIconStyles();
    const { completed } = props;

    return (
        <div className={cn(classes.root)}>
            {completed ? <FaCircle className={classes.circle}/> : <FaDotCircle className={classes.circle}/>}
        </div>
    )
};

export default WizardStepper;
