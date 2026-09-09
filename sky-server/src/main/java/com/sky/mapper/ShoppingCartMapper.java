package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 动态条件查询购物车
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据用户id查询购物车
     * @param userId
     * @return
     */
    @Select("select * from shopping_cart where user_id = #{userId}")
    List<ShoppingCart> listByUserId(Long userId);

    /**
     * 更新商品数量+1
     * @param id
     */
    @Update("update shopping_cart set number = number + 1 where id = #{id}")
    void updateNumberById(Long id);

    /**
     * 插入购物车数据
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户id删除购物车数据
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

}
