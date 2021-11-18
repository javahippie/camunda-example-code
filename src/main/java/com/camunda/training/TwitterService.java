package com.camunda.training;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.springframework.stereotype.Service;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import java.net.ConnectException;

@Service
public class TwitterService {

    public void sendTweet(String content) throws ConnectException, TwitterException {
        if(content.equals("network error")) {
            throw new ConnectException("Twitter is down!");
        }

        AccessToken accessToken = new AccessToken("220324559-jet1dkzhSOeDWdaclI48z5txJRFLCnLOK45qStvo", "B28Ze8VDucBdiE38aVQqTxOyPc7eHunxBVv7XgGim4say");
        Twitter twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer("lRhS80iIXXQtm6LM03awjvrvk", "gabtxwW8lnSL9yQUNdzAfgBOgIMSRqh7MegQs79GlKVWF36qLS");
        twitter.setOAuthAccessToken(accessToken);

        try {
            twitter.updateStatus(content);
        } catch (TwitterException e) {
            if(e.getErrorCode() == 187) {
                throw new BpmnError("TWT_DUPLICATE", e.getErrorMessage(), e);
            } else {
                throw e;
            }
        }
    }
}
