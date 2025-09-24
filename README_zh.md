# 智脑AI Spring客户端

`zhinao-spring-ai` 是一个用于**360智脑AI API**的Java客户端。更多信息请访问[智脑AI文档](https://ai.360.com/platform/docs/overview)。

## 概述

本项目是一个**多模块Maven项目**：

* **`zhinao-parent`**: 父POM，用于管理依赖和版本
* **`zhinao-core`**: 智脑AI客户端的核心实现
* **`spring-ai-autoconfigure-model-zhinao`**: 智脑模型的Spring Boot自动配置
* **`zhinao-spring-boot-starter`**: 用于与Spring Boot应用程序轻松集成的启动模块
* **`docs`**: 使用Antora构建的文档模块

## 功能特性

* **聊天模型**: 完全支持Zhinao聊天API
* **灵活配置**: 可配置温度、最大令牌数、top-p采样等参数
* **流式响应**: 逐令牌流式传输，实现实时交互应用
* **函数调用**: 注册可由智脑模型调用的Java函数，支持类型化参数
* **健壮的错误处理**: 内置重试机制，支持可配置的指数退避
* **可观测性**: 通过Micrometer收集指标，用于性能监控
* **类型安全**: 完全类型化的API，具有编译时安全性和出色的IDE支持

## 系统要求

* **Java**: 17或更高版本
* **Maven**: 3.6.3或更高版本

## 从源码构建

构建项目：

```bash
./mvnw clean install
```

### 构建文档

本地构建文档：

```bash
cd docs
../mvnw antora:antora
```

生成的网站将在 `docs/target/antora/site/` 路径下可用。

## 使用方法

在您的Maven项目中添加以下依赖：

```xml
<dependency>
    <groupId>io.github.lin-coco</groupId>
    <artifactId>zhinao-core</artifactId>
    <version>latest-version</version>
</dependency>
```

### 示例代码

```java
// 初始化智脑API客户端
ZhinaoApi zhinaoApi = new ZhinaoApi("your-api-key");

// 创建聊天模型实例
ZhinaoChatModel chatModel = new ZhinaoChatModel(zhinaoApi);

// 准备提示词
UserMessage userMessage = new UserMessage("给我讲一个关于编程的笑话");
Prompt prompt = new Prompt(List.of(userMessage));

// 获取响应
ChatResponse response = chatModel.call(prompt);
String content = response.getResult().getOutput().getContent();
System.out.println(content);
```

## 许可证

本项目采用[Apache License 2.0](LICENSE)许可证。