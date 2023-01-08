package net.requef.requirecat;

import java.util.*;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TopologicalSorter<T> {
    private final Collection<T> nodes;
    private final Function<T, Collection<T>> dependencyGetter;

    private List<T> cycle;

    public TopologicalSorter(final @NotNull Collection<T> nodes,
                             final @NotNull Function<T, Collection<T>> dependencyGetter) {
        this.nodes = nodes;
        this.dependencyGetter = dependencyGetter;
    }

    /**
     * Sorts the nodes in topological order.
     * Returns null if the nodes contain a circular dependency.
     * @return The sorted nodes or null if there is a circular dependency.
     */
    public @Nullable List<T> sort() {
        final var sortedNodes = new Stack<T>();
        // TODO: nodes should not really depend on the hashCode method of T.
        final Set<T> unvisitedNodes = new HashSet<>(nodes);
        final Set<T> visitingNodes = new HashSet<>();

        while (!unvisitedNodes.isEmpty()) {
            visit(unvisitedNodes.iterator().next(), unvisitedNodes, visitingNodes, sortedNodes);
        }

        return cycle == null ? sortedNodes : null;
    }

    /**
     * Returns some cycle (a circular dependency) in the graph if there are any or null.
     *
     * @return A cycle or null.
     */
    public @Nullable List<T> getCycle() {
        return cycle;
    }

    private void visit(final @NotNull T node, 
                       final @NotNull Set<T> unvisitedNodes,
                       final @NotNull Set<T> visitingNodes, 
                       final @NotNull Stack<T> sortedNodes) {
        if (!unvisitedNodes.contains(node)) {
            return;
        }

        if (visitingNodes.contains(node)) {
            // TODO: cycle is just a copy of visitingNodes, maybe we can just return it.
            cycle = new ArrayList<>(visitingNodes);
            return;
        }

        visitingNodes.add(node);

        for (final var dependency : dependencyGetter.apply(node)) {
            if (cycle == null) {
                visit(dependency, unvisitedNodes, visitingNodes, sortedNodes);
            }
        }

        visitingNodes.remove(node);
        unvisitedNodes.remove(node);
        sortedNodes.push(node);
    }
}
