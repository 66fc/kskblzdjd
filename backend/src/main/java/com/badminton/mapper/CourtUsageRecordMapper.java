package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.badminton.entity.CourtUsageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface CourtUsageRecordMapper extends BaseMapper<CourtUsageRecord> {

    IPage<CourtUsageRecord> selectPageWithNames(IPage<CourtUsageRecord> page,
                                               @Param("courtId") Long courtId,
                                               @Param("memberId") Long memberId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM court_usage_record u " +
            "JOIN booking b ON b.booking_id = u.booking_id WHERE b.member_id = #{memberId}")
    long countByMember(@Param("memberId") Long memberId);
}
