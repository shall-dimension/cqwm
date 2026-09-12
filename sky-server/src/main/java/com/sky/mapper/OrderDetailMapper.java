package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 根据订单id查询订单明细
     *
     * @param orderId 订单id
     * @return 订单明细列表
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);

    /**
     * 批量插入订单明细
     *
     * @param orderDetailList 订单明细列表
     */
    void insertBatch(@Param("orderDetailList") List<OrderDetail> orderDetailList);
}
