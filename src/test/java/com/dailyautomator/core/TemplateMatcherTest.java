package com.dailyautomator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "dailyautomator.enableRobotTests", matches = "true")
class TemplateMatcherTest {

    private TemplateMatcher matcher;
    private ScreenCapture screenCapture;
    private BufferedImage sourceImage;
    private BufferedImage templateImage;

    @BeforeEach
    void setUp() throws AWTException {
        screenCapture = new ScreenCapture(new Robot());
        matcher = new TemplateMatcher(screenCapture);

        // Source: 200x200 white background with a black cross pattern at (50,50)
        sourceImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sourceImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.BLACK);
        g.fillRect(50, 30, 20, 80);  // vertical bar
        g.fillRect(30, 50, 80, 20);  // horizontal bar
        g.dispose();

        // Template: the same cross pattern, small crop
        templateImage = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D gt = templateImage.createGraphics();
        gt.setColor(Color.WHITE);
        gt.fillRect(0, 0, 20, 20);
        gt.setColor(Color.BLACK);
        gt.fillRect(8, 0, 4, 20);   // vertical
        gt.fillRect(0, 8, 20, 4);   // horizontal
        gt.dispose();
    }

    @Test
    void match_exactTemplate_findsLocation() {
        TemplateMatcher.MatchResult result = matcher.match(sourceImage, templateImage, 0.2);
        assertTrue(result.found, "Should find the cross pattern in source");
        assertTrue(result.confidence > 0.2, "Confidence should be above threshold");
    }

    @Test
    void match_highThreshold_noMatch() {
        BufferedImage different = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = different.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 20, 20);
        g.setColor(Color.GRAY);
        // Diagonal gradient -- not present in source
        for (int i = 0; i < 20; i++) {
            different.setRGB(i, i, Color.BLACK.getRGB());
        }
        g.dispose();

        TemplateMatcher.MatchResult result = matcher.match(sourceImage, different, 0.99);
        assertFalse(result.found, "Should not find diagonal pattern in cross image");
    }

    @Test
    void matchAll_emptySource_returnsEmptyList() {
        BufferedImage tiny = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        List<TemplateMatcher.MatchResult> results = matcher.matchAll(tiny, templateImage, 0.99);
        assertTrue(results.isEmpty(), "Should not match template in tiny source");
    }

    @Test
    void centerOf_returnsCorrectCenter() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        TemplateMatcher.MatchResult result = new TemplateMatcher.MatchResult(
            new java.awt.Point(30, 30), 0.95, true);
        java.awt.Point center = TemplateMatcher.centerOf(result, img);
        assertEquals(55, center.x, "Center X should be 30 + 25");
        assertEquals(55, center.y, "Center Y should be 30 + 25");
    }

    @Test
    void loadTemplate_validFile_returnsImage(@TempDir File tempDir) throws IOException {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        File imgFile = new File(tempDir, "template.png");
        ImageIO.write(img, "png", imgFile);
        BufferedImage loaded = TemplateMatcher.loadTemplate(imgFile.getAbsolutePath());
        assertNotNull(loaded);
        assertEquals(20, loaded.getWidth());
        assertEquals(20, loaded.getHeight());
    }

    @Test
    void loadTemplate_invalidFile_throwsException() {
        assertThrows(RuntimeException.class,
            () -> TemplateMatcher.loadTemplate("/nonexistent/path/image.png"));
    }

    @Test
    void matchResult_toString_containsInfo() {
        TemplateMatcher.MatchResult r = new TemplateMatcher.MatchResult(
            new java.awt.Point(10, 20), 0.85, true);
        String s = r.toString();
        assertTrue(s.contains("MatchResult"));
        assertTrue(s.contains("found=true"));
        assertTrue(s.contains("confidence"));
    }
}
