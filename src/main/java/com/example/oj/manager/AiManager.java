package com.example.oj.manager;

import com.example.oj.common.ErrorCode;
import com.example.oj.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AiManager {

    @Resource
    private YuCongMingClient yuCongMingClient;

    /**
     * AI 对话
     * @param message
     * @return
     */
    public String doChat(long modeId, String message){
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modeId);
        devChatRequest.setMessage(message);
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        System.out.println(response);
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 响应错误");
        }
        return response.getData().getContent();
    }
}
