package com.java_devrel.samples.panama.part_4;

import com.java_devrel.samples.stdlib.dirent.dirent;
import com.java_devrel.samples.stdlib.dirent.dirent_h;
import com.java_devrel.samples.stdlib.string.string_h;

import java.io.IOException;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.file.Files;
import java.nio.file.Path;


public class WalkDirectory {

    private static void listDirsJavaStyle(String dirName) throws IOException {
        try(var filesStream = Files.walk(Path.of(dirName))) {
            filesStream.forEach(System.out::println);
        }
    }

    private static void listDirsCStyle(String dirName, int indent, MemorySession scope) throws IOException {
        // const char *name
        var dirNameSegment = scope.allocateUtf8String(dirName);
        // DIR *dir;
        var dirStructPointerAddress = dirent_h.opendir(dirNameSegment);
        // if (!(dir = opendir(name)))
        if (dirStructPointerAddress == MemoryAddress.NULL) {
            throw new IOException("unable to open given directory");
        }

        while (true) {
            var dirEntryMemoryAddress = dirent_h.readdir(dirStructPointerAddress);
            // (entry = readdir(dir)) != NULL
            if (dirEntryMemoryAddress == MemoryAddress.NULL) {
                break;
            }
            // struct dirent *entry;
            var entryStruct = MemorySegment.ofAddress(dirEntryMemoryAddress, dirent.sizeof(), scope);
            // entry->d_type
            var entryType = dirent.d_type$get(entryStruct);
            // entry->d_name
            var nameMemorySegment = dirent.d_name$slice(entryStruct);
            var nameStr = nameMemorySegment.getUtf8String(0);
            // if (entry->d_type == DT_DIR)
            if (entryType == dirent_h.DT_DIR()) {
                var isCurrentDir = string_h.strcmp(nameMemorySegment, scope.allocateUtf8String(".")) == 0;
                var isParentDir = string_h.strcmp(nameMemorySegment, scope.allocateUtf8String("..")) == 0;
                // if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
                if (isCurrentDir || isParentDir) {
                    continue;
                }
                var path = "%s/%s".formatted(dirName, nameStr);
                System.out.printf("%s%s\n","-".repeat(indent), path);
                listDirsCStyle(path, indent + 3, scope);
            } else {
                System.out.printf("%s%s/%s\n","-".repeat(indent), dirName, nameStr);
            }
        }

         dirent_h.closedir(dirNameSegment);
    }

    public static void main(String[] args) throws IOException {
        var dirName = args[0];
        try(var scope = MemorySession.openConfined()) {
            listDirsCStyle(dirName, 0, scope);
        }
    }

}
