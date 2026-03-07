pipeline {
    agent any

    parameters {
        choice(
                name: 'BRANCH',
                choices: ['main', 'develop', 'feature/*'],
                description: '选择要构建的分支'
        )
        choice(
                name: 'ENVIRONMENT',
                choices: ['dev', 'test', 'prod'],
                description: '选择部署环境'
        )
        string(
                name: 'PROJECT_ID',
                defaultValue: '1',
                description: '要测试的项目ID'
        )
        string(
                name: 'CRON_EXPRESSION',
                defaultValue: '0 0 2 * * ?',
                description: 'Cron表达式（格式：秒 分 时 日 月 周）'
        )
        booleanParam(
                name: 'RUN_TESTS',
                defaultValue: true,
                description: '是否执行自动化测试'
        )
        booleanParam(
                name: 'GENERATE_REPORT',
                defaultValue: true,
                description: '是否生成测试报告'
        )
        booleanParam(
                name: 'CREATE_SCHEDULE',
                defaultValue: false,
                description: '是否创建定时任务'
        )
        string(
                name: 'API_BASE_URL',
                defaultValue: '',
                description: '自动化测试平台API基础URL'
        )
    }

    tools {
        maven 'Maven-3.9.12'
        jdk 'jdk-17'
    }

    environment {
        PROJECT_NAME = 'AutoTestPlatform'
        APP_PORT = '8080'

        // 从参数获取配置
        PROJECT_ID = "${params.PROJECT_ID}"
        CRON_EXPRESSION = "${params.CRON_EXPRESSION}"
        API_BASE_URL = "${params.API_BASE_URL}"

        // 报告路径配置
        ALLURE_RESULTS = 'target/allure-results'
        ALLURE_REPORT = 'target/allure-report'
        EXTENT_REPORT_DIR = 'target/reports'
        EXTENT_REPORT_HTML = 'target/reports/extent.html'

        // Jenkins构建信息
        JOB_NAME = "${env.JOB_NAME}"
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        BUILD_URL = "${env.BUILD_URL}"

        // Maven配置
        MAVEN_VERSION = '3.9.12'
        MAVEN_SETTINGS = 'settings.xml'

        // 项目信息
        GIT_URL = ''
    }

    stages {
        stage('环境检查') {
            steps {
                script {
                    echo "=== 环境检查 ==="
                    echo "项目ID: ${PROJECT_ID}"
                    echo "环境: ${ENVIRONMENT}"
                    echo "分支: ${BRANCH}"
                    echo "Cron表达式: ${CRON_EXPRESSION}"
                    echo "API基础URL: ${API_BASE_URL}"
                    echo "Maven版本: ${MAVEN_VERSION}"
                    echo "Extent报告目录: ${EXTENT_REPORT_DIR}"

                    sh """
                    echo "Java版本:"
                    java -version
                    echo ""
                    echo "Maven版本:"
                    mvn -v
                    """
                }
            }
        }

        stage('安装必要工具') {
            steps {
                script {
                    sh '''
                    echo "安装必要工具..."
                    
                    # 安装curl
                    if ! command -v curl &> /dev/null; then
                        echo "安装curl..."
                        sudo apt-get update && sudo apt-get install -y curl || true
                    fi
                    
                    # 安装jq
                    if ! command -v jq &> /dev/null; then
                        echo "安装jq..."
                        sudo apt-get install -y jq || true
                    fi
                    
                    echo "无需安装Allure，只使用ExtentReports"
                    
                    # 验证安装
                    curl --version
                    jq --version || echo "jq未安装"
                    '''
                }
            }
        }

        stage('配置Maven') {
            steps {
                script {
                    sh """
                    echo "配置Maven ${MAVEN_VERSION}..."
                    
                    # 创建Maven settings文件 - 修复XML命名空间
                    cat > ${MAVEN_SETTINGS} << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 
                              https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <localRepository>\${user.home}/.m2/repository</localRepository>
  <interactiveMode>true</interactiveMode>
  <offline>false</offline>
  <pluginGroups/>
  <servers/>
  <mirrors>
    <mirror>
      <id>aliyun-maven</id>
      <mirrorOf>*</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>dev</id>
      <properties>
        <spring.profiles.active>dev</spring.profiles.active>
      </properties>
    </profile>
    <profile>
      <id>test</id>
      <properties>
        <spring.profiles.active>test</spring.profiles.active>
      </properties>
    </profile>
    <profile>
      <id>prod</id>
      <properties>
        <spring.profiles.active>prod</spring.profiles.active>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>${ENVIRONMENT}</activeProfile>
  </activeProfiles>
</settings>
EOF
                    
                    echo "Maven配置完成"
                    echo "=== 验证Maven配置 ==="
                    ls -la ${MAVEN_SETTINGS}
                    
                    # 验证settings.xml格式
                    if command -v xmllint &> /dev/null; then
                        echo "验证XML格式..."
                        xmllint --format ${MAVEN_SETTINGS} > /dev/null && echo "✅ XML格式验证通过"
                    else
                        echo "⚠️ 未安装xmllint，跳过XML格式验证"
                    fi
                    """
                }
            }
        }

        stage('代码拉取') {
            steps {
                checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${BRANCH}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[
                                                    url: '${GIT_URL}',
                                                    credentialsId: 'git-credentials'
                                            ]]
                ])

                sh """
                echo "当前分支:"
                git branch -a
                echo ""
                echo "最新提交:"
                git log -1 --oneline
                """
            }
        }

        stage('依赖安装') {
            steps {
                sh """
                echo "使用Maven ${MAVEN_VERSION}安装依赖..."
                echo "Maven settings文件: ${MAVEN_SETTINGS}"
                
                mvn clean install -DskipTests \\
                    -s ${MAVEN_SETTINGS} \\
                    -P${ENVIRONMENT} \\
                    -Dmaven.test.skip=true
                    
                echo "依赖安装完成"
                
                # 验证有效settings
                echo "=== 验证Maven有效配置 ==="
                mvn help:effective-settings -s ${MAVEN_SETTINGS} 2>&1 | grep -A5 -B5 "activeProfile" || true
                """
            }
        }

        stage('启动应用') {
            steps {
                script {
                    sh """
                    echo "启动自动化测试平台应用..."
                    
                    # 先停止可能存在的旧进程
                    pkill -f "java.*AutoTestPlatform" 2>/dev/null || true
                    sleep 3
                    
                    # 启动应用
                    nohup mvn spring-boot:run \\
                        -Dspring-boot.run.profiles=${ENVIRONMENT} \\
                        -Dserver.port=${APP_PORT} \\
                        -s ${MAVEN_SETTINGS} \\
                        > app.log 2>&1 &
                    echo \$! > app.pid
                    echo "应用启动中，PID: \$(cat app.pid)"
                    
                    # 等待应用启动
                    for i in {1..30}; do
                        if curl -s "${API_BASE_URL}/actuator/health" > /dev/null 2>&1; then
                            echo "✅ 应用启动成功"
                            break
                        fi
                        echo "等待应用启动... (\$i/30)"
                        sleep 5
                    done
                    
                    if ! curl -s "${API_BASE_URL}/actuator/health" > /dev/null 2>&1; then
                        echo "❌ 应用启动失败"
                        echo "应用日志:"
                        tail -100 app.log
                        exit 1
                    fi
                    """
                }
            }
        }

        stage('验证API服务') {
            steps {
                script {
                    sh """
                    echo "验证自动化测试平台API服务..."
                    echo "API基础URL: ${API_BASE_URL}"
                    
                    # 健康检查
                    HEALTH_RESPONSE=\$(curl -s -o /dev/null -w "%{http_code}" \\
                        "${API_BASE_URL}/actuator/health" || echo "000")
                        
                    if [ "\$HEALTH_RESPONSE" = "200" ]; then
                        echo "✅ API服务健康检查通过"
                        
                        # 获取详细健康信息
                        echo "健康检查详情:"
                        curl -s "${API_BASE_URL}/actuator/health" | jq . || curl -s "${API_BASE_URL}/actuator/health"
                    else
                        echo "❌ API服务健康检查失败 (HTTP: \$HEALTH_RESPONSE)"
                        echo "应用日志:"
                        tail -50 app.log
                        exit 1
                    fi
                    """
                }
            }
        }

        stage('验证项目') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                script {
                    sh """
                    echo "验证项目ID: ${PROJECT_ID}..."
                    
                    # 验证项目是否存在
                    RESPONSE=\$(curl -s -o /dev/null -w "%{http_code}" \\
                        "${API_BASE_URL}/project/${PROJECT_ID}")
                        
                    if [ "\$RESPONSE" = "200" ]; then
                        echo "✅ 项目ID ${PROJECT_ID} 验证成功"
                        
                        # 获取项目详情
                        PROJECT_DETAIL=\$(curl -s "${API_BASE_URL}/project/${PROJECT_ID}")
                        echo "项目详情:"
                        echo "\$PROJECT_DETAIL" | jq . 2>/dev/null || echo "\$PROJECT_DETAIL"
                        
                    elif [ "\$RESPONSE" = "404" ]; then
                        echo "❌ 项目ID ${PROJECT_ID} 不存在"
                        # 尝试创建测试项目
                        echo "创建测试项目..."
                        PROJECT_CREATE_RESPONSE=\$(curl -s -w "\\n%{http_code}" \\
                            -X POST "${API_BASE_URL}/project/add" \\
                            -H "Content-Type: application/json" \\
                            -d '{
                                "projectName": "Jenkins测试项目_${PROJECT_ID}",
                                "projectDesc": "Jenkins自动创建的项目",
                                "status": 1
                            }')
                        PROJECT_CREATE_CODE=\$(echo "\$PROJECT_CREATE_RESPONSE" | tail -1)
                        
                        if [ "\$PROJECT_CREATE_CODE" = "200" ]; then
                            echo "✅ 测试项目创建成功"
                        else
                            echo "❌ 项目创建失败，响应: \$PROJECT_CREATE_RESPONSE"
                            exit 1
                        fi
                    else
                        echo "❌ 项目验证失败 (HTTP: \$RESPONSE)"
                        echo "应用日志:"
                        tail -50 app.log
                        exit 1
                    fi
                    """
                }
            }
        }

        stage('执行测试') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                script {
                    sh """
                    echo "开始执行项目 ${PROJECT_ID} 的测试..."
                    
                    # 调用批量执行接口
                    RESPONSE=\$(curl -s -w "\\n%{http_code}" \\
                        -X POST "${API_BASE_URL}/testCase/executeBatch/${PROJECT_ID}" \\
                        -H "Content-Type: application/json")
                    HTTP_CODE=\$(echo "\$RESPONSE" | tail -1)
                    BODY=\$(echo "\$RESPONSE" | sed '\$d')
                    
                    echo "响应状态码: \$HTTP_CODE"
                    if [ "\$HTTP_CODE" = "200" ]; then
                        echo "✅ 测试执行成功"
                        echo "执行响应:"
                        if command -v jq &> /dev/null; then
                            echo "\$BODY" | jq .
                        else
                            echo "\$BODY"
                        fi
                        
                        # 保存执行ID
                        if command -v jq &> /dev/null; then
                            EXECUTION_ID=\$(echo "\$BODY" | jq -r '.data.executionId' 2>/dev/null || echo "")
                            if [ -n "\$EXECUTION_ID" ]; then
                                echo "EXECUTION_ID=\$EXECUTION_ID" > execution.env
                            fi
                        fi
                    else
                        echo "❌ 测试执行失败: HTTP \$HTTP_CODE"
                        echo "响应内容: \$BODY"
                        echo "应用日志:"
                        tail -100 app.log
                        exit 1
                    fi
                    
                    echo "测试执行完成，等待ExtentReports生成..."
                    sleep 20
                    """
                }
            }
        }

        stage('获取Extent报告') {
            when {
                expression { params.GENERATE_REPORT == true }
            }
            steps {
                script {
                    sh """
                    echo "获取项目 ${PROJECT_ID} 的测试结果和报告..."
                    
                    # 查询最近的测试报告
                    RESPONSE=\$(curl -s \\
                        "${API_BASE_URL}/reportData/listByProjectId?projectId=${PROJECT_ID}")
                    
                    echo "原始响应: \$RESPONSE"
                    
                    # 解析JSON响应获取报告信息
                    if command -v jq &> /dev/null; then
                        REPORT_COUNT=\$(echo "\$RESPONSE" | jq 'length' 2>/dev/null || echo "0")
                        echo "解析的报告数量: \$REPORT_COUNT"
                    else
                        # 简单的计数方法
                        REPORT_COUNT=\$(echo "\$RESPONSE" | grep -o "\\"id\\":" | wc -l || echo "0")
                    fi
                    
                    echo "测试报告数量: \$REPORT_COUNT"
                    
                    if [ "\$REPORT_COUNT" -gt 0 ]; then
                        echo "获取最新测试报告详情..."
                        
                        if command -v jq &> /dev/null; then
                            LATEST_REPORT=\$(echo "\$RESPONSE" | jq '.[0]')
                            REPORT_ID=\$(echo "\$LATEST_REPORT" | jq -r '.id')
                            PASS_COUNT=\$(echo "\$LATEST_REPORT" | jq -r '.passCount // 0')
                            FAIL_COUNT=\$(echo "\$LATEST_REPORT" | jq -r '.failCount // 0')
                            PASS_RATE=\$(echo "\$LATEST_REPORT" | jq -r '.passRate // "0"')
                            EXTENT_URL=\$(echo "\$LATEST_REPORT" | jq -r '.extentReportPath // ""')
                        else
                            # 简单解析
                            REPORT_ID=\$(echo "\$RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2 || echo "0")
                            PASS_COUNT=\$(echo "\$RESPONSE" | grep -o '"passCount":[0-9]*' | head -1 | cut -d: -f2 || echo "0")
                            FAIL_COUNT=\$(echo "\$RESPONSE" | grep -o '"failCount":[0-9]*' | head -1 | cut -d: -f2 || echo "0")
                            PASS_RATE=\$(echo "\$RESPONSE" | grep -o '"passRate":"[^"]*"' | head -1 | cut -d: -f2 | tr -d '\"' || echo "0")
                            EXTENT_URL=\$(echo "\$RESPONSE" | grep -o '"extentReportPath":"[^"]*"' | head -1 | cut -d: -f2 | tr -d '\"' || echo "")
                        fi
                        
                        echo "=== 测试报告详情 ==="
                        echo "报告ID: \$REPORT_ID"
                        echo "通过数: \$PASS_COUNT"
                        echo "失败数: \$FAIL_COUNT"
                        echo "通过率: \$PASS_RATE%"
                        
                        if [ -n "\$EXTENT_URL" ] && [ "\$EXTENT_URL" != "null" ]; then
                            echo "Extent报告路径: \${EXTENT_URL}"
                            
                            # 处理相对路径
                            if [[ "\$EXTENT_URL" != /* ]]; then
                                EXTENT_URL="/\${EXTENT_URL}"
                            fi
                            
                            echo "Extent报告URL: \${API_BASE_URL}\${EXTENT_URL}"
                            
                            # 保存到环境变量
                            echo "PASS_COUNT=\$PASS_COUNT" > env.properties
                            echo "FAIL_COUNT=\$FAIL_COUNT" >> env.properties
                            echo "PASS_RATE=\$PASS_RATE" >> env.properties
                            echo "EXTENT_REPORT_URL=\${API_BASE_URL}\${EXTENT_URL}" >> env.properties
                            echo "EXTENT_REPORT_PATH=\${EXTENT_URL}" >> env.properties
                            echo "REPORT_ID=\$REPORT_ID" >> env.properties
                            
                            # 创建报告归档目录
                            mkdir -p "archive/reports"
                            
                            # 尝试获取Extent报告文件
                            echo "尝试获取Extent报告..."
                            
                            # 方法1: 通过HTTP下载报告
                            curl -s "\${API_BASE_URL}\${EXTENT_URL}" -o "archive/reports/extent-report-\${REPORT_ID}.html" 2>/dev/null && echo "✅ Extent报告下载成功" || echo "⚠️ Extent报告下载失败"
                            
                            # 方法2: 检查本地文件
                            LOCAL_PATH="\$(pwd)\${EXTENT_URL}"
                            if [ -f "\$LOCAL_PATH" ]; then
                                echo "✅ 找到本地Extent报告文件: \$LOCAL_PATH"
                                cp "\$LOCAL_PATH" "archive/reports/"
                            fi
                            
                            # 保存简化的HTML报告用于Jenkins展示
                            cat > "archive/reports/test-summary.html" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>测试报告摘要 - 项目 ${PROJECT_ID}</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .summary { margin: 20px 0; padding: 15px; background: #f5f5f5; border-radius: 5px; }
        .success { color: #4CAF50; }
        .failure { color: #F44336; }
        .stats { font-size: 18px; }
        a { color: #2196F3; text-decoration: none; }
        a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <h1>测试报告摘要 - 项目 ${PROJECT_ID}</h1>
    <div class="summary">
        <h2>测试结果</h2>
        <p class="stats">通过数: <span class="success">\$PASS_COUNT</span></p>
        <p class="stats">失败数: <span class="failure">\$FAIL_COUNT</span></p>
        <p class="stats">通过率: \$PASS_RATE%</p>
    </div>
    <div>
        <h2>报告链接</h2>
        <p>完整Extent报告: <a href="\${API_BASE_URL}\${EXTENT_URL}" target="_blank">点击查看</a></p>
        <p>构建链接: <a href="\${BUILD_URL}" target="_blank">\${JOB_NAME} #\${BUILD_NUMBER}</a></p>
    </div>
    <div>
        <p><small>生成时间: \$(date)</small></p>
    </div>
</body>
</html>
EOF
                            
                        else
                            echo "⚠️ 未找到Extent报告URL"
                            echo "PASS_COUNT=\$PASS_COUNT" > env.properties
                            echo "FAIL_COUNT=\$FAIL_COUNT" >> env.properties
                            echo "PASS_RATE=\$PASS_RATE" >> env.properties
                        fi
                    else
                        echo "⚠️ 未找到测试报告"
                        echo "PASS_COUNT=0" > env.properties
                        echo "FAIL_COUNT=0" >> env.properties
                        echo "PASS_RATE=0" >> env.properties
                    fi
                    
                    # 显示环境变量
                    echo "=== 生成的环境变量 ==="
                    cat env.properties 2>/dev/null || echo "无环境变量"
                    """
                }
            }
        }

        stage('创建定时任务') {
            when {
                expression { params.CREATE_SCHEDULE == true }
            }
            steps {
                script {
                    sh """
                    echo "为项目 ${PROJECT_ID} 创建定时任务..."
                    
                    # 生成唯一的任务名称
                    TIMESTAMP=\$(date +%Y%m%d_%H%M%S)
                    SCHEDULE_NAME="Jenkins_定时任务_\${TIMESTAMP}_项目${PROJECT_ID}"
                    
                    # 创建定时任务
                    REQUEST_BODY=\$(cat << EOF
{
    "scheduleName": "\${SCHEDULE_NAME}",
    "projectId": ${PROJECT_ID},
    "cronExpression": "${CRON_EXPRESSION}",
    "status": 1
}
EOF
                    )
                    
                    echo "请求体: \$REQUEST_BODY"
                    
                    RESPONSE=\$(curl -s -w "\\n%{http_code}" \\
                        -X POST "${API_BASE_URL}/schedule/add" \\
                        -H "Content-Type: application/json" \\
                        -d "\$REQUEST_BODY")
                    HTTP_CODE=\$(echo "\$RESPONSE" | tail -1)
                    BODY=\$(echo "\$RESPONSE" | sed '\$d')
                    
                    echo "创建定时任务响应状态码: \$HTTP_CODE"
                    if [ "\$HTTP_CODE" = "200" ]; then
                        echo "✅ 定时任务创建成功: \$SCHEDULE_NAME"
                        echo "响应内容:"
                        if command -v jq &> /dev/null; then
                            echo "\$BODY" | jq .
                        else
                            echo "\$BODY"
                        fi
                        
                        # 保存定时任务ID
                        if command -v jq &> /dev/null; then
                            SCHEDULE_ID=\$(echo "\$BODY" | jq -r '.data.id' 2>/dev/null || echo "")
                            if [ -n "\$SCHEDULE_ID" ]; then
                                echo "SCHEDULE_ID=\$SCHEDULE_ID" >> schedule.env
                                echo "定时任务ID: \$SCHEDULE_ID"
                            fi
                        fi
                    else
                        echo "❌ 定时任务创建失败"
                        echo "响应内容: \$BODY"
                    fi
                    """
                }
            }
        }

        stage('质量门禁') {
            steps {
                script {
                    // 加载环境变量
                    if (fileExists('env.properties')) {
                        load 'env.properties'
                    }

                    echo "=== 质量门禁检查 ==="
                    echo "通过数: ${env.PASS_COUNT ?: 0}"
                    echo "失败数: ${env.FAIL_COUNT ?: 0}"
                    echo "通过率: ${env.PASS_RATE ?: 0}%"

                    // 定义质量阈值
                    def MIN_PASS_RATE = 80.0
                    def currentPassRate = 0.0

                    try {
                        currentPassRate = Double.parseDouble(env.PASS_RATE ?: "0")
                    } catch (Exception e) {
                        currentPassRate = 0.0
                    }

                    if (currentPassRate < MIN_PASS_RATE) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ 质量门禁警告: 通过率${currentPassRate}% 低于阈值${MIN_PASS_RATE}%"
                    } else {
                        echo "✅ 质量门禁通过: 通过率${currentPassRate}% 达标"
                    }

                    // 记录测试结果
                    echo "## 测试结果摘要" > summary.md
                    echo "- ✅ 通过数: ${env.PASS_COUNT ?: 0}" >> summary.md
                    echo "- ❌ 失败数: ${env.FAIL_COUNT ?: 0}" >> summary.md
                    echo "- 📊 通过率: ${env.PASS_RATE ?: 0}%" >> summary.md
                    echo "- 📁 报告URL: ${env.EXTENT_REPORT_URL ?: '未生成'}" >> summary.md
                }
            }
        }

        stage('停止应用') {
            steps {
                script {
                    sh '''
                    echo "清理应用..."
                    if [ -f app.pid ]; then
                        PID=$(cat app.pid)
                        echo "停止进程: $PID"
                        kill $PID 2>/dev/null || true
                        sleep 5
                        # 强制杀死
                        kill -9 $PID 2>/dev/null || true
                        rm -f app.pid
                    fi
                    
                    # 确保进程被杀死
                    pkill -f "spring-boot:run" 2>/dev/null || true
                    pkill -f "AutoTestPlatform" 2>/dev/null || true
                    echo "应用清理完成"
                    
                    # 查看应用日志
                    if [ -f app.log ]; then
                        echo "=== 最后50行应用日志 ==="
                        tail -50 app.log
                    fi
                    '''
                }
            }
        }
    }

    post {
        always {
            // 存档报告
            script {
                if (fileExists("archive/reports")) {
                    archiveArtifacts artifacts: 'archive/reports/**/*', fingerprint: true
                }

                // 存档日志
                if (fileExists("app.log")) {
                    archiveArtifacts artifacts: 'app.log', fingerprint: true
                }

                // 存档Maven settings
                if (fileExists("${MAVEN_SETTINGS}")) {
                    archiveArtifacts artifacts: "${MAVEN_SETTINGS}", fingerprint: true
                }

                // 存档环境变量
                if (fileExists("env.properties")) {
                    archiveArtifacts artifacts: 'env.properties', fingerprint: true
                }

                // 存档schedule配置
                if (fileExists("schedule.env")) {
                    archiveArtifacts artifacts: 'schedule.env', fingerprint: true
                }

                // 存档摘要
                if (fileExists("summary.md")) {
                    archiveArtifacts artifacts: 'summary.md', fingerprint: true
                }

                // 存档执行ID
                if (fileExists("execution.env")) {
                    archiveArtifacts artifacts: 'execution.env', fingerprint: true
                }
            }

            // 清理工作空间
            cleanWs(
                    cleanWhenNotBuilt: false,
                    deleteDirs: false,
                    disableDeferredWipeout: false,
                    notFailBuild: true
            )
        }

        success {
            // 发布ExtentReports报告
            script {
                if (fileExists("env.properties")) {
                    load 'env.properties'

                    if (env.EXTENT_REPORT_PATH && env.API_BASE_URL) {
                        echo "发布ExtentReports报告..."
                        def extentUrl = "${env.API_BASE_URL}${env.EXTENT_REPORT_PATH}"

                        // 创建报告目录
                        sh """
                        mkdir -p "html-reports/extent" || true
                        """

                        // 尝试下载报告
                        sh """
                        echo "尝试下载Extent报告: \${extentUrl}"
                        curl -s "\${extentUrl}" -o "html-reports/extent/extent-report.html" 2>/dev/null || echo "无法下载报告"
                        """

                        // 检查是否成功下载
                        if (fileExists("html-reports/extent/extent-report.html")) {
                            // 发布HTML报告
                            publishHTML([
                                    reportDir: 'html-reports/extent',
                                    reportFiles: 'extent-report.html',
                                    reportName: "ExtentReports-项目${PROJECT_ID}",
                                    keepAll: true,
                                    alwaysLinkToLastBuild: true
                            ])
                            echo "✅ ExtentReports报告已发布"
                        } else {
                            // 发布摘要报告
                            publishHTML([
                                    reportDir: 'archive/reports',
                                    reportFiles: 'test-summary.html',
                                    reportName: "测试摘要-项目${PROJECT_ID}",
                                    keepAll: true,
                                    alwaysLinkToLastBuild: true
                            ])
                            echo "⚠️ Extent报告未下载，发布摘要报告"
                        }
                    } else {
                        echo "⚠️ ExtentReports报告URL不可用，跳过发布"
                    }
                }
            }

            // 成功通知
            script {
                if (fileExists('env.properties')) {
                    load 'env.properties'
                }

                def passRate = env.PASS_RATE ?: "0"
                def passCount = env.PASS_COUNT ?: "0"
                def failCount = env.FAIL_COUNT ?: "0"
                def extentReportUrl = env.EXTENT_REPORT_URL ?: ""

                emailext(
                        to: 'dev-team@example.com',
                        subject: "✅ 自动化测试完成: 项目${PROJECT_ID} - ${env.JOB_NAME} #${env.BUILD_NUMBER} (Maven ${MAVEN_VERSION})",
                        body: """
                    <h2>自动化测试完成 - ${currentBuild.result ?: 'SUCCESS'}</h2>
                    <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                        <tr><th style="background:#f2f2f2; padding:8px;">项目</th><td>${PROJECT_ID}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">构建号</th><td>${env.BUILD_NUMBER}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">环境</th><td>${ENVIRONMENT}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">Maven版本</th><td>${MAVEN_VERSION}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;" colspan="2">测试结果</th></tr>
                        <tr><td>&nbsp;&nbsp;通过数</td><td style="color:#4CAF50; font-weight:bold;">${passCount}</td></tr>
                        <tr><td>&nbsp;&nbsp;失败数</td><td style="color:#F44336; font-weight:bold;">${failCount}</td></tr>
                        <tr><td>&nbsp;&nbsp;通过率</td><td style="font-weight:bold;">${passRate}%</td></tr>
                    </table>
                    <p><strong>🔗 报告链接:</strong></p>
                    <ul>
                        <li>Jenkins构建: <a href="${env.BUILD_URL}">${env.BOB_NAME} #${env.BUILD_NUMBER}</a></li>
                        ${extentReportUrl ? '<li>Extent报告: <a href="' + extentReportUrl + '">' + extentReportUrl + '</a></li>' : '<li>Extent报告: 未生成</li>'}
                    </ul>
                    <p><strong>📅 下一次构建:</strong> 如果设置了定时任务，下次将在 ${params.CREATE_SCHEDULE == 'true' ? '指定时间' : '手动'} 执行</p>
                    """,
                        mimeType: 'text/html'
                )
            }
        }

        failure {
            // 失败通知
            script {
                emailext(
                        to: 'dev-team@example.com',
                        subject: "❌ 自动化测试失败: 项目${PROJECT_ID} - ${env.JOB_NAME} #${env.BUILD_NUMBER} (Maven ${MAVEN_VERSION})",
                        body: """
                    <h2 style="color:#F44336;">自动化测试失败</h2>
                    <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                        <tr><th style="background:#f2f2f2; padding:8px;">项目</th><td>${PROJECT_ID}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">构建号</th><td>${env.BUILD_NUMBER}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">环境</th><td>${ENVIRONMENT}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">Maven版本</th><td>${MAVEN_VERSION}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">Cron表达式</th><td>${CRON_EXPRESSION}</td></tr>
                    </table>
                    <p><strong>❌ 失败原因:</strong> 请检查构建日志</p>
                    <p><strong>🔍 调试信息:</strong></p>
                    <ul>
                        <li>应用日志: 检查 <code>app.log</code> 文件</li>
                        <li>API连接: 确保 ${API_BASE_URL} 可访问</li>
                        <li>项目ID: ${PROJECT_ID} 是否正确</li>
                    </ul>
                    <p><strong>🔗 控制台链接:</strong> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
                    """,
                        mimeType: 'text/html'
                )
            }
        }

        unstable {
            // 质量门禁失败通知
            script {
                if (fileExists('env.properties')) {
                    load 'env.properties'
                }

                emailext(
                        to: 'dev-team@example.com',
                        subject: "⚠️ 质量门禁不达标: 项目${PROJECT_ID} - ${env.JOB_NAME} #${env.BUILD_NUMBER} (Maven ${MAVEN_VERSION})",
                        body: """
                    <h2 style="color:#FF9800;">质量门禁不达标</h2>
                    <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                        <tr><th style="background:#f2f2f2; padding:8px;">项目</th><td>${PROJECT_ID}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">构建号</th><td>${env.BUILD_NUMBER}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">Maven版本</th><td>${MAVEN_VERSION}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">通过数</th><td style="color:#4CAF50;">${env.PASS_COUNT ?: 0}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">失败数</th><td style="color:#F44336;">${env.FAIL_COUNT ?: 0}</td></tr>
                        <tr><th style="background:#f2f2f2; padding:8px;">通过率</th><td style="color:#F44336; font-weight:bold;">${env.PASS_RATE ?: 0}%</td></tr>
                    </table>
                    <p><strong>⚠️ 注意:</strong> 通过率低于80%，需要检查测试用例</p>
                    <p><strong>🔍 建议:</strong></p>
                    <ul>
                        <li>检查失败的测试用例</li>
                        <li>查看详细的Extent报告</li>
                        <li>调整质量门禁阈值（当前: 80%）</li>
                    </ul>
                    <p><strong>🔗 控制台链接:</strong> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
                    ${env.EXTENT_REPORT_URL ? '<p><strong>📄 详细报告:</strong> <a href="' + env.EXTENT_REPORT_URL + '">点击查看</a></p>' : ''}
                    """,
                        mimeType: 'text/html'
                )
            }
        }
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        retry(1)
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }
}