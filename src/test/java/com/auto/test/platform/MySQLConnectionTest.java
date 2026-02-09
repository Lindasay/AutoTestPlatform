package com.auto.test.platform;

import com.auto.test.platform.entity.Project;
import com.auto.test.platform.mapper.ProjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MySQLConnectionTest {

	@Autowired
	private ProjectMapper projectMapper;

	// 测试数据库连接+新增数据
	@Test
	public void testInsert() {
		// 1. 创建对象，只设置必填字段
		Project project = new Project();
		project.setProjectName("落地验证项目");
		project.setProjectDesc("验证MySQL整合是否可落地");
		project.setStatus(1);

		// 2. 插入（无需手动设置时间）
		int result = projectMapper.insert(project);

		// 3. 打印结果
		System.out.println("插入行数：" + result);
		System.out.println("自动生成的ID：" + project.getId());
		System.out.println("自动填充的创建时间：" + project.getCreateTime());
	}

	// 测试查询数据
	@Test
	public void testSelect() {
		// 先新增一条数据
		Project insertProject = new Project();
		insertProject.setProjectName("查询测试");
		insertProject.setProjectDesc("Mapper查询验证");
		insertProject.setStatus(1);
		projectMapper.insert(insertProject);

		// 根据ID查询
		Project selectProject = projectMapper.selectById(insertProject.getId());
		Assertions.assertEquals("查询测试", selectProject.getProjectName());
		System.out.println("查询结果：" + selectProject);
	}
}
