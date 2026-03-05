package com.venn.velocity.runner;

import com.venn.velocity.model.LoadRequest;
import com.venn.velocity.model.LoadResponse;
import com.venn.velocity.service.LoadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;

@Component
public class FileProcessorRunner implements CommandLineRunner {

    @Value("${app.runner.enabled:true}")
    private boolean enabled;

    private final LoadService service;
    private final ObjectMapper mapper;
    private final ApplicationContext context;

    public FileProcessorRunner(LoadService service, ObjectMapper mapper, ApplicationContext context) {
        this.service = service;
        this.mapper = mapper;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Path input = Paths.get("Input.txt");
        Path output = Paths.get("Output.txt");

        try (BufferedReader bufferedReader = Files.newBufferedReader(input);
             BufferedWriter bufferedWriter = Files.newBufferedWriter(output)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    LoadRequest req = mapper.readValue(line, LoadRequest.class);
                    Optional<LoadResponse> resp = service.process(req);

                    if (resp.isPresent()) {
                        bufferedWriter.write(mapper.writeValueAsString(resp.get()));
                        bufferedWriter.newLine();
                    }
                } catch (Exception e) {
                    System.err.println("Unprocessable line: " + line);
                    e.printStackTrace(System.err);
                }
            }
        }
        SpringApplication.exit(context, () -> 0);
    }

}
