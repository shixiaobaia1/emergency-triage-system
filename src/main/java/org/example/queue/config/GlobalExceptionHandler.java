package org.example.queue.config;

import org.example.queue.vo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

/**
 * 全局异常处理器——统一把 Controller 层抛出的异常转成 Result.fail 返回
 * FIX: 原来 Controller 每个方法都 throws IOException，现在由这里统一兜底
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IOException.class)
    public Result<Void> handleIO(IOException e) {
        return Result.fail(500, "IO 错误：" + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleArg(IllegalArgumentException e) {
        return Result.fail(400, "参数错误：" + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e) {
        return Result.fail(500, "服务器内部错误：" + e.getMessage());
    }
}
