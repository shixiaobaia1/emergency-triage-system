package org.example.queue.algorithm;

import lombok.Data;
import org.example.queue.entity.Patient;

import java.util.concurrent.CyclicBarrier;

@Data
public class MaxHeap {
    private Patient[] maxHeap;
    public MaxHeap(CycleQueue cycleQueue) {
        change(cycleQueue);
        buildMaxHeap(maxHeap);
    }
    //令循环队列变成正常的队列
    public void change(CycleQueue cycleQueue) {
        maxHeap = new Patient[cycleQueue.getLength()];
        int i = 0;
        int front = cycleQueue.getFront();
        int rear = cycleQueue.getRear();
        while ((front+1)%cycleQueue.getMaxLength()<rear) {
            maxHeap[i] = cycleQueue.getPatients()[front];
            front = (front+1)%cycleQueue.getMaxLength();
        }
    }
    //创建大根堆
    public void buildMaxHeap(Patient[] maxHeap){
        int n = maxHeap.length;
        for(int i=(n-2)/2;i>=0;i--) {
            shiftDown(maxHeap,n,i);
        }
    }
    //把大的放上去
    private void shiftDown(Patient[] patients, int n, int i) {
        while (true){
            int leftChild = 2*i+1;
            int rightChild = 2*i+2;
            if(leftChild>n-1) break;
            int large = i;
            if(patients[leftChild].getWaitTime()>patients[large].getWaitTime()){
                large = leftChild;
            }
            if(patients[rightChild].getWaitTime()>patients[large].getWaitTime()&&rightChild<n){
                large = rightChild;
            }
            if(large == i) {
                break;
            }else {
                swap(patients,large,i);
                i = large;
            }

        }
    }
    //交换
    private void swap(Patient[] patients, int i, int j) {
        Patient temp = patients[i];
        patients[i] = patients[j];
        patients[j] = temp;
    }
    //堆排序，升序
    public void heapSort(Patient[] patients){
        for(int i=0;i<patients.length-1;i++){
            swap(patients,0,patients.length-i-1);
            shiftDown(patients,patients.length-i-1,0);
        }
    }

}
