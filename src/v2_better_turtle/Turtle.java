package v2_better_turtle;

public class Turtle extends Strategy {
    public Turtle(Bot b) {
        super(b);
        soupPriorities[8] = 0;
    }
}
