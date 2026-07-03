 # 棕色尘埃2 每日助手 (BD2 Daily Automator)
 
 一款用于自动完成棕色尘埃2（Brown Dust II）日常任务的 Windows 桌面工具。
 
 ## 简介
 
 BD2 Daily Automator 基于 Java 开发，通过模拟鼠标点击和键盘按键来自动执行游戏内的每日例行操作，解放双手。
 
 核心功能包括：
 
 - **屏幕截图** — 使用 `java.awt.Robot` 进行全屏/区域截图
 - **鼠标控制** — 模拟鼠标移动、点击、拖拽和滚动
 - **键盘控制** — 双模式键盘：Win32 `PostMessage` 后台发送（不抢焦点）+ `java.awt.Robot` 前台发送
 - **窗口查找** — 通过 JNA / `user32.dll` 查找和管理 Windows 原生窗口
 - **图像匹配** — 基于 OpenCV 的模板匹配（`matchTemplate`）
 - **图形界面** — JavaFX 桌面 GUI，支持任务管理、队列编排、实时日志
 
 ## 快速开始
 
 ### 环境要求
 
 - Java 17+
 - Maven 3.8+
 - Windows 系统（JNA/PostMessage + OpenCV 原生绑定为平台相关）
 
 ### 构建
 
 ```bash
 mvn clean package
 ```
 
 ### 运行（GUI）
 
 ```bash
 mvn javafx:run
 ```
 
 或直接运行打包后的 jar：
 
 ```bash
 java -jar target/daily-automator-1.0.0.jar
 ```
 
 ### 运行测试
 
 ```bash
 mvn test
 ```
 
 ## 项目结构
 
 ```
 Project_Neowiz/
 ├── pom.xml
 ├── src/
 │   └── main/java/com/dailyautomator/
 │       ├── core/
 │       │   ├── ScreenCapture.java      # 屏幕截图
 │       │   ├── MouseController.java     # 鼠标控制
 │       │   ├── KeyboardController.java  # 键盘控制
 │       │   ├── WindowFinder.java        # 窗口查找
 │       │   └── TemplateMatcher.java     # 图像模板匹配
 │       ├── gui/
 │       │   └── MainApp.java             # JavaFX 主界面
 │       ├── task/
 │       │   ├── Action.java              # 动作接口
 │       │   ├── ClickAction.java         # 点击动作
 │       │   ├── WaitAction.java          # 等待动作
 │       │   ├── KeyPressAction.java      # 按键动作
 │       │   ├── ActionContext.java       # 动作执行上下文
 │       │   ├── TaskDefinition.java      # 任务定义
 │       │   ├── TaskRepository.java      # 任务持久化
 │       │   └── TaskRunner.java          # 任务执行引擎
 │       └── DailyAutomator.java          # 入口
 ├── tasks.json                           # 任务配置文件
 └── README.md
 ```
 
 ## 使用说明
 
 ### 任务列表
 
 左侧面板显示所有已保存的任务。可以新建、重命名、删除任务。
 
 ### 动作编辑器
 
 中间面板用于编辑选中任务的执行步骤，支持三种动作：
 
 - **点击** — 在指定屏幕坐标位置执行鼠标点击
 - **等待** — 暂停指定时长（毫秒），等待游戏界面加载
 - **按键** — 模拟键盘按键（如 ESC、F、H 等），用于关闭弹窗或触发交互
 
 动作列表支持编辑参数、调整顺序和删除。
 
 ### 启动队列
 
 右侧面板管理任务的执行顺序。从任务列表中将任务添加到队列，调整顺序后点击"开始"即可依次执行。
 
 ### 控制栏
 
 - **开始/停止** — 启动或中止自动化流程
 - **坐标** — 开启鼠标坐标追踪，方便获取点击位置
 - **测试点击** — 在当前位置执行一次鼠标点击用于测试
 - **截图查看/保存** — 查看或保存当前游戏窗口截图
 - **归位** — 将游戏窗口移动到屏幕左上角，确保坐标与任务配置一致
 
 ## 注意事项
 
 1. **游戏窗口**：运行任务前请确保棕色尘埃2窗口已打开且可见
 2. **窗口置顶**：任务执行时会自动将游戏窗口置前，请勿在此期间操作鼠标键盘
 3. **分辨率**：任务中的点击坐标基于固定窗口位置（左上角归位），移动窗口后需要重新校准
 4. **安全测试**：测试用例会真实移动鼠标和发送按键，运行前请保存工作
 
 ## 安全警告
 
 **运行测试用例会真实移动鼠标和发送按键。**
 
 测试套件使用 `java.awt.Robot` 模拟鼠标和键盘输入。执行 `mvn test` 时，以下测试会在实际桌面上运行：
 
 - MouseControllerTest — 移动鼠标、点击、拖拽、滚动
 - KeyboardControllerTest — 发送按键、输入文字、触发快捷键
 - ScreenCaptureTest — 截取屏幕画面
 - TemplateMatcherTest — 使用 Robot 进行屏幕截图
 
 **这些测试默认禁用**。启用需传递参数：
 
 ```bash
 mvn test -Ddailyautomator.enableRobotTests=true
 ```
 
 启用前请确保没有重要窗口处于焦点中，并保存好当前工作。
 
 ### 测试安全对照表
 
 | 测试类 | 是否使用 Robot？ | 默认运行？ |
 |---|---|---|
 | WindowFinderTest | 否（仅 JNA） | 是 |
 | KeyboardControllerTest | 是 | 否 |
 | MouseControllerTest | 是 | 否 |
 | ScreenCaptureTest | 是 | 否 |
 | TemplateMatcherTest | 是 | 否 |
 
