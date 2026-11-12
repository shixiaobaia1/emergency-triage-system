package org.example.queue.algorithm;

import org.example.queue.entity.Patient;

public class MaxHeap {
    private Patient[] maxHeap;  // 堆的底层数组

    public MaxHeap(CycleQueue cycleQueue) {
        change(cycleQueue);   // 把循环队列变成普通数组
        buildMaxHeap();       // 建大根堆
    }

//从循环数组到正常数组
    public void change(CycleQueue cycleQueue) {
        maxHeap = new Patient[cycleQueue.getLength()];
        int i = 0;
        //从front开始到rear结束
        int front = cycleQueue.getFront();
        int rear = cycleQueue.getRear();
        while (front != rear) {
            maxHeap[i] = cycleQueue.getPatients()[front];
            front = (front + 1) % cycleQueue.getMaxLength();
            i++;
        }
    }

    //除了叶子节点都要往下找
    public void buildMaxHeap(){
        int n = maxHeap.length;
        for(int i = (n - 2) / 2; i >= 0; i--) {
            shiftDown(n, i);
        }
    }


    private void shiftDown(int n, int i) {
        while (true){
            //两个孩子
            int leftChild = 2 * i + 1;
            int rightChild = 2 * i + 2;
            if(leftChild > n - 1) break;
            int large = i;
            //孩子节点值大就调整
            if(maxHeap[leftChild].getTotalWaitTime() > maxHeap[large].getTotalWaitTime()){
                large = leftChild;
            }
            //先判断rightChild会不会越界
            if(rightChild < n && maxHeap[rightChild].getTotalWaitTime() > maxHeap[large].getTotalWaitTime()){
                large = rightChild;
            }
            if(large == i) {
                break;  // 子节点都比自己小，那就结束
            } else {
                swap(large, i);
                i = large;  // 交换后继续从large节点往下调
            }
        }
    }

    //交换元素
    private void swap(int i, int j) {
        Patient temp = maxHeap[i];
        maxHeap[i] = maxHeap[j];
        maxHeap[j] = temp;
    }

    /*
     把堆顶（最大的）交换到末尾，缩小范围继续交换知道最后一个
     最终结果是 totalWaitTime 升序（适合第三阶段折半查找）
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
