package org.example.queue.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Patient {
    private int id;
    private String name;
    private LocalTime arriveTime;
    private int proveTime;
    private int priority;
    private int waitTime;
}
