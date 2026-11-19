package com.badminton.controller;

import com.badminton.aspect.AdminLog;
import com.badminton.common.ApiResponse;
import com.badminton.common.PageResult;
import com.badminton.dto.ProductRequest;
import com.badminton.dto.SalesCreateRequest;
import com.badminton.dto.SalesStatisticsResponse;
import com.badminton.entity.Product;
import com.badminton.entity.SalesOrder;
import com.badminton.security.CurrentUser;
import com.badminton.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "后台-销售管理", description = "商品、库存、商品销售单与收入统计")
public class AdminSalesController {

    private final SalesService salesService;

    public AdminSalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/products")
    @Operation(summary = "商品列表")
    public ApiResponse<List<Product>> products(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String status) {
        return ApiResponse.ok(salesService.products(keyword, category, status));
    }

    @PostMapping("/products")
    @AdminLog(module = "PRODUCT", action = "CREATE_PRODUCT", targetType = "PRODUCT", detail = "新增商品")
    @Operation(summary = "新增商品")
    public ApiResponse<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("商品创建成功", salesService.createProduct(request));
    }

    @PutMapping("/products/{productId}")
    @AdminLog(module = "PRODUCT", action = "UPDATE_PRODUCT", targetType = "PRODUCT", detail = "修改商品")
    @Operation(summary = "修改商品 / 库存")
    public ApiResponse<Product> updateProduct(@PathVariable Long productId,
                                              @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("商品已更新", salesService.updateProduct(productId, request));
    }

    @DeleteMapping("/products/{productId}")
    @AdminLog(module = "PRODUCT", action = "DELETE_PRODUCT", targetType = "PRODUCT", detail = "删除商品")
    @Operation(summary = "删除商品（有销售记录时自动下架）")
    public ApiResponse<String> deleteProduct(@PathVariable Long productId) {
        return ApiResponse.ok(salesService.deleteProduct(productId));
    }

    @GetMapping("/sales")
    @Operation(summary = "商品销售订单列表")
    public ApiResponse<PageResult<SalesOrder>> orders(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                      @RequestParam(required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) Long memberId,
                                                      @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(salesService.orders(page, size, from, to, status, memberId, keyword));
    }

    @PostMapping("/sales")
    @AdminLog(module = "SALES", action = "CREATE_SALE", targetType = "SALES_ORDER", detail = "前台商品销售")
    @Operation(summary = "创建商品销售单（扣库存、计价、支付）")
    public ApiResponse<SalesOrder> createOrder(@Valid @RequestBody SalesCreateRequest request) {
        return ApiResponse.ok("销售单创建成功", salesService.createOrder(request, CurrentUser.id()));
    }

    @GetMapping("/sales/statistics")
    @Operation(summary = "销售统计（场地租赁收入与商品收入分列，按日/月）")
    public ApiResponse<SalesStatisticsResponse> statistics(@RequestParam
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                           @RequestParam(defaultValue = "day") String groupBy) {
        return ApiResponse.ok(salesService.statistics(from, to, groupBy));
    }

    @GetMapping("/sales/{salesOrderId}")
    @Operation(summary = "销售订单详情")
    public ApiResponse<SalesOrder> orderDetail(@PathVariable Long salesOrderId) {
        return ApiResponse.ok(salesService.orderDetail(salesOrderId));
    }
}
