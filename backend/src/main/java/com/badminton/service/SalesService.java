package com.badminton.service;

import com.badminton.common.BizException;
import com.badminton.common.PageResult;
import com.badminton.dto.SalesCreateRequest;
import com.badminton.dto.SalesStatisticsResponse;
import com.badminton.dto.DailyRevenueRow;
import com.badminton.dto.ProductRequest;
import com.badminton.entity.Member;
import com.badminton.entity.Product;
import com.badminton.entity.SalesOrder;
import com.badminton.entity.SalesOrderItem;
import com.badminton.mapper.MemberMapper;
import com.badminton.mapper.ProductMapper;
import com.badminton.mapper.ReportMapper;
import com.badminton.mapper.SalesOrderItemMapper;
import com.badminton.mapper.SalesOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SalesService {

    private final ProductMapper productMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper itemMapper;
    private final MemberMapper memberMapper;
    private final ReportMapper reportMapper;
    private final MemberService memberService;
    private final SimulatedClockService clock;

    public SalesService(ProductMapper productMapper, SalesOrderMapper salesOrderMapper,
                        SalesOrderItemMapper itemMapper, MemberMapper memberMapper,
                        ReportMapper reportMapper, MemberService memberService,
                        SimulatedClockService clock) {
        this.productMapper = productMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.itemMapper = itemMapper;
        this.memberMapper = memberMapper;
        this.reportMapper = reportMapper;
        this.memberService = memberService;
        this.clock = clock;
    }

    public List<Product> products(String keyword, String category, String status) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .like(keyword != null && !keyword.isBlank(), Product::getProductName, keyword)
                .eq(category != null && !category.isBlank(), Product::getCategory, category)
                .eq(status != null && !status.isBlank(), Product::getStatus, status)
                .orderByAsc(Product::getProductId));
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        long same = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getProductName, request.getProductName().trim()));
        if (same > 0) {
            throw BizException.conflict("商品名称已存在");
        }
        Product product = new Product();
        apply(product, request);
        productMapper.insert(product);
        return productMapper.selectById(product.getProductId());
    }

    @Transactional
    public Product updateProduct(Long productId, ProductRequest request) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        long same = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getProductName, request.getProductName().trim())
                .ne(Product::getProductId, productId));
        if (same > 0) {
            throw BizException.conflict("商品名称已存在");
        }
        apply(product, request);
        productMapper.updateById(product);
        return productMapper.selectById(productId);
    }

    @Transactional
    public String deleteProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        long used = itemMapper.selectCount(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getProductId, productId));
        if (used > 0) {
            product.setStatus("OFF_SALE");
            productMapper.updateById(product);
            return "商品已产生销售记录，已下架（逻辑删除）";
        }
        productMapper.deleteById(productId);
        return "商品已删除";
    }

    public PageResult<SalesOrder> orders(long page, long size, LocalDate from, LocalDate to,
                                         String status, Long memberId, String keyword) {
        IPage<SalesOrder> result = salesOrderMapper.selectPageWithMember(new Page<>(page, size),
                from, to, status, memberId, keyword);
        result.getRecords().forEach(order -> {
            order.setItems(itemMapper.selectByOrder(order.getSalesOrderId()));
        });
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public SalesOrder orderDetail(Long salesOrderId) {
        SalesOrder order = salesOrderMapper.selectDetail(salesOrderId);
        if (order == null) {
            throw BizException.notFound("销售订单不存在");
        }
        order.setItems(itemMapper.selectByOrder(salesOrderId));
        return order;
    }

    /**
     * 创建商品销售单：服务端按数据库价格计价、扣减库存、记录支付。
     * 商品收入与场地租赁收入在统计上严格分离。
     */
    @Transactional
    public SalesOrder createOrder(SalesCreateRequest request, Long operatorId) {
        Member member = null;
        if (request.getMemberId() != null) {
            member = memberMapper.selectById(request.getMemberId());
            if (member == null) {
                throw BizException.notFound("会员不存在");
            }
        }

        LocalDateTime now = clock.now();
        BigDecimal total = BigDecimal.ZERO;
        List<SalesOrderItem> items = new ArrayList<>();

        for (SalesCreateRequest.Item reqItem : request.getItems()) {
            Product product = productMapper.selectById(reqItem.getProductId());
            if (product == null) {
                throw BizException.notFound("商品不存在：" + reqItem.getProductId());
            }
            if (!"ON_SALE".equals(product.getStatus())) {
                throw new BizException("商品已下架：" + product.getProductName());
            }
            if (product.getStock() < reqItem.getQuantity()) {
                throw BizException.conflict("库存不足：" + product.getProductName()
                        + " 当前库存 " + product.getStock());
            }
            int deducted = productMapper.deductStock(product.getProductId(), reqItem.getQuantity());
            if (deducted == 0) {
                throw BizException.conflict("库存扣减失败，请重试：" + product.getProductName());
            }
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(reqItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);

            SalesOrderItem item = new SalesOrderItem();
            item.setProductId(product.getProductId());
            item.setQuantity(reqItem.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(subtotal);
            items.add(item);
        }

        SalesOrder order = new SalesOrder();
        order.setOrderNo(generateOrderNo());
        order.setMemberId(request.getMemberId());
        order.setTotalAmount(total);
        order.setPaymentStatus("PAID");
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaidAt(now);
        order.setCreatedAt(now);
        salesOrderMapper.insert(order);

        for (SalesOrderItem item : items) {
            item.setSalesOrderId(order.getSalesOrderId());
            itemMapper.insert(item);
        }

        if (member != null) {
            memberService.addConsumptionAndUpgrade(member.getMemberId(), total);
        }

        return orderDetail(order.getSalesOrderId());
    }

    public SalesStatisticsResponse statistics(LocalDate from, LocalDate to, String groupBy) {
        if (from == null || to == null) {
            throw new BizException("统计起止日期不能为空");
        }
        if (to.isBefore(from)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        List<DailyRevenueRow> rental = reportMapper.selectDailyRentalTrend(from, to);
        List<DailyRevenueRow> product = reportMapper.selectDailyProductTrend(from, to);

        BigDecimal rentalTotal = rental.stream().map(DailyRevenueRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productTotal = product.stream().map(DailyRevenueRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SalesStatisticsResponse response = new SalesStatisticsResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setGroupBy(groupBy == null ? "day" : groupBy);
        response.setRentalSeries(rental);
        response.setProductSeries(product);
        response.setRentalTotal(rentalTotal);
        response.setProductTotal(productTotal);
        response.setGrandTotal(rentalTotal.add(productTotal));

        if ("month".equalsIgnoreCase(groupBy)) {
            Map<String, SalesStatisticsResponse.MonthlyPoint> map = new TreeMap<>();
            for (DailyRevenueRow row : rental) {
                String month = row.getStatDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                SalesStatisticsResponse.MonthlyPoint point = map.computeIfAbsent(month, key -> {
                    SalesStatisticsResponse.MonthlyPoint p = new SalesStatisticsResponse.MonthlyPoint();
                    p.setMonth(key);
                    p.setRental(BigDecimal.ZERO);
                    p.setProduct(BigDecimal.ZERO);
                    p.setTotal(BigDecimal.ZERO);
                    return p;
                });
                point.setRental(point.getRental().add(row.getAmount()));
            }
            for (DailyRevenueRow row : product) {
                String month = row.getStatDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                SalesStatisticsResponse.MonthlyPoint point = map.computeIfAbsent(month, key -> {
                    SalesStatisticsResponse.MonthlyPoint p = new SalesStatisticsResponse.MonthlyPoint();
                    p.setMonth(key);
                    p.setRental(BigDecimal.ZERO);
                    p.setProduct(BigDecimal.ZERO);
                    p.setTotal(BigDecimal.ZERO);
                    return p;
                });
                point.setProduct(point.getProduct().add(row.getAmount()));
            }
            map.values().forEach(p -> p.setTotal(p.getRental().add(p.getProduct())));
            response.setMonthlySeries(new ArrayList<>(map.values()));
        }
        return response;
    }

    /**
     * 商品销售额（按支付日期），用于数据总览。
     */
    public BigDecimal productRevenue(LocalDate date) {
        List<DailyRevenueRow> rows = reportMapper.selectDailyProductTrend(date, date);
        return rows.isEmpty() ? BigDecimal.ZERO : rows.get(0).getAmount();
    }

    public Map<String, Object> productSummary(LocalDate date) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", date);
        map.put("productRevenue", productRevenue(date));
        map.put("rentalRevenue", reportMapper.callDailySummary(date) == null
                ? BigDecimal.ZERO : reportMapper.callDailySummary(date).getTotalSalesAmount());
        return map;
    }

    private void apply(Product product, ProductRequest request) {
        product.setProductName(request.getProductName().trim());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setStatus(request.getStatus() == null || request.getStatus().isBlank()
                ? "ON_SALE" : request.getStatus());
        if (!List.of("ON_SALE", "OFF_SALE").contains(product.getStatus())) {
            throw new BizException("商品状态只能是 ON_SALE / OFF_SALE");
        }
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "SO" + timestamp + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
