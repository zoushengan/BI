package com.example.oj.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import static com.example.oj.constant.BiMqConstant.BI_EXCHANGE;
import static com.example.oj.constant.BiMqConstant.BI_ROUTING_KEY;

@Component
public class BiMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message){
        rabbitTemplate.convertAndSend(BI_EXCHANGE,BI_ROUTING_KEY,message);
    }
}
