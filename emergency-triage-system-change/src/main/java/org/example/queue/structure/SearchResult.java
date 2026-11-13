package org.example.queue.structure;
/*
*查找结果包裹类——一口气返回三个值
* index 找到元素所在下标
* count ASL算了多少次
* data 患者信息
 */
public class SearchResult<T> {
    private final int index;
    private final int count;
    private final T data;

    //全参构造
    public SearchResult(int index, int count, T data) {
        this.index = index;
        this.count = count;
        this.data = data;
    }

    //查找失败
    public static <T> SearchResult<T> miss(int count) {
        return new SearchResult<>(-1, count, null);
    }

    public int getIndex() {
        return index;
    }

    public int getCount() {
        return count;
    }

    public T getData() {
        return data;
    }

    //是否找到
    public boolean isFound() {
        return index != -1;
    }
}