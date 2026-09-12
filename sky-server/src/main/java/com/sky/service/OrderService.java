package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 分页查询当前用户的历史订单
     *
     * @param ordersPageQueryDTO 分页及订单状态查询条件
     * @return 历史订单分页结果
     */
    PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询当前用户的订单详情
     *
     * @param orderId 订单id
     * @return 订单详情
     */
    OrderVO getOrderDetail(Long orderId);

    /**
     * 取消当前用户的订单
     *
     * @param orderId 订单id
     */
    void cancelById(Long orderId) throws Exception;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO 下单信息
     * @return 下单结果
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO 支付信息
     * @return 支付参数
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功后修改订单状态
     *
     * @param orderNumber 订单号
     */
    void paySuccess(String orderNumber);
}
