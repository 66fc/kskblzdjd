package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.dto.AvailabilityRow;
import com.badminton.entity.CourtSlot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CourtSlotMapper extends BaseMapper<CourtSlot> {

    /**
     * 锁定预约区间内的所有时间段，防止并发重复预约。
     */
    @Select("SELECT * FROM court_slot " +
            "WHERE court_id = #{courtId} AND slot_start >= #{start} AND slot_end <= #{end} " +
            "ORDER BY slot_start FOR UPDATE")
    List<CourtSlot> lockRange(@Param("courtId") Long courtId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /**
     * 依赖视图 v_available_court_slots 查询区间内完整空闲的场地。
     */
    @Select("SELECT venue_id, venue_name, court_id, court_name, hourly_rate, COUNT(*) AS free_slots " +
            "FROM v_available_court_slots " +
            "WHERE slot_start >= #{start} AND slot_end <= #{end} " +
            "GROUP BY venue_id, venue_name, court_id, court_name, hourly_rate " +
            "HAVING COUNT(*) = #{neededSlots} " +
            "ORDER BY hourly_rate, court_id")
    List<AvailabilityRow> selectAvailableCourts(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("neededSlots") int neededSlots);

    @Select("SELECT * FROM court_slot WHERE court_id = #{courtId} " +
            "AND slot_start >= #{start} AND slot_end <= #{end} ORDER BY slot_start")
    List<CourtSlot> selectRange(@Param("courtId") Long courtId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    @Select("SELECT * FROM court_slot WHERE court_id = #{courtId} AND DATE(slot_start) = #{date} ORDER BY slot_start")
    List<CourtSlot> selectByCourtAndDate(@Param("courtId") Long courtId, @Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM court_slot WHERE court_id = #{courtId} " +
            "AND slot_start >= #{start} AND slot_end <= #{end}")
    int countRange(@Param("courtId") Long courtId,
                   @Param("start") LocalDateTime start,
                   @Param("end") LocalDateTime end);

    @Insert("<script>" +
            "INSERT IGNORE INTO court_slot (court_id, slot_start, slot_end, slot_status) VALUES " +
            "<foreach collection='starts' item='s' separator=','>" +
            "(#{courtId}, #{s}, DATE_ADD(#{s}, INTERVAL 30 MINUTE), 'AVAILABLE')" +
            "</foreach>" +
            "</script>")
    int insertIgnoreSlots(@Param("courtId") Long courtId, @Param("starts") List<LocalDateTime> starts);

    /**
     * 调用存储过程批量生成时间段（后台"生成未来时间段"与系统初始化使用）。
     */
    @Select("{CALL sp_generate_court_slots(#{from}, #{to})}")
    Map<String, Object> callGenerateSlots(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("SELECT COUNT(*) FROM court_slot WHERE DATE(slot_start) = #{date} " +
            "AND slot_status IN ('BOOKED','IN_USE')")
    long countBookedSlots(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM court_slot s JOIN court c ON c.court_id = s.court_id " +
            "WHERE s.slot_start <= #{now} AND s.slot_end > #{now} " +
            "AND s.slot_status = 'AVAILABLE' AND c.operational_status = 'OPEN'")
    long countFreeCourtsNow(@Param("now") LocalDateTime now);

    @Select("SELECT COUNT(DISTINCT s.court_id) FROM court_slot s " +
            "WHERE s.slot_start <= #{now} AND s.slot_end > #{now} AND s.slot_status = 'IN_USE'")
    long countInUseCourtsNow(@Param("now") LocalDateTime now);
}
