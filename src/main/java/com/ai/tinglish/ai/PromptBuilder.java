package com.ai.tinglish.ai;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptBuilder {

    public String buildPass1(String input, List<String> context) {
        return """
                You are a professional Telugu linguist.
                
                Previous context:
                %s
                
                Task:
                Convert Tinglish into grammatically correct Telugu script.
                - Preserve meaning
                - Fix tense
                - Expand short forms
                - Add punctuation
                - Output only Telugu
                
                Input:
                %s
                """.formatted(String.join("\n", context), input);
    }

    public String buildPass2(String telugu) {
        return """
                You are a Telugu grammar perfectionist.
                
                Refine grammar, tense, agreement, punctuation.
                Return only Telugu text.
                
                Text:
                %s
                """.formatted(telugu);
    }

    public String buildValidator(String telugu) {
        return """
                Ensure:
                - No English letters
                - Natural Telugu flow
                - No grammar errors
                
                If correct return as is.
                Else correct.
                
                Text:
                %s
                """.formatted(telugu);
    }

    public String buildConsistency(String original, String telugu) {
        return """
                Original Tinglish:
                %s
                
                Telugu:
                %s
                
                Check meaning preservation.
                If deviation exists, correct.
                Return only Telugu.
                """.formatted(original, telugu);
    }
}
