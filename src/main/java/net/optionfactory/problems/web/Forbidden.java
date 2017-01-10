package net.optionfactory.problems.web;

import net.optionfactory.problems.Failure;
import net.optionfactory.problems.Problem;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class Forbidden extends Failure {

    public Forbidden(List<Problem> problems, Throwable cause) {
        super(problems, cause);
    }

    public Forbidden(List<Problem> problems, String reason) {
        super(problems, reason);
    }

    public Forbidden(Problem problem, Throwable cause) {
        super(problem, cause);
    }

    public Forbidden(Problem problem, String reason) {
        super(problem, reason);
    }

    public Forbidden(List<Problem> problems) {
        super(problems);
    }

    public Forbidden(Problem problem) {
        super(problem);
    }

}
