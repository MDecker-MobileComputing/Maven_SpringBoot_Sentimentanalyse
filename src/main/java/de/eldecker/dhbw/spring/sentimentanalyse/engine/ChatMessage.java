package de.eldecker.dhbw.spring.sentimentanalyse.engine;


/**
 * Einzelner Nachricht/Prompt innerhalb eines Request.
 * 
 * @param role Rolle, z.B. "user", "system" oder "tool" (letzteres
 *             für Antwort von externem System)
 * 
 * @param content Eigentlicher Prompt, den die KI verarbeiten soll.
 */
public record ChatMessage( 
		                   String role, 
		                   String content 
		                 ) {
}
