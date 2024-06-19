package com.example.oj.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oj.model.entity.Chart;
import com.example.oj.service.ChartService;
import com.example.oj.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-06-16 17:18:30
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




