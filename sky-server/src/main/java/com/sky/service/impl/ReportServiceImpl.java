package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

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
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计营业额
     * @param start
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate start, LocalDate end) {
        // 1. 构造日期列表（用局部变量遍历，避免修改入参 start）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            dateList.add(cursor);
            cursor = cursor.plusDays(1);
        }

        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date: dateList){
            LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("start", startTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        // 5. 封装返回
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate cursor = begin;
        while (!cursor.isAfter(end)) {
            dateList.add(cursor);
            cursor = cursor.plusDays(1);
        }
        // 2. 逐天统计新增用户数和累计用户数
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for(LocalDate date: dateList){
            LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 新增用户：当天注册
            Long newUserCount = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .ge(User::getCreateTime, startTime)
                    .le(User::getCreateTime, endTime));

            // 累计用户：截止到当天
            Long totalUserCount = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .le(User::getCreateTime, endTime));

            newUserList.add(newUserCount.intValue());
            totalUserList.add(totalUserCount.intValue());
        }

        // 3. 封装返回
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate cursor = begin;
        while (!cursor.isAfter(end)) {
            dateList.add(cursor);
            cursor = cursor.plusDays(1);
        }

        // 每日订单数 / 每日有效订单数
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        long totalCount = 0;  // 订单总数（累加）
        long validCount = 0;  // 有效订单数（累加）

        for(LocalDate date: dateList){
            LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 当天订单总数
            long orderTotal = orderMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                    .ge(Orders::getOrderTime, startTime)
                    .le(Orders::getOrderTime, endTime));
            orderCountList.add((int) orderTotal);
            totalCount += orderTotal;

            // 当天有效订单数
            long validOrder = orderMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                    .ge(Orders::getOrderTime, startTime)
                    .le(Orders::getOrderTime, endTime)
                    .eq(Orders::getStatus, Orders.COMPLETED));
            validOrderCountList.add((int) validOrder);
            validCount += validOrder;
        }

        // 订单完成率 = 有效订单数 / 订单总数
        double rate = totalCount == 0 ? 0.0 : (double) validCount / totalCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount((int) totalCount)
                .validOrderCount((int) validCount)
                .orderCompletionRate(rate)
                .build();
    }

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getTop10(beginTime,endTime);
        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String name = StringUtils.join(nameList,",");
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String number = StringUtils.join(numberList,",");
        return SalesTop10ReportVO.builder()
                .nameList(name)
                .numberList(number)
                .build();
    }

    /**
     * 导出数据
     * @param response
     */
    public void exportData(HttpServletResponse response) {
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin,LocalTime.MIN),LocalDateTime.of(dateEnd,LocalTime.MAX));

        // 写入excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            // 填充数据
            XSSFSheet sheet = excel.getSheet("sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间"+dateBegin+"至"+dateEnd);

            // 获取第四行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            // 获取第五行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            for(int i = 0;i < 30;i++){
                LocalDate date = dateBegin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN),LocalDateTime.of(date,LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            out.close();
            excel.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}