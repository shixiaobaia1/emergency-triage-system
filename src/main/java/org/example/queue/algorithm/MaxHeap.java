package org.example.queue.algorithm;

import org.example.queue.entity.Patient;

/**
 * 大根堆——第二阶段核心
 * 堆比较的依据是 Patient.waitTime（等待时间），等待时间大的堆顶
 * 流程：CycleQueue 数组 → change() 拍平成普通数组 → buildMaxHeap() 建堆 → heapSort() 升序排列
 */
public class MaxHeap {
    private Patient[] maxHeap;  // 堆的底层数组（按 waitTime 建大根堆）

    public MaxHeap(CycleQueue cycleQueue) {
        change(cycleQueue);   // 把循环队列拍平成普通数组
        buildMaxHeap();       // 建大根堆
    }

    /**
     * 把循环队列拍平成普通数组（循环队列的 front 不一定是 0，要按顺序一个个搬）
     * FIX: change() 原实现少了 i++ 会死循环，已修复
     */
    public void change(CycleQueue cycleQueue) {
        maxHeap = new Patient[cycleQueue.getLength()];
        int i = 0;
        int front = cycleQueue.getFront();
        int rear = cycleQueue.getRear();
        while (front != rear) {
            maxHeap[i] = cycleQueue.getPatients()[front];
            front = (front + 1) % cycleQueue.getMaxLength();
            i++;  // FIX: 之前漏了 i++，会死循环
        }
    }

    /**
     * 建大根堆——从最后一个非叶子节点往前，依次做向下调整
     */
    public void buildMaxHeap(){
        int n = maxHeap.length;
        for(int i = (n - 2) / 2; i >= 0; i--) {
            shiftDown(n, i);
        }
    }

    /**
     * 向下调整：把当前节点和左右子节点中最大的那个交换，直到子节点都比自己小
     * FIX: 原实现先访问 rightChild 再判断越界，顺序反了，已修复
     */
    private void shiftDown(int n, int i) {
        while (true){
            int leftChild = 2 * i + 1;
            int rightChild = 2 * i + 2;
            if(leftChild > n - 1) break;
            int large = i;
            if(maxHeap[leftChild].getWaitTime() > maxHeap[large].getWaitTime()){
                large = leftChild;
            }
            // FIX: 先判断 rightChild < n 短路，再访问数组，防止越界
            if(rightChild < n && maxHeap[rightChild].getWaitTime() > maxHeap[large].getWaitTime()){
                large = rightChild;
            }
            if(large == i) {
                break;  // 子节点都比自己小，堆已经满足性质
            } else {
                swap(large, i);
                i = large;  // 交换后 large 位置继续往下调
            }
        }
    }

    /** 交换堆里的两个元素 */
    private void swap(int i, int j) {
        Patient temp = maxHeap[i];
        maxHeap[i] = maxHeap[j];
        maxHeap[j] = temp;
    }

    /**
     * 堆排序：把堆顶（最大的）交换到末尾，缩小范围继续堆化
     * 最终结果是 waitTime 升序（适合第三阶段折半查找）
     */
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
