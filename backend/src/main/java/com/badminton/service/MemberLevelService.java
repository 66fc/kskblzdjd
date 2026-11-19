package com.badminton.service;

import com.badminton.common.BizException;
import com.badminton.entity.Member;
import com.badminton.entity.MemberLevel;
import com.badminton.mapper.MemberLevelMapper;
import com.badminton.mapper.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MemberLevelService {

    private final MemberLevelMapper levelMapper;
    private final MemberMapper memberMapper;

    public MemberLevelService(MemberLevelMapper levelMapper, MemberMapper memberMapper) {
        this.levelMapper = levelMapper;
        this.memberMapper = memberMapper;
    }

    public List<MemberLevel> list() {
        return levelMapper.selectWithMemberCount();
    }

    public List<MemberLevel> enabledLevels() {
        return levelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, "ENABLED")
                .orderByAsc(MemberLevel::getLevelRank));
    }

    @Transactional
    public MemberLevel create(MemberLevel level) {
        validate(level);
        long sameName = levelMapper.selectCount(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getLevelName, level.getLevelName()));
        if (sameName > 0) {
            throw BizException.conflict("等级名称已存在");
        }
        long sameRank = levelMapper.selectCount(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getLevelRank, level.getLevelRank()));
        if (sameRank > 0) {
            throw BizException.conflict("等级序号已存在");
        }
        level.setLevelId(null);
        if (level.getStatus() == null) {
            level.setStatus("ENABLED");
        }
        levelMapper.insert(level);
        return levelMapper.selectById(level.getLevelId());
    }

    @Transactional
    public MemberLevel update(Long levelId, MemberLevel level) {
        MemberLevel existing = levelMapper.selectById(levelId);
        if (existing == null) {
            throw BizException.notFound("会员等级不存在");
        }
        level.setLevelId(levelId);
        validate(level);
        long sameName = levelMapper.selectCount(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getLevelName, level.getLevelName())
                .ne(MemberLevel::getLevelId, levelId));
        if (sameName > 0) {
            throw BizException.conflict("等级名称已存在");
        }
        long sameRank = levelMapper.selectCount(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getLevelRank, level.getLevelRank())
                .ne(MemberLevel::getLevelId, levelId));
        if (sameRank > 0) {
            throw BizException.conflict("等级序号已存在");
        }
        levelMapper.updateById(level);
        return levelMapper.selectById(levelId);
    }

    @Transactional
    public String delete(Long levelId) {
        MemberLevel level = levelMapper.selectById(levelId);
        if (level == null) {
            throw BizException.notFound("会员等级不存在");
        }
        long used = memberMapper.selectCount(new LambdaQueryWrapper<Member>()
                .eq(Member::getLevelId, levelId));
        if (used > 0) {
            level.setStatus("DISABLED");
            levelMapper.updateById(level);
            return "该等级仍有 " + used + " 名会员使用，已停用而不是删除";
        }
        levelMapper.deleteById(levelId);
        return "等级已删除";
    }

    private void validate(MemberLevel level) {
        if (level.getLevelName() == null || level.getLevelName().isBlank()) {
            throw new BizException("等级名称不能为空");
        }
        if (level.getLevelRank() == null || level.getLevelRank() < 1) {
            throw new BizException("等级序号必须大于0");
        }
        if (level.getDiscountRate() == null
                || level.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                || level.getDiscountRate().compareTo(BigDecimal.ONE) > 0) {
            throw new BizException("折扣比例需大于0且不超过1");
        }
        if (level.getMinimumConsumption() == null
                || level.getMinimumConsumption().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("升级门槛不能为负数");
        }
        if (level.getStatus() == null) {
            level.setStatus("ENABLED");
        }
        if (!List.of("ENABLED", "DISABLED").contains(level.getStatus())) {
            throw new BizException("状态只能是 ENABLED / DISABLED");
        }
    }
}
