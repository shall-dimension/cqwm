package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.entity.User;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.properties.PaymentProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private PaymentProperties paymentProperties;

    /**
     * 分页查询当前用户的历史订单
     *
     * @param ordersPageQueryDTO 分页及订单状态查询条件
     * @return 历史订单分页结果
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> records = new ArrayList<>();
        for (Orders orders : page.getResult()) {
            records.add(buildOrderVO(orders));
        }

        return new PageResult(page.getTotal(), records);
    }

    /**
     * 管理端根据条件分页搜索订单
     *
     * @param ordersPageQueryDTO 订单查询条件
     * @return 订单分页结果
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        return pageQuery(ordersPageQueryDTO);
    }

    /**
     * 统计待接单、待派送和派送中的订单数量
     *
     * @return 各状态订单数量
     */
    @Override
    public OrderStatisticsVO statistics() {
        return orderMapper.statistics(
                Orders.TO_BE_CONFIRMED,
                Orders.CONFIRMED,
                Orders.DELIVERY_IN_PROGRESS
        );
    }

    /**
     * 查询当前用户的订单详情
     *
     * @param orderId 订单id
     * @return 订单详情
     */
    @Override
    public OrderVO getOrderDetail(Long orderId) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByIdAndUserId(orderId, userId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        return buildOrderVO(orders);
    }

    /**
     * 管理端查询订单详情
     *
     * @param orderId 订单id
     * @return 订单详情
     */
    @Override
    public OrderVO getOrderDetailForAdmin(Long orderId) {
        Orders orders = orderMapper.getById(orderId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        return buildOrderVO(orders);
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO 接单参数
     */
    @Override
    @Transactional
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Long orderId = ordersConfirmDTO.getId();
        Orders orders = orderMapper.getById(orderId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders confirmedOrder = Orders.builder()
                .id(orderId)
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(confirmedOrder);
    }

    /**
     * 取消当前用户的订单
     *
     * @param orderId 订单id
     */
    @Override
    @Transactional
    public void cancelById(Long orderId) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByIdAndUserId(orderId, userId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(orders.getStatus(), Orders.PENDING_PAYMENT)
                && !Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders cancelledOrder = Orders.builder()
                .id(orderId)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();

        refundIfPaid(orders, cancelledOrder);

        orderMapper.update(cancelledOrder);
    }

    /**
     * 将原订单菜品重新加入购物车
     *
     * @param orderId 原订单id
     */
    @Override
    @Transactional
    public void repetition(Long orderId) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByIdAndUserId(orderId, userId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        LocalDateTime createTime = LocalDateTime.now();
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(createTime);
            shoppingCartList.add(shoppingCart);
        }

        shoppingCartMapper.deleteByUserId(userId);
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO 下单信息
     * @return 下单结果
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        // 查询并校验当前用户的购物车
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.listByUserId(userId);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 查询并校验当前用户的收货地址
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null || !Objects.equals(addressBook.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 构造订单并写入订单表
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(buildAddress(addressBook));
        orderMapper.insert(orders);

        // 将购物车数据复制为订单明细并批量写入
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart shoppingCart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 下单成功后清空当前用户的购物车
        shoppingCartMapper.deleteByUserId(userId);

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO 支付信息
     * @return 支付参数
     */
    @Override
    @Transactional
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());

        // 支付接口只能操作当前用户自己的待付款订单
        if (orders == null || !Objects.equals(orders.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Objects.equals(orders.getPayStatus(), Orders.PAID)) {
            throw new OrderBusinessException(MessageConstant.ORDER_ALREADY_PAID);
        }
        if (!Objects.equals(orders.getStatus(), Orders.PENDING_PAYMENT)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 模拟支付不调用微信接口，直接推进订单状态
        if (paymentProperties.isMock()) {
            updateOrderPaid(orders, ordersPaymentDTO.getPayMethod());
            return OrderPaymentVO.builder()
                    .mock(true)
                    .build();
        }

        User user = userMapper.getById(userId);
        JSONObject jsonObject = weChatPayUtil.pay(
                orders.getNumber(),
                orders.getAmount(),
                "苍穹外卖订单",
                user.getOpenid()
        );

        if ("ORDERPAID".equals(jsonObject.getString("code"))) {
            throw new OrderBusinessException(MessageConstant.ORDER_ALREADY_PAID);
        }

        OrderPaymentVO orderPaymentVO = JSON.toJavaObject(jsonObject, OrderPaymentVO.class);
        orderPaymentVO.setPackageStr(jsonObject.getString("package"));
        orderPaymentVO.setMock(false);
        return orderPaymentVO;
    }

    /**
     * 支付成功后修改订单状态
     *
     * @param orderNumber 订单号
     */
    @Override
    @Transactional
    public void paySuccess(String orderNumber) {
        Orders orders = orderMapper.getByNumber(orderNumber);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Objects.equals(orders.getPayStatus(), Orders.PAID)) {
            return;
        }
        updateOrderPaid(orders, orders.getPayMethod());
    }

    /**
     * 将订单更新为已支付、待接单
     *
     * @param orders 订单信息
     * @param payMethod 支付方式
     */
    private void updateOrderPaid(Orders orders, Integer payMethod) {
        Orders paidOrder = Orders.builder()
                .id(orders.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .payMethod(payMethod)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(paidOrder);
    }

    /**
     * 拼接完整收货地址
     *
     * @param addressBook 地址簿信息
     * @return 完整地址
     */
    private String buildAddress(AddressBook addressBook) {
        return String.join("",
                valueOrEmpty(addressBook.getProvinceName()),
                valueOrEmpty(addressBook.getCityName()),
                valueOrEmpty(addressBook.getDistrictName()),
                valueOrEmpty(addressBook.getDetail()));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 已付款订单执行退款，并设置更新对象的退款状态
     */
    private void refundIfPaid(Orders orders, Orders updateOrder) throws Exception {
        if (!Objects.equals(orders.getPayStatus(), Orders.PAID)) {
            return;
        }

        if (!paymentProperties.isMock()) {
            weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    orders.getAmount(),
                    orders.getAmount()
            );
        }
        updateOrder.setPayStatus(Orders.REFUND);
    }

    /**
     * 将订单及其明细组装为订单视图对象
     */
    private OrderVO buildOrderVO(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        orderVO.setOrderDishes(buildOrderDishes(orderDetailList));
        return orderVO;
    }

    /**
     * 将订单明细拼接为管理端列表展示文本
     */
    private String buildOrderDishes(List<OrderDetail> orderDetailList) {
        StringBuilder orderDishes = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            if (orderDishes.length() > 0) {
                orderDishes.append(";");
            }
            orderDishes.append(orderDetail.getName())
                    .append("*")
                    .append(orderDetail.getNumber());
        }
        return orderDishes.toString();
    }
}
