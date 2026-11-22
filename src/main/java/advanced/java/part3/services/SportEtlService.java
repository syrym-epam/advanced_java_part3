package advanced.java.part3.services;

import advanced.java.part3.entity.Sport;
import advanced.java.part3.repositories.SportRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportEtlService implements CommandLineRunner {

    private static final int BATCH_SIZE = 20;

    private final SportRepository sportRepository;
    private final WebClient webClient = WebClient.create("https://webservice.rakuten.co.jp");
    // https://webservice.rakuten.co.jp/explorer/api/IchibaItem/Search

    @Override
    public void run(String... args) {
        Flux<Sport> sportFlux = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/explorer/api/IchibaItem/Search")
                        // .queryParam("applicationId", "RAKUTEN_APP_ID")
                        // .queryParam("keyword", "sport")
                        .build())
                .retrieve()
                .bodyToMono(RakutenApiResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .flatMapMany(response -> response.getItems() == null
                        ? Flux.empty()
                        : Flux.fromIterable(response.getItems()))
                .filter(wrapper -> wrapper != null && wrapper.getItem() != null && wrapper.getItem().getItemName() != null)
                .map(wrapper -> new Sport(null, wrapper.getItem().getItemName()))
                .concatMap(sportRepository::save)
                .log("sport-etl-pipeline");

        sportFlux.subscribe(new BaseSubscriber<Sport>() {
            private int countInBatch = 0;
            private int totalProcessed = 0;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                log.info("Subscribed to ETL stream. Requesting initial {} elements via request({})", BATCH_SIZE, BATCH_SIZE);
                request(BATCH_SIZE);
            }

            @Override
            protected void hookOnNext(Sport savedSport) {
                countInBatch++;
                totalProcessed++;
                log.info("onNext #{} (batch {}/{}): saved sport id={}, name={}",
                        totalProcessed, countInBatch, BATCH_SIZE, savedSport.getId(), savedSport.getName());

                if (countInBatch >= BATCH_SIZE) {
                    countInBatch = 0;
                    log.info("Batch of {} completed. Requesting next {} elements via request({})",
                            BATCH_SIZE, BATCH_SIZE, BATCH_SIZE);
                    request(BATCH_SIZE);
                }
            }

            @Override
            protected void hookOnComplete() {
                log.info("ETL stream completed. Total sports processed: {}", totalProcessed);
            }

            @Override
            protected void hookOnError(Throwable throwable) {
                log.error("ETL stream failed after {} processed items: {}", totalProcessed, throwable.getMessage(), throwable);
            }
        });
    }

    @Data
    public static class RakutenApiResponse {
        private List<ItemWrapper> items;
    }

    @Data
    public static class ItemWrapper {
        private Item item;
    }

    @Data
    public static class Item {
        private String itemName;
    }
}