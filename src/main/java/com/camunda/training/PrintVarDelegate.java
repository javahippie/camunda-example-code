package com.camunda.training;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class PrintVarDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println("PRINTING ALL THE VARS!!!!!");
        execution.getVariables().forEach((s, o) ->
                System.out.println(String.format("%s -> %s", s, o)));
    }

}
