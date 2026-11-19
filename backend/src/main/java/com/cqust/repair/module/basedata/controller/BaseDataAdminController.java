package com.cqust.repair.module.basedata.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.module.basedata.biz.BaseDataAdminBiz;
import com.cqust.repair.module.basedata.dto.BuildingSaveRequest;
import com.cqust.repair.module.basedata.dto.FaultTypeSaveRequest;
import com.cqust.repair.module.basedata.dto.RoomSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基础数据维护接口（管理员）：楼栋 / 房间 / 故障类型增改删。
 *
 * @author school-repair
 */
@Tag(name = "基础数据维护")
@RestController
@RequestMapping("/api/v1/base")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class BaseDataAdminController {

    private final BaseDataAdminBiz baseDataAdminBiz;

    @Operation(summary = "新增楼栋")
    @PostMapping("/buildings")
    public R<Long> createBuilding(@Valid @RequestBody BuildingSaveRequest request) {
        return R.success(baseDataAdminBiz.createBuilding(request));
    }

    @Operation(summary = "修改楼栋")
    @PutMapping("/buildings/{id}")
    public R<Void> updateBuilding(@PathVariable Long id, @Valid @RequestBody BuildingSaveRequest request) {
        baseDataAdminBiz.updateBuilding(id, request);
        return R.success();
    }

    @Operation(summary = "删除楼栋")
    @DeleteMapping("/buildings/{id}")
    public R<Void> deleteBuilding(@PathVariable Long id) {
        baseDataAdminBiz.deleteBuilding(id);
        return R.success();
    }

    @Operation(summary = "新增房间")
    @PostMapping("/rooms")
    public R<Long> createRoom(@Valid @RequestBody RoomSaveRequest request) {
        return R.success(baseDataAdminBiz.createRoom(request));
    }

    @Operation(summary = "修改房间")
    @PutMapping("/rooms/{id}")
    public R<Void> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomSaveRequest request) {
        baseDataAdminBiz.updateRoom(id, request);
        return R.success();
    }

    @Operation(summary = "删除房间")
    @DeleteMapping("/rooms/{id}")
    public R<Void> deleteRoom(@PathVariable Long id) {
        baseDataAdminBiz.deleteRoom(id);
        return R.success();
    }

    @Operation(summary = "新增故障类型")
    @PostMapping("/fault-types")
    public R<Long> createFaultType(@Valid @RequestBody FaultTypeSaveRequest request) {
        return R.success(baseDataAdminBiz.createFaultType(request));
    }

    @Operation(summary = "修改故障类型")
    @PutMapping("/fault-types/{id}")
    public R<Void> updateFaultType(@PathVariable Long id, @Valid @RequestBody FaultTypeSaveRequest request) {
        baseDataAdminBiz.updateFaultType(id, request);
        return R.success();
    }

    @Operation(summary = "删除故障类型")
    @DeleteMapping("/fault-types/{id}")
    public R<Void> deleteFaultType(@PathVariable Long id) {
        baseDataAdminBiz.deleteFaultType(id);
        return R.success();
    }
}
