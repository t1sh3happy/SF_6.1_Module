import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Scanner;

public class WeatherService {
    private static final String API_KEY = "ff89396b-c5d7-415c-832c-e501d38a1f16";
    private static final String API_URL = "https://api.weather.yandex.ru/v2/forecast";

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            int limit = 0;

            // Просим пользователя ввести количество дней в разрешенном диапазоне.
            while (limit < 1 || limit > 30) {
                System.out.print("Введите количество дней для подсчета средней температуры (от 1 до 11): "); // предел запроса для текущего api = 11 дней
                limit = scanner.nextInt();
                if (limit < 1 || limit > 30) {
                    System.out.println("Введите значение от 1 до 11.");
                }
            }

            double latitude = 55.75; // Широта для запроса (Москва, для примера)
            double longitude = 37.62; // Долгота для запроса (Москва, для примера)

            String jsonResponse = getWeatherData(latitude, longitude, limit);

            // Парсинг JSON ответа
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            // Вывод полного ответа
            System.out.println(jsonNode.toPrettyString());

            // Извлечение и вывод температуры 'fact.temp'
            JsonNode factNode = jsonNode.get("fact");
            if (factNode != null) {
                JsonNode tempNode = factNode.get("temp");
                if (tempNode != null) {
                    int temperature = tempNode.asInt();
                    System.out.println("Текущая температура: " + temperature + "°C");
                } else {
                    System.out.println("Информация о температуре недоступна в данный момент.");
                }
            } else {
                System.out.println("Информация недоступна на данный момент.");
            }

            // Вычисление средней температуры за период
            double averageTemperature = calculateAverageTemperature(jsonNode, limit);
            System.out.println(String.format("Средняя температура за %d дня/ей: %.2f°C", limit, averageTemperature));

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getWeatherData(double lat, double lon, int limit) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(API_URL + "?lat=" + lat + "&lon=" + lon + "&limit=" + (limit > 11 ? 11 : limit))) // ограничим запрос к API по известным лимитам
                .header("X-Yandex-Weather-Key", API_KEY)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Выводим статус ответа для диагностики.
        if (response.statusCode() != 200) {
            System.out.println("Error: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }

    private static double calculateAverageTemperature(JsonNode jsonNode, int limit) {
        double sumTemp = 0;
        JsonNode forecastsNode = jsonNode.get("forecasts");
        if (forecastsNode != null && forecastsNode.isArray()) {
            for (int i = 0; i < Math.min(limit, forecastsNode.size()); i++) {
                JsonNode dayPartNode = forecastsNode.get(i).get("parts").get("day");
                if (dayPartNode != null && dayPartNode.get("temp_avg") != null) {
                    int tempAvg = dayPartNode.get("temp_avg").asInt();
                    sumTemp += tempAvg;
                }
            }
        }
        return sumTemp / Math.min(limit, forecastsNode.size());
    }
}