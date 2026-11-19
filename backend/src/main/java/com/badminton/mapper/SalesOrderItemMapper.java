package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {

    @Select("SELECT i.*, p.product_name FROM sales_order_item i " +
            "JOIN product p ON p.product_id = i.product_id " +
            "WHERE i.sales_order_id = #{salesOrderId} ORDER BY i.item_id")
    List<SalesOrderItem> selectByOrder(@Param("salesOrderId") Long salesOrderId);
}
