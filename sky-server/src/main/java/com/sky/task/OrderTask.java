package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    private static final String TIMEOUT_CANCEL_REASON = "订单超时，自动取消";

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 每分钟处理超过15分钟仍未支付的订单
     */
    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Shanghai")
    public void processTimeoutOrder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutThreshold = now.minusMinutes(15);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(
                Orders.PENDING_PAYMENT,
                timeoutThreshold
        );

        int updatedCount = 0;
        if (ordersList != null) {
            for (Orders orders : ordersList) {
                Orders cancelledOrder = Orders.builder()
                        .id(orders.getId())
                        .status(Orders.CANCELLED)
                        .cancelReason(TIMEOUT_CANCEL_REASON)
                        .cancelTime(now)
                        .build();
                updatedCount += orderMapper.updateStatusIfCurrent(
                        cancelledOrder,
                        Orders.PENDING_PAYMENT
                );
            }
        }

        int queriedCount = ordersList == null ? 0 : ordersList.size();
        log.info("定时处理支付超时订单完成，查询数量：{}，更新数量：{}，跳过数量：{}",
                queriedCount, updatedCount, queriedCount - updatedCount);
    }

    /**
     * 每天凌晨1点处理超过1小时仍处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Shanghai")
    public void processDeliveryOrder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deliveryThreshold = now.minusHours(1);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(
                Orders.DELIVERY_IN_PROGRESS,
                deliveryThreshold
        );

        int updatedCount = 0;
        if (ordersList != null) {
            for (Orders orders : ordersList) {
                Orders completedOrder = Orders.builder()
                        .id(orders.getId())
                        .status(Orders.COMPLETED)
                        .deliveryTime(now)
                        .build();
                updatedCount += orderMapper.updateStatusIfCurrent(
                        completedOrder,
                        Orders.DELIVERY_IN_PROGRESS
                );
            }
        }

        int queriedCount = ordersList == null ? 0 : ordersList.size();
        log.info("定时处理派送中订单完成，查询数量：{}，更新数量：{}，跳过数量：{}",
                queriedCount, updatedCount, queriedCount - updatedCount);
    }
}
