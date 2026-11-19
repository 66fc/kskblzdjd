package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {

    List<MemberLevel> selectWithMemberCount();
}
