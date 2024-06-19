package com.example.oj.model.dto.chart;


import java.io.Serializable;
import com.example.oj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 创建者id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}