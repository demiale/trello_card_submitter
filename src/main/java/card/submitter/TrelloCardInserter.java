package card.submitter;

import card.submitter.func.HttpAssistant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TrelloCardInserter {

    public static void main(String[] args) {

        if (args.length > 0) {
            Path fileName = Paths.get(args[0]);
            if (Files.exists(fileName) && Files.isReadable(fileName)) {
                HttpAssistant.submitCardsFromFile(fileName);
            } else {
                System.out.println("Files does not exist or cannot be read");
            }
        } else {
            System.out.println("Please provide the full file name as a parameter");
        }
    }

}
