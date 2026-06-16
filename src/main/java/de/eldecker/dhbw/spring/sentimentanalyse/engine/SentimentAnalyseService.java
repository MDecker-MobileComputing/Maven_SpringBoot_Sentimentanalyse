package de.eldecker.dhbw.spring.sentimentanalyse.engine;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.core.JacksonException;
import  tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


/**
 * Diese Bean-Klasse kapselt die Kommunikation mit der REST-Schnittstelle
 * der KI.
 */
@Service
public class SentimentAnalyseService {

    /** Objekt für REST-Calls. */
    private final RestClient _restClient;

	/** JSON-Parser für Antwort-Body. */
	private final ObjectMapper _objectMapper = new ObjectMapper();

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

            final ResponseEntity<String> responseEntity =
    										_restClient.post()
                                                       .uri( pfad )
                                                       .contentType( APPLICATION_JSON )
                                                       .body( chatAnfrage )
                                                       .retrieve()
                                                       .toEntity( String.class );

            final String jsonString = responseEntity.getBody();

			return parseErgebnisJson( jsonString );
		}
		catch ( RestClientResponseException ex ) {

			System.out.println( "Fehler bei REST-Request: " + ex );
			return Optional.empty();
		}
	}


	/**
	 * Antwort-JSON von KI manuell parsen.
	 * 
	 * @param jsonString Antwort-JSON von KI
	 * 
	 * @return Optional enthält Ergebnisobjekt oder ist im Fehlerfall leer.
	 */
	private Optional<SentimentErgebnis> parseErgebnisJson( String jsonString ) {

		final String DEFAULT_SENTIMENT  = "???";		
		final float  DEFAULT_CONFIDENCE = -0.0f;		
		
		if ( jsonString == null || jsonString.isBlank() ) {
			
			return Optional.empty();
		}

		try {
			
			final JsonNode wurzelNode = _objectMapper.readTree( jsonString );

			String sentiment = wurzelNode.path( "sentiment" ).asString( DEFAULT_SENTIMENT );
			float confidence = (float) wurzelNode.path( "confidence" ).asDouble( DEFAULT_CONFIDENCE );

			if ( sentiment == null || sentiment.isBlank() ) {
				
				sentiment = DEFAULT_SENTIMENT;
			}

			confidence = Math.clamp( confidence, 0.0f, 1.0f );

			final SentimentErgebnis sentimentErgebnis = 
										new SentimentErgebnis( sentiment.toLowerCase(), 
												               confidence ); 			
			return Optional.of( sentimentErgebnis );
		}
		catch ( JacksonException ex ) {
			
			System.out.println( "Fehler beim Parsen des Ergebnis-JSON: " + ex.getMessage() );
			return Optional.empty();
		}

	}
}
