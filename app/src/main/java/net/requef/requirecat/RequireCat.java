package net.requef.requirecat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RequireCat {
    private static final Logger logger = new Logger();

    public static void main(String[] args) {
        // TODO: For now we'll use the sample testing directory.
        // Later, the specific command line argument will be used.
        final String rootPath = "/home/requef/code/hw/software-design/lecture/2-RequireCat/app/src/test/resources/root-invalid-require";
        final var rootDirectory = new File(rootPath);
        final var outputFile = new File(rootPath, "out.txt");

        logger.info("Starting for root folder: '%s'", rootPath);
        final var fileNodes = findFiles(rootDirectory, outputFile);

        // Check that all dependencies have been resolved.
        for (final var dependencies : fileNodes.values()) {
            for (final var dependency : dependencies) {
                if (!fileNodes.containsKey(dependency)) {
                    logger.error("File '%s' required by '%s' does not exist, skipping it", dependency.getPath(),
                            dependency.getPath());
                }
            }
        }

        logger.info("Analyzed %d files, starting topological sort", fileNodes.size());
        final var sortedFiles = new TopologicalSorter<>(fileNodes.keySet(), fileNodes::get).sort();
        // Files contain a circular dependency.
        // TODO: Print the file and the dependency that caused the circular dependency.
        if (sortedFiles == null) {
            logger.error("Files contain a circular dependency, aborting");
            return;
        }

        logger.info("Files sorted, compiling output file");
        // Creating output file.
        // TODO: Add output file command line argument.

        // Output file already exists.
        if (outputFile.isFile()) {
            logger.warn("Output file '%s' already exists, overwriting it", outputFile.getPath());
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
            logger.error("Failed to write to the output file: %s", e.getMessage());
            return;
        }

        logger.success("Output of %d files saved to '%s'", sortedFiles.size(), rootPath);
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
    private static @NotNull Map<File, List<File>> findFiles(final @NotNull File rootDirectory,
                                                            final @Nullable File outFile) {
        if (!rootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Root path is not a directory");
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

                    final var files = getFileDependencies(rootDirectory, subObject);
                    if (files != null) {
                        fileNodes.put(subObject, files);
                    }
                }
            }
        }

        return fileNodes;
    }

    /**
     * Parses the given file and returns a list of file's dependencies.
     * Returns null if the file cannot be read.
     * @param rootDirectory The root directory to be used to resolve relative path in 'require' statements.
     * @param file The file to parse.
     * @return A list of files that the given file depends on or null if the file cannot be read.
     */
    private static List<File> getFileDependencies(final @NotNull File rootDirectory,
                                                  final @NotNull File file) {
        try (final var inputScanner = new Scanner(file)) {
            final List<File> dependencies = new ArrayList<>();
            int lineNumber = 0;

            while (inputScanner.hasNextLine()) {
                lineNumber++;
                final String line = inputScanner.nextLine();

                if (!line.startsWith("require ")) {
                    continue;
                }

                final var dependencyFilePath = parseRequireStatement(line);
                if (dependencyFilePath.isError()) {
                    logger.warn("(%s:%d) Could not parse 'require' statement, skipping it: %s", file.getPath(),
                            lineNumber, dependencyFilePath.getError());
                    continue;
                }

                final var dependencyFile = new File(rootDirectory, dependencyFilePath.getValue());
                if (!dependencyFile.isFile()) {
                    logger.error("(%s:%d) 'require' statement points to an invalid file, skipping it",
                            file.getPath(), lineNumber);
                    continue;
                }

                dependencies.add(dependencyFile);
            }

            return dependencies;
        } catch (final IOException e) {
            logger.warn("Can't read file '%s', skipping it", file.getPath());
            return null;
        }
    }

    /**
     * Parses the given 'require' statement and returns the dependency's file path or an error.
     * The given statement is already assumed to start with "require ".
     * Returns an error if the statement is invalid.
     * A valid require statement is of the form "require ‘path/to/file’".
     * Note that this method does not check the existence of the defined path.
     * @param statement The 'require' statement to parse.
     * @return The dependency's file name or error if the statement is invalid.
     */
    private static @NotNull ErrorOr<String> parseRequireStatement(final @NotNull String statement) {
        if (!statement.startsWith("require ")) {
            throw new IllegalArgumentException("statement does not start with 'require '");
        }

        final var dependencyFormat = statement.substring(8);
        if (dependencyFormat.isEmpty()) {
            return ErrorOr.error("empty statement");
        }

        if (!dependencyFormat.startsWith("‘")) {
            return ErrorOr.error("statement must start with '‘', but starts with '%s'", dependencyFormat.charAt(0));
        }

        if (!dependencyFormat.endsWith("’")) {
            return ErrorOr.error("statement must end with '’', but ends with '%s'", dependencyFormat.charAt(dependencyFormat.length() - 1));
        }

        final var dependencyFilepath = dependencyFormat.substring(1, dependencyFormat.length() - 1);
        if (dependencyFilepath.isEmpty()) {
            return ErrorOr.error("empty dependency filepath");
        }

        return ErrorOr.ok(dependencyFilepath);
    }
}
