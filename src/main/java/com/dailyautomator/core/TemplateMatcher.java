package com.dailyautomator.core;

import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.javacpp.DoublePointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenCV-based template matcher.
 * Uses Imgproc.matchTemplate() with TM_CCOEFF_NORMED (default) to locate
 * UI elements within a screenshot.
 */
public class TemplateMatcher {

    private final ScreenCapture screenCapture;
    private final int matchMethod;

    /** Result of a single template match operation. */
    public static class MatchResult {
        /** Top-left corner of the matched region. */
        public final java.awt.Point location;
        /** Confidence score (0.0–1.0, higher = better). */
        public final double confidence;
        /** Whether a match above the threshold was found. */
        public final boolean found;

        public MatchResult(java.awt.Point location, double confidence, boolean found) {
            this.location = location;
            this.confidence = confidence;
            this.found = found;
        }

        @Override
        public String toString() {
            return String.format("MatchResult{found=%s, loc=(%d,%d), confidence=%.4f",
                found, location.x, location.y, confidence);
        }
    }

    /** Default constructor — uses TM_CCOEFF_NORMED. */
    public TemplateMatcher(ScreenCapture screenCapture) {
        this(screenCapture, opencv_imgproc.TM_CCOEFF_NORMED);
    }

    /**
     * Constructor with explicit match method.
     * @param matchMethod OpenCV match method constant (e.g., TM_CCOEFF_NORMED, TM_CCORR_NORMED).
     */
    public TemplateMatcher(ScreenCapture screenCapture, int matchMethod) {
        this.screenCapture = screenCapture;
        this.matchMethod = matchMethod;
    }

    /**
     * Match a template within a source image.
     * @param source    The screenshot / large image.
     * @param template  The UI element image to find.
     * @param threshold Minimum confidence (0.0–1.0) to consider a match.
     * @return The best match result.
     */
    public MatchResult match(BufferedImage source, BufferedImage template, double threshold) {
        List<MatchResult> results = matchAll(source, template, threshold);
        return results.isEmpty()
            ? new MatchResult(new java.awt.Point(0, 0), 0.0, false)
            : results.get(0);
    }

    /**
     * Match a template against a full-screen capture.
     * @param template  The UI element image to find.
     * @param threshold Minimum confidence (0.0–1.0).
     */
    public MatchResult matchOnScreen(BufferedImage template, double threshold) {
        BufferedImage fullScreen = screenCapture.captureFullScreen();
        return match(fullScreen, template, threshold);
    }

    /**
     * Match a template against a region of the screen.
     * @param template  The UI element image to find.
     * @param threshold Minimum confidence (0.0–1.0).
     */
    public MatchResult matchOnRegion(BufferedImage template, double threshold,
                                     int x, int y, int width, int height) {
        BufferedImage region = screenCapture.captureRegion(x, y, width, height);
        return match(region, template, threshold);
    }

    /**
     * Find all matches above the threshold (multi-match).
     * Uses a simple local-maximum approach on the result matrix.
     */
    public List<MatchResult> matchAll(BufferedImage source, BufferedImage template, double threshold) {
        // Guard: template must be smaller than or equal to source
        if (template.getWidth() > source.getWidth() || template.getHeight() > source.getHeight()) {
            return List.of();
        }

        Mat sourceMat = Java2DFrameUtils.toMat(source);
        Mat templateMat = Java2DFrameUtils.toMat(template);
        Mat resultMat = new Mat();

        opencv_imgproc.matchTemplate(sourceMat, templateMat, resultMat, matchMethod);

        DoublePointer minVal = new DoublePointer(1);
        DoublePointer maxVal = new DoublePointer(1);
        Point minLoc = new Point();
        Point maxLoc = new Point();
        opencv_core.minMaxLoc(resultMat, minVal, maxVal, minLoc, maxLoc, null);

        double max = maxVal.get(0);
        double min = minVal.get(0);
        boolean isSqdiff = (matchMethod == opencv_imgproc.TM_SQDIFF ||
                            matchMethod == opencv_imgproc.TM_SQDIFF_NORMED);

        // For squared-difference methods, convert so higher = better
        double bestVal = isSqdiff ? (1.0 - min) : max;
        // For SQDIFF the best location is minLoc; for others it's maxLoc
        Point bestLoc = isSqdiff ? minLoc : maxLoc;

        if (bestVal >= threshold) {
            List<MatchResult> results = new ArrayList<>();
            results.add(new MatchResult(
                new java.awt.Point(bestLoc.x(), bestLoc.y()),
                bestVal,
                true
            ));
            return results;
        }

        return List.of();
    }

    /**
     * Compute the center point of a match result given the template size.
     */
    public static java.awt.Point centerOf(MatchResult result, BufferedImage template) {
        return new java.awt.Point(
            result.location.x + template.getWidth() / 2,
            result.location.y + template.getHeight() / 2
        );
    }

    /** Load a template image from disk. */
    public static BufferedImage loadTemplate(String filePath) {
        try {
            return ImageIO.read(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template image: " + filePath, e);
        }
    }
}
