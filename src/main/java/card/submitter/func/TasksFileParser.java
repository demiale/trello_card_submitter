package card.submitter.func;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static card.submitter.func.HttpAssistant.*;


public class TasksFileParser {

    public static List<String> getRequestParamsForTasks(String fileName) {

        return getParamsFromFile(getTasksFromFile(fileName));

    }

    private static List<String> getParamsFromFile(List<Map<String,String>> tasks) {

        List<String> requestParams = new ArrayList<>();

        for (Map<String, String> task : tasks) {

            List<String> params = new ArrayList<>();

            String[] labels = task.get(ID_LABELS_KEY).split(",");
            List<String> labelsIDs = new ArrayList<>();

            for (String label : labels) {
                labelsIDs.add(getIdForName(getAllLabelsOnBoard(task.get(BOARD_NAME_KEY)), label));
            }
            task.put(ID_LABELS_KEY, String.join(",", labelsIDs));

            task.put(ID_LIST_KEY, getIdForName(getAllListsOnBoard(task.get(BOARD_NAME_KEY)), task.get(ID_LIST_KEY)));
            task.remove(BOARD_NAME_KEY);
            task.remove(LIST_NAME_KEY);
            task.put(NAME_KEY, URLEncoder.encode(task.get(NAME_KEY), StandardCharsets.UTF_8));
            task.put(DESC_KEY, URLEncoder.encode(task.get(DESC_KEY), StandardCharsets.UTF_8));

            task.put(ID_LABELS_KEY, URLEncoder.encode(task.get(ID_LABELS_KEY), StandardCharsets.UTF_8));

            for (Map.Entry<String, String> entry : task.entrySet()) {

                String param = String.join("=",
                        entry.getKey(), entry.getValue());
                params.add(param);
            }

            String queryParamsStr = "&" + String.join("&", params);
            requestParams.add(String.join("&", queryParamsStr));
        }

        return requestParams;
    }

    private static List<Map<String,String>> getTasksFromFile(String fileName) {

        final List<Map<String,String>> tasks = new ArrayList<>();

        try (
                Reader reader = Files.newBufferedReader(Paths.get(fileName));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.newFormat(';')
                        .withFirstRecordAsHeader()
                        .withHeader("BoardName","ListName","TaskName","Description","DueDate","Labels")
                        .withIgnoreHeaderCase()
                        .withTrim());
        ) {
            for (CSVRecord csvRecord : csvParser) {
                String boardName = csvRecord.get("BoardName");
                String listName = csvRecord.get("ListName");
                String taskName = csvRecord.get("TaskName");
                String description = csvRecord.get("Description");
                String dueDate = csvRecord.get("DueDate");
                String labels = csvRecord.get("Labels");

                Map<String, String> taskDetails = new HashMap<>();
                if (boardName != null) taskDetails.put(BOARD_NAME_KEY, boardName);
                if (listName != null) taskDetails.put(ID_LIST_KEY, listName);
                if (taskName != null) taskDetails.put(NAME_KEY, taskName);
                if (description != null) taskDetails.put(DESC_KEY, description);
                if (dueDate != null) taskDetails.put(DUE_KEY, dueDate);
                if (labels != null) taskDetails.put(ID_LABELS_KEY, labels);

                tasks.add(taskDetails);

            }
        } catch (IOException e) {
            System.out.println("Error reading from file. Please ensure the format is correct and content is not corrupted");
            e.printStackTrace();
        }

        return tasks;
    }

    private static final String ID_LIST_KEY = "idList";
    private static final String BOARD_NAME_KEY = "boardName";
    private static final String ID_LABELS_KEY = "idLabels";
    private static final String NAME_KEY = "name";
    private static final String DESC_KEY = "desc";
    private static final String DUE_KEY = "due";
    private static final String LIST_NAME_KEY = "listName";

}
