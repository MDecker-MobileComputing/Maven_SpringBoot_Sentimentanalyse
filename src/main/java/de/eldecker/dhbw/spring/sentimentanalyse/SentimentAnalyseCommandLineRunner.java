package de.eldecker.dhbw.spring.sentimentanalyse;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import de.eldecker.dhbw.spring.sentimentanalyse.engine.SentimentAnalyseService;
import de.eldecker.dhbw.spring.sentimentanalyse.engine.SentimentErgebnis;


/**
 * Methode {@code run()} dieser Bean-Klasse wird unmittelbar nach Start der
 * Spring-Boot-Anwendung ausgeführt.  
 */
@Component
public class SentimentAnalyseCommandLineRunner implements CommandLineRunner {

	/** Bean mit eigentlicher KI-Abfrage. */
	private SentimentAnalyseService _sentimentAnalyseService; 

	
	/**
	 * Konstruktor für Dependency Injection.
	 */
	@Autowired
	public SentimentAnalyseCommandLineRunner( SentimentAnalyseService sentimentAnalyseService ) {
		
		_sentimentAnalyseService = sentimentAnalyseService;
	}

	
	/**
	 * Diese Methode wird unmittelbar nach Start der Spring-Boot-Anwendung ausgeführt. 
	 * 
	 * @param args Wird nicht ausgewertet.
	 */
	@Override
	public void run( String... args ) throws Exception {

		final Optional<SentimentErgebnis> ergebnisOptional = 
								_sentimentAnalyseService.sentimentAnalysieren( "A must-have for everyone" );
		
		if ( ergebnisOptional.isPresent() ) {
	
			System.out.println( ergebnisOptional.get() );
			
		} else {
			
			System.out.println( "Keine Antwort von KI erhalten" );
		}
	}
		
}
