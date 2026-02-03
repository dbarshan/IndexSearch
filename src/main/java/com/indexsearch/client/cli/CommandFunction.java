package com.indexsearch.client.cli;
import com.indexsearch.client.model.CollectionInfo;
import com.indexsearch.client.service.ClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;  

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommandFunction {
    private final ClientService clientService; 
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void createCollection(String... args) throws Exception {
        requireArgs(args, 2);
        JsonNode mapping = parseJsonArg(args[1]);
        clientService.createCollection(mapping);
        System.out.println("Collection created.");
    }

    public void deleteCollection(String... args) {
        requireArgs(args, 2);
        clientService.deleteCollection(args[1]);
        System.out.println("Collection deleted.");
    }

    public void listCollections(String... args) {
        requireArgs(args, 1);
        List<CollectionInfo> collections = clientService.listCollections();
        if (collections == null || collections.isEmpty()) {
            System.out.println("No collections found.");
            return;
        }
        for (CollectionInfo info : collections) {
            System.out.println(info.getName()
                    + " | docs=" + info.getDocumentCount()
                    + " | size=" + info.getSizeHuman());
        }
    }

    public void addDocument(String... args) throws Exception {
        requireArgs(args, 3);
        String collection = args[1];
        JsonNode document = parseJsonArg(args[2]);
        JsonNode created = clientService.addDocument(collection, document);
        System.out.println(created.toString());
    }

    public void updateDocument(String... args) throws Exception {
        requireArgs(args, 4);
        String collection = args[1];
        String docId = args[2];
        JsonNode document = parseJsonArg(args[3]);
        JsonNode updated = clientService.updateDocument(collection, docId, document);
        System.out.println(updated.toString());
    }

    public void getDocument(String... args) {
        requireArgs(args, 3);
        JsonNode doc = clientService.getDocument(args[1], args[2]);
        if (doc == null) {
            System.out.println("Document not found.");
        } else {
            System.out.println(doc.toString());
        }
    }

    public void deleteDocument(String... args) {
        requireArgs(args, 3);
        clientService.deleteDocument(args[1], args[2]);
        System.out.println("Document deleted.");
    }

    public void search(String... args) {
        requireArgs(args, 3);
        List<JsonNode> results = clientService.search(args[1], args[2]);
        System.out.println(results.toString());
    }

    public void searchSql(String... args) {
        requireArgs(args, 2);
        String sqlQuery = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        List<JsonNode> results = clientService.sql(sqlQuery);
        System.out.println(results.toString());
    }

    public void rebuildIndex(String... args) {
        requireArgs(args, 2);
        System.out.println("Index rebuild is a server-side operation.");
    }

    public JsonNode parseJsonArg(String arg) throws Exception {
        String json = arg;
        if (arg.startsWith("@")) {
            json = Files.readString(Path.of(arg.substring(1)));
        }
        return objectMapper.readTree(json);
    }

    public void requireArgs(String[] args, int count) {
        if (args.length < count) {
            printUsage();
            throw new IllegalArgumentException("Expected at least " + (count - 1) + " arguments.");
        }
    }

    public void printUsage() {
        System.out.println("IndexSearch CLI");
        System.out.println("Commands:");
        System.out.println("  create-collection <mapping-json|@file>");
        System.out.println("  list-collections");
        System.out.println("  delete-collection <collection>");
        System.out.println("  add-doc <collection> <document-json|@file>");
        System.out.println("  update-doc <collection> <docId> <document-json|@file>");
        System.out.println("  get-doc <collection> <docId>");
        System.out.println("  delete-doc <collection> <docId>");
        System.out.println("  search <collection> <query>");
        System.out.println("  sql <sql-query>");
        System.out.println("  rebuild-index <collection>");
    }

}
