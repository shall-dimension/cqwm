package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
     * 根据订单id和用户id查询订单
     *
     * @param orderId 订单id
     * @param userId 用户id
     * @return 订单信息
     */
    @Select("select * from orders where id = #{orderId} and user_id = #{userId}")
    Orders getByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

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
}
