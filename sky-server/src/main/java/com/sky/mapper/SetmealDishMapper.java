package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询关联的套餐数量
     * @param dishId
     * @return
     */
    Integer countByDishId(Long dishId);

}
