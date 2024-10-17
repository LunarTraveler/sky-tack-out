package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

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

    /**
     * 导出Excel报表接口
     * @param response
     */
    void export(HttpServletResponse response);
}
