package card.submitter.func;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static card.submitter.func.TasksFileParser.getRequestParamsForTasks;


public class HttpAssistant {


    public static void submitCardsFromFileAsync(Path fileName) {

        System.out.println("Submitting tasks from " + fileName.toString());

        List<String> paramSets = getRequestParamsForTasks(fileName.toString());

        Long submitted = paramSets.stream()
                .map(HttpAssistant::sendCardPOSTRequestAsync)
                .collect(Collectors.toList())
                .stream().map(CompletableFuture::join)
                .filter(code -> code == 200)
                .count();

        System.out.println(submitted + " tasks submitted successfully!");

    }


    private static CompletableFuture<Integer> sendCardPOSTRequestAsync(String withParams) {

        return client.sendAsync
                (constructPOSTRequest(URI.create(BASE_URL + CARDS_ENDPOINT + AUTH_PARAMS + withParams)),
                        HttpResponse.BodyHandlers.discarding())
                .thenApply(HttpAssistant::getResponseCode)
                .exceptionally(e -> -1);

    }


    private static HttpRequest constructPOSTRequest(URI uri) {
        return HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
    }


    private static int getResponseCode(HttpResponse<Void> response) {
        return response.statusCode();
    }


    static String getIdForName(Map<String, String> entries, String searchValue) {

        return entries.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(searchValue))
                .findFirst().orElseThrow(IllegalArgumentException::new)
                .getKey();

    }


    static Map<String, String> getAllLabelsOnBoard(String boardName) {

        String boardId = getIdForName(getAllBoardsIdNames(), boardName);

        URI uri = constructURIFor(boardId, LABELS_ENDPOINT, NAME_PARAM);

        HttpResponse<String> response = sendHTTPRequest(constructGETRequest(uri));

        if (response == null) throw new IllegalArgumentException("No response was received");

        return parseResponseAsMap(response);

    }


    static Map<String, String> getAllListsOnBoard(String boardName) {

        String boardId = getIdForName(getAllBoardsIdNames(), boardName);

        URI uri = constructURIFor(boardId, LISTS_ENDPOINT, NAME_PARAM);

        HttpResponse<String> response = sendHTTPRequest(constructGETRequest(uri));

        if (response == null) throw new IllegalArgumentException("No response was received");

        return parseResponseAsMap(response);

    }


    private static Map<String, String> getAllBoardsIdNames() {

        URI uri = URI.create(BASE_URL + BOARDS_ENDPOINT + AUTH_PARAMS + NAME_PARAM);

        HttpResponse<String> response = sendHTTPRequest(constructGETRequest(uri));

        if (response == null) throw new IllegalArgumentException("No response was received");

        return parseResponseAsMap(response);

    }


    private static URI constructURIFor(String boardID, String endPoint, String params) {
        return URI.create(BASE_URL + BOARD_ENDPOINT + boardID + endPoint + AUTH_PARAMS + params);
    }


    private static HttpRequest constructGETRequest(URI uri) {
        return HttpRequest.newBuilder(uri).build();
    }


    private static HttpResponse<String> sendHTTPRequest(HttpRequest request) {

        HttpResponse<String> response = null;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return response;

    }


    private static Map<String, String> parseResponseAsMap(HttpResponse<String> response) {
        TypeReference<List<Map<String, String>>> typeRef = new TypeReference<>() {};
        List<Map<String, String>> list = new ArrayList<>();

        try {
            list = objectMapper.readValue(response.body(), typeRef);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Map<String, String> resultingMap = new HashMap<>();

        list.forEach(map -> resultingMap.put(map.get("id"), map.get("name")));

        return resultingMap;
    }


    // Replace placeholders with valid key and token

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String key = "<Trello_API_key>";
    private static final String token = "<Trello_API_token>";

    private static final String BASE_URL = "https://api.trello.com/";
    private static final String BOARD_ENDPOINT = "1/boards/";
    private static final String CARDS_ENDPOINT = "1/cards";
    private static final String LISTS_ENDPOINT = "/lists";
    private static final String LABELS_ENDPOINT = "/labels";

    private static final String AUTH_PARAMS = String.format("?key=%s&token=%s", key, token);

    private static final String BOARDS_ENDPOINT = "1/members/me/boards";

    private static final String NAME_PARAM = "&fields=name";

}
