package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.DataOverViewQueryDTO;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.exception.BaseException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

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
     * 查询指定日期范围内每天的订单数量和有效订单数量
     *
     * @param begin 开始日期
     * @param end 结束日期
     * @return 订单统计数据
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        validateDateRange(begin, end);

        List<LocalDate> dateList = buildDateList(begin, end);
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();

            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("begin", beginTime);
            queryMap.put("end", endTime);
            Integer orderCount = orderMapper.countByMap(queryMap);

            queryMap.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(queryMap);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        int totalOrderCount = orderCountList.stream()
                .mapToInt(Integer::intValue)
                .sum();
        int validOrderCount = validOrderCountList.stream()
                .mapToInt(Integer::intValue)
                .sum();
        double orderCompletionRate = totalOrderCount == 0
                ? 0.0
                : (double) validOrderCount / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 查询指定日期范围内销量排名前十的商品
     *
     * @param begin 开始日期
     * @param end 结束日期
     * @return 销量排名统计数据
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        validateDateRange(begin, end);

        DataOverViewQueryDTO queryDTO = DataOverViewQueryDTO.builder()
                .begin(begin.atStartOfDay())
                .end(end.plusDays(1).atStartOfDay())
                .build();
        List<GoodsSalesDTO> goodsSalesList = orderMapper.getSalesTop10(queryDTO);

        String nameList = goodsSalesList.stream()
                .map(GoodsSalesDTO::getName)
                .collect(Collectors.joining(","));
        String numberList = goodsSalesList.stream()
                .map(goodsSalesDTO -> String.valueOf(goodsSalesDTO.getNumber()))
                .collect(Collectors.joining(","));

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出最近30个完整自然日的运营数据报表
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate beginDate = endDate.minusDays(29);

        BusinessDataVO overview = workspaceService.getBusinessData(
                beginDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("template/运营数据报表模板.xlsx")) {
            if (inputStream == null) {
                throw new BaseException(MessageConstant.REPORT_TEMPLATE_NOT_FOUND);
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
                XSSFSheet sheet = workbook.getSheet("Sheet1");
                if (sheet == null) {
                    throw new BaseException(MessageConstant.REPORT_TEMPLATE_NOT_FOUND);
                }

                fillOverview(sheet, beginDate, endDate, overview);
                fillDailyDetails(sheet, beginDate);

                ServletOutputStream outputStream = response.getOutputStream();
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (IOException exception) {
            throw new BaseException(MessageConstant.REPORT_EXPORT_FAILED);
        }
    }

    /**
     * 填充30天概览数据
     */
    private void fillOverview(XSSFSheet sheet, LocalDate beginDate, LocalDate endDate,
                              BusinessDataVO overview) {
        sheet.getRow(1).getCell(1)
                .setCellValue("时间：" + beginDate + "至" + endDate);

        XSSFRow row = sheet.getRow(3);
        row.getCell(2).setCellValue(overview.getTurnover());
        row.getCell(4).setCellValue(overview.getOrderCompletionRate());
        row.getCell(6).setCellValue(overview.getNewUsers());

        row = sheet.getRow(4);
        row.getCell(2).setCellValue(overview.getValidOrderCount());
        row.getCell(4).setCellValue(overview.getUnitPrice());
    }

    /**
     * 填充每天的运营明细
     */
    private void fillDailyDetails(XSSFSheet sheet, LocalDate beginDate) {
        for (int index = 0; index < 30; index++) {
            LocalDate date = beginDate.plusDays(index);
            BusinessDataVO dailyData = workspaceService.getBusinessData(
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay()
            );

            XSSFRow row = sheet.getRow(7 + index);
            row.getCell(1).setCellValue(date.toString());
            row.getCell(2).setCellValue(dailyData.getTurnover());
            row.getCell(3).setCellValue(dailyData.getValidOrderCount());
            row.getCell(4).setCellValue(dailyData.getOrderCompletionRate());
            row.getCell(5).setCellValue(dailyData.getUnitPrice());
            row.getCell(6).setCellValue(dailyData.getNewUsers());
        }
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
