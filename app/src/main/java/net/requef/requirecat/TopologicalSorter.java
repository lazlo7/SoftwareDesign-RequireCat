package net.requef.requirecat;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

public class TopologicalSorter<T> {
    private final @NonNull Collection<T> nodes;
    private final @NonNull Function<T, Collection<T>> dependencyGetter;

    private boolean hasCircularDependency = false;

    public TopologicalSorter(final @NonNull Collection<T> nodes,
                             final @NonNull Function<T, Collection<T>> dependencyGetter) {
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
        final Set<T> unvisitedNodes = new HashSet<T>(nodes); 
        final Set<T> visitingNodes = new HashSet<T>();

        while (!unvisitedNodes.isEmpty()) {
            visit(unvisitedNodes.iterator().next(), unvisitedNodes, visitingNodes, sortedNodes);
        }
        
        return hasCircularDependency ? null : sortedNodes;
    }

    private void visit(final @NonNull T node, 
                       final @NonNull Set<T> unvisitedNodes,
                       final @NonNull Set<T> visitingNodes, 
                       final @NonNull Stack<T> sortedNodes) {
        if (hasCircularDependency) {
            return;
        }

        if (!unvisitedNodes.contains(sortedNodes)) {
            return;
        }

        if (visitingNodes.contains(node)) {
            hasCircularDependency = true;
            return;
        }

        visitingNodes.add(node);

        for (final var dependency : dependencyGetter.apply(node)) {
            visit(dependency, unvisitedNodes, visitingNodes, sortedNodes);
        }

        visitingNodes.remove(node);
        unvisitedNodes.remove(node);
        sortedNodes.push(node);
    }
}
