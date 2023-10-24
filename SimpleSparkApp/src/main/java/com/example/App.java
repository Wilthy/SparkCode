package com.example;

import static spark.Spark.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.awt.*;

public class App
{
    private static final ConcurrentHashMap<String, UserSessionThread> nameToThreadMap = new ConcurrentHashMap<String, UserSessionThread>();
    private static final ConcurrentHashMap<String, Bullet> bullets = new ConcurrentHashMap<>();
    private static final GridSquareDrawer drawer = new GridSquareDrawer(1000, 1000, 10, 10);
    private static final AtomicInteger connected = new AtomicInteger(0);

    public static void main(String[] args)
    {
        port(4567);
        staticFiles.location("/public");

        after((request, response) -> {
            response.header("Cache-Control", "no-cache, no-store, must-revalidate");
            response.header("Pragma", "no-cache");
            response.header("Expires", "0");
        });

        post("/create-session", (req, res) ->
        {
            String name = req.queryParams("name");
            String data = req.body();

            if (nameToThreadMap.containsKey(name))
            {
                UserSessionThread userThread = nameToThreadMap.get(name);
                if (userThread != null)
                    return userThread.codeSubmitted(data);
                else
                    return "Client thread not found for " + name;
            }
            else
            {
                System.out.println("Client " + name + " has joined.");
                UserSessionThread userThread = new UserSessionThread(name, data);
                nameToThreadMap.put(name, userThread);
                new Thread(userThread).start();
                return "Client thread started for " + name;
            }
        });

        post("/delete-session", (req, res) ->
        {
            String name = req.queryParams("name");
            UserSessionThread userThread = nameToThreadMap.get(name);
            if (userThread != null)
            {
                nameToThreadMap.get(name).disconnect();
                nameToThreadMap.remove(name);
                System.out.println("User session deleted for " + name);
                return "User session deleted for " + name;
            }
            else
            {
                return "User session not found for " + name;
            }
        });

        while (true) {
            drawer.clearScreen();
            for (UserSessionThread thread: nameToThreadMap.values()) {
                drawer.drawSquare(thread.row, thread.column, Color.RED, Color.BLUE);
            }
            for (Bullet bullet: bullets.values()) {
                bullet.move();
                drawer.drawCircle(bullet.r, bullet.c, Color.RED);
            }
            try { Thread.sleep(1000); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    static class Bullet {
        public String direction;
        public int r, c;
        Bullet(int rr, int cc, String d) {
            direction = d;
            r = rr;
            c = cc;
        }
        public void move() {
            if (direction.equals("Right")) ++c;
            else if (direction.equals("Left")) --c;
            else if (direction.equals("Up")) --r;
            else if (direction.equals("Down")) ++r;
        }
    }

    static class UserSessionThread implements Runnable {
        private AtomicReference<Process> playerProcess;
        private AtomicReference<BufferedReader> playerReader;
        private AtomicReference<BufferedWriter> playerWriter;
        private AtomicBoolean signal;
        private String name;
        public int column;
        public int row;

        UserSessionThread(String n, String c)
        {
            row = connected.get();
            connected.set(connected.get() + 1);
            column = 0;

            name = n;
            signal = new AtomicBoolean(false);
            playerProcess = new AtomicReference<Process>();
            playerReader = new AtomicReference<BufferedReader>();
            playerWriter = new AtomicReference<BufferedWriter>();
            codeSubmitted(c);
        }

        public void disconnect() {
            try {
                signal.set(true);
                while (signal.get() == true);
                playerProcess.get().destroy();
                playerReader.get().close();
                playerWriter.get().close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        public String codeSubmitted(String c)
        {
            try
            {
                String fileName = name.replaceAll("[^a-zA-Z]+", "");
                String code =
                "import java.util.Scanner;\n" +
                "public class " + fileName + " {\n" +
                "   public static final Scanner reader = new Scanner(System.in);\n" +
                "   public static final int sleepTime = 1000;\n" +
                "   public static void main(String[] args) {\n" +
                        c +
                "   }\n" +
                "   public static void move(String location) {\n" +
                "       System.out.println(\"move\" + location);\n" +
                "       try { Thread.sleep(sleepTime); } catch (Exception e) { }\n" +
                "   }\n" +
                "   public static void fire(String location) {\n" +
                "       System.out.println(\"fire\" + location);\n" +
                "       try { Thread.sleep(sleepTime); } catch (Exception e) { }\n" +
                "   }\n" +
                "   public static String get(String location) {\n" +
                "       System.out.println(\"get\" + location);\n" +
                "       return reader.nextLine();\n" +
                "   }\n" +
                "}";

                String javaFileName = fileName + ".java";
                String classFileName = fileName + ".class";
        
                File javaFile = new File(javaFileName);
                File classFile = new File(classFileName);

                if (javaFile.exists() && javaFile.delete());
                if (classFile.exists() && classFile.delete());

                java.io.FileWriter fileWriter = new java.io.FileWriter(fileName + ".java");
                fileWriter.write(code);
                fileWriter.close();

                Process compilationProcess = Runtime.getRuntime().exec("javac " + fileName + ".java");
                InputStream errorStream = compilationProcess.getErrorStream();
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                StringBuilder errorMessages = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null)
                    errorMessages.append(errorLine).append("\n");

                int compilationResult = compilationProcess.waitFor();
                if (compilationResult == 0)
                {
                    if (playerProcess.get() != null) {
                        playerProcess.get().destroy();
                        playerProcess.get().waitFor();
                    }
                    if (playerReader.get() != null)
                        playerReader.get().close();
                    if (playerWriter.get() != null)
                        playerWriter.get().close();
                    
                    Process current = Runtime.getRuntime().exec("java " + fileName);
                    playerProcess.set(current);
                    playerReader.set(new BufferedReader(new InputStreamReader(current.getInputStream())));
                    playerWriter.set(new BufferedWriter(new OutputStreamWriter(current.getOutputStream())));
                    
                    return "Compilation Successful: Running";
                }
                else {
                    return errorMessages.toString();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        public void sendData(String data) {
            try {
                if (playerWriter.get() != null) {
                    playerWriter.get().write(data);
                    playerWriter.get().newLine();
                    playerWriter.get().flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true)
            {
                try
                {
                    if (signal.get() == true) {
                        signal.set(false);
                        return;
                    }
                    for (String bulletName: bullets.keySet()) {
                        Bullet bullet = bullets.get(bulletName);
                        if (bullet.r == row && bullet.c == column && !bulletName.equals(name)) {
                            bullets.remove(bulletName);
                            nameToThreadMap.remove(name);
                            disconnect();
                        }
                    }
                    if (playerReader.get() != null) {
                        String nextLine = playerReader.get().readLine();
                        if (nextLine != null) {
                            switch (nextLine) {
                                case "moveRight":
                                    if (column + 1 >= drawer.columnLength) break;
                                    column += 1;
                                    break;
                                case "moveLeft":
                                    if (column - 1 < 0) break;
                                    column -= 1;
                                    break;
                                case "moveUp":
                                    if (row - 1 < 0) break;
                                    row -= 1;
                                    break;
                                case "moveDown":
                                    if (row + 1 >= drawer.rowLength) break;
                                    row += 1;
                                    break;
                                case "getRight":
                                    if (column + 1 >= drawer.columnLength)
                                        sendData("Border");
                                    else
                                        sendData("Nothing");
                                    break;
                                case "getLeft":
                                    if (column - 1 < 0)
                                        sendData("Border");
                                    else
                                        sendData("Nothing");
                                    break;
                                case "getUp":
                                    if (row - 1 < 0)
                                        sendData("Border");
                                    else
                                        sendData("Nothing");
                                    break;
                                case "getDown":
                                    if (row + 1 >= drawer.rowLength)
                                        sendData("Border");
                                    else
                                        sendData("Nothing");
                                    break;
                                case "fireRight":
                                    bullets.put(name, new Bullet(row, column + 1, "Right"));
                                    break;
                                case "fireLeft":
                                    bullets.put(name, new Bullet(row, column - 1, "Left"));
                                    break;
                                case "fireUp":
                                    bullets.put(name, new Bullet(row - 1, column, "Up"));
                                    break;
                                case "fireDown":
                                    bullets.put(name, new Bullet(row + 1, column, "Down"));
                                    break;
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                }
            }
        }
    }
}