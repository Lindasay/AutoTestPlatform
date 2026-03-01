package com.auto.test.platform.controller;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.entity.TestCase;
import com.auto.test.platform.service.TestCaseService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 测试用例管理器（表现层，接口开发）
 * 遵循企业级接口规范，与ProjectController风格统一，调用TestCaseServie实现业务逻辑
 * 接口文档由Knife4j自动生成，支持接口调试、参数校验提示
 */
@RestController
@RequestMapping("/testCase")
@Tag(name = "测试用例管理接口", description = "用例新增、修改、删除、查询、分页等接口，核心业务接口")
public class TestCaseController {

    //注入测试用例Service，调用业务逻辑层方法（依赖注入，Spring容器自动管理）
    @Resource
    private TestCaseService testCaseService;

    /**
     * 新增测试用例
     * @param testCase 请求参数
     * @return testCase
     */
    @PostMapping("/add")
    @Operation(summary = "新增测试用例", description = "测试用例信息（JSON格式，需包含项目ID、用例名称等核心字段）")
    public Result<?> addTestCase(@RequestBody TestCase testCase) {
        return testCaseService.addTestCase(testCase);
    }

    /**
     * 修改测试用例
     * @param testCase 请求参数
     * @return testCase
     */
    @PutMapping("/update") // PUT请求，适配修改操作，与新增接口路径区分
    @Operation(summary = "修改测试用例", description = "传入用例ID及修改后的信息，完成测试用例修改，校验用例存在、唯一")
    public Result<?> updateTestCase(@RequestBody TestCase testCase) {
        return testCaseService.updateTestCase(testCase);
    }

    /**
     * 删除测试用例
     * @param id 请求参数
     * @return 返回删除确认信息
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除测试用例", description = "传入用例ID，删除用例")
    public Result<?> deleteTestCase(@Parameter(name = "id", description = "测试用例ID",required = true)
                                    @PathVariable Long id) {
        return testCaseService.deleteTestCase(id);
    }

    /**
     * 根据id查询测试用例
     * @param id
     * @return
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "根据用例ID查询测试用例", description = "传入用例ID，查询测试用例，校验测试用例存在")
    public  Result<TestCase> getTestCaseById(@Parameter(name = "id", description = "测试用例ID",required = true)
                                             @PathVariable Long id) {
        return testCaseService.getTestCaseById(id);
    }


    /**
     * 分页查询测试用例
     * @param pageNum 分页页码
     * @param pageSize 分页条数
     * @param projectId 关联项目ID
     * @param caseType 测试用例类型
     * @param caseName 测试用例名称
     * @return 测试用例
     */
    @GetMapping("/page")
    @Operation(summary = "测试用例分页查询",description = "传入页码、每页条数、项目ID、用例类型、用例名称，完成分页模糊查询")
    public Result<IPage<TestCase>> getTestCaseByPage(@Parameter(name = "pageNum",description = "当前页码，默认1", required = false)
                                                     @RequestParam(required = false) Integer pageNum,
                                                     @Parameter(name="pageSize",description = "每页条数，默认10，最大100",required = false )
                                                     @RequestParam(required = false) Integer pageSize,
                                                     @Parameter(name = "projectId",description = "关联项目ID",required = false) @RequestParam(required = false) Long projectId,
                                                     @Parameter(name = "caseType",description = "用例类型（可选，1-接口用例，2-UI用例）",required = false) @RequestParam(required = false) Integer caseType,
                                                     @Parameter(name = "caseName",description = "用例名称（可选，模糊查询）",required = false) @RequestParam(required = false) String caseName){
        return testCaseService.getTestCasePage(pageNum,pageSize,projectId,caseType,caseName);

    }

    /**
     * 修改测试用例状态
     * @param id 用例ID
     * @param status 目标状态
     * @return
     */
    @PutMapping("/update-status/{id}/{status}")
    @Operation(summary = "修改测试用例状态",description = "传入测试用例ID、目标状态（0-禁用，1-启用），修改用例状态")
    public Result<?> updateTestCaseStatus(@Parameter(name = "id",description = "测试用例ID",required = true)
                                          @PathVariable Long id,
                                          @Parameter(name = "status",description = "目标状态（0-禁用，1-启用）",required = true)
                                          @PathVariable Integer status){
        return testCaseService.updateTestCaseStatus(id,status);
    }
}
