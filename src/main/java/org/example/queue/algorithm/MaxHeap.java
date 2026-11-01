package org.example.queue.algorithm;

import lombok.Data;
import org.example.queue.entity.Patient;

import java.util.concurrent.CyclicBarrier;

@Data
public class MaxHeap {
    private Patient[] maxHeap;
    public MaxHeap(CycleQueue cycleQueue) {
        change(cycleQueue);
        buildMaxHeap();
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
    public void buildMaxHeap(){
        int n = maxHeap.length;
        for(int i=(n-2)/2;i>=0;i--) {
            shiftDown(n,i);
        }
    }
    //把大的放上去
    private void shiftDown( int n, int i) {
        while (true){
            int leftChild = 2*i+1;
            int rightChild = 2*i+2;
            if(leftChild>n-1) break;
            int large = i;
            if(maxHeap[leftChild].getWaitTime()>maxHeap[large].getWaitTime()){
                large = leftChild;
            }
            if(maxHeap[rightChild].getWaitTime()>maxHeap[large].getWaitTime()&&rightChild<n){
                large = rightChild;
            }
            if(large == i) {
                break;
            }else {
                swap(large,i);
                i = large;
            }

        }
    }
    //交换
    private void swap( int i, int j) {
        Patient temp = maxHeap[i];
        maxHeap[i] = maxHeap[j];
        maxHeap[j] = temp;
    }
    //堆排序，升序
    public void heapSort(){
        for(int i=0;i<maxHeap.length-1;i++){
            swap(0,maxHeap.length-i-1);
            shiftDown(maxHeap.length-i-1,0);
        }
    }

}
