package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.badminton.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    IPage<Member> selectPageWithLevel(IPage<Member> page,
                                      @Param("kw") String keyword,
                                      @Param("levelId") Long levelId,
                                      @Param("status") String status,
                                      @Param("role") String role);

    Member selectWithLevel(@Param("memberId") Long memberId);

    @Update("UPDATE member SET total_consumption = total_consumption + #{amount} " +
            "WHERE member_id = #{memberId}")
    int addConsumption(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    @Select("SELECT consume_type, ref_no, description, amount, consume_at " +
            "FROM v_member_consumption_history WHERE member_id = #{memberId} " +
            "ORDER BY consume_at DESC LIMIT 100")
    List<Map<String, Object>> selectConsumptionHistory(@Param("memberId") Long memberId);

    @Select("SELECT consume_type AS consumeType, ref_no AS refNo, description, " +
            "amount, consume_at AS consumeAt " +
            "FROM v_member_consumption_history WHERE member_id = #{memberId} " +
            "ORDER BY consume_at DESC LIMIT 200")
    List<com.badminton.dto.ConsumptionRecord> selectConsumptionRecords(@Param("memberId") Long memberId);
}
