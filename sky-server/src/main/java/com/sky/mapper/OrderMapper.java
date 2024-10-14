package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from sky_take_out.orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> page(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 统计订单状态对应的数量
     * @param status
     * @return
     */
    @Select("select count(1) from sky_take_out.orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 查询满足状态和订单时间差值的订单
     * @param status
     * @param time
     * @return
     */
    @Select("select * from sky_take_out.orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime time);

    /**
     * 计算一天的营业额总数
     * @param map
     * @return
     */
    Double getSumTurnoverByDays(Map map);

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

    /**
     * 得到一段时间内的总订单数
     * @param map
     * @return
     */
    @Select("select count(1) from sky_take_out.orders where order_time between #{begin} and #{end}")
    Integer getOrderCount(Map map);

    /**
     * 得到一段时间内的有效总订单数
     * @param map
     * @return
     */
    @Select("select count(1) from sky_take_out.orders where delivery_time between #{begin} and #{end}")
    Integer getVaildOrderCount(Map map);

    /**
     * 查询销量排名top10(这里既包括菜品，又包括套餐，是一起算的)
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> top10(LocalDateTime begin, LocalDateTime end);
}
