package com.sky.service;

import com.sky.vo.TurnoverReportVO;

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
}
