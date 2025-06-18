package com.ticket.ddd.controller.model.vo;

import java.io.Serializable;

import lombok.Data;
@Data
public class ResultMessage<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Cờ thành cônge
     */
    private boolean success;

    /**
     * Thông báo
     */
    private String message;

    /**
     * Mã trả về
     */
    private Integer code;

    /**
     * Dấu thời gian
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * Đối tượng kết quả
     */
    private T result;
}
