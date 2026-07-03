package com.dailyautomator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledIfSystemProperty(named = "dailyautomator.enableRobotTests", matches = "true")
class ScreenCaptureTest {

    private ScreenCapture screenCapture;

    @BeforeEach
    void setUp() throws AWTException {
        screenCapture = new ScreenCapture(new Robot());
    }

    @Test
    void captureFullScreen_returnsNonNullImage() {
        BufferedImage img = screenCapture.captureFullScreen();
        assertNotNull(img, "Full-screen capture should not be null");
        assertTrue(img.getWidth() > 0, "Width should be positive");
        assertTrue(img.getHeight() > 0, "Height should be positive");
    }

    @Test
    void captureRegion_returnsCorrectDimensions() {
        BufferedImage img = screenCapture.captureRegion(10, 10, 200, 150);
        assertEquals(200, img.getWidth(), "Region width should match");
        assertEquals(150, img.getHeight(), "Region height should match");
    }

    @Test
    void captureRegion_invalidSize_returnsAvailableArea() {
        BufferedImage img = screenCapture.captureRegion(-100, -100, 50, 50);
        assertNotNull(img);
    }

    @Test
    void captureCurrentMonitor_returnsNonNullImage() {
        BufferedImage img = screenCapture.captureCurrentMonitor();
        assertNotNull(img);
        assertTrue(img.getWidth() > 0);
    }

    @Test
    void captureWindow_returnsNonNullImage() {
        Rectangle rect = new Rectangle(0, 0, 100, 100);
        BufferedImage img = screenCapture.captureWindow(rect);
        assertNotNull(img);
        assertEquals(100, img.getWidth());
    }

    @Test
    void captureWindowByHwnd_returnsCorrectClientArea() {
        long hwnd = WindowFinder.getForegroundHwnd();
        assumeTrue(hwnd != 0, "No foreground window available");

        BufferedImage img = screenCapture.captureWindow(hwnd);
        assertNotNull(img, "Captured image should not be null");
        assertTrue(img.getWidth() > 0, "Width should be positive");
        assertTrue(img.getHeight() > 0, "Height should be positive");
    }

    @Test
    void saveToFile_createsFile(@TempDir File tempDir) throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        String path = new File(tempDir, "test_save.png").getAbsolutePath();
        String result = screenCapture.saveToFile(img, path);
        assertEquals(new File(path).getAbsolutePath(), result);
        assertTrue(new File(path).exists(), "File should exist after save");
    }

    @Test
    void saveToFile_defaultPath_returnsAbsolutePath() throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        String result = screenCapture.saveToFile(img);
        assertNotNull(result);
        assertTrue(result.endsWith(".png"));
        // Clean up
        new File(result).delete();
    }

    @Test
    void saveToFile_withFormat_usesSpecifiedFormat(@TempDir File tempDir) throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        String path = new File(tempDir, "test.jpg").getAbsolutePath();
        String result = screenCapture.saveToFile(img, path, "jpg");
        assertTrue(new File(result).exists());
        BufferedImage loaded = ImageIO.read(new File(result));
        assertNotNull(loaded);
    }
}
