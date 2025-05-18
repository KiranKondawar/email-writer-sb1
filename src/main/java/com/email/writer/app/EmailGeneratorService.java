package com.email.writer.app;

import ch.qos.logback.core.CoreConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.Map;

@Service
public class EmailGeneratorService {
    private final WebClient webClient;

    public EmailGeneratorService(WebClient.Builder webClientBuilder){
        this.webClient= webClientBuilder.build();
    }

    @Value("${gemini.api.url}")
    private String geminiAPIUrl;
    @Value("${gemini.api.key}")
    private String geminiAPIKey;

    public String generateEmailReply (EmailRequest emailRequest){
        //build prompt to reply
        String prompt = buildPrompt(emailRequest);

        //Craft request
        Map<String,Object> requestBody = Map.of(
                "contents",new Object[] {
                      Map.of("parts",new Object[] {
                          Map.of("text",prompt)
                })
        });



        //Do http request & get Response
 String response= webClient.post().uri(geminiAPIUrl + geminiAPIKey)
         .header("Content-Type","application/json")
         .bodyValue(requestBody)
         .retrieve()
         .bodyToMono(String.class)
         .block();



        //extarct response & Return Response
        System.out.println("response after extract" +requestBody.toString());
        return  extractResponseContent(response);

    }

    private String extractResponseContent(String response) {

        try{
            ObjectMapper mapper =new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();


        }catch(Exception e){
            return  "Error while processing request: "+ e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for following email content.Please dont generate subject line ");
        if(emailRequest.getTone()!= null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a").append(emailRequest.getTone()).append(" tone ");

        }
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
