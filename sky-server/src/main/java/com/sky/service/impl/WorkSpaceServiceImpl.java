package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkSpaceServiceImpl implements WorkSpaceService {

    private final UserMapper userMapper;

    private final OrderMapper orderMapper;

    private final DishMapper dishMapper;

    private final SetmealMapper setmealMapper;

    /**
     * 查询今日运营数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO businessData(LocalDateTime begin, LocalDateTime end) {
        /*
         * 运营数据：
         * newUsers 新增用户
         * orderCompletionRate 订单完成率
         * turnover 营业额
         * unitPrice 平均单价
         * validOrderCount 有效订单量
         */
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);

        // 新增用户
        Integer newUsers = userMapper.getAmountOfNewUserByDays(map);
        // 总订单
        Integer totalOrderCount = orderMapper.getOrderCount(map);
        // 有效订单量
        Integer validOrderCount = orderMapper.getVaildOrderCount(map);
        // 订单完成率(保留两位小数)
        Double orderCompletionRate = 0.0;
        // 营业额
        map.put("status", Orders.COMPLETED);
        Double turnover = orderMapper.getSumTurnoverByDays(map);
        if (turnover == null) {
            turnover = 0.0;
        }
        // 平均单价
        Double unitPrice = 0.0;
        if (validOrderCount != 0 && totalOrderCount != 0) {
            orderCompletionRate = (validOrderCount * 1.0 / totalOrderCount) * 100;
            orderCompletionRate = Math.round(orderCompletionRate) / 100.0;
            unitPrice = (turnover * 1.0 / validOrderCount);
        }

        return BusinessDataVO.builder()
                .newUsers(newUsers)
                .orderCompletionRate(orderCompletionRate)
                .turnover(turnover)
                .unitPrice(unitPrice)
                .validOrderCount(validOrderCount)
                .build();
    }

    /**
     * 查询订单管理数据(其实这种看当天数据的只要大于今天0点就行，没有必要在小与今天的24点)
     * @return
     */
    @Override
    public OrderOverViewVO orderOverView() {
        Map map = new HashMap();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));
        map.put("end", LocalDateTime.now().with(LocalTime.MAX));// 这个其实加不加都是可行

        // 待接单数量
        map.put("status", Orders.TO_BE_CONFIRMED);
        Integer waitingOrders = orderMapper.getOrderCount(map);
        // 待派送数量
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.getOrderCount(map);
        // 完成数量
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.getOrderCount(map);
        // 已取消数量
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.getOrderCount(map);
        // 总订单
        map.put("status", null);
        Integer allOrders = orderMapper.getOrderCount(map);

        // 封装返回数据
        return OrderOverViewVO.builder()
                .allOrders(allOrders)
                .cancelledOrders(cancelledOrders)
                .completedOrders(completedOrders)
                .deliveredOrders(deliveredOrders)
                .waitingOrders(waitingOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }



}
