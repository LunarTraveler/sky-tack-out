package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkSpaceService;
import com.sky.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderMapper orderMapper;

    private final UserMapper userMapper;

    private final WorkSpaceService workSpaceService;

    @Override
    public TurnoverReportVO turnoverStatistic(LocalDate begin, LocalDate end) {
        // 计算日期的列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 方法一使用stream流处理
        String dateString1 = dateList.stream().map(String::valueOf).collect(Collectors.joining(","));
        // 方法二使用
        String dateString2 = StringUtils.join(dateList, ",");

        // 计算营业额的列表
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            // 注意营业额是一天的状态为 “已完成” 的订单的营业额的总和
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap();
            map.put("status", Orders.COMPLETED);
            map.put("begin", beginTime);
            map.put("end", endTime);

            Double turnovers = orderMapper.getSumTurnoverByDays(map);
            // 为了防止展示数据是出现null,要用0代替
            if (turnovers == null) {
                turnovers = 0.0;
            }
            turnoverList.add(turnovers);
        }

        // 封装结果
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dateString1)
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
        return turnoverReportVO;
    }

    /**
     * 用户数量统计数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistic(LocalDate begin, LocalDate end) {
        // 计算时间日期的列表
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        String dateString = dateList.stream().map(String::valueOf).collect(Collectors.joining(","));

        // 计算每一天的新增用户的列表
        List<Integer> newUserList = new ArrayList<>();
        // 计算截至到这天的全部用户数据量
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);

            Integer newUserCount = userMapper.getAmountOfNewUserByDays(map);
            map.remove("begin");
            Integer totalUserCount = userMapper.getAmountOfTotalUserByDay(map);

            newUserList.add(newUserCount);
            totalUserList.add(totalUserCount);
        }

        // 结果转换为字符串，以逗号连接
        String newUserString = newUserList.stream().map(String::valueOf).collect(Collectors.joining(","));
        String totalUserString = totalUserList.stream().map(String::valueOf).collect(Collectors.joining(","));

        return UserReportVO.builder()
                .dateList(dateString)
                .newUserList(newUserString)
                .totalUserList(totalUserString)
                .build();
    }

    /**
     * 订单统计数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO orderStatistic(LocalDate begin, LocalDate end) {
        // 计算时间日期的列表(时间类都是不可变对象)
        LocalDate time = begin;
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(time);
        while (!time.equals(end)) {
            time = time.plusDays(1);
            dateList.add(time);
        }

//        List<LocalDate> dateList = Stream.iterate(begin, date -> date.plusDays(1))
//                .limit(ChronoUnit.DAYS.between(begin, end) + 1)
//                .collect(Collectors.toList());

        String dateString = dateList.stream().map(String::valueOf).collect(Collectors.joining(","));

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);

        // 说明: 订单是指状态可以为已付款之后的所有，也可以定义为所得订单
        // 说明: 有效订单是指状态必须为 Orders.COMPLETED(5) 已完成的订单
        // 每一个订单只要是订单都会有下单时间（order_time）, 每一个有效订单都会有送达时间（delivery_time）（有了这个时间也代表着状态一定为已送达）
        // 每个订单时都有下单时间的，但是只有有效订单是有状态为COMPLETED

        // 整个时间段的总订单数
        Integer totalOrderCounts = orderMapper.getOrderCount(map);
        // 整个时间段的总有效订单数
        Integer validOrderCounts = orderMapper.getVaildOrderCount(map);
        // 订单完成率(保留了两位小数)(要小心不能出现/0的异常)
        double orderCompletionRate = 0.0;
        if (totalOrderCounts != 0) {
            orderCompletionRate = (validOrderCounts * 1.0 / totalOrderCounts) * 100.0;
            orderCompletionRate = Math.round(orderCompletionRate) / 100.0;
        }
        // 计算每一天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        // 计算每一天的有效订单总数
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate localDate : dateList) {
            beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            map.put("begin", beginTime);
            map.put("end", endTime);

            Integer orderCount = orderMapper.getOrderCount(map);
            Integer validOrderCount = orderMapper.getVaildOrderCount(map);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        // 封装结果返回
        return OrderReportVO.builder()
                .dateList(dateString)
                .totalOrderCount(totalOrderCounts)
                .validOrderCount(validOrderCounts)
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .build();
    }

    /**
     * 查询销量排名top10(这里既包括菜品，又包括套餐，是一起算的)
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        // 首先要获取到完整形式的时间表达
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.top10(beginTime, endTime);
        List<String> nameList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        String nameString = StringUtils.join(nameList, ",");
        String numberString = numberList.stream().map(String::valueOf).collect(Collectors.joining(","));

        return SalesTop10ReportVO.builder()
                .nameList(nameString)
                .numberList(numberString)
                .build();
    }

    /**
     * 导出Excel报表接口
     * @param response
     */
    @Override
    public void export(HttpServletResponse response) {
        // 1.查询近30天的数据报表
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        // 查询相应的数据
        BusinessDataVO businessData = workSpaceService.businessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        // 2.通过POI把数据写入到excel文件内
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // 首先获取那个excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 获取相应的sheet表
            XSSFSheet sheet = excel.getSheet("Sheet1");

            // 填写相应的数据
            XSSFRow row = sheet.getRow(1);
            row.getCell(1).setCellValue("统计时间 : " + begin + "-" + end);

            row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO data = workSpaceService.businessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());
            }

            // 3.把这个excel文件写回到浏览器 进行下载
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
