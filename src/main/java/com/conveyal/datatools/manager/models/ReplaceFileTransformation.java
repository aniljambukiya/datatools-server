package com.conveyal.datatools.manager.models;

import com.conveyal.datatools.common.status.MonitorableJob;
import com.conveyal.datatools.manager.persistence.Persistence;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ReplaceFileTransformation extends FeedTransformation {

    /** no-arg constructor for de/serialization */
    public ReplaceFileTransformation() {}

    public static ReplaceFileTransformation create(String sourceVersionId, String table) {
        ReplaceFileTransformation transformation = new ReplaceFileTransformation();
        transformation.sourceVersionId = sourceVersionId;
        transformation.table = table;
        return transformation;
    }

    public void transform(FeedVersion target, MonitorableJob.Status status) {
        // TODO: Refactor into validation code?
        FeedVersion sourceVersion = Persistence.feedVersions.getById(sourceVersionId);
        if (sourceVersion == null) {
            status.fail("Source version ID must reference valid version.");
            return;
        }
        if (table == null) {
            status.fail("Must specify transformation table name.");
            return;
        }
        String tableName = table + ".txt";
        String tableNamePath = "/" + tableName;

        // Run the replace transformation
        Path sourceZipPath = Paths.get(sourceVersion.retrieveGtfsFile().getAbsolutePath());
        try (FileSystem sourceZipFs = FileSystems.newFileSystem(sourceZipPath, null)) {
            // If the source txt file does not exist, NoSuchFileException will be thrown and caught below.
            Path sourceTxtFilePath = sourceZipFs.getPath(tableNamePath);
            Path targetZipPath = Paths.get(target.retrieveGtfsFile().getAbsolutePath());
            try( FileSystem targetZipFs = FileSystems.newFileSystem(targetZipPath, null) ){
                Path targetTxtFilePath = targetZipFs.getPath(tableNamePath);
                // Copy a file into the zip file, replacing it if it already exists.
                Files.copy(sourceTxtFilePath, targetTxtFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (NoSuchFileException e) {
            status.fail("Source version does not contain table: " + tableName, e);
        } catch (Exception e) {
            status.fail("Unknown error encountered while transforming zip file", e);
        }
    }

    public boolean isAppliedBeforeLoad() {
        return true;
    }
}