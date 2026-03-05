package com.ai.tinglish.ai;

import com.ai.tinglish.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiOrchestrator {

    @Autowired
    private TranslationService translationService;
    @Autowired
    private PromptBuilder promptBuilder;

    private final List<String> sessionContext = new ArrayList<>();

    public String streamTranslation(String tinglish) {

        // PASS 1
        String p1 = translationService.callModel(
                promptBuilder.buildPass1(tinglish, sessionContext));

        // PASS 2
        String p2 = translationService.callModel(
                promptBuilder.buildPass2(p1));

        // PASS 3
        String p3 = translationService.callModel(
                promptBuilder.buildValidator(p2));

        // PASS 4
        String finalOutput = translationService.callModel(
                promptBuilder.buildConsistency(tinglish, p3));

        // English leakage detection
        if(finalOutput.matches(".*[a-zA-Z].*")) {
            finalOutput = translationService.callModel(
                    promptBuilder.buildPass2(finalOutput));
        }

        sessionContext.add(finalOutput);
        if(sessionContext.size() > 3) {
            sessionContext.remove(0);
        }

        return finalOutput.trim();
    }
}
