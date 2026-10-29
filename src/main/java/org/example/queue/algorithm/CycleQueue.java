package org.example.queue.algorithm;

import lombok.Data;
import org.example.queue.entity.Patient;

import java.util.Arrays;
import java.util.List;

@Data
public class CycleQueue {
    private Patient[] patients;
    private int front;//队头
    private int rear;//队尾
    private int maxLength;//数组长度
    private int length;//目前的数组里有多少个元素
    //构建
    public CycleQueue(int length) {
        patients = new Patient[length+10];
        front = 0;
        rear = 0;
        maxLength = length+10;
        this.length = 0;
    }
    //空否
    public boolean isEmpty() {
        if(rear==front) return true;
        return false;
    }
    //满否
    public boolean isFull(){
        if((rear+1)%maxLength==front) return true;
        return false;
    }
    //入队
//    public boolean enQueue(Patient patient){
//        if(isFull()) return false;
//        patients[rear] = patient;
//        rear = (rear+1)%maxLength;
//        length++;
//        return true;
//    }
    //出队
    public Patient deQueue(){
        if(isEmpty()) return null;
        Patient result = patients[front];
        front = (front+1)%maxLength;
        length--;
        return result;
    }
    //按序入队
    public boolean insertPriority(Patient patient){
        if(isFull()) return false;
        int priority = patient.getPriority();
        int flag = (rear-1)%maxLength;
        int num = 0;
        length++;
        rear = (rear+1)%maxLength;
        for(;num<length;num++) {
            if(patients[flag].getPriority()>priority) {
                patients[(flag+1)%length] = patients[flag];
                flag = (flag-1)%maxLength;
            } else {
                break;
            }
        }
        patients[(flag+1)%maxLength] = patient;
        return true;
    }
    //拿所有患者的信息（按序）
    public List<Patient> getAll(){
        List<Patient> list = Arrays.asList(patients);
        List<Patient> result;
        if(front<rear)
            result = list.subList(front,rear);
        else {
            result = list.subList(front,maxLength);
            List<Patient> tmp = list.subList(0,rear);
            result.addAll(tmp);
        }
        return result;
    }
    //清空队列
    public void clear(){
        front = rear = 0;
        length = 0;
    }

}
