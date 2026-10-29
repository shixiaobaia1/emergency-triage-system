package org.example.queue.entity;

import lombok.Data;

@Data
public class QueueMessage {
    private int front;
    private int rear;
    private double avgTime;
}
