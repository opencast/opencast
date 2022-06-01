import React from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import { Step, StepButton, StepLabel, Stepper } from '@material-ui/core';
import { useStepperStyle } from '../../../utils/wizardUtils';
import CustomStepIcon from './CustomStepIcon';
import { checkAcls } from '../../../thunks/aclThunks';
import { connect } from 'react-redux';
import { checkConflicts } from '../../../thunks/eventThunks';


const WizardStepperEvent = ({ steps, page, setPage, formik, checkAcls, checkConflicts }) => {
  const { t } = useTranslation();

  const classes = useStepperStyle();

  const handleOnClick = async key => {
    if (steps[page].name === "source") {
      let dateCheck = await checkConflicts(formik.values);
      if (!dateCheck) {
        return;
      }
    }

    if (steps[page].name === "processing" && !formik.values.processingWorkflow) {
      return;
    }

    let aclCheck =  await checkAcls(formik.values.acls);
    if (!aclCheck) {
      return;
    }

    if (formik.isValid) {
      setPage(key);
    }
  }

  const disabled = !(formik.dirty && formik.isValid);

  return (
    <Stepper activeStep={page}
             nonLinear
             alternativeLabel
             connector={false}
             className={cn("step-by-step", classes.root)}>
      {steps.map((label, key) => (
        !label.hidden ? (
          <Step key={label.translation}>
            <StepButton onClick={() => handleOnClick(key)} disabled={disabled}>
              <StepLabel StepIconComponent={CustomStepIcon}>{t(label.translation)}</StepLabel>
            </StepButton>
          </Step>
        ) : null
      ))}
    </Stepper>
  );
};

const mapDispatchToProps = dispatch => ({
  checkAcls: acls => dispatch(checkAcls(acls)),
  checkConflicts: values => dispatch(checkConflicts(values))
});

export default connect(null, mapDispatchToProps)(WizardStepperEvent);
