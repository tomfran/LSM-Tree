package com.tomfran.lsm;

import com.tomfran.lsm.tree.LSMTree;
import com.tomfran.lsm.types.ByteArrayPair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    static final String DIRECTORY = "LSM-data";

    public static void main(String[] args) throws IOException {

        if (new File(DIRECTORY).exists())
            deleteDir();

        LSMTree tree = new LSMTree(3, 2, DIRECTORY);

        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\n");

        System.out.println(
                """
                LSM Tree console

                Commands:
                - ins <key> <value> : insert a key-value pair
                - get <key>         : get a value for a key
                - del <key>         : delete a key-value pair
                - exit              : exit the application

                """
        );

        boolean exit = false;

        while (!exit) {
            System.out.print("Enter a command: ");
            String command = scanner.nextLine();

            var parts = command.split(" ");

            switch (parts[0]) {
                case "exit" -> {
                    System.out.println("Exiting...");
                    exit = true;
                }
                case "ins" -> tree.add(new ByteArrayPair(parts[1].getBytes(), parts[2].getBytes()));
                case "del" -> tree.delete(parts[1].getBytes());
                case "get" -> {
                    String key = parts[1];
                    byte[] value = tree.get(key.getBytes());

                    var msg = (value == null || value.length == 0) ? "No value found for key " + key :
                            "Value for key " + key + " is " + new String(value);
                    System.out.println(msg);
                }
                default -> System.out.println("Unknown command: " + command);
            }
            System.out.println();
        }
        tree.stop();
        scanner.close();

        deleteDir();
    }

    static private void deleteDir() throws IOException {
        try (var files = Files.list(Path.of(DIRECTORY))) {
            files.forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        Files.delete(Path.of(DIRECTORY));
    }

}
