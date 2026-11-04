package org.example.queue.algorithm;

import org.example.queue.entity.Patient;

import java.util.ArrayList;
import java.util.List;

public class CycleQueue {
    private Patient[] patients;
    private int front;//队头
    private int rear;//队尾
    private int maxLength;//数组长度
    private int length;//目前的数组里有多少个元素

    public CycleQueue(int length) {
        patients = new Patient[length + 1];
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
    //正常入队
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
    //按优先级入队（优先级越小越紧急，插前面）
    public boolean insertPriority(Patient patient){
        if(isFull()) return false;
        int priority = patient.getPriority();

        // 先确定队尾位置
        int newRear = (rear + 1) % maxLength;

        //从 rear-1 开始往前找插入点，同时把优先级更大的元素往后搬
        //最多搬 length 次，不会越过已填充区域
        int i = (rear - 1 + maxLength) % maxLength;
        int moved = 0;
        while(moved < length) {
            //能搬的往后搬
            if(patients[i] != null && patients[i].getPriority() > priority) {
                patients[(i + 1) % maxLength] = patients[i];
                i = (i - 1 + maxLength) % maxLength;
                moved++;
            } else {
                break;  //到不该搬的就结束
            }
        }

        //插入点在最后搬完的位置的后一格
        int insertPos = (i + 1) % maxLength;
        patients[insertPos] = patient;
        rear = newRear;
        length++;
        return true;
    }
    //按序拿所有患者的信息
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
                    //后面的推到前面去
                    patients[(i - 1 + maxLength) % maxLength] = patients[i];
                    i = (i + 1) % maxLength;
                    flag = true;
                }
                break;
            }
            i = (i + 1) % maxLength;
        }
        //改length和rear
        if(flag) length--;
        if(flag) rear = (rear - 1 + maxLength) % maxLength;
    }

    public Patient[] getPatients() {
        return patients;
    }

    public void setPatients(Patient[] patients) {
        this.patients = patients;
    }

    public int getFront() {
        return front;
    }

    public void setFront(int front) {
        this.front = front;
    }

    public int getRear() {
        return rear;
    }

    public void setRear(int rear) {
        this.rear = rear;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
