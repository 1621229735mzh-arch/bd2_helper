package com.dailyautomator;

import com.dailyautomator.core.*;
import com.dailyautomator.gui.MainApp;
import javafx.application.Application;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Scanner;

/**
 * DailyAutomator 鈥?main entry point.
 *
 * Usage:
 *   java -jar daily-automator.jar        鈫?launch GUI
 *   java -jar daily-automator.jar --cli   鈫?launch CLI interactive demo
 *   mvn javafx:run                        鈫?launch GUI via Maven plugin
 */
public class DailyAutomator {

    public static void main(String[] args) {
        if (args.length > 0 && "--cli".equals(args[0])) {
            new DailyAutomator().runDemo();
        } else {
            Application.launch(MainApp.class, args);
        }
    }

    /**
     * CLI interactive demo that exercises the core modules.
     */
    public void runDemo() {
        System.out.println("==========================================");
        System.out.println("  DailyAutomator v1.0.0  (CLI)");
        System.out.println("==========================================");

        try {
            Robot robot = new Robot();
            ScreenCapture sc = new ScreenCapture(robot);
            MouseController mc = new MouseController(robot, MouseController.Mode.FOREGROUND);

            Scanner scanner = new Scanner(System.in);
            boolean exit = false;

            while (!exit) {
                System.out.println("\n--- Main Menu ---");
                System.out.println("1. Take screenshot (full screen)");
                System.out.println("2. Search window by title");
                System.out.println("3. Show mouse position");
                System.out.println("4. Send keyboard input");
                System.out.println("5. Run template match demo");
                System.out.println("0. Exit");
                System.out.print("Choice: ");

                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1" -> {
                        BufferedImage img = sc.captureFullScreen();
                        String path = sc.saveToFile(img);
                        System.out.println("Screenshot saved: " + path);
                        System.out.println("Dimensions: " + img.getWidth() + "x" + img.getHeight());
                    }
                    case "2" -> {
                        System.out.print("Enter window title keyword: ");
                        String kw = scanner.nextLine().trim();
                        long hwnd = WindowFinder.findByTitle(kw);
                        if (hwnd != 0) {
                            Rectangle rect = WindowFinder.getWindowRect(hwnd);
                            System.out.println("Found: HWND=" + hwnd);
                            System.out.println("  Title: " + WindowFinder.getWindowTitle(hwnd));
                            System.out.println("  Class: " + WindowFinder.getClassName(hwnd));
                            System.out.println("  Bounds: " + rect);
                        } else {
                            System.out.println("No window found.");
                        }
                    }
                    case "3" -> {
                        Point p = mc.getPosition();
                        System.out.println("Mouse in FOREGROUND mode");
                        System.out.println("Current mouse position: (" + p.x + ", " + p.y + ")");
                    }
                    case "4" -> {
                        try {
                            KeyboardController kc = new KeyboardController(robot);
                            System.out.println("Keyboard in FOREGROUND mode");
                            System.out.print("Enter text to type: ");
                            String text = scanner.nextLine();
                            kc.type(text);
                            System.out.println("Typed: " + text);
                        } catch (IllegalArgumentException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }
                    case "5" -> {
                        System.out.println("TemplateMatch demo: capturing screen and matching...");
                        BufferedImage full = sc.captureFullScreen();
                        // Use a small region of the screen as a self-template
                        BufferedImage template = full.getSubimage(0, 0,
                            Math.min(full.getWidth(), 200), Math.min(full.getHeight(), 200));
                        TemplateMatcher matcher = new TemplateMatcher(sc);
                        TemplateMatcher.MatchResult result = matcher.match(full, template, 0.85);
                        System.out.println("Match result: " + result);
                    }
                    case "0" -> {
                        exit = true;
                        System.out.println("Goodbye.");
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }
        } catch (AWTException e) {
            System.err.println("Failed to initialize Robot: " + e.getMessage());
        }
    }
}


