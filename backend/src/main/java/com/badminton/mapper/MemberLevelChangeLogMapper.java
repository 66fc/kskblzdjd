package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.entity.MemberLevelChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberLevelChangeLogMapper extends BaseMapper<MemberLevelChangeLog> {

    List<MemberLevelChangeLog> selectByMember(@Param("memberId") Long memberId);

    List<MemberLevelChangeLog> selectRecent(@Param("limit") int limit);
}
