package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTask {

    private final OrderMapper orderMapper;

    /**
     * 处理订单支付超时(没三分钟一次检查)
     */
    @Scheduled(cron = "* 3 * * * ?")
    public void processPayTimeOut() {
        log.info("处理订单支付超时 {}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, time);

        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时取消支付");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直在派送中的状态(每天的凌晨时间检查一次)
     */
    @Scheduled(cron = "* * 1 * * ?")
    public void processDeliveryOrder() {
        log.info("处理一直在派送中的订单 {}", LocalDateTime.now());

        // 这个感觉要根据不同的shangpin来论，因为如果是餐饮行业的话确实是要签收时间较短
        // 但是如果是快递行业的话，那么签收时间就不能可能会倒退一到两天
        LocalDateTime time = LocalDateTime.now().plusHours(-1);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, time);

        if (ordersList !=null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

}
