package com.healthlens.api.events;

import java.util.Map;

public interface ApplicationStreamPublisher {

    void publish(String streamName, Map<String, String> payload);

    void publishAfterCommit(String streamName, Map<String, String> payload);
}
