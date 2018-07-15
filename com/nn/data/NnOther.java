package com.nn.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class NnOther {
    public static void nnBackup(String source, String dest) throws IOException {
        File sour = new File(source);
        if(!sour.exists()) return;
        File des;
        if (dest != null) {
            des = new File(dest);
        } else {
            des = new File("nn_backup");
            if (!des.isDirectory() && des.exists() && !des.delete()) return;
            if(!des.mkdirs()) return;
            des = new File("nn_backup/" + sour.getName());
        }

        if(des.exists() && !des.delete()) return;

        Files.copy(sour.toPath(), des.toPath());
    }

    public static void nnBackup(String source) throws IOException {
        nnBackup(source, null);
    }
}
