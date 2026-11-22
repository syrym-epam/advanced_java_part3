package advanced.java.part3.services;

import advanced.java.part3.entity.Sport;
import advanced.java.part3.repositories.SportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SportHandler {

    private final SportRepository sportRepository;

    public Mono<ServerResponse> createSport(ServerRequest request) {
        String sportName = request.pathVariable("sportname");

        return sportRepository.findByName(sportName)
                .flatMap(existing -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"error\": \"Sport with name '" + sportName + "' already exists\"}"))
                .switchIfEmpty(
                        sportRepository.save(new Sport(null, sportName))
                                .flatMap(savedSport -> ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(savedSport))
                );
    }

    public Mono<ServerResponse> searchSports(ServerRequest request) {
        String query = request.queryParam("q").orElse("");

        var sportsFlux = query.isEmpty() 
                ? sportRepository.findAll() 
                : sportRepository.findByNameContainingIgnoreCase(query);

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(sportsFlux, Sport.class);
    }
}