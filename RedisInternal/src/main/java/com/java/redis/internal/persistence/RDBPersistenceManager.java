package com.java.redis.internal.persistence;

import com.java.redis.internal.datastore.DataStore;
import com.java.redis.internal.datastore.ValueEntry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RDBPersistenceManager implements PersistenceHandler{
    private final Path filePath;

    public RDBPersistenceManager(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void load(DataStore store) {
        if(Files.exists(filePath)){
            try(ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(filePath)))) {
                Object obj = ois.readObject();
                if(obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, ?> data = (Map<String,  ?>) obj;
                    store.loadSnapshot((Map<String, ValueEntry>) data);
                    System.out.println("Loaded snapshot from " + filePath);
                } else {
                    System.err.println("Snapshot file has unexpected content: " + obj.getClass());
                }
                System.out.println("Snapshot loaded successfully from: " + filePath);
            } catch (Exception e) {
                System.err.println("Failed to load snapshot from " + filePath + ": " + e.getMessage());
            }
        }else {
            System.out.println("No snapshot file found at startup: " + filePath);
        }
    }

    @Override
    public void appendCommand(String cmd) {

    }

    @Override
    public void saveSnapshot(DataStore store) {
        Path tmpPath = filePath.resolveSibling(filePath.getFileName() + ".tmp");
        try( ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpPath)))) {
            Map<String, ValueEntry> map = store.getStore();
            oos.writeObject(map);
            oos.flush();
            Files.move(tmpPath, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Snapshot saved successfully to: " + filePath);
        } catch (Exception e) {
            System.err.println("Failed to save snapshot to " + filePath + ": " + e.getMessage());
            try {
                Files.deleteIfExists(tmpPath);
            } catch (IOException deleteEx) {
                System.err.println("Failed to delete temporary snapshot file: " + deleteEx.getMessage());
            }
        }
    }
}
