package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 查询指定日期范围内每天的营业额
     *
     * @param begin 开始日期
     * @param end 结束日期
     * @return 营业额统计数据
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 查询指定日期范围内每天的新增用户和累计用户数量
     *
     * @param begin 开始日期
     * @param end 结束日期
     * @return 用户统计数据
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 查询指定日期范围内每天的订单数量和有效订单数量
     *
     * @param begin 开始日期
     * @param end 结束日期
     * @return 订单统计数据
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 查询指定日期范围内销量排名前十的商品
     *
     * @param begin 开始日期
     * @param end 结束日期
     * @return 销量排名统计数据
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);
}
