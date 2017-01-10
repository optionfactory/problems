package net.optionfactory.problems.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.optionfactory.problems.Problem;
import java.util.List;
import java.util.function.Supplier;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

/**
 *
 * @author rferranti
 */
public class JsonExceptionResolverTest {

    @Test
    public void exceptionsAreResolvedWithMappingJackson2JsonView() {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonExceptionResolver er = new JsonExceptionResolver(mapper, JsonExceptionResolver.LOWEST_PRECEDENCE + 1);
        Logger.getLogger(JsonExceptionResolver.class).setLevel(Level.OFF);

        final MockHttpServletRequest req = new MockHttpServletRequest();
        final MockHttpServletResponse res = new MockHttpServletResponse();
        final Exception exception = new IllegalArgumentException();

        final ModelAndView got = silently(JsonExceptionResolver.class, () -> {
            return er.resolveException(req, res, null, exception);
        });

        Assert.assertTrue(got.getView() instanceof MappingJackson2JsonView);
    }

    @Test
    public void exceptionsAreReportedAsProblemsInModel() {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonExceptionResolver er = new JsonExceptionResolver(mapper, JsonExceptionResolver.LOWEST_PRECEDENCE + 1);
        Logger.getLogger(JsonExceptionResolver.class).setLevel(Level.OFF);

        final MockHttpServletRequest req = new MockHttpServletRequest();
        final MockHttpServletResponse res = new MockHttpServletResponse();
        final Exception exception = new IllegalArgumentException();

        final ModelAndView got = silently(JsonExceptionResolver.class, () -> {
            return er.resolveException(req, res, null, exception);
        });
        Object failures = got.getModel().get("errors");
        Assert.assertTrue(failures instanceof List && ((List) failures).get(0) instanceof Problem);
    }

    public static final <R> R silently(Class<?> k, Supplier<R> fn) {
        final Level oldLevel = Logger.getLogger(k).getLevel();
        Logger.getLogger(k).setLevel(Level.OFF);
        try {
            return fn.get();
        } finally {
            Logger.getLogger(k).setLevel(oldLevel);
        }
    }
}
