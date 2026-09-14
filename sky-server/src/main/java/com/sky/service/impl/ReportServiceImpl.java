package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.exception.BaseException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 查询指定日期范围内每天的营业额
     *
     * @param begin 开始日期
     * @param end 结束日期
     * @return 营业额统计数据
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        validateDateRange(begin, end);

        List<LocalDate> dateList = buildDateList(begin, end);
        List<BigDecimal> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();

            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("begin", beginTime);
            queryMap.put("end", endTime);
            queryMap.put("status", Orders.COMPLETED);

            BigDecimal turnover = orderMapper.sumByMap(queryMap);
            turnoverList.add(turnover == null ? BigDecimal.ZERO : turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 查询指定日期范围内每天的新增用户和累计用户数量
     *
     * @param begin 开始日期
     * @param end 结束日期
     * @return 用户统计数据
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        validateDateRange(begin, end);

        List<LocalDate> dateList = buildDateList(begin, end);
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();

            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("end", endTime);
            Integer totalUser = userMapper.countByMap(queryMap);

            queryMap.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(queryMap);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 校验报表日期范围
     */
    private void validateDateRange(LocalDate begin, LocalDate end) {
        if (begin == null || end == null || begin.isAfter(end)) {
            throw new BaseException(MessageConstant.REPORT_DATE_RANGE_ERROR);
        }
    }

    /**
     * 构造包含开始日期和结束日期的连续日期集合
     */
    private List<LocalDate> buildDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end)) {
            dateList.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        return dateList;
    }
}
