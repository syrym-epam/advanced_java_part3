package advanced.java.part3.service; 

import advanced.java.part3.entity.Sport;
import advanced.java.part3.repositories.SportRepository;
import advanced.java.part3.services.SportHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportHandlerTest {

    @Mock
    private SportRepository sportRepository;

    @InjectMocks
    private SportHandler sportHandler;

    private Sport tennis;
    private Sport tableTennis;

    @BeforeEach
    void setUp() {
        tennis = new Sport(1, "Tennis");
        tableTennis = new Sport(2, "Table Tennis");
    }

    @Test
    void createSport_whenSportDoesNotExist_savesAndReturnsOk() {
        when(sportRepository.findByName("Tennis")).thenReturn(Mono.empty());
        when(sportRepository.save(any(Sport.class))).thenReturn(Mono.just(tennis));

        MockServerRequest request = MockServerRequest.builder()
                .pathVariable("sportname", "Tennis")
                .build();

        Mono<ServerResponse> responseMono = sportHandler.createSport(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                })
                .verifyComplete();

        verify(sportRepository).findByName("Tennis");
        verify(sportRepository).save(any(Sport.class));
    }

    @Test
    void createSport_whenSportAlreadyExists_returnsBadRequest() {
        when(sportRepository.findByName("Tennis")).thenReturn(Mono.just(tennis));

        MockServerRequest request = MockServerRequest.builder()
                .pathVariable("sportname", "Tennis")
                .build();

        Mono<ServerResponse> responseMono = sportHandler.createSport(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verifyComplete();

        verify(sportRepository, never()).save(any());
    }

    @Test
    void searchSports_withoutQuery_returnsAllSports() {
        when(sportRepository.findAll()).thenReturn(Flux.just(tennis, tableTennis));

        MockServerRequest request = MockServerRequest.builder().build();

        StepVerifier.create(sportHandler.searchSports(request))
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        verify(sportRepository).findAll();
        verify(sportRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    void searchSports_withQuery_searchesByNameIgnoreCase() {
        when(sportRepository.findByNameContainingIgnoreCase("ten")).thenReturn(Flux.just(tennis, tableTennis));

        MockServerRequest request = MockServerRequest.builder()
                .queryParam("q", "ten")
                .build();

        StepVerifier.create(sportHandler.searchSports(request))
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        verify(sportRepository).findByNameContainingIgnoreCase("ten");
        verify(sportRepository, never()).findAll();
    }
}