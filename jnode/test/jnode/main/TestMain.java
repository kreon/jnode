package jnode.main;

import jnode.orm.ORMManager;
import org.junit.Test;

import java.io.File;
import java.net.URL;


public class TestMain {

    @Test()
    public void testNormalStart() throws Exception {
        Main testNode = new Main();
        URL url = TestMain.class.getResource("example.conf.ru");
        File file = new File(url.getPath());

        testNode.readConfig(file.getAbsolutePath());
        ORMManager.INSTANSE.start();
        assert true;
    }


}