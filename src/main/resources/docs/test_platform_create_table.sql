-- 1. 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS test_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE test_platform;

-- 3. 创建项目表（对应entity/Project.java）
DROP TABLE IF EXISTS project;
CREATE TABLE project (
    id BIGINT AUTO_INCREMENT COMMENT '项目ID（主键）' PRIMARY KEY,
    project_name VARCHAR(50) NOT NULL COMMENT '项目名称（唯一，非空）',
    project_desc VARCHAR(200) DEFAULT '' COMMENT '项目描述',
    status INT NOT NULL DEFAULT 1 COMMENT '项目状态（0-禁用，1-启用）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_project_name (project_name) COMMENT '项目名称唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目管理表';

-- 4. 创建测试用例表（对应entity/TestCase.java，提前创建，第3天用）
DROP TABLE IF EXISTS test_case;
CREATE TABLE test_case (
    id BIGINT AUTO_INCREMENT COMMENT '用例ID（主键）' PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '关联项目ID（外键，关联project.id）',
    case_name VARCHAR(100) NOT NULL COMMENT '用例名称',
    case_type INT NOT NULL COMMENT '用例类型（1-接口用例，2-UI用例）',
    case_content TEXT NOT NULL COMMENT '用例内容（JSON格式存储）',
    status INT NOT NULL DEFAULT 1 COMMENT '用例状态（0-禁用，1-启用）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_project_id (project_id) COMMENT '项目ID索引，优化查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';

-- 5. 创建任务执行表（对应entity/TaskExecution.java，提前创建，后续用）
DROP TABLE IF EXISTS task_execution;
CREATE TABLE task_execution (
    id BIGINT AUTO_INCREMENT COMMENT '任务ID（主键）' PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '关联项目ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_status INT NOT NULL DEFAULT 0 COMMENT '任务状态（0-未执行，1-执行中，2-执行成功，3-执行失败）',
    execute_time DATETIME DEFAULT NULL COMMENT '执行时间',
    execute_duration INT DEFAULT 0 COMMENT '执行时长（单位：毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_project_id (project_id) COMMENT '项目ID索引',
    KEY idx_task_status (task_status) COMMENT '任务状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行表';

-- 6. 创建测试报告表（对应entity/ReportData.java，提前创建，后续用）
DROP TABLE IF EXISTS report_data;
CREATE TABLE report_data (
    id BIGINT AUTO_INCREMENT COMMENT '报告ID（主键）' PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联任务ID（外键，关联task_execution.id）',
    project_id BIGINT NOT NULL COMMENT '关联项目ID',
    pass_count INT NOT NULL DEFAULT 0 COMMENT '通过用例数',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败用例数',
    total_count INT NOT NULL DEFAULT 0 COMMENT '总用例数',
    pass_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '通过率（保留2位小数）',
    report_content TEXT DEFAULT NULL COMMENT '报告详情（JSON格式）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_task_id (task_id) COMMENT '任务ID索引',
    KEY idx_project_id (project_id) COMMENT '项目ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试报告表';

-- 插入测试数据（项目表，用于第2天MySQL连接测试）
INSERT INTO project (project_name, project_desc, status) VALUES 
('测试平台初始化项目', '企业级自动化测试平台初始化项目，用于测试MySQL连接', 1);