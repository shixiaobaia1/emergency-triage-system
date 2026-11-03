package org.example.queue.algorithm;

import lombok.Data;
import org.example.queue.entity.Patient;

import java.util.ArrayList;
import java.util.List;

// FIX: 加回 Lombok @Data，自动生成 getter/setter，之前丢失导致手写了一堆
@Data
public class CycleQueue {
    private Patient[] patients;
    private int front;//队头
    private int rear;//队尾
    private int maxLength;//数组长度
    private int length;//目前的数组里有多少个元素
    //构建：FIX: 牺牲一个存储单元来区分空满，所以数组开length+1而不是length+10
    public CycleQueue(int length) {
        patients = new Patient[length + 1];  // FIX: +1，为了用rear==front判空、(rear+1)%N==front判满
        front = 0;
        rear = 0;
        maxLength = length + 1;
        this.length = 0;
    }
    //空否
    public boolean isEmpty() {
        return rear == front;
    }
    //满否
    public boolean isFull(){
        return (rear + 1) % maxLength == front;
    }
    // FIX: 恢复基础的按到达顺序入队方法（第一阶段核心）
    public boolean enQueue(Patient patient){
        if(isFull()) return false;
        patients[rear] = patient;
        rear = (rear + 1) % maxLength;
        length++;
        return true;
    }
    //出队
    public Patient deQueue(){
        if(isEmpty()) return null;
        Patient result = patients[front];
        front = (front + 1) % maxLength;
        length--;
        return result;
    }
    //按优先级插入入队（优先级越小越紧急，插前面）
    public boolean insertPriority(Patient patient){
        if(isFull()) return false;
        int priority = patient.getPriority();
        // FIX: Java负数取模还是负数，必须加maxLength再模
        int flag = (rear - 1 + maxLength) % maxLength;
        int num = 0;
        length++;
        rear = (rear + 1) % maxLength;
        for(; num < length; num++) {
            if(patients[flag].getPriority() > priority) {
                // FIX: 用maxLength取模，不是length（元素个数）；同时注意负数取模安全
                patients[(flag + 1) % maxLength] = patients[flag];
                flag = (flag - 1 + maxLength) % maxLength;  // FIX: 负数取模安全
            } else {
                break;
            }
        }
        patients[(flag + 1) % maxLength] = patient;  // FIX: 用maxLength取模
        return true;
    }
    //拿所有患者的信息（按序）
    public List<Patient> getAll(){
        List<Patient> result = new ArrayList<>();
        int i = front;
        while(i != rear){
            result.add(patients[i]);
            i = (i + 1) % maxLength;
        }
        return result;
    }
    //清空队列
    public void clear(){
        front = rear = 0;
        length = 0;
    }

    //去除中间某一个患者（放弃排队）
    public void remove(int id){
        int i = front;
        boolean flag = false;
        while(i != rear){
            if(patients[i].getId() == id){
                i = (i + 1) % maxLength;
                while(i != rear){
                    // FIX: 负数取模安全
                    patients[(i - 1 + maxLength) % maxLength] = patients[i];
                    i = (i + 1) % maxLength;
                    flag = true;
                }
                break;  // FIX: 找到就退出外层
            }
            i = (i + 1) % maxLength;
        }
        // FIX: 删除后length必须减1
        if(flag) length--;
        if(flag) rear = (rear - 1 + maxLength) % maxLength;  // FIX: rear也要回退
    }
    // FIX: @Data 已自动生成 patients/front/rear/maxLength/length 的 getter/setter，不再手写
}
