package com.example.oj.mapper;

import com.example.oj.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2024-06-16 17:18:30
* @Entity com.example.oj.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

    List<Map<String, Object>> queryChartData(@Param("querySql") String querySql);
}




