package org.example.queue.entity;

import lombok.Data;

import java.util.List;

@Data
public class QueueMessage {
    private int front;
    private int rear;
    private double avgTime;
    private List<Patient> patientList;
}
