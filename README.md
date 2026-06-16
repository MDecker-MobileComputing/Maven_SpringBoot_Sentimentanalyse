# Sentimentanalyse mit über REST-API angebundener KI #

<br>

Das Repo enthält eine mit Java programmierte Spring-Boot-Anwendung, die über die Konsole
eine Nutzerbewertung (z.B. für ein Produkt in einem Online-Shop) einliest und diesen
Text dann per REST-API an eine LLM-KI schickt.
Es wird das REST-API-Format von *OpenAI* verwendet.

<br>

Standardmäßig wird eine lokale Instanz des [Docker Model Runner](https://docs.docker.com/ai/model-runner/)
verwendet.
Durch Eintrag der entsprechenden Konfigurationswerte in `application-gemini.properties` (v.a. API-Key)
und Start der Anwendung mit dem Spring-Profil `gemini` (z.B. mit Skript `maven_start_gemini.bat`) kann
die Anwendung auch gegen ein Google-Gemini-Modell ausgeführt werden.

<br>

![Screenshot](screenshot_1.png)
Verwendetes AI-Model in *Docker Model Runner*:
[mistral:7B-Q4_0](https://hub.docker.com/layers/ai/mistral/7B-Q4_0/)

<br>

----

## License ##

<br>

See the [LICENSE file](LICENSE.md) for license rights and limitations (BSD 3-Clause License).

<br>
