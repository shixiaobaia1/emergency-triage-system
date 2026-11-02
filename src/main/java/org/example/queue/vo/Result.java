package org.example.queue.vo;


/**
 * 统一响应包装，Controller 层所有接口都返回 Result<T>
 * FIX: 新增 VO 分层后，替代原 Controller 直接返回 Entity/数组/原始类型的做法
 * @param <T> 数据类型
 */
public class Result<T> {
    private int code;    // 状态码：200成功，其他失败
    private String msg;   // 提示信息
    private T data;      // 业务数据

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }
    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
