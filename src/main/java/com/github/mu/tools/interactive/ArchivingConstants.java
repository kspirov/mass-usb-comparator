package com.github.mu.tools.interactive;

public interface ArchivingConstants {

    String MEDIA_FOLDER_START = "/media/";

    String DEV_FOLDER_START = "/dev/";

    String LIST_PARTITIONS_CMD = "lsblk -l";

    String UMOUNT_CMD = "umount %s -l";

    String FULL_UNMOUNT_CMD = "udisksctl power-off -b %s";
}
