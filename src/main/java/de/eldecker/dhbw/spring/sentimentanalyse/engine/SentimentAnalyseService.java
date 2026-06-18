package de.eldecker.dhbw.spring.sentimentanalyse.engine;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import jakarta.annotation.PostConstruct;
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

	/** JSON-Parser für Antwort-Body. */
	private final ObjectMapper _objectMapper = new ObjectMapper();


    /** Objekt für REST-Calls. */
    private RestClient _restClient;


	/**
	 * Basis-URL für REST-API von KI, kann durch Eintrag {@code sentimentanalyse.basisurl}
	 * in Datei {@code application.properties} überschrieben werden.
	 * Der Default-Wert ist {@code localhost:12434} für den <i>Docker Model Runner</i>.
	 */
	@Value( "${sentimentanalyse.basisurl:http://localhost:12434}" )
	private String _basisUrl;

	/**
	 * Pfad für REST-API von KI, kann durch Eintrag {@code sentimentanalyse.pfad}
	 * in Datei {@code application.properties} überschrieben werden.
	 * Der Default-Wert ist {@code /engines/v1/chat/completions} für den
	 * <i>Docker Model Runner</i>.
	 */
	@Value( "${sentimentanalyse.pfad:/engines/v1/chat/completions}" )
	private String _restPfad;

	/** Technischer Name des zu verwendeten KI-Models (LLM). */
	@Value( "${sentimentanalyse.model:ai/mistral:7B-Q4_0}" )
	private String _model;

	/** API-Key; ist leerer String für Aufruf von <i>Docker Model Runner</i>. */
	@Value( "${sentimentanalyse.apikey:}" )
	private String _apiKey;

	
	/**
	 * {@code RestClient}-Objekt erzeugen; benötigt {@code basisUrl}, die evtl.
	 * in Konfigurationsdatei überschrieben ist, weshalb dies nicht im Konstruktor
	 * gemacht werden.
	 */
	@PostConstruct
	private void initialisiere() {

		_restClient = RestClient.builder()
				                .baseUrl( _basisUrl )
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

			final ChatMessage chatMessage = new ChatMessage( "user", prompt );
			final ChatAnfrage chatAnfrage =
									new ChatAnfrage(
											_model,
					                        List.of( chatMessage )
				                    );

            final ResponseEntity<String> responseEntity =
    										_restClient.post()
                                                       .uri( _restPfad )
                                                       .contentType( APPLICATION_JSON )
													   .header( "Authorization", "Bearer " + _apiKey ) // wird von Docker Model Runner ignoriert
                                                       .body( chatAnfrage )
                                                       .retrieve()
                                                       .toEntity( String.class );

            final String jsonString = responseEntity.getBody();
            // Beispiel-Response: https://gist.github.com/MDecker-MobileComputing/64bc680596b379751dc4049ac145b16d

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
	 * @param wurzelNode Knoten aus JSON-Dokument, der untersucht werden soll;
	 *                   {@code null}, wenn der Knoten ein unerwartetes Format
	 *                   hat
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


	/**
	 * Knoten aus JSON-Dokument daraufhin untersuchen, ob er ein Ergebnisfeld enthält.
	 *
	 * @param node Knoten, der untersucht werden soll
	 *
	 * @return {@code true} gdw. der Knoten ein erwartes Ergebnisfeld (nämlich
	 *         {@code sentiment} oder {@code confidence} hat
	 */
	private boolean hatErgebnisFelder( JsonNode node ) {

		return  node != null         &&
			   !node.isMissingNode() &&
			   ( node.has( "sentiment" ) || node.has( "confidence" ) );
	}
}
