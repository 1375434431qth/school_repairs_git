package com.cqust.repair.module.order.service;

import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.order.dto.RepairerLoadVO;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修工负载查询：在手单数 + 今日完工数，供派单时均衡参考。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class RepairerLoadService {

    private final UserService userService;
    private final BuildingService buildingService;
    private final DispatchService dispatchService;

    /**
     * 查询维修工负载。
     *
     * @param buildingId       指定楼栋；为空表示全部
     * @param scopeBuildingIds 楼栋范围限制（管理员权限范围），为空表示不限制
     */
    public List<RepairerLoadVO> load(Long buildingId, Collection<Long> scopeBuildingIds) {
        List<SysUser> repairers = userService.listAvailableRepairers(buildingId);
        if (scopeBuildingIds != null && !scopeBuildingIds.isEmpty()) {
            repairers = repairers.stream()
                    .filter(item -> item.getBuildingId() != null && scopeBuildingIds.contains(item.getBuildingId()))
                    .toList();
        }
        if (repairers.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = repairers.stream().map(SysUser::getId).toList();
        List<Map<String, Object>> stats = dispatchService.loadStats(ids, LocalDate.now().atStartOfDay());
        Map<Long, int[]> statMap = new HashMap<>(stats.size());
        for (Map<String, Object> row : stats) {
            Long assigneeId = toLong(row.get("assigneeId"));
            if (assigneeId == null) {
                assigneeId = toLong(row.get("assignee_id"));
            }
            if (assigneeId == null) {
                continue;
            }
            statMap.put(assigneeId, new int[]{toInt(row.get("handCount")), toInt(row.get("todayFinishCount"))});
        }
        Map<Long, String> buildingNames = buildingService.nameMap();
        List<RepairerLoadVO> result = new ArrayList<>(repairers.size());
        for (SysUser repairer : repairers) {
            int[] stat = statMap.getOrDefault(repairer.getId(), new int[]{0, 0});
            RepairerLoadVO vo = new RepairerLoadVO();
            vo.setRepairerId(repairer.getId());
            vo.setUsername(repairer.getUsername());
            vo.setRealName(repairer.getRealName());
            vo.setBuildingId(repairer.getBuildingId());
            vo.setBuildingName(buildingNames.get(repairer.getBuildingId()));
            vo.setSkillTags(repairer.getSkillTags());
            vo.setHandCount(stat[0]);
            vo.setTodayFinishCount(stat[1]);
            result.add(vo);
        }
        result.sort((a, b) -> {
            int diff = Integer.compare(b.getHandCount(), a.getHandCount());
            return diff != 0 ? diff : Long.compare(a.getRepairerId(), b.getRepairerId());
        });
        return result;
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
