package advanced.java.part3.service; 

import advanced.java.part3.entity.Sport;
import advanced.java.part3.repositories.SportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SportEtlServiceTest {

    @Mock
    private SportRepository sportRepository;

    @Test
    void repositorySavePipeline_filtersNullsAndSavesInOrder() {
        when(sportRepository.save(any(Sport.class)))
                .thenAnswer(invocation -> {
                    Sport input = invocation.getArgument(0);
                    return Mono.just(new Sport(100, input.getName()));
                });

        Flux<Sport> pipeline = Flux.just("Running", "Cycling")
                .map(name -> new Sport(null, name))
                .concatMap(sportRepository::save);

        StepVerifier.create(pipeline, 1) // request 1 at a time to verify demand propagation
                .expectNext(new Sport(100, "Running"))
                .thenRequest(1)
                .expectNext(new Sport(100, "Cycling"))
                .verifyComplete();
    }
}