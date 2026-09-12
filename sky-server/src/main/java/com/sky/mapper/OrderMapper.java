package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 根据条件分页查询订单
     *
     * @param ordersPageQueryDTO 查询条件
     * @return 订单分页数据
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计指定三种状态的订单数量
     *
     * @param toBeConfirmed 待接单状态
     * @param confirmed 待派送状态
     * @param deliveryInProgress 派送中状态
     * @return 各状态订单数量
     */
    OrderStatisticsVO statistics(@Param("toBeConfirmed") Integer toBeConfirmed,
                                 @Param("confirmed") Integer confirmed,
                                 @Param("deliveryInProgress") Integer deliveryInProgress);

    /**
     * 根据订单id和用户id查询订单
     *
     * @param orderId 订单id
     * @param userId 用户id
     * @return 订单信息
     */
    @Select("select * from orders where id = #{orderId} and user_id = #{userId}")
    Orders getByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /**
     * 根据订单id查询订单及下单用户名称
     *
     * @param orderId 订单id
     * @return 订单信息
     */
    @Select("select o.*, u.name as user_name from orders o " +
            "left join user u on o.user_id = u.id where o.id = #{orderId}")
    Orders getById(Long orderId);

    /**
     * 插入订单
     *
     * @param orders 订单信息
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber 订单号
     * @return 订单信息
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 动态修改订单
     *
     * @param orders 订单信息
     */
    void update(Orders orders);

    /**
     * 根据订单状态和下单时间查询订单
     *
     * @param status 订单状态
     * @param orderTime 下单时间上限
     * @return 订单列表
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(@Param("status") Integer status,
                                          @Param("orderTime") LocalDateTime orderTime);

    /**
     * 在订单仍处于预期状态时更新状态及相关字段
     *
     * @param orders 待更新的订单信息
     * @param expectedStatus 预期原状态
     * @return 受影响行数
     */
    int updateStatusIfCurrent(@Param("orders") Orders orders,
                              @Param("expectedStatus") Integer expectedStatus);
}
