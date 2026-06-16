package de.eldecker.dhbw.spring.sentimentanalyse.engine;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


/**
 * Diese Bean-Klasse kapselt die Kommunikation mit der REST-Schnittstelle
 * der KI.
 */
@Service
public class SentimentAnalyseService {

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


    /** Objekt für REST-Calls. */
    private final RestClient _restClient;

	/** JSON-Parser für Antwort-Body. */
	private final ObjectMapper _objectMapper = new ObjectMapper();


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

            final Optional<SentimentErgebnis> ergebnisOptional = parseErgebnisJson( jsonString );

			return ergebnisOptional;
		}
		catch ( RestClientResponseException ex ) {

			System.out.println( "Fehler bei REST-Request: " + ex );
			return Optional.empty();
		}
	}


	/**
	 * Antwort-JSON von KI manuell parsen.
	 *
	 * @param jsonString Antwort-JSON von KI;
	 *                   Beispiel für JSON von "mistral:7B-Q4_0" in Docker Model Runner:
	 *                   https://gist.github.com/MDecker-MobileComputing/f6e4b112dad6ebe1120be7d9f1a07400
	 *
	 * @return Optional enthält Ergebnisobjekt oder ist im Fehlerfall leer.
	 */
	private Optional<SentimentErgebnis> parseErgebnisJson( String jsonString ) {

		final String DEFAULT_SENTIMENT  = "neutral";
		final float  DEFAULT_CONFIDENCE = 0.0f;

		try {

			if ( jsonString == null || jsonString.isBlank() ) {
				return Optional.empty();
			}

			final JsonNode wurzelNode = _objectMapper.readTree( jsonString );
			final JsonNode payloadNode = ermittlePayloadNode( wurzelNode );
			if ( payloadNode == null ) {
				
				System.out.println( "Unerwartete Ergebnisstruktur" );
				return Optional.empty();
			}

			String sentiment  = payloadNode.path( "sentiment"  ).asString( DEFAULT_SENTIMENT  );
			double confidence = payloadNode.path( "confidence" ).asDouble( DEFAULT_CONFIDENCE );

			if ( sentiment == null || sentiment.isBlank() ) {

				sentiment = DEFAULT_SENTIMENT;
			}

			confidence = Math.clamp( confidence, 0.0f, 1.0f );

			final SentimentErgebnis sentimentErgebnis =
										new SentimentErgebnis( sentiment.toLowerCase(),
												               (float) confidence );
			return Optional.of( sentimentErgebnis );
		}
		catch ( Exception ex ) {

			System.out.println( "Fehler beim Parsen des Ergebnis-JSON: " + ex.getMessage() );
			return Optional.empty();
		}

	}


	/**
	 * Sucht die Nutzdaten für das Ergebnis in mehreren bekannten Formaten.
	 * 
	 * @param wurzelNode = 
	 */
	private JsonNode ermittlePayloadNode( JsonNode wurzelNode ) {

		if ( hatErgebnisFelder( wurzelNode ) ) {
			
			return wurzelNode;
		}

		final JsonNode choicesArray = wurzelNode.path( "choices" );
		if ( !choicesArray.isArray() || choicesArray.isEmpty() ) {
			
			return null;
		}

		final JsonNode choiceNode = choicesArray.get( 0 );
		if ( hatErgebnisFelder( choiceNode ) ) {
			
			return choiceNode;
		}

		final JsonNode messageNode = choiceNode.path( "message" );
		if ( hatErgebnisFelder( messageNode ) ) {
			
			return messageNode;
		}

		final JsonNode contentNode = messageNode.path( "content" );
		if ( !contentNode.isString() ) {
			
			return null;
		}

		final String contentString = contentNode.asString( "" );
		if ( contentString.isBlank() ) {
			
			return null;
		}

		try {
			final JsonNode contentJsonNode = _objectMapper.readTree( contentString );
			if ( hatErgebnisFelder( contentJsonNode ) ) {
				
				return contentJsonNode;
			}
		}
		catch ( Exception ex ) {
			
			return null;
		}

		return null;
	}


	private boolean hatErgebnisFelder( JsonNode node ) {

		return node != null &&
			   !node.isMissingNode() &&
			   ( node.has( "sentiment" ) || node.has( "confidence" ) );
	}
}
