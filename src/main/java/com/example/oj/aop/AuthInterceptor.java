package com.example.oj.aop;

import com.example.oj.annotation.AuthCheck;
import com.example.oj.common.ErrorCode;
import com.example.oj.exception.BusinessException;
import com.example.oj.model.entity.User;
import com.example.oj.model.enums.UserRoleEnum;
import com.example.oj.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验aop
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     * @param joinPoint
     * @param authCheck
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //当前登录用户
        User loginUser = userService.getLoginUser(request);
        //必须有该权限才能通过
        if (StringUtils.isNotBlank(mustRole)) {
            UserRoleEnum mustUserRolEnum = UserRoleEnum.getEnumByValue(mustRole);
            if (mustUserRolEnum == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            String userRole = loginUser.getUserRole();
            // 如果被封号，直接拒绝
            if (UserRoleEnum.BAN.equals(mustUserRolEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //必须有管理员权限
            if (UserRoleEnum.ADMIN.equals(mustUserRolEnum)) {
                if (!mustRole.equals(userRole)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
                }
            }
        }
        //通过校验，放行
        return joinPoint.proceed();
    }
}
