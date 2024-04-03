package com.gypApp_main.service;

import com.gypApp_main.dto.TrainerWorkloadRequest;
import com.gypApp_main.model.Training;
import com.gypApp_main.security.JWTProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerWorkLoadService implements WorkLoadService {

    private final WebClient.Builder webClientBuilder;
    private final JWTProvider provider;


    @CircuitBreaker(name = "TrainerWorkLoadService", fallbackMethod = "updateWorkLoadFallback")
    public void updateWorkLoad(Training training, String action) {

        log.debug("Training details {}", training);
        String token = provider.generateTokenForWorkLoad();
        log.info("Here is toke for trainerworkload {}", token);
        log.info("Action type {}", action);

        webClientBuilder.build()
                .post()
                .uri("/updateWorkLoad/update")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createRequest(training, action)))
                .retrieve()
                .bodyToMono(TrainerWorkloadRequest.class)
                .subscribe();

        log.debug("Action type {}", action);
    }

    public TrainerWorkloadRequest createRequest(Training training, String action) {

        String username = training.getTrainer().getUser().getUserName();
        String firstName = training.getTrainer().getUser().getFirstName();
        String lastName = training.getTrainer().getUser().getLastName();
        Boolean isAcitve = training.getTrainer().getUser().getIsActive();
        log.info("Create request with trainer details {}", training);
        log.info("Action type {}", action);

        return TrainerWorkloadRequest.builder()
                .trainerUsername(username)
                .trainerFirstname(firstName)
                .trainerLastname(lastName)
                .isActive(isAcitve)
                .type(action)
                .trainingDuration(training.getTrainingDuration())
                .trainingDate(training.getTrainingDate())
                .build();

    }

    public void updateWorkLoadFallback(Training training, String action, Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                log.error("SERVICE_UNAVAILABLE ", ex.getMessage());
            }
        }
        log.error("Circuit Breaker triggered for updateWorkLoad: {}", throwable.getMessage());
    }


}
