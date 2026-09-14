package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        Map<String, Object> orderQuery = buildTimeQuery(begin, end);

        Integer totalOrderCount = orderMapper.countByMap(orderQuery);

        orderQuery.put("status", Orders.COMPLETED);
        BigDecimal turnoverAmount = orderMapper.sumByMap(orderQuery);
        double turnover = turnoverAmount == null ? 0.0 : turnoverAmount.doubleValue();
        Integer validOrderCount = orderMapper.countByMap(orderQuery);

        double orderCompletionRate = 0.0;
        double unitPrice = 0.0;
        if (totalOrderCount != null && totalOrderCount > 0
                && validOrderCount != null && validOrderCount > 0) {
            orderCompletionRate = (double) validOrderCount / totalOrderCount;
            unitPrice = turnover / validOrderCount;
        }

        Integer newUsers = userMapper.countByMap(buildTimeQuery(begin, end));

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 查询今日订单管理数据
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        Map<String, Object> queryMap = buildTimeQuery(
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
        );

        queryMap.put("status", Orders.TO_BE_CONFIRMED);
        Integer waitingOrders = orderMapper.countByMap(queryMap);

        queryMap.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(queryMap);

        queryMap.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(queryMap);

        queryMap.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(queryMap);

        queryMap.remove("status");
        Integer allOrders = orderMapper.countByMap(queryMap);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     */
    @Override
    public DishOverViewVO getDishOverView() {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(queryMap);

        queryMap.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(queryMap);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(queryMap);

        queryMap.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(queryMap);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    private Map<String, Object> buildTimeQuery(LocalDateTime begin, LocalDateTime end) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("begin", begin);
        queryMap.put("end", end);
        return queryMap;
    }
}
