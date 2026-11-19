package com.cqust.repair.module.basedata.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.module.basedata.dto.BuildingVO;
import com.cqust.repair.module.basedata.dto.FaultTypeNodeVO;
import com.cqust.repair.module.basedata.dto.RoomVO;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.basedata.service.FaultTypeService;
import com.cqust.repair.module.basedata.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 基础数据接口：三端可读的楼栋 / 房间 / 故障类型字典。
 *
 * @author school-repair
 */
@Tag(name = "基础数据")
@RestController
@RequestMapping("/api/v1/base")
@RequiredArgsConstructor
public class BaseDataController {

    private final BuildingService buildingService;
    private final RoomService roomService;
    private final FaultTypeService faultTypeService;

    @Operation(summary = "楼栋列表")
    @GetMapping("/buildings")
    public R<List<BuildingVO>> buildings() {
        return R.success(buildingService.listAll());
    }

    @Operation(summary = "某楼栋房间列表")
    @GetMapping("/rooms")
    public R<List<RoomVO>> rooms(@RequestParam Long buildingId) {
        return R.success(roomService.listByBuilding(buildingId));
    }

    @Operation(summary = "故障类型树（两级）")
    @GetMapping("/fault-types")
    public R<List<FaultTypeNodeVO>> faultTypes() {
        return R.success(faultTypeService.tree());
    }
}
