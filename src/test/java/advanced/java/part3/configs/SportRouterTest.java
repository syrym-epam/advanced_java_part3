package advanced.java.part3.configs;

import advanced.java.part3.entity.Sport;
import advanced.java.part3.repositories.SportRepository;
import advanced.java.part3.services.SportHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportRouterTest {

    @Mock
    private SportRepository sportRepository;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        SportHandler handler = new SportHandler(sportRepository);
        SportRouter routerConfig = new SportRouter();
        this.webTestClient = WebTestClient.bindToRouterFunction(routerConfig.sportRoutes(handler)).build();
    }

    @Test
    void postCreateSport_returnsCreatedSportJson() {
        when(sportRepository.findByName("Badminton")).thenReturn(Mono.empty());
        when(sportRepository.save(any(Sport.class))).thenReturn(Mono.just(new Sport(10, "Badminton")));

        webTestClient.post()
                .uri("/api/v1/sport/{sportname}", "Badminton")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(10)
                .jsonPath("$.name").isEqualTo("Badminton");
    }

    @Test
    void postCreateSport_duplicate_returns400WithErrorMessage() {
        when(sportRepository.findByName("Badminton")).thenReturn(Mono.just(new Sport(10, "Badminton")));

        webTestClient.post()
                .uri("/api/v1/sport/{sportname}", "Badminton")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Sport with name 'Badminton' already exists");
    }

    @Test
    void getSearchSports_withQueryParam_returnsMatchingSports() {
        when(sportRepository.findByNameContainingIgnoreCase("ball"))
                .thenReturn(Flux.just(new Sport(1, "Football"), new Sport(2, "Basketball")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/sport").queryParam("q", "ball").build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Sport.class)
                .hasSize(2)
                .contains(new Sport(1, "Football"), new Sport(2, "Basketball"));
    }
}