package com.lincoco.springai.zhinao.chat;

import com.lincoco.springai.zhinao.ZhinaoTestConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = ZhinaoTestConfiguration.class)
public class ZhinaoChatModelIT {

    private static final Logger logger = LoggerFactory.getLogger(ZhinaoChatModelIT.class);

    @Autowired
    ChatModel chatModel;

    @Autowired
    StreamingChatModel streamingChatModel;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;

    @Test
    void roleTest() {
        UserMessage userMessage = new UserMessage("Tell me about 3 famous pirates from the Golden Age of Piracy and why they did.");
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", "Bob", "voice", "pirate"));
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatResponse response = this.chatModel.call(prompt);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOutput().getText()).contains("Blackbeard");
        logger.info("Response: {}", response);
    }

    @Test
    void listOutputConverter() {
        DefaultConversionService conversionService = new DefaultConversionService();
        ListOutputConverter outputConverter = new ListOutputConverter(conversionService);

        String format = outputConverter.getFormat();
        String template = """
                List five {subject}
                {format}
                """;

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(template)
                .variables(Map.of("subject", "ice cream flavors", "format", format))
                .build();

        Prompt prompt = new Prompt(promptTemplate.createMessage());
        Generation generation = this.chatModel.call(prompt).getResult();
        List<String> list = outputConverter.convert(generation.getOutput().getText());
        logger.info("Response: {}", generation.getOutput().getText());
        assertThat(list).hasSize(5);
        logger.info("List: {}", list);
    }

    @Test
    void mapOutputConverter() {
        MapOutputConverter outputConverter = new MapOutputConverter();

        String format = outputConverter.getFormat();
        String template = """
                Provide me a List of {subject}
                {format}
                """;

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(template)
                .variables(Map.of("subject", """
                        numbers from 1 to 9 under they key name 'numbers'.
                        """, "format", format))
                .build();

        Prompt prompt = new Prompt(promptTemplate.createMessage());
        Generation generation = this.chatModel.call(prompt).getResult();
        Map<String, Object> result = outputConverter.convert(generation.getOutput().getText());
        logger.info("Response: {}", generation.getOutput().getText());
        assertThat(result.get("numbers")).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        logger.info("Map: {}", result);
    }

    @Test
    void beanOutputConverter() {
        BeanOutputConverter<ActorsFilms> outputConverter = new BeanOutputConverter<>(ActorsFilms.class);

        String format = outputConverter.getFormat();
        String template = """
                Generate the filmography for a random actor.
                {format}
                """;

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(template)
                .variables(Map.of("format", format))
                .build();

        Prompt prompt = new Prompt(promptTemplate.createMessage());

        Generation generation = this.chatModel.call(prompt).getResult();
        ActorsFilms actorsFilms = outputConverter.convert(generation.getOutput().getText());
        logger.info("Response: {}", generation.getOutput().getText());
        assertThat(actorsFilms.getActor()).isNotBlank();
        logger.info("Bean: {}", actorsFilms);
    }

    @Test
    void beanOutputConverterRecords() {

        BeanOutputConverter<ActorsFilmsRecord> outputConverter = new BeanOutputConverter<>(ActorsFilmsRecord.class);

        String format = outputConverter.getFormat();
        String template = """
				Generate the filmography of 5 movies for Tom Hanks.
				{format}
				""";
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(template)
                .variables(Map.of("format", format))
                .build();
        Prompt prompt = new Prompt(promptTemplate.createMessage());
        Generation generation = this.chatModel.call(prompt).getResult();
        ActorsFilmsRecord actorsFilmsRecord = outputConverter.convert(generation.getOutput().getText());
        logger.info("Response: {}", generation.getOutput().getText());
        assertThat(actorsFilmsRecord.actor()).isNotBlank();
        logger.info("Bean: {}", actorsFilmsRecord);
    }

    @Test
    void beanStreamOutputConverterRecords() {

        BeanOutputConverter<ActorsFilmsRecord> outputConverter = new BeanOutputConverter<>(ActorsFilmsRecord.class);

        String format = outputConverter.getFormat();
        String template = """
				Generate the filmography of 5 movies for Tom Hanks.
				{format}
				""";
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(template)
                .variables(Map.of("format", format))
                .build();
        Prompt prompt = new Prompt(promptTemplate.createMessage());

        String generationTextFromStream = this.streamingChatModel.stream(prompt)
                .collectList()
                .block()
                .stream()
                .map(ChatResponse::getResults)
                .flatMap(List::stream)
                .map(Generation::getOutput)
                .map(AssistantMessage::getText)
                .collect(Collectors.joining());

        ActorsFilmsRecord actorsFilmsRecord = outputConverter.convert(generationTextFromStream);
        logger.info("Response: {}", generationTextFromStream);
        assertThat(actorsFilmsRecord.actor()).isNotBlank();
        logger.info("Bean: {}", actorsFilmsRecord);
    }
}
