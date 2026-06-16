package de.eldecker.dhbw.spring.sentimentanalyse.engine;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;


/**
 * Diese Bean-Klasse kapselt die Kommunikation mit der REST-Schnittstelle
 * der KI.
 */
@Service
public class SentimentAnalyseService {

    /** Objekt für REST-Calls. */
    private final RestClient _restClient;

    /** Prompt-Template. */
    private static final String PROMPT_TEMPLATE =
    		"""
    		Führe eine Sentimentanalyse für den folgenden Nutzerkommentar durch.
    		Antworte ausschließlich mit gültigem JSON nach diesem Schema:
    		{
    			"sentiment": "positiv|neutral|negativ",
    			"confidence": 0.0-1.0
    		}
    		Nutzerkommentar: {{kommentar}}
    		""";

    /**
     * Konstruktor, erzeugt REST-Client-Objekt.
     */
	public SentimentAnalyseService() {

		_restClient = RestClient.builder()
								.baseUrl( "http://localhost:12434" )
								.build();
	}


	/**
	 * REST-Call für Sentiment-Analyse ausführen.
	 *
	 * @param kommentar Kommentar, auf dem die Sentiment-Analyse auszuführen ist
	 *
	 * @return Optional mit Ergebnis der Sentiment-Analyse;
	 *         ist im Fehlerfall leer
	 */
	public Optional<SentimentErgebnis> sentimentAnalysieren( String kommentar )  {

		try {

			final String prompt =
					PROMPT_TEMPLATE.replace( "{{kommentar}}", kommentar ) ;

			final String pfad = "/engines/v1/chat/completions";

			final ChatAnfrage chatAnfrage = new ChatAnfrage(
					"ai/mistral:7B-Q4_0",
					List.of( new ChatMessage( "user", prompt ) )
				);

            final ResponseEntity<SentimentErgebnis> responseEntity =
    										_restClient.post()
                                                       .uri( pfad )
                                                       .contentType( APPLICATION_JSON )
                                                       .body( chatAnfrage )
                                                       .retrieve()
                                                       .toEntity( SentimentErgebnis.class );

			return Optional.ofNullable( responseEntity.getBody() );
		}
		catch ( RestClientResponseException ex ) {

			System.out.println( "Fehler bei REST-Request: " + ex );
			return Optional.empty();
		}
	}

}
