package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ShellCommandRunnerTest {

    public static final String DUMMY_COMMAND = "/Users/terskine/git/online-photo-submission/cloudcard-photo-downloader/dummy-script.sh";
    ShellCommandRunner shellCommandRunner;

    @Before
    public void before() {

        shellCommandRunner = new ShellCommandRunner();
    }

    @Test
    public void testPositivePath() {

        boolean result = shellCommandRunner.run(DUMMY_COMMAND);
        assertThat(result).isTrue();
    }

    @Test
    public void testNegativePath() {

        boolean result = shellCommandRunner.run("non-existent-script.sh");
        assertThat(result).isFalse();
    }
}