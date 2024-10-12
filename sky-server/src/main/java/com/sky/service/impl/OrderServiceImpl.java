package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final AddressBookMapper addressBookMapper;

    private final ShoppingCartMapper shoppingCartMapper;

    private final OrderMapper orderMapper;

    private final OrderDetailMapper orderDetailMapper;

    private final UserMapper userMapper;

    private final WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 第一步检查是否确定了默认的配送地址，还要检查购物车中是否还有物品（为了代码的健壮性）
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.selectById(addressBookId);
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 要做验证顾客的地址与商家的地址距离是否有超过了一定的距离（5000m）
        String address = addressBook.getProvinceName() + addressBook.getCityName() +
                addressBook.getDistrictName() + addressBook.getDetail();
        checkOutOfRange(address);

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCartFilter = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCartFilter);
        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 第二步 一个订单插入数据库 多个订单项插入数据库（涉及多个表操作，要用事务, 还要主键回显下面会用到）
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders); // 地址id 付款方式 总金额
        orders.setNumber(String.valueOf(System.currentTimeMillis())); // 订单号
        orders.setStatus(Orders.PENDING_PAYMENT); // 订单的状态
        orders.setUserId(userId); // 用户id
        orders.setOrderTime(LocalDateTime.now()); // 下单时间
        orders.setPayStatus(Orders.UN_PAID); // 支付状态
        orders.setPhone(addressBook.getPhone()); // 手机号
        orders.setConsignee(addressBook.getConsignee()); // 收货人

        orderMapper.insert(orders);

        // 最好是批量插入，这要的话，sql语句只用发送一条，服务器压力会小很多
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart shoppingCart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetails);

        // 第三步清理购物车（但是如果支付失败的话，那就没了，还有待商榷， 也有办法返回就是从订单项中在获取商品信息插入回去）
        // shoppingCartMapper.deleteByUserId(userId);

        // 最后 设置返回的数据格式
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    // 商家的具体位置文本形式
    @Value("${sky.shop.address}")
    private String shopAddress;

    // 百度地图平台的接口验证
    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 用于验证两者的距离是否相差5000米以内
     * @param address 顾客的具体地址的文本形式
     */
    private void checkOutOfRange(String address) {
        // https://api.map.baidu.com/geocoding/v3
        String baiduGeocodingUrl = "https://api.map.baidu.com/geocoding/v3";

        Map map = new HashMap();
        map.put("address", shopAddress);
        map.put("ak", ak);
        map.put("output", "json");

        // 获取商店的经纬度坐标（返回的是json转换为了字符串）
        String shopCoordinate = HttpClientUtil.doGet(baiduGeocodingUrl, map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("商店地址解析错误");
        }

        // 数据解析 获取到具体的经纬度坐标
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        String shopLngLat = lat + "," + lng;

        map.put("address", address);

        // 获取用户的经纬度坐标（返回的是json转换为了字符串）
        String customerCoordinate = HttpClientUtil.doGet(baiduGeocodingUrl, map);

        JSONObject customerJsonObject = JSON.parseObject(customerCoordinate);
        if (!customerJsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("顾客地址解析错误");
        }

        // 数据解析 获取到具体的经纬度坐标
        JSONObject customerLocation = customerJsonObject.getJSONObject("result").getJSONObject("location");
        String customerLat = customerLocation.getString("lat");
        String customerLng = customerLocation.getString("lng");
        String customerLngLat = customerLat + "," + customerLng;

        // https://api.map.baidu.com/directionlite/v1/driving
        String baiduDirectionLite = "https://api.map.baidu.com/directionlite/v1/driving";
        map.put("origin", shopLngLat);
        map.put("destination", customerLngLat);
        map.put("steps_info","0");

        String jsonString = HttpClientUtil.doGet(baiduDirectionLite, map);
        jsonObject = JSON.parseObject(jsonString);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");
        log.info("距离是多少 {}", distance);

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }

    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        // 生成空的json,暂时跳过微信支付
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     * 感觉要在这里清空购物车会好一点
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 在清空购物车
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 查询历史订单
     * @param pageNo
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult page(Integer pageNo, Integer pageSize, Integer status) {
        PageHelper.startPage(pageNo, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        // 分页条件查询
        Page<Orders> page = orderMapper.page(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();

        if (page != null && page.getTotal() > 0) {
            for (Orders order : page) {
                Long orderId = order.getId(); // 订单id
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), list);
    }

    /**
     * 根据订单id查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetailByOrderId(Long id) {
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancelOrder(Long id) throws Exception {
        // 检查订单是否存在
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        Integer status = order.getStatus();
        if (status > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(order.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    order.getNumber(), //商户订单号
//                    order.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        // 获取订单详细的信息
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 封装购物车对象
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = orderDetailList.stream().map(od -> {
                ShoppingCart shoppingCart = new ShoppingCart();
                BeanUtils.copyProperties(od, shoppingCart);
                shoppingCart.setUserId(userId);
                shoppingCart.setCreateTime(LocalDateTime.now());
                return shoppingCart;
            }
        ).collect(Collectors.toList());

        // 批量插入
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    /**
     * 条件搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.page(ordersPageQueryDTO);

        List<OrderVO> list = null;
        if (page != null && page.getTotal() > 0) {
            list = page.stream().map(order -> {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDishes(getOrderDishStr(order));
                return orderVO;
            }).collect(Collectors.toList());
        }

        return new PageResult(page.getTotal(), list);
    }

    /**
     * 获取订单的菜品详情连接成字符串
     * @param order
     * @return
     */
    private String getOrderDishStr(Orders order) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());

        List<String> orderDishes = orderDetailList.stream().map(od -> {
            String orderDish = od.getName() + "*" + od.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // orderDishes.stream().collect(Collectors.joining("")).toString();
        return String.join("", orderDishes);
    }

    /**
     * 各个状态的订单统计
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 已接单(confirmed) 派送中(deliveryInProgress) 待接单(toBeConfirmed)
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO(toBeConfirmed, confirmed, deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 就是把订单的状态修改为已接单
        Orders ordersFilter = Orders.builder()
                .status(Orders.CONFIRMED)
                .id(ordersConfirmDTO.getId())
                .build();
        orderMapper.update(ordersFilter);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 首先就是要已经接单才能够拒单
        Long id = ordersRejectionDTO.getId();
        Orders orders = orderMapper.getById(id);

        if (orders == null || !orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 支付状态
        Integer payStatus = orders.getPayStatus();
        // 这里就是调用微信退款的功能
//        if (payStatus == Orders.PAID) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders ordersFilter = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(ordersFilter);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        // 首先就是要已经接单才能够派送订单
        Orders orders = orderMapper.getById(id);

        if (orders == null || !orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 然后就跟新订单状态就行了
        Orders ordersFilter = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(ordersFilter);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        // 首先就是要已经派送中才能够完成订单
        Orders orders = orderMapper.getById(id);

        if (orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders ordersFilter = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();
        orderMapper.update(ordersFilter);

    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());

        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 支付状态
        Integer payStatus = orders.getStatus();
        if (payStatus != 1 && payStatus != 6) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
            log.info("已退款");
        }

        Orders ordersFilter = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(ordersFilter);
    }


}
