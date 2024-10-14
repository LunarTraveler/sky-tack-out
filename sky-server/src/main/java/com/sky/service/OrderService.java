package com.sky.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.*;

import java.time.LocalDate;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo) throws JsonProcessingException;

    /**
     * 查询历史订单
     * @param pageNo
     * @param pageSize
     * @param status
     * @return
     */
    PageResult page(Integer pageNo, Integer pageSize, Integer status);

    /**
     * 根据订单id查询订单详情
     * @param id
     * @return
     */
    OrderVO getOrderDetailByOrderId(Long id);

    /**
     * 顾客取消订单
     * @param id
     */
    void cancelOrder(Long id) throws Exception;

    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单统计
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 派送订单
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 催单
     * @param id
     */
    void reminder(Long id) throws JsonProcessingException;

    /**
     * 统计每天的营业额
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO turnoverStatistic(LocalDate begin, LocalDate end);

    /**
     * 用户数量统计数据
     * @param begin
     * @param end
     * @return
     */
    UserReportVO userStatistic(LocalDate begin, LocalDate end);

    /**
     * 订单统计数据
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO orderStatistic(LocalDate begin, LocalDate end);

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO top10(LocalDate begin, LocalDate end);
}
