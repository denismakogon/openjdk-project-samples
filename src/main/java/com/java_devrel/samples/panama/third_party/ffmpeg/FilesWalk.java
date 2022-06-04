package com.java_devrel.samples.panama.third_party.ffmpeg;

import com.java_devrel.samples.ffmpeg.libavformat.avio.AVIODirEntry;
import com.java_devrel.samples.ffmpeg.libavformat.avio.avio_h;
import com.java_devrel.samples.ffmpeg.libavformat.avformat.avformat_h;

import com.java_devrel.samples.stdlib.grp.group;
import com.java_devrel.samples.stdlib.grp.grp_h;
import com.java_devrel.samples.stdlib.pwd.passwd;
import com.java_devrel.samples.stdlib.pwd.pwd_h;
import java.lang.foreign.*;

import java.util.Map;

import static com.java_devrel.samples.ffmpeg.libavformat.avformat.avformat_h.C_POINTER;


public class FilesWalk {
    private final static Map<Integer, String> switcher = Map.of(
            avio_h.AVIO_ENTRY_DIRECTORY(), "<DIR>",
            avio_h.AVIO_ENTRY_FILE(), "<FILE>",
            avio_h.AVIO_ENTRY_BLOCK_DEVICE(), "<BLOCK DEVICE>",
            avio_h.AVIO_ENTRY_CHARACTER_DEVICE(), "<CHARACTER DEVICE>",
            avio_h.AVIO_ENTRY_NAMED_PIPE(), "<PIPE>",
            avio_h.AVIO_ENTRY_SYMBOLIC_LINK(), "<LINK>",
            avio_h.AVIO_ENTRY_SOCKET(), "<SOCKET>",
            avio_h.AVIO_ENTRY_SERVER(), "<SERVER>",
            avio_h.AVIO_ENTRY_SHARE(), "<SHARE>",
            avio_h.AVIO_ENTRY_WORKGROUP(),"<WORKGROUP>"
    );

    private static String typeString(int type) {
        return switcher.getOrDefault(type, "<UNKNOWN>");
    }

    private static ExitCodeException listOp(String dirName, SegmentAllocator allocator, MemorySession session) {
        ExitCodeException ex = null;
        int ret;
        // AVIODirEntry *entry = NULL;
        // p2p - pointer to pointer
        var p2p_AVIODirEntry = MemorySegment.allocateNative(C_POINTER, session);
        // AVIODirContext *ctx = NULL;
        var p2p_AVIODirContext = MemorySegment.allocateNative(C_POINTER, session);
        var cString = allocator.allocateUtf8String(dirName);

        // ret = avio_open_dir(&ctx, input_dir, NULL))
        ret = avio_h.avio_open_dir(p2p_AVIODirContext, cString, MemoryAddress.NULL);
        if (ret < 0) {
            System.err.println("can't open a dir: " + dirName);
            return ExitCodeException.fromReturnCode("avio_open_dir", ret, session);
        }

        var p_AVIODirContext = p2p_AVIODirContext.get(C_POINTER, 0);

        while (true) {
            // ret = avio_read_dir(ctx, &entry)
            ret = avio_h.avio_read_dir(p_AVIODirContext, p2p_AVIODirEntry);
            if (ret < 0) {
                System.err.println("can't read a dir: " + dirName);
                ex = ExitCodeException.fromReturnCode("avio_read_dir", ret, session);
                break;
            }
            // obtaining [AVIODirEntry *entry] pointer address
            var p_AVIODirEntry = p2p_AVIODirEntry.get(C_POINTER, 0);
            // checking if pointer is not NULL
            if (p_AVIODirEntry == MemoryAddress.NULL) {
                System.out.println("possibly no entries in a folder left");
                break;
            }
            // obtaining [AVIODirEntry entry] struct
            var dirEntryStruct = MemorySegment.ofAddress(
                    p_AVIODirEntry, AVIODirEntry.sizeof(), session
            );
            // AVIODirEntry entry->type
            var typeString = typeString(AVIODirEntry.type$get(dirEntryStruct));
            // AVIODirEntry entry->name
            var fileName = AVIODirEntry.name$get(dirEntryStruct);
            // AVIODirEntry entry->filemode
            var fileMode = AVIODirEntry.filemode$get(dirEntryStruct);
            // AVIODirEntry entry->user_id
            var userID = AVIODirEntry.user_id$get(dirEntryStruct);
            // AVIODirEntry entry->group_id
            var groupID = AVIODirEntry.group_id$get(dirEntryStruct);
            // AVIODirEntry entry->size
            var size = AVIODirEntry.size$get(dirEntryStruct);
            // AVIODirEntry entry->modification_timestamp
            var modifiedTimestamp = AVIODirEntry.modification_timestamp$get(dirEntryStruct);
            // AVIODirEntry entry->access_timestamp
            var accessTimestamp = AVIODirEntry.access_timestamp$get(dirEntryStruct);
            // AVIODirEntry entry->status_change_timestamp
            var statusChangeTimestamp = AVIODirEntry.status_change_timestamp$get(dirEntryStruct);

            // pwd = getpwuid(id);
            var passwdPointerAddress = pwd_h.getpwuid((int)userID);
            if (passwdPointerAddress == MemoryAddress.NULL) {
                ex = new ExitCodeException("passwdPointerAddress is NULL");
                break;
            }
            // struct passwd *pwd;
            var passwdSegment = MemorySegment.ofAddress(passwdPointerAddress, passwd.sizeof(), session);
            // pwd->pw_name
            var userName = passwd.pw_name$get(passwdSegment);

            // grp = getgrgid(id);
            var grpPointerAddress = grp_h.getgrgid((int)groupID);
            if (grpPointerAddress == MemoryAddress.NULL) {
                ex = new ExitCodeException("grpPointerAddress is NULL");
                break;
            }
            // struct group *grp;
            var grpSegment = MemorySegment.ofAddress(grpPointerAddress, group.sizeof(), session);
            // grp->gr_name
            var groupName = group.gr_name$get(grpSegment);

            System.out.printf("""
                    Type: %s
                    Name: %s
                    Size: %s
                    Mode: %s
                    User: %s
                    Group: %s
                    modified timestamp: %s
                    access timestamp: %s
                    status changed timestamp: %s
                    %n""",
                    typeString, fileName.getUtf8String(0),
                    size, fileMode, userName.getUtf8String(0),
                    groupName.getUtf8String(0),
                    modifiedTimestamp, accessTimestamp,
                    statusChangeTimestamp
            );

            avio_h.avio_free_directory_entry(p2p_AVIODirEntry);
        }

        return ex;
    }

    public static void main(String[] args) throws ExitCodeException {
        ExitCodeException ex;
        try (var session = MemorySession.openConfined()) {
            var allocator = SegmentAllocator.newNativeArena(session);
            avformat_h.avformat_network_init();
            session.addCloseAction(avformat_h::avformat_network_deinit);
            ex = listOp(args[0], allocator, session);
        }
        if (ex != null) {
            throw ex;
        }
    }

}
