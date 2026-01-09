package com.rishiraj.spring_ai.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SignatureSpi;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatClient client;
    private final Resource tweetSystemMessage;

    public ChatController(ChatClient.Builder client,
                          @Value("classpath:prompts/tweet-system-message.st")
                          Resource tweetSystemMessage){

        this.client = client
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.tweetSystemMessage = tweetSystemMessage;
    }


    @GetMapping
    public String test(){
         return client
                .prompt("Hi, how are you?")
                .call()
                .content();
    }

    //using system prompt from resources
    @PostMapping("/generate-tweet")
    public String generateTweet(@RequestBody String input) throws IOException {
        String systemPrompt = tweetSystemMessage.getContentAsString(UTF_8);
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(input);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return client.prompt(prompt).call().content();
    }


    //using prompt template
    @PostMapping("/suggest-title")
    public String suggestTitles(@RequestBody SuggestTitleReqInput input) throws IOException {
        PromptTemplate promptTemplate = new PromptTemplate("I would like to" +
                " give presentation on the following topic: {topic}. Can you give me {count}" +
                " title that would be relevant.");
        Map<String, Object> vars = Map.of("topic", input.topic(), "count", input.count());
        Prompt prompt = promptTemplate.create(vars);
        return client.prompt(prompt).call().content();
    }


    //getting structured format in output using a list format
    @PostMapping("/suggest-title-structured")
    public TitleSuggestionResponse suggestTitlesAndStructuredOutput(@RequestBody SuggestTitleReqInput input) throws IOException {

        ListOutputConverter outputConverter = new ListOutputConverter();

        PromptTemplate promptTemplate = new PromptTemplate("""
                I would like to give presentation on the following topic: {topic}.
                
                Can you give me {count} title that would be relevant.
            
                {format}
                """);

        Map<String, Object> vars = Map.of(
                "topic", input.topic(),
                "count", input.count(),
                "format", outputConverter.getFormat()
        );

        Prompt prompt = promptTemplate.create(vars);
        String response = client.prompt(prompt).call().content();

        List<String> convert = outputConverter.convert(response);
        return new TitleSuggestionResponse(convert);
    }



    //getting structured format in output using a map format
    @GetMapping("/langs")
    public Map<String, Object> getCodingLang() {

        MapOutputConverter mapOutputConverter = new MapOutputConverter();

        PromptTemplate promptTemplate = new PromptTemplate("""
                Return all popular programming languages and their inception year.
            
                {format}
                """);

        Map<String, Object> vars = Map.of(
                "format", mapOutputConverter.getFormat()
        );

        Prompt prompt = promptTemplate.create(vars);
        String response = client.prompt(prompt).call().content();

        Map<String, Object> convert = mapOutputConverter.convert(response);
        return convert;
    }

    //bean output converter, we can make the output of any class type we want
    @PostMapping("/tweet")
    public Tweet generateTweet2(@RequestBody String topic) throws IOException {

        String systemPrompt = tweetSystemMessage.getContentAsString(UTF_8);
        SystemMessage systemMessage = new SystemMessage(systemPrompt);

        BeanOutputConverter<Tweet> beanOutputConverter = new BeanOutputConverter<>(Tweet.class);

        PromptTemplate promptTemplate = new PromptTemplate("""
                Generate a tweet for the following topic: {topic}.
                
                
                {format}
                
                """);
        Map<String, Object> vars = Map.of(
                "topic", topic,
                "format", beanOutputConverter.getFormat()
        );

        Message userMessage = promptTemplate.createMessage(vars);

        Prompt prompt = new Prompt(List.of(userMessage, systemMessage));

        String content = client.prompt(prompt).call().content();

       return beanOutputConverter.convert(content);

  

    }

    record SuggestTitleReqInput(String topic, int count){}
    record TitleSuggestionResponse(List<String> titles){}
    record Tweet(String content, List<String> hashtags){};

}



