-- 1. 创建数据库（不存在则创建）
CREATE DATABASE IF NOT EXISTS test_platform
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE test_platform;

-- 2. 创建项目表（project）
DROP TABLE IF EXISTS project;
CREATE TABLE IF NOT EXISTS project (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目主键ID（自增）',
                                       project_name VARCHAR(100) NOT NULL COMMENT '项目名称（唯一）',
    project_desc VARCHAR(500) DEFAULT '' COMMENT '项目描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '项目状态（1-启用，0-禁用）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_project_name (project_name) COMMENT '项目名称唯一约束'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试项目表';

-- 3. 创建测试用例表（test_case）
DROP TABLE IF EXISTS test_case;
CREATE TABLE IF NOT EXISTS test_case (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用例主键ID（自增）',
                                         project_id BIGINT NOT NULL COMMENT '关联项目ID（关联project表id）',
                                         case_name VARCHAR(200) NOT NULL COMMENT '用例名称',
    case_type TINYINT NOT NULL COMMENT '用例类型（1-接口用例，2-UI用例）',
    case_content TEXT NOT NULL COMMENT '用例内容（JSON格式，存储请求参数/元素定位等）',
    expect_result VARCHAR(500) NOT NULL COMMENT '预期结果',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '用例状态（1-启用，0-禁用）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_project_id (project_id) COMMENT '项目ID索引，提升查询效率'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';

-- 4. 创建任务执行表（task_execution）
DROP TABLE IF EXISTS task_execution;
CREATE TABLE IF NOT EXISTS task_execution (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务主键ID（自增）',
                                              task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    project_id BIGINT NOT NULL COMMENT '关联项目ID（关联project表id）',
    case_ids VARCHAR(1000) NOT NULL COMMENT '关联用例ID集合（逗号分隔，如1,2,3）',
    execute_status TINYINT NOT NULL DEFAULT 0 COMMENT '执行状态（0-未执行，1-执行中，2-执行成功，3-执行失败）',
    execute_time DATETIME DEFAULT NULL COMMENT '执行时间',
    execute_duration INT DEFAULT 0 COMMENT '执行耗时（单位：秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_project_id (project_id) COMMENT '项目ID索引',
    KEY idx_execute_status (execute_status) COMMENT '执行状态索引'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行表';

-- 5. 创建报告数据表（report_data）
DROP TABLE IF EXISTS report_data;
CREATE TABLE IF NOT EXISTS report_data (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告主键ID（自增）',
                                           task_id BIGINT NOT NULL COMMENT '关联任务ID（关联task_execution表id）',
                                           project_id BIGINT NOT NULL COMMENT '关联项目ID（关联project表id）',
                                           total_case INT NOT NULL COMMENT '总用例数',
                                           success_case INT NOT NULL COMMENT '成功用例数',
                                           fail_case INT NOT NULL COMMENT '失败用例数',
                                           success_rate DECIMAL(5,2) NOT NULL COMMENT '成功率（保留2位小数）',
    report_path VARCHAR(500) DEFAULT '' COMMENT 'Allure报告存储路径',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报告生成时间',
    KEY idx_task_id (task_id) COMMENT '任务ID索引',
    KEY idx_project_id (project_id) COMMENT '项目ID索引'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试报告表';
SHOW TABLES;
SELECT '所有表创建完成' AS result;