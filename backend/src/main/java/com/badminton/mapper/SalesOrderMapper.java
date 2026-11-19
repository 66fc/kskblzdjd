package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.badminton.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    IPage<SalesOrder> selectPageWithMember(IPage<SalesOrder> page,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           @Param("status") String status,
                                           @Param("memberId") Long memberId,
                                           @Param("keyword") String keyword);

    SalesOrder selectDetail(@Param("salesOrderId") Long salesOrderId);
}
