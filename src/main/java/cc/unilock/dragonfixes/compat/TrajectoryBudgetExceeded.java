package cc.unilock.dragonfixes.compat;

/** Internal control-flow signal for an exhausted trajectory search only. */
public final class TrajectoryBudgetExceeded extends RuntimeException {
    public TrajectoryBudgetExceeded() { super(null,null,false,false); }
}
