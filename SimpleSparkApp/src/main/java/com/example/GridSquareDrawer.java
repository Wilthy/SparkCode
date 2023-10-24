package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class GridSquareDrawer {
    private JFrame frame;
    private JPanel gridPanel;
    public int width, height, rowLength, columnLength, cellSize;
    private ArrayList<Shape> shapes;
    private int currentRow = -1;
    private int currentColumn = -1;
    private int heldKey = -1;

    public GridSquareDrawer(int width, int height, int rowLength, int columnLength) {
        this.width = width;
        this.height = height;
        this.rowLength = rowLength;
        this.columnLength = columnLength;
        this.cellSize = Math.min(width / rowLength, height / columnLength);
        this.shapes = new ArrayList<Shape>();

        initializeWindow();
    }

    private void initializeWindow() {
        frame = new JFrame("Grid Shape Drawer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);

        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGrid(g);
                drawShapes(g);
            }
        };

        gridPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentRow = e.getY() / cellSize;
                currentColumn = e.getX() / cellSize;
            }
        });

        frame.add(gridPanel);
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                heldKey = e.getKeyCode();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                heldKey = -1;
            }
        });

        frame.setVisible(true);
    }

    private void drawGrid(Graphics g) {
        g.setColor(Color.BLACK);
        for (int x = 0; x <= width; x += cellSize) {
            g.drawLine(x, 0, x, height);
        }
        for (int y = 0; y <= height; y += cellSize) {
            g.drawLine(0, y, width, y);
        }
    }

    private void drawShapes(Graphics g) {
        for (Shape shape : shapes) {
            shape.draw(g, cellSize);
        }
    }

    public void drawSquare(int row, int column, Color color, Color borderColor) {
        Square square = new Square(row, column, color, borderColor);
        shapes.add(square);
        gridPanel.repaint();
    }

    public void drawCircle(int row, int column, Color color) {
        Circle circle = new Circle(row, column, color);
        shapes.add(circle);
        gridPanel.repaint();
    }

    public void clearScreen() {
        shapes.clear();
        gridPanel.repaint();
    }

    private interface Shape {
        void draw(Graphics g, int cellSize);
    }

    private class Square implements Shape {
        private int row, column;
        private Color color, borderColor;

        public Square(int row, int column, Color color, Color borderColor) {
            this.row = row;
            this.column = column;
            this.color = color;
            this.borderColor = borderColor;
        }

        @Override
        public void draw(Graphics g, int cellSize) {
            g.setColor(borderColor);
            g.fillRect(column * cellSize + 2, row * cellSize + 2, cellSize - 4, cellSize - 4);
            g.setColor(color);
            g.fillRect(column * cellSize + 10, row * cellSize + 10, cellSize - 20, cellSize - 20);
        }
    }

    private class Circle implements Shape {
        private int row, column;
        private Color color;

        public Circle(int row, int column, Color color) {
            this.row = row;
            this.column = column;
            this.color = color;
        }

        @Override
        public void draw(Graphics g, int cellSize) {
            g.setColor(color);
            g.fillOval(column * cellSize + 2, row * cellSize + 2, cellSize - 4, cellSize - 4);
        }
    }

    public int currentRowClicked() {
        return currentRow;
    }

    public int currentColumnClicked() {
        return currentColumn;
    }

    public String currentKeyPressed() {
        if (heldKey != -1) {
            return KeyEvent.getKeyText(heldKey);
        } else {
            return "No key held";
        }
    }
}
