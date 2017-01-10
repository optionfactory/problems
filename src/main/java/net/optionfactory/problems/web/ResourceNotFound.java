package net.optionfactory.problems.web;

import net.optionfactory.problems.Failure;
import net.optionfactory.problems.Problem;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFound extends Failure {

    public ResourceNotFound(List<Problem> problems, Throwable cause) {
        super(problems, cause);
    }

    public ResourceNotFound(List<Problem> problems, String reason) {
        super(problems, reason);
    }

    public ResourceNotFound(Problem problem, Throwable cause) {
        super(problem, cause);
    }

    public ResourceNotFound(Problem problem, String reason) {
        super(problem, reason);
    }

    public ResourceNotFound(List<Problem> problems) {
        super(problems);
    }

    public ResourceNotFound(Problem problem) {
        super(problem);
    }

}
