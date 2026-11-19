package com.badminton.service;

import com.badminton.common.BizException;
import com.badminton.common.PageResult;
import com.badminton.dto.LevelChangeRequest;
import com.badminton.dto.MemberUpdateRequest;
import com.badminton.entity.Booking;
import com.badminton.entity.CourtUsageRecord;
import com.badminton.entity.Member;
import com.badminton.entity.MemberLevel;
import com.badminton.entity.MemberLevelChangeLog;
import com.badminton.mapper.BookingMapper;
import com.badminton.mapper.CourtUsageRecordMapper;
import com.badminton.mapper.MemberLevelChangeLogMapper;
import com.badminton.mapper.MemberLevelMapper;
import com.badminton.mapper.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MemberService {

    private final MemberMapper memberMapper;
    private final MemberLevelMapper levelMapper;
    private final MemberLevelChangeLogMapper changeLogMapper;
    private final BookingMapper bookingMapper;
    private final CourtUsageRecordMapper usageRecordMapper;
    private final SimulatedClockService clock;

    public MemberService(MemberMapper memberMapper, MemberLevelMapper levelMapper,
                         MemberLevelChangeLogMapper changeLogMapper, BookingMapper bookingMapper,
                         CourtUsageRecordMapper usageRecordMapper, SimulatedClockService clock) {
        this.memberMapper = memberMapper;
        this.levelMapper = levelMapper;
        this.changeLogMapper = changeLogMapper;
        this.bookingMapper = bookingMapper;
        this.usageRecordMapper = usageRecordMapper;
        this.clock = clock;
    }

    public PageResult<Member> page(long page, long size, String keyword, Long levelId, String status) {
        IPage<Member> result = memberMapper.selectPageWithLevel(new Page<>(page, size), keyword, levelId, status, "MEMBER");
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Map<String, Object> detail(Long memberId) {
        Member member = memberMapper.selectWithLevel(memberId);
        if (member == null) {
            throw BizException.notFound("用户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("member", member);
        result.put("bookingCount", bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getMemberId, memberId)));
        result.put("usageCount", usageRecordMapper.countByMember(memberId));
        result.put("consumptionHistory", memberMapper.selectConsumptionHistory(memberId));
        result.put("levelHistory", changeLogMapper.selectByMember(memberId));
        return result;
    }

    @Transactional
    public Member update(Long memberId, MemberUpdateRequest request) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BizException.notFound("用户不存在");
        }
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            member.setNickname(request.getNickname().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(member.getPhone())) {
            long same = memberMapper.selectCount(new LambdaQueryWrapper<Member>()
                    .eq(Member::getPhone, request.getPhone())
                    .ne(Member::getMemberId, memberId));
            if (same > 0) {
                throw BizException.conflict("手机号已被其他用户使用");
            }
            member.setPhone(request.getPhone());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            if (!List.of("ACTIVE", "DISABLED").contains(request.getStatus())) {
                throw new BizException("用户状态只能是 ACTIVE / DISABLED");
            }
            if (member.getRole().equals("ADMIN") && "DISABLED".equals(request.getStatus())) {
                throw new BizException("不能禁用管理员账号");
            }
            member.setStatus(request.getStatus());
        }
        memberMapper.updateById(member);
        return memberMapper.selectWithLevel(memberId);
    }

    /**
     * 管理员手动调整会员等级：写入变更日志，并标记为人工指定。
     * 人工指定后自动升级逻辑不再覆盖，直到管理员恢复自动升级。
     */
    @Transactional
    public Member changeLevel(Long memberId, LevelChangeRequest request, Long operatorId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BizException.notFound("用户不存在");
        }
        if ("ADMIN".equals(member.getRole())) {
            throw new BizException("不能调整管理员账号的会员等级");
        }
        MemberLevel target = levelMapper.selectById(request.getLevelId());
        if (target == null) {
            throw BizException.notFound("目标会员等级不存在");
        }
        if (!"ENABLED".equals(target.getStatus())) {
            throw new BizException("目标会员等级已停用");
        }
        if (target.getLevelId().equals(member.getLevelId())) {
            throw new BizException("会员当前已是该等级，无需修改");
        }

        Long oldLevelId = member.getLevelId();
        member.setLevelId(target.getLevelId());
        member.setLevelManuallyAdjusted(true);
        member.setManualAdjustedAt(clock.now());
        member.setManualAdjustedBy(operatorId);
        memberMapper.updateById(member);

        MemberLevelChangeLog log = new MemberLevelChangeLog();
        log.setMemberId(memberId);
        log.setOldLevelId(oldLevelId);
        log.setNewLevelId(target.getLevelId());
        log.setOperatorId(operatorId);
        log.setChangeType("MANUAL");
        log.setReason(request.getReason().trim());
        log.setCreatedAt(clock.now());
        changeLogMapper.insert(log);

        return memberMapper.selectWithLevel(memberId);
    }

    /**
     * 恢复自动升级（清除人工指定标记），并立即按累计消费尝试升级。
     */
    @Transactional
    public Member resetManualLevel(Long memberId, Long operatorId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BizException.notFound("用户不存在");
        }
        if (!Boolean.TRUE.equals(member.getLevelManuallyAdjusted())) {
            return memberMapper.selectWithLevel(memberId);
        }
        member.setLevelManuallyAdjusted(false);
        member.setManualAdjustedAt(clock.now());
        member.setManualAdjustedBy(operatorId);
        memberMapper.updateById(member);

        MemberLevelChangeLog log = new MemberLevelChangeLog();
        log.setMemberId(memberId);
        log.setOldLevelId(member.getLevelId());
        log.setNewLevelId(member.getLevelId());
        log.setOperatorId(operatorId);
        log.setChangeType("MANUAL");
        log.setReason("恢复自动升级规则");
        log.setCreatedAt(clock.now());
        changeLogMapper.insert(log);

        autoUpgrade(memberId);
        return memberMapper.selectWithLevel(memberId);
    }

    public List<MemberLevelChangeLog> levelHistory(Long memberId) {
        return changeLogMapper.selectByMember(memberId);
    }

    public List<MemberLevelChangeLog> recentLevelChanges(int limit) {
        return changeLogMapper.selectRecent(Math.min(limit, 100));
    }

    /**
     * 累计消费并触发自动升级。人工指定等级的会员不会被覆盖。
     */
    @Transactional
    public void addConsumptionAndUpgrade(Long memberId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        memberMapper.addConsumption(memberId, amount);
        autoUpgrade(memberId);
    }

    @Transactional
    public void autoUpgrade(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null || !"ACTIVE".equals(member.getStatus()) || "ADMIN".equals(member.getRole())) {
            return;
        }
        if (Boolean.TRUE.equals(member.getLevelManuallyAdjusted())) {
            return;
        }
        MemberLevel currentLevel = levelMapper.selectById(member.getLevelId());
        List<MemberLevel> levels = levelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, "ENABLED")
                .le(MemberLevel::getMinimumConsumption, member.getTotalConsumption())
                .orderByDesc(MemberLevel::getLevelRank));
        if (levels.isEmpty()) {
            return;
        }
        MemberLevel best = levels.get(0);
        if (currentLevel != null && best.getLevelRank() <= currentLevel.getLevelRank()) {
            return;
        }
        member.setLevelId(best.getLevelId());
        memberMapper.updateById(member);

        MemberLevelChangeLog log = new MemberLevelChangeLog();
        log.setMemberId(memberId);
        log.setOldLevelId(currentLevel == null ? null : currentLevel.getLevelId());
        log.setNewLevelId(best.getLevelId());
        log.setOperatorId(null);
        log.setChangeType("AUTO");
        log.setReason("累计消费 " + member.getTotalConsumption() + " 元，达到自动升级门槛");
        log.setCreatedAt(clock.now());
        changeLogMapper.insert(log);
    }
}
