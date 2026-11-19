package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.badminton.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    IPage<Booking> selectAdminPage(IPage<Booking> page,
                                   @Param("keyword") String keyword,
                                   @Param("memberId") Long memberId,
                                   @Param("courtId") Long courtId,
                                   @Param("status") String status,
                                   @Param("date") LocalDate date);

    IPage<Booking> selectMemberPage(IPage<Booking> page,
                                    @Param("memberId") Long memberId,
                                    @Param("status") String status,
                                    @Param("date") LocalDate date);

    @Select("SELECT status, COUNT(*) AS count FROM booking WHERE member_id = #{memberId} GROUP BY status")
    java.util.List<java.util.Map<String, Object>> selectStatusCounts(@Param("memberId") Long memberId);

    Booking selectDetail(@Param("bookingId") Long bookingId);

    long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByStatus(@Param("status") String status);

    @Select("SELECT * FROM booking WHERE booking_id = #{bookingId} FOR UPDATE")
    Booking selectForUpdate(@Param("bookingId") Long bookingId);
}
