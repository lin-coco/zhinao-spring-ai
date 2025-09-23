# zhinao-spring-ai

`zhinao-spring-ai` is a Java Spring client for the **Zhinao AI API**. For more information, visit [Zhinao AI Documentation](https://ai.360.com/platform/docs/overview).

## Overview

This project is organized as a **multi-module Maven project**:

* **`zhinao-parent`**: Parent POM managing dependencies and versions
* **`zhinao-core`**: Core implementation of the Zhinao AI client
* **`spring-ai-autoconfigure-model-zhinao`**: Spring Boot auto-configuration for Zhinao models
* **`zhinao-spring-boot-starter`**: Starter module for easy integration with Spring Boot applications
* **`docs`**: Documentation module built with Antora

## Features

* **Chat Models**: Full support for the Moonshot Chat API
* **Flexible Configuration**: Configure temperature, max tokens, top-p sampling, and more
* **Streaming Responses**: Token-by-token streaming for real-time interactive applications
* **Function Calling**: Register Java functions callable by Zhinao models with typed arguments
* **Robust Error Handling**: Built-in retries with configurable exponential backoff
* **Observability**: Metrics collection via Micrometer for performance monitoring
* **Type Safety**: Fully typed API with compile-time safety and excellent IDE support

## Requirements

* **Java**: 17 or higher
* **Maven**: 3.6.3 or higher

## Building from Source

To build the project:

```bash
./mvnw clean install
```

### Building the Documentation

To build the documentation locally:

```bash
cd docs
../mvnw antora:antora
```

The generated site will be available at `docs/target/antora/site/`.

## Usage

Add the following dependency to your Maven project:

```xml
<dependency>
    <groupId>com.lincoco.springai</groupId>
    <artifactId>zhinao-core</artifactId>
    <version>latest-version</version>
</dependency>
```

### Example

```java
// Initialize the Zhinao API client
ZhinaoApi zhinaoApi = new ZhinaoApi("your-api-key");

// Create a chat model instance
ZhinaoChatModel chatModel = new ZhinaoChatModel(zhinaoApi);

// Prepare a prompt
UserMessage userMessage = new UserMessage("Tell me a joke about programming");
Prompt prompt = new Prompt(List.of(userMessage));

// Get the response
ChatResponse response = chatModel.call(prompt);
String content = response.getResult().getOutput().getContent();
System.out.println(content);
```

## License

This project is licensed under the [Apache License 2.0](LICENSE).