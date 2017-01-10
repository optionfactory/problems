package net.optionfactory.problems.web;

import net.optionfactory.problems.Failure;
import net.optionfactory.problems.Problem;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequest extends Failure {

    public BadRequest(List<Problem> problems, Throwable cause) {
        super(problems, cause);
    }

    public BadRequest(List<Problem> problems, String reason) {
        super(problems, reason);
    }

    public BadRequest(Problem problem, Throwable cause) {
        super(problem, cause);
    }

    public BadRequest(Problem problem, String reason) {
        super(problem, reason);
    }

    public BadRequest(List<Problem> problems) {
        super(problems);
    }

    public BadRequest(Problem problem) {
        super(problem);
    }

}
