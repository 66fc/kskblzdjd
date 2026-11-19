package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE product SET stock = stock - #{quantity} " +
            "WHERE product_id = #{productId} AND stock >= #{quantity} AND status = 'ON_SALE'")
    int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
