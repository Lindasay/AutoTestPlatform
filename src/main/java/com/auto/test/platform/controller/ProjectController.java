package com.auto.test.platform.controller;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.entity.Project;
import com.auto.test.platform.service.ProjectService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 项目管理Controller(接口层，架构controller层核心)
 * @Tag:Knife4注解，标识接口模块，用于接口文档分类
 */
@RestController
@RequestMapping("/project") //接口统一前缀（与context-path一致）+模块前缀
@Tag(name ="项目管理接口", description = "项目新增、修改、删除、查询、分页等接口，核心业务接口")
public class ProjectController {

    //注入项目Service（调用业务逻辑）
    @Resource
    private ProjectService projectService;

    /**
     * 新增项目接口
     * @Operation：接口描述，Knife4j接口文档展示
     * @RequestBody：接收前端传入的JSON格式项目实体
     */
    @PostMapping("/add")
    @Operation(summary = "新增项目", description = "传入项目名称、描述、状态，完成项目新增，校验项目名称唯一")
    public Result<?> addProject(@RequestBody Project project) {
      return projectService.addProject(project);
    }

    /**
     * 修改项目接口
     * @param project
     * @return
     */
    @PutMapping("/update")
    @Operation(summary = "修改项目", description = "传入项目ID及修改后的信息，完成项目修改，校验项目存在、唯一")
    public Result<?> updateProject(@RequestBody Project project) {
        return projectService.updateProject(project);
    }

    /**
     * @PathVariable：接收路径中的参数（项目ID）
     * @param id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除项目", description = "传入项目ID，删除项目，禁止删除关联有用例的项目")
    public Result<?> deleteProject(@Parameter(name="id",description = "项目ID", required = true)
                                   @PathVariable Long id) {
        return projectService.deleteProject(id);
    }

    /**
     * @PathVariable：接收路径中的参数（项目ID）
     * @param id
     * @return
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "根据ID查询项目",description = "传入项目ID，查询项目完整详情，校验项目存在")
    public Result<Project> getProjectById(@Parameter(name="id",description = "项目Id",required = true)
                                   @PathVariable Long id) {
        return projectService.getProjectById(id);
   }

    /**
     * 项目分页查询接口
     * @RequestParam：接收URL查询参数（分页参数、模糊查询参数），可设置默认值
     * @param pageNum
     * @param pageSize
     * @param projectName
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "项目分页查询", description = "传入页码、每页条数、项目名称（可选），完成分页模糊查询")
    public Result<IPage<Project>> getProjectPage(@Parameter(name ="pageNum", description = "当前页码，默认1",required = false)
                                                 @RequestParam(required = false) Integer pageNum,
                                                 @Parameter(name="pageSize",description = "每页条数，默认10，最大100",required = false)
                                                 @RequestParam(required = false) Integer pageSize,
                                                 @Parameter(name = "projectName",description = "项目名称(模糊查询，可选)")
                                                 @RequestParam(required = false) String projectName){
        return projectService.getProjectPage(pageNum, pageSize, projectName);
    }

    /**
     * 修改项目状态接口
     * @param id
     * @param status
     * @return
     */
    @PutMapping("/update-status/{id}/{status}")
    @Operation(summary = "修改项目状态",description = "传入项目ID、目标状态（0-禁用，1-启用），修改项目状态")
    public  Result<?> updateProjectStatus(@Parameter(name = "id",description = "项目ID", required = true)
                                          @PathVariable Long id,
                                          @Parameter(name = "status", description = "目标状态（0-禁用，1-启用）", required = true)
                                          @PathVariable Integer status) {
        return projectService.updateProjectStatus(id, status);
    }


}
