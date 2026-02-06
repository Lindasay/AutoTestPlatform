/*
 * 企业级自动化测试平台MySQL建表脚本
 * 创建时间：2026-02-01
 * 适配版本：MySQL 5.7+/8.0
 * 字符集：utf8mb4（适配所有字符，包含emoji）
 */

-- 1. 创建数据库（不存在则创建）
CREATE DATABASE IF NOT EXISTS test_platform 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE test_platform;

-- 2. 创建项目表（project）
DROP TABLE IF EXISTS project;
CREATE TABLE IF NOT EXISTS project (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID，自增主键',
                                       project_name VARCHAR(100) NOT NULL COMMENT '项目名称，唯一',
    project_desc VARCHAR(500) DEFAULT '' COMMENT '项目描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '项目状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_project_name (project_name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试项目表';

-- 3. 创建测试用例表（test_case）
DROP TABLE IF EXISTS test_case;
CREATE TABLE IF NOT EXISTS test_case (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用例ID，自增主键',
                                         project_id BIGINT NOT NULL COMMENT '所属项目ID，关联project表id',
                                         case_name VARCHAR(200) NOT NULL COMMENT '用例名称',
    case_type TINYINT NOT NULL COMMENT '用例类型：1-接口用例，2-UI用例',
    case_content TEXT NOT NULL COMMENT '用例内容（JSON格式，存储请求信息/操作步骤）',
    case_tag VARCHAR(100) DEFAULT '' COMMENT '用例标签，如冒烟/回归，逗号分隔',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '用例状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_project_id (project_id),
    KEY idx_case_type (case_type),
    KEY idx_case_tag (case_tag)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';

-- 4. 创建任务执行表（task_execution）
DROP TABLE IF EXISTS task_execution;
CREATE TABLE IF NOT EXISTS task_execution (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID，自增主键',
                                              project_id BIGINT NOT NULL COMMENT '所属项目ID，关联project表id',
                                              task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    execute_scope VARCHAR(500) NOT NULL COMMENT '执行范围（项目/用例集/单个用例）',
    execute_status TINYINT NOT NULL COMMENT '执行状态：0-待执行，1-执行中，2-执行完成，3-执行失败',
    case_total INT NOT NULL DEFAULT 0 COMMENT '总用例数',
    case_success INT NOT NULL DEFAULT 0 COMMENT '成功用例数',
    case_fail INT NOT NULL DEFAULT 0 COMMENT '失败用例数',
    execute_start_time DATETIME DEFAULT NULL COMMENT '执行开始时间',
    execute_end_time DATETIME DEFAULT NULL COMMENT '执行结束时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_project_id (project_id),
    KEY idx_execute_status (execute_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行表';

-- 5. 创建报告数据表（report_data）
DROP TABLE IF EXISTS report_data;
CREATE TABLE IF NOT EXISTS report_data (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID，自增主键',
                                           task_id BIGINT NOT NULL COMMENT '关联任务执行表id',
                                           pass_rate DECIMAL(5,2) NOT NULL COMMENT '用例通过率',
    fail_detail TEXT COMMENT '失败详情（JSON格式）',
    report_path VARCHAR(500) NOT NULL COMMENT 'Allure报告存储路径',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_task_id (task_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告数据表';

-- 6. 验证表创建结果
SHOW TABLES;
SELECT '所有表创建完成' AS result;