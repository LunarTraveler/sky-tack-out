package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 通过微信的用户唯一标识获取用户
     * @param openid
     * @return
     */
    @Select("select * from sky_take_out.user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入用户
     * @param user
     */
    void insert(User user);

    /**
     * 获取用户通过用户id
     * @param userId
     * @return
     */
    @Select("select * from sky_take_out.user where id = #{userId}")
    User getById(Long userId);

    /**
     * 计算每一天的新增用户的列表
     * @param map
     * @return
     */
    Integer getAmountOfNewUserByDays(Map map);

    /**
     * 截至到这个时候的全部用户量
     * @param map
     * @return
     */
    @Select("select count(1) from sky_take_out.user where create_time < #{end}")
    Integer getAmountOfTotalUserByDay(Map map);
}
