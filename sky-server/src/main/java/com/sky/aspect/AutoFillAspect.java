package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自动填充公共字段切面
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切点：mapper包下所有方法，且带有@AutoFill注解
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointcut() {}

    /**
     * 前置通知：自动填充公共字段
     */
    @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始自动填充公共字段...");

        // 1. 获取方法上的注解，确定操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        // 2. 获取方法参数（实体对象）
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];

        // 3. 准备要填充的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 4. 根据操作类型，通过反射填充字段
        if (operationType == OperationType.INSERT) {
            log.info("INSERT操作，填充createTime, updateTime, createUser, updateUser");
            setField(entity, AutoFillConstant.SET_CREATE_TIME, now);
            setField(entity, AutoFillConstant.SET_UPDATE_TIME, now);
            setField(entity, AutoFillConstant.SET_CREATE_USER, currentId);
            setField(entity, AutoFillConstant.SET_UPDATE_USER, currentId);
        } else if (operationType == OperationType.UPDATE) {
            log.info("UPDATE操作，填充updateTime, updateUser");
            setField(entity, AutoFillConstant.SET_UPDATE_TIME, now);
            setField(entity, AutoFillConstant.SET_UPDATE_USER, currentId);
        }

        log.info("公共字段自动填充完成");
    }

    /**
     * 通过反射调用setter方法
     */
    private void setField(Object entity, String methodName, Object value) {
        try {
            // 遍历实体类的方法，找到对应的setter
            for (Method method : entity.getClass().getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                    method.invoke(entity, value);
                    log.info("已调用 {} 填充成功", methodName);
                    return;
                }
            }
            log.warn("未找到方法：{}", methodName);
        } catch (Exception e) {
            log.error("自动填充字段失败：{}", methodName, e);
        }
    }
}
