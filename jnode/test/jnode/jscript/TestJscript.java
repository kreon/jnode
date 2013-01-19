package jnode.jscript;

import org.junit.Before;
import org.junit.Test;

import javax.script.*;

/**
 * Тесты для Jscript
 * @author Manjago (kirill@temnenkov.com)
 */
public class TestJscript {

    private final static IJscriptHelper.Version version = new IJscriptHelper.Version() {
        @Override
        public int getMajor() {
            return 1;
        }

        @Override
        public int getMinor() {
            return 0;
        }
    };
    private ScriptEngine engine;
    private Bindings bindings;
    private TestHelper helper;

    private static void evaluate(ScriptEngine engine, Bindings bindings) throws ScriptException {
        engine.eval("helper.writeFileToEchoarea(\"testecho\",\"testsubj\",\"testfile\")", bindings);
    }

    @Before
    public void init() {
        engine = new ScriptEngineManager()
                .getEngineByName("javascript");
        bindings = new SimpleBindings();
        helper = new TestHelper();
        bindings.put(JscriptExecutor.HELPER, helper);
    }

    @Test
    public void testWriteFileToEchoarea() throws Exception {
        evaluate(engine, bindings);
        check();
    }

    @Test(timeout = 1000L)
    public void testSpeed() throws Exception {
        for (int i = 0; i < 100; ++i) {
            evaluate(engine, bindings);
        }
        check();

    }

    private void check() {
        assert helper.getContent() != null &&
                helper.getContent().equals("testechotestsubjtestfile");
    }

    @Test(expected = ScriptException.class)
    public void testFail() throws Exception {
        engine.eval("паляба", bindings);
        assert false;
    }

    class TestHelper implements IJscriptHelper {

        private String content;

        @Override
        public Version getVersion() {
            return version;
        }

        @Override
        public void writeFileToEchoarea(String echoArea, String subject, String filename) {
            StringBuilder sb = new StringBuilder();
            sb.append(echoArea);
            sb.append(subject);
            sb.append(filename);
            content = sb.toString();
        }

        public String getContent() {
            return content;
        }

    }
}
