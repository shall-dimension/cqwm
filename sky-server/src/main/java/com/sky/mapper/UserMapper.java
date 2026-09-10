package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.User;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 根据id查询用户
     *
     * @param id 用户id
     * @return 用户信息
     */
    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * 插入用户数据
     * @param user
     */
    @AutoFill(OperationType.INSERT)
    void insert(User user);

}
