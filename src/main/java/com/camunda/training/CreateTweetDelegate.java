package com.camunda.training;


import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("createTweetDelegate")
public class CreateTweetDelegate implements JavaDelegate {

    private final Logger LOGGER = LoggerFactory.getLogger(CreateTweetDelegate.class.getName());

    private final TwitterService twitterService;

    @Autowired
    public CreateTweetDelegate(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        String content = (String) execution.getVariable(ProcessInstanceConstants.VAR_CONTENT);
        LOGGER.info("Publishing tweet: " + content);

        twitterService.sendTweet(content);


    }
}
