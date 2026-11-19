package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.badminton.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {

    IPage<AdminOperationLog> selectPageFiltered(IPage<AdminOperationLog> page,
                                                @Param("module") String module,
                                                @Param("keyword") String keyword,
                                                @Param("date") LocalDate date);
}
