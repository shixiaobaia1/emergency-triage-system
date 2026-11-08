package org.example.queue.structure;

public class SearchResult<T> {
    private final int index;
    private final int count;
    private final T data;

    public SearchResult(int index, int count, T data) {
        this.index = index;
        this.count = count;
        this.data = data;
    }

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

    public boolean isFound() {
        return index != -1;
    }
}