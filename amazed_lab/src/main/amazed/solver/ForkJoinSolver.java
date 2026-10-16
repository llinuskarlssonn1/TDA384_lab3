package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver
    extends SequentialSolver
{
    private static volatile boolean foundGoal = false;
    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter)
    {
        this(maze);
        this.forkAfter = forkAfter;
    }


    public ForkJoinSolver(Maze maze, int forkAfter, int startPos, Set<Integer> visited, Map<Integer, Integer> predecessor) {
        this(maze);
        this.forkAfter = forkAfter;
        this.start = startPos;
        this.visited = visited;
        this.predecessor = predecessor;
    }

    @Override
    public void initStructures() {
        visited = new ConcurrentSkipListSet<>();
        predecessor = new ConcurrentHashMap<>();
        frontier = new Stack<>();
    }

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute()
    {
        return parallelSearch();
    }

    private List<Integer> parallelSearch() {
       
        int steps = 0;
        List<ForkJoinSolver> subtasks = new ArrayList<>();
        int player = maze.newPlayer(start);
        frontier.push(start);
        
        while(!frontier.empty() && !foundGoal) {
            
            while(frontier.size() > 1) {
                // if(forkAfter > 0 && steps >= forkAfter) {
                //     int next = frontier.pop();
                //     ForkJoinSolver newSolver = new ForkJoinSolver(maze, forkAfter, next, visited, predecessor);
                //     newSolver.fork();         
                //     subtasks.add(newSolver); 
                //     steps = 0;
                // } else {
                //     break;
                // }
                int next = frontier.pop();
                ForkJoinSolver newSolver = new ForkJoinSolver(maze, forkAfter, next, visited, predecessor);
                newSolver.fork();         
                subtasks.add(newSolver); 
            }
            
            // get the new node to process
            int current = frontier.pop();
            // if current node has a goal
            if (maze.hasGoal(current)) {
                foundGoal = true;
                // move player to goal
                maze.move(player, current);
                // for (int i = 0; i < subtasks.size(); i++) {
                //     List<Integer> result = subtasks.get(i).join();
                // }        
                // search finished: reconstruct and return path
                return pathFromTo(maze.start(), current);
            }
            // if current node has not been visited yet
            if (visited.add(current)) {
                // move player to current node
                maze.move(player, current);
                // mark node as visited
                // for every node nb adjacent to current
                for (int nb: maze.neighbors(current)) {
                    // add nb to the nodes to be processed
                    if (!visited.contains(nb)){
                        frontier.push(nb);
                    }
                    // if nb has not been already visited,
                    // nb can be reached from current (i.e., current is nb's predecessor)
                    if (!visited.contains(nb))
                        predecessor.putIfAbsent(nb, current);
                }
            } 
            // steps++;
        }
        for (int i = 0; i < subtasks.size(); i++) {
            List<Integer> result = subtasks.get(i).join();
            if (result != null) {
                return result;
            }
        }        
        return null;
    }
}
