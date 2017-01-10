package net.optionfactory.problems.web;

import net.optionfactory.problems.Failure;
import net.optionfactory.problems.Problem;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.GATEWAY_TIMEOUT)
public class CannotConnect extends Failure {

    public CannotConnect(List<Problem> problems, Throwable cause) {
        super(problems, cause);
    }

    public CannotConnect(List<Problem> problems, String reason) {
        super(problems, reason);
    }

    public CannotConnect(Problem problem, Throwable cause) {
        super(problem, cause);
    }

    public CannotConnect(Problem problem, String reason) {
        super(problem, reason);
    }

    public CannotConnect(List<Problem> problems) {
        super(problems);
    }

    public CannotConnect(Problem problem) {
        super(problem);
    }

}
