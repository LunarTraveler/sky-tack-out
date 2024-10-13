package com.sky.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "C端-订单模块")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submitOrder(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);

        // 模拟交易成功 修改数据库状态
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        log.info("模拟交易成功: {}", ordersPaymentDTO.getOrderNumber());
        return Result.success(orderPaymentVO);
    }

    /**
     * 查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> page(Integer page, Integer pageSize, Integer status) {
        PageResult pageResult = orderService.page(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 查询订单的详情
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单的详情")
    public Result<OrderVO> getOrderDetailByOrderId(@PathVariable Long id) {
        OrderVO orderVO = orderService.getOrderDetailByOrderId(id);
        return Result.success(orderVO);
    }

    /**
     * 顾客取消订单
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancelOrder(@PathVariable Long id) throws Exception{
        orderService.cancelOrder(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 催单
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("催单")
    public Result reminder(@PathVariable Long id) throws JsonProcessingException {
        orderService.reminder(id);
        return Result.success();
    }

}
