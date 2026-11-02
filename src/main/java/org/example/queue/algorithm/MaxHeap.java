package org.example.queue.algorithm;

import org.example.queue.entity.Patient;

public class MaxHeap {
    private Patient[] maxHeap;
    public MaxHeap(CycleQueue cycleQueue) {
        change(cycleQueue);
        buildMaxHeap();
    }
    //令循环队列变成正常的数组（去掉环形结构，展开成线性）
    public void change(CycleQueue cycleQueue) {
        maxHeap = new Patient[cycleQueue.getLength()];
        int i = 0;
        int front = cycleQueue.getFront();
        int rear = cycleQueue.getRear();
        // FIX: 原循环条件错误，且缺少 i++。改用 front != rear 正确遍历
        while (front != rear) {
            maxHeap[i] = cycleQueue.getPatients()[front];
            front = (front + 1) % cycleQueue.getMaxLength();
            i++;  // FIX: 必须递增数组下标
        }
    }
    //创建大根堆
    public void buildMaxHeap(){
        int n = maxHeap.length;
        for(int i = (n - 2) / 2; i >= 0; i--) {
            shiftDown(n, i);
        }
    }
    //把大的放上去
    private void shiftDown(int n, int i) {
        while (true){
            int leftChild = 2 * i + 1;
            int rightChild = 2 * i + 2;
            if(leftChild > n - 1) break;
            int large = i;
            if(maxHeap[leftChild].getWaitTime() > maxHeap[large].getWaitTime()){
                large = leftChild;
            }
            // FIX: 原代码先访问 maxHeap[rightChild] 再判断越界，顺序改了——先判断再访问
            if(rightChild < n && maxHeap[rightChild].getWaitTime() > maxHeap[large].getWaitTime()){
                large = rightChild;
            }
            if(large == i) {
                break;
            } else {
                swap(large, i);
                i = large;
            }
        }
    }
    //交换
    private void swap(int i, int j) {
        Patient temp = maxHeap[i];
        maxHeap[i] = maxHeap[j];
        maxHeap[j] = temp;
    }
    //堆排序，升序（用于第三阶段折半查找的有序数组）
    public void heapSort(){
        for(int i = 0; i < maxHeap.length - 1; i++){
            swap(0, maxHeap.length - i - 1);
            shiftDown(maxHeap.length - i - 1, 0);
        }
    }

    public Patient[] getMaxHeap() {
        return maxHeap;
    }

    public void setMaxHeap(Patient[] maxHeap) {
        this.maxHeap = maxHeap;
    }
}
