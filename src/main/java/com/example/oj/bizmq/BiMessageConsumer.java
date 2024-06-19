package com.example.oj.bizmq;

import com.example.oj.common.ErrorCode;
import com.example.oj.exception.BusinessException;
import com.example.oj.manager.AiManager;
import com.example.oj.model.entity.Chart;
import com.example.oj.service.ChartService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import static com.example.oj.constant.CommonConstant.BI_MODE_ID;

import static  com.example.oj.constant.BiMqConstant.BI_QUEUE_NAME;


@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        if (StringUtils.isBlank(message)) {
            // 如果失败，消息拒绝
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long charId = Long.parseLong(message);
        Chart chart = chartService.getById(charId);
        if (chart == null) {
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }
        //先修改图表任务状态为“执行中”
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(), "更新图表运行状态失败");
            return;
        }
        //调用ai
        String result = aiManager.doChat(BI_MODE_ID, buildUserInput(chart));
        String[] split = result.split("【【【【【");
        if (split.length < 3) {
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(), "AI 生成错误");
            return;
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus("succeed");
        boolean b2 = chartService.updateById(updateChartResult);
        if (!b2) {
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(), "更新图表成功状态失败！");
            return;
        }
        // 消息确认
        channel.basicAck(deliveryTag, false);
    }

    /**
     * 构建用户输入
     * @param chart
     * @return
     */
    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getCharData();

        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n").append(csvData).append("\n");
        return userInput.toString();
    }

    private void handleChartUpdateError(long charId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(charId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + charId + "," + execMessage);
        }
    }
}
