package net.requef.requirecat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;

public class RequireCat {
    private static final Logger logger = new Logger();

    public static void main(String[] args) {
        // Parsing arguments.
        if (args.length == 0) {
            printUsage();
            exitWithError("At least one argument is required");
            return;
        }

        final var rootPath = args[0];
        final var rootDirectory = new File(rootPath);
        if (!rootDirectory.isDirectory()) {
            exitWithError("'%s' is not a valid directory", rootPath);
            return;
        }

        boolean isQuietMode = false;
        var outputFile = new File(rootDirectory, "out.txt");
        for (int i = 1; i < args.length; ++i) {
            if ("-q".equals(args[i])) {
                isQuietMode = true;
            } else if (args[i].startsWith("-o=")) {
                final var outputFilePath = args[i].substring(3);
                if (outputFilePath.isEmpty() || !isValidPath(outputFilePath)) {
                    exitWithError("'%s' is not a valid path", outputFilePath);
                    return;
                }
                outputFile = new File(rootDirectory, outputFilePath);
            } else {
                printUsage();
                exitWithError("Unrecognised argument: '%s'", args[i]);
                return;
            }
        }

        logger.setLogLevel(isQuietMode ? LogLevel.WARN : LogLevel.INFO);
        logger.info("Starting for root folder: '%s'", rootPath);
        final var fileNodes = findFiles(rootDirectory, outputFile);

        // Check that all dependencies have been resolved.
        assertDependenciesResolved(fileNodes);

        logger.info("Analyzed %d files, starting topological sort", fileNodes.size());
        final var sorter = new TopologicalSorter<>(fileNodes.keySet(), fileNodes::get);
        final var sortedFiles = sorter.sort();

        // Files contain a circular dependency.
        if (sortedFiles == null) {
            logCycle(sorter);
            return;
        }

        logger.info("Files sorted, compiling output file");
        writeOutputFile(outputFile, sortedFiles);
        logger.success("Output of %d files saved to '%s'", sortedFiles.size(), rootPath);
    }

    /**
     * Checks that all dependencies have been resolved.
     * Exits with an error if any dependency is missing.
     * @param fileNodes The map of file nodes.
     */
    private static void assertDependenciesResolved(final @NotNull Map<File, List<File>> fileNodes) {
        for (final var dependencies : fileNodes.values()) {
            for (final var dependency : dependencies) {
                if (!fileNodes.containsKey(dependency)) {
                    exitWithError("File '%s' required by '%s' does not exist", dependency.getPath(),
                            dependency.getPath());
                    return;
                }
            }
        }
    }

    /**
     * Logs the cycle (a circular dependency) that was found in the dependency graph.
     * This method assumes that such cycle exists.
     * @param sorter The sorter that found the cycle.
     * @param <T> The type of the nodes in the graph.
     */
    private static <T> void logCycle(final @NotNull TopologicalSorter<T> sorter) {
        final var cycle = sorter.getCycle();
        assert cycle != null && !cycle.isEmpty();

        final var stringBuilder = new StringBuilder();
        stringBuilder.append("Files contain a circular dependency:%n");
        stringBuilder.append(cycle.get(0));
        stringBuilder.append("%n");

        for (int i = 1; i < cycle.size(); i++) {
            stringBuilder.append("\t<- ");
            stringBuilder.append(cycle.get(i));
            stringBuilder.append("%n");
        }

        stringBuilder.append("\t<- ");
        stringBuilder.append(cycle.get(0));
        stringBuilder.append("%n\t<- ...");

        exitWithError(stringBuilder.toString());
    }

    /**
     * Writes the result of topologically sorted files into the output file.
     * @param outputFile The output file.
     * @param sortedFiles The sorted files.
     */
    private static void writeOutputFile(final @NotNull File outputFile,
                                        final @NotNull List<File> sortedFiles) {
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
            exitWithError("Failed to write to the output file: %s", e.getMessage());
        }
    }

    /**
     * Checks if the given path is valid.
     * @param path The path to check.
     * @return True if the path is valid, false otherwise.
     */
    private static boolean isValidPath(final @NotNull String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ignored) {
            return false;
        }
        return true;
    }

    /**
     * Prints the usage message of the program.
     */
    private static void printUsage() {
        System.out.println("Usage: java -jar RequireCat.jar <root_directory> [-q] [-o=<output_file>]");
        System.out.println("Options:");
        System.out.println("\t-q\tQuiet mode. Only log warnings and errors.");
        System.out.println("\t-o\tOutput file. If not specified, the output will be saved to 'out.txt'.");
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
                    exitWithError("(%s:%d) 'require' statement points to an invalid file",
                            file.getPath(), lineNumber);
                    return null;
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

    /**
     * Immediately exits the program with an error log.
     * @param message The error message.
     * @param args The arguments to be formatted into the message.
     */
    private static void exitWithError(final @NotNull String message, final @NotNull Object... args) {
        logger.error(message + ", aborting", args);
        System.exit(1);
    }
}
