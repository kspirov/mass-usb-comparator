package com.github.mu.tools.interactive;

public interface ArchivingConstants {

    String MEDIA_FOLDER_START = "/media/";

    String DEV_FOLDER_START = "/dev/";

    String LIST_PARTITIONS_CMD = "lsblk -l";

    String UDISK2_SERVICE_STOP = "systemctl stop udisks2";

    String UDISK2_SERVICE_START = "systemctl start udisks2";

    String SYNC_CMD = "sync";
    String UMOUNT_CMD = "umount %s";

    String UDISKCTL_UMOUNT_CMD = "umount %s -l";

    String UDISKCTL_POWER_OFF_CMD = "udisksctl power-off -b %s";
}
