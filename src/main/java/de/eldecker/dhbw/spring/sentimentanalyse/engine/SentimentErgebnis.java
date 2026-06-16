package de.eldecker.dhbw.spring.sentimentanalyse.engine;


/**
 * Record-Klasse für Antwort von KI.
 * 
 * @param sentiment Einstufung Sentiment
 * 
 * @param confidence Wie sicher ist sich die KI (0.0: unsicher; 1.0: total sicher)
 */
public record SentimentErgebnis( String sentiment, 
		                         float confidence ) {
}
