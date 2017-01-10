package net.optionfactory.problems.web;

import net.optionfactory.problems.Failure;
import net.optionfactory.problems.Problem;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
public class RemoteFailure extends Failure {

    public RemoteFailure(List<Problem> problems, Throwable cause) {
        super(problems, cause);
    }

    public RemoteFailure(List<Problem> problems, String reason) {
        super(problems, reason);
    }

    public RemoteFailure(Problem problem, Throwable cause) {
        super(problem, cause);
    }

    public RemoteFailure(Problem problem, String reason) {
        super(problem, reason);
    }

    public RemoteFailure(List<Problem> problems) {
        super(problems);
    }

    public RemoteFailure(Problem problem) {
        super(problem);
    }

}
