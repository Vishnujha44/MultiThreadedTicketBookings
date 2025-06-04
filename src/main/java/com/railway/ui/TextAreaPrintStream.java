package com.railway.ui;

import javax.swing.*;
import java.io.PrintStream;

public class TextAreaPrintStream extends PrintStream {
    private final JTextArea textArea;

    public TextAreaPrintStream(JTextArea textArea) {
        super(System.out);
        this.textArea = textArea;
    }

    @Override
    public void println(String x) {
        textArea.append(x + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}

