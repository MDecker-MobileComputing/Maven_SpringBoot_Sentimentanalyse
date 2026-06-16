package de.eldecker.dhbw.spring.sentimentanalyse.engine;

import java.util.List;


/**
 * Record-Klasse für Payload, die über REST an KI geschickt wird, also v.a.
 * den Prompt enthält.
 * 
 * @param model Name des zu verwendeten KI-Models, z.B. "ai/mistral:7B-Q4_0"
 *              bei Verwendung von <i>Docker Model Runner</i>.
 * 
 * @param messages Liste der Nachrichten, die an KI übergeben werden
 */
public record ChatAnfrage( 
						   String            model,
		                   List<ChatMessage> messages 
		                 ) {
}
