package nars.web;

import jcog.Texts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.tooling.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

public class ClientBuilder {

    public static void main(String[] args) {
        rebuild(WebClientJS.class, false);
    }

    /** TODO target directory params, cache directory = system determined temp directory */
    public static synchronized void rebuild(Class entryClass, boolean clean) {
        try {

            long start = System.nanoTime();

            TeaVMTool tea = new TeaVMTool();

            if (clean) {
                try {
                    org.apache.commons.io.FileUtils.deleteDirectory(new File("/tmp/teacache"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            try {
                org.apache.commons.io.FileUtils.deleteDirectory(new File("/tmp/tea"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            tea.setMainClass(entryClass.getName());
            tea.setCacheDirectory(new File("/tmp/teacache"));
            tea.setIncremental(true);

            //tea.setDebugInformationGenerated(true);

            tea.setTargetDirectory(new File("/tmp/tea"));
            tea.setLog(LOG);


            tea.setRuntime(RuntimeCopyOperation.SEPARATE);

            tea.setTargetType(TeaVMTargetType.JAVASCRIPT);

            tea.generate();

            long end = System.nanoTime();

            logger.info("generated {} in {}", tea.getGeneratedFiles(), Texts.timeStr(end-start));

        } catch (TeaVMToolException e) {
            e.printStackTrace();
        }
    }

    static final Logger logger = LoggerFactory.getLogger(ClientBuilder.class);

    static final TeaVMToolLog LOG = new TeaVMToolLog() {
        @Override
        public void info(String text) {
            logger.info(text);
        }

        @Override
        public void debug(String text) {
            logger.debug(text);
        }

        @Override
        public void warning(String text) {
            logger.warn(text);
        }

        @Override
        public void error(String text) {
            logger.error(text);
        }

        @Override
        public void info(String text, Throwable e) {
            if (logger.isInfoEnabled())
                logger.info(text + " {}", e);
        }

        @Override
        public void debug(String text, Throwable e) {
            if (logger.isDebugEnabled())
                logger.debug(text + " {}", e);
        }

        @Override
        public void warning(String text, Throwable e) {
            logger.warn(text + " {}", e);
        }

        @Override
        public void error(String text, Throwable e) {
            logger.error(text + " {}", e);
        }
    };

    public static void rebuildAsync(Class<WebClientJS> webClientJSClass, boolean clean) {
        ForkJoinPool.commonPool().execute(()->{
            rebuild(webClientJSClass, clean);
        });
    }
}
