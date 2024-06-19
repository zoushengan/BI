package com.example.oj.model.dto.chart;

import java.io.Serializable;
import lombok.Data;

/**
 * 创建请求
 *
 */
@Data
public class ChartAddRequest implements Serializable {

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String charData;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}