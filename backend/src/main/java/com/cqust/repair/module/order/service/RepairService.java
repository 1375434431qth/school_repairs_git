package com.cqust.repair.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.order.dto.RepairRecordVO;
import com.cqust.repair.module.order.entity.PartUsage;
import com.cqust.repair.module.order.entity.RepairRecord;
import com.cqust.repair.module.order.mapper.PartUsageMapper;
import com.cqust.repair.module.order.mapper.RepairRecordMapper;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 维修过程原子服务：按轮次保存维修记录、配件整单覆盖、完工信息回写。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class RepairService {

    private final RepairRecordMapper repairRecordMapper;
    private final PartUsageMapper partUsageMapper;
    private final UserService userService;

    /**
     * 查询工单某一轮的维修记录，可能为空。
     */
    public RepairRecord getByOrder(Long orderId, int roundNo) {
        return repairRecordMapper.selectOne(new QueryWrapper<RepairRecord>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .last("LIMIT 1"));
    }

    /**
     * 查询工单最新一轮的维修记录。
     */
    public RepairRecord getLatest(Long orderId) {
        return repairRecordMapper.selectOne(new QueryWrapper<RepairRecord>()
                .eq("order_id", orderId)
                .orderByDesc("round_no")
                .last("LIMIT 1"));
    }

    /**
     * 某一轮的配件明细。
     */
    public List<PartUsage> listParts(Long orderId, int roundNo) {
        List<PartUsage> parts = partUsageMapper.selectList(new QueryWrapper<PartUsage>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .orderByAsc("id"));
        return parts == null ? Collections.emptyList() : parts;
    }

    /**
     * 某一轮是否已登记配件（BR-06 完工前置校验）。
     */
    public boolean hasParts(Long orderId, int roundNo) {
        Long count = partUsageMapper.selectCount(new QueryWrapper<PartUsage>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo));
        return count != null && count > 0;
    }

    /**
     * 保存某一轮维修过程并整轮覆盖配件（BR-05）。
     */
    public void saveRecord(Long orderId, int roundNo, Long repairerId, String processDesc,
                           boolean noPartsFlag, List<PartUsage> parts) {
        RepairRecord existing = getByOrder(orderId, roundNo);
        if (existing == null) {
            RepairRecord record = new RepairRecord();
            record.setOrderId(orderId);
            record.setRoundNo(roundNo);
            record.setRepairerId(repairerId);
            record.setProcessDesc(processDesc);
            record.setNoPartsFlag(noPartsFlag ? 1 : 0);
            repairRecordMapper.insert(record);
        } else {
            existing.setRepairerId(repairerId);
            existing.setProcessDesc(processDesc);
            existing.setNoPartsFlag(noPartsFlag ? 1 : 0);
            if (repairRecordMapper.updateById(existing) == 0) {
                throw new BizException(ResultCode.CONCURRENT_MODIFIED);
            }
        }
        partUsageMapper.delete(new QueryWrapper<PartUsage>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo));
        if (parts != null) {
            for (PartUsage part : parts) {
                part.setId(null);
                part.setOrderId(orderId);
                part.setRoundNo(roundNo);
                partUsageMapper.insert(part);
            }
        }
    }

    /**
     * 回写某一轮完工说明与维修耗时。
     */
    public void complete(Long orderId, int roundNo, String resultDesc, int costMinutes) {
        RepairRecord existing = getByOrder(orderId, roundNo);
        if (existing == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "请先填写维修过程");
        }
        existing.setResultDesc(resultDesc);
        existing.setCostMinutes(costMinutes);
        if (repairRecordMapper.updateById(existing) == 0) {
            throw new BizException(ResultCode.CONCURRENT_MODIFIED);
        }
    }

    /**
     * 某一轮维修记录展示对象（含配件与维修工姓名）。
     */
    public RepairRecordVO getVO(Long orderId, int roundNo) {
        RepairRecord record = getByOrder(orderId, roundNo);
        if (record == null) {
            return null;
        }
        RepairRecordVO vo = new RepairRecordVO();
        vo.setId(record.getId());
        vo.setRepairerId(record.getRepairerId());
        SysUser repairer = userService.findById(record.getRepairerId());
        vo.setRepairerName(repairer == null ? null : repairer.getRealName());
        vo.setProcessDesc(record.getProcessDesc());
        vo.setResultDesc(record.getResultDesc());
        vo.setNoPartsFlag(record.getNoPartsFlag());
        vo.setCostMinutes(record.getCostMinutes());
        vo.setUpdateTime(record.getUpdateTime());
        List<RepairRecordVO.PartUsageVO> partVOs = new ArrayList<>();
        for (PartUsage part : listParts(orderId, roundNo)) {
            RepairRecordVO.PartUsageVO partVO = new RepairRecordVO.PartUsageVO();
            partVO.setId(part.getId());
            partVO.setPartName(part.getPartName());
            partVO.setQuantity(part.getQuantity());
            partVO.setUnit(part.getUnit());
            partVO.setUnitPrice(part.getUnitPrice());
            partVOs.add(partVO);
        }
        vo.setParts(partVOs);
        return vo;
    }
}
