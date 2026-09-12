package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端-订单接口")
@Slf4j
public class OrderController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private OrderService orderService;

    /**
     * 根据条件分页搜索订单
     *
     * @param ordersPageQueryDTO 订单查询条件
     * @return 订单分页结果
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        Integer status = ordersPageQueryDTO.getStatus();
        if (status != null && (status < Orders.PENDING_PAYMENT || status > Orders.REFUNDED)) {
            return Result.error(MessageConstant.ORDER_STATUS_ERROR);
        }
        if (ordersPageQueryDTO.getBeginTime() != null
                && ordersPageQueryDTO.getEndTime() != null
                && ordersPageQueryDTO.getBeginTime().isAfter(ordersPageQueryDTO.getEndTime())) {
            return Result.error("开始时间不能晚于结束时间");
        }

        int page = ordersPageQueryDTO.getPage() > 0
                ? ordersPageQueryDTO.getPage()
                : DEFAULT_PAGE;
        int pageSize = ordersPageQueryDTO.getPageSize() > 0
                ? Math.min(ordersPageQueryDTO.getPageSize(), MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;
        ordersPageQueryDTO.setPage(page);
        ordersPageQueryDTO.setPageSize(pageSize);
        ordersPageQueryDTO.setUserId(null);

        log.info("订单搜索：{}", ordersPageQueryDTO);
        return Result.success(orderService.conditionSearch(ordersPageQueryDTO));
    }

    /**
     * 统计待接单、待派送和派送中的订单数量
     *
     * @return 各状态订单数量
     */
    @GetMapping("/statistics")
    @ApiOperation("各状态订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        return Result.success(orderService.statistics());
    }

    /**
     * 查询订单详情
     *
     * @param id 订单id
     * @return 订单详情
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id) {
        log.info("管理端查询订单详情：{}", id);
        return Result.success(orderService.getOrderDetailForAdmin(id));
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO 接单参数
     * @return 操作结果
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result<String> confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        log.info("接单：{}", ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO 拒单参数
     * @return 操作结果
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result<String> rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        log.info("拒单：{}", ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }
}
