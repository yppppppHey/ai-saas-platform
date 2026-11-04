package com.aisaas.common.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基类
 */
@Data
public class BasePageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 10;
}
