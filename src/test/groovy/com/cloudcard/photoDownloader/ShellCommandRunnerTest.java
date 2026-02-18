package com.cloudcard.photoDownloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ShellCommandRunnerTest {

    public static final String DUMMY_COMMAND = "./dummy-script.sh";
    ShellCommandRunner shellCommandRunner;

    @BeforeEach
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