package com.example.oj.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.annotation.AuthCheck;
import com.example.oj.bizmq.BiMessageProducer;
import com.example.oj.common.BaseResponse;
import com.example.oj.common.DeleteRequest;
import com.example.oj.common.ErrorCode;
import com.example.oj.common.ResultUtils;
import com.example.oj.constant.CommonConstant;
import com.example.oj.constant.UserConstant;
import com.example.oj.exception.BusinessException;
import com.example.oj.exception.ThrowUtils;
import com.example.oj.manager.AiManager;
import com.example.oj.manager.RedisLimiterManager;
import com.example.oj.model.dto.chart.*;
import com.example.oj.model.entity.Chart;
import com.example.oj.model.entity.User;
import com.example.oj.model.vo.BiResponse;
import com.example.oj.service.ChartService;
import com.example.oj.service.UserService;
import com.example.oj.utlis.ExcelUtils;
import com.example.oj.utlis.SqlUtils;
import com.google.gson.Gson;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    private final static Gson GSON = new Gson();

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 智能分析*(异步)
     * @param
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> getChartByAIAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isNotBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        //校验文件大小
        final long ONE_MB = 1 * 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xls","xlsx");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
        User loginUser = userService.getLoginUser(request);
        //限流判断,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        //直接调用现有模型
        /*final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照一下固定格式给你提供内容：\n" +
                "分析需求:\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据:\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照一下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【\n" +
                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化。不要生成任何多余的内容，不然注释}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";*/
        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        //读取用户上传的excel文件，进行一个csv处理
        String data = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据:").append("\n").append(data).append("\n");

        long biModelId = 1785187475477090306L;
        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setCharData(data);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR,  "图表保存失败");
        CompletableFuture.runAsync(() -> {
            //先修改图表任务状态为“执行中”
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(), "更新图表运行状态失败");
                return;
            }
            //调用ai
            String result = aiManager.doChat(biModelId, userInput.toString());
            String[] split = result.split("【【【【【");
            if (split.length < 3) {
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
            boolean b2 = chartService.updateById(updateChart);
            if (!b2) {
                handleChartUpdateError(chart.getId(), "更新图表成功状态失败！");
                return;
            }
        }, threadPoolExecutor);
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
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

    /**
     * 智能分析*(异步消息队列)
     * @param
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> getChartByAIAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        //校验文件大小
        final long ONE_MB = 1 * 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xls","xlsx");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
        User loginUser = userService.getLoginUser(request);
        //限流判断,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        //直接调用现有模型
        /*final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照一下固定格式给你提供内容：\n" +
                "分析需求:\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据:\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照一下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【\n" +
                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化。不要生成任何多余的内容，不然注释}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";*/
        //读取用户上传的excel文件，进行一个csv处理
        String data = ExcelUtils.excelToCsv(multipartFile);
        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setCharData(data);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR,  "图表保存失败");

        //发送消息到消息队列
        biMessageProducer.sendMessage(String.valueOf(chart.getId()));
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }


    /**
     * 获取查询条件
     * @param chartQueryRequest
     * @return
     */
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();

        queryWrapper.eq(id!= null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId",userId);
        queryWrapper.eq("isDelete",false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
