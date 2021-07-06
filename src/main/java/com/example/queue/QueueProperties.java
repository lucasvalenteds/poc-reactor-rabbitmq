package com.example.queue;

public final class QueueProperties {

    private final String appId;
    private final Integer deliveryMode;

    private final String queue;
    private final String exchange;
    private final String routingKey;

    public QueueProperties(String appId, Integer deliveryMode, String queue, String exchange, String routingKey) {
        this.appId = appId;
        this.deliveryMode = deliveryMode;
        this.queue = queue;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public String getAppId() {
        return appId;
    }

    public Integer getDeliveryMode() {
        return deliveryMode;
    }

    public String getQueue() {
        return queue;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
