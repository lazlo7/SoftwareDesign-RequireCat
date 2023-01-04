package net.requef.requirecat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RequireCat {
    public static void main(String[] args) {
        // TODO: For now we'll use the sample testing directory.
        // Later, the specific command line argument will be used.
        final String rootPath = "/home/requef/code/hw/software-design/lecture/2-RequireCat/app/src/test/resources/sample-root-1";
        final var rootDirectory = new File(rootPath);
        final var outputFile = new File(rootPath, "out.txt");
        final var fileNodes = findFiles(rootDirectory, outputFile);
        printfInfo("Working with %s files%n", fileNodes.size());

        // Check that all dependencies have been resolved.
        for (final var dependencies : fileNodes.values()) {
            for (final var dependency : dependencies) {
                if (!fileNodes.containsKey(dependency)) {
                    printfError("File '%s' required by '%s' does not exist, skipping it.", dependency.getPath(),
                            dependency.getPath());
                }
            }
        }

        printfInfo("Starting topological sort%n");
        final var sortedFiles = new TopologicalSorter<>(fileNodes.keySet(), fileNodes::get).sort();
        // Files contain a circular dependency.
        // TODO: Print the file and the dependency that caused the circular dependency.
        if (sortedFiles == null) {
            printfError("Files contain a circular dependency, aborting.%n");
            return;
        }

        printfInfo("Starting to write to output file%n");
        // Creating output file.
        // TODO: Add output file command line argument.

        // Output file already exists.
        if (outputFile.isFile()) {
            printfWarning("Output file '%s' already exists, overwriting it.%n", outputFile.getPath());
        }

        try (final var outputWriter = new FileWriter(outputFile, false)) {
            for (final var file : sortedFiles) {
                final var inputScanner = new Scanner(file);
                while (inputScanner.hasNextLine()) {
                    outputWriter.write(inputScanner.nextLine());
                    outputWriter.write(System.lineSeparator());
                }
                inputScanner.close();
            }
        } catch (final IOException e) {
            printfError("Failed to write to the output file: %s%n", e.getMessage());
            printfError("Stacktrace: %n");
            e.printStackTrace();
            return;
        }

        // TODO: Add success logging.
        System.out.printf("[success] Output of %d files saved to %s%n", sortedFiles.size(), rootPath);
    }

    /**
     * Finds all files in the given 'root' directory and returns a list of the
     * respected file paths with their dependencies.
     * Traverses the directories in a breadth-first search manner.
     * 
     * @param rootDirectory The root directory to search for files.
     * @param outFile The output file to be ignored to avoid infinite looping or null.
     * @return A list of file paths with their dependencies.
     */
    private static @NotNull Map<File, List<File>> findFiles(@NotNull final File rootDirectory,
                                                            final @Nullable File outFile) {
        if (!rootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Root path is not a directory.");
        }

        final var fileNodes = new HashMap<File, List<File>>();

        final Queue<File> directoryQueue = new LinkedList<>();
        directoryQueue.add(rootDirectory);

        while (!directoryQueue.isEmpty()) {
            final var currentDirectory = directoryQueue.remove();
            final File[] subObjects = currentDirectory.listFiles();

            if (subObjects == null) {
                continue;
            }

            for (final File subObject : subObjects) {
                if (subObject.isDirectory()) {
                    directoryQueue.add(subObject);
                } else if (subObject.isFile()) {
                    // Ignore the output file.
                    if (subObject.equals(outFile)) {
                        continue;
                    }

                    final var files = parseFile(rootDirectory.getPath(), subObject);
                    if (files != null) {
                        fileNodes.put(subObject, files);
                    }
                }
            }
        }

        return fileNodes;
    }

    /**
     * Parses the given file and returns a FileNode with the file's dependencies.
     * 
     * @param rootPath The file to parse.
     * @return A FileNode with the file's dependencies.
     */
    private static List<File> parseFile(@NotNull final String rootPath, @NotNull final File file) {
        // TODO: Add proper require parsing and error checking.
        try (final var inputScanner = new Scanner(file)) {
            final List<File> dependencies = new ArrayList<>();
            while (inputScanner.hasNextLine()) {
                final String line = inputScanner.nextLine();
                if (line.startsWith("require ")) {
                    final var dependencyFormat = line.substring(8);
                    if (dependencyFormat.length() < 3 || !dependencyFormat.startsWith("‘")
                            || !dependencyFormat.endsWith("’")) {
                        printfWarning("File '%s' contains an invalid require statement, skipping it.%n",
                                file.getPath());
                        continue;
                    }

                    final var dependencyFilepath = dependencyFormat.substring(1, dependencyFormat.length() - 1);
                    final var dependencyFile = new File(rootPath, dependencyFilepath);

                    if (!dependencyFile.isFile()) {
                        // For now, we'll simply skip the file
                        printfWarning("File '%s' required by '%s' does not exist, skipping it.%n",
                                dependencyFile.getAbsolutePath(),
                                file.getPath());
                        continue;
                    }

                    dependencies.add(dependencyFile);
                }
            }

            return dependencies;
        } catch (final IOException e) {
            printfWarning("Can't read file '%s', skipping it%n", file.getPath());
            return null;
        }
    }

    // TODO: Devise a proper logging system.
    // TODO: Add console colors.
    private static void printfInfo(@NotNull final String format, @NotNull final Object... args) {
        System.out.printf("[info] " + format, args);
    }

    private static void printfWarning(@NotNull final String format, @NotNull final Object... args) {
        System.out.printf("[warning] " + format, args);
    }

    private static void printfError(@NotNull final String format, @NotNull final Object... args) {
        System.out.printf("[error] " + format, args);
    }
}
