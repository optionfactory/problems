package net.optionfactory.problems.web;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import net.optionfactory.problems.Failure;
import net.optionfactory.problems.Problem;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import net.optionfactory.problems.web.ExceptionMapping.ExceptionMappings;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

/**
 * A custom exception resolver resolving Spring and Jackson2 exceptions with a
 * MappingJackson2JsonView. Sample serialized form of the response is:  <code>
 * [
 *   {"type": "", "context": "fieldName", "reason": a field validation error", "details": null},
 *   {"type": "", "context": null, "reason": "a global error", "details": null},
 * ]
 * </code>
 */
public class JsonExceptionResolver extends DefaultHandlerExceptionResolver {

    private final ObjectMapper objectMapper;

    public JsonExceptionResolver(ObjectMapper objectMapper, int order) {
        this.objectMapper = objectMapper;
        this.setOrder(order);
    }

    protected HttpStatusAndFailures toStatusAndErrors(HttpServletRequest request, HttpServletResponse response, HandlerMethod hm, Exception ex) {
        final String requestUri = request.getRequestURI();
        if (ex instanceof HttpMessageNotReadableException) {
            final Throwable cause = ex.getCause();
            if (cause instanceof UnrecognizedPropertyException) {
                final UnrecognizedPropertyException inner = (UnrecognizedPropertyException) cause;
                final Map<String, Object> metadata = new ConcurrentHashMap<>();
                metadata.put("known", inner.getKnownPropertyIds());
                metadata.put("in", inner.getReferringClass().getSimpleName());
                final Problem failure = Problem.of("UNRECOGNIZED_PROPERTY", inner.getPropertyName(), "unrecognized field", metadata);
                logger.debug(String.format("Unrecognized property at %s: %s", requestUri, failure));
                return new HttpStatusAndFailures(HttpStatus.BAD_REQUEST, Arrays.asList(failure));
            }
            if (cause instanceof InvalidFormatException) {
                final InvalidFormatException inner = (InvalidFormatException) cause;
                final String path = inner.getPath().stream().map(p -> p.getFieldName()).collect(Collectors.joining("."));
                final Problem failure = Problem.of("INVALID_FORMAT", path, inner.getMessage(), null);
                logger.debug(String.format("Invalid format at %s: %s", requestUri, failure));
                return new HttpStatusAndFailures(HttpStatus.BAD_REQUEST, Arrays.asList(failure));
            }
            if (cause instanceof JsonMappingException) {
                final JsonMappingException inner = (JsonMappingException) cause;
                final String path = inner.getPath().stream().map(p -> p.getFieldName()).collect(Collectors.joining("."));
                final Problem failure = Problem.of("INVALID_FORMAT", path, inner.getMessage(), null);
                logger.debug(String.format("Json mapping exception at %s: %s", requestUri, failure));
                return new HttpStatusAndFailures(HttpStatus.BAD_REQUEST, Arrays.asList(failure));
            }
            final Problem failure = Problem.of("MESSAGE_NOT_READABLE", null, cause != null ? cause.getMessage() : ex.getMessage(), cause);
            logger.debug(String.format("Unreadable message at %s: %s", requestUri, failure));
            return new HttpStatusAndFailures(HttpStatus.BAD_REQUEST, Arrays.asList(failure));
        }
        if (ex instanceof BindException) {
            final BindException be = (BindException) ex;
            final Stream<Problem> globalFailures = be.getGlobalErrors().stream().map(JsonExceptionResolver::objectErrorToProblem);
            final Stream<Problem> fieldFailures = be.getFieldErrors().stream().map(JsonExceptionResolver::fieldErrorToProblem);
            final List<Problem> failures = Stream.concat(globalFailures, fieldFailures).collect(Collectors.toList());
            logger.debug(String.format("Binding failure at %s: %s", requestUri, failures));
            return new HttpStatusAndFailures(HttpStatus.BAD_REQUEST, failures);
        }
        if (ex instanceof MethodArgumentNotValidException) {
            final MethodArgumentNotValidException manve = (MethodArgumentNotValidException) ex;
            final Stream<Problem> globalFailures = manve.getBindingResult().getGlobalErrors().stream().map(JsonExceptionResolver::objectErrorToProblem);
            final Stream<Problem> fieldFailures = manve.getBindingResult().getFieldErrors().stream().map(JsonExceptionResolver::fieldErrorToProblem);
            final List<Problem> failures = Stream.concat(globalFailures, fieldFailures).collect(Collectors.toList());
            logger.debug(String.format("Invalid method argument at %s: %s", requestUri, failures));
            return new HttpStatusAndFailures(HttpStatus.BAD_REQUEST, failures);
        }
        if (ex instanceof ResponseStatusException) {
            final ResponseStatusException rse = (ResponseStatusException)ex;
            final Problem problem = Problem.of(rse.getStatus().name(), rse.getReason());
            return new HttpStatusAndFailures(rse.getStatus(), Collections.singletonList(problem));
        }
        if (hm != null && hm.hasMethodAnnotation(ExceptionMappings.class)) {
            final ExceptionMappings ems = hm.getMethodAnnotation(ExceptionMappings.class);
            for (ExceptionMapping em : ems.value()) {
                if (em.exception().isAssignableFrom(ex.getClass())) {
                    if (ex instanceof Failure) {
                        final Failure failure = (Failure) ex;
                        logger.debug(String.format("Failure at %s", requestUri), failure);
                        return new HttpStatusAndFailures(em.code(), failure.problems);
                    }
                    final Problem problem = Problem.of(em.type(), em.context(), ex.getMessage(), null);
                    logger.debug(String.format("Failure at %s: %s", requestUri, problem));
                    return new HttpStatusAndFailures(em.code(), Collections.singletonList(problem));
                }

            }
        }
        if (hm != null && hm.hasMethodAnnotation(ExceptionMapping.class)) {
            final ExceptionMapping em = hm.getMethodAnnotation(ExceptionMapping.class);
            if (em.exception().isAssignableFrom(ex.getClass())) {
                if (ex instanceof Failure) {
                    final Failure failure = (Failure) ex;
                    logger.debug(String.format("Failure at %s", requestUri), failure);
                    return new HttpStatusAndFailures(em.code(), failure.problems);
                }
                final Problem problem = Problem.of(em.type(), em.context(), ex.getMessage(), null);
                logger.debug(String.format("Failure at %s: %s", requestUri, problem));
                return new HttpStatusAndFailures(em.code(), Collections.singletonList(problem));
            }
        }
        final ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            if (ex instanceof Failure) {
                final Failure failure = (Failure) ex;
                logger.debug(String.format("Failure at %s", requestUri), failure);
                return new HttpStatusAndFailures(responseStatus.value(), failure.problems);
            }
            final String reason = responseStatus.reason().isEmpty() ? ex.getMessage() : responseStatus.reason();
            final Problem problem = Problem.of("GENERIC_PROBLEM", null, reason, null);
            logger.debug(String.format("Failure at %s: %s", requestUri, problem));
            return new HttpStatusAndFailures(responseStatus.value(), Collections.singletonList(problem));
        }
        if (ex instanceof Failure) {
            final Failure failure = (Failure) ex;
            logger.debug(String.format("Failure at %s", requestUri), failure);
            return new HttpStatusAndFailures(HttpStatus.BAD_REQUEST, failure.problems);
        }
        if (ex instanceof AccessDeniedException) {
            final Problem problem = Problem.of("FORBIDDEN", null, ex.getMessage(), null);
            logger.debug(String.format("Access denied at %s: %s", requestUri, problem));
            return new HttpStatusAndFailures(HttpStatus.FORBIDDEN, Collections.singletonList(problem));
        }
        if (null != super.doResolveException(request, new SendErrorToSetStatusHttpServletResponse(response), hm, ex)) {
            if (request.getAttribute("javax.servlet.error.exception") != null) {
                logger.warn(String.format("got an internal error from spring at %s", requestUri), ex);
            }
            final HttpStatus currentStatus = HttpStatus.valueOf(response.getStatus());
            logger.warn(String.format("got an unexpected error while processing request at %s", requestUri), ex);
            return new HttpStatusAndFailures(currentStatus, Collections.singletonList(Problem.of("INTERNAL_ERROR", null, ex.getMessage(), null)));
        }
        logger.error(String.format("got an unexpected error while processing request at %s", requestUri), ex);
        return new HttpStatusAndFailures(HttpStatus.INTERNAL_SERVER_ERROR, Collections.singletonList(Problem.of("UNEXPECTED_PROBLEM", null, ex.getMessage(), null)));
    }

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        final HandlerMethod hm = (HandlerMethod) handler;
        final MappingJackson2JsonView view = new MappingJackson2JsonView();
        view.setExtractValueFromSingleKeyModel(true);
        view.setObjectMapper(objectMapper);
        view.setContentType("application/json;charset=UTF-8");
        final HttpStatusAndFailures statusAndErrors = toStatusAndErrors(request, response, hm, ex);
        response.setStatus(statusAndErrors.status.value());
        return new ModelAndView(view, "errors", statusAndErrors.failures);
    }

    public static class HttpStatusAndFailures {

        public final HttpStatus status;
        public final List<Problem> failures;

        public HttpStatusAndFailures(HttpStatus status, List<Problem> failures) {
            this.status = status;
            this.failures = failures;
        }

    }

    private static Problem fieldErrorToProblem(FieldError error) {
        return Problem.of("FIELD_ERROR", error.getField(), error.getDefaultMessage(), null);
    }

    private static Problem objectErrorToProblem(ObjectError error) {
        return Problem.of("OBJECT_ERROR", null, error.getDefaultMessage(), null);
    }

    public static class SendErrorToSetStatusHttpServletResponse extends HttpServletResponseWrapper {

        private final HttpServletResponse inner;

        public SendErrorToSetStatusHttpServletResponse(HttpServletResponse inner) {
            super(inner);
            this.inner = inner;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            inner.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            inner.setStatus(sc);
        }
    }
}
