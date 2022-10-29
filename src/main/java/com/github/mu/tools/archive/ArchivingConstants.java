package com.github.mu.tools.archive;

public interface ArchivingConstants {

    String MEDIA_FOLDER_START = "/media/";

    String DEV_FOLDER_START = "/dev/";

    String LIST_PARTITIONS_CMD = "lsblk -l";

    String UMOUNT_CMD = "umount %s -l";
}
