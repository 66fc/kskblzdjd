package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.dto.CourtOverviewRow;
import com.badminton.entity.Court;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourtMapper extends BaseMapper<Court> {

    List<Court> selectWithVenue(@Param("status") String status, @Param("venueId") Long venueId);

    Court selectDetail(@Param("courtId") Long courtId);

    long countActiveBookings(@Param("courtId") Long courtId, @Param("now") LocalDateTime now);

    List<CourtOverviewRow> selectOverview(@Param("date") LocalDate date,
                                          @Param("now") LocalDateTime now);
}
