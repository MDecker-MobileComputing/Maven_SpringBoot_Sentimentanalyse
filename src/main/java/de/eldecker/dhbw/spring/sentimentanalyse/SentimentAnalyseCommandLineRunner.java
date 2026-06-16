package de.eldecker.dhbw.spring.sentimentanalyse;

import java.util.Optional;
import java.util.Scanner;

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

    /** Text-Scanner für Einlesen Nutzerkommentar von Tastatur. */
    final Scanner _scanner = new Scanner( System.in );
	
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
	 * @param args Wird nicht ausgewertet
	 */
	@Override
	public void run( String... args ) throws Exception {

		while ( true ) {

			final Optional<String> kommentarOptional = kommentarEinlesen();
			if ( kommentarOptional.isEmpty() ) {
				
				System.out.println( "\nKein Kommentar, beende Programm.\n" );
				break;
			}
			
			final String kommentar = kommentarOptional.get();
			
			final Optional<SentimentErgebnis> ergebnisOptional = 
					_sentimentAnalyseService.sentimentAnalysieren( kommentar );

			if ( ergebnisOptional.isEmpty() ) {
				
				System.out.println( "\nFehler: Keine Antwort von KI erhalten.\n" );
				
			} else {
				
				final SentimentErgebnis sentimentErgebnis = ergebnisOptional.get();
				System.out.println( "\nAuswertung: " + sentimentErgebnis + "\n" );
			}
		}		
	}
	
	
	/**
	 * Kommentar von Tastatur einlesen.
	 * 
	 * @return Optional enthält Kommentar, oder leer 
	 */
    private Optional<String> kommentarEinlesen() {

        System.out.print ( "\nNutzerkommentar einlesen (leer für Programmende) > " );
        String nutzereingabeString1 = _scanner.nextLine();

        nutzereingabeString1 = nutzereingabeString1.trim();
        if ( nutzereingabeString1.isBlank() ) {

            return Optional.empty();

        } else {

            return Optional.of( nutzereingabeString1 );
        }
    }	
		
}
