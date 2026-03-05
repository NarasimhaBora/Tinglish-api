package com.ai.tinglish.controller;

import com.ai.tinglish.ai.AiOrchestrator;
import com.ai.tinglish.dto.TranslationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/v1/translations")
@CrossOrigin
public class TranslationController {

    @Autowired
    private AiOrchestrator orchestrator;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamTranslation(@RequestBody TranslationRequest req) {

        return Flux.create(sink -> {

            String result = orchestrator.streamTranslation(req.text());

            sink.next(result);
            sink.complete();
        });
    }
}
