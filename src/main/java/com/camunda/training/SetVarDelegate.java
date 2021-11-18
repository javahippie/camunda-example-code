package com.camunda.training;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.TypedValue;

public class SetVarDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        delegateExecution.setVariable("globalVar", "Lives in the process");
        delegateExecution.setVariable("scopedVar", "Only in the Sub Process", "Activity_Subprocess");
        delegateExecution.setVariableLocal("localVar", "This is local!");

        TypedValue transientVar = Variables.stringValue("This is short-lived", true);
        delegateExecution.setVariable("transientVar", transientVar);
    }

}
