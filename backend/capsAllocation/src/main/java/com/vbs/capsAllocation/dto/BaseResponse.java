package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base response wrapper for all API responses
 * @param <T> The type of data being returned
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private String status;
    private int code;
    private String message;
    private T data;

    /**
     * Create a success response with data
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>("success", 200, message, data);
    }

    /**
     * Create a success response without data
     */
    public static <T> BaseResponse<T> success(String message) {
        return new BaseResponse<>("success", 200, message, null);
    }

    /**
     * Create a success response with custom code
     */
    public static <T> BaseResponse<T> success(String message, T data, int code) {
        return new BaseResponse<>("success", code, message, data);
    }

    /**
     * Create an error response
     */
    public static <T> BaseResponse<T> error(String message, int code) {
        return new BaseResponse<>("error", code, message, null);
    }

    /**
     * Create an error response with data
     */
    public static <T> BaseResponse<T> error(String message, T data, int code) {
        return new BaseResponse<>("error", code, message, data);
    }
}
